package prod.nipponhubv1.nipponhubv1.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import prod.nipponhubv1.nipponhubv1.Models.AffiliateCommission;
import prod.nipponhubv1.nipponhubv1.Models.Enums.CommissionStatus;

@Repository
public interface AffiliateCommissionRepository extends JpaRepository<AffiliateCommission, Long> {

    // Commissions d'un partenaire
    Page<AffiliateCommission> findByAffiliateIdOrderByCreatedAtDesc(
        Long affiliateId, Pageable pageable
    );

    // Commissions en attente (Owner valide)
    List<AffiliateCommission> findByAffiliateIdAndStatus(
        Long affiliateId, CommissionStatus status
    );

    // Toutes les commissions PENDING (Owner)
    List<AffiliateCommission> findByStatus(CommissionStatus status);

    // Total gagné par un partenaire sur une période
    @Query("""
        SELECT COALESCE(SUM(ac.commissionAmount), 0)
        FROM AffiliateCommission ac
        WHERE ac.affiliate.id = :affiliateId
        AND ac.status = 'CONFIRMED'
        AND ac.createdAt BETWEEN :from AND :to
        """)
    BigDecimal sumConfirmedCommissions(
        @Param("affiliateId") Long affiliateId,
        @Param("from")        LocalDateTime from,
        @Param("to")          LocalDateTime to
    );

    // Commissions d'un partenaire sans ordre de paiement (non encore groupées)
    @Query("""
        SELECT ac FROM AffiliateCommission ac
        WHERE ac.affiliate.id = :affiliateId
        AND ac.status = 'CONFIRMED'
        AND ac.paymentOrder IS NULL
        """)
    List<AffiliateCommission> findUnpaidConfirmed(@Param("affiliateId") Long affiliateId);
}
