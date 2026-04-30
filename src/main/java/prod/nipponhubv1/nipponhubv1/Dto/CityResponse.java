package prod.nipponhubv1.nipponhubv1.Dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CityResponse {

    private Long id;
    private Long countryId;
    private String countryName;
    private String name;
    private boolean active;
    private LocalDateTime createdAt;
    private List<WhatsappContactResponse> whatsappContacts;
}
