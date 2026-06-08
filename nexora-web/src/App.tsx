import {
  AlertCircle,
  Check,
  ChevronRight,
  Clipboard,
  Clock3,
  Copy,
  CreditCard,
  Download,
  Eye,
  History,
  LayoutDashboard,
  Loader2,
  LogOut,
  MessageCircle,
  RefreshCw,
  Search,
  Send,
  Settings,
  ShieldCheck,
  UserCheck,
  UserRound,
  Users,
  X,
  ZoomIn,
  ZoomOut,
  RotateCcw,
} from "lucide-react";
import { ChangeEvent, FormEvent, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ApiError, DEFAULT_API_URL, NexoraApi } from "./api";
import type {
  AdminContribution,
  AdminOverview,
  AdminSupport,
  AdminUser,
  AuditLog,
  ContributionHistory,
  Dashboard,
  LoginResponse,
  OcrResult,
  PixInstruction,
  Profile,
  SupportRequest,
} from "./types";
import { compactId, copyText, dateTime, fileToBase64, isRandomPixKey, isValidCpf, money, onlyDigits, sha256Hex } from "./utils";

type Page = "dashboard" | "community" | "request" | "history" | "profile" | "admin" | "settings";
type AdminTab = "users" | "requests" | "contributions";
type Notice = { text: string; kind: "ok" | "error" } | null;
type BeforeInstallPromptEvent = Event & {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: "accepted" | "dismissed" }>;
};

const tokenKey = "nexora.web.token";
const apiKey = "nexora.web.apiUrl";

const initialInvite = new URLSearchParams(window.location.search).get("invite") || "";

function cn(...values: Array<string | false | null | undefined>) {
  return values.filter(Boolean).join(" ");
}

function roleLabel(role: string): string {
  return role === "SUPER_ADMIN" ? "Administrador geral" : role === "ADMIN" ? "Administrador" : "Usuário";
}

function statusLabel(status: string): string {
  const labels: Record<string, string> = {
    PENDING_REVIEW: "Aguardando revisão",
    PENDING_ADMIN: "Aguardando comprovantes",
    APPROVED: "Aprovado",
    BLOCKED: "Bloqueado",
    OPEN: "Aberto",
    FUNDED: "Completo",
    RETURNED: "Retornado",
    CONFIRMED: "Validado",
    REJECTED: "Recusado",
    EXPIRED: "Expirado",
    CANCELLED: "Cancelado",
  };
  return labels[status] || status.replace(/_/g, " ").toLowerCase();
}

function formatBirthdateInput(value: string): string {
  const digits = onlyDigits(value).slice(0, 8);
  if (digits.length <= 2) return digits;
  if (digits.length <= 4) return `${digits.slice(0, 2)}/${digits.slice(2)}`;
  return `${digits.slice(0, 2)}/${digits.slice(2, 4)}/${digits.slice(4)}`;
}

function parseBirthdateInput(value: string): { date: Date; iso: string } | null {
  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value.trim());
  if (!match) return null;
  const day = Number(match[1]);
  const month = Number(match[2]);
  const year = Number(match[3]);
  const date = new Date(year, month - 1, day);
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) {
    return null;
  }
  return {
    date,
    iso: `${year.toString().padStart(4, "0")}-${month.toString().padStart(2, "0")}-${day.toString().padStart(2, "0")}`,
  };
}

function auditActionLabel(action: string): string {
  const labels: Record<string, string> = {
    USER_REGISTERED: "Usuário cadastrado",
    USER_APPROVED: "Usuário aprovado",
    USER_BLOCKED: "Usuário bloqueado",
    SUPPORT_REQUEST_CREATED: "Solicitação criada",
    SUPPORT_REQUEST_APPROVED: "Solicitação aprovada",
    SUPPORT_REQUEST_REJECTED: "Solicitação recusada",
    SUPPORT_REQUEST_RETURNED: "Retorno validado",
    CONTRIBUTION_CREATED: "Apoio criado",
    CONTRIBUTION_CONFIRMED: "Pix validado",
    PIX_SENDER_RECEIPT_SUBMITTED: "Comprovante de envio recebido",
    PIX_RECEIVER_RECEIPT_SUBMITTED: "Comprovante de recebimento recebido",
    ADMIN_FEE_CONFIRMED: "Taxa baixada",
    PASSWORD_RESET: "Senha redefinida",
    SUPER_ADMIN_BOOTSTRAPPED: "Administrador inicializado",
  };
  return labels[action] || action.replace(/_/g, " ").toLowerCase();
}

export function App() {
  const [apiUrl, setApiUrl] = useState(localStorage.getItem(apiKey) || DEFAULT_API_URL);
  const apiRef = useRef(new NexoraApi(apiUrl));
  const [token, setToken] = useState(localStorage.getItem(tokenKey) || "");
  const [profile, setProfile] = useState<Profile | null>(null);
  const [page, setPage] = useState<Page>(token ? "dashboard" : "dashboard");
  const [notice, setNotice] = useState<Notice>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [community, setCommunity] = useState<SupportRequest[]>([]);
  const [myRequests, setMyRequests] = useState<SupportRequest[]>([]);
  const [history, setHistory] = useState<ContributionHistory[]>([]);
  const [adminOverview, setAdminOverview] = useState<AdminOverview | null>(null);
  const [adminUsers, setAdminUsers] = useState<AdminUser[]>([]);
  const [adminRequests, setAdminRequests] = useState<AdminSupport[]>([]);
  const [adminContributions, setAdminContributions] = useState<AdminContribution[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [instruction, setInstruction] = useState<PixInstruction | null>(null);

  const api = apiRef.current;
  const isAdmin = profile?.role === "ADMIN" || profile?.role === "SUPER_ADMIN";

  const showNotice = useCallback((text: string, kind: "ok" | "error" = "ok") => {
    setNotice({ text, kind });
    window.clearTimeout(Number(window.__nexoraNoticeTimer));
    window.__nexoraNoticeTimer = window.setTimeout(() => setNotice(null), 5200);
  }, []);

  const saveSession = useCallback((login: LoginResponse) => {
    localStorage.setItem(tokenKey, login.token);
    setToken(login.token);
    setProfile(login.profile);
    setPage("dashboard");
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(tokenKey);
    setToken("");
    setProfile(null);
    setDashboard(null);
    setCommunity([]);
    setMyRequests([]);
    setHistory([]);
    setAdminOverview(null);
    setAdminUsers([]);
    setAdminRequests([]);
    setAdminContributions([]);
    setAuditLogs([]);
    setInstruction(null);
  }, []);

  const loadWorkspace = useCallback(
    async (sessionToken = token, silent = false) => {
      if (!sessionToken) return;
      if (!silent) setRefreshing(true);
      try {
        const nextProfile = await api.me(sessionToken);
        const [nextDashboard, nextCommunity, nextRequests, nextHistory] = await Promise.all([
          api.dashboard(sessionToken),
          api.community(sessionToken),
          api.myRequests(sessionToken),
          api.myContributions(sessionToken),
        ]);
        setProfile(nextProfile);
        setDashboard(nextDashboard);
        setCommunity(nextCommunity);
        setMyRequests(nextRequests);
        setHistory(nextHistory);

        if (nextProfile.role === "ADMIN" || nextProfile.role === "SUPER_ADMIN") {
          const [overview, users, requests, contributions, logs] = await Promise.all([
            api.adminOverview(sessionToken),
            api.adminUsers(sessionToken),
            api.adminSupportRequests(sessionToken),
            api.adminContributions(sessionToken),
            api.auditLogs(sessionToken),
          ]);
          setAdminOverview(overview);
          setAdminUsers(users);
          setAdminRequests(requests);
          setAdminContributions(contributions);
          setAuditLogs(logs);
        }
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) logout();
        showNotice(error instanceof Error ? error.message : "Erro ao atualizar.", "error");
      } finally {
        if (!silent) setRefreshing(false);
      }
    },
    [api, logout, showNotice, token],
  );

  useEffect(() => {
    if (token) void loadWorkspace(token, false);
  }, [loadWorkspace, token]);

  const updateApiUrl = (value: string) => {
    const clean = value.trim().replace(/\/$/, "");
    setApiUrl(clean);
    api.setBaseUrl(clean);
    localStorage.setItem(apiKey, clean);
    showNotice("API atualizada.");
  };

  return (
    <div className="app-shell">
      {notice && (
        <div className={cn("toast", notice.kind === "error" && "toast-error")} role="status">
          {notice.kind === "error" ? <AlertCircle size={18} /> : <Check size={18} />}
          <span>{notice.text}</span>
        </div>
      )}

      <Sidebar
        page={page}
        setPage={setPage}
        profile={profile}
        isAdmin={isAdmin}
        onLogout={logout}
      />

      <main className="workspace">
        <Topbar
          profile={profile}
          refreshing={refreshing}
          onRefresh={() => void loadWorkspace(token)}
        />

        {!token || !profile ? (
          <AuthPanel api={api} onLogin={saveSession} showNotice={showNotice} initialInvite={initialInvite} />
        ) : (
          <>
            {page === "dashboard" && (
              <DashboardView
                profile={profile}
                dashboard={dashboard}
                myRequests={myRequests}
                history={history}
                onNavigate={setPage}
              />
            )}
            {page === "community" && (
              <CommunityView
                token={token}
                api={api}
                community={community}
                busyAction={busyAction}
                setBusyAction={setBusyAction}
                showNotice={showNotice}
                onInstruction={setInstruction}
                reload={() => loadWorkspace(token, true)}
              />
            )}
            {page === "request" && (
              <RequestView
                token={token}
                api={api}
                profile={profile}
                showNotice={showNotice}
                reload={() => loadWorkspace(token, true)}
              />
            )}
            {page === "history" && (
              <HistoryView
                token={token}
                api={api}
                history={history}
                showNotice={showNotice}
                reload={() => loadWorkspace(token, true)}
              />
            )}
            {page === "profile" && <ProfileView profile={profile} showNotice={showNotice} />}
            {page === "settings" && <SettingsView apiUrl={apiUrl} onSave={updateApiUrl} />}
            {page === "admin" && isAdmin && (
              <AdminView
                token={token}
                api={api}
                overview={adminOverview}
                users={adminUsers}
                requests={adminRequests}
                contributions={adminContributions}
                logs={auditLogs}
                busyAction={busyAction}
                setBusyAction={setBusyAction}
                showNotice={showNotice}
                reload={() => loadWorkspace(token, true)}
              />
            )}
          </>
        )}
      </main>

      {instruction && <PixModal instruction={instruction} onClose={() => setInstruction(null)} showNotice={showNotice} />}
    </div>
  );
}

