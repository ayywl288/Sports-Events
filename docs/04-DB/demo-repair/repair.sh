#!/bin/sh
set -eu

export MYSQL_PWD="$MYSQL_PASSWORD"
exec mysql --protocol=TCP -h 127.0.0.1 -u "$MYSQL_USER" \
    --default-character-set=utf8mb4 "$MYSQL_DATABASE" \
    < /opt/sports-events/demo-repair/repair.sql
