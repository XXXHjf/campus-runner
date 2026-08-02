#!/bin/bash
set -euo pipefail

APP_DIR=/root
APP_JAR="$APP_DIR/campus-runner-0.0.1-SNAPSHOT.jar"
APP_ENV="$APP_DIR/campus-runner.env"
APP_LOG="$APP_DIR/nohup.out"
APP_PID="$APP_DIR/campus-runner.pid"
JAVA_BIN="${JAVA_BIN:-/root/java_install/jdk-21.0.4/bin/java}"

if [[ ! -r "$APP_ENV" ]]; then
    echo "Missing production environment file: $APP_ENV" >&2
    exit 1
fi

if [[ ! -f "$APP_JAR" ]]; then
    echo "Missing application JAR: $APP_JAR" >&2
    exit 1
fi

if [[ ! -x "$JAVA_BIN" ]]; then
    echo "Java executable not found: $JAVA_BIN" >&2
    exit 1
fi

set -a
# shellcheck disable=SC1090
source "$APP_ENV"
set +a

if [[ -z "${DB_PASSWORD:-}" ]]; then
    echo "DB_PASSWORD is not configured" >&2
    exit 1
fi

cd "$APP_DIR"
nohup "$JAVA_BIN" -jar "$APP_JAR" \
    --server.port=8080 \
    --server.ssl.enabled=false \
    --spring.servlet.multipart.max-file-size=10MB \
    --spring.servlet.multipart.max-request-size=20MB \
    --com.mikasa.campus-runner.dev.mock-payment-enabled=false \
    --logging.level.com.mikasa.campusrunner.mapper=INFO \
    --logging.level.com.wechat.pay.contrib.apache.httpclient.SignatureExec=OFF \
    > "$APP_LOG" 2>&1 &

echo $! > "$APP_PID"
echo "Campus Runner API started with PID $(cat "$APP_PID")"
