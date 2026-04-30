package prod.nipponhubv1.nipponhubv1.Dto.Response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CountryKpi {
    private Long       countryId;
    private String     countryName;
    private String     countryCode;
    private String     currency;
    private BigDecimal revenue;
    private long       totalOrders;
}
