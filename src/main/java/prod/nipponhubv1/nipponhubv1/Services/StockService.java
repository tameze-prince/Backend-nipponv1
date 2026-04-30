package prod.nipponhubv1.nipponhubv1.Services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import prod.nipponhubv1.nipponhubv1.Dto.StockAdjustmentRequest;
import prod.nipponhubv1.nipponhubv1.Dto.StockResponse;
import prod.nipponhubv1.nipponhubv1.Exception.OtakuException;
import prod.nipponhubv1.nipponhubv1.Mappers.StockMapper;
import prod.nipponhubv1.nipponhubv1.Models.Country;
import prod.nipponhubv1.nipponhubv1.Models.OrderItem;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Models.ProductVariant;
import prod.nipponhubv1.nipponhubv1.Models.Stock;
import prod.nipponhubv1.nipponhubv1.Models.StockMovement;
import prod.nipponhubv1.nipponhubv1.Models.Wishlist;
import prod.nipponhubv1.nipponhubv1.Models.Enums.MovementType;
import prod.nipponhubv1.nipponhubv1.Models.Enums.Role;
import prod.nipponhubv1.nipponhubv1.Repository.CountryRepository;
import prod.nipponhubv1.nipponhubv1.Repository.ProductVariantRepository;
import prod.nipponhubv1.nipponhubv1.Repository.StockMovementRepository;
import prod.nipponhubv1.nipponhubv1.Repository.StockRepository;
import prod.nipponhubv1.nipponhubv1.Repository.UserRepository;
import prod.nipponhubv1.nipponhubv1.Repository.WishlistRepository;

