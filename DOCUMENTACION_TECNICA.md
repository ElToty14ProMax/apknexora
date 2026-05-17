# NEXORA - Documentación Técnica Completa

## 1. Introducción

### 1.1 Descripción del Proyecto

NEXORA es una plataforma de互助 financiera (red de apoyo solidaria) que permite a los usuarios registrados solicitar y proporcionar apoyo financiero entre sí mediante transferencias Pix. El sistema implementa un modelo de reputación basado en niveles y XP donde los usuarios pueden evolucionar conforme participan activamente en la comunidad.

### 1.2 Arquitectura General

El proyecto está compuesto por tres aplicaciones principales:

| Componente | Tecnología | Descripción |
|------------|-------------|-------------|
| **Backend** | Laravel (PHP 8.2+) | API REST con autenticación JWT |
| **Frontend Web** | React 18 + TypeScript + Vite | Interfaz web responsiva |
| **App Móvil** | Kotlin (Android) | Aplicación nativa para Android |
| **Base de Datos** | PostgreSQL (Neon) | Base de datos relacional |

### 1.3 Distribución del Proyecto

```
NEXORA/
├── backend-laravel/     # API REST Laravel
├── nexora-web/          # Frontend React + Vite
├── android/             # App Android Kotlin
└── data/                # Base de datos SQLite (desarrollo)
```

---

## 2. Tecnologías y Dependencias

### 2.1 Backend - Laravel

**Versiones mínimas requeridas:**
- PHP: 8.2+
- Composer: 2.x
- Laravel: 11.x

**Paquetes principales:**
- `laravel/framework`: 11.x - Framework principal
- `symfony/mailgun-mailer`: Servicio de email
- `pda/pheanstalk`: Cola de mensajes (Beanstalk)
- Extensiones PHP requeridas: `pdo_pgsql`, `openssl`, `mbstring`, `Tokenizer`, `XML`, `ctype`, `json`

### 2.2 Frontend - React

**Versiones:**
- Node.js: 20.x LTS
- React: 18.x
- TypeScript: 5.x
- Vite: 5.x
- Lucide React: Iconos
- date-fns: Fechas

**Dependencias del package.json:**
```json
{
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "lucide-react": "^0.468.0",
    "date-fns": "^4.1.0"
  },
  "devDependencies": {
    "@types/react": "^18.3.12",
    "@types/react-dom": "^18.3.1",
    "@vitejs/plugin-react": "^4.3.4",
    "typescript": "^5.6.3",
    "vite": "^6.0.1"
  }
}
```

### 2.3 Base de Datos

**PostgreSQL (Producción):**
- Neon PostgreSQL
- PostgreSQL 15+

**SQLite (Desarrollo):**
- Para desarrollo local sin configuración de BD

---

## 3. Estructura de la Base de Datos

### 3.1 Diagrama de Entidades

```
┌─────────────────┐       ┌─────────────────┐
│     users       │       │  auth_tokens    │
├─────────────────┤       ├─────────────────┤
│ id (PK)         │◄──────│ user_id (FK)    │
│ public_id       │       │ token_hash (PK)│
│ name            │       │ expires_at     │
│ email           │       │ created_at_ms  │
│ cpf_hash        │       └─────────────────┘
│ cpf_cipher      │
│ pix_cipher      │       ┌─────────────────┐
│ password_hash   │       │ support_requests│
│ status          │       ├─────────────────┤
│ role            │       │ id (PK)         │
│ xp              │◄──────│ requester_id   │
│ level           │       │ public_code    │
│ buff_bps        │       │ amount_cents   │
│ invited_by (FK) │       │ funded_cents   │
│ invite_code     │       │ due_days       │
└─────────────────┘       │ status         │
                           └────────┬────────┘
                                    │
                                    │ 1:N
                                    ▼
                           ┌─────────────────┐
                           │  contributions  │
                           ├─────────────────┤
                           │ id (PK)         │
                           │ request_id (FK) │
                           │ donor_id (FK)   │
                           │ amount_cents    │
                           │ status          │
                           │ transaction_id  │
                           │ sender_receipt  │
                           │ receiver_receipt│
                           └─────────────────┘

                           ┌─────────────────┐
                           │   audit_logs    │
                           ├─────────────────┤
                           │ id (PK)         │
                           │ actor_user_id   │
                           │ action          │
                           │ target          │
                           │ created_at_ms   │
                           └─────────────────┘
```

