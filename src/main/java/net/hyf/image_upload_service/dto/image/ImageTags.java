package net.hyf.image_upload_service.dto.image;

import java.util.List;

public record ImageTags(
        List<String> objects,
        List<String> tags,
        List<String> colors
) {
    public static ImageTags empty() {
        return new ImageTags(
                List.of(),
                List.of(),
                List.of()
        );
    }
}