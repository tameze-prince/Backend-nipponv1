package prod.nipponhubv1.nipponhubv1.Dto.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Le prénom est requis")
    private String firstName;

    @NotBlank(message = "Le nom est requis")
    private String lastName;

    @Email(message = "Email invalide")
    @NotBlank
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Numéro de téléphone invalide")
    private String phone;

    @NotBlank
    @Size(min = 8, message = "Mot de passe minimum 8 caractères")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$",
        message = "Le mot de passe doit contenir une majuscule, un chiffre et un caractère spécial"
    )
    private String password;

    private Long   countryId;
    private Long   cityId;
    private String referralCode;   // Code partenaire optionnel
}