### 3.2 Descripción de Tablas

#### Tabla: `users`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | VARCHAR(36) | UUID único del usuario |
| public_id | VARCHAR(20) | ID público legible (NX-XXXXXXXX) |
| name | VARCHAR(80) | Nombre completo |
| email | VARCHAR(254) | Email único normalizado |
| email_verified | BOOLEAN | Si el email fue verificado |
| verification_code_hash | VARCHAR(128) | Hash del código de verificación |
| verification_expires_at | BIGINT | Timestamp de expiración del código |
| password_reset_code_hash | VARCHAR(128) | Hash del código de recuperación |
| password_reset_expires_at | BIGINT | Timestamp de expiración de recuperación |
| cpf_hash | VARCHAR(64) | Hash del CPF (SHA-256) |
| cpf_cipher | TEXT | CPF encriptado (AES-256-GCM) |
| pix_cipher | TEXT | Clave Pix encriptada |
| password_hash | TEXT | Hash de la contraseña (PBKDF2) |
| status | VARCHAR(40) | PENDING_REVIEW, APPROVED, BLOCKED |
| role | VARCHAR(40) | USER, ADMIN, SUPER_ADMIN |
| xp | BIGINT | Puntos de experiencia acumulados |
| level | INT | Nivel actual (1-1000) |
| buff_bps | INT | Bonus de XP porcentage (0-10000) |
| on_time_returned_cents | BIGINT | Total devuelto en término |
| early_returned_cents | BIGINT | Total devuelto antes de tiempo |
| invited_by | VARCHAR(36) | ID del usuario que invitó |
| invite_code | VARCHAR(8) | Código único de invitación |
| created_at_ms | BIGINT | Timestamp de creación |
| admin_fee_due_cents | BIGINT | Tasa administrativa acumulada |

#### Tabla: `auth_tokens`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| token_hash | VARCHAR(64) | Hash SHA-256 del token (PK) |
| user_id | VARCHAR(36) | ID del usuario (FK) |
| expires_at | BIGINT | Timestamp de expiración |
| created_at_ms | BIGINT | Timestamp de creación |

#### Tabla: `support_requests`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | VARCHAR(36) | UUID único de la solicitud |
| requester_id | VARCHAR(36) | ID del usuario solicitante (FK) |
| public_code | VARCHAR(9) | Código público (AP-XXXXXXX) |
| amount_cents | BIGINT | Monto solicitado en centavos |
| funded_cents | BIGINT | Monto acumulado de apoyos |
| due_days | INT | Días para vencimiento |
| due_at | BIGINT | Timestamp de vencimiento |
| description | TEXT | Descripción de la solicitud |
| status | VARCHAR(40) | PENDING_ADMIN, OPEN, FUNDED, RETURNED, REJECTED |
| created_at_ms | BIGINT | Timestamp de creación |
| approved_at | BIGINT | Timestamp de aprobación |
| returned_at | BIGINT | Timestamp de retorno confirmado |
| rejected_reason | TEXT | Razón del rechazo |

#### Tabla: `contributions`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | VARCHAR(36) | UUID único del apoyo |
| request_id | VARCHAR(36) | ID de la solicitud (FK) |
| donor_id | VARCHAR(36) | ID del usuario que aporta (FK) |
| amount_cents | BIGINT | Monto del apoyo |
| status | VARCHAR(40) | PENDING_ADMIN, CONFIRMED |
| created_at_ms | BIGINT | Timestamp de creación |
| confirmed_at | BIGINT | Timestamp de confirmación |
| transaction_id | VARCHAR(80) | ID de transacción Pix |
| sender_receipt_hash | VARCHAR(64) | Hash SHA-256 del comprobante de envío |
| sender_receipt_image_base64 | LONGTEXT | Imagen del comprobante de envío (Base64) |
| sender_receipt_mime_type | VARCHAR(20) | Tipo MIME de imagen de envío |
| sender_receipt_date | DATE | Fecha del comprobante de envío |
| sender_receipt_submitted_at | BIGINT | Timestamp de envío del comprobante |
| receiver_receipt_hash | VARCHAR(64) | Hash SHA-256 del comprobante de recepción |
| receiver_receipt_image_base64 | LONGTEXT | Imagen del comprobante de recepción |
| receiver_receipt_mime_type | VARCHAR(20) | Tipo MIME de imagen de recepción |
| receiver_receipt_date | DATE | Fecha del comprobante de recepción |
| receiver_receipt_submitted_at | BIGINT | Timestamp de envío del comprobante |

