package prod.nipponhubv1.nipponhubv1.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import prod.nipponhubv1.nipponhubv1.Models.AffiliatePaymentOrder;
import prod.nipponhubv1.nipponhubv1.Models.Enums.PaymentOrderStatus;

@Repository
public interface AffiliatePaymentOrderRepository extends JpaRepository<AffiliatePaymentOrder, Long> {

    List<AffiliatePaymentOrder> findByAffiliateIdOrderByCreatedAtDesc(Long affiliateId);

    // Ordres de paiement en attente (Owner)
    List<AffiliatePaymentOrder> findByStatus(PaymentOrderStatus status);

    // Vérifier chevauchement de période pour un partenaire
    @Query("""
        SELECT COUNT(apo) > 0 FROM AffiliatePaymentOrder apo
        WHERE apo.affiliate.id = :affiliateId
        AND apo.periodStart <= :periodEnd
        AND apo.periodEnd   >= :periodStart
        """)
    boolean existsOverlapForAffiliate(
        @Param("affiliateId")  Long affiliateId,
        @Param("periodStart")  LocalDate periodStart,
        @Param("periodEnd")    LocalDate periodEnd
    );
}
