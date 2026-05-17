package com.nexora.backend

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

class EmailService(private val config: AppConfig) {
    fun isConfigured(): Boolean =
        !config.smtp.username.isNullOrBlank() && !config.smtp.password.isNullOrBlank()

    fun sendVerificationCode(to: String, name: String, code: String) {
        if (!isConfigured()) {
            println("NEXORA DEV EMAIL: verification code for $to is $code")
            return
        }

        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", config.smtp.host)
            put("mail.smtp.port", config.smtp.port.toString())
        }
        val session = Session.getInstance(
            props,
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication =
                    PasswordAuthentication(config.smtp.username, config.smtp.password)
            },
        )

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(config.smtp.from, "Nexora"))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            subject = "Código de verificação Nexora"
            setText(
                """
                Olá, $name.

                Seu código de verificação Nexora é: $code

                O código expira em 30 minutos. Se você não iniciou este cadastro, ignore esta mensagem.
                """.trimIndent(),
                Charsets.UTF_8.name(),
            )
        }
        jakarta.mail.Transport.send(message)
    }

    fun sendRecoveryCode(to: String, code: String) {
        if (!isConfigured()) {
            println("NEXORA DEV EMAIL: recovery code for $to is $code")
            return
        }

        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", config.smtp.host)
            put("mail.smtp.port", config.smtp.port.toString())
        }
        val session = Session.getInstance(
            props,
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication =
                    PasswordAuthentication(config.smtp.username, config.smtp.password)
            },
        )

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(config.smtp.from, "Nexora"))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            subject = "Recuperação de acesso Nexora"
            setText(
                """
                Seu código de recuperação Nexora é: $code

                O código expira em 30 minutos. Se você não pediu recuperação, ignore esta mensagem.
                """.trimIndent(),
                Charsets.UTF_8.name(),
            )
        }
        jakarta.mail.Transport.send(message)
    }
}
