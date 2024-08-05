package com.example.translator.service;

import com.example.translator.model.TranslatorRequest;
import com.example.translator.repository.TranslatorRequestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslatorService {
    private final RestTemplate restTemplate;
    private final TranslatorRequestRepository repository;
    // максимальное количество работающих одновременно потоков
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public static final Logger logger = LoggerFactory.getLogger(TranslatorService.class);

    @Value("${rapidapi.key}")
    private String rapidApiKey;

    @Value("${rapidapi.host}")
    private String rapidApiHost;

    @Value("${baseUrl}")
    private String baseUrl;

    public String translate(String originalText, String sourceLang, String targetLang, String ipAddress) {
        String[] words = originalText.split(" ");
        List<CompletableFuture<String>> futures = Stream.of(words).map(word -> CompletableFuture.supplyAsync(() ->
                translateWord(word, sourceLang, targetLang), executor)).toList();
        List<String> translatedWords = futures.stream().map(this::getFutureResult).collect(Collectors.toList());
        String translatedText = String.join(" ", translatedWords);
        TranslatorRequest request = new TranslatorRequest();
        request.setIpAddress(ipAddress);
        request.setOriginalText(originalText);
        request.setTranslatedText(translatedText);
        repository.save(request);
        return translatedText;
    }

    private String getFutureResult(CompletableFuture<String> future) {
        try {
            return future.get();
        } catch (Exception e) {
            System.err.println("Не удалось получить результат перевода: " + e.getMessage());
            return "";
        }
    }

    // из-за ограничения количества запросов в секунду бывает исключение - повторяем при выбросе исключения до 3-х раз
    @SneakyThrows
    @Retryable(
            retryFor = { Exception.class },
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public String translateWord(String word, String sourceLang, String targetLang) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("q", word)
                .queryParam("source", sourceLang)
                .queryParam("target", targetLang);
        URI uri = builder.build().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-rapidapi-host", rapidApiHost);
        headers.set("x-rapidapi-key", rapidApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        logger.info("Посылаем запрос URL: " + uri);
        logger.info("Заголовки запросов: " + headers);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
        String responseBody = response.getBody();
        logger.info("Ответ: " + responseBody);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode translations = root.path("data").path("translations");
        if (translations.isArray() && !translations.isEmpty()) {
            String translatedText = translations.get(0).path("translatedText").asText();
            return URLDecoder.decode(translatedText, StandardCharsets.UTF_8);
        } else {
            logger.warn("Перевод не найден");
            return "";
        }
    }
}