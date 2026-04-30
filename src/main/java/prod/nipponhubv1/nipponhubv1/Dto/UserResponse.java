package prod.nipponhubv1.nipponhubv1.Dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;
import prod.nipponhubv1.nipponhubv1.Models.Enums.Role;

@Data @Builder
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String avatarUrl;
    private Role role;
    private Long countryId;
    private String countryName;
    private Long cityId;
    private String cityName;
    private boolean active;
    private LocalDateTime createdAt;
    // Loyalty fields (null if not a CLIENT)
    private Integer loyaltyPoints;
    private String loyaltyGrade;

    public void setLoyaltyGrade(String name) {
        this.loyaltyGrade = name;
    }
}
