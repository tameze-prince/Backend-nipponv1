package prod.nipponhubv1.nipponhubv1.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import prod.nipponhubv1.nipponhubv1.Models.StockMovement;
import prod.nipponhubv1.nipponhubv1.Models.Enums.MovementType;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    // Journal d'une variante dans un pays (trié du plus récent)
    List<StockMovement> findByVariantIdAndCountryIdOrderByCreatedAtDesc(
        Long variantId, Long countryId
    );

    // Mouvements d'un admin
    Page<StockMovement> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Mouvements par type sur une période
    @Query("""
        SELECT sm FROM StockMovement sm
        WHERE sm.country.id     = :countryId
        AND sm.movementType     = :type
        AND sm.createdAt BETWEEN :from AND :to
        ORDER BY sm.createdAt DESC
        """)
    List<StockMovement> findByCountryAndTypeAndPeriod(
        @Param("countryId") Long countryId,
        @Param("type")      MovementType type,
        @Param("from")      LocalDateTime from,
        @Param("to")        LocalDateTime to
    );
}
