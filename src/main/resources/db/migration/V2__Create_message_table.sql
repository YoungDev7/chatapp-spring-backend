CREATE TABLE message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    text TEXT,
    user_uid VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    
    CONSTRAINT fk_message_user FOREIGN KEY (user_uid) REFERENCES user(uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_message_user_uid ON message(user_uid);
CREATE INDEX idx_message_id ON message(id);

ALTER TABLE message COMMENT = 'Chat messages sent by users';