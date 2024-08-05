package com.example.translator.repository;

import com.example.translator.model.TranslatorRequest;

import java.util.List;

public interface TranslatorRequestRepository {
    void save(TranslatorRequest request);
    List<TranslatorRequest> findAll();
}
