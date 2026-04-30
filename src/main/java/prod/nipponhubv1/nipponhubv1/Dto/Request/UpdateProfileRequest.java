package prod.nipponhubv1.nipponhubv1.Dto.Request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String currentPassword;   // requis pour changer le mot de passe
    private String newPassword;
    private Long   cityId;
}
