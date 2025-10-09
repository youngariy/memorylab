# Backend Setup Guide

**Date**: 2025-10-09
**Status**: Production Backend Down (502 Bad Gateway)

---

## Current Issue - RESOLVED ✅

~~Both **production** (`https://mlab.snowytiger.me`) and **local development** are experiencing 502 Bad Gateway errors because the Spring Boot backend is not running.~~

**UPDATE**: Spring Boot 3 path pattern issue has been fixed. See `SPRING_BOOT_3_FIX.md` for details.

The invalid pattern `/**/{spring:[^\\.]*}` has been removed and replaced with a proper ErrorController-based SPA fallback.

### Symptoms

```
GET http://localhost:5173/api/board 502 (Bad Gateway)
GET https://mlab.snowytiger.me/ 502 (Bad Gateway)
```

---

## Solution: Run Backend Locally

### Prerequisites

- **Java 21** (required by Spring Boot 3.3.1)
- **MySQL Database** (running locally or remotely)
- **Gradle** (included via `gradlew`)

### Step 1: Set Environment Variables

Create a `.env` file in the backend directory or set system environment variables:

```bash
# Database Configuration
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/memorylab?useSSL=false&serverTimezone=UTC
export SPRING_DATASOURCE_USERNAME=your_db_username
export SPRING_DATASOURCE_PASSWORD=your_db_password

# JWT Secret (for development, use default or set your own)
export JWT_SECRET=YourSecretKeyHere1234567890

# Mail Configuration (optional for email verification)
export SPRING_MAIL_USERNAME=your_gmail@gmail.com
export SPRING_MAIL_PASSWORD=your_gmail_app_password

# GPU Server Webhook Secret
export GPU_WEBHOOK_SECRET=YourWebhookSecretKey
```

**Windows (PowerShell):**
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/memorylab?useSSL=false&serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME="your_db_username"
$env:SPRING_DATASOURCE_PASSWORD="your_db_password"
$env:JWT_SECRET="YourSecretKeyHere1234567890"
```

### Step 2: Start MySQL Database

**Option A: Local MySQL**
```bash
# Install MySQL 8.0+
# Create database
mysql -u root -p
CREATE DATABASE memorylab CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
exit;
```

**Option B: Docker MySQL**
```bash
docker run -d \
  --name memorylab-mysql \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=memorylab \
  -e MYSQL_USER=memorylab \
  -e MYSQL_PASSWORD=memorylab123 \
  -p 3306:3306 \
  mysql:8.0
```

### Step 3: Run Backend

```bash
# Navigate to backend directory
cd memorylab

# Run with Gradle wrapper
./gradlew bootRun

# Or on Windows
.\gradlew.bat bootRun
```

**Expected Output:**
```
Started MemorylabApplication in 5.123 seconds (process running for 5.456)
Tomcat started on port(s): 8080 (http)
```

### Step 4: Verify Backend is Running

```bash
# Test health endpoint
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}

# Test API endpoint
curl http://localhost:8080/api/board?page=0&size=12
```

### Step 5: Update Frontend Proxy

**File:** `memories_lab/vite.config.dev.ts`

Change proxy target from production to localhost:

```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // ← Change this line
      changeOrigin: true,
      secure: false,  // ← Change to false for http
    },
  },
}
```

### Step 6: Restart Frontend Dev Server

```bash
cd memories_lab
npm run dev
```

---

## Alternative: Mock Data (If Backend Can't Run)

If you cannot run the backend locally (missing database, credentials, etc.), you can temporarily use mock data for frontend development.

**Create:** `memories_lab/src/services/mockData.ts`

```typescript
import type { BoardPageResponse } from '@/types/api';

