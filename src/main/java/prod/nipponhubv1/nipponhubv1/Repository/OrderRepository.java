package prod.nipponhubv1.nipponhubv1.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import prod.nipponhubv1.nipponhubv1.Models.Order;
import prod.nipponhubv1.nipponhubv1.Models.Enums.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Historique commandes d'un client
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Commandes par statut (Admin)
    Page<Order> findByCountryIdAndStatusOrderByCreatedAtDesc(
        Long countryId, OrderStatus status, Pageable pageable
    );

    // Toutes les commandes d'un pays (Admin)
    Page<Order> findByCountryIdOrderByCreatedAtDesc(Long countryId, Pageable pageable);

    Page<Order> findByCityIdOrderByCreatedAtDesc(Long cityId, Pageable pageable);

    Page<Order> findByCityIdAndStatusOrderByCreatedAtDesc(
        Long cityId, OrderStatus status, Pageable pageable
    );

    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Commandes à livrer (POS + ONLINE)
    @Query("""
        SELECT o FROM Order o
        WHERE o.country.id = :countryId
        AND o.status IN ('CONFIRMED', 'PROCESSING', 'SHIPPED')
        ORDER BY o.createdAt ASC
        """)
    List<Order> findPendingDeliveryByCountry(@Param("countryId") Long countryId);

    // Chiffre d'affaires par pays sur une période (KPIs Owner)
    @Query("""
        SELECT SUM(o.totalAmount) FROM Order o
        WHERE o.country.id = :countryId
        AND o.status = 'DELIVERED'
        AND o.createdAt BETWEEN :from AND :to
        """)
    Optional<BigDecimal> sumRevenueByCountryAndPeriod(
        @Param("countryId") Long countryId,
        @Param("from")      LocalDateTime from,
        @Param("to")        LocalDateTime to
    );

    // Nombre de commandes par statut (dashboard Admin)
    @Query("""
        SELECT o.status, COUNT(o) FROM Order o
        WHERE o.country.id = :countryId
        GROUP BY o.status
        """)
    List<Object[]> countByStatusForCountry(@Param("countryId") Long countryId);

    // Commandes liées à un partenaire affilié
    Page<Order> findByAffiliateIdOrderByCreatedAtDesc(Long affiliateId, Pageable pageable);

    // Dernière commande d'un client
    Optional<Order> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
