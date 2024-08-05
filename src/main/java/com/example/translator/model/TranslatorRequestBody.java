package com.example.translator.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TranslatorRequestBody {
    private String text;
    private String sourceLang;
    private String targetLang;
    private String format;

}
