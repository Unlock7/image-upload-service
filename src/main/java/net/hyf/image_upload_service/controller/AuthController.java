package net.hyf.image_upload_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hyf.image_upload_service.dto.auth.AuthResponse;
import net.hyf.image_upload_service.dto.auth.LoginRequest;
import net.hyf.image_upload_service.dto.auth.RegisterRequest;
import net.hyf.image_upload_service.dto.auth.UserResponse;
import net.hyf.image_upload_service.model.Session;
import net.hyf.image_upload_service.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        UserResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        Session session = authService.login(request);

        ResponseCookie cookie = ResponseCookie
                .from("SESSION_ID", session.getSessionToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofHours(24))
                .build();

        AuthResponse response = new AuthResponse(
                "Login successful",
                session.getExpiresAt()
        );

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);

    }

}