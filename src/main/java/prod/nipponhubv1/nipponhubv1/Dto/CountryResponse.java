package prod.nipponhubv1.nipponhubv1.Dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountryResponse {

    private Long id;
    private String name;
    private String code;
    private String currency;
    private boolean active;
    private LocalDateTime createdAt;
}
