-- Create token table
CREATE TABLE token (
    id INT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(500) UNIQUE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    user_uid VARCHAR(36) NOT NULL,
    CONSTRAINT fk_token_user FOREIGN KEY (user_uid) REFERENCES user (uid) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Create indexes for token lookups
CREATE INDEX idx_token_value ON token (token);

CREATE INDEX idx_token_user ON token (user_uid);

CREATE INDEX idx_token_revoked ON token (revoked);