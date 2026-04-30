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
import prod.nipponhubv1.nipponhubv1.Models.Category;
import prod.nipponhubv1.nipponhubv1.Repository.CategoryRepository;
import prod.nipponhubv1.nipponhubv1.Services.CloudinaryService;

/**
 * GET    /api/v1/categories               → liste (PUBLIC)
 * POST   /api/v1/categories               → créer (ADMIN/OWNER)
 * PUT    /api/v1/categories/{id}          → modifier (ADMIN/OWNER)
 * DELETE /api/v1/categories/{id}          → désactiver (ADMIN/OWNER)
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Catégories produits")
public class CategoryController {

    private final CategoryRepository categoryRepo;
    private final CloudinaryService  cloudinaryService;

    @GetMapping
    public ResponseEntity<List<Category>> getAll() {
        return ResponseEntity.ok(categoryRepo.findByActiveTrue());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Category> create(
            @RequestParam String name,
            @RequestParam String slug,
            @RequestPart(value = "image", required = false) MultipartFile image)
            throws IOException {
        if (categoryRepo.existsBySlug(slug)) {
            throw OtakuException.conflict("Slug déjà utilisé : " + slug);
        }
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            var uploadResult = cloudinaryService
                    .upload(image, "otakushop/categories", "cat_" + slug);
            imageUrl = uploadResult.url();
        }
        Category cat = Category.builder()
                .name(name).slug(slug)
                .imageUrl(imageUrl).active(true)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryRepo.save(cat));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Category> update(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestPart(value = "image", required = false) MultipartFile image)
            throws IOException {
        Category cat = categoryRepo.findById(id)
                .orElseThrow(() -> OtakuException.notFound("Catégorie", id));
        if (name != null && !name.isBlank()) cat.setName(name);
        if (image != null && !image.isEmpty()) {
            var uploadResult = cloudinaryService
                    .upload(image, "otakushop/categories", "cat_" + id);
            if (uploadResult.isUploaded()) {
                cat.setImageUrl(uploadResult.url());
            }
        }
        return ResponseEntity.ok(categoryRepo.save(cat));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        Category cat = categoryRepo.findById(id)
                .orElseThrow(() -> OtakuException.notFound("Catégorie", id));
        cat.setActive(false);
        categoryRepo.save(cat);
        return ResponseEntity.noContent().build();
    }
}
