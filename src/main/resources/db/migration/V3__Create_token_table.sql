CREATE TABLE token (
    id INT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(512) UNIQUE,
    revoked BOOLEAN DEFAULT FALSE,
    user_uid VARCHAR(36),
    
    CONSTRAINT fk_token_user FOREIGN KEY (user_uid) REFERENCES user(UID) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE token COMMENT = 'JWT refresh tokens for user authentication';