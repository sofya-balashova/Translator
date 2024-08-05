package com.example.translator.controller;

import com.example.translator.model.TranslatorRequestBody;
import com.example.translator.service.TranslatorService;
import com.example.translator.service.LanguageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.TreeMap;
import static com.example.translator.service.TranslatorService.logger;

@Tag(name = "Translator | Переводчик")
@RestController
@RequestMapping("/translator")
@AllArgsConstructor
public class TranslatorController {
    private final LanguageService languageService;
    private final TranslatorService translationService;

    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Результат получен"),
            @ApiResponse(responseCode = "404", description = "Не найдено"),
            @ApiResponse(responseCode = "500", description = "Серверная ошибка")})
    @PostMapping("/translate")
    public ResponseEntity<String> translate(@RequestBody TranslatorRequestBody body, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        if (languageService.isNotSupported(body.getSourceLang())) {
            return ResponseEntity.badRequest().body("Не найден язык исходного сообщения");
        }
        if (languageService.isNotSupported(body.getTargetLang())) {
            return ResponseEntity.badRequest().body("Не найден целевой язык сообщения");
        }
        try {
            String translatedText = translationService.translate(body.getText(), body.getSourceLang(),
                    body.getTargetLang(), ipAddress);
            return ResponseEntity.ok(translatedText);
        } catch (Exception e) {
            logger.error("Ошибка перевода ", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/languages")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<TreeMap<String, String>> getAvailableLanguages() {
        try {
            TreeMap<String, String> languages = languageService.getSupportedLanguages("en");
            return ResponseEntity.ok(languages);
        } catch (Exception e) {
            logger.error("Ошибка получения списка языков", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }
}
