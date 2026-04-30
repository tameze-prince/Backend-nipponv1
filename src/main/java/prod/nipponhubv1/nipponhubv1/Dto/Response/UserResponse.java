package prod.nipponhubv1.nipponhubv1.Dto.Response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;
import prod.nipponhubv1.nipponhubv1.Models.Enums.Role;

@Data @Builder
public class UserResponse {
    private Long          id;
    private String        firstName;
    private String        lastName;
    private String        email;
    private String        phone;
    private String        avatarUrl;
    private Role          role;
    private String        countryName;
    private String        countryCode;
    private String        cityName;
    private boolean       active;
    private LocalDateTime createdAt;

    // Complément loyalty (null si pas CLIENT)
    private Integer       loyaltyPoints;
    private String        loyaltyGrade;
}
