#!/usr/bin/env sh
set -eu

SCRIPT_NAME=/index.php \
SCRIPT_FILENAME=/var/www/nexora/public/index.php \
REQUEST_METHOD=GET \
REQUEST_URI=/health \
cgi-fcgi -bind -connect 127.0.0.1:9000 | grep -q 'nexora-backend-laravel'
