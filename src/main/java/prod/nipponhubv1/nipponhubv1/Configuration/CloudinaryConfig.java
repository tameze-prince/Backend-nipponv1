package prod.nipponhubv1.nipponhubv1.Configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.cloudinary.utils.ObjectUtils;

import com.cloudinary.Cloudinary;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

//     CLOUDINARY_CLOUD_NAME="NipponFIle"
// CLOUDINARY_API_KEY=195126157196771
// CLOUDINARY_API_SECRET="xniQHz-_YEzANTzywcj6zYfhLcE"

    // @Bean
    // public Cloudinary cloudinary() {
    //     return new Cloudinary(ObjectUtils.asMap(
    //         "cloud_name", cloudName,
    //         "api_key",    apiKey,
    //         "api_secret", apiSecret,
    //         "secure",     true      // HTTPS uniquement
    //     ));
    // }

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "djusjnutn",
            "api_key",    "195126157196771",
            "api_secret", "xniQHz-_YEzANTzywcj6zYfhLcE"
            // "secure",     true      // HTTPS uniquement
        ));
    }
}
