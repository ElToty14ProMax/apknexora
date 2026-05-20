package com.nexora.app

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
