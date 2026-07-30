package net.hyf.image_upload_service.controller;

import lombok.RequiredArgsConstructor;
import net.hyf.image_upload_service.dto.image.ImageResponse;
import net.hyf.image_upload_service.service.ImageService;
import net.hyf.image_upload_service.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;
    private final SessionService sessionService;

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ImageResponse> upload(
            @CookieValue(
                    name = "SESSION_ID",
                    required = false
            )
            String sessionToken,

            @RequestParam("file")
            MultipartFile file,

            @RequestParam(
                    name = "title",
                    required = false
            )
            String title
    ) {
        Long userId = sessionService
                .requireValidUserId(sessionToken);

        ImageResponse response = imageService.upload(
                userId,
                file,
                title
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}