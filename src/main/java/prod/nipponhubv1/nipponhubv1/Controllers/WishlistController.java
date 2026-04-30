package prod.nipponhubv1.nipponhubv1.Controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import prod.nipponhubv1.nipponhubv1.Exception.OtakuException;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Models.ProductVariant;
import prod.nipponhubv1.nipponhubv1.Models.Wishlist;
import prod.nipponhubv1.nipponhubv1.Repository.ProductVariantRepository;
import prod.nipponhubv1.nipponhubv1.Repository.UserRepository;
import prod.nipponhubv1.nipponhubv1.Repository.WishlistRepository;

/**
 * GET    /api/v1/wishlist          → ma wishlist (CLIENT)
 * POST   /api/v1/wishlist/{variantId}   → ajouter (CLIENT)
 * DELETE /api/v1/wishlist/{variantId}   → retirer (CLIENT)
 */
@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Liste de souhaits client")
public class WishlistController {

    private final WishlistRepository wishlistRepo;
    private final ProductVariantRepository variantRepo;
    private final UserRepository userRepo;

    @GetMapping
    public ResponseEntity<List<Wishlist>> getMyWishlist(
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser user = userRepo.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(wishlistRepo.findByUserId(user.getId()));
    }

    @PostMapping("/{variantId}")
    public ResponseEntity<Void> add(
            @PathVariable Long variantId,
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser user = userRepo.findByEmail(userDetails.getUsername()).orElseThrow();
        if (wishlistRepo.existsByUserIdAndVariantId(user.getId(), variantId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        ProductVariant variant = variantRepo.findById(variantId)
                .orElseThrow(() -> OtakuException.notFound("Variante", variantId));
        wishlistRepo.save(Wishlist.builder().user(user).variant(variant).build());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{variantId}")
    public ResponseEntity<Void> remove(
            @PathVariable Long variantId,
            @AuthenticationPrincipal UserDetails userDetails) {
        OurUser user = userRepo.findByEmail(userDetails.getUsername()).orElseThrow();
        wishlistRepo.deleteByUserIdAndVariantId(user.getId(), variantId);
        return ResponseEntity.noContent().build();
    }
}