#### Tabla: `audit_logs`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | VARCHAR(36) | UUID único del log |
| actor_user_id | VARCHAR(36) | ID del usuario que realizó la acción (FK nullable) |
| action | VARCHAR(60) | Tipo de acción realizada |
| target | TEXT | Identificador del objeto afectado |
| created_at_ms | BIGINT | Timestamp de la acción |

---

## 4. Backend - Laravel

### 4.1 Estructura de Archivos

```
backend-laravel/
├── app/
│   ├── Http/
│   │   └── Controllers/
│   │       └── NexoraController.php    # Controlador principal
│   ├── Services/
│   │   ├── SecurityService.php          # Criptografía y seguridad
│   │   ├── ReputationRules.php          # Reglas de niveles y XP
│   │   ├── RoadmapRules.php             # Roadmap de capacidad
│   │   ├── CpfValidator.php             # Validación de CPF
│   │   └── PixCopyCode.php              # Generación códigos Pix
│   ├── Exceptions/
│   │   └── ApiException.php             # Manejo de errores API
│   ├── Database/
│   │   └── NeonPostgresConnector.php    # Conector Neon Postgres
│   ├── Providers/
│   │   └── AppServiceProvider.php       # Proveedor de servicios
│   └── Models/
│       └── User.php                     # Modelo de usuario
├── config/
│   ├── nexora.php                       # Configuración específica
│   └── database.php                     # Configuración de BD
├── routes/
│   ├── api.php                          # Rutas de la API
│   └── web.php                          # Rutas web
├── database/
│   └── migrations/                      # Migraciones de BD
├── vercel.json                          # Configuración Vercel
└── bootstrap/
    └── app.php                          # Bootstrap de la aplicación
```

### 4.2 Controlador Principal (NexoraController.php)

El `NexoraController` es el controlador central que maneja todas las operaciones de la API. A continuación se detallan todos los endpoints disponibles:

#### Endpoints de Autenticación

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/health` | Verificación de estado del servicio |
| POST | `/api/auth/register` | Registro de nuevo usuario |
| POST | `/api/auth/resend-verification` | Reenviar código de verificación |
| POST | `/api/auth/recover-password` | Iniciar recuperación de contraseña |
| POST | `/api/auth/reset-password` | Restablecer contraseña con código |
| POST | `/api/auth/verify-email` | Verificar email con código |
| POST | `/api/auth/login` | Iniciar sesión |

#### Endpoints de Usuario

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/me` | Obtener perfil del usuario actual |
| GET | `/api/dashboard` | Obtener estadísticas del dashboard |
| GET | `/api/community` | Listar solicitudes abiertas de otros usuarios |
| GET | `/api/support-requests/mine` | Listar mis solicitudes |
| GET | `/api/support-requests/contributions/mine` | Historial de apoyos |

#### Endpoints de Solicitudes de Apoyo

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/support-requests` | Crear nueva solicitud de apoyo |
| POST | `/api/support-requests/{id}/contributions` | Crear apoyo a una solicitud |
| POST | `/api/support-requests/contributions/auto-split` | Distribuir automáticamente |
| POST | `/api/support-requests/contributions/{id}/receipt` | Enviar comprobante |

#### Endpoints de Administración

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/admin/overview` | Estadísticas generales del sistema |
| GET | `/api/admin/audit-logs` | Logs de auditoría |
| GET | `/api/admin/users` | Listar usuarios (con filtros) |
| POST | `/api/admin/users/{id}/approve` | Aprobar usuario |
| POST | `/api/admin/users/{id}/block` | Bloquear usuario |
| POST | `/api/admin/users/{id}/confirm-admin-fee` | Confirmar tasa administrativa |
| POST | `/api/admin/users/{id}/role` | Actualizar rol de usuario |
| POST | `/api/admin/users/{id}/reputation` | Actualizar reputación (XP/Level) |
| POST | `/api/admin/system/reset-database` | Reiniciar base de datos |
| GET | `/api/admin/support-requests` | Listar solicitudes |
| POST | `/api/admin/support-requests/{id}/approve` | Aprobar solicitud |
| POST | `/api/admin/support-requests/{id}/reject` | Rechazar solicitud |
| POST | `/api/admin/support-requests/{id}/confirm-return` | Confirmar retorno |
| GET | `/api/admin/contributions` | Listar apoyos |
| POST | `/api/admin/contributions/{id}/confirm` | Confirmar apoyo |