function Sidebar({
  page,
  setPage,
  profile,
  isAdmin,
  onLogout,
}: {
  page: Page;
  setPage: (page: Page) => void;
  profile: Profile | null;
  isAdmin: boolean;
  onLogout: () => void;
}) {
  const items: Array<{ page: Page; label: string; icon: JSX.Element; admin?: boolean }> = [
    { page: "dashboard", label: "Painel", icon: <LayoutDashboard size={18} /> },
    { page: "community", label: "Comunidade", icon: <Users size={18} /> },
    { page: "request", label: "Solicitar", icon: <CreditCard size={18} /> },
    { page: "history", label: "Histórico", icon: <History size={18} /> },
    { page: "profile", label: "Perfil", icon: <UserRound size={18} /> },
    { page: "admin", label: "Admin", icon: <ShieldCheck size={18} />, admin: true },
    { page: "settings", label: "Ajustes", icon: <Settings size={18} /> },
  ];

  return (
    <aside className="sidebar">
      <div className="brand">
        <img className="brand-logo" src="/nexora-logo.png" alt="" />
        <div>
          <strong>Nexora</strong>
          <small>Rede solidária</small>
        </div>
      </div>
      <nav>
        {items
          .filter((item) => !item.admin || isAdmin)
          .map((item) => (
            <button
              key={item.page}
              className={cn("nav-item", page === item.page && "active")}
              onClick={() => setPage(item.page)}
            >
              {item.icon}
              <span>{item.label}</span>
            </button>
          ))}
      </nav>
      {profile && (
        <div className="sidebar-profile">
          <strong title={profile.name}>{profile.name}</strong>
          <span>{profile.publicId}</span>
          <button className="ghost danger-text" onClick={onLogout}>
            <LogOut size={16} />
            Sair
          </button>
        </div>
      )}
    </aside>
  );
}

function Topbar({
  profile,
  refreshing,
  onRefresh,
}: {
  profile: Profile | null;
  refreshing: boolean;
  onRefresh: () => void;
}) {
  return (
    <header className="topbar">
      <div>
        <h1>{profile ? "Painel Nexora" : "Entrar na Nexora"}</h1>
        <p>{profile ? roleLabel(profile.role) : "Use sua conta para acessar comunidade e administração."}</p>
      </div>
      {profile && (
        <button className="refresh-button" onClick={onRefresh} disabled={refreshing}>
          {refreshing ? <Loader2 className="spin" size={18} /> : <RefreshCw size={18} />}
          {refreshing ? "Atualizando" : "Atualizar"}
        </button>
      )}
    </header>
  );
}

