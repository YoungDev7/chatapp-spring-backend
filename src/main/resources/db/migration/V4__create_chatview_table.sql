-- Create chatview table
CREATE TABLE chatview (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Create chatview_users join table for many-to-many relationship
CREATE TABLE chatview_users (
    chatview_id VARCHAR(36) NOT NULL,
    user_uid VARCHAR(36) NOT NULL,
    PRIMARY KEY (chatview_id, user_uid),
    CONSTRAINT fk_chatview_users_chatview FOREIGN KEY (chatview_id) REFERENCES chatview (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_chatview_users_user FOREIGN KEY (user_uid) REFERENCES user (uid) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Create indexes for better join performance
CREATE INDEX idx_chatview_users_chatview ON chatview_users (chatview_id);

CREATE INDEX idx_chatview_users_user ON chatview_users (user_uid);