### 4.3 Servicios del Backend

#### SecurityService

Encargado de toda la seguridad, criptografía y generación de códigos:

```php
// Métodos principales:
- normalizeEmail(string $email): string           // Normaliza email a minúsculas
- isValidEmail(string $email): bool              // Valida formato de email
- isValidPixKey(string $value): bool              // Valida clave Pix (CPF, email, teléfono, UUID)
- isValidSha256(string $value): bool              // Valida hash SHA-256
- hashCpf(string $cpf): string                    // Hash del CPF con HMAC
- hashToken(string $token): string                // Hash del token de sesión
- hashVerificationCode(string $email, string $code): string
- hashRecoveryCode(string $email, string $code): string
- newVerificationCode(): string                  // Genera código de 6 dígitos
- newToken(): string                              // Genera token aleatorio de 32 bytes
- publicId(): string                              // Genera ID público NX-XXXXXXXX
- inviteCode(): string                            // Genera código de invitación
- supportCode(): string                           // Genera código de solicitud AP-XXXXXXX
- paymentReference(string $contributionId): string // Genera referencia de pago
- hashPassword(string $password): string          // Hash con PBKDF2 (210,000 iteraciones)
- verifyPassword(string $password, string $stored): bool
- encrypt(string $value): string                  // Encripta con AES-256-GCM
- decrypt(string $value): string                  // Desencripta datos
```

**Algoritmos de seguridad:**
- **Contraseñas**: PBKDF2-SHA256 con 210,000 iteraciones
- **Datos sensibles**: AES-256-GCM con IV aleatorio de 12 bytes
- **Hashes**: HMAC-SHA256 con "pepper" de CPF

#### ReputationRules

Define el sistema de niveles y reputación:

```php
// Constantes:
const MIN_HELP_LEVEL = 2              // Nivel mínimo para solicitar ayuda
const MIN_HELP_XP = 100               // XP mínimo para solicitar ayuda
const ADMIN_FEE_BLOCK_LIMIT_CENTS = 500 // Límite de tasa administrativa

// Métodos principales:
- xpRequiredForLevel(int $level): int        // XP necesario para un nivel
- totalXpRequiredToEnterLevel(int $level): int // XP total acumulado hasta nivel
- levelForXp(int $totalXp): int               // Calcula nivel dado XP total
- xpIntoLevel(int $totalXp): int              // XP dentro del nivel actual
- supportLimitCents(int $level): int          // Límite de solicitud por nivel
- adminFeeFor(int $amountCents): int           // Calcula tasa administrativa (1%)
- adminFeeLimitCents(int $level): int         // Límite de tasa por nivel
- canRequestHelp(object|array $user): bool     // Puede solicitar ayuda
- xpForCompletedReturn(int $amountCents, int $buffBps): int // XP por retorno
- recalculateBuffBps(int $onTimeReturnedCents, int $earlyReturnedCents, int $guestsAtLevelFive): int
```

**Tabla de niveles y límites:**

| Nivel | XP Requerido | Límite de Solicitud |
|-------|--------------|---------------------|
| 1 | 0 | R$ 0 (no puede solicitar) |
| 2 | 100 | R$ 100 |
| 3 | 210 | R$ 150 |
| 4 | 463 | R$ 210 |
| 5 | 1,013 | R$ 294 |
| 6 | 2,205 | R$ 382 |
| 7 | 4,796 | R$ 497 |
| 8 | 10,428 | R$ 646 |
| 9 | 22,651 | R$ 840 |
| 10 | 49,153 | R$ 1,092 |

#### RoadmapRules

