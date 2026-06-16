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

export const DEFAULT_API_URL =
  import.meta.env.VITE_API_URL || "https://backend-laravel-two.vercel.app";

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
  }
}

function verificationCodeForApi(code: string): string {
  return code.replace(/\D/g, "").slice(0, 6);
}

type RequestOptions = {
  method?: "GET" | "POST";
  body?: unknown;
  token?: string | null;
};

type AdminFilters = Record<string, string | undefined>;

function queryString(filters?: AdminFilters): string {
  if (!filters) return "";
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value && value !== "ALL") params.set(key, value);
  });
  const value = params.toString();
  return value ? `?${value}` : "";
}

export class NexoraApi {
  constructor(public baseUrl: string) {}

  setBaseUrl(value: string) {
    this.baseUrl = value.replace(/\/$/, "");
  }

  async request<T>(path: string, options: RequestOptions = {}): Promise<T> {
    const headers: Record<string, string> = {
      Accept: "application/json",
    };
    if (options.body !== undefined) headers["Content-Type"] = "application/json";
    if (options.token) headers.Authorization = `Bearer ${options.token}`;

    const response = await fetch(`${this.baseUrl}${path}`, {
      method: options.method || "GET",
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });

    const text = await response.text();
    const data = text ? JSON.parse(text) : {};
    if (!response.ok) {
      throw new ApiError(data.error || data.message || `Erro ${response.status}`, response.status);
    }
    return data as T;
  }

  health() {
    return this.request<{ ok: boolean; message: string }>("/health");
  }

  register(body: {
    name: string;
    email: string;
    cpf: string;
    birthdate: string;
    pixKey: string;
    password: string;
    inviteCode?: string;
  }) {
    return this.request<{ message: string; devVerificationCode: string | null }>("/auth/register", {
      method: "POST",
      body,
    });
  }

  verifyEmail(email: string, code: string) {
    return this.request<LoginResponse & { message?: string }>("/auth/verify-email", {
      method: "POST",
      body: { email: email.trim().toLowerCase(), code: verificationCodeForApi(code) },
    });
  }

  resendVerification(email: string) {
    return this.request<{ ok: boolean; message: string }>("/auth/resend-verification", {
      method: "POST",
      body: { email: email.trim().toLowerCase() },
    });
  }

  recoverPassword(email: string) {
    return this.request<{ ok: boolean; message: string }>("/auth/recover-password", {
      method: "POST",
      body: { email: email.trim().toLowerCase() },
    });
  }

  resetPassword(email: string, code: string, newPassword: string) {
    return this.request<{ ok: boolean; message: string }>("/auth/reset-password", {
      method: "POST",
      body: { email: email.trim().toLowerCase(), code: verificationCodeForApi(code), newPassword },
    });
  }

  login(identifier: string, password: string) {
    return this.request<LoginResponse>("/auth/login", {
      method: "POST",
      body: { identifier, password },
    });
  }

  me(token: string) {
    return this.request<Profile>("/me", { token });
  }

  dashboard(token: string) {
    return this.request<Dashboard>("/dashboard", { token });
  }

  community(token: string) {
    return this.request<SupportRequest[]>("/community", { token });
  }

  myRequests(token: string) {
    return this.request<SupportRequest[]>("/support-requests/mine", { token });
  }

  myContributions(token: string) {
    return this.request<ContributionHistory[]>("/support-requests/contributions/mine", { token });
  }

  createSupportRequest(token: string, body: { amountCents: number; dueDays: number; description: string }) {
    return this.request<SupportRequest>("/support-requests", {
      method: "POST",
      token,
      body,
    });
  }

  createContribution(token: string, requestId: string, amountCents: number) {
    return this.request<PixInstruction>(`/support-requests/${requestId}/contributions`, {
      method: "POST",
      token,
      body: { amountCents },
    });
  }

  autoSplit(token: string, amountCents: number) {
    return this.request<{
      requestedAmountCents: number;
      allocatedAmountCents: number;
      unallocatedAmountCents: number;
      instructions: PixInstruction[];
      message: string;
    }>("/support-requests/contributions/auto-split", {
      method: "POST",
      token,
      body: { amountCents },
    });
  }

  submitReceipt(
    token: string,
    contributionId: string,
    body: {
      side: "SENDER" | "RECEIVER";
      amountCents: number;
      transactionId: string;
      receiptMimeType: string;
      receiptImageBase64: string;
      receiptHash: string;
    },
  ) {
    return this.request<ContributionHistory>(`/support-requests/contributions/${contributionId}/receipt`, {
      method: "POST",
      token,
      body,
    });
  }

  analyzeReceipt(token: string, imageBase64: string, mimeType: string) {
    return this.request<OcrResult>("/receipts/analyze", {
      method: "POST",
      token,
      body: { imageBase64, mimeType },
    });
  }

  adminOverview(token: string) {
    return this.request<AdminOverview>("/admin/overview", { token });
  }

  adminUsers(token: string, filters?: AdminFilters) {
    return this.request<AdminUser[]>(`/admin/users${queryString(filters)}`, { token });
  }

  adminSupportRequests(token: string, filters?: AdminFilters) {
    return this.request<AdminSupport[]>(`/admin/support-requests${queryString(filters)}`, { token });
  }

  adminContributions(token: string, filters?: AdminFilters) {
    return this.request<AdminContribution[]>(`/admin/contributions${queryString(filters)}`, { token });
  }

  auditLogs(token: string) {
    return this.request<AuditLog[]>("/admin/audit-logs?limit=120", { token });
  }

  adminPost(token: string, path: string, body: unknown = {}) {
    return this.request<{ ok: boolean; message: string }>(path, {
      method: "POST",
      token,
      body,
    });
  }
}
