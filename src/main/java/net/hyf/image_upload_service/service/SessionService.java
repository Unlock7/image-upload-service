package net.hyf.image_upload_service.service;

import lombok.RequiredArgsConstructor;
import net.hyf.image_upload_service.exception.UnauthorizedException;
import net.hyf.image_upload_service.model.Session;
import net.hyf.image_upload_service.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final Duration SESSION_DURATION = Duration.ofHours(24);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SessionRepository sessionRepository;

    @Transactional
    public Session createSession(Long userId) {
        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(SESSION_DURATION);

        Session session = new Session(
                null,
                generateToken(),
                userId,
                createdAt,
                expiresAt
        );

        return sessionRepository.save(session);
    }

    public void deleteSession(String token) {
        sessionRepository.deleteByToken(token);
    }

    private String generateToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }
    public Long requireValidUserId(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Authentication is required");
        }

        return sessionRepository.findValidByToken(token)
                .map(Session::getUserId)
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "Session is invalid or expired"
                        )
                );
    }
}