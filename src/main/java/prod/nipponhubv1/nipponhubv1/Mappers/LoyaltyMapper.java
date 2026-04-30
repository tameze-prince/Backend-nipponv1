package prod.nipponhubv1.nipponhubv1.Mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import prod.nipponhubv1.nipponhubv1.Dto.LoyaltyAccountResponse;
import prod.nipponhubv1.nipponhubv1.Models.LoyaltyAccount;

@Mapper(componentModel = "spring")
public interface LoyaltyMapper {

    @Mapping(target = "gradeName",     source = "grade.name")
    @Mapping(target = "gradeColorHex", source = "grade.colorHex")
    @Mapping(target = "badgeImageUrl", source = "grade.badgeImageUrl")
    LoyaltyAccountResponse toResponse(LoyaltyAccount account);
}
