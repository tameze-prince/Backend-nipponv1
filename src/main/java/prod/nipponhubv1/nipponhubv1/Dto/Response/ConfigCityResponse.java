package prod.nipponhubv1.nipponhubv1.Dto.Response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfigCityResponse {
    private Long id;
    private Long countryId;
    private String name;
    private boolean active;
    private LocalDateTime createdAt;
}
