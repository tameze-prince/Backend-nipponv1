package prod.nipponhubv1.nipponhubv1.Controllers;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import prod.nipponhubv1.nipponhubv1.Exception.OtakuException;
import prod.nipponhubv1.nipponhubv1.Models.Franchise;
import prod.nipponhubv1.nipponhubv1.Repository.FranchiseRepository;
import prod.nipponhubv1.nipponhubv1.Services.CloudinaryService;

/**
 * GET    /api/v1/franchises               → liste (PUBLIC)
 * POST   /api/v1/franchises               → créer (ADMIN/OWNER)
 * PUT    /api/v1/franchises/{id}          → modifier (ADMIN/OWNER)
 * DELETE /api/v1/franchises/{id}          → désactiver (ADMIN/OWNER)
 */
@RestController
@RequestMapping("/api/v1/franchises")
@RequiredArgsConstructor
@Tag(name = "Franchises", description = "Franchises anime (Naruto, One Piece…)")
public class FranchiseController {

    private final FranchiseRepository franchiseRepo;
    private final CloudinaryService   cloudinaryService;

    @GetMapping
    public ResponseEntity<List<Franchise>> getAll() {
        return ResponseEntity.ok(franchiseRepo.findByActiveTrue());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Franchise> create(
            @RequestParam String name,
            @RequestParam String slug,
            @RequestPart(value = "logo", required = false) MultipartFile logo)
            throws IOException {
        if (franchiseRepo.existsBySlug(slug)) {
            throw OtakuException.conflict("Slug déjà utilisé : " + slug);
        }
        String imageUrl = null;
        if (logo != null && !logo.isEmpty()) {
            var uploadResult = cloudinaryService.uploadFranchiseLogo(logo, null);
            imageUrl = uploadResult.url();
        }
        Franchise f = Franchise.builder()
                .name(name).slug(slug)
                .imageUrl(imageUrl).active(true)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(franchiseRepo.save(f));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Franchise> update(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestPart(value = "logo", required = false) MultipartFile logo)
            throws IOException {
        Franchise f = franchiseRepo.findById(id)
                .orElseThrow(() -> OtakuException.notFound("Franchise", id));
        if (name != null && !name.isBlank()) f.setName(name);
        if (logo != null && !logo.isEmpty()) {
            var uploadResult = cloudinaryService.uploadFranchiseLogo(logo, id);
            if (uploadResult.isUploaded()) {
                f.setImageUrl(uploadResult.url());
            }
        }
        return ResponseEntity.ok(franchiseRepo.save(f));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        Franchise f = franchiseRepo.findById(id)
                .orElseThrow(() -> OtakuException.notFound("Franchise", id));
        f.setActive(false);
        franchiseRepo.save(f);
        return ResponseEntity.noContent().build();
    }
}
