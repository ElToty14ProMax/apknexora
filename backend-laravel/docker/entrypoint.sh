#!/usr/bin/env sh
set -eu

mkdir -p \
  storage/app/private \
  storage/app/public \
  storage/framework/cache/data \
  storage/framework/sessions \
  storage/framework/testing \
  storage/framework/views \
  storage/logs \
  bootstrap/cache

chown -R www-data:www-data storage bootstrap/cache 2>/dev/null || true

if [ "${APP_ENV:-production}" = "production" ] && [ "${NEXORA_CACHE_ON_BOOT:-true}" = "true" ]; then
  php artisan config:clear --no-interaction >/dev/null 2>&1 || true
  php artisan route:clear --no-interaction >/dev/null 2>&1 || true
  php artisan view:clear --no-interaction >/dev/null 2>&1 || true
  php artisan config:cache --no-interaction
  php artisan view:cache --no-interaction >/dev/null 2>&1 || true
fi

if [ "${RUN_MIGRATIONS:-false}" = "true" ]; then
  php artisan migrate --force --no-interaction
fi

exec "$@"
