package prod.nipponhubv1.nipponhubv1.Services;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import prod.nipponhubv1.nipponhubv1.Exception.OtakuException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.enabled:false}")
    private boolean cloudinaryEnabled;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;
    private static final String[] ALLOWED_TYPES =
        {"image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"};

    public CloudinaryUploadResult upload(MultipartFile file,
                                         String folder,
                                         String publicId) throws IOException {
        validateFile(file);

        if (!cloudinaryEnabled) {
            log.warn("Cloudinary desactive: upload ignore pour folder={} publicId={}", folder, publicId);
            return CloudinaryUploadResult.empty();
        }

        Map<String, Object> options = new HashMap<>();
        options.put("folder", folder);
        options.put("resource_type", "image");

        if (publicId != null && !publicId.isBlank()) {
            options.put("public_id", publicId);
            options.put("overwrite", true);
        }

        options.put("quality", "auto");
        options.put("fetch_format", "auto");

        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);

            String url = (String) result.get("secure_url");
            String pid = (String) result.get("public_id");

            log.info("Image uploadee sur Cloudinary - public_id={} url={}", pid, url);
            return new CloudinaryUploadResult(url, pid);
        } catch (Exception ex) {
            if (isUnknownHost(ex)) {
                log.warn("Cloudinary indisponible (DNS/reseau) pour folder={} : {}", folder, ex.getMessage());
                return CloudinaryUploadResult.empty();
            }

            if (ex instanceof IOException ioException) {
                throw ioException;
            }

            throw new IOException("Echec upload Cloudinary", ex);
        }
    }

    public CloudinaryUploadResult uploadAvatar(MultipartFile file, Long userId) throws IOException {
        return upload(file, "otakushop/avatars", "avatar_" + userId);
    }

    public CloudinaryUploadResult uploadProductImage(MultipartFile file, Long productId) throws IOException {
        return upload(file, "otakushop/products", null);
    }

    public CloudinaryUploadResult uploadCategoryImage(MultipartFile file, Long categoryId) throws IOException {
        return upload(file, "otakushop/categories", "cat_" + categoryId);
    }

    public CloudinaryUploadResult uploadFranchiseLogo(MultipartFile file, Long franchiseId) throws IOException {
        return upload(file, "otakushop/franchises", "franchise_" + franchiseId);
    }

    public CloudinaryUploadResult uploadAffiliateProof(MultipartFile file, Long paymentOrderId) throws IOException {
        return upload(file, "otakushop/affiliate-proofs", "proof_" + paymentOrderId);
    }

    public void delete(String publicId) {
        if (!cloudinaryEnabled || publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, Collections.emptyMap());
            log.info("Image supprimee - public_id={}", publicId);
        } catch (Exception e) {
            log.warn("Impossible de supprimer l'image {} : {}", publicId, e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw OtakuException.badRequest("Le fichier est vide.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw OtakuException.badRequest("Fichier trop volumineux (max 5 MB).");
        }

        String contentType = file.getContentType();
        boolean valid = Arrays.asList(ALLOWED_TYPES).contains(contentType);
        if (!valid) {
            throw OtakuException.badRequest(
                "Format non supporte. Formats acceptes : JPEG, PNG, WEBP, GIF"
            );
        }
    }

    private boolean isUnknownHost(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof UnknownHostException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public record CloudinaryUploadResult(String url, String publicId) {
        public static CloudinaryUploadResult empty() {
            return new CloudinaryUploadResult(null, null);
        }

        public boolean isUploaded() {
            return url != null && !url.isBlank();
        }
    }
}
