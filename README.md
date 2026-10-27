# Demo: Tasks REST API

A small Spring Boot REST API for managing tasks, backed by PostgreSQL.
The schema and seed data are created on startup by Liquibase.
The project ships with a Docker Compose stack (application, PostgreSQL, pgAdmin) for local development.

## Stack

| Component  | Version / notes                                   |
|------------|---------------------------------------------------|
| Java       | 25                                                |
| Spring Boot| 4.1.1 (Web MVC, JDBC, Liquibase)                  |
| Database   | PostgreSQL 17                                     |
| Build      | Maven via the bundled wrapper (`./mvnw`)          |
| Tests      | JUnit 5, MockMvc, Testcontainers, JaCoCo (90% line coverage gate) |
| Containers | Docker Compose: `app`, `postgres`, `pgadmin`      |

## Prerequisites

- **Docker** with the Compose plugin (`docker compose version` should work). Required for the Compose stack and for running the tests, which start a throwaway PostgreSQL container via Testcontainers.
- **JDK 25** on your `PATH`. Needed to build the jar and to run the app outside Docker. Maven itself is downloaded by the wrapper, no separate install is needed.

## Quick start (Docker Compose)

The easiest way to run everything is the helper script in `docker/`.
It builds the jar with Maven, builds the images, and starts the stack:

```bash
./docker/compose.sh up --build
```

To skip the test suite during the Maven build:

```bash
./docker/compose.sh up --build --skip-tests
```

Once the images exist, start again without rebuilding:

```bash
./docker/compose.sh up
```

The script waits for health checks, so when it returns the API is ready.

### What is running

| Service   | URL (host)                      | Credentials                     |
|-----------|---------------------------------|---------------------------------|
| API       | http://localhost:8181/api/tasks | none                            |
| PostgreSQL| localhost:5434, database `demo` | user `demo`, password `demo`    |
| pgAdmin   | http://localhost:5435           | `admin@demo.com` / `admin`      |

pgAdmin comes with the `demo` server pre-registered. When it prompts for the database password, use `demo`.

Host ports are defined in `docker/.env`. Change them there if they clash with something on your machine.

### Stopping

```bash
./docker/compose.sh down
```

Add `--images` to also remove the images built for this stack, or `--volumes` to also delete the database and pgAdmin data:

```bash
./docker/compose.sh down --volumes
```

### Using docker compose directly

The script is a thin wrapper. The equivalent manual steps are:

```bash
./mvnw -B package -DskipTests
```

```bash
docker compose -f docker/docker-compose.yml --project-directory docker up -d --build --wait
```

The Spring Boot Maven plugin writes the jar to `docker/app/demo.jar`, which is what the `app` image copies in. Run the Maven build first, otherwise the image build fails because the jar is missing.

## Running without Docker (local JVM)

Point the application at any PostgreSQL 17 instance. The defaults in `src/main/resources/application.properties` expect a database `demo` on `localhost:5432` with user and password `demo`.

The simplest option is to reuse the Compose database. Start only PostgreSQL:

```bash
docker compose -f docker/docker-compose.yml --project-directory docker up -d postgres
```

Then start the app with the datasource pointed at the published host port:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/demo ./mvnw spring-boot:run
```

If you already run PostgreSQL on port 5432 with a `demo` database and user, plain `./mvnw spring-boot:run` works with no overrides.

Datasource settings can be overridden with the standard Spring environment variables: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.

## Building and testing

Build the jar and run the full test suite:

```bash
./mvnw clean verify
```

This runs the tests, generates a JaCoCo report at `target/site/jacoco/index.html`, and fails the build if line coverage of the application code drops below 90 percent.

Tests need a running Docker daemon because the integration tests spin up `postgres:17-alpine` with Testcontainers.

Build without tests:

```bash
./mvnw clean package -DskipTests
```

## API

Base path: `/api/tasks`. Request and response bodies are JSON. Errors are returned as RFC 9457 `application/problem+json`.

| Method | Path              | Description                              | Success |
|--------|-------------------|------------------------------------------|---------|
| GET    | `/api/tasks`      | List all tasks, ordered by id            | 200     |
| GET    | `/api/tasks/{id}` | Get one task                             | 200     |
| POST   | `/api/tasks`      | Create a task                            | 201 + `Location` header |
| PUT    | `/api/tasks/{id}` | Replace title and completed flag         | 200     |
| DELETE | `/api/tasks/{id}` | Delete a task                            | 204     |

Request body for POST and PUT:

```json
{ "title": "Write Helm chart", "completed": false }
```

A blank or missing title returns 400. An unknown id returns 404.

Examples:

```bash
curl http://localhost:8181/api/tasks
```

```bash
curl -X POST http://localhost:8181/api/tasks -H 'Content-Type: application/json' -d '{"title":"New task","completed":false}'
```

```bash
curl -X PUT http://localhost:8181/api/tasks/1 -H 'Content-Type: application/json' -d '{"title":"Set up Git repository","completed":true}'
```

```bash
curl -X DELETE http://localhost:8181/api/tasks/1
```

The database starts with ten seed tasks loaded from `src/main/resources/db/changelog/data/tasks.csv`.

## Project layout

```
.
├── docker/
│   ├── compose.sh            # start/stop helper (see Quick start)
│   ├── docker-compose.yml    # wires app, postgres, pgadmin together
│   ├── .env                  # host ports
│   ├── app/                  # Dockerfile + app.env; demo.jar is built here by Maven
│   ├── postgres/             # Dockerfile + postgres.env
│   └── pgadmin/              # Dockerfile + pgadmin.env + pre-registered server
├── src/main/java/com/example/demo/
│   ├── controller/           # REST endpoints
│   ├── service/              # validation and business logic
│   ├── repository/           # JdbcClient access to PostgreSQL
│   ├── model/                # Task and TaskRequest records
│   └── exception/            # problem+json error handling
├── src/main/resources/
│   ├── application.properties
│   └── db/changelog/         # Liquibase changelog and seed CSV
└── src/test/java/            # unit tests and Testcontainers-based integration tests
```

## Troubleshooting

- **`compose.sh up` says it cannot start without building.** No images exist yet. Run `./docker/compose.sh up --build`.
- **App image build fails with "demo.jar not found".** Run the Maven build first, or use the `--build` flag of the script, which does it for you.
- **Tests fail with a Docker or Testcontainers error.** Make sure the Docker daemon is running and your user can talk to it.
- **Port already in use.** Edit the ports in `docker/.env` and start again.