function AuthPanel({
  api,
  onLogin,
  showNotice,
  initialInvite,
}: {
  api: NexoraApi;
  onLogin: (login: LoginResponse) => void;
  showNotice: (text: string, kind?: "ok" | "error") => void;
  initialInvite: string;
}) {
  const [mode, setMode] = useState<"login" | "register" | "verify" | "recover">("login");
  const [busy, setBusy] = useState(false);
  const [loginIdentifier, setLoginIdentifier] = useState("");
  const [loginPassword, setLoginPassword] = useState("");
  const [register, setRegister] = useState({
    name: "",
    email: "",
    cpf: "",
    birthdate: "",
    pixKey: "",
    password: "",
    inviteCode: initialInvite,
  });
  const [verifyEmail, setVerifyEmail] = useState("");
  const [verifyCode, setVerifyCode] = useState("");
  const [recoverEmail, setRecoverEmail] = useState("");
  const [recoverCode, setRecoverCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [registerPasswordCache, setRegisterPasswordCache] = useState("");
  const [installPrompt, setInstallPrompt] = useState<BeforeInstallPromptEvent | null>(null);

  useEffect(() => {
    const captureInstallPrompt = (event: Event) => {
      event.preventDefault();
      setInstallPrompt(event as BeforeInstallPromptEvent);
    };
    window.addEventListener("beforeinstallprompt", captureInstallPrompt);
    return () => window.removeEventListener("beforeinstallprompt", captureInstallPrompt);
  }, []);

  const installApp = async () => {
    if (!installPrompt) {
      showNotice("Abra o menu do navegador e escolha Instalar app ou Adicionar à tela inicial.");
      return;
    }
    await installPrompt.prompt();
    setInstallPrompt(null);
  };

  const submitLogin = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    try {
      const login = await api.login(loginIdentifier.trim(), loginPassword);
      onLogin(login);
      showNotice("Sessão iniciada.");
    } catch (error) {
      if (
        error instanceof ApiError &&
        error.status === 403 &&
        error.message.toLowerCase().includes("e-mail")
      ) {
        setVerifyEmail(loginIdentifier.trim());
        setMode("verify");
        showNotice("Digite o código do e-mail para continuar.", "error");
        return;
      }
      const digits = onlyDigits(loginIdentifier);
      const hint =
        digits.length === 11 && isValidCpf(digits)
          ? "Esse CPF é válido, mas não existe como CPF cadastrado ou a senha não confere. Chave Pix não entra no login."
          : error instanceof Error
            ? error.message
            : "Login falhou.";
      showNotice(hint, "error");
    } finally {
      setBusy(false);
    }
  };

  const submitRegister = async (event: FormEvent) => {
    event.preventDefault();
    const cpfDigits = onlyDigits(register.cpf);
    if (!isValidCpf(cpfDigits)) {
      showNotice("CPF inválido. Use um CPF real com 11 dígitos.", "error");
      return;
    }
    if (!register.birthdate) {
      showNotice("Informe a data de nascimento.", "error");
      return;
    }
    const parsedBirthdate = parseBirthdateInput(register.birthdate);
    if (!parsedBirthdate) {
      showNotice("Data de nascimento inválida. Use DD/MM/AAAA.", "error");
      return;
    }
    const birthDate = parsedBirthdate.date;
    const today = new Date();
    const minAge = new Date(today.getFullYear() - 13, today.getMonth(), today.getDate());
    const maxAge = new Date(today.getFullYear() - 120, today.getMonth(), today.getDate());
    if (birthDate > minAge) {
      showNotice("Precisa ter pelo menos 13 anos para se cadastrar.", "error");
      return;
    }
    if (birthDate < maxAge) {
      showNotice("Data de nascimento inválida.", "error");
      return;
    }
    if (!isRandomPixKey(register.pixKey)) {
      showNotice("Use apenas a chave Pix aleatória gerada pelo banco. CPF, e-mail e telefone não são aceitos.", "error");
      return;
    }
    setBusy(true);
    try {
      await api.register({ ...register, cpf: cpfDigits, birthdate: parsedBirthdate.iso });
      setVerifyEmail(register.email);
      setRegisterPasswordCache(register.password);
      setMode("verify");
      showNotice("Cadastro criado. Digite o código enviado por e-mail.");
    } catch (error) {
      showNotice(error instanceof Error ? error.message : "Cadastro falhou.", "error");
    } finally {
      setBusy(false);
    }
  };

  const submitVerify = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    try {
      await api.verifyEmail(verifyEmail, verifyCode.trim());
      showNotice("Email verificado.");
      const cachedPassword = registerPasswordCache || (verifyEmail.trim() === loginIdentifier.trim() ? loginPassword : "");
      if (cachedPassword) {
        const login = await api.login(verifyEmail, cachedPassword);
        onLogin(login);
      } else {
        setLoginIdentifier(verifyEmail);
        setMode("login");
      }
    } catch (error) {
      showNotice(error instanceof Error ? error.message : "Código inválido.", "error");
    } finally {
      setBusy(false);
    }
  };

  const submitRecover = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    try {
      if (!recoverCode.trim()) {
        await api.recoverPassword(recoverEmail);
        showNotice("Se o e-mail existir, o código será enviado.");
      } else {
        if (newPassword.length < 8 || newPassword !== confirmPassword) {
          showNotice("A nova senha precisa ter 8 caracteres e confirmar igual.", "error");
          return;
        }
        await api.resetPassword(recoverEmail, recoverCode.trim(), newPassword);
        const login = await api.login(recoverEmail, newPassword);
        onLogin(login);
        showNotice("Senha alterada. Você voltou ao painel.");
      }
    } catch (error) {
      showNotice(error instanceof Error ? error.message : "Recuperação falhou.", "error");
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="auth-layout">
      <div className="auth-copy">
        <img className="auth-logo" src="/nexora-logo.png" alt="Nexora" />
        <span className="eyebrow">Nexora Web</span>
        <h2>Comunidade, Pix e administração em uma área de trabalho.</h2>
        <p>Acesse sua conta para acompanhar solicitações, convites, comprovantes e validações.</p>
        <button className="secondary install-button" onClick={installApp}>
          <Download size={18} />
          Instalar app
        </button>
      </div>

      <div className="auth-card">
        <div className="segmented">
          <button className={cn(mode === "login" && "active")} onClick={() => setMode("login")}>Entrar</button>
          <button className={cn(mode === "register" && "active")} onClick={() => setMode("register")}>Criar conta</button>
          <button className={cn(mode === "recover" && "active")} onClick={() => setMode("recover")}>Recuperar</button>
        </div>

        {mode === "login" && (
          <form onSubmit={submitLogin} className="form-grid">
            <label>
              Email ou CPF
              <input
                value={loginIdentifier}
                onChange={(event) => setLoginIdentifier(event.target.value)}
                placeholder="email@exemplo.com ou CPF"
                autoComplete="username"
                autoCorrect="off"
                spellCheck={false}
                required
              />
            </label>
            <label>
              Senha
              <input
                value={loginPassword}
                onChange={(event) => setLoginPassword(event.target.value)}
                type="password"
                autoComplete="current-password"
                autoCorrect="off"
                spellCheck={false}
                required
              />
            </label>
            <small className="field-help">Login por CPF usa o CPF cadastrado. Chave Pix não autentica.</small>
            <SubmitButton busy={busy} label="Entrar" />
          </form>
        )}

        {mode === "register" && (
          <form onSubmit={submitRegister} className="form-grid">
            <label>
              Nome completo
              <input value={register.name} onChange={(event) => setRegister({ ...register, name: event.target.value })} required minLength={2} />
            </label>
            <label>
              Email
              <input type="email" value={register.email} onChange={(event) => setRegister({ ...register, email: event.target.value })} required />
            </label>
            <label>
              CPF
              <input value={register.cpf} onChange={(event) => setRegister({ ...register, cpf: event.target.value })} inputMode="numeric" required />
            </label>
            <label>
              Data de nascimento
              <input
                value={register.birthdate}
                onChange={(event) => setRegister({ ...register, birthdate: formatBirthdateInput(event.target.value) })}
                inputMode="numeric"
                placeholder="DD/MM/AAAA"
                maxLength={10}
                required
              />
            </label>
            <label>
              Chave Pix aleatória
              <input value={register.pixKey} onChange={(event) => setRegister({ ...register, pixKey: event.target.value.trim() })} placeholder="xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx" required />
              <small className="field-help">Use a chave aleatória EVP gerada pelo banco. CPF, e-mail e telefone não são aceitos.</small>
            </label>
            <label>
              Senha
              <input
                type="password"
                value={register.password}
                onChange={(event) => setRegister({ ...register, password: event.target.value })}
                autoComplete="new-password"
                autoCorrect="off"
                spellCheck={false}
                required
                minLength={8}
              />
            </label>
            <label>
              Código de convite
              <input value={register.inviteCode} onChange={(event) => setRegister({ ...register, inviteCode: event.target.value })} />
            </label>
            <SubmitButton busy={busy} label="Criar conta" />
          </form>
        )}

        {mode === "verify" && (
          <form onSubmit={submitVerify} className="form-grid">
            <label>
              Email
              <input type="email" value={verifyEmail} onChange={(event) => setVerifyEmail(event.target.value)} required />
            </label>
            <label>
              Código
              <input
                value={verifyCode}
                onChange={(event) => setVerifyCode(event.target.value)}
                inputMode="numeric"
                autoComplete="one-time-code"
                required
              />
            </label>
            <div className="split-actions">
              <SubmitButton busy={busy} label="Verificar" />
              <button type="button" className="secondary" onClick={() => void api.resendVerification(verifyEmail).then(() => showNotice("Código reenviado."))}>
                Reenviar código
              </button>
            </div>
          </form>
        )}

        {mode === "recover" && (
          <form onSubmit={submitRecover} className="form-grid">
            <label>
              Email
              <input type="email" value={recoverEmail} onChange={(event) => setRecoverEmail(event.target.value)} required />
            </label>
            <label>
              Código recebido
              <input value={recoverCode} onChange={(event) => setRecoverCode(event.target.value)} inputMode="numeric" autoComplete="one-time-code" />
            </label>
            {recoverCode.trim().length >= 6 && (
              <>
                <label>
                  Nova senha
                  <input type="password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} autoComplete="new-password" required minLength={8} />
                </label>
                <label>
                  Confirmar nova senha
                  <input type="password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} autoComplete="new-password" required minLength={8} />
                </label>
              </>
            )}
            <div className="split-actions">
              <SubmitButton busy={busy} label={recoverCode.trim().length >= 6 ? "Alterar senha" : "Enviar código"} />
              {recoverEmail && (
                <button type="button" className="secondary" onClick={() => void api.recoverPassword(recoverEmail).then(() => showNotice("Código reenviado."))}>
                  Reenviar código
                </button>
              )}
            </div>
          </form>
        )}
        <AuthFaq />
      </div>
    </section>
  );
}

function AuthFaq() {
  return (
    <section className="faq-mini" aria-label="FAQ">
      <h3>FAQ | Preguntas Frecuentes</h3>
      <details>
        <summary>¿Qué copio para invitar?</summary>
        <p>Copia solo el código de invitación. El enlace es para compartir o instalar la app desde la página.</p>
      </details>
      <details>
        <summary>¿Qué Pix debo usar?</summary>
        <p>Usa siempre el código Pix que genera Nexora. La clave de quien recibe no se muestra separada.</p>
      </details>
      <details>
        <summary>¿Dónde envío el comprobante?</summary>
        <p>Después de transferir, sube la foto desde tu historial para que la administración valide el apoyo.</p>
      </details>
      <p className="faq-contact">
        Contacto: <a href="mailto:frankegr14@gmail.com">frankegr14@gmail.com</a> · <a href="https://wa.me/5511913463247">+5511913463247</a>
      </p>
    </section>
  );
}

function SubmitButton({ busy, label }: { busy: boolean; label: string }) {
  return (
    <button type="submit" className="primary" disabled={busy}>
      {busy ? <Loader2 className="spin" size={18} /> : <ChevronRight size={18} />}
      {label}
    </button>
  );
}

function DashboardView({
  profile,
  dashboard,
  myRequests,
  history,
  onNavigate,
}: {
  profile: Profile;
  dashboard: Dashboard | null;
  myRequests: SupportRequest[];
  history: ContributionHistory[];
  onNavigate: (page: Page) => void;
}) {
  const activeMine = myRequests.filter((item) => ["PENDING_ADMIN", "OPEN", "FUNDED"].includes(item.status));
  const completed = history.filter((item) => item.status === "CONFIRMED").length;
  return (
    <section className="view-stack">
      <div className="hero-panel">
        <div>
          <span className="eyebrow">Bem-vindo</span>
          <h2 title={profile.name}>{profile.name}</h2>
          <p>Nivel {profile.level} | XP: {profile.xp} | Limite: {money(profile.supportLimitCents)}</p>
        </div>
        <button className="secondary" onClick={() => onNavigate("profile")}>
          Ver convite
        </button>
      </div>
      <div className="metric-grid">
        <Metric label="Solicitacoes ativas" value={dashboard?.activeRequests ?? 0} />
        <Metric label="Operações concluídas" value={dashboard?.completedOperations ?? completed} />
        <Metric label="Usuários ativos" value={dashboard?.activeUsers ?? 0} />
        <Metric label="Em circulação" value={money(dashboard?.inCirculationCents ?? 0)} />
        <Metric label="Meu limite" value={money(dashboard?.userLimitCents ?? profile.supportLimitCents)} />
        <Metric label="Taxa pendente" value={money(profile.adminFeeDueCents)} tone={profile.adminFeeDueCents > 0 ? "warn" : "ok"} />
      </div>
      <div className="two-columns">
        <Panel title="Minhas solicitações ativas">
          <ListEmpty show={activeMine.length === 0} text="Nenhuma solicitação ativa." />
          {activeMine.map((item) => <SupportRow key={item.id} item={item} />)}
        </Panel>
        <Panel title="Ultimos apoios">
          <ListEmpty show={history.length === 0} text="Nenhum apoio ainda." />
          {history.slice(0, 6).map((item) => (
            <div className="row-card" key={item.id}>
              <div>
                <strong>{item.requestPublicCode}</strong>
                <span>{item.direction === "SENT" ? "Enviado" : "Recebido"} - {statusLabel(item.status)}</span>
              </div>
              <b>{money(item.amountCents)}</b>
            </div>
          ))}
        </Panel>
      </div>
    </section>
  );
}

