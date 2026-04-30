package prod.nipponhubv1.nipponhubv1.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import prod.nipponhubv1.nipponhubv1.Models.Wishlist;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByUserId(Long userId);

    Optional<Wishlist> findByUserIdAndVariantId(Long userId, Long variantId);

    boolean existsByUserIdAndVariantId(Long userId, Long variantId);

    void deleteByUserIdAndVariantId(Long userId, Long variantId);

    // Tous les clients qui ont mis en wishlist une variante (pour notif restockage)
    @Query("""
        SELECT w FROM Wishlist w
        JOIN FETCH w.user u
        WHERE w.variant.id = :variantId
        """)
    List<Wishlist> findByVariantIdWithUsers(@Param("variantId") Long variantId);
}
