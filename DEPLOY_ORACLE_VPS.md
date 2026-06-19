# Nexora Oracle VPS Deployment Guide

This guide prepares the existing Nexora production web platform for Oracle Cloud VPS deployment without replacing the current Vercel + Neon setup.

Scope for Oracle VPS:

- Deploy `backend-laravel/` as the active Laravel API.
- Deploy `nexora-web/` as the React + TypeScript + Vite frontend.
- Do not deploy `android/`.
- Do not deploy `backend/` (legacy Kotlin/Ktor backend).
- Do not delete Vercel files, Android files, legacy backend files, or `/var/www/backendnexora` on the VPS.
- Do not commit real secrets.

## 1. Real structure

The live repository structure is:

- `android/`: Android client.
- `backend-laravel/`: active production Laravel API.
- `backend/`: old legacy Kotlin/Ktor backend, keep only as reference.
- `nexora-web/`: React + TypeScript + Vite frontend.

## 2. Critical Git blockers

The original deployment concern was `backend-laravel`, but the live inspection shows two nested repositories stored as gitlinks in the root repository:

- root index entry mode is `160000 backend-laravel`
- root index entry mode is `160000 nexora-web`
- there is no `.gitmodules`
- `backend-laravel/` also contains its own real `.git/` directory
- `nexora-web/` also contains its own real `.git/` directory

This is fragile and is not safe for a normal production clone because:

1. `git clone` of the root repository does not reliably include the real files under `backend-laravel/` and `nexora-web/` as normal root-managed content.
2. `git submodule status` fails because there is no `.gitmodules` mapping.
3. deployment tooling on the VPS can break if `backend-laravel/` or `nexora-web/` is missing or detached from the root history.

For Oracle deployment, the safest target state is:

- `backend-laravel/` becomes a normal tracked directory inside the root repository
- `nexora-web/` becomes a normal tracked directory inside the root repository
- the root repository can be cloned normally into `/var/www/nexora`
- no submodule-like behavior remains for either deployment-critical directory

I did not execute that conversion automatically because it changes Git structure and can discard the nested repo metadata if done carelessly.

## 3. Safest Git fix before VPS clone

Run this from your PC only after reviewing both nested deployment repositories and confirming you want to flatten them into the root repo.

### 3.1 Backup both nested repos first

```powershell
cd D:\Toty\Trabalho\Nexora\NEXORA
git -C backend-laravel status --short --branch
git -C backend-laravel remote -v
git -C backend-laravel bundle create ..\backend-laravel-backup.bundle --all
git -C nexora-web status --short --branch
git -C nexora-web remote -v
git -C nexora-web bundle create ..\nexora-web-backup.bundle --all
```

Those `.bundle` files are your safety net before removing the nested `.git` directories.

### 3.2 Convert both deployment-critical nested repos into normal root-tracked directories

Important: the commands below remove gitlinks from the root index and remove nested `.git` directories from both `backend-laravel/` and `nexora-web/`. Review backup steps first.

```powershell
cd D:\Toty\Trabalho\Nexora\NEXORA
git rm --cached backend-laravel
git rm --cached nexora-web
Remove-Item -Recurse -Force backend-laravel\.git
Remove-Item -Recurse -Force nexora-web\.git
git add backend-laravel
git add nexora-web
```

Expected result after that:

- `git ls-files -s backend-laravel` no longer shows mode `160000`
- `git ls-files -s nexora-web` no longer shows mode `160000`
- the root repo tracks real files under `backend-laravel/`
- the root repo tracks real files under `nexora-web/`
- a VPS clone into `/var/www/nexora` includes both Laravel and the frontend correctly

Before committing, review `git status` carefully. The nested `.gitignore` files remain in place, so `backend-laravel/vendor`, `backend-laravel/node_modules`, `nexora-web/node_modules`, and `nexora-web/dist` should stay ignored, but you should still verify the staged set before pushing.

## 4. Files added for Oracle deployment

- `DEPLOY_ORACLE_VPS.md`
- `nginx/nexoraappbr.com.conf`
- `scripts/deploy-oracle.sh`
- `backend-laravel/.env.oracle.example`
- `nexora-web/.env.production.example`

## 5. Local Git commands from your PC

Use this section only after flattening `backend-laravel/` and `nexora-web/` into normal root-tracked directories.

