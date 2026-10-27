#!/usr/bin/env bash
# Start or stop the demo stack (app, postgres, pgadmin) with docker compose.
#
#   ./compose.sh up                  start from existing images, no build
#   ./compose.sh up --build          rebuild demo.jar with Maven, rebuild images, start
#   ./compose.sh up --build --skip-tests   same, but skip Maven tests
#   ./compose.sh down                stop and remove containers (keeps images and data)
#   ./compose.sh down --images       also remove the images built for this stack
#   ./compose.sh down --volumes      also delete database and pgAdmin data
#
# Works from any directory: paths are resolved relative to this script.
set -euo pipefail

DOCKER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$DOCKER_DIR")"
COMPOSE=(docker compose --project-directory "$DOCKER_DIR" -f "$DOCKER_DIR/docker-compose.yml")

usage() {
    sed -n '2,10p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
    exit "${1:-0}"
}

build_jar() {
    local maven_args=(-B package)
    [[ "$1" == true ]] && maven_args+=(-DskipTests)
    echo ">> Building demo.jar with Maven (${maven_args[*]})"
    (cd "$PROJECT_DIR" && ./mvnw "${maven_args[@]}")
}

cmd_up() {
    local build=false skip_tests=false
    for arg in "$@"; do
        case "$arg" in
            --build)      build=true ;;
            --skip-tests) skip_tests=true ;;
            -h|--help)    usage ;;
            *) echo "Unknown option for up: $arg" >&2; usage 1 ;;
        esac
    done
    if [[ "$skip_tests" == true && "$build" == false ]]; then
        echo "--skip-tests only makes sense together with --build" >&2
        exit 1
    fi

    if [[ "$build" == true ]]; then
        build_jar "$skip_tests"
        echo ">> Building images and starting containers"
        "${COMPOSE[@]}" up -d --build --wait
    else
        echo ">> Starting containers from existing images"
        if ! "${COMPOSE[@]}" up -d --no-build --wait; then
            echo "Could not start without building. If images are missing, run: $0 up --build" >&2
            exit 1
        fi
    fi
    "${COMPOSE[@]}" ps
}

cmd_down() {
    local down_args=()
    for arg in "$@"; do
        case "$arg" in
            --images)  down_args+=(--rmi local) ;;
            --volumes) down_args+=(--volumes) ;;
            -h|--help) usage ;;
            *) echo "Unknown option for down: $arg" >&2; usage 1 ;;
        esac
    done
    echo ">> Stopping and removing containers ${down_args[*]:-}"
    "${COMPOSE[@]}" down "${down_args[@]}"
}

[[ $# -ge 1 ]] || usage 1
command="$1"; shift
case "$command" in
    up)        cmd_up "$@" ;;
    down)      cmd_down "$@" ;;
    -h|--help) usage ;;
    *) echo "Unknown command: $command" >&2; usage 1 ;;
esac
