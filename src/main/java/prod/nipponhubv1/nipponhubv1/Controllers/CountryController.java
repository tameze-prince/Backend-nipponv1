package prod.nipponhubv1.nipponhubv1.Controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import prod.nipponhubv1.nipponhubv1.Dto.CountryRequest;
import prod.nipponhubv1.nipponhubv1.Dto.CountryResponse;
import prod.nipponhubv1.nipponhubv1.Services.CountryService;

/**
 * GET    /api/v1/countries           → liste (PUBLIC)
 * GET    /api/v1/countries/active    → liste active (PUBLIC)
 * GET    /api/v1/countries/{id}      → détail (PUBLIC)
 * POST   /api/v1/countries           → créer (ADMIN)
 * PUT    /api/v1/countries/{id}      → modifier devise (ADMIN)
 */
@RestController
@RequestMapping("/api/v1/countries")
@RequiredArgsConstructor
@Tag(name = "Countries", description = "Gestion des pays et devises")
public class CountryController {

    private final CountryService countryService;

    @GetMapping
    @Operation(summary = "Lister tous les pays")
    public ResponseEntity<List<CountryResponse>> getAllCountries() {
        return ResponseEntity.ok(countryService.getAllCountries());
    }

    @GetMapping("/active")
    @Operation(summary = "Lister les pays actifs")
    public ResponseEntity<List<CountryResponse>> getActiveCountries() {
        return ResponseEntity.ok(countryService.getActiveCountries());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un pays par ID")
    public ResponseEntity<CountryResponse> getCountryById(@PathVariable Long id) {
        return ResponseEntity.ok(countryService.getCountryById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un nouveau pays")
    public ResponseEntity<CountryResponse> createCountry(@Valid @RequestBody CountryRequest request) {
        CountryResponse response = countryService.createCountry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier la devise d'un pays")
    public ResponseEntity<CountryResponse> updateCurrency(
            @PathVariable Long id,
            @Valid @RequestBody CountryRequest request) {
        CountryResponse response = countryService.updateCurrency(id, request);
        return ResponseEntity.ok(response);
    }
}
