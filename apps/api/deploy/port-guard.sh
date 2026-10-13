#!/bin/bash
set -euo pipefail
# Persistent, narrowly scoped defence; never flush or replace other rules.
for command in iptables ip6tables; do
    if ! "$command" -C INPUT ! -i lo -p tcp --dport 8080 -j REJECT --reject-with tcp-reset 2>/dev/null; then
        "$command" -I INPUT 1 ! -i lo -p tcp --dport 8080 -j REJECT --reject-with tcp-reset
    fi
done
