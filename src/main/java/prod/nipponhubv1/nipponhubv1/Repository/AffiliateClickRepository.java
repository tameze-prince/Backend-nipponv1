package prod.nipponhubv1.nipponhubv1.Repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import prod.nipponhubv1.nipponhubv1.Models.AffiliateClick;

@Repository
public interface AffiliateClickRepository extends JpaRepository<AffiliateClick, Long> {

    long countByAffiliateId(Long affiliateId);

    // Clics d'un partenaire sur une période (stats)
    @Query("""
        SELECT COUNT(ac) FROM AffiliateClick ac
        WHERE ac.affiliate.id = :affiliateId
        AND ac.clickedAt BETWEEN :from AND :to
        """)
    long countByAffiliateAndPeriod(
        @Param("affiliateId") Long affiliateId,
        @Param("from")        LocalDateTime from,
        @Param("to")          LocalDateTime to
    );

    // Éviter les clics dupliqués (même IP dans les 24h)
    @Query("""
        SELECT COUNT(ac) > 0 FROM AffiliateClick ac
        WHERE ac.affiliate.id = :affiliateId
        AND ac.ipAddress = :ip
        AND ac.clickedAt >= :since
        """)
    boolean existsRecentClick(
        @Param("affiliateId") Long affiliateId,
        @Param("ip")          String ip,
        @Param("since")       LocalDateTime since
    );
}
