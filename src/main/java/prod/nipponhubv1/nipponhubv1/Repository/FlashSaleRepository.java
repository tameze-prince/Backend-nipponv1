package prod.nipponhubv1.nipponhubv1.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import prod.nipponhubv1.nipponhubv1.Models.FlashSale;

@Repository
public interface FlashSaleRepository extends JpaRepository<FlashSale, Long> {

    // Vente flash active d'un produit en ce moment
    @Query("""
        SELECT fs FROM FlashSale fs
        WHERE fs.product.id = :productId
        AND fs.active = true
        AND fs.startsAt <= :now
        AND fs.endsAt   >= :now
        """)
    Optional<FlashSale> findActiveByProductId(
        @Param("productId") Long productId,
        @Param("now")       LocalDateTime now
    );

    // Toutes les ventes flash actives (page d'accueil)
    @Query("""
        SELECT fs FROM FlashSale fs
        JOIN FETCH fs.product p
        WHERE fs.active = true
        AND fs.startsAt <= :now
        AND fs.endsAt   >= :now
        """)
    List<FlashSale> findAllActive(@Param("now") LocalDateTime now);

    @Query("""
        SELECT fs FROM FlashSale fs
        JOIN FETCH fs.product p
        LEFT JOIN FETCH fs.createdBy u
        WHERE fs.active = true
        ORDER BY fs.startsAt DESC
        """)
    List<FlashSale> findAllEnabled();

    // Chevauchement — éviter deux flash sales en même temps sur un produit
    @Query("""
        SELECT COUNT(fs) > 0 FROM FlashSale fs
        WHERE fs.product.id = :productId
        AND fs.active = true
        AND fs.startsAt < :endsAt
        AND fs.endsAt   > :startsAt
        """)
    boolean existsOverlap(
        @Param("productId") Long productId,
        @Param("startsAt")  LocalDateTime startsAt,
        @Param("endsAt")    LocalDateTime endsAt
    );

    @Query("""
        SELECT COUNT(fs) > 0 FROM FlashSale fs
        WHERE fs.product.id = :productId
        AND fs.active = true
        AND fs.id <> :flashSaleId
        AND fs.startsAt < :endsAt
        AND fs.endsAt   > :startsAt
        """)
    boolean existsOverlapExcludingId(
        @Param("flashSaleId") Long flashSaleId,
        @Param("productId")   Long productId,
        @Param("startsAt")    LocalDateTime startsAt,
        @Param("endsAt")      LocalDateTime endsAt
    );
}
