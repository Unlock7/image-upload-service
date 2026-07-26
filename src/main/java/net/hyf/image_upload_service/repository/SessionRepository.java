package net.hyf.image_upload_service.repository;

import lombok.RequiredArgsConstructor;
import net.hyf.image_upload_service.model.Session;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public Session save(Session session) {
        String sql = """
                INSERT INTO sessions (
                    session_token,
                    user_id,
                    created_at,
                    expires_at
                )
                VALUES (?, ?, ?, ?)
                RETURNING id
                """;

        Long id = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                session.getSessionToken(),
                session.getUserId(),
                Timestamp.from(session.getCreatedAt()),
                Timestamp.from(session.getExpiresAt())
        );

        session.setId(id);
        return session;
    }

    public Optional<Session> findValidByToken(String token) {
        String sql = """
                SELECT id, session_token, user_id, created_at, expires_at
                FROM sessions
                WHERE session_token = ?
                  AND expires_at > CURRENT_TIMESTAMP
                """;

        return jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> new Session(
                        resultSet.getLong("id"),
                        resultSet.getString("session_token"),
                        resultSet.getLong("user_id"),
                        resultSet.getTimestamp("created_at").toInstant(),
                        resultSet.getTimestamp("expires_at").toInstant()
                ),
                token
        ).stream().findFirst();
    }

    public void deleteByToken(String token) {
        String sql = """
                DELETE FROM sessions
                WHERE session_token = ?
                """;

        jdbcTemplate.update(sql, token);
    }

    public void deleteExpiredSessions() {
        String sql = """
                DELETE FROM sessions
                WHERE expires_at <= ?
                """;

        jdbcTemplate.update(sql, Timestamp.from(Instant.now()));
    }
}