function CommunityView({
  token,
  api,
  community,
  busyAction,
  setBusyAction,
  showNotice,
  onInstruction,
  reload,
}: {
  token: string;
  api: NexoraApi;
  community: SupportRequest[];
  busyAction: string | null;
  setBusyAction: (value: string | null) => void;
  showNotice: (text: string, kind?: "ok" | "error") => void;
  onInstruction: (instruction: PixInstruction) => void;
  reload: () => Promise<void>;
}) {
  const [splitAmount, setSplitAmount] = useState("");

  const autoSplit = async () => {
    const amountCents = Math.round(Number(splitAmount || 0) * 100);
    setBusyAction("auto-split");
    try {
      const result = await api.autoSplit(token, amountCents);
      onInstruction(result.instructions[0]);
      showNotice(result.message);
      await reload();
    } catch (error) {
      showNotice(error instanceof Error ? error.message : "Não foi possível distribuir.", "error");
    } finally {
      setBusyAction(null);
    }
  };

  return (
    <section className="view-stack">
      <Panel title="Distribuir por ordem cronológica" action={
        <div className="inline-form">
          <input value={splitAmount} onChange={(event) => setSplitAmount(event.target.value)} inputMode="decimal" placeholder="Valor R$" />
          <button onClick={autoSplit} disabled={busyAction === "auto-split"}>
            {busyAction === "auto-split" ? <Loader2 className="spin" size={16} /> : <Send size={16} />}
            Distribuir
          </button>
        </div>
      }>
        <p className="muted">A Nexora gera o código Pix para a chave cadastrada por quem recebe, sem mostrar essa chave como campo separado.</p>
      </Panel>
      <div className="request-grid">
        {community.map((item) => (
          <article className="support-card" key={item.id}>
            <div className="support-head">
              <strong>{item.publicCode}</strong>
              <span>{statusLabel(item.status)}</span>
            </div>
            <div className="privacy-line">Solicitação ativa</div>
            <p>Recebedor {item.requesterPublicId} - Nivel {item.requesterLevel}</p>
          </article>
        ))}
      </div>
      <ListEmpty show={community.length === 0} text="Não há solicitações abertas de outras pessoas." />
    </section>
  );
}

function RequestView({
  token,
  api,
  profile,
  showNotice,
  reload,
}: {
  token: string;
  api: NexoraApi;
  profile: Profile;
  showNotice: (text: string, kind?: "ok" | "error") => void;
  reload: () => Promise<void>;
}) {
  const [amount, setAmount] = useState("");
  const [dueDays, setDueDays] = useState("7");
  const [description, setDescription] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    try {
      await api.createSupportRequest(token, {
        amountCents: Math.round(Number(amount || 0) * 100),
        dueDays: Number(dueDays),
        description,
      });
      setAmount("");
      setDescription("");
      showNotice("Solicitação enviada para validação.");
      await reload();
    } catch (error) {
      showNotice(error instanceof Error ? error.message : "Solicitacao falhou.", "error");
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="two-columns">
      <Panel title="Nova solicitacao">
        <form className="form-grid" onSubmit={submit}>
          <label>
            Valor
            <input value={amount} onChange={(event) => setAmount(event.target.value)} inputMode="decimal" placeholder="Ex: 50" required />
          </label>
          <label>
            Prazo em dias
            <input value={dueDays} onChange={(event) => setDueDays(event.target.value)} inputMode="numeric" min={1} max={30} placeholder="Máx: 30 dias" required />
          </label>
          <label>
            Descricao
            <textarea value={description} onChange={(event) => setDescription(event.target.value)} maxLength={500} />
          </label>
          <SubmitButton busy={busy} label="Enviar para validação" />
        </form>
      </Panel>
      <Panel title="Regras da sua conta">
        <div className="metric-list">
          <Metric label="Nivel" value={profile.level} />
          <Metric label="XP" value={profile.xp} />
          <Metric label="Limite atual" value={money(profile.supportLimitCents)} />
          <Metric label="Taxa pendente" value={money(profile.adminFeeDueCents)} />
        </div>
      </Panel>
    </section>
  );
}

function HistoryView({
  token,
  api,
  history,
  showNotice,
  reload,
}: {
  token: string;
  api: NexoraApi;
  history: ContributionHistory[];
  showNotice: (text: string, kind?: "ok" | "error") => void;
  reload: () => Promise<void>;
}) {
  return (
    <section className="view-stack">
      <Panel title="Apoios e comprovantes">
        <ListEmpty show={history.length === 0} text="Nada para revisar ainda." />
        {history.map((item) => (
          <ReceiptCard key={item.id} token={token} api={api} item={item} showNotice={showNotice} reload={reload} />
        ))}
      </Panel>
    </section>
  );
}

function ReceiptCard({
  token,
  api,
  item,
  showNotice,
  reload,
}: {
  token: string;
  api: NexoraApi;
  item: ContributionHistory;
  showNotice: (text: string, kind?: "ok" | "error") => void;
  reload: () => Promise<void>;
}) {
  const [transactionId, setTransactionId] = useState(item.transactionId || "");
  const [file, setFile] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);
  const [ocrBusy, setOcrBusy] = useState(false);
  const [ocrResult, setOcrResult] = useState<OcrResult | null>(null);
  const side = item.direction === "SENT" ? "SENDER" : "RECEIVER";
  const alreadySent = side === "SENDER" ? item.hasSenderReceipt : item.hasReceiverReceipt;

  const submit = async () => {
    if (!file) {
      showNotice("Anexe a foto do comprovante.", "error");
      return;
    }
    setBusy(true);
    try {
      const [receiptHash, receiptImageBase64] = await Promise.all([sha256Hex(file), fileToBase64(file)]);
      await api.submitReceipt(token, item.id, {
        side,
        amountCents: item.amountCents,
        transactionId,
        receiptMimeType: file.type || "image/jpeg",
        receiptImageBase64,
        receiptHash,
      });
      showNotice("Comprovante enviado para revisão.");
      await reload();
    } catch (error) {
      showNotice(error instanceof Error ? error.message : "Comprovante falhou.", "error");
    } finally {
      setBusy(false);
    }
  };

  const analyzeReceiptImage = async (nextFile: File) => {
    setOcrBusy(true);
    setOcrResult(null);
    try {
      const imageBase64 = await fileToBase64(nextFile);
      const cleanBase64 = imageBase64.includes("base64,") ? imageBase64.split("base64,")[1] : imageBase64;
      const mimeType = nextFile.type || "image/jpeg";
      const result = await api.analyzeReceipt(token, cleanBase64, mimeType);
      setOcrResult(result);
      if (result.transactionId) {
        setTransactionId(result.transactionId);
        showNotice("ID da transação preenchido automaticamente.");
      }
      if (!result.transactionId && !result.amountCents) {
        showNotice("Não foi possível identificar dados no comprovante. Envie uma foto mais nítida.", "error");
      }
    } catch (error) {
      showNotice(error instanceof Error ? error.message : "Análise automática falhou. Envie uma foto mais nítida.", "error");
    } finally {
      setOcrBusy(false);
    }
  };

  const handleReceiptFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const nextFile = event.target.files?.[0] || null;
    setFile(nextFile);
    setOcrResult(null);
    if (nextFile) void analyzeReceiptImage(nextFile);
  };

  return (
    <article className="history-card">
      <div>
        <strong>{item.requestPublicCode}</strong>
        <span>{item.direction === "SENT" ? "Você enviou" : "Você recebeu"} - {statusLabel(item.status)}</span>
        <span>{money(item.amountCents)} - {dateTime(item.createdAt)}</span>
      </div>
      <div className="receipt-flags">
        <Badge ok={item.hasSenderReceipt}>Envio</Badge>
        <Badge ok={item.hasReceiverReceipt}>Recebimento</Badge>
        <Badge ok={item.evidenceComplete}>Completo</Badge>
      </div>
      {!alreadySent && item.status === "PENDING_ADMIN" && (
        <div className="receipt-form">
          <input value={transactionId} readOnly placeholder="ID lido automaticamente da foto" aria-label="ID lido automaticamente da foto" />
          <input type="file" accept="image/jpeg,image/png,image/webp" onChange={handleReceiptFileChange} required />
          {ocrBusy && (
            <div className="receipt-auto-analysis">
              <Loader2 className="spin" size={16} />
              <span>Lendo comprovante automaticamente...</span>
            </div>
          )}
          <div className="receipt-ocr-actions">
            <button className="btn-submit" onClick={submit} disabled={busy || ocrBusy}>
              {busy ? <Loader2 className="spin" size={18} /> : <Clipboard size={18} />}
              Enviar comprovante
            </button>
          </div>
          {ocrResult && (
            <div className={`ocr-feedback ocr-feedback-${ocrResult.confidence}`}>
              {ocrResult.amountFormatted && <span>Valor detectado: <strong>R$ {ocrResult.amountFormatted}</strong></span>}
              {ocrResult.date && <span>Data: <strong>{ocrResult.date}</strong></span>}
              {ocrResult.validationErrors?.length ? <span>{ocrResult.validationErrors.join(" ")}</span> : null}
              <span className="ocr-confidence">Confiança: {ocrResult.confidence}</span>
            </div>
          )}
        </div>
      )}
    </article>
  );
}