/**
 * Gestion des stocks par pays.
 *
 * Responsabilités :
 *  - Consultation des stocks (Admin, par pays)
 *  - Ajustement manuel avec journal (StockMovement)
 *  - Décrémentation lors des commandes
 *  - Alertes stock faible
 *  - Notification wishlist au restockage
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final StockRepository           stockRepo;
    private final StockMovementRepository   movementRepo;
    private final ProductVariantRepository  variantRepo;
    private final CountryRepository         countryRepo;
    private final WishlistRepository        wishlistRepo;
    private final UserRepository            userRepo;
    private final StockMapper               stockMapper;

    // ── Consultation ──────────────────────────────────────────────────────────

    /**
     * Tous les stocks d'un pays avec détails produit/variante.
     */
    @Transactional(readOnly = true)
    public List<StockResponse> getStockByCountry(Long countryId, Long cityId, OurUser requester) {
        Long effectiveCountryId = resolveAccessibleCountryId(countryId, cityId, requester);
        return stockMapper.toResponseList(
            stockRepo.findByCountryIdWithDetails(effectiveCountryId)
        );
    }

    /**
     * Stocks faibles dans un pays (alerte réapprovisionnement).
     */
    @Transactional(readOnly = true)
    public List<StockResponse> getLowStocks(Long countryId, Long cityId, OurUser requester) {
        Long effectiveCountryId = resolveAccessibleCountryId(countryId, cityId, requester);
        return stockMapper.toResponseList(
            stockRepo.findLowStockByCountry(effectiveCountryId, LOW_STOCK_THRESHOLD)
        );
    }

    /**
     * Journal des mouvements d'une variante dans un pays.
     */
    @Transactional(readOnly = true)
    public List<StockMovement> getMovements(Long variantId, Long countryId, Long cityId, OurUser requester) {
        Long effectiveCountryId = resolveAccessibleCountryId(countryId, cityId, requester);
        return movementRepo.findByVariantIdAndCountryIdOrderByCreatedAtDesc(
            variantId, effectiveCountryId
        );
    }

    // ── Ajustement manuel (Admin) ─────────────────────────────────────────────

    /**
     * Ajuste le stock manuellement et enregistre le mouvement dans le journal.
     * Déclenche les notifications wishlist si c'est un restockage (IN / ADJUSTMENT).
     */
    @Transactional
    public StockResponse adjust(StockAdjustmentRequest req, OurUser admin) {
        ProductVariant variant = variantRepo.findById(req.getVariantId())
            .orElseThrow(() -> OtakuException.notFound("Variante", req.getVariantId()));

        Country country = countryRepo.findById(req.getCountryId())
            .orElseThrow(() -> OtakuException.notFound("Pays", req.getCountryId()));

        Stock stock = stockRepo.findByVariantIdAndCountryId(
                req.getVariantId(), req.getCountryId())
            .orElseGet(() -> Stock.builder()
                .variant(variant)
                .country(country)
                .quantity(0)
                .build()
            );

        int before = stock.getQuantity();
        int after  = computeNewQuantity(stock.getQuantity(), req);

        if (after < 0) {
            throw OtakuException.badRequest(
                "Stock insuffisant. Stock actuel : " + before
            );
        }

        stock.setQuantity(after);
        Stock saved = stockRepo.save(stock);

        // Journal
        movementRepo.save(StockMovement.builder()
            .variant(variant)
            .country(country)
            .user(admin)
            .movementType(req.getMovementType())
            .quantity(req.getQuantity())
            .reason(req.getReason())
            .build());

        log.info("Stock ajusté — variant={} country={} {} → {}",
            req.getVariantId(), req.getCountryId(), before, after);

        // Notifier les clients wishlist si restockage
        if ((req.getMovementType() == MovementType.IN
             || req.getMovementType() == MovementType.ADJUSTMENT)
            && before == 0 && after > 0) {
            notifyWishlistUsers(req.getVariantId(), country);
        }

        return stockMapper.toResponse(saved);
    }

    // ── Opérations internes (appelées par OrderService) ───────────────────────

    /**
     * Vérifie et décrémente le stock lors d'une commande.
     * Appelé dans une transaction existante (@Transactional de OrderService).
     *
     * @throws OtakuException si stock insuffisant pour un article
     */
    public void decrementForOrder(List<OrderItem> items, Long countryId) {
        for (OrderItem item : items) {
            Long variantId = item.getVariant().getId();
            Stock stock = stockRepo.findByVariantIdAndCountryId(variantId, countryId)
                .orElseThrow(() -> OtakuException.badRequest(
                    "Pas de stock disponible pour la variante id=" + variantId
                        + " dans ce pays."
                ));

            if (stock.getQuantity() < item.getQuantity()) {
                throw OtakuException.badRequest(
                    "Stock insuffisant pour « "
                        + item.getVariant().getProduct().getName()
                        + " — " + item.getVariant().getLabel()
                        + " ». Disponible : " + stock.getQuantity()
                        + ", demandé : " + item.getQuantity()
                );
            }

            stock.setQuantity(stock.getQuantity() - item.getQuantity());
            stockRepo.save(stock);
        }
    }

    /**
     * Réincrémente le stock lors d'une annulation de commande.
     */
    public void restoreForCancelledOrder(List<OrderItem> items, Long countryId) {
        for (OrderItem item : items) {
            stockRepo.findByVariantIdAndCountryId(item.getVariant().getId(), countryId)
                .ifPresent(s -> {
                    s.setQuantity(s.getQuantity() + item.getQuantity());
                    stockRepo.save(s);
                });
        }
    }

    // ── Notifications wishlist ────────────────────────────────────────────────

    private void notifyWishlistUsers(Long variantId, Country country) {
        List<Wishlist> wishlists = wishlistRepo.findByVariantIdWithUsers(variantId);
        if (wishlists.isEmpty()) return;

        log.info("Notification restockage — {} client(s) à notifier (variant={})",
            wishlists.size(), variantId);

        // TODO : intégrer un service d'envoi WhatsApp / Email
        // Pour l'instant, on logue les clients à notifier
        wishlists.forEach(w ->
            log.info("→ Notifier {} {} ({})",
                w.getUser().getFirstName(),
                w.getUser().getLastName(),
                w.getUser().getEmail())
        );
    }

    // ── Interne ───────────────────────────────────────────────────────────────

    private int computeNewQuantity(int current, StockAdjustmentRequest req) {
        return switch (req.getMovementType()) {
            case IN         -> current + req.getQuantity();
            case OUT        -> current - req.getQuantity();
            case ADJUSTMENT -> req.getQuantity();   // valeur absolue
            case RETURN     -> current + req.getQuantity();
        };
    }

    private Long resolveAccessibleCountryId(Long countryId, Long cityId, OurUser requester) {
        if (requester == null || requester.getRole() != Role.OWNER) {
            return countryId;
        }

        if (requester.getCountry() == null) {
            throw OtakuException.forbidden("Compte owner sans pays rattache.");
        }

        if (countryId != null && !requester.getCountry().getId().equals(countryId)) {
            throw OtakuException.forbidden("Un owner ne peut consulter que le stock de son pays.");
        }

        if (requester.getCity() != null) {
            if (cityId == null) {
                throw OtakuException.forbidden("Le cityId est requis pour un owner.");
            }

            if (!requester.getCity().getId().equals(cityId)) {
                throw OtakuException.forbidden("Un owner ne peut consulter que le stock de sa ville.");
            }
        }

        return requester.getCountry().getId();
    }
}
