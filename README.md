# FriendChat Backend

Production Spring Boot 3 backend for the FriendChat ephemeral messaging platform.

---

## Stack

| Layer         | Technology                              |
|---------------|-----------------------------------------|
| Runtime       | Java 21 (virtual threads ready)         |
| Framework     | Spring Boot 3.3                         |
| Security      | Spring Security + JWT (jjwt 0.12)       |
| Auth          | Google OAuth2 ID Token verification     |
| WebSocket     | Spring WebSocket (raw WS, no STOMP)     |
| ORM           | Spring Data JPA / Hibernate 6           |
| Database      | PostgreSQL 16                           |
| Cache/Presence| Redis 7 (Lettuce pool)                  |
| Migrations    | Flyway                                  |
| File Storage  | AWS S3                                  |
| Mapping       | MapStruct                               |
| Boilerplate   | Lombok                                  |

---

## Package Structure

```
com.friendchat
├── auth/
│   ├── RefreshToken.java              Entity
│   ├── RefreshTokenRepository.java
│   ├── service/AuthService.java       Google login, refresh, logout
│   └── controller/
│       ├── AuthController.java        POST /auth/google|refresh|logout|logout-all
│       └── dto/AuthDtos.java          TokenPair, AuthResponse, request records
│
├── user/
│   ├── entity/User.java               JPA entity + UserStatus enum
│   ├── entity/UserRepository.java     JPQL search with pg_trgm indexes
│   ├── dto/                           UserDto, UserProfileDto, UpdateProfileRequest
│   └── service/UserService.java       Profile CRUD, search, status
│   └── controller/UserController.java GET /profile, PUT /profile, GET /users/search
│
├── friend/
│   ├── entity/FriendRequest.java      + FriendRequestStatus, Friendship
│   ├── entity/FriendRequestRepository.java
│   ├── entity/FriendshipRepository.java
│   ├── dto/FriendRequestDto.java       FriendshipDto
│   ├── service/FriendService.java      Full request lifecycle + block
│   └── controller/FriendController.java
│
├── chat/
│   ├── entity/ChatRoom.java           + ChatType, ChatRoomMember, MemberRole
│   ├── entity/ChatRoomRepository.java  findDirectChat(), findAllByMemberUserId()
│   ├── dto/ChatRoomDto.java            ChatMemberDto
│   ├── service/ChatService.java        DM/group creation, read receipts, leave
│   └── controller/ChatController.java
│
├── message/
│   ├── entity/Message.java            + MessageType, MessageStatus, MessageReaction
│   ├── entity/MessageRepository.java   deleteExpired(), countUnread(), markAllRead()
│   ├── dto/MessageDto.java             ReactionGroupDto
│   ├── service/MessageMapper.java      Entity → DTO with reaction grouping
│   ├── service/MessageService.java     Send, file upload, react (toggle), soft-delete
│   └── controller/MessageController.java
│
├── notification/
│   ├── entity/Notification.java
│   ├── entity/NotificationRepository.java
│   ├── dto/NotificationDto.java
│   ├── service/NotificationService.java  Factory methods + WS push
│   └── controller/NotificationController.java
│
├── websocket/
│   ├── handler/ChatWebSocketHandler.java  Session registry, inbound event routing
│   └── event/WsEventPublisher.java        Outbound event broadcast to rooms/users
│
├── security/
│   ├── JwtService.java                HMAC-SHA access tokens + opaque refresh tokens
│   ├── JwtAuthenticationFilter.java   OncePerRequestFilter
│   └── CurrentUser.java               UserDetails principal
│
├── common/
│   ├── dto/PageResponse.java          Generic paginated wrapper
│   ├── exception/                     5 typed exceptions + GlobalExceptionHandler
│   └── util/
│       ├── S3Service.java             Avatar + file upload, content-type validation
│       └── RedisPresenceService.java  Online/offline with 90 s TTL heartbeat
│
├── config/
│   ├── SecurityConfig.java            CORS, CSRF-off, stateless, JWT filter
│   ├── WebSocketConfig.java           WS endpoint + JWT handshake interceptor
│   ├── RedisConfig.java               RedisTemplate with Jackson + type info
│   └── WebMvcConfig.java              ObjectMapper bean, /api path prefix
│
└── scheduler/
    └── MaintenanceScheduler.java      Message expiry (every 30 min) + token purge (03:00)
```

---

## Quick Start

### Prerequisites

- Java 21+, Maven 3.9+
- Docker (for PostgreSQL + Redis)

### 1 — Start infrastructure

```bash
docker compose up postgres redis -d
```

