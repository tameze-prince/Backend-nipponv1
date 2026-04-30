package prod.nipponhubv1.nipponhubv1.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import prod.nipponhubv1.nipponhubv1.Models.ProductVariant;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdAndActiveTrue(Long productId);

    List<ProductVariant> findByProductId(Long productId);

    // Variante avec son produit (évite N+1)
    @Query("""
        SELECT v FROM ProductVariant v
        JOIN FETCH v.product p
        WHERE v.id = :id AND v.active = true
        """)
    Optional<ProductVariant> findByIdWithProduct(@Param("id") Long id);
}
