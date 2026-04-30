package prod.nipponhubv1.nipponhubv1.Services;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import prod.nipponhubv1.nipponhubv1.Dto.CountryRequest;
import prod.nipponhubv1.nipponhubv1.Dto.CountryResponse;
import prod.nipponhubv1.nipponhubv1.Exception.OtakuException;
import prod.nipponhubv1.nipponhubv1.Models.Country;
import prod.nipponhubv1.nipponhubv1.Repository.CountryRepository;

/**
 * Service de gestion des pays : devise, activation, etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CountryService {

    private final CountryRepository countryRepo;

    @Transactional(readOnly = true)
    public List<CountryResponse> getAllCountries() {
        return countryRepo.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CountryResponse> getActiveCountries() {
        return countryRepo.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CountryResponse getCountryById(Long id) {
        Country country = countryRepo.findById(id)
                .orElseThrow(() -> OtakuException.notFound("Pays", id));
        return toResponse(country);
    }

    @Transactional
    public CountryResponse updateCurrency(Long countryId, CountryRequest request) {
        Country country = countryRepo.findById(countryId)
                .orElseThrow(() -> OtakuException.notFound("Pays", countryId));
        
        if (request.getName() != null && !request.getName().isBlank()) {
            country.setName(request.getName());
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            country.setCurrency(request.getCurrency());
        }
        if (request.getCode() != null && !request.getCode().isBlank()) {
            if (!country.getCode().equals(request.getCode()) && 
                countryRepo.existsByCode(request.getCode())) {
                throw OtakuException.conflict("Ce code pays existe déjà");
            }
            country.setCode(request.getCode());
        }
        country.setActive(request.isActive());
        
        Country updated = countryRepo.save(country);
        log.info("Devise du pays {} mise à jour : {}", countryId, request.getCurrency());
        return toResponse(updated);
    }

    @Transactional
    public CountryResponse createCountry(CountryRequest request) {
        if (countryRepo.existsByCode(request.getCode())) {
            throw OtakuException.conflict("Ce code pays existe déjà : " + request.getCode());
        }
        
        Country country = Country.builder()
                .name(request.getName())
                .code(request.getCode())
                .currency(request.getCurrency())
                .active(request.isActive())
                .build();
        
        Country saved = countryRepo.save(country);
        log.info("Pays créé : {} ({})", request.getName(), request.getCode());
        return toResponse(saved);
    }

    private CountryResponse toResponse(Country country) {
        return CountryResponse.builder()
                .id(country.getId())
                .name(country.getName())
                .code(country.getCode())
                .currency(country.getCurrency())
                .active(country.isActive())
                .createdAt(country.getCreatedAt())
                .build();
    }
}
