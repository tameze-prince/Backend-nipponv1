package prod.nipponhubv1.nipponhubv1.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsappContactResponse {

    private Long id;
    private Long cityId;
    private String cityName;
    private String whatsappNumber;
    private String label;
    private boolean active;
}
