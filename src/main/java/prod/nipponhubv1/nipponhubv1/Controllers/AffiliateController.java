package prod.nipponhubv1.nipponhubv1.Controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import prod.nipponhubv1.nipponhubv1.Dto.AffiliateCommissionResponse;
import prod.nipponhubv1.nipponhubv1.Dto.AffiliatePaymentRequest;
import prod.nipponhubv1.nipponhubv1.Dto.AffiliateStatsResponse;
import prod.nipponhubv1.nipponhubv1.Models.AffiliatePaymentOrder;
import prod.nipponhubv1.nipponhubv1.Services.AffiliateService;

/**
 * ── PARTNER (Portail) ──────────────────────────────────────────────────
 * GET    /api/v1/affiliate/portal/stats            → mes statistiques
 * GET    /api/v1/affiliate/portal/commissions      → mes commissions
 * GET    /api/v1/affiliate/portal/payment-orders   → mes ordres de paiement
 * GET    /api/v1/affiliate/track?code=&ip=         → tracker un click (PUBLIC)
 *
 * ── OWNER (Gestion) ───────────────────────────────────────────────────
 * GET    /api/v1/affiliate/manage/partners         → tous les partenaires
 * PATCH  /api/v1/affiliate/manage/{id}/active      → activer/désactiver
 * PATCH  /api/v1/affiliate/manage/{id}/rate        → modifier taux
 * POST   /api/v1/affiliate/manage/payment-orders   → créer ordre de paiement
 * POST   /api/v1/affiliate/manage/payment-orders/{id}/validate → valider + preuve
 * PATCH  /api/v1/affiliate/manage/commissions/{id}/confirm   → confirmer
 * PATCH  /api/v1/affiliate/manage/commissions/{id}/reject    → rejeter
 */
@RestController
@RequestMapping("/api/v1/affiliate")
@RequiredArgsConstructor
@Tag(name = "Affiliate", description = "Programme d'affiliation")
public class AffiliateController {

    private final AffiliateService affiliateService;

    // ── PARTNER ──────────────────────────────────────────────────────────────

    @GetMapping("/portal/stats")
    @PreAuthorize("hasAnyRole('PARTNER','ADMIN','OWNER')")
    public ResponseEntity<AffiliateStatsResponse> getMyStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                affiliateService.getMyStats(userDetails.getUsername()));
    }

    @GetMapping("/portal/commissions")
    @PreAuthorize("hasAnyRole('PARTNER','ADMIN','OWNER')")
    public ResponseEntity<Page<AffiliateCommissionResponse>> getMyCommissions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                affiliateService.getMyCommissions(userDetails.getUsername(), pageable));
    }

    @GetMapping("/portal/payment-orders")
    @PreAuthorize("hasAnyRole('PARTNER','ADMIN','OWNER')")
    public ResponseEntity<List<AffiliatePaymentOrder>> getMyPaymentOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                affiliateService.getMyPaymentOrders(userDetails.getUsername()));
    }

    @GetMapping("/track")
    public ResponseEntity<Void> trackClick(
            @RequestParam String code,
            HttpServletRequest request) {
        String ip = Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                .orElse(request.getRemoteAddr());
        String ua = request.getHeader("User-Agent");
        affiliateService.trackClick(code, ip, ua);
        return ResponseEntity.ok().build();
    }

    // ── OWNER ────────────────────────────────────────────────────────────────

    @GetMapping("/manage/partners")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<AffiliateStatsResponse>> getAllPartners() {
        return ResponseEntity.ok(affiliateService.getAllPartners());
    }

    @PatchMapping("/manage/{id}/active")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> toggleActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        affiliateService.togglePartnerActive(id, active);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/manage/{id}/rate")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<AffiliateStatsResponse> updateRate(
            @PathVariable Long id,
            @RequestParam BigDecimal rate) {
        return ResponseEntity.ok(affiliateService.updateCommissionRate(id, rate));
    }

    @PostMapping("/manage/payment-orders")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<AffiliatePaymentOrder> createPaymentOrder(
            @Valid @RequestBody AffiliatePaymentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(affiliateService.createPaymentOrder(req));
    }

    @PostMapping(value = "/manage/payment-orders/{id}/validate",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<AffiliatePaymentOrder> validatePayment(
            @PathVariable Long id,
            @RequestPart(value = "proof", required = false) MultipartFile proof)
            throws IOException {
        return ResponseEntity.ok(affiliateService.validatePayment(id, proof));
    }

    @PatchMapping("/manage/commissions/{id}/confirm")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<AffiliateCommissionResponse> confirmCommission(
            @PathVariable Long id) {
        return ResponseEntity.ok(affiliateService.confirmCommission(id));
    }

    @PatchMapping("/manage/commissions/{id}/reject")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<AffiliateCommissionResponse> rejectCommission(
            @PathVariable Long id) {
        return ResponseEntity.ok(affiliateService.rejectCommission(id));
    }
}
