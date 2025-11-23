-- Create chatview table
CREATE TABLE IF NOT EXISTS chatview (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create junction table for many-to-many relationship between chatview and user
CREATE TABLE IF NOT EXISTS chatview_users (
    chatview_id BIGINT NOT NULL,
    user_uid VARCHAR(36) NOT NULL,
    PRIMARY KEY (chatview_id, user_uid),
    FOREIGN KEY (chatview_id) REFERENCES chatview(id) ON DELETE CASCADE,
    FOREIGN KEY (user_uid) REFERENCES user(uid) ON DELETE CASCADE,
    INDEX idx_user_uid (user_uid),
    INDEX idx_chatview_id (chatview_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create user_session table to track online/offline status
CREATE TABLE IF NOT EXISTS user_session (
    user_uid VARCHAR(36) PRIMARY KEY,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    websocket_session_id VARCHAR(255),
    last_seen DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    FOREIGN KEY (user_uid) REFERENCES user(uid) ON DELETE CASCADE,
    INDEX idx_is_online (is_online),
    INDEX idx_websocket_session_id (websocket_session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
