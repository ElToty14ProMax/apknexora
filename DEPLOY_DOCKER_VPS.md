# Nexora Docker VPS Deploy

Este modo deja el VPS en una forma de produccion completa:

- `caddy`: entrada publica con HTTPS automatico y renovacion de certificados.
- `web`: Nginx interno con la SPA compilada y proxy FastCGI a Laravel.
- `app`: Laravel en PHP 8.3 FPM con OPcache.
- `scheduler`: `php artisan schedule:run` cada minuto.
- Neon sigue fuera de Docker como base gestionada.

## Primera vez

```bash
cd /var/www/nexora
cp .env.docker.example .env
cp backend-laravel/.env.docker.example backend-laravel/.env
nano .env
nano backend-laravel/.env
```

En `.env`, deja los dominios reales:

```env
NEXORA_DOMAIN=nexoraappbr.com
NEXORA_WWW_DOMAIN=www.nexoraappbr.com
ACME_EMAIL=nexora@nexoraappbr.com
```

En `backend-laravel/.env`, rellena como minimo:

- `APP_KEY`
- `DATABASE_URL` de Neon con SSL
- `NEXORA_ADMIN_TOKEN`
- `NEXORA_DATA_KEY_B64`
- `NEXORA_CPF_PEPPER`
- credenciales SMTP
- `OCR_SPACE_API_KEY`

Antes de levantar Caddy, el DNS del dominio debe apuntar al VPS y el firewall debe permitir 80/443.

## Levantar

```bash
docker compose build
docker compose run --rm app php artisan migrate --force
docker compose up -d
docker compose ps
curl -I https://nexoraappbr.com
curl https://nexoraappbr.com/api/health
```

## Actualizar

```bash
cd /var/www/nexora
git pull
docker compose build
docker compose run --rm app php artisan migrate --force
docker compose up -d
docker image prune -f
```

## Local o pruebas sin dominio

Caddy intenta emitir certificados publicos para los dominios configurados. Para una prueba local rapida, puedes exponer temporalmente el servicio `web` cambiando en `docker-compose.yml`:

```yaml
web:
  ports:
    - "8080:8080"
```

Luego:

```bash
docker compose up -d app web
curl http://localhost:8080/api/health
```

Reviértelo antes de produccion para dejar Caddy como unica entrada publica.

## Seguridad y rendimiento

- No subas `.env` ni `backend-laravel/.env`.
- Mantén `APP_DEBUG=false`.
- Mantén Neon con `DB_SSLMODE=require`.
- Usa `NEXORA_CORS_ORIGINS` solo con dominios reales.
- Caddy guarda certificados en el volumen `caddy-data`; no lo borres en cada deploy.
- Los contenedores usan `read_only`, `no-new-privileges`, healthchecks y logs rotados.
- Nginx no cachea API; solo cachea assets versionados.
- OPcache tiene `validate_timestamps=0`; despues de cada cambio debes reconstruir imagen y recrear contenedores.
- Firewall recomendado: solo 22, 80 y 443 abiertos.
