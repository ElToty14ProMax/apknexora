package com.nexora.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ApiError(message: String) : Exception(message)

class ApiClient(
    var baseUrl: String,
    var token: String? = null,
) {
    suspend fun register(
        name: String,
        email: String,
        cpf: String,
        birthdate: String,
        pixKey: String,
        password: String,
        inviteCode: String?,
    ): String = withContext(Dispatchers.IO) {
        val response = requestObject(
            path = "/auth/register",
            method = "POST",
            body = JSONObject()
                .put("name", name)
                .put("email", email)
                .put("cpf", cpf)
                .put("birthdate", birthdate)
                .put("pixKey", pixKey)
                .put("password", password)
                .put("inviteCode", inviteCode ?: ""),
            auth = false,
        )
        val devCode = if (response.isNull("devVerificationCode")) null else response.optString("devVerificationCode")
        val message = if (response.isNull("message")) null else response.optString("message")

        devCode?.takeIf { it.isNotBlank() && it != "null" }
            ?: message?.takeIf { it.isNotBlank() && it != "null" }
            ?: "Cadastro realizado com sucesso."
    }

    suspend fun verifyEmail(email: String, code: String) = withContext(Dispatchers.IO) {
        requestObject(
            path = "/auth/verify-email",
            method = "POST",
            body = JSONObject().put("email", email).put("code", code),
            auth = false,
        )
    }

    suspend fun resendVerification(email: String) = withContext(Dispatchers.IO) {
        requestObject(
            path = "/auth/resend-verification",
            method = "POST",
            body = JSONObject().put("email", email),
            auth = false,
        )
    }

    suspend fun recoverPassword(email: String) = withContext(Dispatchers.IO) {
        requestObject(
            path = "/auth/recover-password",
            method = "POST",
            body = JSONObject().put("email", email),
            auth = false,
        )
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String) = withContext(Dispatchers.IO) {
        requestObject(
            path = "/auth/reset-password",
            method = "POST",
            body = JSONObject()
                .put("email", email)
                .put("code", code)
                .put("newPassword", newPassword),
            auth = false,
        )
    }

    suspend fun login(identifier: String, password: String): Profile = withContext(Dispatchers.IO) {
        val response = requestObject(
            path = "/auth/login",
            method = "POST",
            body = JSONObject().put("identifier", identifier).put("password", password),
            auth = false,
        )
        token = response.getString("token")
        response.getJSONObject("profile").toProfile()
    }

    suspend fun me(): Profile = withContext(Dispatchers.IO) {
        requestObject("/me").toProfile()
    }

    suspend fun dashboard(): Dashboard = withContext(Dispatchers.IO) {
        requestObject("/dashboard").toDashboard()
    }

    suspend fun community(): List<SupportRequest> = withContext(Dispatchers.IO) {
        requestArray("/community").mapObjects { it.toSupportRequest() }
    }

    suspend fun myRequests(): List<SupportRequest> = withContext(Dispatchers.IO) {
        requestArray("/support-requests/mine").mapObjects { it.toSupportRequest() }
    }

    suspend fun contributionHistory(): List<ContributionHistory> = withContext(Dispatchers.IO) {
        requestArray("/support-requests/contributions/mine").mapObjects { it.toContributionHistory() }
    }

    suspend fun createSupportRequest(amountCents: Long, dueDays: Int, description: String?) = withContext(Dispatchers.IO) {
        requestObject(
            path = "/support-requests",
            method = "POST",
            body = JSONObject()
                .put("amountCents", amountCents)
                .put("dueDays", dueDays)
                .put("description", description ?: ""),
        ).toSupportRequest()
    }

    suspend fun createContribution(requestId: String, amountCents: Long): PixInstruction = withContext(Dispatchers.IO) {
        requestObject(
            path = "/support-requests/$requestId/contributions",
            method = "POST",
            body = JSONObject().put("amountCents", amountCents),
        ).toPixInstruction()
    }

    suspend fun createContributionBatch(amountCents: Long): List<PixInstruction> = withContext(Dispatchers.IO) {
        requestObject(
            path = "/support-requests/contributions/auto-split",
            method = "POST",
            body = JSONObject().put("amountCents", amountCents),
        ).getJSONArray("instructions").mapObjects { it.toPixInstruction() }
    }

    suspend fun submitPixReceipt(
        contributionId: String,
        transactionId: String,
        upload: ReceiptUpload,
        amountCents: Long,
        receiptDate: String,
        side: String,
    ) = withContext(Dispatchers.IO) {
        requestObject(
            path = "/support-requests/contributions/$contributionId/receipt",
            method = "POST",
            body = JSONObject()
                .put("transactionId", transactionId)
                .put("receiptHash", upload.hash)
                .put("receiptImageBase64", upload.imageBase64)
                .put("receiptMimeType", upload.mimeType)
                .put("amountCents", amountCents)
                .put("receiptDate", receiptDate)
                .put("side", side),
        )
    }

    suspend fun analyzeReceipt(imageBase64: String, mimeType: String): OcrResult = withContext(Dispatchers.IO) {
        val response = requestObject(
            path = "/receipts/analyze",
            method = "POST",
            body = JSONObject()
                .put("imageBase64", imageBase64)
                .put("mimeType", mimeType),
        )
        OcrResult(
            ok = response.optBoolean("ok", false),
            transactionId = response.optString("transactionId").takeIf { it.isNotBlank() },
            amountCents = if (response.has("amountCents")) response.getLong("amountCents") else null,
            amountFormatted = response.optString("amountFormatted").takeIf { it.isNotBlank() },
            date = response.optString("date").takeIf { it.isNotBlank() },
            time = response.optString("time").takeIf { it.isNotBlank() },
            sender = response.optString("sender").takeIf { it.isNotBlank() },
            receiver = response.optString("receiver").takeIf { it.isNotBlank() },
            confidence = response.optString("confidence").takeIf { it.isNotBlank() },
            rawText = response.optString("rawText"),
            provider = response.optString("provider"),
        )
    }

    suspend fun submitReceiptWithOcr(
        contributionId: String,
        upload: ReceiptUpload,
        amountCents: Long,
        receiptDate: String,
        side: String,
    ): SubmitReceiptResult = withContext(Dispatchers.IO) {
        val response = requestObject(
            path = "/support-requests/contributions/$contributionId/receipt",
            method = "POST",
            body = JSONObject()
                .put("receiptHash", upload.hash)
                .put("receiptImageBase64", upload.imageBase64)
                .put("receiptMimeType", upload.mimeType)
                .put("amountCents", amountCents)
                .put("receiptDate", receiptDate)
                .put("side", side),
        )
        SubmitReceiptResult(
            contributionId = response.optString("contributionId"),
            transactionId = response.optString("transactionId").takeIf { it.isNotBlank() },
            status = response.optString("status"),
            hasSenderReceipt = response.optBoolean("hasSenderReceipt"),
            hasReceiverReceipt = response.optBoolean("hasReceiverReceipt"),
            evidenceComplete = response.optBoolean("evidenceComplete"),
            ocrResult = if (response.has("ocrResult") && !response.isNull("ocrResult")) {
                val ocr = response.getJSONObject("ocrResult")
                OcrResult(
                    ok = ocr.optBoolean("ok", false),
                    transactionId = ocr.optString("transactionId").takeIf { it.isNotBlank() },
                    amountCents = if (ocr.has("amountCents")) ocr.getLong("amountCents") else null,
                    amountFormatted = ocr.optString("amountFormatted").takeIf { it.isNotBlank() },
                    date = ocr.optString("date").takeIf { it.isNotBlank() },
                    time = ocr.optString("time").takeIf { it.isNotBlank() },
                    sender = ocr.optString("sender").takeIf { it.isNotBlank() },
                    receiver = ocr.optString("receiver").takeIf { it.isNotBlank() },
                    confidence = ocr.optString("confidence").takeIf { it.isNotBlank() },
                    rawText = ocr.optString("rawText"),
                    provider = ocr.optString("provider"),
                )
            } else null,
            ocrComparison = if (response.has("ocrComparison") && !response.isNull("ocrComparison")) {
                val comp = response.getJSONObject("ocrComparison")
                OcrComparison(
                    senderTransactionId = comp.optString("senderTransactionId").takeIf { it.isNotBlank() },
                    receiverTransactionId = comp.optString("receiverTransactionId").takeIf { it.isNotBlank() },
                    result = comp.optString("result").takeIf { it.isNotBlank() },
                    notes = comp.optString("notes").takeIf { it.isNotBlank() },
                    senderConfidence = comp.optString("senderConfidence").takeIf { it.isNotBlank() },
                    receiverConfidence = comp.optString("receiverConfidence").takeIf { it.isNotBlank() },
                    senderProvider = comp.optString("senderProvider").takeIf { it.isNotBlank() },
                    receiverProvider = comp.optString("receiverProvider").takeIf { it.isNotBlank() },
                )
            } else null,
        )
    }

    suspend fun adminUsers(): List<AdminUser> = withContext(Dispatchers.IO) {
        requestArray("/admin/users").mapObjects { it.toAdminUser() }
    }

    suspend fun adminSupportRequests(): List<AdminSupportRequest> = withContext(Dispatchers.IO) {
        requestArray("/admin/support-requests").mapObjects { it.toAdminSupportRequest() }
    }

    suspend fun adminContributions(): List<AdminContribution> = withContext(Dispatchers.IO) {
        requestArray("/admin/contributions").mapObjects { it.toAdminContribution() }
    }

    suspend fun adminPost(path: String) = withContext(Dispatchers.IO) {
        requestObject(path = path, method = "POST", body = JSONObject())
    }

    private fun requestObject(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        auth: Boolean = true,
    ): JSONObject = JSONObject(requestRaw(path, method, body, auth).ifBlank { "{}" })

    private fun requestArray(path: String): JSONArray = JSONArray(requestRaw(path, "GET", null, true))

    private fun requestRaw(path: String, method: String, body: JSONObject?, auth: Boolean): String {
        val cleanBase = baseUrl.trim().trimEnd('/')
        val connection = (URL(cleanBase + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            if (auth) {
                val currentToken = token ?: throw ApiError("Sessão expirada.")
                setRequestProperty("Authorization", "Bearer $currentToken")
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
        }

        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        }.orEmpty()
        connection.disconnect()
        if (status !in 200..299) {
            val message = runCatching {
                val json = JSONObject(text)
                if (json.isNull("error")) null else json.optString("error")
            }.getOrNull()
            throw ApiError(message?.takeIf { it.isNotBlank() && it != "null" } ?: "Erro $status")
        }
        return text
    }
}

private inline fun <T> JSONArray.mapObjects(block: (JSONObject) -> T): List<T> =
    List(length()) { index -> block(getJSONObject(index)) }
