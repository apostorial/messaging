package ma.tayeb.messaging_backend.services.interfaces;

import org.springframework.web.multipart.MultipartFile;

import ma.tayeb.messaging_backend.dtos.file.FileResponse;

public interface FileService {
    FileResponse upload(MultipartFile file);
}
