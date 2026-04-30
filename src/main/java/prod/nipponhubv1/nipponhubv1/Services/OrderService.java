package prod.nipponhubv1.nipponhubv1.Services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import prod.nipponhubv1.nipponhubv1.Dto.OrderItemRequest;
import prod.nipponhubv1.nipponhubv1.Dto.OrderRequest;
import prod.nipponhubv1.nipponhubv1.Dto.OrderResponse;
import prod.nipponhubv1.nipponhubv1.Exception.OtakuException;
import prod.nipponhubv1.nipponhubv1.Mappers.OrderMapper;
import prod.nipponhubv1.nipponhubv1.Models.AffiliateCommission;
import prod.nipponhubv1.nipponhubv1.Models.AffiliateProfile;
import prod.nipponhubv1.nipponhubv1.Models.City;
import prod.nipponhubv1.nipponhubv1.Models.Country;
import prod.nipponhubv1.nipponhubv1.Models.LoyaltyAccount;
import prod.nipponhubv1.nipponhubv1.Models.LoyaltyTransaction;
import prod.nipponhubv1.nipponhubv1.Models.Order;
import prod.nipponhubv1.nipponhubv1.Models.OrderItem;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Models.Product;
import prod.nipponhubv1.nipponhubv1.Models.ProductVariant;
import prod.nipponhubv1.nipponhubv1.Models.Enums.CommissionStatus;
import prod.nipponhubv1.nipponhubv1.Models.Enums.OrderStatus;
import prod.nipponhubv1.nipponhubv1.Models.Enums.OrderType;
import prod.nipponhubv1.nipponhubv1.Models.Enums.Role;
import prod.nipponhubv1.nipponhubv1.Models.Enums.TransactionType;
import prod.nipponhubv1.nipponhubv1.Repository.AffiliateCommissionRepository;
import prod.nipponhubv1.nipponhubv1.Repository.AffiliateProfileRepository;
import prod.nipponhubv1.nipponhubv1.Repository.CityRepository;
import prod.nipponhubv1.nipponhubv1.Repository.InvoiceRepository;
import prod.nipponhubv1.nipponhubv1.Repository.LoyaltyAccountRepository;
import prod.nipponhubv1.nipponhubv1.Repository.LoyaltyGradeRepository;
import prod.nipponhubv1.nipponhubv1.Repository.LoyaltyTransactionRepository;
import prod.nipponhubv1.nipponhubv1.Repository.OrderItemRepository;
import prod.nipponhubv1.nipponhubv1.Repository.OrderRepository;
import prod.nipponhubv1.nipponhubv1.Repository.ProductVariantRepository;
import prod.nipponhubv1.nipponhubv1.Repository.UserRepository;

