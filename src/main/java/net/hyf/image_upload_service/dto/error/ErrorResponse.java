package net.hyf.image_upload_service.dto.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;
    private Instant timestamp;
}