package prod.nipponhubv1.nipponhubv1.Controllers;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import prod.nipponhubv1.nipponhubv1.Dto.LoyaltyAccountResponse;
import prod.nipponhubv1.nipponhubv1.Models.LoyaltyGrade;
import prod.nipponhubv1.nipponhubv1.Models.LoyaltyTransaction;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Services.LoyaltyService;

/**
 * GET    /api/v1/loyalty/me                → mon compte Nippon Pass (CLIENT)
 * GET    /api/v1/loyalty/me/history        → historique transactions (CLIENT)
 * POST   /api/v1/loyalty/me/qr-code        → régénérer QR code (CLIENT)
 * GET    /api/v1/loyalty/grades            → tous les grades (PUBLIC)
 * GET    /api/v1/loyalty/{userId}          → compte d'un client (ADMIN/OWNER)
 * GET    /api/v1/loyalty/qr/{code}         → lookup QR code POS (ADMIN/OWNER)
 * POST   /api/v1/loyalty/{userId}/bonus    → attribuer points bonus (ADMIN/OWNER)
 * GET    /api/v1/loyalty/top?countryId=    → top spenders (ADMIN/OWNER)
 * POST   /api/v1/loyalty/grades            → créer grade (OWNER)
 * PUT    /api/v1/loyalty/grades/{id}       → modifier grade (OWNER)
 */
@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
@Tag(name = "Loyalty", description = "Programme de fidélité Nippon Pass")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    // ── CLIENT ───────────────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<LoyaltyAccountResponse> getMyAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                loyaltyService.getMyAccount(userDetails.getUsername()));
    }

    @GetMapping("/me/history")
    public ResponseEntity<Page<LoyaltyTransaction>> getMyHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                loyaltyService.getMyHistory(userDetails.getUsername(), pageable));
    }

    @PostMapping("/me/qr-code")
    public ResponseEntity<LoyaltyAccountResponse> regenerateQrCode(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                loyaltyService.regenerateQrCode(userDetails.getUsername()));
    }

    // ── PUBLIC ───────────────────────────────────────────────────────────────

    @GetMapping("/grades")
    public ResponseEntity<List<LoyaltyGrade>> getGrades() {
        return ResponseEntity.ok(loyaltyService.getAllGrades());
    }

    // ── ADMIN / OWNER ────────────────────────────────────────────────────────

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<LoyaltyAccountResponse> getByUserId(
            @PathVariable Long userId) {
        return ResponseEntity.ok(loyaltyService.getAccountByUserId(userId));
    }

    @GetMapping("/qr/{code}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<LoyaltyAccountResponse> getByQrCode(
            @PathVariable String code) {
        return ResponseEntity.ok(loyaltyService.getByQrCode(code));
    }

    @PostMapping("/{userId}/bonus")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<LoyaltyAccountResponse> grantBonus(
            @PathVariable Long userId,
            @RequestParam int points,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser admin = (OurUser) userDetails;
        return ResponseEntity.ok(
                loyaltyService.grantBonusPoints(userId, points, reason, admin));
    }

    @GetMapping("/top")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<LoyaltyAccountResponse>> getTopSpenders(
            @RequestParam Long countryId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(loyaltyService.getTopSpenders(countryId, limit));
    }

    // ── OWNER ────────────────────────────────────────────────────────────────

    @PostMapping("/grades")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<LoyaltyGrade> createGrade(
            @RequestBody LoyaltyGrade grade) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loyaltyService.createGrade(grade));
    }

    @PutMapping("/grades/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<LoyaltyGrade> updateGrade(
            @PathVariable Long id,
            @RequestBody LoyaltyGrade update) {
        return ResponseEntity.ok(loyaltyService.updateGrade(id, update));
    }
}
