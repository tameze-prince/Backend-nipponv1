package prod.nipponhubv1.nipponhubv1.Services;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import prod.nipponhubv1.nipponhubv1.Dto.Request.LoginRequest;
import prod.nipponhubv1.nipponhubv1.Dto.Request.RegisterRequest;
import prod.nipponhubv1.nipponhubv1.Dto.Response.AuthResponse;
import prod.nipponhubv1.nipponhubv1.Exception.OtakuException;
import prod.nipponhubv1.nipponhubv1.Mappers.UserMapper;
import prod.nipponhubv1.nipponhubv1.Models.AffiliateClick;
import prod.nipponhubv1.nipponhubv1.Models.City;
import prod.nipponhubv1.nipponhubv1.Models.Country;
import prod.nipponhubv1.nipponhubv1.Models.LoyaltyAccount;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;
import prod.nipponhubv1.nipponhubv1.Models.Enums.Role;
import prod.nipponhubv1.nipponhubv1.Repository.AffiliateClickRepository;
import prod.nipponhubv1.nipponhubv1.Repository.AffiliateProfileRepository;
import prod.nipponhubv1.nipponhubv1.Repository.CityRepository;
import prod.nipponhubv1.nipponhubv1.Repository.CountryRepository;
import prod.nipponhubv1.nipponhubv1.Repository.LoyaltyAccountRepository;
import prod.nipponhubv1.nipponhubv1.Repository.UserRepository;

