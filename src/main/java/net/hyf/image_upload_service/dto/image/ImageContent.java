package net.hyf.image_upload_service.dto.image;

public record ImageContent(
        byte[] data,
        String contentType
) {
}