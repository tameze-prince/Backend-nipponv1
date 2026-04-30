package prod.nipponhubv1.nipponhubv1.Controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import prod.nipponhubv1.nipponhubv1.Dto.FlashSaleRequest;
import prod.nipponhubv1.nipponhubv1.Dto.FlashSaleResponse;
import prod.nipponhubv1.nipponhubv1.Models.FlashSale;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Models.Product;
import prod.nipponhubv1.nipponhubv1.Services.FlashSaleService;

/**
 * GET    /api/v1/flash-sales               → ventes flash actives (PUBLIC)
 * POST   /api/v1/flash-sales               → créer (ADMIN/OWNER)
 * DELETE /api/v1/flash-sales/{id}          → désactiver (ADMIN/OWNER)
 */
@RestController
@RequestMapping("/api/v1/flash-sales")
@RequiredArgsConstructor
@Tag(name = "Flash Sales", description = "Ventes flash")
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    @GetMapping
    public ResponseEntity<List<FlashSaleResponse>> getActive() {
        return ResponseEntity.ok(
                flashSaleService.getEnabledFlashSales()
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<FlashSaleResponse> create(
            @Valid @RequestBody FlashSaleRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser admin = (OurUser) userDetails;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(flashSaleService.create(req, admin)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<FlashSaleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FlashSaleRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser admin = (OurUser) userDetails;
        return ResponseEntity.ok(toResponse(flashSaleService.update(id, req, admin)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser requester = (OurUser) userDetails;
        flashSaleService.deactivate(id, requester);
        return ResponseEntity.noContent().build();
    }

    private FlashSaleResponse toResponse(FlashSale flashSale) {
        Product product = flashSale.getProduct();
        OurUser createdBy = flashSale.getCreatedBy();

        return FlashSaleResponse.builder()
                .id(flashSale.getId())
                .product(product != null
                        ? FlashSaleResponse.ProductSummary.builder()
                                .id(product.getId())
                                .name(product.getName())
                                .slug(product.getSlug())
                                .build()
                        : null)
                .discountPct(flashSale.getDiscountPct())
                .startsAt(flashSale.getStartsAt())
                .endsAt(flashSale.getEndsAt())
                .active(flashSale.isActive())
                .createdBy(createdBy != null
                        ? FlashSaleResponse.UserSummary.builder()
                                .id(createdBy.getId())
                                .firstName(createdBy.getFirstName())
                                .lastName(createdBy.getLastName())
                                .email(createdBy.getEmail())
                                .build()
                        : null)
                .build();
    }
}
