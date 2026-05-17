# Nexora

Nexora is a Brazilian Portuguese Android MVP plus Kotlin/Ktor backend for a community coordination platform. It uses community identifiers, manual approval, Pix instructions, chronological fractional transfers, XP, levels, reputation buffs, invitation progression, duplicate transaction-ID protection, receipt evidence, biometric session unlock, and an administrative fee ledger.

The product copy intentionally avoids terms that could frame the app as a bank or regulated financial provider. This code is an engineering MVP, not a legal opinion or production security certification. Before a Play Store release or real operation, validate the model with Brazilian counsel, a privacy professional, and any required payments/financial regulation specialists.

## Structure

- `android/`: Kotlin Android app using Jetpack Compose.
- `backend-laravel/`: active Laravel API using PostgreSQL.
- `backend/`: previous Kotlin/Ktor API kept intact for reference; the Android app now defaults to Laravel.

## Active Laravel Backend

The Android app default API URL now points to Laravel on port `8000`.

```powershell
cd backend-laravel
composer install
php artisan migrate
php artisan serve --host=0.0.0.0 --port=8000
```

Configure PostgreSQL and Nexora secrets in `backend-laravel/.env`. See `backend-laravel/README.md` for the full list.

## Local Backend

Legacy Kotlin backend notes follow. Use this only if you explicitly want to run the previous implementation.

Set secrets before running. Development defaults exist only to make local testing possible.

```powershell
$env:NEXORA_ADMIN_TOKEN="change-this-admin-token"
$env:NEXORA_DATA_KEY_B64="<base64-32-byte-key>"
$env:NEXORA_CPF_PEPPER="<long-random-pepper>"
$env:NEXORA_ADMIN_PIX_KEY="<your-platform-pix-key>"
$env:NEXORA_CORS_ORIGINS="https://admin.your-domain.com"
$env:NEXORA_SUPER_ADMIN_EMAIL="<founder-email>"
$env:NEXORA_SUPER_ADMIN_CPF="<founder-cpf>"
$env:NEXORA_SUPER_ADMIN_PASSWORD="<only-for-local-bootstrap>"
$env:SMTP_USERNAME="<smtp-user>"
$env:SMTP_PASSWORD="<gmail-app-password>"
```

Run:

```powershell
.\gradlew.bat :backend:run
```

If SMTP settings are missing, the backend logs the verification code in development mode instead of sending email.

## Admin Web

The backend also serves a lightweight admin console:

```text
http://localhost:8080/admin-web/index.html
```

Use either `NEXORA_ADMIN_TOKEN` or log in with a user whose role is `ADMIN` or `SUPER_ADMIN`. The bootstrap super admin is controlled by `NEXORA_SUPER_ADMIN_EMAIL`, `NEXORA_SUPER_ADMIN_CPF`, and `NEXORA_SUPER_ADMIN_PASSWORD`.

## Deployment

This backend is a long-running Kotlin/Ktor JVM service backed by SQLite. It should be deployed to a JVM/container host with persistent storage, or migrated to a managed SQL database before real production use.

Vercel is not a good target for this API in its current form:

- Vercel Functions do not include an official JVM/Kotlin runtime.
- Vercel Functions have a read-only filesystem with only temporary `/tmp` write space, so local SQLite is not durable there.
- If you want Vercel for the admin frontend, host only static/proxy assets there and point them to a separate backend URL.

Production minimums:

- Set `NEXORA_ENV=prod`.
- Use fresh secrets for `NEXORA_ADMIN_TOKEN`, `NEXORA_DATA_KEY_B64`, `NEXORA_CPF_PEPPER`, SMTP, and the platform Pix key.
- Use a random Pix key owned by the platform, not a personal receiver key.
- Set `NEXORA_CORS_ORIGINS` to the exact admin web origin instead of allowing every origin.
- Keep the API behind HTTPS and configure the Android app API URL to that HTTPS endpoint.

## Android

Open the repository in Android Studio and run the `android` configuration. The emulator can use `http://10.0.2.2:8080` as the API URL. A physical Redmi device needs the computer LAN IP, for example `http://192.168.0.10:8080`.

Build a debug APK:

```powershell
.\gradlew.bat :android:assembleDebug
```

## Security Notes

- CPF is mandatory, validated by checksum, hashed for lookup, and encrypted at rest for admin display.
- Public community lists expose only a generated identifier, never CPF, email, or name.
- Passwords use PBKDF2-HMAC-SHA256 with per-user salts.
- API tokens are stored hashed server-side and expire.
- Requests, users, contributions, returns, and administrative fee payments are approved manually.
- Contributions cannot be approved until both sender and receiver receipt photos are attached to the same transaction ID.
- Transaction IDs are stored once and duplicate IDs are blocked from creating repeated history entries.
- Android requests `INTERNET` and `USE_BIOMETRIC` permissions.
- Contribution instructions return a platform Pix copy/paste code and never return the receiver's Pix key to the Android client.
- Receipt dates are assigned by the backend server when the proof is submitted; the client cannot override them.
- Auth endpoints have basic per-IP rate limiting for login, verification, recovery, and reset attempts.

CPF checksum validation does not prove that a CPF belongs to a real, active person. True identity verification requires an official/contracted verification provider or a manual KYC process.

## Policy Pointers

- Receita Federal provides official CPF consultation services: https://www.gov.br/receitafederal/pt-br/servicos/cadastro/cidadao
- Banco Central publishes Pix rules and manuals: https://www.bcb.gov.br/estabilidadefinanceira/pix-normas
- Google Play requires declarations and compliance for financial features: https://support.google.com/googleplay/android-developer/answer/16322411
- Google Play treats government IDs and financial/payment data as sensitive user data: https://support.google.com/googleplay/android-developer/answer/10144311
