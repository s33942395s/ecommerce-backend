# Docker Deployment

## Requirements

- Docker
- Docker Compose v2

## Start Services

```bash
docker compose up --build -d
```

Default endpoints and credentials:

- Backend API: `http://localhost:8080`
- PostgreSQL: `localhost:5432`
- Database: `ecommerce_db`
- User: `postgres`

## Configuration

Create a local `.env` file in the project root when you need to override deployment settings. Use `.env.example` as the reference.

Common settings:

```env
APP_PORT=8080
DB_NAME=ecommerce_db
DB_USER=postgres
DB_PASS=887985
DB_PORT=5432
JPA_DDL_AUTO=update
JWT_SECRET=replace-with-a-long-random-secret
```

## Stop Services

```bash
docker compose down
```

To also remove the PostgreSQL data volume:

```bash
docker compose down -v
```

## Rebuild Backend Image

```bash
docker compose build --no-cache ecommerce-backend
docker compose up -d
```
