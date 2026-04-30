package prod.nipponhubv1.nipponhubv1.Controllers;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import prod.nipponhubv1.nipponhubv1.Dto.BulkProductRequest;
import prod.nipponhubv1.nipponhubv1.Dto.ProductRequest;
import prod.nipponhubv1.nipponhubv1.Dto.ProductResponse;
import prod.nipponhubv1.nipponhubv1.Dto.VariantRequest;
import prod.nipponhubv1.nipponhubv1.Dto.VariantResponse;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Services.ProductService;

/**
 * GET    /api/v1/products                  → catalogue paginé (PUBLIC)
 * GET    /api/v1/products/flash-sales       → ventes flash actives (PUBLIC)
 * GET    /api/v1/products/{slug}           → fiche produit (PUBLIC)
 * POST   /api/v1/products                  → créer (ADMIN/OWNER)
 * PUT    /api/v1/products/{id}             → modifier (ADMIN/OWNER)
 * DELETE /api/v1/products/{id}             → désactiver (ADMIN/OWNER)
 * POST   /api/v1/products/{id}/variants    → ajouter variante (ADMIN/OWNER)
 * DELETE /api/v1/products/variants/{id}    → supprimer variante (ADMIN/OWNER)
 * DELETE /api/v1/products/images/{id}      → supprimer image (ADMIN/OWNER)
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Catalogue produits")
public class ProductController {

    private final ProductService productService;

    // ── PUBLIC ───────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getCatalogue(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long franchiseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) Long cityId,
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 12, sort = "createdAt",
                            direction = Sort.Direction.DESC) Pageable pageable) {
        OurUser requester = userDetails instanceof OurUser ? (OurUser) userDetails : null;
        return ResponseEntity.ok(
                productService.getCatalogue(categoryId, franchiseId,
                        keyword, countryId, cityId, requester, pageable));
    }

    @GetMapping("/flash-sales")
    public ResponseEntity<List<ProductResponse>> getFlashSales(
            @RequestParam(required = false) Long countryId) {
        return ResponseEntity.ok(productService.getFlashSaleProducts(countryId));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProductResponse> getBySlug(
            @PathVariable String slug,
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) Long cityId,
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser requester = userDetails instanceof OurUser ? (OurUser) userDetails : null;
        return ResponseEntity.ok(productService.getBySlug(slug, countryId, cityId, requester));
    }

    // ── ADMIN / OWNER ────────────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestPart("data") ProductRequest req,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails)
            throws IOException {
        OurUser requester = (OurUser) userDetails;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(req, images, requester));
    }

    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<ProductResponse>> createBulk(
            @Valid @RequestPart("data") BulkProductRequest req,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails)
            throws IOException {
        OurUser requester = (OurUser) userDetails;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProducts(req, images, requester));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestPart("data") ProductRequest req,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails)
            throws IOException {
        OurUser requester = (OurUser) userDetails;
        return ResponseEntity.ok(productService.updateProduct(id, req, images, requester));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser requester = (OurUser) userDetails;
        productService.deactivateProduct(id, requester);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/variants")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<VariantResponse> addVariant(
            @PathVariable Long id,
            @Valid @RequestBody VariantRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser requester = (OurUser) userDetails;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.addVariant(id, req, requester));
    }

    @DeleteMapping("/variants/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> deleteVariant(
            @PathVariable Long variantId,
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser requester = (OurUser) userDetails;
        productService.deleteVariant(variantId, requester);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/images/{imageId}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId) {
        productService.deleteProductImage(imageId);
        return ResponseEntity.noContent().build();
    }
}
