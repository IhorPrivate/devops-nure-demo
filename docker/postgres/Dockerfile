FROM postgres:17-alpine
LABEL authors="ihor"

# POSTGRES_DB, POSTGRES_USER and POSTGRES_PASSWORD come from postgres.env (see docker-compose.yml)

# docker compose waits for this before starting the app and pgAdmin
HEALTHCHECK --interval=5s --timeout=3s --start-period=10s --retries=10 \
    CMD pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" || exit 1

EXPOSE 5432
