package prod.nipponhubv1.nipponhubv1.Controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import prod.nipponhubv1.nipponhubv1.Dto.CityRequest;
import prod.nipponhubv1.nipponhubv1.Dto.CityResponse;
import prod.nipponhubv1.nipponhubv1.Dto.WhatsappContactRequest;
import prod.nipponhubv1.nipponhubv1.Dto.WhatsappContactResponse;
import prod.nipponhubv1.nipponhubv1.Services.CityService;

/**
 * Gestion des villes et leurs contacts WhatsApp
 * 
 * GET    /api/v1/cities/by-country/{countryId}        → villes du pays (PUBLIC)
 * GET    /api/v1/cities/{id}                           → détail ville (PUBLIC)
 * POST   /api/v1/cities                                → créer ville (ADMIN/OWNER)
 * PUT    /api/v1/cities/{id}                           → modifier ville (ADMIN/OWNER)
 * 
 * GET    /api/v1/cities/{cityId}/whatsapp             → contacts WhatsApp (PUBLIC)
 * POST   /api/v1/cities/{cityId}/whatsapp             → ajouter contact (ADMIN/OWNER)
 * PUT    /api/v1/cities/whatsapp/{contactId}          → modifier contact (ADMIN/OWNER)
 * DELETE /api/v1/cities/whatsapp/{contactId}          → supprimer contact (ADMIN/OWNER)
 */
@RestController
@RequestMapping("/api/v1/cities")
@RequiredArgsConstructor
@Tag(name = "Cities", description = "Gestion des villes et contacts WhatsApp")
public class CityController {

    private final CityService cityService;

    // ── Villes ────────────────────────────────────────────────────

    @GetMapping("/by-country/{countryId}")
    @Operation(summary = "Lister les villes d'un pays")
    public ResponseEntity<List<CityResponse>> getCitiesByCountry(@PathVariable Long countryId) {
        return ResponseEntity.ok(cityService.getCitiesByCountry(countryId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une ville par ID")
    public ResponseEntity<CityResponse> getCityById(@PathVariable Long id) {
        return ResponseEntity.ok(cityService.getCityById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @Operation(summary = "Créer une nouvelle ville")
    public ResponseEntity<CityResponse> createCity(@Valid @RequestBody CityRequest request) {
        CityResponse response = cityService.createCity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @Operation(summary = "Modifier une ville")
    public ResponseEntity<CityResponse> updateCity(
            @PathVariable Long id,
            @Valid @RequestBody CityRequest request) {
        CityResponse response = cityService.updateCity(id, request);
        return ResponseEntity.ok(response);
    }

    // ── Contacts WhatsApp ──────────────────────────────────────────

    @GetMapping("/{cityId}/whatsapp")
    @Operation(summary = "Lister les contacts WhatsApp d'une ville")
    public ResponseEntity<List<WhatsappContactResponse>> getWhatsappContacts(
            @PathVariable Long cityId) {
        return ResponseEntity.ok(cityService.getWhatsappContacts(cityId));
    }

    @PostMapping("/{cityId}/whatsapp")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @Operation(summary = "Ajouter un contact WhatsApp à une ville")
    public ResponseEntity<WhatsappContactResponse> addWhatsappContact(
            @PathVariable Long cityId,
            @Valid @RequestBody WhatsappContactRequest request) {
        // Forcer le cityId du body au cityId du path
        request.setCityId(cityId);
        WhatsappContactResponse response = cityService.addWhatsappContact(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/whatsapp/{contactId}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @Operation(summary = "Modifier un contact WhatsApp")
    public ResponseEntity<WhatsappContactResponse> updateWhatsappContact(
            @PathVariable Long contactId,
            @Valid @RequestBody WhatsappContactRequest request) {
        WhatsappContactResponse response = cityService.updateWhatsappContact(contactId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/whatsapp/{contactId}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    @Operation(summary = "Supprimer un contact WhatsApp")
    public ResponseEntity<Void> deleteWhatsappContact(@PathVariable Long contactId) {
        cityService.deleteWhatsappContact(contactId);
        return ResponseEntity.noContent().build();
    }
}
