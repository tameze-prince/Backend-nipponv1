package prod.nipponhubv1.nipponhubv1.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import prod.nipponhubv1.nipponhubv1.Models.LoyaltyAccount;

@Repository
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {

    Optional<LoyaltyAccount> findByUserId(Long userId);

    // Lookup par QR code (scan POS)
    Optional<LoyaltyAccount> findByQrCode(String qrCode);

    // Clients avec solde de points (pour campagnes)
    @Query("""
        SELECT la FROM LoyaltyAccount la
        WHERE la.pointsBalance >= :minPoints
        ORDER BY la.pointsBalance DESC
        """)
    List<LoyaltyAccount> findWithMinimumPoints(@Param("minPoints") int minPoints);

    // Top clients par dépenses (KPI Owner)
    @Query("""
        SELECT la FROM LoyaltyAccount la
        JOIN FETCH la.user u
        WHERE u.country.id = :countryId
        ORDER BY la.totalSpent DESC
        """)
    List<LoyaltyAccount> findTopSpendersByCountry(
        @Param("countryId") Long countryId,
        Pageable pageable
    );
}
