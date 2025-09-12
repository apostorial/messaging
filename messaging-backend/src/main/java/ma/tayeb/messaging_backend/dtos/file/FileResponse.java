package ma.tayeb.messaging_backend.dtos.file;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileResponse {
    String fileUrl;
    String fileType;
    String originalName;
    Long size;
}
