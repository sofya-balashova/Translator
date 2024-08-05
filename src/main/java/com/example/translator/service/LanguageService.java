package com.example.translator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.TreeMap;

@Service
public class LanguageService {
    private static final Logger logger = LoggerFactory.getLogger(TranslatorService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;
    private TreeMap<String, String> availableLanguages;

    @Value("${rapidapi.key}")
    private String rapidApiKey;

    @Value("${rapidapi.host}")
    private String rapidApiHost;

    @Value("${languageUrl}")
    private String languageUrl;

    public LanguageService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        fetchAvailableLanguages();
    }

    @PostConstruct
    private void fetchAvailableLanguages() {
        try {
            this.availableLanguages = getSupportedLanguages("en");
        } catch (Exception e) {
            logger.error("Не удалось получить список языков", e);
            this.availableLanguages = new TreeMap<>();
        }
    }

    @SneakyThrows// до 3-х повторений с увеличивающимся интервалом при выбросе исключения
    @Retryable(retryFor = { Exception.class }, backoff = @Backoff(delay = 1000, multiplier = 2))
    public TreeMap<String, String> getSupportedLanguages(String target) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(languageUrl).queryParam("target", target);
        URI uri = builder.build().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-host", rapidApiHost);
        headers.set("x-rapidapi-key", rapidApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
        String responseBody = response.getBody();
        logger.info("Получен ответ: {}", responseBody);
        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode languagesNode = rootNode.path("data").path("languages");
                TreeMap<String, String> languagesMap = new TreeMap<>();
                for (JsonNode languageNode : languagesNode) {
                    languagesMap.put(languageNode.path("language").asText(), languageNode.path("name").asText());
                }
                return languagesMap;
            } catch (IOException e) {
                throw new RuntimeException("Ошибка парсинга", e);
            }
        } else {
            throw new RuntimeException("Не удалось получить список языков: " + response.getStatusCode() + " " +
                    response.getBody());
        }
    }

    public boolean isNotSupported(String languageCode) {
        return !availableLanguages.containsKey(languageCode);
    }

    @Recover // если ни одна из попыток не сработает
    public TreeMap<String, String> recover(Exception e) {
        logger.error("Не удалось получить список языков после повторных попыток", e);
        return new TreeMap<>();
    }
}