export const mockBoardsResponse: BoardPageResponse = {
  content: [
    {
      id: 1,
      title: 'Sample Board 1',
      author: { id: 1, nickname: 'User1' },
      category: 'FREE',
      viewCount: 42,
      likeCount: 5,
      commentCount: 3,
      createdAt: new Date().toISOString(),
      isLikedByCurrentUser: false,
      thumbnailPath: null,
      thumbnailStatus: 'NONE',
      status: 'READY',
    },
    // Add more mock boards...
  ],
  totalPages: 1,
  totalElements: 1,
  currentPage: 0,
  pageSize: 12,
  isFirst: true,
  isLast: true,
};
```

Then update `BoardList.tsx` to use mock data when API fails:

```typescript
const fetchBoards = async () => {
  setIsLoading(true);
  setError(null);

  try {
    const response = await boardEndpoints.list({ page, size: 12 });
    setBoards(response.content);
    setTotalPages(response.totalPages);
  } catch (err) {
    console.warn('Backend unavailable, using mock data');
    setBoards(mockBoardsResponse.content);
    setTotalPages(1);
  } finally {
    setIsLoading(false);
  }
};
```

---

## Production Troubleshooting

### Check Backend Status on Production Server

**SSH into production server:**

```bash
ssh -i Ariy-key.pem ubuntu@54.180.3.34
# or
ssh -i Ariy-key.pem ubuntu@mlab.snowytiger.me
```

**Check backend status:**

```bash
# Check if Spring Boot is running
ps aux | grep java

# Check systemd service (if configured)
sudo systemctl status memorylab-backend

# Check backend logs
sudo journalctl -u memorylab-backend -n 100 --no-pager

# Or check log files
tail -f /var/log/memorylab/application.log
```

**Common Issues:**

1. **Backend crashed due to OOM (Out of Memory)**
   ```bash
   # Check memory usage
   free -h

   # Restart backend
   sudo systemctl restart memorylab-backend
   ```

2. **Database connection failed**
   ```bash
   # Check MySQL status
   sudo systemctl status mysql

   # Test connection
   mysql -u memorylab -p memorylab
   ```

3. **Port 8080 already in use**
   ```bash
   # Find process using port 8080
   sudo lsof -i :8080

   # Kill the process
   sudo kill -9 <PID>
   ```

4. **Nginx misconfiguration**
   ```bash
   # Test nginx config
   sudo nginx -t

   # Reload nginx
   sudo systemctl reload nginx

   # Check nginx error logs
   tail -f /var/log/nginx/error.log
   ```

---

## Quick Start (Local Development)

**Terminal 1: Start Backend**
```bash
cd memorylab

# Set environment variables
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/memorylab
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=password

# Run backend
./gradlew bootRun
```

**Terminal 2: Start Frontend**
```bash
cd memories_lab

# Update vite.config.dev.ts to use localhost:8080

# Run frontend
npm run dev
```

**Open Browser:**
```
http://localhost:5173
```

---

## Environment Variables Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | ✅ | None | MySQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | ✅ | None | Database username |
| `SPRING_DATASOURCE_PASSWORD` | ✅ | None | Database password |
| `JWT_SECRET` | ⚠️ | Dev default | JWT signing secret (64+ chars recommended) |
| `SPRING_MAIL_USERNAME` | ❌ | None | Gmail for email verification |
| `SPRING_MAIL_PASSWORD` | ❌ | None | Gmail app password |
| `GPU_WEBHOOK_SECRET` | ⚠️ | Dev default | GPU server webhook validation |

---

## Database Schema

The backend uses JPA with `ddl-auto: validate`, meaning the database schema must already exist. Run migrations or use Flyway/Liquibase to create tables.

**Sample Schema (excerpt):**

```sql
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  name VARCHAR(100) NOT NULL,
  nickname VARCHAR(50) UNIQUE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE board (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  content TEXT,
  author_id BIGINT NOT NULL,
  category VARCHAR(20) NOT NULL,
  status VARCHAR(20) DEFAULT 'NONE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (author_id) REFERENCES users(id)
);

-- Add more tables as needed
```

---

## Next Steps

1. ✅ Set up local MySQL database
2. ✅ Configure environment variables
3. ✅ Run backend with `./gradlew bootRun`
4. ✅ Update Vite proxy to `http://localhost:8080`
5. ✅ Restart frontend dev server
6. ⚠️ Fix production backend (SSH access required)

---

**For Production Issues:**

Contact server administrator with SSH access to diagnose and restart backend services.
