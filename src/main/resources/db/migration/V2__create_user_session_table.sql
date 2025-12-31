-- Create user_session table
CREATE TABLE user_session (
    user_uid VARCHAR(36) PRIMARY KEY,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    websocket_session_id VARCHAR(255) NULL,
    last_seen DATETIME(6) NULL,
    CONSTRAINT fk_user_session_user FOREIGN KEY (user_uid) REFERENCES user (uid) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Create index for websocket session lookups
CREATE INDEX idx_user_session_websocket ON user_session (websocket_session_id);