function ProfileView({ profile, showNotice }: { profile: Profile; showNotice: (text: string, kind?: "ok" | "error") => void }) {
  const inviteUrl = `${window.location.origin}${window.location.pathname}?invite=${encodeURIComponent(profile.inviteCode)}`;
  const shareText = `Entra comigo na Nexora usando o código ${profile.inviteCode}. Acesse ou instale pela página: ${inviteUrl}`;
  const shareWhatsapp = () => {
    window.open(`https://wa.me/?text=${encodeURIComponent(shareText)}`, "_blank", "noopener,noreferrer");
  };
  const shareTelegram = () => {
    window.open(`https://t.me/share/url?url=${encodeURIComponent(inviteUrl)}&text=${encodeURIComponent(shareText)}`, "_blank", "noopener,noreferrer");
  };

  return (
    <section className="two-columns">
      <Panel title="Perfil">
        <div className="profile-block">
          <strong title={profile.name}>{profile.name}</strong>
          <span>{profile.email}</span>
          <span>{profile.publicId} - {statusLabel(profile.status)}</span>
          <CopyField label="Pix aleatório cadastrado" value={profile.pixKeyMasked} showNotice={showNotice} />
        </div>
      </Panel>
      <Panel title="Convite e pontos">
        <p className="muted">Compartilhe seu convite. Pessoas referenciadas contam para sua rede e podem melhorar seus bônus quando evoluírem.</p>
        <CopyField label="Código de convite" value={profile.inviteCode} showNotice={showNotice} />
        <CopyField label="Link de convite" value={inviteUrl} showNotice={showNotice} />
        <div className="share-buttons">
          <button className="whatsapp" onClick={shareWhatsapp}>
            <MessageCircle size={18} />
            WhatsApp
          </button>
          <button className="telegram" onClick={shareTelegram}>
            <Send size={18} />
            Telegram
          </button>
        </div>
        <Metric label="Convidados" value={profile.invitedCount} />
      </Panel>
      <Panel title="Taxa administrativa">
        <Metric label="Taxa acumulada" value={`${money(profile.adminFeeDueCents)} / ${money(profile.adminFeeLimitCents)}`} tone={profile.adminFeeDueCents > 0 ? "warn" : "ok"} />
        {profile.adminPixKey ? (
          <>
            <p className="muted">Envie a taxa acumulada para a conta administrativa e aguarde a baixa pelo admin.</p>
            <CopyField label="Pix aleatório do admin" value={profile.adminPixKey} showNotice={showNotice} />
          </>
        ) : (
          <p className="muted">Sem taxa pendente para envio.</p>
        )}
      </Panel>
    </section>
  );
}

function SettingsView({ apiUrl, onSave }: { apiUrl: string; onSave: (value: string) => void }) {
  const [value, setValue] = useState(apiUrl);
  return (
    <Panel title="Ajustes">
      <div className="form-stack">
        <label>
          URL da API
          <input value={value} onChange={(event) => setValue(event.target.value)} />
        </label>
        <button className="primary settings-save" onClick={() => onSave(value)}>
          <Check size={18} />
          Salvar
        </button>
      </div>
    </Panel>
  );
}

