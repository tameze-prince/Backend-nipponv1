package prod.nipponhubv1.nipponhubv1.Services;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import prod.nipponhubv1.nipponhubv1.Dto.LoyaltyAccountResponse;
import prod.nipponhubv1.nipponhubv1.Exception.OtakuException;
import prod.nipponhubv1.nipponhubv1.Mappers.LoyaltyMapper;
import prod.nipponhubv1.nipponhubv1.Models.LoyaltyAccount;
import prod.nipponhubv1.nipponhubv1.Models.LoyaltyGrade;
import prod.nipponhubv1.nipponhubv1.Models.LoyaltyTransaction;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Models.Enums.TransactionType;
import prod.nipponhubv1.nipponhubv1.Repository.LoyaltyAccountRepository;
import prod.nipponhubv1.nipponhubv1.Repository.LoyaltyGradeRepository;
import prod.nipponhubv1.nipponhubv1.Repository.LoyaltyTransactionRepository;
import prod.nipponhubv1.nipponhubv1.Repository.UserRepository;

/**
 * Gestion du programme de fidélité "Nippon Pass".
 *
 * Responsabilités :
 *  - Consulter son solde / grade / historique (CLIENT)
 *  - Générer / régénérer le QR code Nippon Pass
 *  - Attribution manuelle de points bonus (ADMIN/OWNER)
 *  - Recalcul du grade après chaque opération
 *  - Statistiques fidélité (ADMIN/OWNER)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyService {

    private final LoyaltyAccountRepository     loyaltyRepo;
    private final LoyaltyGradeRepository       gradeRepo;
    private final LoyaltyTransactionRepository txRepo;
    private final UserRepository               userRepo;
    private final LoyaltyMapper                loyaltyMapper;

    // ── CLIENT — consulter son compte ────────────────────────────────────────

    /**
     * Retourne le compte fidélité complet du client connecté.
     * Inclut grade actuel, points, QR code (Nippon Pass).
     */
    public LoyaltyAccountResponse getMyAccount(String email) {
        OurUser user = findUserByEmail(email);
        LoyaltyAccount account = findAccountByUser(user.getId());
        return loyaltyMapper.toResponse(account);
    }

    /**
     * Historique paginé des transactions de points.
     */
    public Page<LoyaltyTransaction> getMyHistory(String email, Pageable pageable) {
        OurUser user = findUserByEmail(email);
        LoyaltyAccount account = findAccountByUser(user.getId());
        return txRepo.findByLoyaltyAccountIdOrderByCreatedAtDesc(
            account.getId(), pageable
        );
    }

    /**
     * Régénère le QR code du Nippon Pass.
     * Utile si le client pense que son QR code a été compromis.
     */
    @Transactional
    public LoyaltyAccountResponse regenerateQrCode(String email) {
        OurUser user = findUserByEmail(email);
        LoyaltyAccount account = findAccountByUser(user.getId());

        account.setQrCode(generateQrCode(user.getId()));
        LoyaltyAccount saved = loyaltyRepo.save(account);

        log.info("QR code régénéré — userId={}", user.getId());
        return loyaltyMapper.toResponse(saved);
    }

    // ── ADMIN / OWNER — gestion ───────────────────────────────────────────────

    /**
     * Attribution manuelle de points bonus à un client (ADMIN/OWNER).
     * Exemple : compensation suite à un problème de commande.
     */
    @Transactional
    public LoyaltyAccountResponse grantBonusPoints(Long userId,
                                                    int points,
                                                    String reason,
                                                    OurUser admin) {
        if (points <= 0) {
            throw OtakuException.badRequest("Le nombre de points doit être positif.");
        }

        LoyaltyAccount account = findAccountByUser(userId);
        account.setPointsBalance(account.getPointsBalance() + points);

        // Recalculer le grade
        recalculateGrade(account);
        LoyaltyAccount saved = loyaltyRepo.save(account);

        txRepo.save(LoyaltyTransaction.builder()
            .loyaltyAccount(saved)
            .transactionType(TransactionType.BONUS)
            .points(points)
            .description("Bonus manuel : " + reason + " (par " + admin.getEmail() + ")")
            .build());

        log.info("Bonus {} points → userId={} par admin={}",
            points, userId, admin.getEmail());
        return loyaltyMapper.toResponse(saved);
    }

    /**
     * Consulter le compte fidélité d'un client spécifique (Admin/POS).
     */
    public LoyaltyAccountResponse getAccountByUserId(Long userId) {
        LoyaltyAccount account = findAccountByUser(userId);
        return loyaltyMapper.toResponse(account);
    }

    /**
     * Lookup par QR code — utilisé au POS pour identifier le client.
     */
    public LoyaltyAccountResponse getByQrCode(String qrCode) {
        LoyaltyAccount account = loyaltyRepo.findByQrCode(qrCode)
            .orElseThrow(() -> OtakuException.notFound(
                "Compte fidélité (QR=" + qrCode + ")", 0L
            ));
        return loyaltyMapper.toResponse(account);
    }

    /**
     * Top clients par dépenses dans un pays (KPI Owner/Admin).
     */
    public List<LoyaltyAccountResponse> getTopSpenders(Long countryId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("totalSpent").descending());
        return loyaltyRepo.findTopSpendersByCountry(countryId, pageable)
            .stream()
            .map(loyaltyMapper::toResponse)
            .toList();
    }

    /**
     * Liste tous les grades disponibles (affiché sur la page Nippon Pass).
     */
    public List<LoyaltyGrade> getAllGrades() {
        return gradeRepo.findAllByOrderByMinTotalSpentAsc();
    }

    // ── CRUD Grades (OWNER) ───────────────────────────────────────────────────

    @Transactional
    public LoyaltyGrade createGrade(LoyaltyGrade grade) {
        return gradeRepo.save(grade);
    }

    @Transactional
    public LoyaltyGrade updateGrade(Long id, LoyaltyGrade update) {
        LoyaltyGrade existing = gradeRepo.findById(id)
            .orElseThrow(() -> OtakuException.notFound("Grade", id));

        if (update.getName() != null)         existing.setName(update.getName());
        if (update.getMinTotalSpent() != null) existing.setMinTotalSpent(update.getMinTotalSpent());
        if (update.getColorHex() != null)     existing.setColorHex(update.getColorHex());
        if (update.getBadgeImageUrl() != null) existing.setBadgeImageUrl(update.getBadgeImageUrl());

        return gradeRepo.save(existing);
    }

    // ── Utilitaires internes ──────────────────────────────────────────────────

    /**
     * Recalcule et met à jour le grade d'un compte selon son totalSpent.
     * Appelé après chaque crédit/débit de points ou commande livrée.
     */
    public void recalculateGrade(LoyaltyAccount account) {
        gradeRepo.findGradeForSpent(account.getTotalSpent())
            .ifPresent(grade -> {
                if (account.getGrade() == null
                    || !account.getGrade().getId().equals(grade.getId())) {
                    account.setGrade(grade);
                    log.info("Grade mis à jour → userId={} grade={}",
                        account.getUser().getId(), grade.getName());
                }
            });
    }

    private OurUser findUserByEmail(String email) {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> OtakuException.notFound("Utilisateur (" + email + ")", 0L));
    }

    private LoyaltyAccount findAccountByUser(Long userId) {
        return loyaltyRepo.findByUserId(userId)
            .orElseThrow(() -> OtakuException.notFound(
                "Compte fidélité (userId=" + userId + ")", 0L
            ));
    }

    private String generateQrCode(Long userId) {
        return "OTAKU-" + userId + "-"
            + UUID.randomUUID().toString().replace("-", "")
                  .substring(0, 8).toUpperCase();
    }
}
