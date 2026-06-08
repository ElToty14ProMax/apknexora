# Nexora Web - Vercel verification

Date: 2026-05-16

## Deployment

- Project: `nexora-web`
- Production URL: https://nexora-web-mauve.vercel.app
- Deployment URL: https://nexora-6sbaxswtq-eltoty14promaxs-projects.vercel.app
- Deployment id: `dpl_GWgeCQC7tCq2DJM4gvi2VXsLv6b3`
- Status: `READY`
- Framework detected by Vercel: Vite

## Backend connection

- API URL used by the web app: https://backend-laravel-two.vercel.app
- Backend health check passed from the deployed web origin.
- CORS preflight for `POST /auth/login` returned `204` and allowed:
  - Origin: `https://nexora-web-mauve.vercel.app`
  - Methods: `GET, POST, OPTIONS`
  - Headers: `Content-Type, Authorization, X-Admin-Token, Accept`

## Local build

Command:

```powershell
npm run build
```

Result: passed.

Build output:

- `dist/index.html`
- `dist/assets/index-CSxvYziD.css`
- `dist/assets/index-R4ipge9W.js`

## Production browser test

Tested with Chromium against the production Vercel URL.

Checked flows:

- Public app loads and shows the login screen.
- CPF/Pix confusion case tested with `11976639247`: the UI now explains that this CPF is valid but is not registered as CPF, or the password is wrong, and that a Pix key cannot be used for login.
- Login against the production Laravel backend with an existing verified admin account succeeded.
- Admin workspace opened correctly.
- Admin sections visible: `Solicitacoes abertas`, `Apoios Pix`, `Auditoria`.
- Console errors: `0` unexpected errors.

## Vercel logs

Command:

```powershell
npx --yes vercel@53.3.2 logs --since 1h --level error
```

Result: no logs found for `eltoty14promaxs-projects/nexora-web`, which is expected for a static Vite app with no server functions.

## Notes

- The Laravel backend is now API-only. The old mini frontend was removed from `backend-laravel`; it was not deleted as a backend project.
- The CPF `11976639247` is mathematically valid, but production data has no user registered with that CPF hash. If that number is a Pix key, it is intentionally rejected for login.
- Pix receiver keys are not displayed in the React web app. The UI exposes a copyable Pix code/instruction and requires receipt evidence later through history/admin review.
