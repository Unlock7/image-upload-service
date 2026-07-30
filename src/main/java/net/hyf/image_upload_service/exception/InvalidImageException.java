package net.hyf.image_upload_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidImageException extends RuntimeException {

    private final HttpStatus status;

    public InvalidImageException(
            HttpStatus status,
            String message
    ) {
        super(message);
        this.status = status;
    }
}