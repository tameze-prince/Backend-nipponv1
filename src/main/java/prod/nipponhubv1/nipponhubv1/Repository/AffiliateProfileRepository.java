package prod.nipponhubv1.nipponhubv1.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import prod.nipponhubv1.nipponhubv1.Models.AffiliateProfile;

@Repository
public interface AffiliateProfileRepository extends JpaRepository<AffiliateProfile, Long> {

    Optional<AffiliateProfile> findByUserId(Long userId);

    Optional<AffiliateProfile> findByReferralCode(String referralCode);

    boolean existsByReferralCode(String referralCode);

    // Tous les partenaires actifs (Owner)
    List<AffiliateProfile> findByActiveTrue();

    // Partenaires par solde décroissant (Owner)
    @Query("""
        SELECT ap FROM AffiliateProfile ap
        JOIN FETCH ap.user u
        WHERE ap.active = true
        ORDER BY ap.balance DESC
        """)
    List<AffiliateProfile> findActiveOrderByBalanceDesc();
}
