package prod.nipponhubv1.nipponhubv1.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import prod.nipponhubv1.nipponhubv1.Models.Stock;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    // Stock d'une variante dans un pays précis
    Optional<Stock> findByVariantIdAndCountryId(Long variantId, Long countryId);

    // Tous les stocks d'un pays (vue Admin)
    @Query("""
        SELECT s FROM Stock s
        JOIN FETCH s.variant v
        JOIN FETCH v.product p
        WHERE s.country.id = :countryId
        ORDER BY p.name ASC
        """)
    List<Stock> findByCountryIdWithDetails(@Param("countryId") Long countryId);

    // Stocks faibles (alerte réapprovisionnement)
    @Query("""
        SELECT s FROM Stock s
        JOIN FETCH s.variant v
        JOIN FETCH v.product p
        WHERE s.country.id = :countryId
        AND s.quantity <= :threshold
        """)
    List<Stock> findLowStockByCountry(
        @Param("countryId") Long countryId,
        @Param("threshold") int threshold
    );

    // Vérifier disponibilité avant commande
    @Query("""
        SELECT s.quantity FROM Stock s
        WHERE s.variant.id = :variantId
        AND s.country.id   = :countryId
        """)
    Optional<Integer> getQuantity(
        @Param("variantId") Long variantId,
        @Param("countryId") Long countryId
    );

    @Query("""
        SELECT COALESCE(SUM(s.quantity), 0) FROM Stock s
        WHERE s.variant.id = :variantId
        """)
    int getTotalQuantityByVariantId(@Param("variantId") Long variantId);
}
