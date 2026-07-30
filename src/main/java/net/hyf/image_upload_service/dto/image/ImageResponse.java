package net.hyf.image_upload_service.dto.image;

import net.hyf.image_upload_service.model.Image;

import java.time.LocalDateTime;

public record ImageResponse(
        Long id,
        String title,
        String contentType,
        Long fileSize,
        ImageTags tags,
        String imageUrl,
        LocalDateTime createdAt
) {
    public static ImageResponse from(
            Image image,
            ImageTags tags
    ) {
        return new ImageResponse(
                image.getId(),
                image.getTitle(),
                image.getContentType(),
                image.getFileSize(),
                tags,
                "/api/v1/images/"
                        + image.getId()
                        + "/content",
                image.getCreatedAt()
        );
    }
}