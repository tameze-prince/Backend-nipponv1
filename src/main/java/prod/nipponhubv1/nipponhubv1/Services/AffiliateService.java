package prod.nipponhubv1.nipponhubv1.Services;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import prod.nipponhubv1.nipponhubv1.Dto.AffiliateCommissionResponse;
import prod.nipponhubv1.nipponhubv1.Dto.AffiliatePaymentRequest;
import prod.nipponhubv1.nipponhubv1.Dto.AffiliateStatsResponse;
import prod.nipponhubv1.nipponhubv1.Exception.OtakuException;
import prod.nipponhubv1.nipponhubv1.Mappers.AffiliateMapper;
import prod.nipponhubv1.nipponhubv1.Models.AffiliateClick;
import prod.nipponhubv1.nipponhubv1.Models.AffiliateCommission;
import prod.nipponhubv1.nipponhubv1.Models.AffiliatePaymentOrder;
import prod.nipponhubv1.nipponhubv1.Models.AffiliateProfile;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Models.Enums.CommissionStatus;
import prod.nipponhubv1.nipponhubv1.Models.Enums.PaymentOrderStatus;
import prod.nipponhubv1.nipponhubv1.Repository.AffiliateClickRepository;
import prod.nipponhubv1.nipponhubv1.Repository.AffiliateCommissionRepository;
import prod.nipponhubv1.nipponhubv1.Repository.AffiliatePaymentOrderRepository;
import prod.nipponhubv1.nipponhubv1.Repository.AffiliateProfileRepository;
import prod.nipponhubv1.nipponhubv1.Repository.UserRepository;

