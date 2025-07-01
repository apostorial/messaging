package ma.tayeb.messaging_backend.dtos;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class MessageEditRequest {
    private UUID id;
    private String content;
} 