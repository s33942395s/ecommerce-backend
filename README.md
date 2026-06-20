# Ecommerce Backend

Spring Boot 電商後端 API，包含會員註冊/登入、商品管理、下單、庫存扣減與 PostgreSQL 資料庫遷移。

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Data JPA
- PostgreSQL
- Flyway
- Docker Compose
- JWT

## Features

- 使用者註冊與登入
- JWT token 產生
- 商品查詢、新增、更新、軟刪除
- 建立訂單
- 下單時檢查並扣減庫存
- Flyway 管理資料庫 schema migration

## Quick Start

複製環境變數範例：

```bash
cp .env.example .env
```

啟動服務：

```bash
docker compose up -d --build
```

查看服務狀態：

```bash
docker compose ps
```

查看後端 log：

```bash
docker compose logs -f ecommerce-backend
```

預設 API 位址：

```text
http://localhost:8080
```

如果部署在 VPS 且 `.env` 設定 `APP_PORT=8081`，則使用：

```text
http://YOUR_VPS_IP:8081
```

## Environment Variables

`.env.example`:

```env
APP_PORT=8080

DB_NAME=ecommerce_db
DB_USER=postgres
DB_PASS=887985
DB_PORT=5432

JPA_DDL_AUTO=validate
JWT_SECRET=replace-with-a-long-random-secret
```

正式或部署環境建議使用：

```env
JPA_DDL_AUTO=validate
```


## Database Migration

Migration 檔案放在：

```text
src/main/resources/db/migration
```

目前初始 schema：

```text
V1__init_schema.sql
```

未來新增欄位或資料表時，不要修改已執行過的 `V1`，請新增下一版 migration，例如：

```text
V2__add_product_category.sql
```

範例：

```sql
ALTER TABLE products
ADD COLUMN category VARCHAR(100);
```

## API Endpoints

### Auth

Register:

```http
POST /api/auth/register
```

```json
{
  "username": "testuser",
  "password": "123456"
}
```

Login:

```http
POST /api/auth/login
```

```json
{
  "username": "testuser",
  "password": "123456"
}
```

### Products

List products:

```http
GET /api/products?page=0&size=10
```

Get product:

```http
GET /api/products/{id}
```

Create product:

```http
POST /api/products
```

```json
{
  "name": "Test Product",
  "price": 100.00,
  "stock": 10
}
```

Update product:

```http
PUT /api/products/{id}
```

```json
{
  "name": "Updated Product",
  "price": 120.00,
  "stock": 5
}
```

Delete product:

```http
DELETE /api/products/{id}
```

### Orders

Create order:

```http
POST /api/orders
```

Headers:

```text
Authorization: Bearer JWT_TOKEN
Content-Type: application/json
```

Body:

```json
{
  "items": [
    {
      "productId": 1,
      "quantity": 1
    }
  ]
}
```

## Test Flow

1. 註冊使用者：`POST /api/auth/register`
2. 登入取得 token：`POST /api/auth/login`
3. 新增商品：`POST /api/products`
4. 查詢商品：`GET /api/products`
5. 使用 token 建立訂單：`POST /api/orders`

## Automated Tests

The project includes fast integration tests under:

```text
src/test/java/com/example/ecommerce_backend
```

Run all tests:

```bash
./mvnw test
```

Windows:

```powershell
.\mvnw.cmd test
```

The tests use H2 in-memory database, so they do not require local PostgreSQL or Docker.

Current test coverage:

- Application context startup.
- Login flow: register a user and login to get JWT token.
- Permission checks: product write APIs require token and `ADMIN` role.
- Product features: create, get, update, list, and soft delete.
- Order and stock: successful order deducts stock, insufficient stock is rejected.
- Concurrent stock test: multiple buyers order the same product at the same time, verifying stock does not oversell.

Main test file:

```text
src/test/java/com/example/ecommerce_backend/EcommerceIntegrationTests.java
```

## Useful Commands

停止服務：

```bash
docker compose down
```

停止服務並刪除 PostgreSQL volume：

```bash
docker compose down -v
```

注意：`down -v` 會刪除資料庫資料，只適合本機測試或測試環境。

進入 PostgreSQL：

```bash
docker exec -it ecommerce-postgres psql -U postgres -d ecommerce_db
```

