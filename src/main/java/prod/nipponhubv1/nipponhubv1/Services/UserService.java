package prod.nipponhubv1.nipponhubv1.Services;

import java.io.IOException;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import prod.nipponhubv1.nipponhubv1.Dto.Request.UpdateProfileRequest;
import prod.nipponhubv1.nipponhubv1.Dto.Request.UserAddressRequest;
import prod.nipponhubv1.nipponhubv1.Dto.UserAddressResponse;
import prod.nipponhubv1.nipponhubv1.Dto.UserResponse;
import prod.nipponhubv1.nipponhubv1.Exception.OtakuException;
import prod.nipponhubv1.nipponhubv1.Mappers.UserMapper;
import prod.nipponhubv1.nipponhubv1.Models.City;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Models.UserAddress;
import prod.nipponhubv1.nipponhubv1.Models.Enums.Role;
import prod.nipponhubv1.nipponhubv1.Repository.CityRepository;
import prod.nipponhubv1.nipponhubv1.Repository.LoyaltyAccountRepository;
import prod.nipponhubv1.nipponhubv1.Repository.UserAddressRepository;
import prod.nipponhubv1.nipponhubv1.Repository.UserRepository;

/**
 * Gestion du profil utilisateur et administration des comptes.
 *
 * Chaque utilisateur ne peut modifier que son propre profil (vérifié par email JWT).
 * Les ADMIN/OWNER peuvent accéder à tous les profils.
 * Le changement de rôle est réservé à l'OWNER.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository         userRepo;
    private final CityRepository         cityRepo;
    private final LoyaltyAccountRepository loyaltyRepo;
    private final UserAddressRepository  userAddressRepo;
    private final CloudinaryService      cloudinaryService;
    private final UserMapper             userMapper;
    private final PasswordEncoder        passwordEncoder;

    // ── Profil courant (moi) ──────────────────────────────────────────────────

    /**
     * Retourne le profil enrichi de l'utilisateur connecté (avec loyalty).
     */
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String email) {
        OurUser user = findByEmailOrThrow(email);
        return enrichWithLoyalty(userMapper.toResponse(user), user);
    }

    /**
     * Met à jour le profil de l'utilisateur connecté.
     * Seuls les champs non-null de la requête sont modifiés (partial update).
     */
    @Transactional
    public UserResponse updateMyProfile(String email,
                                         UpdateProfileRequest req) {
        OurUser user = findByEmailOrThrow(email);

        if (hasText(req.getFirstName())) user.setFirstName(req.getFirstName());
        if (hasText(req.getLastName()))  user.setLastName(req.getLastName());
        if (hasText(req.getPhone()))     user.setPhone(req.getPhone());

        if (req.getCityId() != null) {
            City city = cityRepo.findById(req.getCityId())
                .orElseThrow(() -> OtakuException.notFound("Ville", req.getCityId()));
            user.setCity(city);
        }

        // Changement de mot de passe — requiert l'ancien
        if (hasText(req.getNewPassword())) {
            if (!hasText(req.getCurrentPassword())) {
                throw OtakuException.badRequest(
                    "Le mot de passe actuel est requis pour en définir un nouveau."
                );
            }
            if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
                throw OtakuException.badRequest("Mot de passe actuel incorrect.");
            }
            user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
            log.info("Mot de passe mis à jour — userId={}", user.getId());
        }

        return userMapper.toResponse(userRepo.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserAddressResponse> getMyAddresses(String email) {
        OurUser user = findByEmailOrThrow(email);
        return userAddressRepo.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId())
            .stream()
            .map(this::toAddressResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public UserAddressResponse getMyAddressById(String email, Long addressId) {
        OurUser user = findByEmailOrThrow(email);
        UserAddress address = userAddressRepo.findByIdAndUserId(addressId, user.getId())
            .orElseThrow(() -> OtakuException.notFound("Adresse", addressId));
        return toAddressResponse(address);
    }

    @Transactional
    public UserAddressResponse createMyAddress(String email, UserAddressRequest req) {
        OurUser user = findByEmailOrThrow(email);
        boolean setDefault = Boolean.TRUE.equals(req.getIsDefault())
            || userAddressRepo.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId()).isEmpty();

        if (setDefault) {
            clearDefaultAddress(user.getId());
        }

        UserAddress address = UserAddress.builder()
            .user(user)
            .label(req.getLabel().trim())
            .firstName(req.getFirstName().trim())
            .lastName(trimToNull(req.getLastName()))
            .phone(req.getPhone().trim())
            .address(req.getAddress().trim())
            .city(req.getCity().trim())
            .state(trimToNull(req.getState()))
            .country(req.getCountry().trim())
            .postalCode(trimToNull(req.getPostalCode()))
            .isDefault(setDefault)
            .build();

        return toAddressResponse(userAddressRepo.save(address));
    }

    @Transactional
    public UserAddressResponse updateMyAddress(String email, Long addressId, UserAddressRequest req) {
        OurUser user = findByEmailOrThrow(email);
        UserAddress address = userAddressRepo.findByIdAndUserId(addressId, user.getId())
            .orElseThrow(() -> OtakuException.notFound("Adresse", addressId));

        if (Boolean.TRUE.equals(req.getIsDefault()) && !address.isDefault()) {
            clearDefaultAddress(user.getId());
            address.setDefault(true);
        } else if (req.getIsDefault() != null && !req.getIsDefault()) {
            address.setDefault(false);
        }

        address.setLabel(req.getLabel().trim());
        address.setFirstName(req.getFirstName().trim());
        address.setLastName(trimToNull(req.getLastName()));
        address.setPhone(req.getPhone().trim());
        address.setAddress(req.getAddress().trim());
        address.setCity(req.getCity().trim());
        address.setState(trimToNull(req.getState()));
        address.setCountry(req.getCountry().trim());
        address.setPostalCode(trimToNull(req.getPostalCode()));

        UserAddress saved = userAddressRepo.save(address);
        ensureOneDefaultAddress(user.getId());
        return toAddressResponse(saved);
    }

    @Transactional
    public void deleteMyAddress(String email, Long addressId) {
        OurUser user = findByEmailOrThrow(email);
        UserAddress address = userAddressRepo.findByIdAndUserId(addressId, user.getId())
            .orElseThrow(() -> OtakuException.notFound("Adresse", addressId));
        boolean wasDefault = address.isDefault();
        userAddressRepo.delete(address);

        if (wasDefault) {
            ensureOneDefaultAddress(user.getId());
        }
    }

    @Transactional
    public UserAddressResponse setDefaultAddress(String email, Long addressId) {
        OurUser user = findByEmailOrThrow(email);
        UserAddress address = userAddressRepo.findByIdAndUserId(addressId, user.getId())
            .orElseThrow(() -> OtakuException.notFound("Adresse", addressId));
        clearDefaultAddress(user.getId());
        address.setDefault(true);
        return toAddressResponse(userAddressRepo.save(address));
    }

    /**
     * Remplace l'avatar de l'utilisateur connecté.
     * Supprime l'ancien avatar de Cloudinary avant d'uploader le nouveau.
     */
    @Transactional
    public UserResponse updateMyAvatar(String email,
                                        MultipartFile file) throws IOException {
        OurUser user = findByEmailOrThrow(email);

        var result = cloudinaryService.uploadAvatar(file, user.getId());
        if (!result.isUploaded()) {
            log.warn("Avatar non mis a jour car Cloudinary est indisponible - userId={}", user.getId());
            return userMapper.toResponse(user);
        }

        // Supprimer l'ancienne image uniquement apres upload reussi
        if (hasText(user.getAvatarPublicId())) {
            cloudinaryService.delete(user.getAvatarPublicId());
        }

        user.setAvatarUrl(result.url());
        user.setAvatarPublicId(result.publicId());

        return userMapper.toResponse(userRepo.save(user));
    }

    // ── Administration (ADMIN / OWNER) ────────────────────────────────────────

    /**
     * Liste paginée des clients d'un pays.
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Long countryId, Role role, Pageable pageable) {
        return userRepo.findAllByCountryIdAndRole(countryId, role, pageable)
            .map(userMapper::toResponse);
    }

    /**
     * Détail d'un utilisateur (Admin).
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        OurUser user = userRepo.findById(id)
            .orElseThrow(() -> OtakuException.notFound("Utilisateur", id));
        return enrichWithLoyalty(userMapper.toResponse(user), user);
    }

    /**
     * Recherche client pour le POS (scan ou saisie).
     */
    @Transactional(readOnly = true)
    public List<UserResponse> searchClients(String query) {
        return userRepo.searchClients(query)
            .stream()
            .map(userMapper::toResponse)
            .toList();
    }

    /**
     * Activer / désactiver un compte (ADMIN/OWNER).
     */
    @Transactional
    public UserResponse toggleUserActive(Long id, boolean active) {
        OurUser user = userRepo.findById(id)
            .orElseThrow(() -> OtakuException.notFound("Utilisateur", id));
        user.setActive(active);
        log.info("{} compte userId={}", active ? "Activation" : "Désactivation", id);
        return userMapper.toResponse(userRepo.save(user));
    }

    /**
     * Changer le rôle d'un utilisateur.
     * Réservé à l'OWNER — enforced ici + @PreAuthorize dans le controller.
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public UserResponse changeRole(Long id, Role newRole) {
        OurUser user = userRepo.findById(id)
            .orElseThrow(() -> OtakuException.notFound("Utilisateur", id));

        // Interdit de se rétrograder soi-même
        if (user.getRole() == Role.OWNER && newRole != Role.OWNER) {
            throw OtakuException.forbidden(
                "Impossible de changer le rôle d'un OWNER via cette opération."
            );
        }

        user.setRole(newRole);
        log.info("Rôle changé — userId={} newRole={}", id, newRole);
        return userMapper.toResponse(userRepo.save(user));
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private OurUser findByEmailOrThrow(String email) {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> OtakuException.notFound("Utilisateur (email=" + email + ")", 0L));
    }

    private UserResponse enrichWithLoyalty(UserResponse resp, OurUser user) {
        if (user.getRole() == Role.CLIENT) {
            loyaltyRepo.findByUserId(user.getId()).ifPresent(la -> {
                resp.setLoyaltyPoints(la.getPointsBalance());
                if (la.getGrade() != null) {
                    resp.setLoyaltyGrade(la.getGrade().getName());
                }
            });
        }
        return resp;
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private void clearDefaultAddress(Long userId) {
        userAddressRepo.findFirstByUserIdAndIsDefaultTrue(userId).ifPresent(address -> {
            address.setDefault(false);
            userAddressRepo.save(address);
        });
    }

    private void ensureOneDefaultAddress(Long userId) {
        if (userAddressRepo.findFirstByUserIdAndIsDefaultTrue(userId).isPresent()) {
            return;
        }

        userAddressRepo.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
            .stream()
            .findFirst()
            .ifPresent(address -> {
                address.setDefault(true);
                userAddressRepo.save(address);
            });
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UserAddressResponse toAddressResponse(UserAddress address) {
        return UserAddressResponse.builder()
            .id(address.getId())
            .userId(address.getUser().getId())
            .label(address.getLabel())
            .firstName(address.getFirstName())
            .lastName(address.getLastName())
            .phone(address.getPhone())
            .address(address.getAddress())
            .city(address.getCity())
            .state(address.getState())
            .country(address.getCountry())
            .postalCode(address.getPostalCode())
            .isDefault(address.isDefault())
            .createdAt(address.getCreatedAt())
            .updatedAt(address.getUpdatedAt())
            .build();
    }
}
