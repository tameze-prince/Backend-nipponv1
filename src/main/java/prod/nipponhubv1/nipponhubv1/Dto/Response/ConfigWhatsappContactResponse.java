package prod.nipponhubv1.nipponhubv1.Dto.Response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfigWhatsappContactResponse {
    private Long id;
    private Long cityId;
    private String whatsappNumber;
    private String label;
    private boolean active;
}
