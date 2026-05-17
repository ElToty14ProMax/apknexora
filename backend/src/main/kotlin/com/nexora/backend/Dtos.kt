package com.nexora.backend

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class RegisterRequest(
    val name: String? = null,
    val email: String,
    val cpf: String,
    val pixKey: String,
    val password: String,
    val inviteCode: String? = null,
)

@Serializable
data class RegisterResponse(
    val message: String,
    val devVerificationCode: String? = null,
)

@Serializable
data class VerifyEmailRequest(val email: String, val code: String)

@Serializable
data class ResendVerificationRequest(val email: String)

@Serializable
data class RecoverPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(val email: String, val code: String, val newPassword: String)

@Serializable
data class LoginRequest(val identifier: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val profile: ProfileResponse)

@Serializable
data class ProfileResponse(
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
    val adminPixKey: String? = null,
)

@Serializable
data class DashboardResponse(
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

@Serializable
data class AdminOverviewResponse(
    val communityLiquidityCents: Long,
    val inCirculationCents: Long,
    val completionPercent: Double,
    val activeRequests: Int,
    val completedOperations: Int,
    val activeUsers: Int,
    val totalUsers: Int,
    val pendingUsers: Int,
    val blockedUsers: Int,
    val pendingRequests: Int,
    val openRequests: Int,
    val fundedRequests: Int,
    val pendingContributions: Int,
    val pendingReceipts: Int,
    val adminFeeDueCents: Long,
    val roadmapStep: Int,
    val roadmapCapacity: Int,
    val generatedAt: Long,
)

@Serializable
data class CreateSupportRequest(
    val amountCents: Long,
    val dueDays: Int,
    val description: String? = null,
)

@Serializable
data class SupportRequestResponse(
    val id: String,
    val publicCode: String,
    val requesterPublicId: String,
    val requesterLevel: Int,
    val amountCents: Long,
    val fundedCents: Long,
    val dueDays: Int,
    val status: String,
    val description: String? = null,
    val createdAt: Long,
)

@Serializable
data class CreateContributionRequest(val amountCents: Long)

@Serializable
data class CreateContributionBatchRequest(val amountCents: Long)

@Serializable
data class ContributionInstructionResponse(
    val contributionId: String,
    val requestPublicCode: String,
    val receiverIdentifier: String,
    val receiverPixKey: String = "",
    val pixCopyCode: String,
    val amountCents: Long,
    val message: String,
)

@Serializable
data class ContributionBatchResponse(
    val requestedAmountCents: Long,
    val allocatedAmountCents: Long,
    val unallocatedAmountCents: Long,
    val instructions: List<ContributionInstructionResponse>,
    val message: String,
)

@Serializable
data class AdminUserResponse(
    val id: String,
    val publicId: String,
    val name: String,
    val email: String,
    val status: String,
    val role: String,
    val level: Int,
    val xp: Long,
    val buffBps: Int,
    val supportLimitCents: Long,
    val adminFeeDueCents: Long,
    val adminFeeLimitCents: Long,
    val createdAt: Long,
)

@Serializable
data class AdminSupportRequestResponse(
    val id: String,
    val publicCode: String,
    val requesterPublicId: String,
    val requesterName: String,
    val amountCents: Long,
    val fundedCents: Long,
    val dueDays: Int,
    val status: String,
    val adminFeeCents: Long,
    val description: String? = null,
    val createdAt: Long,
)

@Serializable
data class AdminContributionResponse(
    val id: String,
    val requestPublicCode: String,
    val donorPublicId: String,
    val receiverPublicId: String,
    val amountCents: Long,
    val status: String,
    val createdAt: Long,
    val confirmedAt: Long? = null,
    val transactionId: String? = null,
    val senderReceiptHash: String? = null,
    val senderReceiptDate: String? = null,
    val senderReceiptImageBase64: String? = null,
    val senderReceiptMimeType: String? = null,
    val receiverReceiptHash: String? = null,
    val receiverReceiptDate: String? = null,
    val receiverReceiptImageBase64: String? = null,
    val receiverReceiptMimeType: String? = null,
    val hasSenderReceipt: Boolean,
    val hasReceiverReceipt: Boolean,
    val evidenceComplete: Boolean,
)

@Serializable
data class ContributionHistoryResponse(
    val id: String,
    val transactionId: String? = null,
    val requestPublicCode: String,
    val donorPublicId: String,
    val receiverPublicId: String,
    val direction: String,
    val amountCents: Long,
    val status: String,
    val createdAt: Long,
    val confirmedAt: Long? = null,
    val senderReceiptDate: String? = null,
    val receiverReceiptDate: String? = null,
    val hasSenderReceipt: Boolean,
    val hasReceiverReceipt: Boolean,
    val evidenceComplete: Boolean,
)

@Serializable
data class RejectRequest(val reason: String? = null)

@Serializable
data class ConfirmReturnRequest(val returnedAt: Long? = null)

@Serializable
data class OkResponse(val ok: Boolean = true, val message: String = "OK")

@Serializable
data class UpdateRoleRequest(val role: String)

@Serializable
data class UpdateReputationRequest(
    val xp: Long? = null,
    val level: Int? = null,
    val buffBps: Int? = null,
    val adminFeeDueCents: Long? = null,
)

@Serializable
data class AuditLogResponse(
    val id: String,
    val actorPublicId: String?,
    val action: String,
    val target: String,
    val createdAt: Long,
)

@Serializable
data class SubmitPixReceiptRequest(
    val transactionId: String,
    val receiptHash: String,
    val receiptImageBase64: String,
    val receiptMimeType: String = "image/jpeg",
    val amountCents: Long,
    val receiptDate: String,
    val side: String = "SENDER",
)

@Serializable
data class PixReceiptResponse(
    val contributionId: String,
    val transactionId: String,
    val side: String,
    val receiptHash: String,
    val receiptImageBase64: String,
    val receiptMimeType: String,
    val amountCents: Long,
    val receiptDate: String,
    val submittedAt: Long,
    val status: String,
    val hasSenderReceipt: Boolean,
    val hasReceiverReceipt: Boolean,
    val evidenceComplete: Boolean,
)
