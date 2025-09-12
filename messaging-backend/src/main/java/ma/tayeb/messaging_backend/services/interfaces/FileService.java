package ma.tayeb.messaging_backend.services.interfaces;

import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    Map<String, String> upload(MultipartFile file);
}