/**
 * Gestion de l'authentification : register · login · refresh · logout.
 *
 * Différences vs exemple original :
 *  - Access token + Refresh token distincts (claims type différents)
 *  - Création automatique du LoyaltyAccount à l'inscription (CLIENT)
 *  - Lien partenaire (referralCode) tracé à l'inscription via AffiliateClick
 *  - Logout via blacklist token (Set en mémoire, remplaçable par Redis)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository            userRepo;
    private final CountryRepository         countryRepo;
    private final CityRepository            cityRepo;
    private final LoyaltyAccountRepository  loyaltyRepo;
    private final AffiliateProfileRepository affiliateRepo;
    private final AffiliateClickRepository  clickRepo;
    private final NotificationService       notificationService;
    private final JWTUtils                  jwtUtils;
    private final AuthenticationManager     authManager;
    private final PasswordEncoder           passwordEncoder;
    private final CloudinaryService         cloudinaryService;
    private final UserMapper                userMapper;

    // Blacklist en mémoire — remplacer par Redis en production
    private final Set<String> tokenBlacklist =
        Collections.newSetFromMap(new ConcurrentHashMap<>());

    // ── REGISTER ──────────────────────────────────────────────────────────────

    /**
     * Inscrit un nouveau CLIENT.
     * Flow :
     *  1. Vérifier unicité email + téléphone
     *  2. Upload avatar si fourni
     *  3. Persister OurUser
     *  4. Créer LoyaltyAccount automatiquement
     *  5. Tracker le click affilié si referralCode fourni
     *  6. Retourner tokens + profil
     */
    @Transactional
    public AuthResponse register(RegisterRequest req,
                                  MultipartFile   avatar) throws IOException {
        // ── Unicité ──────────────────────────────────────────────────────────
        if (userRepo.existsByEmail(req.getEmail())) {
            throw OtakuException.conflict(
                "Un compte existe déjà avec l'email : " + req.getEmail()
            );
        }
        if (userRepo.existsByPhone(req.getPhone())) {
            throw OtakuException.conflict(
                "Un compte existe déjà avec ce numéro de téléphone."
            );
        }

        // ── Avatar Cloudinary ────────────────────────────────────────────────
        String avatarUrl      = null;
        String avatarPublicId = null;

        if (avatar != null && !avatar.isEmpty()) {
            // On utilise un id temporaire, sera mis à jour après save
            var result = cloudinaryService.upload(
                avatar, "otakushop/avatars", null
            );
            if (result.isUploaded()) {
                avatarUrl      = result.url();
                avatarPublicId = result.publicId();
            }
        }

        // ── Résolution pays / ville ──────────────────────────────────────────
        Country country = req.getCountryId() != null
            ? countryRepo.findById(req.getCountryId())
                .orElseThrow(() -> OtakuException.notFound("Pays", req.getCountryId()))
            : null;

        City city = req.getCityId() != null
            ? cityRepo.findById(req.getCityId())
                .orElseThrow(() -> OtakuException.notFound("Ville", req.getCityId()))
            : null;

        // ── Création utilisateur ─────────────────────────────────────────────
        OurUser user = OurUser.builder()
            .firstName(req.getFirstName())
            .lastName(req.getLastName())
            .email(req.getEmail())
            .phone(req.getPhone())
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .role(Role.CLIENT)
            .country(country)
            .city(city)
            .avatarUrl(avatarUrl)
            .avatarPublicId(avatarPublicId)
            .active(true)
            .build();

        OurUser saved = userRepo.save(user);

        // ── Créer LoyaltyAccount automatiquement ─────────────────────────────
        LoyaltyAccount loyalty = LoyaltyAccount.builder()
            .user(saved)
            .pointsBalance(0)
            .totalSpent(BigDecimal.ZERO)
            .qrCode(generateQrCode(saved.getId()))
            .build();
        loyaltyRepo.save(loyalty);

        // ── Notifier l'admin d'une nouvelle inscription ──────────────────────
        userRepo.findByRole(Role.ADMIN).forEach(admin ->
            notificationService.notifyAdminNewUser(admin.getId(), saved.getId(), saved.getEmail())
        );

        // ── Tracker le click affilié si referralCode fourni ──────────────────
        if (req.getReferralCode() != null && !req.getReferralCode().isBlank()) {
            affiliateRepo.findByReferralCode(req.getReferralCode())
                .ifPresent(ap -> {
                    AffiliateClick click = AffiliateClick.builder()
                        .affiliate(ap)
                        .ipAddress("signup")
                        .userAgent("registration")
                        .build();
                    clickRepo.save(click);
                    log.info("Click affilié tracé — code={}", req.getReferralCode());
                });
        }

        log.info("✓ Nouveau CLIENT inscrit — id={} email={}", saved.getId(), saved.getEmail());
        return buildAuthResponse(saved);
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    /**
     * Authentifie un utilisateur et retourne access + refresh tokens.
     */
    public AuthResponse login(LoginRequest req) {
        // Spring Security vérifie les credentials
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        OurUser user = userRepo.findByEmail(req.getEmail())
            .orElseThrow(() -> OtakuException.notFound("Utilisateur", 0L));

        if (!user.isActive()) {
            throw OtakuException.forbidden("Votre compte est désactivé. Contactez le support.");
        }

        log.info("✓ Login — id={} role={}", user.getId(), user.getRole());
        return buildAuthResponse(user);
    }

    // ── REFRESH ───────────────────────────────────────────────────────────────

    /**
     * Émet un nouvel access token à partir d'un refresh token valide.
     * Le refresh token doit être du bon type (token_type=refresh).
     */
    public AuthResponse refresh(String refreshToken) {
        if (tokenBlacklist.contains(refreshToken)) {
            throw OtakuException.forbidden("Token révoqué.");
        }
        if (!jwtUtils.isRefreshToken(refreshToken)) {
            throw OtakuException.badRequest("Ce n'est pas un refresh token valide.");
        }
        if (jwtUtils.isTokenExpired(refreshToken)) {
            throw OtakuException.forbidden("Refresh token expiré. Veuillez vous reconnecter.");
        }

        String email = jwtUtils.extractEmail(refreshToken);
        OurUser user = userRepo.findByEmail(email)
            .orElseThrow(() -> OtakuException.notFound("Utilisateur", 0L));

        String newAccessToken = jwtUtils.generateAccessToken(user);

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)     // même refresh token
            .tokenType("Bearer")
            .expiresIn(jwtUtils.getAccessExpirationSeconds())
            .role(user.getRole())
            .userId(user.getId())
            .fullName(user.getFirstName() + " " + user.getLastName())
            .avatarUrl(user.getAvatarUrl())
            .build();
    }

    // ── LOGOUT ────────────────────────────────────────────────────────────────

    /**
     * Révoque le refresh token (blacklist en mémoire).
     * En production : utiliser Redis avec TTL = durée du refresh token.
     */
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenBlacklist.add(refreshToken);
            log.info("Token révoqué (logout)");
        }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(OurUser user) {
        return AuthResponse.builder()
            .accessToken(jwtUtils.generateAccessToken(user))
            .refreshToken(jwtUtils.generateRefreshToken(user))
            .tokenType("Bearer")
            .expiresIn(jwtUtils.getAccessExpirationSeconds())
            .role(user.getRole())
            .userId(user.getId())
            .fullName(user.getFirstName() + " " + user.getLastName())
            .avatarUrl(user.getAvatarUrl())
            .build();
    }

    private String generateQrCode(Long userId) {
        return "OTAKU-" + userId + "-"
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
