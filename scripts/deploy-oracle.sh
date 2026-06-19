#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="/var/www/nexora"
BACKEND_DIR="${ROOT_DIR}/backend-laravel"
FRONTEND_DIR="${ROOT_DIR}/nexora-web"

echo "Starting Nexora Oracle VPS deployment from ${ROOT_DIR}"

cd "${ROOT_DIR}"
git pull

if [ ! -d "${BACKEND_DIR}" ]; then
  echo "ERROR: ${BACKEND_DIR} does not exist."
  echo "The root repository may still be cloning backend-laravel as a broken gitlink."
  echo "Fix the backend-laravel Git structure first, then redeploy."
  exit 1
fi

if [ ! -d "${FRONTEND_DIR}" ]; then
  echo "ERROR: ${FRONTEND_DIR} does not exist."
  echo "The root repository may still be cloning nexora-web as a broken gitlink."
  echo "Fix the nexora-web Git structure first, then redeploy."
  exit 1
fi

cd "${BACKEND_DIR}"
composer install --no-dev --optimize-autoloader

if [ ! -f .env ]; then
  echo "ERROR: ${BACKEND_DIR}/.env is missing."
  echo "Create it from backend-laravel/.env.oracle.example before continuing."
  exit 1
fi

sudo chown -R www-data:www-data storage bootstrap/cache
sudo find storage bootstrap/cache -type d -exec chmod 775 {} \;
sudo find storage bootstrap/cache -type f -exec chmod 664 {} \;

sudo -u www-data php artisan config:clear
sudo -u www-data php artisan cache:clear
sudo -u www-data php artisan route:clear
sudo -u www-data php artisan view:clear
sudo -u www-data php artisan migrate --force
sudo -u www-data php artisan config:cache
sudo -u www-data php artisan view:cache

if grep -Eq "Route::.*(fn \\(|function \\()" routes/web.php routes/api.php; then
  echo "Skipping php artisan route:cache because closure-based HTTP routes are present."
else
  sudo -u www-data php artisan route:cache
fi

cd "${FRONTEND_DIR}"

if [ ! -f .env.production ]; then
  echo "ERROR: ${FRONTEND_DIR}/.env.production is missing."
  echo "Create it from nexora-web/.env.production.example before continuing."
  exit 1
fi

if [ -f package-lock.json ]; then
  npm ci
else
  npm install
fi

npm run build

if systemctl cat php8.3-fpm.service >/dev/null 2>&1; then
  PHP_FPM_SERVICE="php8.3-fpm"
elif systemctl cat php8.2-fpm.service >/dev/null 2>&1; then
  PHP_FPM_SERVICE="php8.2-fpm"
else
  echo "ERROR: No supported PHP-FPM service was found."
  exit 1
fi

sudo nginx -t
sudo systemctl restart "${PHP_FPM_SERVICE}"
sudo systemctl restart nginx

echo "Nexora Oracle VPS deployment completed successfully."
