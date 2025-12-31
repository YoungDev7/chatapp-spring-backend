-- Create message table
CREATE TABLE message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    text TEXT NOT NULL,
    user_uid VARCHAR(36) NOT NULL,
    chatview_id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_message_user FOREIGN KEY (user_uid) REFERENCES user (uid) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_message_chatview FOREIGN KEY (chatview_id) REFERENCES chatview (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Create indexes for better query performance
CREATE INDEX idx_message_user ON message (user_uid);

CREATE INDEX idx_message_chatview ON message (chatview_id);

CREATE INDEX idx_message_created_at ON message (created_at);