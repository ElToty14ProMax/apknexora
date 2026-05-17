package com.nexora.backend

import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor

object CpfValidator {
    fun digits(value: String): String = value.filter(Char::isDigit)

    fun isValid(value: String): Boolean {
        val cpf = digits(value)
        if (cpf.length != 11 || cpf.toSet().size == 1) return false
        val first = checkDigit(cpf, 9)
        val second = checkDigit(cpf, 10)
        return cpf[9].digitToInt() == first && cpf[10].digitToInt() == second
    }

    fun format(value: String): String {
        val cpf = digits(value)
        return if (cpf.length == 11) {
            "${cpf.substring(0, 3)}.${cpf.substring(3, 6)}.${cpf.substring(6, 9)}-${cpf.substring(9)}"
        } else {
            value
        }
    }

    private fun checkDigit(cpf: String, length: Int): Int {
        var sum = 0
        for (index in 0 until length) {
            sum += cpf[index].digitToInt() * (length + 1 - index)
        }
        val result = 11 - (sum % 11)
        return if (result >= 10) 0 else result
    }
}

class SecurityService(private val config: AppConfig) {
    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun isValidEmail(email: String): Boolean =
        email.length in 6..254 && Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE).matches(email)

    fun isValidPixKey(value: String): Boolean {
        val clean = value.trim()
        val digits = clean.filter(Char::isDigit)
        val isRandom = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
            .matches(clean)
        val isCpf = CpfValidator.isValid(clean)
        val isPhone = clean.startsWith("+55") && digits.length in 12..13
        val isEmail = isValidEmail(clean)
        return isRandom || isCpf || isPhone || isEmail
    }

    fun isValidSha256(value: String): Boolean =
        Regex("^[0-9a-fA-F]{64}$").matches(value.trim())

    fun hashCpf(cpf: String): String = hmac("cpf:${CpfValidator.digits(cpf)}")

    fun hashToken(token: String): String = sha256(token)

    fun hashVerificationCode(email: String, code: String): String =
        hmac("verify:${normalizeEmail(email)}:$code")

    fun hashRecoveryCode(email: String, code: String): String =
        hmac("recover:${normalizeEmail(email)}:$code")

    fun newVerificationCode(): String = random.nextInt(1_000_000).toString().padStart(6, '0')

    fun newToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }

    fun publicId(): String = "NX-" + randomCode(8)

    fun inviteCode(): String = randomCode(8)

    fun supportCode(): String = "AP-" + randomCode(7)

    fun paymentReference(contributionId: String): String =
        ("NX" + hmac("payment:$contributionId").filter { it.isLetterOrDigit() }.take(23))
            .uppercase()

    fun hashPassword(password: String): String {
        val salt = ByteArray(16)
        random.nextBytes(salt)
        val iterations = 210_000
        val hash = pbkdf2(password, salt, iterations)
        return listOf("pbkdf2_sha256", iterations.toString(), encoder.encodeToString(salt), encoder.encodeToString(hash))
            .joinToString("$")
    }

    fun verifyPassword(password: String, stored: String): Boolean {
        val parts = stored.split("$")
        if (parts.size != 4 || parts[0] != "pbkdf2_sha256") return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = decoder.decode(parts[2])
        val expected = decoder.decode(parts[3])
        val actual = pbkdf2(password, salt, iterations)
        return constantTimeEquals(expected, actual)
    }

    fun encrypt(value: String): String {
        val iv = ByteArray(12)
        random.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(config.dataKey, "AES"), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return "${encoder.encodeToString(iv)}.${encoder.encodeToString(encrypted)}"
    }

    fun decrypt(value: String): String {
        val parts = value.split(".")
        require(parts.size == 2) { "Invalid encrypted payload." }
        val iv = decoder.decode(parts[0])
        val encrypted = decoder.decode(parts[1])
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(config.dataKey, "AES"), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    private fun randomCode(size: Int): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(size) {
            repeat(size) { append(alphabet[random.nextInt(alphabet.length)]) }
        }
    }

    private fun hmac(message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(config.cpfPepper, "HmacSHA256"))
        return encoder.encodeToString(mac.doFinal(message.toByteArray(Charsets.UTF_8)))
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return encoder.encodeToString(digest)
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0.toByte()
        for (index in a.indices) result = result xor (a[index] xor b[index])
        return result.toInt() == 0
    }
}