/**
 * Gestion du programme d'affiliation.
 *
 * Acteurs :
 *  - PARTNER  : voit son portail (commissions, statistiques, lien parrainage)
 *  - OWNER    : valide les paiements, modifie les taux, active/désactive
 *
 * Flux de paiement :
 *  commissions PENDING
 *    → confirmées par Owner (CONFIRMED)
 *    → groupées dans un AffiliatePaymentOrder
 *    → preuve de virement uploadée
 *    → Owner valide → statut PAID
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AffiliateService {

    private final AffiliateProfileRepository    profileRepo;
    private final AffiliateCommissionRepository commissionRepo;
    private final AffiliatePaymentOrderRepository paymentOrderRepo;
    private final AffiliateClickRepository      clickRepo;
    private final UserRepository                userRepo;
    private final CloudinaryService             cloudinaryService;
    private final AffiliateMapper               affiliateMapper;

    // ── PARTNER — Portail ─────────────────────────────────────────────────────

    /**
     * Statistiques complètes du partenaire connecté.
     */
    public AffiliateStatsResponse getMyStats(String email) {
        OurUser user = findUserByEmail(email);
        AffiliateProfile profile = findProfileByUser(user.getId());

        long totalClicks = clickRepo.countByAffiliateId(profile.getId());
        long totalOrders = commissionRepo
            .findByAffiliateIdAndStatus(profile.getId(), CommissionStatus.CONFIRMED)
            .size();

        Pageable last10 = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        List<AffiliateCommissionResponse> recent =
            commissionRepo.findByAffiliateIdOrderByCreatedAtDesc(profile.getId(), last10)
                .stream()
                .map(affiliateMapper::toCommissionResponse)
                .toList();

        AffiliateStatsResponse stats = affiliateMapper.toStatsResponse(profile);
        stats.setTotalClicks(totalClicks);
        stats.setTotalOrders(totalOrders);
        stats.setRecentCommissions(recent);
        return stats;
    }

    /**
     * Historique paginé des commissions du partenaire.
     */
    public Page<AffiliateCommissionResponse> getMyCommissions(String email,
                                                               Pageable pageable) {
        OurUser user = findUserByEmail(email);
        AffiliateProfile profile = findProfileByUser(user.getId());
        return commissionRepo
            .findByAffiliateIdOrderByCreatedAtDesc(profile.getId(), pageable)
            .map(affiliateMapper::toCommissionResponse);
    }

    /**
     * Historique des ordres de paiement du partenaire.
     */
    public List<AffiliatePaymentOrder> getMyPaymentOrders(String email) {
        OurUser user = findUserByEmail(email);
        AffiliateProfile profile = findProfileByUser(user.getId());
        return paymentOrderRepo.findByAffiliateIdOrderByCreatedAtDesc(profile.getId());
    }

    /**
     * Enregistre un click sur le lien de parrainage.
     * Déduplication : un même IP ne compte qu'une fois par 24h.
     */
    @Transactional
    public void trackClick(String referralCode, String ipAddress, String userAgent) {
        profileRepo.findByReferralCode(referralCode).ifPresent(profile -> {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            boolean alreadyClicked = clickRepo.existsRecentClick(
                profile.getId(), ipAddress, since
            );
            if (!alreadyClicked) {
                clickRepo.save(AffiliateClick.builder()
                    .affiliate(profile)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build());
                log.debug("Click tracé — code={} ip={}", referralCode, ipAddress);
            }
        });
    }

    // ── OWNER — Gestion des partenaires ──────────────────────────────────────

    /**
     * Liste tous les partenaires actifs.
     */
    public List<AffiliateStatsResponse> getAllPartners() {
        return profileRepo.findActiveOrderByBalanceDesc()
            .stream()
            .map(p -> {
                AffiliateStatsResponse s = affiliateMapper.toStatsResponse(p);
                s.setTotalClicks(clickRepo.countByAffiliateId(p.getId()));
                return s;
            })
            .toList();
    }

    /**
     * Activer ou désactiver un partenaire.
     */
    @Transactional
    public void togglePartnerActive(Long profileId, boolean active) {
        AffiliateProfile profile = profileRepo.findById(profileId)
            .orElseThrow(() -> OtakuException.notFound("Profil affilié", profileId));
        profile.setActive(active);
        profileRepo.save(profile);
        log.info("{} partenaire — profileId={}",
            active ? "Activation" : "Désactivation", profileId);
    }

    /**
     * Modifier le taux de commission d'un partenaire.
     */
    @Transactional
    public AffiliateStatsResponse updateCommissionRate(Long profileId,
                                                        BigDecimal newRate) {
        if (newRate.compareTo(BigDecimal.ZERO) <= 0
            || newRate.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw OtakuException.badRequest("Le taux doit être entre 0 et 100%.");
        }

        AffiliateProfile profile = profileRepo.findById(profileId)
            .orElseThrow(() -> OtakuException.notFound("Profil affilié", profileId));

        profile.setCommissionPct(newRate);
        profileRepo.save(profile);

        log.info("Taux commission mis à jour → profileId={} taux={}%", profileId, newRate);
        return affiliateMapper.toStatsResponse(profile);
    }

    // ── OWNER — Validation des commissions ───────────────────────────────────

    /**
     * Confirme toutes les commissions PENDING d'un partenaire
     * et crée un AffiliatePaymentOrder groupant la période.
     */
    @Transactional
    public AffiliatePaymentOrder createPaymentOrder(AffiliatePaymentRequest req) {
        AffiliateProfile profile = profileRepo.findById(req.getAffiliateId())
            .orElseThrow(() -> OtakuException.notFound("Profil affilié", req.getAffiliateId()));

        // Vérifier chevauchement de période
        if (paymentOrderRepo.existsOverlapForAffiliate(
                profile.getId(), req.getPeriodStart(), req.getPeriodEnd())) {
            throw OtakuException.conflict(
                "Un ordre de paiement existe déjà pour cette période."
            );
        }

        // Récupérer les commissions CONFIRMED non encore payées
        List<AffiliateCommission> unpaid =
            commissionRepo.findUnpaidConfirmed(profile.getId());

        if (unpaid.isEmpty()) {
            throw OtakuException.badRequest(
                "Aucune commission confirmée en attente de paiement."
            );
        }

        BigDecimal total = unpaid.stream()
            .map(AffiliateCommission::getCommissionAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        AffiliatePaymentOrder order = AffiliatePaymentOrder.builder()
            .affiliate(profile)
            .periodStart(req.getPeriodStart())
            .periodEnd(req.getPeriodEnd())
            .totalAmount(total)
            .status(PaymentOrderStatus.PENDING)
            .paymentMethod(req.getPaymentMethod())
            .build();

        AffiliatePaymentOrder saved = paymentOrderRepo.save(order);

        // Lier les commissions à cet ordre
        unpaid.forEach(c -> {
            c.setPaymentOrder(saved);
            commissionRepo.save(c);
        });

        // Mettre à jour le solde du partenaire
        profile.setBalance(profile.getBalance().add(total));
        profile.setTotalEarned(profile.getTotalEarned().add(total));
        profileRepo.save(profile);

        log.info("PaymentOrder créé — affiliateId={} total={} commissions={}",
            profile.getId(), total, unpaid.size());
        return saved;
    }

    /**
     * Upload la preuve de virement et valide le paiement.
     */
    @Transactional
    public AffiliatePaymentOrder validatePayment(Long paymentOrderId,
                                                  MultipartFile proof) throws IOException {
        AffiliatePaymentOrder order = paymentOrderRepo.findById(paymentOrderId)
            .orElseThrow(() -> OtakuException.notFound("Ordre de paiement", paymentOrderId));

        if (order.getStatus() == PaymentOrderStatus.PAID) {
            throw OtakuException.conflict("Cet ordre est déjà marqué comme payé.");
        }

        // Upload preuve de virement sur Cloudinary
        if (proof != null && !proof.isEmpty()) {
            var result = cloudinaryService.uploadAffiliateProof(proof, paymentOrderId);
            if (result.isUploaded()) {
                order.setPaymentProofUrl(result.url());
            }
        }

        order.setStatus(PaymentOrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());

        // Marquer les commissions liées comme PAID
        order.getCommissions().forEach(c -> {
            c.setStatus(CommissionStatus.PAID);
            c.setConfirmedAt(LocalDateTime.now());
            commissionRepo.save(c);
        });

        // Déduire du solde du partenaire
        AffiliateProfile profile = order.getAffiliate();
        profile.setBalance(
            profile.getBalance().subtract(order.getTotalAmount())
                   .max(BigDecimal.ZERO)
        );
        profileRepo.save(profile);

        log.info("Paiement validé — paymentOrderId={} affiliateId={}",
            paymentOrderId, profile.getId());
        return paymentOrderRepo.save(order);
    }

    /**
     * Confirme une commission individuelle PENDING → CONFIRMED (Owner).
     */
    @Transactional
    public AffiliateCommissionResponse confirmCommission(Long commissionId) {
        AffiliateCommission commission = commissionRepo.findById(commissionId)
            .orElseThrow(() -> OtakuException.notFound("Commission", commissionId));

        if (commission.getStatus() != CommissionStatus.PENDING) {
            throw OtakuException.conflict(
                "La commission n'est pas en statut PENDING (statut actuel : "
                    + commission.getStatus() + ")."
            );
        }

        commission.setStatus(CommissionStatus.CONFIRMED);
        commission.setConfirmedAt(LocalDateTime.now());

        return affiliateMapper.toCommissionResponse(commissionRepo.save(commission));
    }

    /**
     * Rejette une commission (commande retournée, fraude...).
     */
    @Transactional
    public AffiliateCommissionResponse rejectCommission(Long commissionId) {
        AffiliateCommission commission = commissionRepo.findById(commissionId)
            .orElseThrow(() -> OtakuException.notFound("Commission", commissionId));

        commission.setStatus(CommissionStatus.REJECTED);
        return affiliateMapper.toCommissionResponse(commissionRepo.save(commission));
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private OurUser findUserByEmail(String email) {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> OtakuException.notFound("Utilisateur (" + email + ")", 0L));
    }

    private AffiliateProfile findProfileByUser(Long userId) {
        return profileRepo.findByUserId(userId)
            .orElseThrow(() -> OtakuException.badRequest(
                "Aucun profil affilié trouvé pour cet utilisateur."
            ));
    }
}
