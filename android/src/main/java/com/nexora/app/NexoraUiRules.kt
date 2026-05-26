package com.nexora.app

import java.time.LocalDate

internal fun filterContributionHistory(
    history: List<ContributionHistory>,
    filter: String,
): List<ContributionHistory> {
    val sorted = history.sortedByDescending { it.createdAt }
    return when (filter) {
        "ACTIVE" -> sorted.filter { it.status != "CANCELLED" && it.status != "EXPIRED" }
        "CANCELLED" -> sorted.filter { it.status == "CANCELLED" || it.status == "EXPIRED" }
        else -> sorted
    }
}

internal fun isRandomPixKey(value: String): Boolean =
    Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
        .matches(value.trim())

internal fun formatBirthdateInput(value: String): String {
    val digits = value.filter(Char::isDigit).take(8)
    return buildString {
        digits.forEachIndexed { index, char ->
            if (index == 2 || index == 4) append('/')
            append(char)
        }
    }
}

internal fun parseBirthdateInput(value: String): LocalDate? {
    val match = Regex("""^(\d{2})/(\d{2})/(\d{4})$""").matchEntire(value.trim()) ?: return null
    val day = match.groupValues[1].toInt()
    val month = match.groupValues[2].toInt()
    val year = match.groupValues[3].toInt()
    return runCatching { LocalDate.of(year, month, day) }.getOrNull()
}

internal fun birthdateInputToIso(value: String): String? = parseBirthdateInput(value)?.toString()

internal fun friendlyErrorMessage(message: String): String {
    val normalized = message.lowercase()
    return when {
        normalized.contains("nao ha solicitacoes") ||
            normalized.contains("não há solicitações") ->
            "Não há solicitações abertas de outras pessoas para distribuir esse valor. Se a única solicitação é sua, ela não pode receber seu próprio Pix."

        normalized.contains("acesso") && normalized.contains("autorizado") ->
            "CPF/e-mail ou senha incorretos. Confira os dados e tente novamente."

        normalized.contains("código inválido") ||
            normalized.contains("codigo invalido") ->
            "Código inválido ou expirado. Confira o e-mail ou solicite um novo código."

        normalized.contains("payload criptografado invalido") ||
            normalized.contains("descriptografar dados") ->
            "Dados seguros da conta ficaram inconsistentes. Entre novamente apos a limpeza da base."

        isConnectionProblem(normalized) ->
            "Problema de conexão. Confira sua internet e tente novamente."

        else -> message
    }
}

private fun isConnectionProblem(message: String): Boolean =
    listOf(
        "backend",
        "failed to connect",
        "connection refused",
        "network is unreachable",
        "unable to resolve host",
        "timeout",
        "timed out",
        "unexpected end of stream",
        "java.net",
        "erro 5",
        "http 5",
    ).any { message.contains(it) }
