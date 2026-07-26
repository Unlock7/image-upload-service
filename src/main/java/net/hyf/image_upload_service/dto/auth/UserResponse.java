package net.hyf.image_upload_service.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hyf.image_upload_service.model.User;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}