Controla la capacidad de la comunidad basada en el progreso:

```php
// Métodos principales:
- currentStep(array $levelCounts): array      // Determina el paso actual del roadmap

// Pasos del Roadmap:
[
  ['step' => 1, 'capacity' => 20,   'requirements' => []],
  ['step' => 2, 'capacity' => 50,   'requirements' => [[2, 5]]],
  ['step' => 3, 'capacity' => 100,  'requirements' => [[3, 5], [2, 10]]],
  ['step' => 4, 'capacity' => 200,  'requirements' => [[4, 5], [3, 10], [2, 20]]],
  ['step' => 5, 'capacity' => 350,  'requirements' => [[5, 5], [4, 10], [3, 20]]],
  // ...continúa hasta paso 10
]
```

### 4.4 Configuración del Backend

#### Archivo: `config/nexora.php`

```php
return [
    'env' => env('NEXORA_ENV', 'local'),
    'admin_token' => env('NEXORA_ADMIN_TOKEN', 'dev-admin-token-change-me'),
    'admin_pix_key' => env('NEXORA_ADMIN_PIX_KEY'),
    'pix_merchant_name' => env('NEXORA_PIX_MERCHANT_NAME', 'NEXORA'),
    'pix_merchant_city' => env('NEXORA_PIX_MERCHANT_CITY', 'SAO PAULO'),
    'data_key_b64' => env('NEXORA_DATA_KEY_B64'),
    'cpf_pepper' => env('NEXORA_CPF_PEPPER', '...'),
    'super_admin_email' => env('NEXORA_SUPER_ADMIN_EMAIL', 'admin@nexora.local'),
    'super_admin_cpf' => env('NEXORA_SUPER_ADMIN_CPF', '00000000000'),
    'super_admin_password' => env('NEXORA_SUPER_ADMIN_PASSWORD'),
    'founder_emails' => array_values(array_filter(...)),
];
```

#### Variables de Entorno Requeridas

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `NEXORA_ADMIN_TOKEN` | Token para acceso administrativo | `admin-token-secreto` |
| `NEXORA_ADMIN_PIX_KEY` | Clave Pix para recibir tasas | `minha@chave.pix` |
| `NEXORA_DATA_KEY_B64` | Clave de encriptación (Base64) | `base64key...` |
| `NEXORA_CPF_PEPPER` | Secret para hashing de CPF | `pepper-secreto` |
| `NEXORA_SUPER_ADMIN_EMAIL` | Email del super admin | `admin@nexora.com` |
| `NEXORA_SUPER_ADMIN_CPF` | CPF del super admin | `12345678901` |
| `NEXORA_SUPER_ADMIN_PASSWORD` | Contraseña inicial del super admin | `senha123456` |
| `DATABASE_URL` | URL de conexión PostgreSQL | `postgres://...` |

### 4.5 Autenticación y Seguridad

#### Flujo de Autenticación

1. **Registro:**
   - Usuario envía: name, email, CPF, clave Pix, contraseña
   - Validación de datos
   - Verificación de capacidad del roadmap
   - Creación con status PENDING_REVIEW ( USER) o APPROVED (ADMIN/SUPER_ADMIN)
   - Envío de código de verificación por email

2. **Verificación de Email:**
   - Usuario envía código recibido
   - Se verifica hash y fecha de expiración (30 min)
   - Se marca email_verified = true

3. **Login:**
   - Identificador puede ser email o CPF
   - Verifica contraseña con PBKDF2
   - Verifica email_verified = true
   - Verifica status != BLOCKED
   - Genera token JWT de 7 días

#### Sistema de Roles

| Rol | Permisos |
|-----|-----------|
| USER | Crear solicitudes, contribuir, ver perfil |
| ADMIN | + Gestión de usuarios, solicitudes, contribuciones |
| SUPER_ADMIN | + Cambios de roles, reset de base de datos |

### 4.6 Middleware y Rate Limiting

```php
// Rate limits configurados:
- auth/register: 5 solicitudes / 5 minutos
- auth/resend-verification: 3 solicitudes / 5 minutos
- auth/recover-password: 3 solicitudes / 5 minutos
- auth/reset-password: 6 solicitudes / 5 minutos
- auth/verify-email: 8 solicitudes / 5 minutos
- General API: 12 solicitudes / 1 minuto
```

