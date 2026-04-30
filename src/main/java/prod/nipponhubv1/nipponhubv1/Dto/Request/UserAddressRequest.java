package prod.nipponhubv1.nipponhubv1.Dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserAddressRequest {

    @NotBlank
    private String label;

    @NotBlank
    private String firstName;

    private String lastName;

    @NotBlank
    private String phone;

    @NotBlank
    private String address;

    @NotBlank
    private String city;

    private String state;

    @NotBlank
    private String country;

    private String postalCode;

    private Boolean isDefault;
}
