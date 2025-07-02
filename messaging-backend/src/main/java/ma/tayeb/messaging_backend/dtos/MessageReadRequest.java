package ma.tayeb.messaging_backend.dtos;

import lombok.Builder;
import lombok.Getter;
import ma.tayeb.messaging_backend.enums.ReaderType;

@Getter @Builder
public class MessageReadRequest {
    private ReaderType readerType;
}
