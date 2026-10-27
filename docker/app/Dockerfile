FROM amazoncorretto:25-al2023
LABEL authors="ihor"

# Create group "work" and user "ihor" that belongs to it
RUN dnf install -y shadow-utils \
    && dnf clean all \
    && groupadd --system work \
    && useradd --system --gid work --no-create-home --shell /sbin/nologin ihor

WORKDIR /opt/app
COPY --chown=ihor:work demo.jar /opt/app/demo.jar

USER ihor:work

# Database connection settings come from app.env (see docker-compose.yml)

EXPOSE 8181

# Healthy once the API answers; docker compose --wait relies on this
HEALTHCHECK --interval=5s --timeout=3s --start-period=20s --retries=10 \
    CMD curl -fs http://localhost:8181/api/tasks > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/opt/app/demo.jar"]
