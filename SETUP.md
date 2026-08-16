# FriendChat — Full Stack Setup Guide

Complete setup guide for the FriendChat ephemeral messaging platform.
Messages auto-delete after 24 hours. Built with React 19 + Spring Boot 3 + PostgreSQL + Redis.

---

## Prerequisites

Install these before starting:

| Tool | Version | Download |
|---|---|---|
| Node.js | 20+ | https://nodejs.org |
| Java JDK | 21+ | https://adoptium.net |
| Maven | 3.9+ | https://maven.apache.org |
| Docker Desktop | Latest | https://docker.com/products/docker-desktop |
| Git | Any | https://git-scm.com |

Verify installations:
```bash
node -v        # v20+
java -version  # 21+
mvn -v         # 3.9+
docker -v      # any recent
```

---

## Project Structure

```
friendchat/
├── friendchat-frontend/     ← React 19 + Vite + TypeScript
└── friendchat-backend/      ← Spring Boot 3 + Java 21
```

---

## Step 1 — Google OAuth Setup (Free)

You need a Google Client ID for login to work.

1. Go to https://console.cloud.google.com
2. Click the project dropdown → **New Project** → name it `FriendChat` → **Create**
3. Left menu → **APIs & Services** → **OAuth consent screen**
   - Choose **External** → **Create**
   - App name: `FriendChat`
   - User support email: your Gmail
   - Developer contact email: your Gmail
   - Click **Save and Continue** through all steps
   - Leave status as **Testing** (free, up to 100 users, no review needed)
4. Left menu → **Credentials** → **+ Create Credentials** → **OAuth 2.0 Client ID**
   - Application type: **Web application**
   - Name: `FriendChat Web`
   - Authorized JavaScript origins: `http://localhost:3000`
   - Authorized redirect URIs: `http://localhost:3000`
   - Click **Create**
5. Copy your **Client ID** — looks like:
   ```
   123456789012-abcdefghijklmnopqrstuvwxyz123456.apps.googleusercontent.com
   ```

---

## Step 2 — Start Infrastructure (PostgreSQL + Redis)

From the `friendchat-backend/` folder, create a `docker-compose.yml` if you don't have one:

```yaml
version: '3.9'
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: friendchat
      POSTGRES_USER: friendchat
      POSTGRES_PASSWORD: friendchat
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
```

Start them:
```bash
docker compose up -d
```

Verify they are running:
```bash
docker compose ps
# Both postgres and redis should show "running"
```

---

## Step 3 — Backend Setup

### 3.1 — Configure environment

Open `src/main/resources/application.yml` and confirm the defaults match your Docker setup:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/friendchat
    username: friendchat
    password: friendchat
  data:
    redis:
      host: localhost
      port: 6379

app:
  jwt:
    secret: your-very-long-secret-key-at-least-64-chars-long-change-in-production
  google:
    client-id: YOUR_GOOGLE_CLIENT_ID_HERE    # ← paste your Client ID from Step 1
```

Or set environment variables instead (recommended for production):
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/friendchat
export SPRING_DATASOURCE_USERNAME=friendchat
export SPRING_DATASOURCE_PASSWORD=friendchat
export JWT_SECRET=your-very-long-secret-key-at-least-64-chars-long
export GOOGLE_CLIENT_ID=123456789012-xxx.apps.googleusercontent.com
```

### 3.2 — Build and run

```bash
cd friendchat-backend

# Download dependencies and build
mvn clean install -DskipTests

# Run the application
mvn spring-boot:run
```

You should see:
```
Started FriendChatApplication in 4.2 seconds
```

Backend is now running at: **http://localhost:8080**

### 3.3 — Verify backend is healthy

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

### 3.4 — Database migrations

Flyway runs automatically on startup. It will create all tables in your PostgreSQL database from `src/main/resources/db/migration/V1__initial_schema.sql`. No manual SQL needed.

---

## Step 4 — Frontend Setup

### 4.1 — Install dependencies

```bash
cd friendchat-frontend
npm install
```

### 4.2 — Configure environment

Create a `.env` file in the `friendchat-frontend/` root:

```env
VITE_API_URL=http://localhost:8080/api
VITE_WS_URL=http://localhost:8080
VITE_GOOGLE_CLIENT_ID=123456789012-xxx.apps.googleusercontent.com
```

Replace `VITE_GOOGLE_CLIENT_ID` with the same Client ID from Step 1.

### 4.3 — Start the dev server

```bash
npm run dev
```

Frontend is now running at: **http://localhost:3000**

---

## Step 5 — Verify Everything Works

1. Open http://localhost:3000
2. You should see the FriendChat login page
3. Click **Continue with Google**
4. Sign in with your Google account
5. You should land on the chat dashboard

---

## Running Both Together (Quick Reference)

Open 3 terminal tabs:

