package prod.nipponhubv1.nipponhubv1.Services;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import prod.nipponhubv1.nipponhubv1.Dto.CityRequest;
import prod.nipponhubv1.nipponhubv1.Dto.CityResponse;
import prod.nipponhubv1.nipponhubv1.Dto.WhatsappContactRequest;
import prod.nipponhubv1.nipponhubv1.Dto.WhatsappContactResponse;
import prod.nipponhubv1.nipponhubv1.Exception.OtakuException;
import prod.nipponhubv1.nipponhubv1.Models.City;
import prod.nipponhubv1.nipponhubv1.Models.Country;
import prod.nipponhubv1.nipponhubv1.Models.WhatsappContact;
import prod.nipponhubv1.nipponhubv1.Repository.CityRepository;
import prod.nipponhubv1.nipponhubv1.Repository.CountryRepository;
import prod.nipponhubv1.nipponhubv1.Repository.WhatsappContactRepository;

/**
 * Service de gestion des villes et contacts WhatsApp par ville
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CityService {

    private final CityRepository cityRepo;
    private final CountryRepository countryRepo;
    private final WhatsappContactRepository whatsappRepo;

    // ── Villes ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CityResponse> getCitiesByCountry(Long countryId) {
        if (!countryRepo.existsById(countryId)) {
            throw OtakuException.notFound("Pays", countryId);
        }
        return cityRepo.findByCountryId(countryId)
                .stream()
                .map(this::toCityResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CityResponse getCityById(Long id) {
        City city = cityRepo.findById(id)
                .orElseThrow(() -> OtakuException.notFound("Ville", id));
        return toCityResponse(city);
    }

    @Transactional
    public CityResponse createCity(CityRequest request) {
        Country country = countryRepo.findById(request.getCountryId())
                .orElseThrow(() -> OtakuException.notFound("Pays", request.getCountryId()));
        
        City city = City.builder()
                .country(country)
                .name(request.getName())
                .active(request.isActive())
                .build();
        
        City saved = cityRepo.save(city);
        log.info("Ville créée : {} dans le pays {}", request.getName(), country.getName());
        return toCityResponse(saved);
    }

    @Transactional
    public CityResponse updateCity(Long cityId, CityRequest request) {
        City city = cityRepo.findById(cityId)
                .orElseThrow(() -> OtakuException.notFound("Ville", cityId));
        
        if (request.getName() != null && !request.getName().isBlank()) {
            city.setName(request.getName());
        }
        city.setActive(request.isActive());
        
        City updated = cityRepo.save(city);
        log.info("Ville mise à jour : {}", cityId);
        return toCityResponse(updated);
    }

    // ── Contacts WhatsApp par ville ────────────────────────────────

    @Transactional(readOnly = true)
    public List<WhatsappContactResponse> getWhatsappContacts(Long cityId) {
        if (!cityRepo.existsById(cityId)) {
            throw OtakuException.notFound("Ville", cityId);
        }
        return whatsappRepo.findByCityId(cityId)
                .stream()
                .map(this::toWhatsappResponse)
                .toList();
    }

    @Transactional
    public WhatsappContactResponse addWhatsappContact(WhatsappContactRequest request) {
        City city = cityRepo.findById(request.getCityId())
                .orElseThrow(() -> OtakuException.notFound("Ville", request.getCityId()));
        
        WhatsappContact contact = WhatsappContact.builder()
                .city(city)
                .whatsappNumber(request.getWhatsappNumber())
                .label(request.getLabel() != null ? request.getLabel() : "Support")
                .active(request.isActive())
                .build();
        
        WhatsappContact saved = whatsappRepo.save(contact);
        log.info("Contact WhatsApp ajouté : {} pour la ville {}", 
                request.getWhatsappNumber(), city.getName());
        return toWhatsappResponse(saved);
    }

    @Transactional
    public WhatsappContactResponse updateWhatsappContact(Long contactId, 
                                                         WhatsappContactRequest request) {
        WhatsappContact contact = whatsappRepo.findById(contactId)
                .orElseThrow(() -> OtakuException.notFound("Contact WhatsApp", contactId));
        
        if (request.getWhatsappNumber() != null && !request.getWhatsappNumber().isBlank()) {
            contact.setWhatsappNumber(request.getWhatsappNumber());
        }
        if (request.getLabel() != null && !request.getLabel().isBlank()) {
            contact.setLabel(request.getLabel());
        }
        contact.setActive(request.isActive());
        
        WhatsappContact updated = whatsappRepo.save(contact);
        log.info("Contact WhatsApp mis à jour : {}", contactId);
        return toWhatsappResponse(updated);
    }

    @Transactional
    public void deleteWhatsappContact(Long contactId) {
        WhatsappContact contact = whatsappRepo.findById(contactId)
                .orElseThrow(() -> OtakuException.notFound("Contact WhatsApp", contactId));
        
        whatsappRepo.delete(contact);
        log.info("Contact WhatsApp supprimé : {}", contactId);
    }

    // ── Mappers ────────────────────────────────────────────────────

    private CityResponse toCityResponse(City city) {
        List<WhatsappContactResponse> whatsappContacts = city.getWhatsappContacts()
                .stream()
                .map(this::toWhatsappResponse)
                .toList();
        
        return CityResponse.builder()
                .id(city.getId())
                .countryId(city.getCountry().getId())
                .countryName(city.getCountry().getName())
                .name(city.getName())
                .active(city.isActive())
                .createdAt(city.getCreatedAt())
                .whatsappContacts(whatsappContacts)
                .build();
    }

    private WhatsappContactResponse toWhatsappResponse(WhatsappContact contact) {
        return WhatsappContactResponse.builder()
                .id(contact.getId())
                .cityId(contact.getCity().getId())
                .cityName(contact.getCity().getName())
                .whatsappNumber(contact.getWhatsappNumber())
                .label(contact.getLabel())
                .active(contact.isActive())
                .build();
    }
}
