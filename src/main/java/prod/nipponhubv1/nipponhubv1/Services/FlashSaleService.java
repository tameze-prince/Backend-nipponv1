package prod.nipponhubv1.nipponhubv1.Services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import prod.nipponhubv1.nipponhubv1.Dto.FlashSaleRequest;
import prod.nipponhubv1.nipponhubv1.Exception.OtakuException;
import prod.nipponhubv1.nipponhubv1.Models.FlashSale;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Models.Product;
import prod.nipponhubv1.nipponhubv1.Models.Enums.Role;
import prod.nipponhubv1.nipponhubv1.Repository.FlashSaleRepository;
import prod.nipponhubv1.nipponhubv1.Repository.ProductRepository;

/**
 * Gestion des ventes flash.
 *
 * Règles métier :
 *  - Un produit ne peut avoir qu'une seule vente flash active à la fois
 *  - La date de fin doit être après la date de début
 *  - La remise doit être entre 1% et 99%
 *  - Seuls ADMIN et OWNER peuvent créer / désactiver
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlashSaleService {

    private final FlashSaleRepository flashSaleRepo;
    private final ProductRepository   productRepo;

    // ── Lecture ───────────────────────────────────────────────────────────────

    /**
     * Toutes les ventes flash actives en ce moment.
     */
    public List<FlashSale> getActiveFlashSales() {
        return flashSaleRepo.findAllActive(LocalDateTime.now());
    }

    /**
     * Toutes les ventes flash actives au sens gestion admin: en cours ou programmees.
     */
    public List<FlashSale> getEnabledFlashSales() {
        return flashSaleRepo.findAllEnabled();
    }

    /**
     * Vente flash active d'un produit (null si aucune).
     */
    public Optional<FlashSale> getActiveForProduct(Long productId) {
        return flashSaleRepo.findActiveByProductId(productId, LocalDateTime.now());
    }

    // ── CRUD Admin ────────────────────────────────────────────────────────────

    /**
     * Créer une vente flash.
     * Vérifie qu'aucune autre vente flash ne chevauche la période sur ce produit.
     * Vérifie aussi que le créateur en a la permission.
     */
    @Transactional
    public FlashSale create(FlashSaleRequest req, OurUser creator) {
        validateDates(req.getStartsAt(), req.getEndsAt());
        Product product = productRepo.findById(req.getProductId())
            .filter(Product::isActive)
            .orElseThrow(() -> OtakuException.notFound("Produit", req.getProductId()));

        // ── Vérifier les permissions ─────────────────────────────────────────
        checkFlashSalePermission(product, creator);

        // Vérifier le chevauchement
        if (flashSaleRepo.existsOverlap(
                req.getProductId(), req.getStartsAt(), req.getEndsAt())) {
            throw OtakuException.conflict(
                "Une vente flash existe déjà sur ce produit pour cette période."
            );
        }

        FlashSale fs = FlashSale.builder()
            .product(product)
            .discountPct(req.getDiscountPct())
            .startsAt(req.getStartsAt())
            .endsAt(req.getEndsAt())
            .createdBy(creator)
            .active(true)
            .build();

        FlashSale saved = flashSaleRepo.save(fs);
        log.info("✓ Flash sale créée — productId={} remise={}% de {} à {} by={}",
            product.getId(), req.getDiscountPct(), req.getStartsAt(), req.getEndsAt(),
            creator.getEmail());
        return saved;
    }

    @Transactional
    public FlashSale update(Long id, FlashSaleRequest req, OurUser updater) {
        validateDates(req.getStartsAt(), req.getEndsAt());

        FlashSale flashSale = flashSaleRepo.findById(id)
            .orElseThrow(() -> OtakuException.notFound("Flash sale", id));

        Product product = productRepo.findById(req.getProductId())
            .filter(Product::isActive)
            .orElseThrow(() -> OtakuException.notFound("Produit", req.getProductId()));

        // ── Vérifier les permissions ─────────────────────────────────────────
        checkFlashSalePermission(product, updater);

        if (flashSaleRepo.existsOverlapExcludingId(
                id, req.getProductId(), req.getStartsAt(), req.getEndsAt())) {
            throw OtakuException.conflict(
                "Une vente flash existe déjà sur ce produit pour cette période."
            );
        }

        flashSale.setProduct(product);
        flashSale.setDiscountPct(req.getDiscountPct());
        flashSale.setStartsAt(req.getStartsAt());
        flashSale.setEndsAt(req.getEndsAt());
        flashSale.setCreatedBy(updater);
        flashSale.setActive(true);

        FlashSale saved = flashSaleRepo.save(flashSale);
        log.info("Flash sale mise à jour — id={} productId={} remise={}% de {} à {} by={}",
            saved.getId(), product.getId(), req.getDiscountPct(), req.getStartsAt(), req.getEndsAt(),
            updater.getEmail());
        return saved;
    }

    /**
     * Désactiver une vente flash avant son terme.
     */
    @Transactional
    public void deactivate(Long id, OurUser requester) {
        FlashSale fs = flashSaleRepo.findById(id)
            .orElseThrow(() -> OtakuException.notFound("Flash sale", id));

        // ── Vérifier les permissions ─────────────────────────────────────────
        checkFlashSalePermission(fs.getProduct(), requester);

        fs.setActive(false);
        flashSaleRepo.save(fs);
        log.info("Flash sale désactivée — id={} by={}", id, requester.getEmail());
    }

    // ── Helper: Vérifier les permissions ───────────────────────────────────

    private void checkFlashSalePermission(Product product, OurUser requester) {
        if (requester.getRole() == Role.ADMIN) {
            return; // Admin peut tout faire
        }

        if (requester.getRole() == Role.OWNER) {
            if (product.getCreatedByUser() == null
                || !product.getCreatedByUser().getId().equals(requester.getId())) {
                throw OtakuException.forbidden(
                    "Vous n'avez pas la permission de gérer les ventes flash de ce produit."
                );
            }
            return;
        }

        throw OtakuException.forbidden(
            "Seul un ADMIN ou un OWNER peut gérer les ventes flash."
        );
    }

    private void validateDates(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw OtakuException.badRequest(
                "La date de fin doit être après la date de début."
            );
        }
        if (startsAt.isBefore(LocalDateTime.now())) {
            throw OtakuException.badRequest(
                "La date de début ne peut pas être dans le passé."
            );
        }
    }
}
