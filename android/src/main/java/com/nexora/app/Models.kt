package com.nexora.app

import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

data class Profile(
    val id: String,
    val publicId: String,
    val name: String,
    val email: String,
    val status: String,
    val role: String,
    val level: Int,
    val xp: Long,
    val xpIntoLevel: Long,
    val xpRequiredThisLevel: Long,
    val buffBps: Int,
    val supportLimitCents: Long,
    val inviteCode: String,
    val invitedCount: Int,
    val adminFeeDueCents: Long,
    val adminFeeLimitCents: Long,
    val pixKeyMasked: String,
    val adminPixKey: String?,
)

data class Dashboard(
    val communityLiquidityCents: Long,
    val inCirculationCents: Long,
    val completionPercent: Double,
    val activeRequests: Int,
    val completedOperations: Int,
    val activeUsers: Int,
    val userLimitCents: Long,
    val roadmapStep: Int,
    val roadmapCapacity: Int,
)

data class SupportRequest(
    val id: String,
    val publicCode: String,
    val requesterPublicId: String,
    val requesterLevel: Int,
    val amountCents: Long,
    val fundedCents: Long,
    val dueDays: Int,
    val status: String,
    val description: String?,
    val createdAt: Long,
)

data class AdminUser(
    val id: String,
    val publicId: String,
    val name: String,
    val email: String,
    val cpf: String,
    val pixKey: String,
    val inviteCode: String,
    val invitedByPublicId: String?,
    val invitedCount: Int,
    val status: String,
    val role: String,
    val level: Int,
    val xp: Long,
    val buffBps: Int,
    val supportLimitCents: Long,
    val adminFeeDueCents: Long,
    val adminFeeLimitCents: Long,
    val adminPixKey: String?,
    val createdAt: Long,
)

data class AdminSupportRequest(
    val id: String,
    val publicCode: String,
    val requesterPublicId: String,
    val requesterName: String,
    val requesterEmail: String,
    val requesterCpf: String,
    val requesterPixKey: String,
    val amountCents: Long,
    val fundedCents: Long,
    val dueDays: Int,
    val status: String,
    val adminFeeCents: Long,
    val description: String?,
    val createdAt: Long,
)

data class AdminContribution(
    val id: String,
    val requestId: String,
    val requestPublicCode: String,
    val requestAmountCents: Long,
    val requestFundedCents: Long,
    val requestStatus: String,
    val donorPublicId: String,
    val donorName: String,
    val donorEmail: String,
    val receiverPublicId: String,
    val receiverName: String,
    val receiverEmail: String,
    val amountCents: Long,
    val status: String,
    val transactionId: String?,
    val senderReceiptHash: String?,
    val senderReceiptDate: String?,
    val senderReceiptSubmittedAt: Long?,
    val senderReceiptImageBase64: String?,
    val senderReceiptMimeType: String?,
    val receiverReceiptHash: String?,
    val receiverReceiptDate: String?,
    val receiverReceiptSubmittedAt: Long?,
    val receiverReceiptImageBase64: String?,
    val receiverReceiptMimeType: String?,
    val hasSenderReceipt: Boolean,
    val hasReceiverReceipt: Boolean,
    val evidenceComplete: Boolean,
    val createdAt: Long,
    val senderOcrTransactionId: String? = null,
    val senderOcrAmountCents: Long? = null,
    val senderOcrConfidence: String? = null,
    val senderOcrProvider: String? = null,
    val senderOcrRawText: String? = null,
    val receiverOcrTransactionId: String? = null,
    val receiverOcrAmountCents: Long? = null,
    val receiverOcrConfidence: String? = null,
    val receiverOcrProvider: String? = null,
    val receiverOcrRawText: String? = null,
    val ocrComparisonResult: String? = null,
    val ocrComparisonNotes: String? = null,
)

data class PixInstruction(
    val contributionId: String,
    val requestPublicCode: String,
    val receiverIdentifier: String,
    val receiverPixKey: String,
    val pixCopyCode: String,
    val amountCents: Long,
    val message: String,
)

data class ContributionHistory(
    val id: String,
    val transactionId: String?,
    val requestPublicCode: String,
    val donorPublicId: String,
    val receiverPublicId: String,
    val direction: String,
    val amountCents: Long,
    val status: String,
    val hasSenderReceipt: Boolean,
    val hasReceiverReceipt: Boolean,
    val evidenceComplete: Boolean,
    val createdAt: Long,
)

data class OcrResult(
    val ok: Boolean,
    val transactionId: String?,
    val amountCents: Long?,
    val amountFormatted: String?,
    val date: String?,
    val time: String?,
    val sender: String?,
    val receiver: String?,
    val confidence: String?,
    val rawText: String,
    val provider: String,
)