function AdminView({
  token,
  api,
  overview,
  users,
  requests,
  contributions,
  logs,
  busyAction,
  setBusyAction,
  showNotice,
  reload,
}: {
  token: string;
  api: NexoraApi;
  overview: AdminOverview | null;
  users: AdminUser[];
  requests: AdminSupport[];
  contributions: AdminContribution[];
  logs: AuditLog[];
  busyAction: string | null;
  setBusyAction: (value: string | null) => void;
  showNotice: (text: string, kind?: "ok" | "error") => void;
  reload: () => Promise<void>;
}) {
  const [tab, setTab] = useState<AdminTab>("users");
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [receiptFilter, setReceiptFilter] = useState("ALL");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
  const [selectedRequest, setSelectedRequest] = useState<AdminSupport | null>(null);
  const [selectedContribution, setSelectedContribution] = useState<AdminContribution | null>(null);
  const [viewerImage, setViewerImage] = useState<{ imageBase64: string; mimeType: string | null; title: string } | null>(null);

  useEffect(() => {
    setStatusFilter("ALL");
    setReceiptFilter("ALL");
  }, [tab]);

  const inDateRange = useCallback((createdAt: number) => {
    if (fromDate && createdAt < new Date(`${fromDate}T00:00:00`).getTime()) return false;
    if (toDate && createdAt > new Date(`${toDate}T23:59:59`).getTime()) return false;
    return true;
  }, [fromDate, toDate]);

  const includesQuery = useCallback((...values: Array<string | number | null | undefined>) => {
    const clean = query.trim().toLowerCase();
    if (!clean) return true;
    return values.some((value) => String(value ?? "").toLowerCase().includes(clean));
  }, [query]);

  const filteredUsers = useMemo(() => {
    return users.filter((user) =>
      (statusFilter === "ALL" || user.status === statusFilter) &&
      inDateRange(user.createdAt) &&
      includesQuery(user.name, user.email, user.publicId, user.cpf, user.pixKey, user.inviteCode, user.role),
    );
  }, [includesQuery, inDateRange, query, statusFilter, users]);

  const filteredRequests = useMemo(() => {
    return requests.filter((request) =>
      (statusFilter === "ALL" || request.status === statusFilter) &&
      inDateRange(request.createdAt) &&
      includesQuery(request.publicCode, request.requesterName, request.requesterEmail, request.requesterPublicId, request.requesterCpf, request.requesterPixKey),
    );
  }, [includesQuery, inDateRange, requests, statusFilter]);

  const filteredContributions = useMemo(() => {
    return contributions.filter((item) =>
      (statusFilter === "ALL" || item.status === statusFilter) &&
      (receiptFilter === "ALL" ||
        (receiptFilter === "complete" && item.evidenceComplete) ||
        (receiptFilter === "missing" && !item.evidenceComplete)) &&
      inDateRange(item.createdAt) &&
      includesQuery(item.id, item.requestPublicCode, item.transactionId, item.donorPublicId, item.donorName, item.donorEmail, item.receiverPublicId, item.receiverName, item.receiverEmail),
    );
  }, [contributions, includesQuery, inDateRange, receiptFilter, statusFilter]);

  const statusOptions = tab === "users"
    ? ["ALL", "PENDING_REVIEW", "APPROVED", "BLOCKED"]
    : tab === "requests"
      ? ["ALL", "PENDING_ADMIN", "OPEN", "FUNDED", "RETURNED", "REJECTED"]
      : ["ALL", "PENDING_ADMIN", "CONFIRMED", "EXPIRED", "CANCELLED"];

  const adminAction = async (key: string, path: string, confirmText: string, body: unknown = {}) => {
    if (!window.confirm(confirmText)) return;
    setBusyAction(key);
    try {
      const result = await api.adminPost(token, path, body);
      showNotice(result.message || "Ação aplicada.");
      await reload();
    } catch (error) {
      showNotice(error instanceof Error ? error.message : "Ação falhou.", "error");
    } finally {
      setBusyAction(null);
    }
  };

  return (
    <section className="view-stack">
      <div className="metric-grid">
        <Metric label="Usuários" value={overview ? overview.totalUsers : "..."} />
        <Metric label="Pendentes" value={overview ? overview.pendingUsers : "..."} tone="warn" />
        <Metric label="Solicitações abertas" value={overview ? overview.openRequests : "..."} />
        <Metric label="Apoios pendentes" value={overview ? overview.pendingContributions : "..."} />
        <Metric label="Fotos pendentes" value={overview ? overview.pendingReceipts : "..."} tone="warn" />
        <Metric label="Taxa acumulada" value={overview ? money(overview.adminFeeDueCents) : "..."} />
      </div>

      <div className="segmented admin-tabs">
        <button className={cn(tab === "users" && "active")} onClick={() => setTab("users")}>Usuários</button>
        <button className={cn(tab === "requests" && "active")} onClick={() => setTab("requests")}>Solicitações</button>
        <button className={cn(tab === "contributions" && "active")} onClick={() => setTab("contributions")}>Apoios Pix</button>
      </div>

      <FilterStrip
        query={query}
        setQuery={setQuery}
        statusFilter={statusFilter}
        setStatusFilter={setStatusFilter}
        statusOptions={statusOptions}
        receiptFilter={receiptFilter}
        setReceiptFilter={setReceiptFilter}
        fromDate={fromDate}
        setFromDate={setFromDate}
        toDate={toDate}
        setToDate={setToDate}
        showReceipt={tab === "contributions"}
      />

      {tab === "users" && (
        <Panel title="Usuários">
          <div className="table-wrap">
            <table className="admin-users-table">
              <colgroup>
                <col /><col /><col /><col /><col /><col /><col />
              </colgroup>
              <thead>
                <tr>
                  <th>Usuário</th>
                  <th>CPF e Pix</th>
                  <th>Situação</th>
                  <th>Função</th>
                  <th>XP</th>
                  <th>Taxa</th>
                  <th className="th-actions">Ações</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((user) => (
                  <tr key={user.id}>
                    <td><strong title={user.name}>{user.name}</strong><span>{user.email}</span><small>{user.publicId}</small></td>
                    <td><span>{user.cpf}</span><small>{user.pixKey}</small></td>
                    <td>{statusLabel(user.status)}</td>
                    <td>{roleLabel(user.role)}</td>
                    <td>{user.xp} - N{user.level}</td>
                    <td>{money(user.adminFeeDueCents)} / {money(user.adminFeeLimitCents)}</td>
                    <td className="table-actions">
                      <div className="table-actions-inner">
                      <ActionButton busy={false} onClick={() => setSelectedUser(user)} label="Ver detalhes" />
                      {user.status !== "APPROVED" && user.status !== "BLOCKED" && (
                        <ActionButton busy={busyAction === `approve-${user.id}`} onClick={() => adminAction(`approve-${user.id}`, `/admin/users/${user.id}/approve`, `Aprovar ${user.name}?`)} label="Aprovar usuário" />
                      )}
                      {user.status === "APPROVED" && (
                        <ActionButton danger busy={busyAction === `block-${user.id}`} onClick={() => adminAction(`block-${user.id}`, `/admin/users/${user.id}/block`, `Bloquear ${user.name}?`)} label="Bloquear usuário" />
                      )}
                      {user.status === "BLOCKED" && (
                        <ActionButton busy={busyAction === `approve-${user.id}`} onClick={() => adminAction(`approve-${user.id}`, `/admin/users/${user.id}/approve`, `Desbloquear ${user.name}?`)} label="Desbloquear usuário" />
                      )}
                      {user.adminFeeDueCents > 0 && (
                        <ActionButton busy={busyAction === `fee-${user.id}`} onClick={() => adminAction(`fee-${user.id}`, `/admin/users/${user.id}/confirm-admin-fee`, `Baixar taxa de ${user.name}?`)} label="Baixar taxa" />
                      )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <ListEmpty show={filteredUsers.length === 0} text="Nenhum usuário encontrado." />
        </Panel>
      )}

      {tab === "requests" && (
        <Panel title="Solicitações">
          <div className="table-wrap">
            <table className="admin-requests-table">
              <colgroup>
                <col /><col /><col /><col /><col /><col />
              </colgroup>
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Solicitante</th>
                  <th>Valor</th>
                  <th>Taxa</th>
                  <th>Situação</th>
                  <th className="th-actions">Ações</th>
                </tr>
              </thead>
              <tbody>
                {filteredRequests.map((request) => (
                  <tr key={request.id}>
                    <td><strong>{request.publicCode}</strong><span>{dateTime(request.createdAt)}</span></td>
                    <td><span title={request.requesterName}>{request.requesterName}</span><small>{request.requesterPublicId}</small><small>{request.requesterEmail}</small></td>
                    <td>{money(request.fundedCents)} / {money(request.amountCents)}</td>
                    <td>{money(request.adminFeeCents)}</td>
                    <td>{statusLabel(request.status)}</td>
                    <td className="table-actions">
                      <div className="table-actions-inner">
                      <ActionButton busy={false} onClick={() => setSelectedRequest(request)} label="Ver detalhes" />
                      {request.status === "PENDING_ADMIN" && (
                        <ActionButton busy={busyAction === `req-approve-${request.id}`} onClick={() => adminAction(`req-approve-${request.id}`, `/admin/support-requests/${request.id}/approve`, `Aprovar solicitação ${request.publicCode}?`)} label="Aprovar solicitação" />
                      )}
                      {request.status === "PENDING_ADMIN" && (
                        <ActionButton danger busy={busyAction === `req-reject-${request.id}`} onClick={() => adminAction(`req-reject-${request.id}`, `/admin/support-requests/${request.id}/reject`, `Recusar solicitação ${request.publicCode}?`, { reason: "Recusado pelo admin." })} label="Recusar solicitação" />
                      )}
                      {request.status === "FUNDED" && (
                        <ActionButton busy={busyAction === `req-return-${request.id}`} onClick={() => adminAction(`req-return-${request.id}`, `/admin/support-requests/${request.id}/confirm-return`, `Confirmar retorno de ${request.publicCode}?`)} label="Validar retorno" />
                      )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <ListEmpty show={filteredRequests.length === 0} text="Nenhuma solicitação encontrada." />
        </Panel>
      )}

      {tab === "contributions" && (
        <Panel title="Apoios Pix">
          <div className="table-wrap">
            <table className="contributions-table">
              <colgroup>
                <col /><col /><col /><col /><col /><col />
              </colgroup>
              <thead>
                <tr>
                  <th>Apoio</th>
                  <th>Partes</th>
                  <th>ID Pix</th>
                  <th>Valor</th>
                  <th>Comprovantes</th>
                  <th className="th-actions">Ações</th>
                </tr>
              </thead>
              <tbody>
                {filteredContributions.map((item) => (
                  <tr key={item.id}>
                    <td><strong>{item.requestPublicCode}</strong><span>{statusLabel(item.status)}</span><small>{item.verificationStatus || compactId(item.id)}</small></td>
                    <td><span>{item.donorName || item.donorPublicId}</span><small>para {item.receiverName || item.receiverPublicId}</small></td>
                    <td><strong style={{whiteSpace: "normal", wordBreak: "break-all"}}>{item.transactionId || "pendente"}</strong></td>
                    <td><strong style={{whiteSpace: "nowrap"}}>{money(item.amountCents)}</strong></td>
                    <td>
                      <div className="receipt-flags">
                        <Badge ok={item.hasSenderReceipt}>Envio</Badge>
                        <Badge ok={item.hasReceiverReceipt}>Recebimento</Badge>
                        <Badge ok={item.evidenceComplete}>Completo</Badge>
                      </div>
                    </td>
                    <td className="table-actions">
                      <div className="table-actions-inner">
                        <ActionButton busy={false} onClick={() => setSelectedContribution(item)} label="Ver detalhes" />
                        {item.status === "PENDING_ADMIN" && (
                          <ActionButton busy={busyAction === `contrib-${item.id}`} disabled={!item.evidenceComplete} onClick={() => adminAction(`contrib-${item.id}`, `/admin/contributions/${item.id}/confirm`, `Validar Pix de ${item.requestPublicCode}?`)} label="Validar Pix" />
                        )}
                        {item.status === "PENDING_ADMIN" && (
                          <ActionButton danger busy={busyAction === `contrib-reject-${item.id}`} onClick={() => adminAction(`contrib-reject-${item.id}`, `/admin/contributions/${item.id}/reject`, `Recusar apoio ${item.requestPublicCode}?`, { reason: "Recusado pelo admin." })} label="Recusar apoio" />
                        )}
                        {(item.status === "EXPIRED" || item.status === "CANCELLED") && (
                          <ActionButton busy={busyAction === `contrib-activate-${item.id}`} onClick={() => adminAction(`contrib-activate-${item.id}`, `/admin/contributions/${item.id}/activate`, `Reativar apoio ${item.requestPublicCode}?`)} label="Reativar apoio" />
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <ListEmpty show={filteredContributions.length === 0} text="Nenhum apoio encontrado." />
        </Panel>
      )}

      {selectedUser && (
        <DetailModal title={selectedUser.name} subtitle="Dados completos do usuário" onClose={() => setSelectedUser(null)}>
          <DetailGrid items={[
            ["Public ID", selectedUser.publicId],
            ["E-mail", selectedUser.email],
            ["CPF", selectedUser.cpf],
            ["Chave Pix aleatória", selectedUser.pixKey],
            ["Status", statusLabel(selectedUser.status)],
            ["Função", roleLabel(selectedUser.role)],
            ["Nível", selectedUser.level],
            ["XP", selectedUser.xp],
            ["Buff", `${selectedUser.buffBps / 100}%`],
            ["Limite", money(selectedUser.supportLimitCents)],
            ["Taxa acumulada", `${money(selectedUser.adminFeeDueCents)} / ${money(selectedUser.adminFeeLimitCents)}`],
            ["Pix aleatório do admin", selectedUser.adminPixKey || "-"],
            ["Convite", selectedUser.inviteCode],
            ["Convidado por", selectedUser.invitedByPublicId || "-"],
            ["Convidados", selectedUser.invitedCount],
            ["Criado em", dateTime(selectedUser.createdAt)],
          ]} />
        </DetailModal>
      )}

      {selectedRequest && (
        <DetailModal title={selectedRequest.publicCode} subtitle="Detalhes da solicitação" onClose={() => setSelectedRequest(null)}>
          <DetailGrid items={[
            ["Solicitante", selectedRequest.requesterName],
            ["Public ID", selectedRequest.requesterPublicId],
            ["E-mail", selectedRequest.requesterEmail],
            ["CPF", selectedRequest.requesterCpf],
            ["Chave Pix aleatória", selectedRequest.requesterPixKey],
            ["Status", statusLabel(selectedRequest.status)],
            ["Valor", `${money(selectedRequest.fundedCents)} / ${money(selectedRequest.amountCents)}`],
            ["Taxa administrativa", money(selectedRequest.adminFeeCents)],
            ["Prazo", `${selectedRequest.dueDays} dias`],
            ["Criada em", dateTime(selectedRequest.createdAt)],
            ["Descrição", selectedRequest.description || "-"],
          ]} />
          <h3>Apoios vinculados</h3>
          <div className="detail-list">
            {contributions.filter((item) => item.requestPublicCode === selectedRequest.publicCode).map((item) => (
              <button key={item.id} className="detail-row-button" onClick={() => setSelectedContribution(item)}>
                <span>{item.donorPublicId} para {item.receiverPublicId}</span>
                <strong>{money(item.amountCents)} - {statusLabel(item.status)}</strong>
              </button>
            ))}
          </div>
        </DetailModal>
      )}

      {selectedContribution && (
        <DetailModal title={selectedContribution.requestPublicCode} subtitle="Detalhes do apoio Pix" onClose={() => setSelectedContribution(null)}>
          <DetailGrid items={[
            ["Apoio", selectedContribution.id],
            ["Solicitação", `${selectedContribution.requestPublicCode} (${statusLabel(selectedContribution.requestStatus)})`],
            ["Valor", money(selectedContribution.amountCents)],
            ["ID da transação", selectedContribution.transactionId || "-"],
            ["Status", statusLabel(selectedContribution.status)],
            ["Criado em", dateTime(selectedContribution.createdAt)],
            ["Confirmado em", dateTime(selectedContribution.confirmedAt)],
            ["Doador", `${selectedContribution.donorName} - ${selectedContribution.donorPublicId}`],
            ["E-mail do doador", selectedContribution.donorEmail],
            ["Recebedor", `${selectedContribution.receiverName} - ${selectedContribution.receiverPublicId}`],
            ["E-mail do recebedor", selectedContribution.receiverEmail],
          ]} />
          {(selectedContribution.senderOcrTransactionId || selectedContribution.receiverOcrTransactionId) && (
            <div className={`ocr-comparison-box ${selectedContribution.ocrComparisonResult?.toLowerCase() || 'pending'}`}>
              <h4>
                {selectedContribution.ocrComparisonResult === 'MATCH' && '✅ IDs COINCIDEM'}
                {selectedContribution.ocrComparisonResult === 'NO_MATCH' && '❌ IDs NÃO COINCIDEM'}
                {!selectedContribution.ocrComparisonResult && '⏳ Aguardando ambos comprovantes'}
              </h4>
              <div className="ocr-comparison-details">
                <div className="ocr-side">
                  <span className="ocr-side-label">Remetente (enviou):</span>
                  <span className="ocr-transaction-id">{selectedContribution.senderOcrTransactionId || '-'}</span>
                  <span className={`ocr-confidence-tag ${selectedContribution.senderOcrConfidence}`}>
                    {selectedContribution.senderOcrConfidence || '-'} • {selectedContribution.senderOcrProvider || '-'}
                  </span>
                </div>
                <div className="ocr-side">
                  <span className="ocr-side-label">Destinatário (recebeu):</span>
                  <span className="ocr-transaction-id">{selectedContribution.receiverOcrTransactionId || '-'}</span>
                  <span className={`ocr-confidence-tag ${selectedContribution.receiverOcrConfidence}`}>
                    {selectedContribution.receiverOcrConfidence || '-'} • {selectedContribution.receiverOcrProvider || '-'}
                  </span>
                </div>
              </div>
              {selectedContribution.ocrComparisonNotes && (
                <div className="ocr-comparison-notes">
                  <AlertCircle size={14} />
                  <span>{selectedContribution.ocrComparisonNotes}</span>
                </div>
              )}
            </div>
          )}
          <div className="receipt-preview-grid">
            <ReceiptPreview
              title="Foto enviada por quem pagou"
              date={selectedContribution.senderReceiptDate}
              submittedAt={selectedContribution.senderReceiptSubmittedAt}
              hash={selectedContribution.senderReceiptHash}
              imageBase64={selectedContribution.senderReceiptImageBase64}
              mimeType={selectedContribution.senderReceiptMimeType}
              onViewImage={(img, mime) => setViewerImage({ imageBase64: img, mimeType: mime, title: "Comprovante de Envio" })}
              ocrTransactionId={selectedContribution.senderOcrTransactionId}
              ocrConfidence={selectedContribution.senderOcrConfidence}
              ocrProvider={selectedContribution.senderOcrProvider}
            />
            <ReceiptPreview
              title="Foto enviada por quem recebeu"
              date={selectedContribution.receiverReceiptDate}
              submittedAt={selectedContribution.receiverReceiptSubmittedAt}
              hash={selectedContribution.receiverReceiptHash}
              imageBase64={selectedContribution.receiverReceiptImageBase64}
              mimeType={selectedContribution.receiverReceiptMimeType}
              onViewImage={(img, mime) => setViewerImage({ imageBase64: img, mimeType: mime, title: "Comprovante de Recebimento" })}
              ocrTransactionId={selectedContribution.receiverOcrTransactionId}
              ocrConfidence={selectedContribution.receiverOcrConfidence}
              ocrProvider={selectedContribution.receiverOcrProvider}
            />
          </div>
        </DetailModal>
      )}

      {viewerImage && (
        <ImageViewer
          imageBase64={viewerImage.imageBase64}
          mimeType={viewerImage.mimeType}
          title={viewerImage.title}
          onClose={() => setViewerImage(null)}
        />
      )}

      <Panel title="Auditoria">
        <div className="audit-list">
          {logs.map((log) => (
            <div className="audit-row" key={log.id}>
              <span>{dateTime(log.createdAt)}</span>
              <strong>{auditActionLabel(log.action)}</strong>
              <span>{log.actorPublicId || "sistema"} - {log.target}</span>
            </div>
          ))}
        </div>
      </Panel>
    </section>
  );
}

function FilterStrip({
  query,
  setQuery,
  statusFilter,
  setStatusFilter,
  statusOptions,
  receiptFilter,
  setReceiptFilter,
  fromDate,
  setFromDate,
  toDate,
  setToDate,
  showReceipt,
}: {
  query: string;
  setQuery: (value: string) => void;
  statusFilter: string;
  setStatusFilter: (value: string) => void;
  statusOptions: string[];
  receiptFilter: string;
  setReceiptFilter: (value: string) => void;
  fromDate: string;
  setFromDate: (value: string) => void;
  toDate: string;
  setToDate: (value: string) => void;
  showReceipt: boolean;
}) {
  return (
    <div className="filter-strip">
      <label className="search-box">
        <Search size={16} />
        <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Buscar por nome, CPF, Pix, ID ou código" />
      </label>
      <label>
        Situação
        <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
          {statusOptions.map((status) => <option key={status} value={status}>{status === "ALL" ? "Todas" : statusLabel(status)}</option>)}
        </select>
      </label>
      {showReceipt && (
        <label>
          Comprovantes
          <select value={receiptFilter} onChange={(event) => setReceiptFilter(event.target.value)}>
            <option value="ALL">Todos</option>
            <option value="complete">Completos</option>
            <option value="missing">Pendentes</option>
          </select>
        </label>
      )}
      <label>
        De
        <input type="date" value={fromDate} onChange={(event) => setFromDate(event.target.value)} />
      </label>
      <label>
        Até
        <input type="date" value={toDate} onChange={(event) => setToDate(event.target.value)} />
      </label>
    </div>
  );
}

function DetailModal({
  title,
  subtitle,
  children,
  onClose,
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
  onClose: () => void;
}) {
  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <div className="modal detail-modal">
        <div className="modal-head">
          <div>
            <span className="eyebrow">{subtitle}</span>
            <h2>{title}</h2>
          </div>
          <button className="ghost icon-ghost" onClick={onClose} aria-label="Fechar">
            <X size={18} />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

function DetailGrid({ items }: { items: Array<[string, React.ReactNode]> }) {
  return (
    <div className="detail-grid">
      {items.map(([label, value]) => (
        <div key={label}>
          <span>{label}</span>
          <strong>{value}</strong>
        </div>
      ))}
    </div>
  );
}

function ReceiptPreview({
  title,
  date,
  submittedAt,
  hash,
  imageBase64,
  mimeType,
  onViewImage,
  ocrTransactionId,
  ocrConfidence,
  ocrProvider,
}: {
  title: string;
  date: string | null;
  submittedAt: number | null;
  hash: string | null;
  imageBase64: string | null;
  mimeType: string | null;
  onViewImage?: (imageBase64: string, mimeType: string | null) => void;
  ocrTransactionId?: string | null;
  ocrConfidence?: string | null;
  ocrProvider?: string | null;
}) {
  const getSource = () => {
    if (!imageBase64) return "";
    if (imageBase64.startsWith("data:")) return imageBase64;
    return `data:${mimeType || "image/jpeg"};base64,${imageBase64}`;
  };
  const source = getSource();
  return (
    <article className="receipt-preview">
      <strong>{title}</strong>
      {source ? (
        <div className="receipt-image-container" onClick={() => onViewImage?.(imageBase64!, mimeType)}>
          <img src={source} alt={title} />
          <div className="receipt-overlay">
            <Eye size={24} />
            <span>Clique para ampliar</span>
          </div>
        </div>
      ) : (
        <div className="receipt-empty">Sem foto anexada</div>
      )}
      <span>Data: {date || "-"}</span>
      <span>Enviado em: {dateTime(submittedAt)}</span>
      <small>{hash || "Hash pendente"}</small>
      {ocrTransactionId && (
        <div className="ocr-info">
          <span className="ocr-label">OCR detectado:</span>
          <span className="ocr-value">{ocrTransactionId}</span>
          <span className={`ocr-confidence confidence-${ocrConfidence}`}>
            {ocrConfidence} ({ocrProvider})
          </span>
        </div>
      )}
    </article>
  );
}

function ImageViewer({
  imageBase64,
  mimeType,
  title,
  onClose,
}: {
  imageBase64: string;
  mimeType: string | null;
  title: string;
  onClose: () => void;
}) {
  const [zoom, setZoom] = useState(1);
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const [dragging, setDragging] = useState(false);
  const [startPos, setStartPos] = useState({ x: 0, y: 0 });

  const source = `data:${mimeType || "image/jpeg"};base64,${imageBase64}`;

  const handleZoomIn = () => setZoom((z) => Math.min(z + 0.5, 4));
  const handleZoomOut = () => setZoom((z) => Math.max(z - 0.5, 0.5));
  const handleReset = () => {
    setZoom(1);
    setPosition({ x: 0, y: 0 });
  };

  const handleDownload = () => {
    const link = document.createElement("a");
    link.href = source;
    link.download = `comprovante-${title.replace(/\s+/g, "-").toLowerCase()}.${mimeType?.split("/")[1] || "jpg"}`;
    link.click();
  };

  const handleMouseDown = (e: React.MouseEvent) => {
    setDragging(true);
    setStartPos({ x: e.clientX - position.x, y: e.clientY - position.y });
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (dragging) {
      setPosition({ x: e.clientX - startPos.x, y: e.clientY - startPos.y });
    }
  };

  const handleMouseUp = () => setDragging(false);

  return (
    <div className="modal-backdrop image-viewer-backdrop" role="dialog" aria-modal="true">
      <div className="image-viewer-container">
        <div className="image-viewer-toolbar">
          <div className="image-viewer-title">
            <h3>{title}</h3>
          </div>
          <div className="image-viewer-controls">
            <button onClick={handleZoomOut} title="Diminuir zoom" disabled={zoom <= 0.5}>
              <ZoomOut size={20} />
            </button>
            <span className="zoom-level">{Math.round(zoom * 100)}%</span>
            <button onClick={handleZoomIn} title="Aumentar zoom" disabled={zoom >= 4}>
              <ZoomIn size={20} />
            </button>
            <button onClick={handleReset} title="Resetar zoom">
              <RotateCcw size={20} />
            </button>
            <div className="toolbar-divider" />
            <button onClick={handleDownload} title="Baixar imagem" className="download-btn">
              <Download size={20} />
              <span>Baixar</span>
            </button>
            <button onClick={onClose} title="Fechar" className="close-btn">
              <X size={20} />
            </button>
          </div>
        </div>
        <div
          className="image-viewer-content"
          onMouseDown={handleMouseDown}
          onMouseMove={handleMouseMove}
          onMouseUp={handleMouseUp}
          onMouseLeave={handleMouseUp}
        >
          <img
            src={source}
            alt={title}
            style={{
              transform: `scale(${zoom}) translate(${position.x / zoom}px, ${position.y / zoom}px)`,
              cursor: dragging ? "grabbing" : "grab",
            }}
            draggable={false}
          />
        </div>
        <div className="image-viewer-hint">
          <span>Arraste para mover • Use os botões para zoom • Clique em Baixar para salvar</span>
        </div>
      </div>
    </div>
  );
}

function PixModal({
  instruction,
  onClose,
  showNotice,
}: {
  instruction: PixInstruction;
  onClose: () => void;
  showNotice: (text: string, kind?: "ok" | "error") => void;
}) {
  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <div className="modal">
        <div className="modal-head">
          <div>
            <span className="eyebrow">Instrução Pix</span>
            <h2>{instruction.requestPublicCode}</h2>
          </div>
          <button className="ghost modal-close-btn" onClick={onClose} aria-label="Fechar">
            <X size={14} />
          </button>
        </div>
        <Metric label="Valor" value={money(instruction.amountCents)} />
        <CopyField label="Código Pix" value={instruction.pixCopyCode} showNotice={showNotice} multiline />
        <p className="muted">{instruction.message}</p>
        <p className="secure-note">A chave Pix da pessoa que recebe não é exibida como campo separado. Use o código copia-e-cola.</p>
      </div>
    </div>
  );
}

function CopyField({
  label,
  value,
  showNotice,
  multiline = false,
}: {
  label: string;
  value: string;
  showNotice: (text: string, kind?: "ok" | "error") => void;
  multiline?: boolean;
}) {
  const copy = async () => {
    await copyText(value);
    showNotice("Copiado.");
  };
  return (
    <label className="copy-field">
      {label}
      <div>
        {multiline ? (
          <textarea readOnly value={value} onFocus={(event) => event.currentTarget.select()} />
        ) : (
          <input readOnly value={value} onFocus={(event) => event.currentTarget.select()} />
        )}
        <button type="button" onClick={copy} aria-label={`Copiar ${label}`}>
          <Copy size={16} />
        </button>
      </div>
    </label>
  );
}

function Panel({ title, children, action }: { title: string; children: React.ReactNode; action?: React.ReactNode }) {
  return (
    <section className="panel">
      <div className="panel-head">
        <h2>{title}</h2>
        {action}
      </div>
      {children}
    </section>
  );
}

function Metric({ label, value, tone }: { label: string; value: string | number; tone?: "ok" | "warn" }) {
  return (
    <article className={cn("metric", tone === "warn" && "metric-warn", tone === "ok" && "metric-ok")}>
      <span>{label}</span>
      <strong title={String(value)}>{value}</strong>
    </article>
  );
}

function SupportRow({ item }: { item: SupportRequest }) {
  return (
    <div className="row-card">
      <div>
        <strong>{item.publicCode}</strong>
        <span>{statusLabel(item.status)} - {item.dueDays} dias</span>
      </div>
      <b>{money(item.fundedCents)} / {money(item.amountCents)}</b>
    </div>
  );
}

function Badge({ ok, children }: { ok: boolean; children: React.ReactNode }) {
  return <span className={cn("badge", ok && "badge-ok")}>{children}</span>;
}

function ActionButton({
  busy,
  onClick,
  label,
  danger,
  disabled,
  confirm,
}: {
  busy: boolean;
  onClick: () => void;
  label: string;
  danger?: boolean;
  disabled?: boolean;
  confirm?: boolean;
}) {
  const icon = label.includes("detalhes")
    ? <Eye size={16} />
    : label.includes("Baixar") || label.includes("Confirmar")
      ? <Check size={16} />
      : label.includes("Aprovar usuário")
        ? <UserCheck size={16} />
        : label.includes("Aprovar solicitação")
          ? <Send size={16} />
          : label.includes("Validar retorno")
            ? <RotateCcw size={16} />
            : label.includes("Validar Pix")
              ? <ShieldCheck size={16} />
              : label.includes("Bloquear") || label.includes("Recusar")
                ? <AlertCircle size={16} />
                : <Check size={16} />;
  return (
    <button className={cn("small-action", danger && "danger", confirm && !disabled && !busy && "confirm")} onClick={onClick} disabled={busy || disabled} title={label} aria-label={label}>
      {busy ? <Loader2 className="spin" size={14} /> : icon}
    </button>
  );
}

function ListEmpty({ show, text }: { show: boolean; text: string }) {
  if (!show) return null;
  return (
    <div className="empty-state">
      <Clock3 size={20} />
      {text}
    </div>
  );
}

declare global {
  interface Window {
    __nexoraNoticeTimer?: number;
  }
}
