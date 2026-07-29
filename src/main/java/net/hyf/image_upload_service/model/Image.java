package net.hyf.image_upload_service.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Image {
    private Long id;
    private Long userId;
    private String title;
    private String storageKey;
    private String contentType;
    private Long fileSize;

    private String tags;
    private LocalDateTime createdAt;
}
