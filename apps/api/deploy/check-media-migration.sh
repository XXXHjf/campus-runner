#!/bin/bash
set -euo pipefail

ACTION="${1:-status}"
LOG_FILE="${LOG_FILE:-/root/nohup.out}"
STATE_FILE="${STATE_FILE:-/root/media-migration-observation.state}"
MIGRATION_SCRIPT="${MIGRATION_SCRIPT:-/root/campus-runner-media-migration.sh}"
OBSERVATION_SECONDS="${OBSERVATION_SECONDS:-604800}"

if [ ! -x "$MIGRATION_SCRIPT" ]; then
  echo "migration verification script is unavailable" >&2
  exit 1
fi

if [ ! -f "$LOG_FILE" ]; then
  echo "application log is unavailable" >&2
  exit 1
fi

start_observation() {
  "$MIGRATION_SCRIPT" verify

  umask 077
  local temporary
  temporary="$(mktemp "${STATE_FILE}.XXXXXX")"
  {
    printf 'STARTED_EPOCH=%q\n' "$(date +%s)"
    printf 'STARTED_UTC=%q\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    printf 'LOG_INODE=%q\n' "$(stat -c '%i' "$LOG_FILE")"
    printf 'LOG_OFFSET=%q\n' "$(stat -c '%s' "$LOG_FILE")"
  } > "$temporary"
  mv "$temporary" "$STATE_FILE"
  chmod 600 "$STATE_FILE"
  echo "observation_started=true"
}

show_status() {
  if [ ! -f "$STATE_FILE" ]; then
    echo "observation_started=false"
    exit 3
  fi

  # shellcheck disable=SC1090
  source "$STATE_FILE"
  local now age current_inode fallback_count continuity verify_output explicit_failed
  now="$(date +%s)"
  age=$((now - STARTED_EPOCH))
  current_inode="$(stat -c '%i' "$LOG_FILE")"
  fallback_count="unknown"
  continuity="broken"

  if [ "$current_inode" = "$LOG_INODE" ] && [ "$(stat -c '%s' "$LOG_FILE")" -ge "$LOG_OFFSET" ]; then
    continuity="ok"
    fallback_count="$(
      tail -c "+$((LOG_OFFSET + 1))" "$LOG_FILE" \
        | grep -c 'LEGACY_MEDIA_FALLBACK' \
        || true
    )"
  fi

  verify_output="$("$MIGRATION_SCRIPT" verify)"
  printf '%s\n' "$verify_output"
  explicit_failed="$(
    printf '%s\n' "$verify_output" \
      | sed -n 's/.*verify\.explicit_failed=\([0-9][0-9]*\).*/\1/p' \
      | tail -n 1
  )"
  # The summary omits zero-valued counters.
  explicit_failed="${explicit_failed:-0}"

  echo "observation_started=true"
  echo "observation_started_utc=$STARTED_UTC"
  echo "observation_age_seconds=$age"
  echo "log_continuity=$continuity"
  echo "legacy_fallback_count=$fallback_count"
  echo "explicit_failed_count=$explicit_failed"

  if [ "$age" -ge "$OBSERVATION_SECONDS" ] \
      && [ "$continuity" = "ok" ] \
      && [ "$fallback_count" = "0" ] \
      && [ "$explicit_failed" = "0" ]; then
    echo "cleanup_eligible=true"
  else
    echo "cleanup_eligible=false"
  fi
}

case "$ACTION" in
  start)
    start_observation
    ;;
  status)
    show_status
    ;;
  *)
    echo "usage: $0 start|status" >&2
    exit 2
    ;;
esac
