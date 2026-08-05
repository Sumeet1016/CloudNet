# CloudNest — Multi-Cloud Backup Platform for Industries

A final-year project: a decently-scoped, real, working multi-cloud backup and
recovery platform aimed at specific industries (Healthcare, Finance,
Education, Manufacturing). Users connect one or more storage providers,
create industry-based backup policies, run manual or scheduled backups with
AES-256 encryption and versioning, and restore files from a dashboard.

## Tech Stack

**Backend:** Java 17, Spring Boot 3, Spring Security + JWT, Spring Data JPA,
MySQL, Spring `@Scheduled` (polling scheduler), Google Drive API, Lombok.

**Frontend:** React 18 + Vite, React Router, Axios, Tailwind CSS, Recharts.

## How storage providers work

All three providers implement one shared interface, `CloudStorageProvider`
(Strategy pattern) — the rest of the app never knows or cares which one is
in use:

| Provider        | What it actually is                                              |
|------------------|-------------------------------------------------------------------|
| `GOOGLE_DRIVE`   | **Real** — uses the Google Drive REST API to upload/download files into a "CloudNest-Backups" folder in the user's own Drive. |
| `LOCAL_DISK`     | Real local folder on the server (`./storage/local-disk`), simulating an on-prem/NAS target. |
| `SIMULATED_S3`   | Real local folder (`./storage/simulated-s3`), organized like S3 "buckets" per user — same interface an actual AWS SDK implementation would use, so it's a one-file swap to go live later. |

This means you get one genuine cloud integration (Google Drive) for the
demo, without needing an AWS account or any billing, while the rest of the
app is architected exactly as it would be with three real cloud providers.

## Project Structure

```
cloudnest/
├── backend/                # Spring Boot app (Maven)
│   └── src/main/java/com/cloudnest/
│       ├── entity/          # JPA entities
│       ├── repository/      # Spring Data repositories
│       ├── dto/              # Request/response DTOs
│       ├── security/         # JWT filter, util, current-user helper
│       ├── config/            # Spring Security config
│       ├── service/           # Business logic
│       │   └── storage/        # CloudStorageProvider strategy + implementations
│       ├── controller/         # REST controllers
│       └── exception/          # Global exception handler
└── frontend/                # React app (Vite)
    └── src/
        ├── api/               # Axios client
        ├── context/            # Auth context (JWT storage)
        ├── components/          # Sidebar, ProtectedRoute, StatCard
        └── pages/                # Login, Register, Dashboard, Providers,
                                     Policies, Backup Jobs, Schedules, Logs
```

## Setup

### 1. Prerequisites
- Java 17+, Maven
- Node.js 18+
- MySQL 8 running locally

### 2. Database
```sql
CREATE DATABASE cloudnest_db;
```
The app will auto-create tables on first run (`ddl-auto: update`).
Update `backend/src/main/resources/application.yml` with your MySQL
username/password if different from the default `root/root`.

### 3. Backend
```bash
cd backend
mvn spring-boot:run
```
Runs on `http://localhost:8080`.

### 4. Frontend
```bash
cd frontend
npm install
npm run dev
```
Runs on `http://localhost:5173` and proxies `/api` calls to the backend.

### 5. (Optional) Real Google Drive integration
1. Go to [Google Cloud Console](https://console.cloud.google.com/) → create
   a project → enable the **Google Drive API**.
2. Create OAuth 2.0 credentials (Web application type).
3. Put the client ID/secret into `application.yml` under `app.google.*`.
4. For a quick demo without building a full OAuth consent screen flow, you
   can generate a short-lived access token via
   [Google's OAuth 2.0 Playground](https://developers.google.com/oauthplayground/)
   with the `https://www.googleapis.com/auth/drive.file` scope, and paste
   that token into the "Connect Provider" form on the Cloud Providers page.
   For a production-grade flow you'd implement the full authorization-code
   exchange in `CloudProviderController`/`CloudProviderService` — the
   `GoogleDriveStorageProvider` service itself is already written to just
   consume a valid access token, so no other code needs to change.

## Core Features

- JWT-based authentication with industry selection at registration
- Connect/manage multiple storage providers per user
- Industry-based backup policies with sensible cron/retention presets
  (overridable), defined in the `Industry` enum
- Manual backup: upload files, optional AES-256 encryption, SHA-256
  checksum, automatic version numbering per filename
- Scheduled backup: point a schedule at a server folder + policy; a
  Spring `@Scheduled` job polls every minute and backs up any due schedule
- Restore: download and (if encrypted) decrypt any historical file version
- Dashboard: total/successful/failed backups, storage used per provider,
  backups-by-provider chart, recent activity feed
- Full activity/audit log

## Notes for your report / viva

- **Design pattern used:** Strategy pattern for `CloudStorageProvider` —
  this is the "multi-cloud" part of the platform; adding a new real
  provider is a single new class + one line in `StorageProviderFactory`.
- **Security:** Spring Security + stateless JWT auth, BCrypt password
  hashing, AES-256 file encryption at rest, SHA-256 integrity checksums.
- **Scheduling:** simple polling scheduler (`@Scheduled(fixedRate = 60000)`)
  checking `next_run_at` on each active `BackupSchedule` row — easy to
  explain and demo, and cron expressions are computed with Spring's
  built-in `CronExpression` parser so users can still define
  industry-standard schedules.
