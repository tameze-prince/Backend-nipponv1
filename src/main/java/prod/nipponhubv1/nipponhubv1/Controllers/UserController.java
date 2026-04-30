package prod.nipponhubv1.nipponhubv1.Controllers;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import prod.nipponhubv1.nipponhubv1.Dto.Request.UpdateProfileRequest;
import prod.nipponhubv1.nipponhubv1.Dto.Request.UserAddressRequest;
import prod.nipponhubv1.nipponhubv1.Dto.UserAddressResponse;
import prod.nipponhubv1.nipponhubv1.Dto.UserResponse;
import prod.nipponhubv1.nipponhubv1.Models.Enums.Role;
import prod.nipponhubv1.nipponhubv1.Services.UserService;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestion des utilisateurs")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                userService.getMyProfile(userDetails.getUsername()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(
                userService.updateMyProfile(userDetails.getUsername(), req));
    }

    @PatchMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> updateAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("avatar") MultipartFile file) throws IOException {
        return ResponseEntity.ok(
                userService.updateMyAvatar(userDetails.getUsername(), file));
    }

    @GetMapping("/me/addresses")
    public ResponseEntity<List<UserAddressResponse>> getMyAddresses(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getMyAddresses(userDetails.getUsername()));
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<UserAddressResponse> createMyAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserAddressRequest req) {
        return ResponseEntity.ok(userService.createMyAddress(userDetails.getUsername(), req));
    }

    @GetMapping("/me/addresses/{id}")
    public ResponseEntity<UserAddressResponse> getMyAddressById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(userService.getMyAddressById(userDetails.getUsername(), id));
    }

    @PutMapping("/me/addresses/{id}")
    public ResponseEntity<UserAddressResponse> updateMyAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UserAddressRequest req) {
        return ResponseEntity.ok(userService.updateMyAddress(userDetails.getUsername(), id, req));
    }

    @DeleteMapping("/me/addresses/{id}")
    public ResponseEntity<Void> deleteMyAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        userService.deleteMyAddress(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/addresses/{id}/default")
    public ResponseEntity<UserAddressResponse> setDefaultAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(userService.setDefaultAddress(userDetails.getUsername(), id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Page<UserResponse>> listUsers(
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 20, sort = "createdAt",
                             direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(countryId, role, pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<UserResponse>> searchClients(
            @RequestParam String q) {
        return ResponseEntity.ok(userService.searchClients(q));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<UserResponse> toggleActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(userService.toggleUserActive(id, active));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable Long id,
            @RequestParam Role role) {
        return ResponseEntity.ok(userService.changeRole(id, role));
    }
}
