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

## File storage

Local storage is the default. To use Supabase Storage, set these variables in `.env.dev`:

```text
FILE_STORAGE_PROVIDER=supabase
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_STORAGE_BUCKET=files
SUPABASE_SERVICE_ROLE_KEY=<server-only-service-role-key>
```

Keep `SUPABASE_SERVICE_ROLE_KEY` on the backend only. To use Cloudflare R2 instead, set:

```text
FILE_STORAGE_PROVIDER=r2
R2_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com
R2_REGION=auto
R2_BUCKET=<bucket-name>
R2_ACCESS_KEY_ID=<access-key-id>
R2_SECRET_ACCESS_KEY=<secret-access-key>
R2_PATH_STYLE_ACCESS=true
```

R2 uses the S3-compatible API. Uploaded object keys are stored in file metadata as `files/<uuid>.<extension>`.
