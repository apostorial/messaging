package ma.tayeb.messaging_backend.dtos;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class MessageEditRequest {
    private String content;
} 