---

## 5. Frontend - React

### 5.1 Estructura de Archivos

```
nexora-web/
├── src/
│   ├── App.tsx                    # Componente principal
│   ├── api.ts                     # Cliente API
│   ├── types.ts                   # Tipos TypeScript
│   ├── utils.ts                   # Utilidades
│   ├── styles.css                 # Estilos CSS
│   ├── main.tsx                   # Punto de entrada
│   └── vite-env.d.ts              # Definiciones Vite
├── public/
│   ├── favicon.svg                # Favicon
│   └── manifest.webmanifest       # PWA manifest
├── package.json
├── vite.config.ts
├── tsconfig.json
├── index.html
└── vercel.json
```

### 5.2 Componentes Principales

#### App.tsx

Componente principal que gestiona:

- **Estados:**
  - `token`: Token de sesión
  - `profile`: Perfil del usuario
  - `page`: Página actual
  - `dashboard`: Estadísticas del dashboard
  - `community`: Solicitudes de la comunidad
  - `myRequests`: Mis solicitudes
  - `history`: Historial de contribuciones
  - `adminOverview`, `adminUsers`, `adminRequests`, `adminContributions`, `auditLogs`: Datos de admin

- **Páginas disponibles:**
  - `dashboard`: Panel principal
  - `community`: Ver y contribuir a solicitudes
  - `request`: Crear nueva solicitud
  - `history`: Ver historial y enviar comprobantes
  - `profile`: Ver perfil y código de invitación
  - `settings`: Configuración de API
  - `admin`: Panel de administración (solo admins)

#### Componentes Secundarios

| Componente | Descripción |
|------------|-------------|
| `Sidebar` | Navegación lateral con menú |
| `Topbar` | Cabecera con info de usuario y botón actualizar |
| `AuthPanel` | Panel de login/register/recuperar contraseña |
| `DashboardView` | Métricas y resumen del usuario |
| `CommunityView` | Lista de solicitudes abiertas para apoyar |
| `RequestView` | Formulario para crear solicitud |
| `HistoryView` | Historial de contribuciones y comprobantes |
| `ProfileView` | Perfil, invitación, taxa administrativa |
| `SettingsView` | Configuración de URL de API |
| `AdminView` | Panel de administración con tabs |
| `PixModal` | Modal para mostrar código Pix |

### 5.3 Tipos TypeScript

#### Tipos principales en `types.ts`:

```typescript
type Role = "USER" | "ADMIN" | "SUPER_ADMIN";

type Profile = {
  id: string;
  publicId: string;
  name: string;
  email: string;
  status: string;
  role: Role;
  level: number;
  xp: number;
  xpIntoLevel: number;
  xpRequiredThisLevel: number;
  buffBps: number;
  supportLimitCents: number;
  inviteCode: string;
  invitedCount: number;
  adminFeeDueCents: number;
  adminFeeLimitCents: number;
  pixKeyMasked: string;
  adminPixKey: string | null;
};

type Dashboard = {
  communityLiquidityCents: number;
  inCirculationCents: number;
  completionPercent: number;
  activeRequests: number;
  completedOperations: number;
  activeUsers: number;
  userLimitCents: number;
  roadmapStep: number;
  roadmapCapacity: number;
};

type SupportRequest = {
  id: string;
  publicCode: string;
  requesterPublicId: string;
  requesterLevel: number;
  amountCents: number;
  fundedCents: number;
  dueDays: number;
  status: string;
  description: string | null;
  createdAt: number;
};

type ContributionHistory = {
  id: string;
  transactionId: string | null;
  requestPublicCode: string;
  donorPublicId: string;
  receiverPublicId: string;
  direction: "SENT" | "RECEIVED";
  amountCents: number;
  status: string;
  createdAt: number;
  confirmedAt: number | null;
  senderReceiptDate: string | null;
  receiverReceiptDate: string | null;
  hasSenderReceipt: boolean;
  hasReceiverReceipt: boolean;
  evidenceComplete: boolean;
};

// Tipos de administración...
```

### 5.4 Cliente API (api.ts)

Clase `NexoraApi` con métodos:

