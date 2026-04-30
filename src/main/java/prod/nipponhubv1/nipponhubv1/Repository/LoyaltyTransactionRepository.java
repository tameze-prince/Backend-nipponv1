package prod.nipponhubv1.nipponhubv1.Repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import prod.nipponhubv1.nipponhubv1.Models.LoyaltyTransaction;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {

    // Historique des transactions d'un compte
    Page<LoyaltyTransaction> findByLoyaltyAccountIdOrderByCreatedAtDesc(
        Long loyaltyAccountId, Pageable pageable
    );

    // Total de points gagnés sur une période
    @Query("""
        SELECT COALESCE(SUM(lt.points), 0)
        FROM LoyaltyTransaction lt
        WHERE lt.loyaltyAccount.id = :accountId
        AND lt.transactionType = 'EARN'
        AND lt.createdAt BETWEEN :from AND :to
        """)
    int sumEarnedPointsInPeriod(
        @Param("accountId") Long accountId,
        @Param("from")      LocalDateTime from,
        @Param("to")        LocalDateTime to
    );
}
