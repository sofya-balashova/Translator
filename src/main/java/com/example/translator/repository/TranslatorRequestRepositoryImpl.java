package com.example.translator.repository;

import com.example.translator.model.TranslatorRequest;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@AllArgsConstructor
public class TranslatorRequestRepositoryImpl implements TranslatorRequestRepository {
    private final JdbcTemplate jdbcTemplate;

    public void save(TranslatorRequest request) {
        String sql = "INSERT INTO translator_requests (ip_address, original_text, translated_text) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, request.getIpAddress(), request.getOriginalText(), request.getTranslatedText());
    }

    public List<TranslatorRequest> findAll() {
        String sql = "SELECT * FROM translator_requests";
        RowMapper<TranslatorRequest> rowMapper = (rs, rowNum) -> {
            TranslatorRequest translationRequest = new TranslatorRequest();
            translationRequest.setId(rs.getLong("id"));
            translationRequest.setIpAddress(rs.getString("ip_address"));
            translationRequest.setOriginalText(rs.getString("original_text"));
            translationRequest.setTranslatedText(rs.getString("translated_text"));
            translationRequest.setCreatedAt(rs.getTimestamp("created_at"));
            return translationRequest;
        };
        return jdbcTemplate.query(sql, rowMapper);
    }
}
