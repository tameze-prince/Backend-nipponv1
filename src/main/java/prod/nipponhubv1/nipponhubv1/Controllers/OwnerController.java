package prod.nipponhubv1.nipponhubv1.Controllers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import prod.nipponhubv1.nipponhubv1.Dto.Response.CountryKpiDetailResponse;
import prod.nipponhubv1.nipponhubv1.Dto.Response.ConfigCityResponse;
import prod.nipponhubv1.nipponhubv1.Dto.Response.ConfigWhatsappContactResponse;
import prod.nipponhubv1.nipponhubv1.Dto.Response.GlobalKpiResponse;
import prod.nipponhubv1.nipponhubv1.Dto.Response.OwnerShareCalculation;
import prod.nipponhubv1.nipponhubv1.Models.City;
import prod.nipponhubv1.nipponhubv1.Models.Country;
import prod.nipponhubv1.nipponhubv1.Models.OwnerShare;
import prod.nipponhubv1.nipponhubv1.Models.WhatsappContact;
import prod.nipponhubv1.nipponhubv1.Services.OwnerService;

/**
 * GET    /api/v1/owner/kpis                        → KPIs globaux
 * GET    /api/v1/owner/kpis/{countryId}            → KPIs par pays
 * GET    /api/v1/owner/share/{countryId}           → calcul part réelle
 * GET    /api/v1/config/countries                  → tous les pays
 * POST   /api/v1/config/countries                  → créer pays
 * PATCH  /api/v1/config/countries/{id}/active      → activer/désactiver pays
 * POST   /api/v1/config/countries/{id}/cities      → créer ville
 * PATCH  /api/v1/config/cities/{id}/active         → activer/désactiver ville
 * POST   /api/v1/config/cities/{id}/whatsapp       → ajouter contact WhatsApp
 * DELETE /api/v1/config/whatsapp/{id}              → supprimer contact WhatsApp
 * GET    /api/v1/config/whatsapp?countryId=        → contacts WhatsApp d'un pays
 * PUT    /api/v1/config/owner-shares/{countryId}   → configurer part Owner
 * GET    /api/v1/config/owner-shares               → toutes les parts
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Owner", description = "Dashboard et configuration Owner")
public class OwnerController {

    private final OwnerService ownerService;

    // ── KPIs ─────────────────────────────────────────────────────────────────

    @GetMapping("/api/v1/owner/kpis")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<GlobalKpiResponse> getGlobalKpis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ownerService.getGlobalKpis(from, to));
    }

    @GetMapping("/api/v1/owner/kpis/{countryId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CountryKpiDetailResponse> getCountryKpis(
            @PathVariable Long countryId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ownerService.getCountryKpis(countryId, from, to));
    }

    @GetMapping("/api/v1/owner/share/{countryId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<OwnerShareCalculation> getOwnerShare(
            @PathVariable Long countryId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(
                ownerService.calculateOwnerShare(countryId, from, to));
    }

    // ── Configuration pays ────────────────────────────────────────────────────

    @GetMapping("/api/v1/config/countries")
    public ResponseEntity<List<Country>> getCountries() {
        return ResponseEntity.ok(ownerService.getAllCountries());
    }

    @PostMapping("/api/v1/config/countries")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<Country> createCountry(
            @RequestBody Country country) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ownerService.createCountry(country));
    }

    @PatchMapping("/api/v1/config/countries/{id}/active")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Country> toggleCountry(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(ownerService.toggleCountryActive(id, active));
    }

    // ── Configuration villes ──────────────────────────────────────────────────

    @PostMapping("/api/v1/config/countries/{countryId}/cities")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<ConfigCityResponse> createCity(
            @PathVariable Long countryId,
            @RequestParam String name) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toCityResponse(ownerService.createCity(countryId, name)));
    }

    @GetMapping("/api/v1/config/countries/{countryId}/cities")
    public ResponseEntity<List<ConfigCityResponse>> getCities(@PathVariable Long countryId) {
        return ResponseEntity.ok(ownerService.getCitiesByCountry(countryId)
                .stream()
                .map(this::toCityResponse)
                .toList());
    }

    @PatchMapping("/api/v1/config/cities/{id}/active")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<ConfigCityResponse> toggleCity(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(toCityResponse(ownerService.toggleCityActive(id, active)));
    }

    // ── WhatsApp contacts ────────────────────────────────────────────────────

    @PostMapping("/api/v1/config/cities/{cityId}/whatsapp")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<ConfigWhatsappContactResponse> addWhatsapp(
            @PathVariable Long cityId,
            @RequestParam String number,
            @RequestParam String label) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toWhatsappResponse(ownerService.addWhatsappContact(cityId, number, label)));
    }

    @DeleteMapping("/api/v1/config/whatsapp/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> deleteWhatsapp(@PathVariable Long id) {
        ownerService.deleteWhatsappContact(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/config/whatsapp")
    public ResponseEntity<List<ConfigWhatsappContactResponse>> getWhatsapp(
            @RequestParam Long countryId) {
        return ResponseEntity.ok(ownerService.getWhatsappByCountry(countryId)
                .stream()
                .map(this::toWhatsappResponse)
                .toList());
    }

    // ── Parts Owner ───────────────────────────────────────────────────────────

    @PutMapping("/api/v1/config/owner-shares/{countryId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<OwnerShare> setShare(
            @PathVariable Long countryId,
            @RequestParam BigDecimal sharePct) {
        return ResponseEntity.ok(ownerService.setOwnerShare(countryId, sharePct));
    }

    @GetMapping("/api/v1/config/owner-shares")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<OwnerShare>> getAllShares() {
        return ResponseEntity.ok(ownerService.getAllOwnerShares());
    }

    private ConfigCityResponse toCityResponse(City city) {
        return ConfigCityResponse.builder()
                .id(city.getId())
                .countryId(city.getCountry() != null ? city.getCountry().getId() : null)
                .name(city.getName())
                .active(city.isActive())
                .createdAt(city.getCreatedAt())
                .build();
    }

    private ConfigWhatsappContactResponse toWhatsappResponse(WhatsappContact contact) {
        return ConfigWhatsappContactResponse.builder()
                .id(contact.getId())
                .cityId(contact.getCity() != null ? contact.getCity().getId() : null)
                .whatsappNumber(contact.getWhatsappNumber())
                .label(contact.getLabel())
                .active(contact.isActive())
                .build();
    }
}