```typescript
class NexoraApi {
  constructor(baseUrl: string)
  
  // Autenticación
  login(identifier: string, password: string): Promise<LoginResponse>
  register(data: RegisterData): Promise<void>
  verifyEmail(email: string, code: string): Promise<void>
  resendVerification(email: string): Promise<void>
  recoverPassword(email: string): Promise<void>
  resetPassword(email: string, code: string, newPassword: string): Promise<void>
  
  // Usuario
  me(token: string): Promise<Profile>
  dashboard(token: string): Promise<Dashboard>
  community(token: string): Promise<SupportRequest[]>
  myRequests(token: string): Promise<SupportRequest[]>
  myContributions(token: string): Promise<ContributionHistory[]>
  
  // Solicitudes
  createSupportRequest(token: string, data: SupportRequestData): Promise<SupportRequest>
  createContribution(token: string, requestId: string, amountCents: number): Promise<PixInstruction>
  autoSplit(token: string, amountCents: number): Promise<{instructions: PixInstruction[], message: string}>
  submitReceipt(token: string, contributionId: string, data: ReceiptData): Promise<void>
  
  // Administración
  adminOverview(token: string): Promise<AdminOverview>
  adminUsers(token: string): Promise<AdminUser[]>
  adminSupportRequests(token: string): Promise<AdminSupport[]>
  adminContributions(token: string): Promise<AdminContribution[]>
  auditLogs(token: string): Promise<AuditLog[]>
  adminPost(token: string, path: string, body: unknown): Promise<{message: string}>
}
```

### 5.5 Estilos CSS (styles.css)

El archivo contiene:
- Variables CSS para temas (colores, espaciado)
- Reset de estilos
- Layout responsivo (app-shell con sidebar y workspace)
- Componentes: toast, sidebar, topbar, metric-grid, panels, tables, forms, modals, badges
- Estilos para estados: loading, error, success
- Media queries para responsividad

---

## 6. Despliegue en Vercel

### 6.1 Configuración del Backend

#### Archivo: `backend-laravel/vercel.json`

```json
{
  "framework": "laravel",
  "outputDirectory": "public",
  "entryPoint": "public/index.php",
  "routes": {
    "^/api/.*": "@vercel-laravel"
  },
  "env": {
    "NEXORA_ENV": "production",
    "APP_ENV": "production"
  },
  "buildCommand": "composer install --optimize-autoloader --no-dev",
  "phpVersion": "8.2"
}
```

#### Configuración Vercel para Laravel

El backend Laravel se despliega usando la integración oficial de Vercel para Laravel. Se configura:
- Framework: Laravel
- PHP Version: 8.2
- Build Command: `composer install --optimize-autoloader --no-dev`

### 6.2 Configuración del Frontend

#### Archivo: `nexora-web/vercel.json`

El frontend React se despliega automáticamente desde Vercel con:
- Framework: Vite (detección automática)
- Build Command: `npm run build`
- Output Directory: `dist`

### 6.3 Variables de Entorno en Vercel

#### Backend:
```
NEXORA_ENV=production
NEXORA_ADMIN_TOKEN=***
NEXORA_ADMIN_PIX_KEY=***
NEXORA_DATA_KEY_B64=***
NEXORA_CPF_PEPPER=***
NEXORA_SUPER_ADMIN_EMAIL=admin@nexora.com
NEXORA_SUPER_ADMIN_CPF=***
NEXORA_SUPER_ADMIN_PASSWORD=***
DATABASE_URL=postgres://***
```

### 6.4 Flujo de Despliegue

1. **Conectar repositorio a Vercel**
2. **Configurar proyecto backend:**
   - Seleccionar carpeta `backend-laravel`
   - Agregar variables de entorno
   - Deploy automático en push a main

3. **Configurar proyecto frontend:**
   - Seleccionar carpeta `nexora-web`
   - Configurar variable `VITE_API_URL` con URL del backend
   - Deploy automático

4. **Post-deployment:**
   - Verificar health endpoint
   - Probar login
   - Verificar funcionamiento completo

---

## 7. Sistema de Niveles y Reputación

### 7.1 Evolución de Niveles

Los usuarios acumulan XP mediante:
- **Retornos completados**: XP base = monto/100, multiplicado por buff
- **Buff (bonus)**:
  - +10% por cada R$1,000 devuelto en término
  - +30% por cada R$1,000 devuelto antes de tiempo
  - +10% por cada invitado que alcance nivel 5

