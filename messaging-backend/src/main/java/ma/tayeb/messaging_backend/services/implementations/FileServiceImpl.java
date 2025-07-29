package ma.tayeb.messaging_backend.services.implementations;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import ma.tayeb.messaging_backend.services.interfaces.FileService;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final S3Client s3Client;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Override
    public String upload(MultipartFile file) {
        try {
            String fileId = UUID.randomUUID().toString();

            Map<String, String> metadata = new HashMap<>();
            metadata.put("original-filename", file.getOriginalFilename());
            metadata.put("content-type", file.getContentType());

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileId)
                    .contentType(file.getContentType())
                    .metadata(metadata)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            String fileUrl = endpoint + "/" + bucketName + "/" + fileId;

            return fileUrl;
        } catch (IOException exception) {
            throw new RuntimeException("Failed to upload file:" + exception.getMessage());
        }
    }
    
}
