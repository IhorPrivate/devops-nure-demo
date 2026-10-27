FROM dpage/pgadmin4:9.18
LABEL authors="ihor"

# Login and listen port come from pgadmin.env (see docker-compose.yml)

# Pre-registers the "demo" PostgreSQL server on first start
COPY --chown=pgadmin:root servers.json /pgadmin4/servers.json

EXPOSE 80
