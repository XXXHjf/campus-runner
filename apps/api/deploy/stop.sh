#!/bin/bash
set -euo pipefail

APP_PID=/root/campus-runner.pid

pid=""
if [[ -r "$APP_PID" ]]; then
    candidate="$(cat "$APP_PID")"
    if [[ "$candidate" =~ ^[0-9]+$ ]] &&
       kill -0 "$candidate" 2>/dev/null &&
       ps -p "$candidate" -o args= | grep -q 'campus-runner-0.0.1-SNAPSHOT.jar'; then
        pid="$candidate"
    fi
fi

if [[ -z "$pid" ]]; then
    pid="$(pgrep -f '[c]ampus-runner-0.0.1-SNAPSHOT.jar' | head -n 1 || true)"
fi

if [[ -z "$pid" ]]; then
    echo "Campus Runner API is already stopped"
    rm -f "$APP_PID"
    exit 0
fi

echo "Stopping Campus Runner API PID $pid"
kill "$pid"

for _ in {1..30}; do
    if ! kill -0 "$pid" 2>/dev/null; then
        rm -f "$APP_PID"
        echo "Campus Runner API stopped"
        exit 0
    fi
    sleep 1
done

echo "Graceful shutdown timed out; terminating PID $pid" >&2
kill -9 "$pid"
rm -f "$APP_PID"
