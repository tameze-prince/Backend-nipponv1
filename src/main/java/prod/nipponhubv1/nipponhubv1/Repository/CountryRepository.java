package prod.nipponhubv1.nipponhubv1.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import prod.nipponhubv1.nipponhubv1.Models.Country;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {

    List<Country> findByActiveTrue();

    Optional<Country> findByCode(String code);

    boolean existsByCode(String code);
}