**Tab 1 — Infrastructure:**
```bash
cd friendchat-backend
docker compose up -d
```

**Tab 2 — Backend:**
```bash
cd friendchat-backend
mvn spring-boot:run
```

**Tab 3 — Frontend:**
```bash
cd friendchat-frontend
npm run dev
```

---

## Common Errors and Fixes

### `APPLICATION FAILED TO START` — Redis bean conflict
**Error:** `bean 'stringRedisTemplate' could not be registered`
**Fix:** Make sure you have the latest `RedisConfig.java` with beans named `jsonRedisTemplate` and `presenceRedisTemplate`.

---

### `Connection refused` — PostgreSQL or Redis not running
**Fix:**
```bash
docker compose up -d
docker compose ps   # check both are "running"
```

---

### `Flyway migration failed`
**Error:** Migration checksum mismatch or table already exists.
**Fix:**
```bash
# Connect to postgres and drop the schema to start fresh
docker exec -it <postgres-container-name> psql -U friendchat -d friendchat -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
# Then restart the backend — Flyway will re-run migrations
```

---

### `Invalid Google ID token`
**Causes and fixes:**
- Wrong Client ID in `application.yml` → double-check it matches exactly
- Client ID not set in frontend `.env` → check `VITE_GOOGLE_CLIENT_ID`
- `localhost:3000` not in Authorized JavaScript Origins → go back to Google Cloud Console → Credentials → edit your OAuth client and add it

---

### `CORS error` in browser console
**Fix:** Open `application.yml` and confirm:
```yaml
app:
  cors:
    allowed-origins:
      - http://localhost:3000
```

---

### Port already in use
```bash
# Kill whatever is on port 8080
lsof -ti:8080 | xargs kill -9

# Kill whatever is on port 3000
lsof -ti:3000 | xargs kill -9
```

---

### Frontend can't connect to WebSocket
**Fix:** Make sure `VITE_WS_URL=http://localhost:8080` is in your `.env` (not `ws://` — Socket.IO handles the protocol upgrade automatically).

---

## Environment Variables Reference

### Backend (`application.yml` or env vars)

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/friendchat` | PostgreSQL connection URL |
| `SPRING_DATASOURCE_USERNAME` | `friendchat` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `friendchat` | DB password |
| `SPRING_REDIS_HOST` | `localhost` | Redis host |
| `SPRING_REDIS_PORT` | `6379` | Redis port |
| `JWT_SECRET` | *(must set)* | Min 64 chars, used to sign access tokens |
| `GOOGLE_CLIENT_ID` | *(must set)* | From Google Cloud Console |
| `AWS_REGION` | `us-east-1` | S3 region (only needed for file uploads) |
| `AWS_S3_BUCKET` | `friendchat-media` | S3 bucket (only needed for file uploads) |
| `SERVER_PORT` | `8080` | Backend port |

### Frontend (`.env`)

| Variable | Description |
|---|---|
| `VITE_API_URL` | Backend API base URL e.g. `http://localhost:8080/api` |
| `VITE_WS_URL` | WebSocket server URL e.g. `http://localhost:8080` |
| `VITE_GOOGLE_CLIENT_ID` | Google OAuth Client ID |

---

## File Upload Setup (Optional — AWS S3)

File and image sharing requires an AWS S3 bucket. Skip this if you only want text messaging.

1. Create an S3 bucket in AWS Console
2. Create an IAM user with `s3:PutObject` permission on that bucket
3. Add to your backend environment:
```bash
export AWS_REGION=us-east-1
export AWS_S3_BUCKET=your-bucket-name
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
```

---

## Production Deployment (AWS EC2)

### Backend Docker image
```bash
cd friendchat-backend
docker build -f Dockerfile.backend -t friendchat-backend .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://your-rds-host:5432/friendchat \
  -e JWT_SECRET=your-production-secret \
  -e GOOGLE_CLIENT_ID=your-client-id \
  friendchat-backend
```

### Frontend Docker image
```bash
cd friendchat-frontend
docker build -f Dockerfile.frontend -t friendchat-frontend .
docker run -p 80:80 friendchat-frontend
```

### Production Google OAuth
When going to production (real domain, not localhost):
1. Google Cloud Console → Credentials → edit your OAuth client
2. Add your production domain to **Authorized JavaScript origins**: `https://yourdomain.com`
3. Add to **Authorized redirect URIs**: `https://yourdomain.com`
4. Go to **OAuth consent screen** → **Publish App** (triggers Google review, still free)

---

## API Quick Test

Once backend is running, test the endpoints with curl:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Search users (requires auth token)
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     "http://localhost:8080/api/users/search?q=alice"

# Get chats
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/api/chats
```

Get a JWT token by logging in through the frontend, then opening DevTools → Application → Local Storage → `friendchat-auth` → copy `accessToken`.
