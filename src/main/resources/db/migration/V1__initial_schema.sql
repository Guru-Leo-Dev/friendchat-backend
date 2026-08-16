-- ════════════════════════════════════════════════════════════════════════════
-- FriendChat – Initial Schema  (MySQL 8.0+)
-- V1__initial_schema.sql
-- ════════════════════════════════════════════════════════════════════════════

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ── Users ─────────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id          CHAR(36)     NOT NULL DEFAULT (UUID()),
    email       VARCHAR(255) NOT NULL,
    username    VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    avatar_url  TEXT,
    bio         VARCHAR(160),
    status      ENUM('online','away','offline') NOT NULL DEFAULT 'offline',
    last_seen   DATETIME(6),
    google_id   VARCHAR(255),
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email      (email),
    UNIQUE KEY uq_users_username   (username),
    UNIQUE KEY uq_users_google_id  (google_id),
    INDEX idx_users_name           (name),
    INDEX idx_users_username_idx   (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Refresh Tokens ────────────────────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id          CHAR(36)     NOT NULL DEFAULT (UUID()),
    user_id     CHAR(36)     NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    device_info VARCHAR(255),
    expires_at  DATETIME(6)  NOT NULL,
    revoked     TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_token_hash (token_hash),
    INDEX idx_refresh_tokens_user    (user_id),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Friend Requests ───────────────────────────────────────────────────────────
CREATE TABLE friend_requests (
    id          CHAR(36)    NOT NULL DEFAULT (UUID()),
    sender_id   CHAR(36)    NOT NULL,
    receiver_id CHAR(36)    NOT NULL,
    status      ENUM('pending','accepted','rejected') NOT NULL DEFAULT 'pending',
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_friend_request   (sender_id, receiver_id),
    INDEX idx_friend_req_sender    (sender_id, status),
    INDEX idx_friend_req_receiver  (receiver_id, status),
    CONSTRAINT fk_friend_req_sender
        FOREIGN KEY (sender_id)   REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_friend_req_receiver
        FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_friend_req_self CHECK (sender_id <> receiver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Friendships ───────────────────────────────────────────────────────────────
CREATE TABLE friendships (
    id         CHAR(36)    NOT NULL DEFAULT (UUID()),
    user_id_a  CHAR(36)    NOT NULL,
    user_id_b  CHAR(36)    NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_friendship       (user_id_a, user_id_b),
    INDEX idx_friendships_a        (user_id_a),
    INDEX idx_friendships_b        (user_id_b),
    CONSTRAINT fk_friendship_a
        FOREIGN KEY (user_id_a) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_friendship_b
        FOREIGN KEY (user_id_b) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_friendship_self CHECK (user_id_a < user_id_b)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Block List ────────────────────────────────────────────────────────────────
CREATE TABLE blocks (
    id         CHAR(36)    NOT NULL DEFAULT (UUID()),
    blocker_id CHAR(36)    NOT NULL,
    blocked_id CHAR(36)    NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_block            (blocker_id, blocked_id),
    INDEX idx_blocks_blocker       (blocker_id),
    INDEX idx_blocks_blocked       (blocked_id),
    CONSTRAINT fk_blocks_blocker
        FOREIGN KEY (blocker_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_blocks_blocked
        FOREIGN KEY (blocked_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_blocks_self CHECK (blocker_id <> blocked_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Chat Rooms ────────────────────────────────────────────────────────────────
CREATE TABLE chat_rooms (
    id            CHAR(36)    NOT NULL DEFAULT (UUID()),
    type          ENUM('direct','group') NOT NULL,
    name          VARCHAR(100),
    avatar_url    TEXT,
    created_by    CHAR(36),
    last_activity DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_chat_rooms_activity  (last_activity),
    CONSTRAINT fk_chat_rooms_creator
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Chat Room Members ─────────────────────────────────────────────────────────
CREATE TABLE chat_room_members (
    id            CHAR(36)    NOT NULL DEFAULT (UUID()),
    chat_room_id  CHAR(36)    NOT NULL,
    user_id       CHAR(36)    NOT NULL,
    role          ENUM('admin','member') NOT NULL DEFAULT 'member',
    joined_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_read_at  DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_chat_member      (chat_room_id, user_id),
    INDEX idx_chat_members_room    (chat_room_id),
    INDEX idx_chat_members_user    (user_id),
    CONSTRAINT fk_chat_members_room
        FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_members_user
        FOREIGN KEY (user_id)      REFERENCES users(id)      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Messages ──────────────────────────────────────────────────────────────────
CREATE TABLE messages (
    id           CHAR(36)    NOT NULL DEFAULT (UUID()),
    chat_room_id CHAR(36)    NOT NULL,
    sender_id    CHAR(36)    NOT NULL,
    content      TEXT        NOT NULL,
    type         ENUM('text','image','file','system') NOT NULL DEFAULT 'text',
    status       ENUM('sent','delivered','read')      NOT NULL DEFAULT 'sent',
    reply_to_id  CHAR(36),
    file_url     TEXT,
    file_name    VARCHAR(255),
    file_size    BIGINT,
    expires_at   DATETIME(6) NOT NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    edited_at    DATETIME(6),
    deleted_at   DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_messages_chat_room   (chat_room_id, created_at),
    INDEX idx_messages_sender      (sender_id),
    INDEX idx_messages_expires     (expires_at),
    INDEX idx_messages_reply       (reply_to_id),
    CONSTRAINT fk_messages_chat_room
        FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender
        FOREIGN KEY (sender_id)    REFERENCES users(id)      ON DELETE CASCADE,
    CONSTRAINT fk_messages_reply
        FOREIGN KEY (reply_to_id)  REFERENCES messages(id)   ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Message Reactions ─────────────────────────────────────────────────────────
CREATE TABLE message_reactions (
    id         CHAR(36)    NOT NULL DEFAULT (UUID()),
    message_id CHAR(36)    NOT NULL,
    user_id    CHAR(36)    NOT NULL,
    emoji      VARCHAR(10) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_reaction         (message_id, user_id, emoji),
    INDEX idx_reactions_message    (message_id),
    CONSTRAINT fk_reactions_message
        FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_reactions_user
        FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Notifications ─────────────────────────────────────────────────────────────
CREATE TABLE notifications (
    id          CHAR(36)     NOT NULL DEFAULT (UUID()),
    user_id     CHAR(36)     NOT NULL,
    type        VARCHAR(30)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    body        TEXT,
    image_url   TEXT,
    read_status TINYINT(1)   NOT NULL DEFAULT 0,
    action_url  TEXT,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_notifications_user   (user_id, created_at),
    INDEX idx_notifications_unread (user_id, read_status),
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
