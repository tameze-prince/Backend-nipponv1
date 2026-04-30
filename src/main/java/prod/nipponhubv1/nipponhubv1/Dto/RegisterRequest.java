package prod.nipponhubv1.nipponhubv1.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank 
    private String firstName;
    @NotBlank 
    private String lastName;
    @Email @NotBlank 
    private String email;
    @NotBlank 
    private String phone;
    @NotBlank @Size(min=8) 
    private String password;
    private Long countryId;
    private Long cityId;
    private String referralCode;   // Lien partenaire optionnel
}
