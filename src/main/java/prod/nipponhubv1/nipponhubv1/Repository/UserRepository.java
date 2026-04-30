package prod.nipponhubv1.nipponhubv1.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Models.Enums.Role;

@Repository
public interface UserRepository extends JpaRepository<OurUser, Long> {

    Optional<OurUser> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // Recherche client pour POS (Admin scanne ou tape)
    @Query("""
        SELECT u FROM OurUser u
        WHERE u.role = 'CLIENT'
        AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
        OR u.phone LIKE CONCAT('%', :query, '%')
        OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :query, '%')))
        """)
    List<OurUser> searchClients(@Param("query") String query);

    // Liste clients d'un pays (Admin)
    Page<OurUser> findByRoleAndCountryId(Role role, Long countryId, Pageable pageable);

    @Query("""
        SELECT u FROM OurUser u
        WHERE (:countryId IS NULL OR u.country.id = :countryId)
        AND (:role IS NULL OR u.role = :role)
        """)
    Page<OurUser> findAllByCountryIdAndRole(
        @Param("countryId") Long countryId,
        @Param("role") Role role,
        Pageable pageable
    );

    // Tous les partenaires actifs
    @Query("""
        SELECT u FROM OurUser u
        JOIN AffiliateProfile ap ON ap.user = u
        WHERE ap.active = true
        """)
    List<OurUser> findActivePartners();

    // Tous les utilisateurs d'un rôle spécifique
    List<OurUser> findByRole(Role role);

    // Vérifier si un user existe et est actif
    Optional<OurUser> findByIdAndActiveTrue(Long id);
}
