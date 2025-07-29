package ma.tayeb.messaging_backend.services.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    String upload(MultipartFile file);
}
