package com.example.translator;

import com.example.translator.controller.TranslatorController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.translator.model.TranslatorRequestBody;
import com.example.translator.service.LanguageService;
import com.example.translator.service.TranslatorService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import java.util.TreeMap;

@SpringBootTest
class TranslatorApplicationTests {
    private TranslatorController translatorController;
    @Mock
    private LanguageService languageService;
    @Mock
    private TranslatorService translationService;
    @Mock
    private HttpServletRequest request;
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        translatorController = new TranslatorController(languageService, translationService);
    }

    @Test
    void contextLoads() {
    }

    @Test
    public void testTranslate_SupportedLanguages_Success() {
        TranslatorRequestBody requestBody = new TranslatorRequestBody(
                "Hello", "en", "es", "text");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(languageService.isNotSupported("en")).thenReturn(false);
        when(languageService.isNotSupported("es")).thenReturn(false);
        when(translationService.translate("Hello", "en", "es", "127.0.0.1"))
                .thenReturn("Hola");
        ResponseEntity<String> response = translatorController.translate(requestBody, request);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Hola", response.getBody());
    }

    @Test
    public void testTranslate_UnsupportedSourceLanguage() {
        TranslatorRequestBody requestBody = new TranslatorRequestBody(
                "Hello", "xx", "es", "text");
        when(languageService.isNotSupported("xx")).thenReturn(true);
        ResponseEntity<String> response = translatorController.translate(requestBody, request);
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Не найден язык исходного сообщения", response.getBody());
    }

    @Test
    public void testTranslate_UnsupportedTargetLanguage() {
        TranslatorRequestBody requestBody = new TranslatorRequestBody(
                "Hello", "en", "xx", "text");
        when(languageService.isNotSupported("en")).thenReturn(false);
        when(languageService.isNotSupported("xx")).thenReturn(true);
        ResponseEntity<String> response = translatorController.translate(requestBody, request);
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Не найден целевой язык сообщения", response.getBody());
    }

    @Test
    public void testTranslate_TranslationServiceError() {
        TranslatorRequestBody requestBody = new TranslatorRequestBody(
                "Hello", "en", "es", "text");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(languageService.isNotSupported("en")).thenReturn(false);
        when(languageService.isNotSupported("es")).thenReturn(false);
        when(translationService.translate(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Ошибка перевода"));
        ResponseEntity<String> response = translatorController.translate(requestBody, request);
        assertEquals(500, response.getStatusCodeValue());
        assertEquals("Ошибка перевода", response.getBody());
    }

    @Test
    public void testGetAvailableLanguages_Success() {
        TreeMap<String, String> languages = new TreeMap<>();
        languages.put("en", "English");
        languages.put("es", "Spanish");
        when(languageService.getSupportedLanguages("en")).thenReturn(languages);
        ResponseEntity<TreeMap<String, String>> response = translatorController.getAvailableLanguages();
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(languages, response.getBody());
    }

    @Test
    public void testGetAvailableLanguages_Error() {
        when(languageService.getSupportedLanguages("en"))
                .thenThrow(new RuntimeException("Ошибка получения языков"));
        ResponseEntity<TreeMap<String, String>> response = translatorController.getAvailableLanguages();
        assertEquals(500, response.getStatusCodeValue());
        assertNull(response.getBody());
    }
}
