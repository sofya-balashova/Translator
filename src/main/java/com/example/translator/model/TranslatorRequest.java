package com.example.translator.model;

import lombok.*;
import java.sql.Timestamp;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TranslatorRequest {
    private Long id;
    private String ipAddress;
    private String originalText;
    private String translatedText;
    private Timestamp createdAt;
}
