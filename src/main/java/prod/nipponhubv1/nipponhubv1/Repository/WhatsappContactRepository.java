package prod.nipponhubv1.nipponhubv1.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import prod.nipponhubv1.nipponhubv1.Models.WhatsappContact;

@Repository
public interface WhatsappContactRepository extends JpaRepository<WhatsappContact, Long> {

    // Tous les numéros d'une ville
    List<WhatsappContact> findByCityId(Long cityId);

    // Tous les numéros actifs d'une ville (pour contacter le bon agent)
    List<WhatsappContact> findByCityIdAndActiveTrue(Long cityId);

    // Tous les numéros actifs d'un pays (via JOIN)
    @Query("""
        SELECT w FROM WhatsappContact w
        JOIN w.city c
        WHERE c.country.id = :countryId
        AND w.active = true
        """)
    List<WhatsappContact> findActiveByCountryId(@Param("countryId") Long countryId);
}
