package prod.nipponhubv1.nipponhubv1.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import prod.nipponhubv1.nipponhubv1.Models.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    // Produits les plus vendus dans un pays
    @Query("""
        SELECT oi.variant.product.id,
               oi.variant.product.name,
               SUM(oi.quantity) AS totalSold
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.country.id = :countryId
        AND o.status = 'DELIVERED'
        AND o.createdAt BETWEEN :from AND :to
        GROUP BY oi.variant.product.id, oi.variant.product.name
        ORDER BY totalSold DESC
        """)
    List<Object[]> findTopProductsByCountry(
        @Param("countryId") Long countryId,
        @Param("from")      LocalDateTime from,
        @Param("to")        LocalDateTime to,
        Pageable pageable
    );
}
