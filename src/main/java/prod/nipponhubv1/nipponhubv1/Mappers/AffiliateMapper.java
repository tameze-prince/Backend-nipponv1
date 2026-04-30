package prod.nipponhubv1.nipponhubv1.Mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import prod.nipponhubv1.nipponhubv1.Dto.AffiliateCommissionResponse;
import prod.nipponhubv1.nipponhubv1.Dto.AffiliateStatsResponse;
import prod.nipponhubv1.nipponhubv1.Models.AffiliateCommission;
import prod.nipponhubv1.nipponhubv1.Models.AffiliateProfile;

@Mapper(componentModel = "spring")
public interface AffiliateMapper {

    @Mapping(target = "totalClicks",        ignore = true)  // calculé en service
    @Mapping(target = "totalOrders",        ignore = true)
    @Mapping(target = "recentCommissions",  ignore = true)
    AffiliateStatsResponse toStatsResponse(AffiliateProfile profile);

    @Mapping(target = "orderId", source = "order.id")
    AffiliateCommissionResponse toCommissionResponse(AffiliateCommission commission);
}
