package net.hyf.image_upload_service.repository;

import lombok.RequiredArgsConstructor;
import net.hyf.image_upload_service.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<User> USER_ROW_MAPPER = (resultSet, rowNumber) ->
            new User(
                    resultSet.getLong("id"),
                    resultSet.getString("username"),
                    resultSet.getString("email"),
                    resultSet.getString("password_hash"),
                    resultSet.getTimestamp("created_at").toLocalDateTime()
            );

    public User save(User user) {
        String sql = """
                INSERT INTO users (
                    username,
                    email,
                    password_hash,
                    created_at
                )
                VALUES (?, ?, ?, ?)
                RETURNING id, username, email, password_hash, created_at
                """;

        return jdbcTemplate.queryForObject(
                sql,
                USER_ROW_MAPPER,
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getCreatedAt()
        );
    }

    public Optional<User> findByEmail(String email) {
        String sql = """
                SELECT id, username, email, password_hash, created_at
                FROM users
                WHERE email = ?
                """;

        try {
            User user = jdbcTemplate.queryForObject(
                    sql,
                    USER_ROW_MAPPER,
                    email
            );

            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public boolean existsByEmail(String email) {
        String sql = """
                SELECT COUNT(*)
                FROM users
                WHERE email = ?
                """;

        Long count = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                email
        );

        return count != null && count > 0;
    }

    public boolean existsByUsername(String username) {
        String sql = """
                SELECT COUNT(*)
                FROM users
                WHERE username = ?
                """;

        Long count = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                username
        );

        return count != null && count > 0;
    }
}