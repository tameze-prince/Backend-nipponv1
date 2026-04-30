package prod.nipponhubv1.nipponhubv1.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import prod.nipponhubv1.nipponhubv1.Models.LoyaltyGrade;

@Repository
public interface LoyaltyGradeRepository extends JpaRepository<LoyaltyGrade, Long> {

    // Trouver le grade correspondant au montant total dépensé
    @Query("""
        SELECT g FROM LoyaltyGrade g
        WHERE g.minTotalSpent <= :totalSpent
        ORDER BY g.minTotalSpent DESC
        LIMIT 1
        """)
    Optional<LoyaltyGrade> findGradeForSpent(@Param("totalSpent") BigDecimal totalSpent);

    List<LoyaltyGrade> findAllByOrderByMinTotalSpentAsc();
}
