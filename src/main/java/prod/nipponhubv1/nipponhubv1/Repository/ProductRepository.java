package prod.nipponhubv1.nipponhubv1.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import prod.nipponhubv1.nipponhubv1.Models.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    @Query("""
        SELECT p FROM Product p
        LEFT JOIN FETCH p.category c
        LEFT JOIN FETCH p.franchise f
        LEFT JOIN FETCH p.variants v
        LEFT JOIN FETCH p.images i
        WHERE p.slug = :slug
        """)
    Optional<Product> findBySlugWithAssociations(@Param("slug") String slug);

    boolean existsBySlug(String slug);

    // Catalogue filtré (catégorie + franchise) avec pagination
    @Query(value = """
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.category c
        LEFT JOIN FETCH p.franchise f
        LEFT JOIN FETCH p.variants v
        LEFT JOIN FETCH p.images i
        WHERE p.active = true
        AND (:categoryId  IS NULL OR c.id = :categoryId)
        AND (:franchiseId IS NULL OR f.id = :franchiseId)
        """,
        countQuery = """
        SELECT COUNT(DISTINCT p) FROM Product p
        LEFT JOIN p.category c
        LEFT JOIN p.franchise f
        WHERE p.active = true
        AND (:categoryId  IS NULL OR c.id = :categoryId)
        AND (:franchiseId IS NULL OR f.id = :franchiseId)
        """)
    Page<Product> findActiveFiltered(
        @Param("categoryId")  Long categoryId,
        @Param("franchiseId") Long franchiseId,
        Pageable pageable
    );

    // Recherche full-text (nom + description)
    @Query(value = """
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.category c
        LEFT JOIN FETCH p.franchise f
        LEFT JOIN FETCH p.variants v
        LEFT JOIN FETCH p.images i
        WHERE p.active = true
        AND (LOWER(p.name)        LIKE LOWER(CONCAT('%', :q, '%'))
        OR   LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))
        """,
        countQuery = """
        SELECT COUNT(DISTINCT p) FROM Product p
        LEFT JOIN p.category c
        LEFT JOIN p.franchise f
        WHERE p.active = true
        AND (LOWER(p.name)        LIKE LOWER(CONCAT('%', :q, '%'))
        OR   LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Product> searchByKeyword(@Param("q") String q, Pageable pageable);

    // Produits en vente flash active en ce moment
    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.category c
        LEFT JOIN FETCH p.franchise f
        LEFT JOIN FETCH p.variants v
        LEFT JOIN FETCH p.images i
        JOIN p.flashSales fs
        WHERE p.active = true
        AND fs.active = true
        AND fs.startsAt <= :now
        AND fs.endsAt   >= :now
        """)
    List<Product> findActiveFlashSaleProducts(@Param("now") LocalDateTime now);

    // Produits d'une franchise
    List<Product> findByFranchiseIdAndActiveTrue(Long franchiseId);

    // Produits d'une catégorie
    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);
}
