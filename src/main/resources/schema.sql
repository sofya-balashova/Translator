CREATE TABLE IF NOT EXISTS translator_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ip_address VARCHAR(255),
    original_text TEXT,
    translated_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
