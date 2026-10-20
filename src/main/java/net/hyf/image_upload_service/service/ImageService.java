package net.hyf.image_upload_service.service;

import lombok.RequiredArgsConstructor;
import net.hyf.image_upload_service.dto.image.ImageContent;
import org.springframework.web.server.ResponseStatusException;
import net.hyf.image_upload_service.dto.image.ImageResponse;
import net.hyf.image_upload_service.dto.image.ImageTags;
import net.hyf.image_upload_service.exception.InvalidImageException;
import net.hyf.image_upload_service.model.Image;
import net.hyf.image_upload_service.repository.ImageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final long MAX_FILE_SIZE =
            10L * 1024L * 1024L;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private static final String EMPTY_TAGS_JSON = """
            {
              "objects": [],
              "tags": [],
              "colors": []
            }
            """;

    private final ImageRepository imageRepository;
    private final ObjectStorageService objectStorageService;

    public ImageResponse upload(
            Long userId,
            MultipartFile file,
            String title
    ) {
        validate(file);

        String contentType = file.getContentType();
        String storageKey = createStorageKey(
                userId,
                contentType
        );

        byte[] fileBytes;

        try {
            fileBytes = file.getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Could not read uploaded image",
                    exception
            );
        }

        objectStorageService.upload(
                storageKey,
                fileBytes,
                contentType
        );

        try {
            Image image = new Image(
                    null,
                    userId,
                    normalizeTitle(title),
                    storageKey,
                    contentType,
                    file.getSize(),
                    EMPTY_TAGS_JSON,
                    LocalDateTime.now()
            );

            Image savedImage = imageRepository.save(image);

            return ImageResponse.from(
                    savedImage,
                    ImageTags.empty()
            );
        } catch (RuntimeException exception) {
            cleanupFailedUpload(storageKey);
            throw exception;
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException(
                    HttpStatus.BAD_REQUEST,
                    "An image file is required"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidImageException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Image must not exceed 10 MB"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidImageException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only JPEG, PNG, and WebP images are supported"
            );
        }
    }

    private String createStorageKey(
            Long userId,
            String contentType
    ) {
        String extension = EXTENSIONS.get(contentType);

        return "users/"
                + userId
                + "/"
                + UUID.randomUUID()
                + extension;
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }

        return title.trim();
    }

    private void cleanupFailedUpload(String storageKey) {
        try {
            objectStorageService.delete(storageKey);
        } catch (RuntimeException cleanupException) {

        }
    }
    public ImageContent getContent(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Image not found"
                        )
                );

        byte[] data = objectStorageService.download(
                image.getStorageKey()
        );

        return new ImageContent(
                data,
                image.getContentType()
        );
    }
}
