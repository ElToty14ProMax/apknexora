package com.nexora.backend

import java.security.MessageDigest
import java.util.Base64

data class SmtpConfig(
    val host: String,
    val port: Int,
    val username: String?,
    val password: String?,
    val from: String,
)

data class AppConfig(
    val env: String,
    val port: Int,
    val databasePath: String,
    val adminToken: String,
    val adminPixKey: String?,
    val corsAllowedOrigins: Set<String>,
    val superAdminEmail: String,
    val superAdminCpf: String,
    val superAdminPassword: String?,
    val founderEmails: Set<String>,
    val dataKey: ByteArray,
    val cpfPepper: ByteArray,
    val smtp: SmtpConfig,
) {
    val isProduction: Boolean = env.equals("prod", ignoreCase = true)

    companion object {
        fun load(): AppConfig {
            val env = System.getenv("NEXORA_ENV") ?: "dev"
            val isProduction = env.equals("prod", ignoreCase = true)
            val dataKey = readSecretBytes(
                name = "NEXORA_DATA_KEY_B64",
                production = isProduction,
                devSeed = "nexora-local-dev-data-key-change-before-production",
            )
            val cpfPepper = readSecretBytes(
                name = "NEXORA_CPF_PEPPER",
                production = isProduction,
                devSeed = "nexora-local-dev-cpf-pepper-change-before-production",
                isBase64 = false,
            )
            if (dataKey.size !in setOf(16, 24, 32)) {
                error("NEXORA_DATA_KEY_B64 must decode to a valid AES key length: 16, 24, or 32 bytes.")
            }
            val adminToken = System.getenv("NEXORA_ADMIN_TOKEN")?.trim()?.takeIf { it.isNotBlank() }
                ?: if (isProduction) {
                    error("NEXORA_ADMIN_TOKEN must be configured in production.")
                } else {
                    "dev-admin-token-change-me"
                }
            if (isProduction && adminToken.length < 32) {
                error("NEXORA_ADMIN_TOKEN must have at least 32 characters in production.")
            }
            val superAdminEmail = (System.getenv("NEXORA_SUPER_ADMIN_EMAIL") ?: "admin@nexora.local").trim().lowercase()

            return AppConfig(
                env = env,
                port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
                databasePath = System.getenv("NEXORA_DB_PATH") ?: "data/nexora.sqlite",
                adminToken = adminToken,
                adminPixKey = System.getenv("NEXORA_ADMIN_PIX_KEY")?.takeIf { it.isNotBlank() },
                corsAllowedOrigins = readCsv("NEXORA_CORS_ORIGINS")
                    .ifEmpty { if (isProduction) emptySet() else setOf("*") },
                superAdminEmail = superAdminEmail,
                superAdminCpf = (System.getenv("NEXORA_SUPER_ADMIN_CPF") ?: "00000000000").filter(Char::isDigit),
                superAdminPassword = System.getenv("NEXORA_SUPER_ADMIN_PASSWORD")?.takeIf { it.length >= 8 },
                founderEmails = readCsv("NEXORA_FOUNDER_EMAILS").ifEmpty { setOf(superAdminEmail) },
                dataKey = dataKey,
                cpfPepper = cpfPepper,
                smtp = SmtpConfig(
                    host = System.getenv("SMTP_HOST") ?: "smtp.gmail.com",
                    port = System.getenv("SMTP_PORT")?.toIntOrNull() ?: 587,
                    username = System.getenv("SMTP_USERNAME"),
                    password = System.getenv("SMTP_PASSWORD"),
                    from = System.getenv("SMTP_FROM") ?: System.getenv("SMTP_USERNAME") ?: "no-reply@nexora.local",
                ),
            )
        }

        private fun readCsv(name: String): Set<String> =
            (System.getenv(name) ?: "")
                .split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }
                .toSet()

        private fun readSecretBytes(
            name: String,
            production: Boolean,
            devSeed: String,
            isBase64: Boolean = true,
        ): ByteArray {
            val raw = System.getenv(name)
            if (!raw.isNullOrBlank()) {
                return if (isBase64) Base64.getDecoder().decode(raw) else raw.toByteArray(Charsets.UTF_8)
            }
            if (production) {
                error("$name must be configured in production.")
            }
            return MessageDigest.getInstance("SHA-256").digest(devSeed.toByteArray(Charsets.UTF_8))
        }
    }
}
