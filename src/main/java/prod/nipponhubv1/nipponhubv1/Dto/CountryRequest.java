package prod.nipponhubv1.nipponhubv1.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountryRequest {

    @NotBlank(message = "Le nom du pays est requis")
    @Size(min = 2, max = 100, message = "Le nom doit avoir entre 2 et 100 caractères")
    private String name;

    @NotBlank(message = "Le code du pays est requis")
    @Size(min = 2, max = 5, message = "Le code doit avoir entre 2 et 5 caractères")
    private String code;

    @NotBlank(message = "La devise est requise")
    @Size(min = 3, max = 10, message = "La devise doit avoir entre 3 et 10 caractères")
    private String currency;

    private boolean active = true;
}