```powershell
cd D:\Toty\Trabalho\Nexora\NEXORA
git add DEPLOY_ORACLE_VPS.md nginx/nexoraappbr.com.conf backend-laravel/.env.oracle.example nexora-web/.env.production.example
git add --chmod=+x scripts/deploy-oracle.sh
git status
git commit -m "chore: prepare Oracle VPS deployment"
git push nexorarepo main
```

If you flatten both nested repos into the root repo, include those conversion changes in the same commit:

```powershell
cd D:\Toty\Trabalho\Nexora\NEXORA
git add backend-laravel
git add nexora-web
git status
git commit -m "chore: flatten deployment repos and prepare Oracle VPS deployment"
git push nexorarepo main
```

### 5.1 If you keep the current nested-repo structure temporarily

This is not the recommended production state. It works only as a temporary documentation split, because the changes now live across three Git histories:

Root repo:

```powershell
cd D:\Toty\Trabalho\Nexora\NEXORA
git add DEPLOY_ORACLE_VPS.md nginx/nexoraappbr.com.conf scripts/deploy-oracle.sh
git add --chmod=+x scripts/deploy-oracle.sh
git status
git commit -m "chore: add Oracle VPS deployment assets"
git push nexorarepo main
```

Laravel nested repo:

```powershell
cd D:\Toty\Trabalho\Nexora\NEXORA\backend-laravel
git add .env.oracle.example
git status
git commit -m "chore: add Oracle env example"
git push origin main
```

Frontend nested repo:

```powershell
cd D:\Toty\Trabalho\Nexora\NEXORA\nexora-web
git add .env.production.example
git status
git commit -m "chore: add Oracle frontend env example"
git push origin master
```

Even if you do that, a plain root clone into `/var/www/nexora` is still not enough until both nested-repo issues are fixed.

## 6. SSH into the VPS

Oracle VPS:

- IP: `136.248.72.243`
- user: `ubuntu`
- key: `D:\Toty\Trabalho\Nexora\ssh-key-2026-06-10.key`

From Windows PowerShell:

```powershell
ssh -i "D:\Toty\Trabalho\Nexora\ssh-key-2026-06-10.key" ubuntu@136.248.72.243
```

## 7. Clone the full repo into `/var/www/nexora`

Do this only after the `backend-laravel` and `nexora-web` gitlink problems are fixed.

```bash
sudo mkdir -p /var/www
sudo chown -R ubuntu:ubuntu /var/www
cd /var/www
git clone https://github.com/ElToty14ProMax/apknexora.git nexora
cd /var/www/nexora
git remote -v
```

If `backend-laravel/` or `nexora-web/` is still a gitlink without `.gitmodules`, stop here and fix the repository from your PC before continuing.

## 8. Create the real Laravel environment file on the VPS

Copy the example first:

```bash
cd /var/www/nexora/backend-laravel
cp .env.oracle.example .env
nano .env
```

Minimum production values you must fill manually:

```env
APP_NAME=Nexora
APP_ENV=production
APP_KEY=base64:YOUR_REAL_LARAVEL_APP_KEY
APP_DEBUG=false
APP_URL=https://nexoraappbr.com

DB_CONNECTION=pgsql
DATABASE_URL=YOUR_REAL_NEON_DATABASE_URL
DB_SSLMODE=require
DB_DISABLE_PREPARES=true
DB_EMULATE_PREPARES=false

NEXORA_ENV=prod
NEXORA_CORS_ORIGINS=https://nexoraappbr.com,https://www.nexoraappbr.com
NEXORA_ADMIN_TOKEN=YOUR_REAL_ADMIN_TOKEN
NEXORA_DATA_KEY_B64=YOUR_REAL_BASE64_32_BYTE_KEY
NEXORA_CPF_PEPPER=YOUR_REAL_CPF_PEPPER
NEXORA_SUPER_ADMIN_EMAIL=YOUR_REAL_SUPER_ADMIN_EMAIL
NEXORA_SUPER_ADMIN_CPF=YOUR_REAL_SUPER_ADMIN_CPF
NEXORA_SUPER_ADMIN_PASSWORD=YOUR_REAL_SUPER_ADMIN_PASSWORD
```

Generate `APP_KEY` if needed:

