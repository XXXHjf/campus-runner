#!/bin/bash
set -euo pipefail

MODE="${1:-}"
CONFIRMATION="${2:-}"

case "$MODE" in
  dry-run|verify)
    ;;
  apply)
    if [ "$CONFIRMATION" != "--confirm-writes" ]; then
      echo "apply mode requires --confirm-writes" >&2
      exit 2
    fi
    ;;
  *)
    echo "usage: $0 dry-run|verify|apply [--confirm-writes]" >&2
    exit 2
    ;;
esac

ENV_FILE="${ENV_FILE:-/root/campus-runner.env}"
JAR_PATH="${JAR_PATH:-/root/campus-runner-0.0.1-SNAPSHOT.jar}"
JAVA_BIN="${JAVA_BIN:-/root/java_install/jdk-21.0.4/bin/java}"
LOCK_FILE="${LOCK_FILE:-/var/lock/campus-runner-media-migration.lock}"

if [ ! -f "$ENV_FILE" ]; then
  echo "production environment file is missing" >&2
  exit 1
fi

ENV_MODE="$(stat -c '%a' "$ENV_FILE")"
if [ "$ENV_MODE" != "600" ]; then
  echo "production environment file must have mode 600" >&2
  exit 1
fi

if [ ! -x "$JAVA_BIN" ]; then
  echo "configured Java 21 executable is unavailable" >&2
  exit 1
fi

if [ ! -f "$JAR_PATH" ]; then
  echo "application JAR is missing" >&2
  exit 1
fi

exec 9>"$LOCK_FILE"
if ! flock -n 9; then
  echo "another historical media migration is already running" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

MODE_VALUE="$(printf '%s' "$MODE" | tr '[:lower:]-' '[:upper:]_')"
ARGS=(
  "--spring.main.web-application-type=none"
  "--spring.main.banner-mode=off"
  "--spring.main.lazy-initialization=true"
  "--app.scheduling.enabled=false"
  "--media-migration.enabled=true"
  "--media-migration.mode=$MODE_VALUE"
  "--media-migration.trusted-external-hosts=${MEDIA_MIGRATION_TRUSTED_EXTERNAL_HOSTS:-mmbiz.qpic.cn}"
)

if [ -n "${MEDIA_MIGRATION_ADMIN_OWNER_ID:-}" ]; then
  ARGS+=("--media-migration.admin-owner-id=$MEDIA_MIGRATION_ADMIN_OWNER_ID")
fi

if [ -n "${MEDIA_MIGRATION_MAX_RECORDS:-}" ]; then
  ARGS+=("--media-migration.max-records=$MEDIA_MIGRATION_MAX_RECORDS")
fi

if [ "$MODE" = "apply" ]; then
  ARGS+=("--media-migration.allow-writes=true")
fi

"$JAVA_BIN" -jar "$JAR_PATH" "${ARGS[@]}"
