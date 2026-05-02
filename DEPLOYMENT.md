# NipponHub API Deployment

This backend is ready for Docker-based deployment on Render and Railway.

## Required environment variables

Use either the split database variables:

```text
DB_URL=jdbc:postgresql://host:5432/database?sslmode=require
DB_USERNAME=postgres_user
DB_PASSWORD=postgres_password
```

Or the single cloud database URL used by platforms such as Railway and Render:

```text
DATABASE_URL=postgresql://postgres_user:postgres_password@host:5432/database
```

Set these secrets in the cloud dashboard:

```text
JWT_SECRET=<strong-base64-secret>
CLOUDINARY_CLOUD_NAME=<cloudinary-cloud-name>
CLOUDINARY_API_KEY=<cloudinary-api-key>
CLOUDINARY_API_SECRET=<cloudinary-api-secret>
```

Recommended runtime values:

```text
SPRING_PROFILES_ACTIVE=prod
CORS_ORIGINS=https://front-end-nipponv1.vercel.app
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000
```

## Health checks

Use this path on every platform:

```text
/actuator/health
```

It is exposed without authentication and returns HTTP 200 only when Spring Boot reports the application as healthy.

## Render

The repository includes `render.yaml`.

1. Create a Render Blueprint from this repository.
2. Keep `plan: free` for a no-cost web service.
3. Fill the `sync: false` environment variables in the dashboard.
4. Deploy.

Render free web services spin down after idle time, so the first request after inactivity can be slow.

## Railway

The repository includes `railway.json`.

1. Create a Railway project from this repository.
2. Railway will use the Dockerfile.
3. Attach a PostgreSQL service or provide database variables manually.
4. Generate a public domain in the service networking settings.
5. Confirm the health check path is `/actuator/health`.

Railway injects `PORT`; the app listens on it automatically.

## Local Docker test

```bash
docker build -t nipponhub-api .
docker run --rm -p 8080:8080 --env-file .env nipponhub-api
```

Then check:

```bash
curl http://localhost:8080/actuator/health
```
