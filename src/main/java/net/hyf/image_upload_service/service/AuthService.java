package net.hyf.image_upload_service.service;

import lombok.RequiredArgsConstructor;
import net.hyf.image_upload_service.dto.auth.LoginRequest;
import net.hyf.image_upload_service.dto.auth.RegisterRequest;
import net.hyf.image_upload_service.dto.auth.UserResponse;
import net.hyf.image_upload_service.exception.ConflictException;
import net.hyf.image_upload_service.exception.UnauthorizedException;
import net.hyf.image_upload_service.model.Session;
import net.hyf.image_upload_service.model.User;
import net.hyf.image_upload_service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already registered");
        }

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        User user = new User(
                null,
                username,
                email,
                passwordEncoder.encode(request.getPassword()),
                Instant.now()
        );

        User savedUser = userRepository.save(user);

        return UserResponse.from(savedUser);
    }

    @Transactional
    public Session login(LoginRequest request) {
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "Invalid email or password"
                        )
                );

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new UnauthorizedException(
                    "Invalid email or password"
            );
        }

        return sessionService.createSession(user.getId());
    }
}