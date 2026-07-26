package net.hyf.image_upload_service.service;

import lombok.RequiredArgsConstructor;
import net.hyf.image_upload_service.dto.auth.RegisterRequest;
import net.hyf.image_upload_service.dto.auth.UserResponse;
import net.hyf.image_upload_service.exception.ConflictException;
import net.hyf.image_upload_service.model.User;
import net.hyf.image_upload_service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists");
        }

        String passwordHash = passwordEncoder.encode(
                request.getPassword()
        );

        User user = new User(
                null,
                username,
                email,
                passwordHash,
                LocalDateTime.now()
        );

        User savedUser = userRepository.save(user);

        return UserResponse.from(savedUser);
    }
}