### 2 — Configure environment

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/friendchat
export SPRING_DATASOURCE_USERNAME=friendchat
export SPRING_DATASOURCE_PASSWORD=friendchat
export JWT_SECRET=your-512-bit-secret-here
export GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
export AWS_REGION=us-east-1
export AWS_S3_BUCKET=friendchat-media
```

### 3 — Run

```bash
mvn spring-boot:run
# or
mvn package -DskipTests && java -jar target/friendchat-backend-*.jar
```

### 4 — Run tests

```bash
mvn test
```

---

## API Reference

### Auth

| Method | Path                  | Auth | Description                      |
|--------|-----------------------|------|----------------------------------|
| POST   | /api/auth/google      | ✗    | Exchange Google ID token for JWT |
| POST   | /api/auth/refresh     | ✗    | Rotate refresh token             |
| POST   | /api/auth/logout      | ✓    | Revoke one refresh token         |
| POST   | /api/auth/logout-all  | ✓    | Revoke all sessions for user     |

**POST /api/auth/google** request:
```json
{ "idToken": "<Google GSI credential>" }
```
Response:
```json
{
  "user": { "id": "...", "name": "...", "email": "...", "avatarUrl": "...", "username": "..." },
  "tokens": { "accessToken": "...", "refreshToken": "...", "expiresIn": 900 }
}
```

### Users

| Method | Path                | Description                  |
|--------|---------------------|------------------------------|
| GET    | /api/profile        | Current user                 |
| PUT    | /api/profile        | Update name/username/bio     |
| POST   | /api/profile/avatar | Upload avatar (multipart)    |
| GET    | /api/users/search   | Search users (`?q=alice`)    |
| GET    | /api/users/{id}     | User profile with friendship |

### Friends

| Method | Path                          | Description          |
|--------|-------------------------------|----------------------|
| GET    | /api/friends                  | My friends list      |
| GET    | /api/friends/requests         | Pending requests     |
| POST   | /api/friends/request          | Send request         |
| POST   | /api/friends/accept/{id}      | Accept request       |
| POST   | /api/friends/reject/{id}      | Reject request       |
| DELETE | /api/friends/{userId}         | Remove friend        |
| POST   | /api/friends/block/{userId}   | Block user           |

### Chats

| Method | Path                   | Description                   |
|--------|------------------------|-------------------------------|
| GET    | /api/chats             | All chats (sorted by activity)|
| GET    | /api/chats/{id}        | Single chat room              |
| POST   | /api/chats/direct      | Create/get DM                 |
| POST   | /api/chats/group       | Create group chat             |
| POST   | /api/chats/{id}/read   | Mark chat as read             |
| POST   | /api/chats/{id}/leave  | Leave group chat              |

### Messages

| Method | Path                          | Description                   |
|--------|-------------------------------|-------------------------------|
| GET    | /api/messages/{chatId}        | Paginated messages            |
| POST   | /api/messages                 | Send text message             |
| POST   | /api/messages/file            | Send file/image (multipart)   |
| POST   | /api/messages/{id}/react      | Toggle reaction               |
| DELETE | /api/messages/{id}            | Soft-delete own message       |

### Notifications

| Method | Path                          | Description      |
|--------|-------------------------------|------------------|
| GET    | /api/notifications            | All (paginated)  |
| POST   | /api/notifications/{id}/read  | Mark one read    |
| POST   | /api/notifications/read-all   | Mark all read    |

---

## WebSocket Protocol

Connect to `ws://host/ws?token=<accessToken>`

All frames are JSON: `{ "event": "<name>", "data": { ... } }`

### Client → Server

| Event           | Data                                        |
|-----------------|---------------------------------------------|
| `message:send`  | `{ chatRoomId, content, replyToId? }`       |
| `typing:start`  | `{ chatRoomId }`                            |
| `typing:stop`   | `{ chatRoomId }`                            |
| `message:read`  | `{ chatRoomId, messageId }`                 |
| `chat:join`     | `{ chatRoomId }` — subscribe to room events |
| `chat:leave`    | `{ chatRoomId }`                            |

### Server → Client

| Event               | Data                                        |
|---------------------|---------------------------------------------|
| `message:receive`   | `{ message: MessageDto }`                   |
| `message:status`    | `{ messageId, chatRoomId, status }`         |
| `message:reaction`  | `{ messageId, reactions: [...] }`           |
| `message:deleted`   | `{ messageId }`                             |
| `message:read`      | `{ chatRoomId, userId }`                    |
| `typing:start`      | `{ chatRoomId, user, isTyping: true }`      |
| `typing:stop`       | `{ chatRoomId, user, isTyping: false }`     |
| `user:online`       | `{ userId, status }`                        |
| `user:offline`      | `{ userId, status, lastSeen }`              |
| `friend:request`    | `{ request: FriendRequestDto }`             |
| `notification:new`  | `{ notification: NotificationDto }`         |

---

## 24-Hour Message Expiry

Every message gets an `expires_at = created_at + 24h` timestamp at insert time.

| Layer       | Mechanism                                                           |
|-------------|---------------------------------------------------------------------|
| PostgreSQL  | `expires_at` column with index; `MaintenanceScheduler` hard-deletes every 30 min |
| Redis       | Presence keys use TTL — no stale entries accumulate                |
| Frontend    | Per-message expiry badge computed from `expiresAt` field; amber warning < 1 h |

---

## Security Notes

- **Access tokens**: HS512 JWT, 15-minute lifetime, secret from env var
- **Refresh tokens**: Opaque 64-char random string, SHA-256 hashed in DB, 30-day TTL
- **Token rotation**: Refresh issues a new token and revokes the old one atomically
- **Refresh token reuse detection**: Reuse of a revoked token triggers full session wipe for that user
- **WebSocket auth**: JWT validated at handshake; no auth = connection rejected immediately
- **CORS**: Configurable via `app.cors.allowed-origins`
- **File uploads**: Content-type validated server-side; max 25 MB