data class OcrComparison(
    val senderTransactionId: String?,
    val receiverTransactionId: String?,
    val result: String?,
    val notes: String?,
    val senderConfidence: String?,
    val receiverConfidence: String?,
    val senderProvider: String?,
    val receiverProvider: String?,
)

data class SubmitReceiptResult(
    val contributionId: String,
    val transactionId: String?,
    val status: String,
    val hasSenderReceipt: Boolean,
    val hasReceiverReceipt: Boolean,
    val evidenceComplete: Boolean,
    val ocrResult: OcrResult?,
    val ocrComparison: OcrComparison?,
)

data class ReceiptUpload(
    val hash: String,
    val imageBase64: String,
    val mimeType: String,
)

fun JSONObject.toProfile(): Profile = Profile(
    id = getString("id"),
    publicId = getString("publicId"),
    name = getString("name"),
    email = getString("email"),
    status = getString("status"),
    role = getString("role"),
    level = getInt("level"),
    xp = getLong("xp"),
    xpIntoLevel = getLong("xpIntoLevel"),
    xpRequiredThisLevel = getLong("xpRequiredThisLevel"),
    buffBps = getInt("buffBps"),
    supportLimitCents = getLong("supportLimitCents"),
    inviteCode = getString("inviteCode"),
    invitedCount = getInt("invitedCount"),
    adminFeeDueCents = getLong("adminFeeDueCents"),
    adminFeeLimitCents = getLong("adminFeeLimitCents"),
    pixKeyMasked = getString("pixKeyMasked"),
    adminPixKey = optString("adminPixKey").takeIf { it.isNotBlank() && it != "null" },
)

fun JSONObject.toDashboard(): Dashboard = Dashboard(
    communityLiquidityCents = getLong("communityLiquidityCents"),
    inCirculationCents = getLong("inCirculationCents"),
    completionPercent = getDouble("completionPercent"),
    activeRequests = getInt("activeRequests"),
    completedOperations = getInt("completedOperations"),
    activeUsers = getInt("activeUsers"),
    userLimitCents = getLong("userLimitCents"),
    roadmapStep = getInt("roadmapStep"),
    roadmapCapacity = getInt("roadmapCapacity"),
)

fun JSONObject.toSupportRequest(): SupportRequest = SupportRequest(
    id = getString("id"),
    publicCode = getString("publicCode"),
    requesterPublicId = getString("requesterPublicId"),
    requesterLevel = getInt("requesterLevel"),
    amountCents = getLong("amountCents"),
    fundedCents = getLong("fundedCents"),
    dueDays = getInt("dueDays"),
    status = getString("status"),
    description = optString("description").takeIf { it.isNotBlank() && it != "null" },
    createdAt = optLong("createdAt", 0L),
)

fun JSONObject.toAdminUser(): AdminUser = AdminUser(
    id = getString("id"),
    publicId = getString("publicId"),
    name = getString("name"),
    email = getString("email"),
    cpf = optString("cpf"),
    pixKey = optString("pixKey"),
    inviteCode = optString("inviteCode"),
    invitedByPublicId = optString("invitedByPublicId").takeIf { it.isNotBlank() && it != "null" },
    invitedCount = optInt("invitedCount"),
    status = getString("status"),
    role = getString("role"),
    level = getInt("level"),
    xp = getLong("xp"),
    buffBps = getInt("buffBps"),
    supportLimitCents = optLong("supportLimitCents", 0L),
    adminFeeDueCents = getLong("adminFeeDueCents"),
    adminFeeLimitCents = optLong("adminFeeLimitCents", 0L),
    adminPixKey = optString("adminPixKey").takeIf { it.isNotBlank() && it != "null" },
    createdAt = optLong("createdAt", 0L),
)

fun JSONObject.toAdminSupportRequest(): AdminSupportRequest = AdminSupportRequest(
    id = getString("id"),
    publicCode = getString("publicCode"),
    requesterPublicId = getString("requesterPublicId"),
    requesterName = getString("requesterName"),
    requesterEmail = optString("requesterEmail"),
    requesterCpf = optString("requesterCpf"),
    requesterPixKey = optString("requesterPixKey"),
    amountCents = getLong("amountCents"),
    fundedCents = getLong("fundedCents"),
    dueDays = getInt("dueDays"),
    status = getString("status"),
    adminFeeCents = getLong("adminFeeCents"),
    description = optString("description").takeIf { it.isNotBlank() && it != "null" },
    createdAt = optLong("createdAt", 0L),
)

