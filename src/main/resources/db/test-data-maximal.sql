-- Maximal test data for development/testing
-- This file creates 5 chatviews with different states and adds 5 additional users

-- Insert 5 additional test users (test3-test7)
-- Password: 'testuserN' where N is the user number (3-7)
-- Using BCrypt hash for 'testuserthree', 'testuserfour', 'testuserfive', 'testusersix', 'testuserseven'
INSERT IGNORE INTO user (uid, name, password, email) VALUES
('test-user-3-uuid', 'Test User Three', '$2a$10$qFqWK7IQ3k5lPmJK8P1YUe5GZXxQxvYzQZJ8pKvQ5qXZ5qXZ5qXZ5', 'test3@email.com'),
('test-user-4-uuid', 'Test User Four', '$2a$10$qFqWK7IQ3k5lPmJK8P1YUe5GZXxQxvYzQZJ8pKvQ5qXZ5qXZ5qXZ5', 'test4@email.com'),
('test-user-5-uuid', 'Test User Five', '$2a$10$qFqWK7IQ3k5lPmJK8P1YUe5GZXxQxvYzQZJ8pKvQ5qXZ5qXZ5qXZ5', 'test5@email.com'),
('test-user-6-uuid', 'Test User Six', '$2a$10$qFqWK7IQ3k5lPmJK8P1YUe5GZXxQxvYzQZJ8pKvQ5qXZ5qXZ5qXZ5', 'test6@email.com'),
('test-user-7-uuid', 'Test User Seven', '$2a$10$qFqWK7IQ3k5lPmJK8P1YUe5GZXxQxvYzQZJ8pKvQ5qXZ5qXZ5qXZ5', 'test7@email.com');

-- Insert 5 chatviews with different states
INSERT IGNORE INTO chatview (id, name) VALUES
('chatview-ls-0-db-gt-0-uuid', 'ls = 0 db > 0'),
('chatview-ls-gt-db-uuid', 'ls > db'),
('chatview-no-messages-uuid', 'no messages'),
('chatview-ls-eq-db-uuid', 'ls = db'),
('chatview-messages-in-queue-uuid', 'messages in queue');

-- Add test user one to all chatviews (UID will be replaced dynamically)
-- Placeholder: {{TEST_USER_ONE_UID}}
INSERT IGNORE INTO chatview_users (chatview_id, user_uid) VALUES
('chatview-ls-0-db-gt-0-uuid', '{{TEST_USER_ONE_UID}}'),
('chatview-ls-gt-db-uuid', '{{TEST_USER_ONE_UID}}'),
('chatview-no-messages-uuid', '{{TEST_USER_ONE_UID}}'),
('chatview-ls-eq-db-uuid', '{{TEST_USER_ONE_UID}}'),
('chatview-messages-in-queue-uuid', '{{TEST_USER_ONE_UID}}');

-- Distribute other users across chatviews randomly
-- Chatview 1: "ls = 0 db > 0" - Test User One, Three, Five
INSERT IGNORE INTO chatview_users (chatview_id, user_uid) VALUES
('chatview-ls-0-db-gt-0-uuid', 'test-user-3-uuid'),
('chatview-ls-0-db-gt-0-uuid', 'test-user-5-uuid');

-- Chatview 2: "ls > db" - Test User One, Two, Four, Seven
INSERT IGNORE INTO chatview_users (chatview_id, user_uid) VALUES
('chatview-ls-gt-db-uuid', '{{TEST_USER_TWO_UID}}'),
('chatview-ls-gt-db-uuid', 'test-user-4-uuid'),
('chatview-ls-gt-db-uuid', 'test-user-7-uuid');

-- Chatview 3: "no messages" - Test User One, Six
INSERT IGNORE INTO chatview_users (chatview_id, user_uid) VALUES
('chatview-no-messages-uuid', 'test-user-6-uuid');

-- Chatview 4: "ls = db" - Test User One, Three, Four, Six
INSERT IGNORE INTO chatview_users (chatview_id, user_uid) VALUES
('chatview-ls-eq-db-uuid', 'test-user-3-uuid'),
('chatview-ls-eq-db-uuid', 'test-user-4-uuid'),
('chatview-ls-eq-db-uuid', 'test-user-6-uuid');

-- Chatview 5: "messages in queue" - Test User One, Two, Five, Seven
INSERT IGNORE INTO chatview_users (chatview_id, user_uid) VALUES
('chatview-messages-in-queue-uuid', '{{TEST_USER_TWO_UID}}'),
('chatview-messages-in-queue-uuid', 'test-user-5-uuid'),
('chatview-messages-in-queue-uuid', 'test-user-7-uuid');

-- Insert messages for different scenarios
-- Chatview 1: "ls = 0 db > 0" - 5 messages in DB
INSERT INTO message (text, user_uid, chatview_id, created_at) VALUES
('Message 1 in ls=0 db>0', '{{TEST_USER_ONE_UID}}', 'chatview-ls-0-db-gt-0-uuid', NOW()),
('Message 2 in ls=0 db>0', 'test-user-3-uuid', 'chatview-ls-0-db-gt-0-uuid', NOW()),
('Message 3 in ls=0 db>0', '{{TEST_USER_ONE_UID}}', 'chatview-ls-0-db-gt-0-uuid', NOW()),
('Message 4 in ls=0 db>0', 'test-user-5-uuid', 'chatview-ls-0-db-gt-0-uuid', NOW()),
('Message 5 in ls=0 db>0', 'test-user-3-uuid', 'chatview-ls-0-db-gt-0-uuid', NOW());

-- Chatview 2: "ls > db" - 3 messages in DB (simulating that local storage has more)
INSERT INTO message (text, user_uid, chatview_id, created_at) VALUES
('Message 1 in ls>db', '{{TEST_USER_ONE_UID}}', 'chatview-ls-gt-db-uuid', NOW()),
('Message 2 in ls>db', '{{TEST_USER_TWO_UID}}', 'chatview-ls-gt-db-uuid', NOW()),
('Message 3 in ls>db', 'test-user-4-uuid', 'chatview-ls-gt-db-uuid', NOW());

-- Chatview 3: "no messages" - No messages inserted

-- Chatview 4: "ls = db" - 4 messages in DB (simulating that local storage matches)
INSERT INTO message (text, user_uid, chatview_id, created_at) VALUES
('Message 1 in ls=db', '{{TEST_USER_ONE_UID}}', 'chatview-ls-eq-db-uuid', NOW()),
('Message 2 in ls=db', 'test-user-3-uuid', 'chatview-ls-eq-db-uuid', NOW()),
('Message 3 in ls=db', 'test-user-4-uuid', 'chatview-ls-eq-db-uuid', NOW()),
('Message 4 in ls=db', 'test-user-6-uuid', 'chatview-ls-eq-db-uuid', NOW());

-- Chatview 5: "messages in queue" - 2 messages in DB, more will be in RabbitMQ queue
INSERT INTO message (text, user_uid, chatview_id, created_at) VALUES
('Message 1 in queue chatview', '{{TEST_USER_TWO_UID}}', 'chatview-messages-in-queue-uuid', NOW()),
('Message 2 in queue chatview', 'test-user-5-uuid', 'chatview-messages-in-queue-uuid', NOW());
