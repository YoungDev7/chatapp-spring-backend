-- Add chatview_id column to message table
ALTER TABLE message 
ADD COLUMN chatview_id BIGINT NOT NULL AFTER user_uid,
ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER chatview_id;

-- Add foreign key constraint
ALTER TABLE message
ADD CONSTRAINT fk_message_chatview
FOREIGN KEY (chatview_id) REFERENCES chatview(id) ON DELETE CASCADE;

-- Add index for better query performance
ALTER TABLE message
ADD INDEX idx_chatview_id (chatview_id),
ADD INDEX idx_created_at (created_at);