fun JSONObject.toAdminContribution(): AdminContribution = AdminContribution(
    id = getString("id"),
    requestId = optString("requestId"),
    requestPublicCode = getString("requestPublicCode"),
    requestAmountCents = optLong("requestAmountCents", 0L),
    requestFundedCents = optLong("requestFundedCents", 0L),
    requestStatus = optString("requestStatus"),
    donorPublicId = getString("donorPublicId"),
    donorName = optString("donorName"),
    donorEmail = optString("donorEmail"),
    receiverPublicId = getString("receiverPublicId"),
    receiverName = optString("receiverName"),
    receiverEmail = optString("receiverEmail"),
    amountCents = getLong("amountCents"),
    status = getString("status"),
    transactionId = optString("transactionId").takeIf { it.isNotBlank() && it != "null" },
    senderReceiptHash = optString("senderReceiptHash").takeIf { it.isNotBlank() && it != "null" },
    senderReceiptDate = optString("senderReceiptDate").takeIf { it.isNotBlank() && it != "null" },
    senderReceiptSubmittedAt = optLong("senderReceiptSubmittedAt").takeIf { it > 0L },
    senderReceiptImageBase64 = optString("senderReceiptImageBase64").takeIf { it.isNotBlank() && it != "null" },
    senderReceiptMimeType = optString("senderReceiptMimeType").takeIf { it.isNotBlank() && it != "null" },
    receiverReceiptHash = optString("receiverReceiptHash").takeIf { it.isNotBlank() && it != "null" },
    receiverReceiptDate = optString("receiverReceiptDate").takeIf { it.isNotBlank() && it != "null" },
    receiverReceiptSubmittedAt = optLong("receiverReceiptSubmittedAt").takeIf { it > 0L },
    receiverReceiptImageBase64 = optString("receiverReceiptImageBase64").takeIf { it.isNotBlank() && it != "null" },
    receiverReceiptMimeType = optString("receiverReceiptMimeType").takeIf { it.isNotBlank() && it != "null" },
    hasSenderReceipt = optBoolean("hasSenderReceipt"),
    hasReceiverReceipt = optBoolean("hasReceiverReceipt"),
    evidenceComplete = optBoolean("evidenceComplete"),
    createdAt = optLong("createdAt", 0L),
    senderOcrTransactionId = optString("senderOcrTransactionId").takeIf { it.isNotBlank() && it != "null" },
    senderOcrAmountCents = if (has("senderOcrAmountCents")) optLong("senderOcrAmountCents") else null,
    senderOcrConfidence = optString("senderOcrConfidence").takeIf { it.isNotBlank() && it != "null" },
    senderOcrProvider = optString("senderOcrProvider").takeIf { it.isNotBlank() && it != "null" },
    senderOcrRawText = optString("senderOcrRawText").takeIf { it.isNotBlank() && it != "null" },
    receiverOcrTransactionId = optString("receiverOcrTransactionId").takeIf { it.isNotBlank() && it != "null" },
    receiverOcrAmountCents = if (has("receiverOcrAmountCents")) optLong("receiverOcrAmountCents") else null,
    receiverOcrConfidence = optString("receiverOcrConfidence").takeIf { it.isNotBlank() && it != "null" },
    receiverOcrProvider = optString("receiverOcrProvider").takeIf { it.isNotBlank() && it != "null" },
    receiverOcrRawText = optString("receiverOcrRawText").takeIf { it.isNotBlank() && it != "null" },
    ocrComparisonResult = optString("ocrComparisonResult").takeIf { it.isNotBlank() && it != "null" },
    ocrComparisonNotes = optString("ocrComparisonNotes").takeIf { it.isNotBlank() && it != "null" },
)

fun JSONObject.toPixInstruction(): PixInstruction = PixInstruction(
    contributionId = getString("contributionId"),
    requestPublicCode = getString("requestPublicCode"),
    receiverIdentifier = getString("receiverIdentifier"),
    receiverPixKey = optString("receiverPixKey"),
    pixCopyCode = optString("pixCopyCode"),
    amountCents = getLong("amountCents"),
    message = getString("message"),
)

fun JSONObject.toContributionHistory(): ContributionHistory = ContributionHistory(
    id = getString("id"),
    transactionId = optString("transactionId").takeIf { it.isNotBlank() && it != "null" },
    requestPublicCode = getString("requestPublicCode"),
    donorPublicId = getString("donorPublicId"),
    receiverPublicId = getString("receiverPublicId"),
    direction = getString("direction"),
    amountCents = getLong("amountCents"),
    status = getString("status"),
    hasSenderReceipt = optBoolean("hasSenderReceipt"),
    hasReceiverReceipt = optBoolean("hasReceiverReceipt"),
    evidenceComplete = optBoolean("evidenceComplete"),
    createdAt = optLong("createdAt", 0L),
)

fun formatMoney(cents: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
    return format.format(cents / 100.0)
}

fun parseMoneyToCents(input: String): Long? {
    val normalized = input
        .replace("R$", "")
        .replace(".", "")
        .replace(",", ".")
        .trim()
    return normalized.toDoubleOrNull()?.let { (it * 100).toLong() }
}

fun formatBuff(bps: Int): String = String.format(Locale.forLanguageTag("pt-BR"), "+%.1f%%", bps / 100.0)
