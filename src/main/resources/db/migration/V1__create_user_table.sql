-- Create user table
CREATE TABLE user (
    uid VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    avatar_link VARCHAR(500) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Create indexes for better query performance
CREATE INDEX idx_user_name ON user (name);

CREATE INDEX idx_user_email ON user (email);