```bash
cd /var/www/nexora/backend-laravel
php artisan key:generate --show
```

## 9. Create the frontend production environment file on the VPS

The frontend must point to the public `/api` entry on the domain:

```bash
cd /var/www/nexora/nexora-web
cp .env.production.example .env.production
nano .env.production
```

Expected content:

```env
VITE_API_URL=https://nexoraappbr.com/api
```

## 10. Nginx configuration

The provided config serves:

- frontend from `/var/www/nexora/nexora-web/dist`
- Laravel from `/var/www/nexora/backend-laravel/public/index.php`
- strips `/api` before Laravel sees the request

Examples:

- public `https://nexoraappbr.com/api/health` reaches Laravel as `/health`
- public `https://nexoraappbr.com/api/auth/login` reaches Laravel as `/auth/login`

Copy and enable the config:

```bash
sudo cp /var/www/nexora/nginx/nexoraappbr.com.conf /etc/nginx/sites-available/nexoraappbr.com.conf
sudo ln -sf /etc/nginx/sites-available/nexoraappbr.com.conf /etc/nginx/sites-enabled/nexoraappbr.com.conf
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

## 11. Run the deploy script

The script:

- pulls latest code
- installs Laravel dependencies
- checks that `backend-laravel/.env` exists
- checks that `nexora-web/.env.production` exists
- clears Laravel caches
- runs migrations
- rebuilds frontend
- validates Nginx
- restarts `php8.2-fpm` and `nginx`

Run it like this:

```bash
cd /var/www/nexora
bash scripts/deploy-oracle.sh
```

## 12. Test commands before HTTPS

Local VPS checks:

```bash
curl http://localhost
curl http://localhost/api/health
```

Public HTTP checks:

```bash
curl http://nexoraappbr.com
curl http://nexoraappbr.com/api/health
```

## 13. Certbot setup

Run exactly:

```bash
sudo snap install core
sudo snap refresh core
sudo snap install --classic certbot
sudo ln -sf /snap/bin/certbot /usr/bin/certbot
sudo certbot --nginx -d nexoraappbr.com -d www.nexoraappbr.com
```

## 14. Final HTTPS tests

```bash
curl https://nexoraappbr.com
curl https://nexoraappbr.com/api/health
```

Browser tests:

- `https://nexoraappbr.com`
- `https://nexoraappbr.com/api/health`

## 15. Route cache safety

Do not force `php artisan route:cache` blindly.

Current repository state contains a closure route in `backend-laravel/routes/web.php`:

```php
Route::get('/', fn () => response()->json([...]));
```

Because of that, route caching is not currently safe. The deploy script skips `route:cache` and prints a clear message.

## 16. Frontend API wrapper check

`nexora-web/src/api.ts` already works with:

```env
VITE_API_URL=https://nexoraappbr.com/api
```

Why it works:

- the client trims any trailing slash from `VITE_API_URL`
- each API call appends paths like `/health`, `/auth/login`, `/me`
- final browser requests become:
  - `https://nexoraappbr.com/api/health`
  - `https://nexoraappbr.com/api/auth/login`
  - `https://nexoraappbr.com/api/me`

No frontend code rewrite is required for the `/api` public prefix.

## 17. Backend CORS check

`backend-laravel/app/Http/Middleware/CorsMiddleware.php` already supports a comma-separated list in `NEXORA_CORS_ORIGINS`.

Recommended Oracle value:

```env
NEXORA_CORS_ORIGINS=https://nexoraappbr.com,https://www.nexoraappbr.com
```

If you still need Vercel web clients during transition, append them:

```env
NEXORA_CORS_ORIGINS=https://nexoraappbr.com,https://www.nexoraappbr.com,https://nexora-web-mauve.vercel.app,https://nexora-6sbaxswtq-eltoty14promaxs-projects.vercel.app
```

## 18. Known warnings

1. `backend-laravel` is still unsafe for production cloning until the gitlink/nested repo issue is fixed.
2. `nexora-web` is also still unsafe for production cloning until its gitlink/nested repo issue is fixed.
3. The deploy script assumes `php8.2-fpm` is installed and uses socket `unix:/run/php/php8.2-fpm.sock`.
4. The frontend build will be wrong for Oracle if `nexora-web/.env.production` is missing.
5. The current guide intentionally leaves Vercel and Neon untouched.
