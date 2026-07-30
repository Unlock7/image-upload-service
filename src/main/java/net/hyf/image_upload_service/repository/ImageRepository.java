package net.hyf.image_upload_service.repository;

import lombok.RequiredArgsConstructor;
import net.hyf.image_upload_service.model.Image;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ImageRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Image> IMAGE_ROW_MAPPER = (rs, rowNum) ->
            new Image(
                    rs.getLong("id"),
                    rs.getLong("user_id"),
                    rs.getString("title"),
                    rs.getString("storage_key"),
                    rs.getString("content_type"),
                    rs.getLong("file_size"),
                    rs.getString("tags"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            );

    public Image save(Image image) {
        String sql = """
                INSERT INTO images (
                    user_id,
                    title,
                    storage_key,
                    content_type,
                    file_size,
                    tags,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)
                RETURNING id, user_id, title, storage_key,
                          content_type, file_size, tags, created_at
                """;

        return jdbcTemplate.queryForObject(
                sql,
                IMAGE_ROW_MAPPER,
                image.getUserId(),
                image.getTitle(),
                image.getStorageKey(),
                image.getContentType(),
                image.getFileSize(),
                image.getTags(),
                image.getCreatedAt()
        );
    }

    public Optional<Image> findById(Long imageId) {
        String sql = """
                SELECT id, user_id, title, storage_key,
                       content_type, file_size, tags, created_at
                FROM images
                WHERE id = ?
                """;

        try {
            return Optional.ofNullable(
                    jdbcTemplate.queryForObject(
                            sql,
                            IMAGE_ROW_MAPPER,
                            imageId
                    )
            );
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public List<Image> findByUserId(Long userId) {
        String sql = """
                SELECT id, user_id, title, storage_key,
                       content_type, file_size, tags, created_at
                FROM images
                WHERE user_id = ?
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(sql, IMAGE_ROW_MAPPER, userId);
    }

    public List<Image> findLatestPublicImages(int limit) {
        String sql = """
                SELECT id, user_id, title, storage_key,
                       content_type, file_size, tags, created_at
                FROM images
                ORDER BY created_at DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, IMAGE_ROW_MAPPER, limit);
    }

    public int deleteByIdAndUserId(Long imageId, Long userId) {
        String sql = """
                DELETE FROM images
                WHERE id = ?
                  AND user_id = ?
                """;

        return jdbcTemplate.update(sql, imageId, userId);
    }
}