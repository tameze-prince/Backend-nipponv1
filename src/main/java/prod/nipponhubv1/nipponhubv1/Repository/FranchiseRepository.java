package prod.nipponhubv1.nipponhubv1.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import prod.nipponhubv1.nipponhubv1.Models.Franchise;

@Repository
public interface FranchiseRepository extends JpaRepository<Franchise, Long> {

    List<Franchise> findByActiveTrue();

    Optional<Franchise> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
