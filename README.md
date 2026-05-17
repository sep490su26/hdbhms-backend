# HDBHMS Backend

## Run local

Start MySQL and Redis:

```powershell
docker compose up -d
```

Run Spring Boot:

```powershell
.\mvnw.cmd spring-boot:run
```

## Test login bằng Postman

POST `http://localhost:8080/api/v1/auth/login`

Body JSON:

```json
{
  "phone_or_email": "0900000001",
  "password": "12345678"
}
```

## Base URL cho Flutter

Web/Chrome:

```text
http://localhost:8080/api/v1
```

Android Emulator:

```text
http://10.0.2.2:8080/api/v1
```

Điện thoại thật:

```text
http://<IP_MAY_TINH>:8080/api/v1
```
