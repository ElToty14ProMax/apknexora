# Nexora Vercel Endpoint Test Report

Run timestamp: 2026-05-16T00:50:16Z (2026-05-15 21:50:16 America/Sao_Paulo)

Historical production URL: legacy Vercel backend

Current production API URL: https://nexoraappbr.com/api

Deployment: `dpl_KbiSGyZtPM9sfwY4pYARafx17isR`

Commit tested: `7d23a55` (`fix: use pgsql pooler-compatible prepares`)

Database: Neon PostgreSQL. All Laravel migrations reported `Ran`.

## Result

The production smoke test completed with `40/40` checks passing.

No HTTP 500 logs were found in Vercel for the final run window.

The destructive endpoint `POST /admin/system/reset-database` was intentionally not executed.

## Issues Found And Fixed

1. SMTP failures were causing auth flows to return HTTP 500.
   - Fixed by catching mail transport exceptions in verification and recovery email senders.
   - Verified that `register`, `resend-verification`, and `recover-password` no longer return 500 when SMTP rejects credentials.

2. Neon pooled PostgreSQL transactions failed through PgBouncer/PDO native prepared statements.
   - Fixed PostgreSQL options with `PDO::PGSQL_ATTR_DISABLE_PREPARES=true` and `PDO::ATTR_EMULATE_PREPARES=false`.
   - Verified admin approval, contribution confirmation, and return confirmation on Vercel production.

## Email Status

The API email endpoints are stable and returned success responses:

- `POST /auth/register` -> `201`
- `POST /auth/resend-verification` -> `200`
- `POST /auth/recover-password` -> `200`

Actual inbox delivery was not confirmed during this historical run because the old SMTP credential was rejected during probing. The backend handled that failure without breaking login/register/recovery flows. Current production mail must use the configured Zoho mailbox for `nexora@nexoraappbr.com`.

## Endpoint Coverage

| Check | Expected | Actual |
| --- | --- | --- |
| `GET /health` | 200 | 200 |
| `GET /me` without token | 401 | 401 |
| `POST /auth/login` invalid credentials | 401 | 401 |
| `POST /auth/register` admin test user | 201 | 201 |
| `POST /auth/register` requester | 201 | 201 |
| `POST /auth/register` donor | 201 | 201 |
| `POST /auth/resend-verification` | 200 | 200 |
| `POST /auth/recover-password` | 200 | 200 |
| `POST /auth/verify-email` invalid code | 400 | 400 |
| `POST /auth/login` admin test user | 200 | 200 |
| `GET /admin/overview` | 200 | 200 |
| `POST /auth/login` requester | 200 | 200 |
| `POST /auth/login` donor | 200 | 200 |
| `GET /me` requester | 200 | 200 |
| `GET /dashboard` requester | 200 | 200 |
| `POST /support-requests` | 201 | 201 |
| `GET /support-requests/mine` | 200 | 200 |
| `POST /admin/support-requests/{id}/approve` | 200 | 200 |
| `GET /community` donor | 200 | 200 |
| `POST /support-requests/{id}/contributions` | 201 | 201 |
| `POST /support-requests/contributions/{id}/receipt` sender | 201 | 201 |
| `POST /support-requests/contributions/{id}/receipt` receiver | 201 | 201 |
| `POST /admin/contributions/{id}/confirm` | 200 | 200 |
| `POST /admin/support-requests/{id}/confirm-return` | 200 | 200 |
| `POST /support-requests/contributions/auto-split` | 201 | 201 |
| `POST /admin/support-requests/{id}/reject` | 200 | 200 |
| `POST /admin/users/{id}/reputation` | 200 | 200 |
| `POST /admin/users/{id}/role` | 200 | 200 |
| `POST /admin/users/{id}/block` | 200 | 200 |
| `POST /admin/users/{id}/approve` | 200 | 200 |
| `POST /admin/users/{id}/confirm-admin-fee` | 200 | 200 |
| `GET /support-requests/contributions/mine` donor | 200 | 200 |
| `GET /support-requests/contributions/mine` requester | 200 | 200 |
| `GET /admin/users` | 200 | 200 |
| `GET /admin/support-requests` | 200 | 200 |
| `GET /admin/contributions` | 200 | 200 |
| `GET /admin/audit-logs` | 200 | 200 |

## Payment Privacy Checks

- Contribution instructions returned `receiverPixKey: ""`.
- The receiver visible identifier was the public request code, not the receiver's Pix key.
- Sender and receiver receipt uploads were both required before admin contribution confirmation.
- `pixCopyCode` is generated from `NEXORA_SUPPORT_PIX_KEY`, a platform Pix key, so the app does not embed the requester's Pix key or name in support payment payloads.
- `receiverPixKey` remains empty in the JSON response, so the UI does not display the receiver's Pix key as a separate visible field.
- `NEXORA_ADMIN_PIX_KEY` is reserved for administrative/platform fees. `NEXORA_SUPPORT_PIX_KEY` is required for normal support transfers and can be an EVP/random Pix key or a PJ CNPJ Pix key.

## Test Data

Run id: `20260516004926`

Temporary test users:

- `codex-admin-20260516004926@nexoraappbr.com`
- `codex-req-20260516004926@nexoraappbr.com`
- `codex-donor-20260516004926@nexoraappbr.com`

Because production does not expose verification codes and SMTP delivery is not confirmed yet, the test promoted/verified these temporary users directly in Neon after confirming the public registration endpoints.