### 7.2 Privilegios por Nivel

| Nivel | Privilegio |
|-------|-----------|
| 1 | Solo puede contribuir |
| 2+ | Puede solicitar ayuda |
| 3+ | Mayor límite de solicitud |
| 5+ | Invitados dan bonus de XP |

### 7.3 Sistema de Tasas

- **Tasa administrativa**: 1% del monto solicitado
- **Límite de tasa**: R$ 5.00 acumulativo
- Al alcanzar el límite, el usuario se bloquea hasta pagar la tasa
- El admin puede confirmar el pago y desbloquear

---

## 8. Flujo de Operación Completo

### 8.1 Registro de Usuario

```
1. Usuario completa formulario de registro
2. Backend valida: email, CPF, clave Pix, contraseña
3. Verifica capacidad del roadmap
4. Crea usuario en status PENDING_REVIEW o APPROVED
5. Envía código de verificación por email
6. Usuario verifica email con código
7. Si todo correcto, puede usar la plataforma
```

### 8.2 Creación de Solicitud de Apoyo

```
1. Usuario crea solicitud (monto, plazo, descripción)
2. Solicitud queda en status PENDING_ADMIN
3. Admin revisa y aprueba (o rechaza con razón)
4. Si aprobada, queda en status OPEN
5. Otros usuarios pueden contribuir
```

### 8.3 Contribución a Solicitud

```
1. Usuario elige solicitud aberta
2. Ingresa monto a contribuir
3. Backend genera código Pix Copia e Cola
4. Usuario realiza transferencia Pix
5. Ambos (donante y requester) submiten:
   - ID de transacción
   - Foto del comprobante (envío y recepción)
6. Admin valida los comprobantes
7. Si todo correcto, se confirma el apoyo
8. Si solicitud queda completamente financiada → status FUNDED
```

### 8.4 Retorno del Apoyo

```
1. Cuando llega la fecha de vencimiento
2. Requester devuelve el monto
3. Se repite el proceso de comprovantes
4. Admin confirma el retorno
5. Requester recibe XP por el retorno
6. Si раннее, recibe bonus adicional
```

---

## 9. Mantenimiento y Operaciones

### 9.1 Monitoreo

- **Health endpoint**: `GET /api/health` retorna "nexora-backend-laravel"
- **Dashboard**: Estadísticas en tiempo real de la comunidad

### 9.2 Logs de Auditoría

Todas las acciones importantes quedan registradas:
- USER_REGISTERED, USER_APPROVED, USER_BLOCKED
- SUPPORT_REQUEST_CREATED, SUPPORT_REQUEST_APPROVED, SUPPORT_REQUEST_REJECTED
- CONTRIBUTION_CREATED, CONTRIBUTION_CONFIRMED
- PIX_SENDER_RECEIPT_SUBMITTED, PIX_RECEIVER_RECEIPT_SUBMITTED

### 9.3 Reset de Base de Datos

Solo el SUPER_ADMIN puede ejecutar:
```
POST /api/admin/system/reset-database
```

Esto borra todos los datos excepto el super admin bootstrap.

---

## 10. Consideraciones de Seguridad

### 10.1 Protección de Datos

- **CPF**: Hash SHA-256 + encriptado AES-256-GCM
- **Clave Pix**: Solo almacenada encriptada
- **Contraseñas**: PBKDF2 con 210,000 iteraciones
- **Tokens**: Hash SHA-256, expiración 7 días

### 10.2 Validaciones

- Rate limiting en endpoints sensibles
- Validación de CPF con algoritmo oficial
- Validación de claves Pix (CPF, email, teléfono, UUID)
- Verificación de bukti gambar (hash SHA-256)
- IDs de transacción únicos (no duplicados)

### 10.3 Recomendaciones de Producción

1. Cambiar todas las claves por defecto
2. Configurar SSL/TLS
3. Usar PostgreSQL gestionado (Neon)
4. Configurar alertas de errores
5. Implementar backup automático
6. Usar CDN para archivos estáticos
7. Configurar headers de seguridad

---

*Documento generado para NEXORA v1.0*
*Fecha: Mayo 2026*