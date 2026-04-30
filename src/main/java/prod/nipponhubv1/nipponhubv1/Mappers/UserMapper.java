package prod.nipponhubv1.nipponhubv1.Mappers;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import prod.nipponhubv1.nipponhubv1.Dto.UserResponse;
import prod.nipponhubv1.nipponhubv1.Models.OurUser;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "countryName", source = "country.name")
    @Mapping(target = "countryId",   source = "country.id")
    @Mapping(target = "cityName",    source = "city.name")
    @Mapping(target = "cityId",      source = "city.id")
    UserResponse toResponse(OurUser user);

    List<UserResponse> toResponseList(List<OurUser> users);
}
