export type Role = "USER" | "ADMIN" | "SUPER_ADMIN";

export type Profile = {
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
  pendingRepaymentCount: number;
  overdueRepaymentCount: number;
  pixKeyMasked: string;
  adminPixKey: string | null;
};

export type LoginResponse = {
  token: string;
  profile: Profile;
};

export type Dashboard = {
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

export type SupportRequest = {
  id: string;
  publicCode: string;
  requesterPublicId: string;
  requesterLevel: number;
  amountCents: number;
  fundedCents: number;
  dueDays: number;
  dueAt: number | null;
  status: string;
  description: string | null;
  createdAt: number;
  returnedAt: number | null;
  overdue: boolean;
};

export type Repayment = {
  id: string;
  requestId: string;
  requestPublicCode: string;
  direction: "OWED" | "RECEIVABLE";
  counterpartyPublicId: string;
  counterpartyName: string;
  amountCents: number;
  dueAt: number | null;
  returnedAt: number | null;
  status: "PENDING" | "PROOF_SUBMITTED" | "CONFIRMED";
  overdue: boolean;
  daysRemaining: number | null;
  penaltyMessage: string | null;
  pixKeyMasked: string | null;
  pixCopyCode: string | null;
  transactionId: string | null;
  hasReceipt: boolean;
  receiptDate: string | null;
  submittedAt: number | null;
  confirmedAt: number | null;
};

export type RepaymentWorkspace = {
  owed: Repayment[];
  receivable: Repayment[];
  summary: {
    pendingCount: number;
    overdueCount: number;
    pendingAmountCents: number;
    nextDueAt: number | null;
  };
};

export type PixInstruction = {
  contributionId: string;
  requestPublicCode: string;
  receiverIdentifier: string;
  receiverPixKey: string;
  pixCopyCode: string;
  amountCents: number;
  message: string;
};

export type ContributionHistory = {
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

export type AdminOverview = Dashboard & {
  totalUsers: number;
  pendingUsers: number;
  blockedUsers: number;
  pendingRequests: number;
  openRequests: number;
  fundedRequests: number;
  pendingContributions: number;
  pendingReceipts: number;
  adminFeeDueCents: number;
  generatedAt: number;
};

export type AdminUser = {
  id: string;
  publicId: string;
  name: string;
  email: string;
  cpf: string;
  pixKey: string;
  inviteCode: string;
  invitedByPublicId: string | null;
  invitedCount: number;
  status: string;
  role: Role;
  level: number;
  xp: number;
  buffBps: number;
  supportLimitCents: number;
  adminFeeDueCents: number;
  adminFeeLimitCents: number;
  adminPixKey: string | null;
  createdAt: number;
};

export type AdminSupport = SupportRequest & {
  requesterName: string;
  requesterEmail: string;
  requesterCpf: string;
  requesterPixKey: string;
  adminFeeCents: number;
};

export type AdminContribution = {
  id: string;
  requestId: string;
  requestPublicCode: string;
  requestAmountCents: number;
  requestFundedCents: number;
  requestStatus: string;
  donorPublicId: string;
  donorName: string;
  donorEmail: string;
  receiverPublicId: string;
  receiverName: string;
  receiverEmail: string;
  amountCents: number;
  status: string;
  verificationStatus: string | null;
  createdAt: number;
  confirmedAt: number | null;
  transactionId: string | null;
  senderReceiptHash: string | null;
  senderReceiptDate: string | null;
  senderReceiptSubmittedAt: number | null;
  senderReceiptImageBase64: string | null;
  senderReceiptMimeType: string | null;
  receiverReceiptHash: string | null;
  receiverReceiptDate: string | null;
  receiverReceiptSubmittedAt: number | null;
  receiverReceiptImageBase64: string | null;
  receiverReceiptMimeType: string | null;
  hasSenderReceipt: boolean;
  hasReceiverReceipt: boolean;
  evidenceComplete: boolean;
  senderOcrTransactionId: string | null;
  senderOcrAmountCents: number | null;
  senderOcrConfidence: string | null;
  senderOcrProvider: string | null;
  senderOcrRawText: string | null;
  receiverOcrTransactionId: string | null;
  receiverOcrAmountCents: number | null;
  receiverOcrConfidence: string | null;
  receiverOcrProvider: string | null;
  receiverOcrRawText: string | null;
  ocrComparisonResult: string | null;
  ocrComparisonNotes: string | null;
  returnStatus: string | null;
  returnTransactionId: string | null;
  returnReceiptHash: string | null;
  returnReceiptDate: string | null;
  returnSubmittedAt: number | null;
  returnConfirmedAt: number | null;
};

export type OcrComparison = {
  senderTransactionId: string | null;
  receiverTransactionId: string | null;
  result: string | null;
  notes: string | null;
  senderConfidence: string | null;
  receiverConfidence: string | null;
  senderProvider: string | null;
  receiverProvider: string | null;
};

export type AuditLog = {
  id: string;
  actorPublicId: string | null;
  action: string;
  target: string;
  createdAt: number;
};

export type OcrResult = {
  ok: boolean;
  transactionId: string | null;
  amountCents: number | null;
  amountFormatted: string | null;
  date: string | null;
  time: string | null;
  sender: string | null;
  receiver: string | null;
  confidence: "alta" | "media" | "baixa";
  isPixReceipt?: boolean;
  validationErrors?: string[];
  rawText: string;
  provider: string;
};
