package prod.nipponhubv1.nipponhubv1.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsappContactRequest {

    @NotNull(message = "L'ID de la ville est requis")
    private Long cityId;

    @NotBlank(message = "Le numéro WhatsApp est requis")
    @Pattern(regexp = "^[+]?[0-9]{1,15}$", message = "Le numéro WhatsApp doit être valide")
    private String whatsappNumber;

    @Size(max = 100, message = "Le libellé ne doit pas dépasser 100 caractères")
    private String label;

    private boolean active = true;
}
