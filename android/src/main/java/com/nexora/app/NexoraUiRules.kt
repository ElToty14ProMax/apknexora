package com.nexora.app

import java.time.LocalDate

internal const val NexoraDefaultBaseUrl = "https://backend-laravel-two.vercel.app"

internal fun normalizeApiBaseUrl(value: String): String {
    val clean = value.trim().trimEnd('/')
    val normalized = clean.lowercase()
    return when {
        clean.isBlank() -> NexoraDefaultBaseUrl
        normalized == "https://nexoraappbr.com" -> NexoraDefaultBaseUrl
        normalized == "https://nexoraappbr.com/api" -> NexoraDefaultBaseUrl
        normalized == "https://www.nexoraappbr.com" -> NexoraDefaultBaseUrl
        normalized == "https://www.nexoraappbr.com/api" -> NexoraDefaultBaseUrl
        normalized == "http://nexoraappbr.com" -> NexoraDefaultBaseUrl
        normalized.startsWith("https://backend-laravel-two.vercel.app") -> NexoraDefaultBaseUrl
        normalized == "http://10.0.2.2" -> NexoraDefaultBaseUrl
        else -> clean
    }
}

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

internal data class BirthdateInputFormat(
    val text: String,
    val cursor: Int,
)

internal fun formatBirthdateInput(value: String, cursor: Int): BirthdateInputFormat {
    val safeCursor = cursor.coerceIn(0, value.length)
    val digitsBeforeCursor = value
        .take(safeCursor)
        .count(Char::isDigit)
        .coerceIn(0, 8)
    val text = formatBirthdateInput(value)
    val mappedCursor = birthdateCursorOffset(digitsBeforeCursor).coerceIn(0, text.length)

    return BirthdateInputFormat(text = text, cursor = mappedCursor)
}

private fun birthdateCursorOffset(digitCount: Int): Int {
    val digits = digitCount.coerceIn(0, 8)
    return digits +
        (if (digits > 2) 1 else 0) +
        (if (digits > 4) 1 else 0)
}

internal fun parseBirthdateInput(value: String): LocalDate? {
    val match = Regex("""^(\d{2})/(\d{2})/(\d{4})$""").matchEntire(value.trim()) ?: return null
    val day = match.groupValues[1].toInt()
    val month = match.groupValues[2].toInt()
    val year = match.groupValues[3].toInt()
    return runCatching { LocalDate.of(year, month, day) }.getOrNull()
}

internal fun birthdateInputToIso(value: String): String? = parseBirthdateInput(value)?.toString()

internal fun bottomTabForTap(
    tabOrder: List<MainTab>,
    barWidthPx: Int,
    tapX: Float,
): MainTab? {
    if (tabOrder.isEmpty() || barWidthPx <= 0) return null
    val itemWidth = barWidthPx / tabOrder.size.toFloat()
    val index = (tapX / itemWidth).toInt().coerceIn(0, tabOrder.lastIndex)
    return tabOrder[index]
}

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

        normalized.contains("<!doctype") ||
            normalized.contains("jsonobject") ||
            normalized.contains("jsonarray") ||
            normalized.contains("api respondeu") ||
            normalized.contains("resposta invalida da api") ->
            "Nao consegui carregar os dados da Nexora. Confira a conexao e tente novamente."

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
        "pagina web",
        "resposta invalida",
        "erro 5",
        "http 5",
    ).any { message.contains(it) }
