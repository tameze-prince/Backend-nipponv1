package prod.nipponhubv1.nipponhubv1.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import prod.nipponhubv1.nipponhubv1.Dto.OrderRequest;
import prod.nipponhubv1.nipponhubv1.Dto.OrderResponse;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Models.Enums.OrderStatus;
import prod.nipponhubv1.nipponhubv1.Services.OrderService;

/**
 * POST   /api/v1/orders                           → passer commande (CLIENT)
 * GET    /api/v1/orders/my-orders                 → mes commandes (CLIENT)
 * GET    /api/v1/orders/my-orders/{id}            → détail commande (CLIENT)
 * GET    /api/v1/orders                           → toutes commandes (ADMIN/OWNER)
 * GET    /api/v1/orders/{id}                      → détail (ADMIN/OWNER)
 * PATCH  /api/v1/orders/{id}/status               → changer statut (ADMIN/OWNER)
 * POST   /api/v1/orders/pos                       → vente comptoir (ADMIN/OWNER)
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Commandes en ligne et ventes POS")
public class OrderController {

    private final OrderService orderService;

    // ── CLIENT ───────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<OrderResponse> placeOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrderRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(userDetails.getUsername(), req));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt",
                             direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                orderService.getMyOrders(userDetails.getUsername(), pageable));
    }

    @GetMapping("/my-orders/{id}")
    public ResponseEntity<OrderResponse> getMyOrderById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(
                orderService.getOrderById(id, userDetails.getUsername()));
    }

    // ── ADMIN / OWNER ────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) OrderStatus status,
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt",
                             direction = Sort.Direction.DESC) Pageable pageable) {
        OurUser requester = (OurUser) userDetails;
        return ResponseEntity.ok(
                orderService.getOrdersForBackoffice(requester, countryId, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<OrderResponse> getById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(
                orderService.getOrderById(id, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser admin = (OurUser) userDetails;
        return ResponseEntity.ok(orderService.updateStatus(id, status, admin));
    }

    // ── POS ──────────────────────────────────────────────────────────────────

    @PostMapping("/pos")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<OrderResponse> createPosOrder(
            @RequestParam String clientQrCode,
            @Valid @RequestBody OrderRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser admin = (OurUser) userDetails;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createPosOrder(clientQrCode, req, admin));
    }
}