/**
 * Gestion complète du cycle de vie des commandes.
 *
 * Flux ONLINE :
 *   CLIENT passe commande → PENDING
 *   Admin confirme        → CONFIRMED
 *   Admin prépare         → PROCESSING
 *   Livraison en cours    → SHIPPED
 *   Livraison confirmée   → DELIVERED (points crédités + commission affilié)
 *   Annulation possible   → CANCELLED (stock restauré)
 *
 * Flux POS (vente comptoir) :
 *   Admin enregistre → DELIVERED directement
 *   Points crédités immédiatement
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository           orderRepo;
    private final OrderItemRepository       orderItemRepo;
    private final UserRepository            userRepo;
    private final ProductVariantRepository  variantRepo;
    private final CityRepository            cityRepo;
    private final LoyaltyAccountRepository  loyaltyRepo;
    private final LoyaltyTransactionRepository loyaltyTxRepo;
    private final LoyaltyGradeRepository    gradeRepo;
    private final AffiliateProfileRepository affiliateRepo;
    private final AffiliateCommissionRepository commissionRepo;
    private final InvoiceRepository         invoiceRepo;
    private final StockService              stockService;
    private final FlashSaleService          flashSaleService;
    private final NotificationService       notificationService;
    private final OrderMapper               orderMapper;

    // Règle de fidélité : 1 point par tranche de 1000 XAF dépensés
    private static final BigDecimal POINTS_PER_UNIT = BigDecimal.valueOf(1000);
    // Valeur d'un point en XAF lors de la dépense
    private static final BigDecimal POINT_VALUE_XAF = BigDecimal.valueOf(100);

    // ── Passer une commande (CLIENT) ──────────────────────────────────────────

    /**
     * Crée une nouvelle commande en ligne.
     *
     * Flow :
     *  1. Résoudre les variantes et calculer les prix (avec flash sale)
     *  2. Vérifier et déduire les points de fidélité si demandé
     *  3. Vérifier + décrémenter les stocks
     *  4. Persister Order + OrderItems
     *  5. Tracker la commande affiliée si le client vient d'un referral
     */
    @Transactional
    public OrderResponse placeOrder(String clientEmail, OrderRequest req) {
        OurUser client = userRepo.findByEmail(clientEmail)
            .orElseThrow(() -> OtakuException.notFound("Utilisateur", 0L));

        City city = cityRepo.findByIdAndActiveTrue(req.getCityId())
            .orElseThrow(() -> OtakuException.notFound("Ville", req.getCityId()));

        Country orderCountry = city.getCountry();
        if (client.getCountry() == null) {
            client.setCountry(orderCountry);
        }
        if (client.getCity() == null || !client.getCity().getId().equals(city.getId())) {
            client.setCity(city);
        }
        userRepo.save(client);

        if (!client.getCountry().getId().equals(orderCountry.getId())) {
            client.setCountry(orderCountry);
        }
        userRepo.save(client);

        // ── Résoudre les articles ────────────────────────────────────────────
        List<OrderItem> items = resolveItems(req.getItems(), orderCountry.getId());

        BigDecimal subtotal = items.stream()
            .map(OrderItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── Points de fidélité ───────────────────────────────────────────────
        BigDecimal pointsDiscount = BigDecimal.ZERO;
        int        pointsUsed     = 0;

        if (req.getPointsToUse() > 0) {
            LoyaltyAccount loyalty = loyaltyRepo.findByUserId(client.getId())
                .orElseThrow(() -> OtakuException.badRequest("Compte fidélité introuvable."));

            if (loyalty.getPointsBalance() < req.getPointsToUse()) {
                throw OtakuException.badRequest(
                    "Solde de points insuffisant. Disponible : "
                        + loyalty.getPointsBalance()
                );
            }

            pointsUsed     = req.getPointsToUse();
            pointsDiscount = BigDecimal.valueOf(pointsUsed)
                .multiply(POINT_VALUE_XAF);

            // Plafonner la remise au total
            if (pointsDiscount.compareTo(subtotal) > 0) {
                pointsDiscount = subtotal;
                pointsUsed = subtotal.divide(POINT_VALUE_XAF, RoundingMode.UP).intValue();
            }
        }

        BigDecimal total = subtotal.subtract(pointsDiscount)
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);

        // ── Résoudre partenaire affilié ───────────────────────────────────────
        AffiliateProfile affiliate = affiliateRepo
            .findByUserId(client.getId())
            .filter(ap -> ap.isActive() && ap.getUser().getId().equals(client.getId()))
            .orElse(null);
        // (plus précisément : chercher si la session du client contient un referralCode actif)

        // ── Persister la commande ────────────────────────────────────────────
        Order order = Order.builder()
            .user(client)
            .country(orderCountry)
            .city(city)
            .orderType(OrderType.ONLINE)
            .status(OrderStatus.PENDING)
            .subtotal(subtotal)
            .discountAmount(pointsDiscount)
            .pointsUsed(pointsUsed)
            .pointsDiscount(pointsDiscount)
            .totalAmount(total)
            .affiliate(affiliate)
            .notes(req.getNotes())
            .build();

        Order saved = orderRepo.save(order);

        // Lier les items à la commande
        items.forEach(i -> i.setOrder(saved));
        orderItemRepo.saveAll(items);

        // ── Déduire les points si utilisés ───────────────────────────────────
        if (pointsUsed > 0) {
            deductLoyaltyPoints(client, saved, pointsUsed);
        }

        // ── Notifications ───────────────────────────────────────────────────
        // Notifier l'admin
        userRepo.findByRole(Role.ADMIN).forEach(admin ->
            notificationService.notifyAdminNewOrder(admin.getId(), saved.getId(),
                client.getEmail(), total)
        );

        // Notifier les owners dont les produits sont dans la commande
        items.stream()
            .map(item -> item.getVariant().getProduct().getCreatedByUser())
            .filter(createdBy -> createdBy != null && createdBy.getRole() == Role.OWNER)
            .distinct()
            .forEach(owner ->
                notificationService.notifyProductOwnerNewOrder(
                    owner.getId(),
                    saved.getId(),
                    items.get(0).getVariant().getProduct().getId(),
                    client.getEmail(),
                    total.toString() + " XAF"
                )
            );

        log.info("✓ Commande créée — id={} client={} total={}",
            saved.getId(), clientEmail, total);

        return orderMapper.toResponse(orderRepo.findById(saved.getId()).orElseThrow());
    }

    // ── Vente POS (Admin comptoir) ────────────────────────────────────────────

    /**
     * Enregistre une vente physique comptoir.
     * Statut directement DELIVERED, points crédités immédiatement.
     *
     * @param clientQrCode QR code du Nippon Pass du client (scanné)
     */
    @Transactional
    public OrderResponse createPosOrder(String clientQrCode,
                                         OrderRequest req,
                                         OurUser admin) {
        // Trouver le client via son QR code
        LoyaltyAccount loyalty = loyaltyRepo.findByQrCode(clientQrCode)
            .orElseThrow(() -> OtakuException.notFound(
                "Client (QR code=" + clientQrCode + ")", 0L
            ));

        OurUser client = loyalty.getUser();

        City city = cityRepo.findByIdAndActiveTrue(req.getCityId())
            .orElseThrow(() -> OtakuException.notFound("Ville", req.getCityId()));

        List<OrderItem> items = resolveItems(req.getItems(),
            client.getCountry() != null ? client.getCountry().getId() : null);

        BigDecimal subtotal = items.stream()
            .map(OrderItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (client.getCountry() != null) {
            stockService.decrementForOrder(items, client.getCountry().getId());
        }

        Order order = Order.builder()
            .user(client)
            .country(client.getCountry())
            .city(city)
            .orderType(OrderType.POS)
            .status(OrderStatus.DELIVERED)   // Livré immédiatement
            .subtotal(subtotal)
            .discountAmount(BigDecimal.ZERO)
            .totalAmount(subtotal)
            .deliveredAt(LocalDateTime.now())
            .notes(req.getNotes())
            .build();

        Order saved = orderRepo.save(order);
        items.forEach(i -> i.setOrder(saved));
        orderItemRepo.saveAll(items);

        // Créditer les points immédiatement
        creditLoyaltyPoints(client, saved, subtotal);

        log.info("✓ Vente POS — orderId={} client={} total={}",
            saved.getId(), client.getEmail(), subtotal);

        return orderMapper.toResponse(saved);
    }

    // ── Gestion statuts (Admin) ───────────────────────────────────────────────

    /**
     * Changer le statut d'une commande.
     * Déclenche les effets de bord selon la transition :
     *   → DELIVERED : crédite points + calcule commission affilié
     *   → CANCELLED : restaure le stock
     */
    @Transactional
    public OrderResponse updateStatus(Long orderId,
                                       OrderStatus newStatus,
                                       OurUser admin) {
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> OtakuException.notFound("Commande", orderId));

        validateStatusTransition(order.getStatus(), newStatus);

        OrderStatus previous = order.getStatus();
        order.setStatus(newStatus);

        if (previous == OrderStatus.PENDING && newStatus == OrderStatus.CONFIRMED) {
            stockService.decrementForOrder(order.getItems(), order.getCountry().getId());
        }

        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
            // Créditer les points fidélité
            creditLoyaltyPoints(order.getUser(), order, order.getTotalAmount());
            // Calculer et créer la commission affiliée
            if (order.getAffiliate() != null) {
                createAffiliateCommission(order);
            }
        }

        if (newStatus == OrderStatus.CANCELLED && shouldRestoreStock(previous)) {
            stockService.restoreForCancelledOrder(
                order.getItems(), order.getCountry().getId()
            );
            // Rembourser les points utilisés
            if (order.getPointsUsed() > 0) {
                refundLoyaltyPoints(order.getUser(), order);
            }
        }

        Order updated = orderRepo.save(order);
        log.info("Statut commande {} : {} → {}", orderId, previous, newStatus);
        return orderMapper.toResponse(updated);
    }

    // ── Historique ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(String email, Pageable pageable) {
        OurUser user = userRepo.findByEmail(email)
            .orElseThrow(() -> OtakuException.notFound("Utilisateur", 0L));
        return orderRepo.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
            .map(orderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrdersByCountry(Long countryId,
                                                      OrderStatus status,
                                                      Pageable pageable) {
        if (countryId == null) {
            if (status != null) {
                return orderRepo.findByStatusOrderByCreatedAtDesc(status, pageable)
                    .map(orderMapper::toResponse);
            }
            return orderRepo.findAllByOrderByCreatedAtDesc(pageable)
                .map(orderMapper::toResponse);
        }

        if (status != null) {
            return orderRepo.findByCountryIdAndStatusOrderByCreatedAtDesc(
                countryId, status, pageable).map(orderMapper::toResponse);
        }
        return orderRepo.findByCountryIdOrderByCreatedAtDesc(countryId, pageable)
            .map(orderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersForBackoffice(OurUser requester,
                                                      Long countryId,
                                                      OrderStatus status,
                                                      Pageable pageable) {
        if (requester.getRole() == Role.ADMIN) {
            return getAllOrdersByCountry(countryId, status, pageable);
        }

        if (requester.getRole() == Role.OWNER) {
            if (requester.getCity() != null) {
                if (status != null) {
                    return orderRepo.findByCityIdAndStatusOrderByCreatedAtDesc(
                        requester.getCity().getId(), status, pageable
                    ).map(orderMapper::toResponse);
                }
                return orderRepo.findByCityIdOrderByCreatedAtDesc(
                    requester.getCity().getId(), pageable
                ).map(orderMapper::toResponse);
            }

            if (requester.getCountry() != null) {
                return getAllOrdersByCountry(requester.getCountry().getId(), status, pageable);
            }
        }

        throw OtakuException.forbidden("Acces backoffice interdit pour ce role ou cette localite.");
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id, String requesterEmail) {
        Order order = orderRepo.findById(id)
            .orElseThrow(() -> OtakuException.notFound("Commande", id));

        // Un client ne peut voir que ses propres commandes
        if (!order.getUser().getEmail().equals(requesterEmail)) {
            OurUser requester = userRepo.findByEmail(requesterEmail).orElseThrow();
            if (requester.getRole() == Role.CLIENT) {
                throw OtakuException.forbidden("Accès refusé à cette commande.");
            }
        }
        return orderMapper.toResponse(order);
    }

    // ── Utilitaires internes ──────────────────────────────────────────────────

    /**
     * Résout les variantes, vérifie leur existence et calcule les prix
     * avec les éventuelles flash sales actives.
     */
    private List<OrderItem> resolveItems(List<OrderItemRequest> reqs, Long countryId) {
        return reqs.stream().map(r -> {
            ProductVariant variant = variantRepo.findByIdWithProduct(r.getVariantId())
                .orElseThrow(() -> OtakuException.notFound("Variante", r.getVariantId()));

            Product product = variant.getProduct();

            // Prix de base avec flash sale éventuelle
            BigDecimal basePrice = flashSaleService
                .getActiveForProduct(product.getId())
                .map(fs -> {
                    BigDecimal pct      = fs.getDiscountPct().divide(BigDecimal.valueOf(100));
                    BigDecimal discount = product.getBasePrice().multiply(pct);
                    return product.getBasePrice().subtract(discount)
                        .setScale(2, RoundingMode.HALF_UP);
                })
                .orElse(product.getBasePrice());

            BigDecimal unitPrice  = basePrice.add(variant.getExtraPrice());
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(r.getQuantity()));

            return OrderItem.builder()
                .variant(variant)
                .quantity(r.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .build();
        }).toList();
    }

    private void creditLoyaltyPoints(OurUser user, Order order, BigDecimal amount) {
        loyaltyRepo.findByUserId(user.getId()).ifPresent(loyalty -> {
            int points = amount.divide(POINTS_PER_UNIT, RoundingMode.DOWN).intValue();
            if (points <= 0) return;

            loyalty.setPointsBalance(loyalty.getPointsBalance() + points);
            loyalty.setTotalSpent(loyalty.getTotalSpent().add(amount));

            // Mettre à jour le grade
            gradeRepo.findGradeForSpent(loyalty.getTotalSpent())
                .ifPresent(loyalty::setGrade);

            loyaltyRepo.save(loyalty);

            loyaltyTxRepo.save(LoyaltyTransaction.builder()
                .loyaltyAccount(loyalty)
                .order(order)
                .transactionType(TransactionType.EARN)
                .points(points)
                .description("Points gagnés sur commande #" + order.getId())
                .build());

            log.info("+{} points fidélité — userId={} orderId={}",
                points, user.getId(), order.getId());
        });
    }

    private void deductLoyaltyPoints(OurUser user, Order order, int points) {
        loyaltyRepo.findByUserId(user.getId()).ifPresent(loyalty -> {
            loyalty.setPointsBalance(loyalty.getPointsBalance() - points);
            loyaltyRepo.save(loyalty);

            loyaltyTxRepo.save(LoyaltyTransaction.builder()
                .loyaltyAccount(loyalty)
                .order(order)
                .transactionType(TransactionType.SPEND)
                .points(-points)
                .description("Points utilisés sur commande #" + order.getId())
                .build());
        });
    }

    private void refundLoyaltyPoints(OurUser user, Order order) {
        loyaltyRepo.findByUserId(user.getId()).ifPresent(loyalty -> {
            loyalty.setPointsBalance(loyalty.getPointsBalance() + order.getPointsUsed());
            loyaltyRepo.save(loyalty);

            loyaltyTxRepo.save(LoyaltyTransaction.builder()
                .loyaltyAccount(loyalty)
                .order(order)
                .transactionType(TransactionType.REFUND)
                .points(order.getPointsUsed())
                .description("Remboursement points — commande annulée #" + order.getId())
                .build());
        });
    }

    private void createAffiliateCommission(Order order) {
        AffiliateProfile affiliate = order.getAffiliate();
        BigDecimal commissionAmount = order.getTotalAmount()
            .multiply(affiliate.getCommissionPct().divide(BigDecimal.valueOf(100)))
            .setScale(2, RoundingMode.HALF_UP);

        commissionRepo.save(AffiliateCommission.builder()
            .affiliate(affiliate)
            .order(order)
            .commissionAmount(commissionAmount)
            .status(CommissionStatus.PENDING)
            .build());

        log.info("Commission affiliée créée — affiliateId={} orderId={} montant={}",
            affiliate.getId(), order.getId(), commissionAmount);
    }

    /**
     * Transitions autorisées :
     * PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
     * PENDING / CONFIRMED / PROCESSING → CANCELLED
     */
    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING    -> next == OrderStatus.CONFIRMED
                            || next == OrderStatus.CANCELLED;
            case CONFIRMED  -> next == OrderStatus.PROCESSING
                            || next == OrderStatus.CANCELLED;
            case PROCESSING -> next == OrderStatus.SHIPPED
                            || next == OrderStatus.DELIVERED
                            || next == OrderStatus.CANCELLED;
            case SHIPPED    -> next == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED, REFUNDED -> false;
        };

        if (!valid) {
            throw OtakuException.badRequest(
                "Transition invalide : " + current + " → " + next
            );
        }
    }

    private boolean shouldRestoreStock(OrderStatus previous) {
        return previous == OrderStatus.CONFIRMED
            || previous == OrderStatus.PROCESSING
            || previous == OrderStatus.SHIPPED;
    }
}
