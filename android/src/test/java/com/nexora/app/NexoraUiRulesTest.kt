package com.nexora.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NexoraUiRulesTest {
    @Test
    fun connection_errors_do_not_expose_backend_wording() {
        val message = friendlyErrorMessage("Problema con backend: java.net.SocketTimeoutException")

        assertEquals("Problema de conexão. Confira sua internet e tente novamente.", message)
        assertFalse(message.contains("backend", ignoreCase = true))
    }

    @Test
    fun history_defaults_can_show_expired_transactions_when_all_is_selected() {
        val history = listOf(
            contribution("active", "PENDING_RECEIPTS", 1000L),
            contribution("expired", "EXPIRED", 2000L),
            contribution("cancelled", "CANCELLED", 3000L),
        )

        assertEquals(listOf("cancelled", "expired", "active"), filterContributionHistory(history, "ALL").map { it.id })
        assertEquals(listOf("active"), filterContributionHistory(history, "ACTIVE").map { it.id })
        assertEquals(listOf("cancelled", "expired"), filterContributionHistory(history, "CANCELLED").map { it.id })
    }

    @Test
    fun language_options_have_flags() {
        assertEquals("🇧🇷", AppLanguage.PT.flag)
        assertEquals("🇪🇸", AppLanguage.ES.flag)
        assertEquals("🇺🇸", AppLanguage.EN.flag)
    }

    @Test
    fun random_pix_key_validator_rejects_email_phone_and_cpf() {
        assertTrue(isRandomPixKey("550e8400-e29b-41d4-a716-446655440000"))
        assertFalse(isRandomPixKey("pix@example.com"))
        assertFalse(isRandomPixKey("+5511987654321"))
        assertFalse(isRandomPixKey("52998224725"))
    }

    @Test
    fun birthdate_input_adds_slashes_and_converts_to_iso() {
        assertEquals("25/05/1990", formatBirthdateInput("25051990"))
        assertEquals("25/05/1990", formatBirthdateInput("25/05/1990"))
        assertEquals("1990-05-25", birthdateInputToIso("25/05/1990"))
        assertEquals(null, birthdateInputToIso("31/02/1990"))
    }

    @Test
    fun birthdate_input_maps_cursor_after_automatic_slashes() {
        assertEquals(BirthdateInputFormat("2", 1), formatBirthdateInput("2", 1))
        assertEquals(BirthdateInputFormat("25", 2), formatBirthdateInput("25", 2))
        assertEquals(BirthdateInputFormat("25/0", 4), formatBirthdateInput("250", 3))
        assertEquals(BirthdateInputFormat("25/05", 5), formatBirthdateInput("25/05", 5))
        assertEquals(BirthdateInputFormat("25/05/1", 7), formatBirthdateInput("25/051", 6))
        assertEquals(BirthdateInputFormat("25/05/1990", 10), formatBirthdateInput("25051990", 8))
    }

    @Test
    fun bottom_tab_for_tap_maps_x_position_to_expected_tab() {
        val tabs = listOf(
            MainTab.PAINEL,
            MainTab.COMUNIDADE,
            MainTab.SOLICITAR,
            MainTab.PERFIL,
            MainTab.ADMIN,
        )

        assertEquals(MainTab.PAINEL, bottomTabForTap(tabs, barWidthPx = 1000, tapX = 0f))
        assertEquals(MainTab.COMUNIDADE, bottomTabForTap(tabs, barWidthPx = 1000, tapX = 250f))
        assertEquals(MainTab.SOLICITAR, bottomTabForTap(tabs, barWidthPx = 1000, tapX = 500f))
        assertEquals(MainTab.PERFIL, bottomTabForTap(tabs, barWidthPx = 1000, tapX = 750f))
        assertEquals(MainTab.ADMIN, bottomTabForTap(tabs, barWidthPx = 1000, tapX = 999f))
    }

    @Test
    fun encrypted_payload_errors_are_hidden_from_users() {
        val message = friendlyErrorMessage("Payload criptografado invalido.")

        assertFalse(message.contains("Payload", ignoreCase = true))
        assertFalse(message.contains("criptografado", ignoreCase = true))
    }

    private fun contribution(id: String, status: String, createdAt: Long) = ContributionHistory(
        id = id,
        transactionId = null,
        requestPublicCode = "AP-TEST",
        donorPublicId = "NX-DONOR",
        receiverPublicId = "NX-RECEIVER",
        direction = "SENT",
        amountCents = 1000,
        status = status,
        hasSenderReceipt = false,
        hasReceiverReceipt = false,
        evidenceComplete = false,
        createdAt = createdAt,
    )
}
