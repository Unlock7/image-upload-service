package net.hyf.image_upload_service.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class AuthResponse {

    private String message;
    private Instant expiresAt;
}