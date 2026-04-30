package prod.nipponhubv1.nipponhubv1.Dto.Response;

import lombok.Builder;
import lombok.Data;
import prod.nipponhubv1.nipponhubv1.Models.Enums.Role;

@Data @Builder
public class AuthResponse {
    private String  accessToken;
    private String  refreshToken;
    private String  tokenType = "Bearer";
    private long    expiresIn;       // secondes
    private Role    role;
    private Long    userId;
    private String  fullName;
    private String  avatarUrl;
}
