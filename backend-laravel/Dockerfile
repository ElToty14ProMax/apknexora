FROM composer:2 AS vendor

WORKDIR /app
COPY composer.json composer.lock ./
RUN composer install \
    --no-dev \
    --no-interaction \
    --prefer-dist \
    --optimize-autoloader \
    --no-scripts

FROM php:8.3-fpm-bookworm

WORKDIR /var/www/nexora
ENV COMPOSER_ALLOW_SUPERUSER=1

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        libfcgi-bin \
        libonig-dev \
        libpq-dev \
        tesseract-ocr \
        tesseract-ocr-por \
        unzip \
    && docker-php-ext-install -j"$(nproc)" mbstring opcache pdo_pgsql \
    && apt-mark manual libonig5 libpq5 \
    && apt-get purge -y --auto-remove libonig-dev libpq-dev \
    && rm -rf /var/lib/apt/lists/*

COPY docker/opcache.ini /usr/local/etc/php/conf.d/10-nexora-opcache.ini
COPY docker/entrypoint.sh /usr/local/bin/nexora-entrypoint
COPY docker/php-fpm-healthcheck.sh /usr/local/bin/nexora-healthcheck
COPY --from=composer:2 /usr/bin/composer /usr/bin/composer
RUN chmod +x /usr/local/bin/nexora-entrypoint /usr/local/bin/nexora-healthcheck

COPY --chown=www-data:www-data . .
COPY --from=vendor --chown=www-data:www-data /app/vendor ./vendor

RUN mkdir -p \
        storage/app/private \
        storage/app/public \
        storage/framework/cache/data \
        storage/framework/sessions \
        storage/framework/testing \
        storage/framework/views \
        storage/logs \
        bootstrap/cache \
    && composer dump-autoload --no-dev --optimize \
    && chown -R www-data:www-data storage bootstrap/cache

EXPOSE 9000

ENTRYPOINT ["nexora-entrypoint"]
CMD ["php-fpm"]
