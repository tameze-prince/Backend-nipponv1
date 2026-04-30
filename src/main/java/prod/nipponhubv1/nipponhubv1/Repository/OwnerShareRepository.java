package prod.nipponhubv1.nipponhubv1.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import prod.nipponhubv1.nipponhubv1.Models.OwnerShare;

@Repository
public interface OwnerShareRepository extends JpaRepository<OwnerShare, Long> {

    Optional<OwnerShare> findByCountryId(Long countryId);

    List<OwnerShare> findAll();
}
