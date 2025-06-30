package ma.tayeb.messaging_backend.services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Client s3Client;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public String upload(MultipartFile file) {
        try {
            String fileId = UUID.randomUUID().toString();

            Map<String, String> metadata = new HashMap<>();
            metadata.put("original-filename", file.getOriginalFilename());

            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileId)
                .contentType(file.getContentType())
                .metadata(metadata)
                .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));

            return "http://localhost:9000/" + bucketName + "/" + fileId;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }
} 