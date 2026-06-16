package com.nexora.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MainTab { PAINEL, COMUNIDADE, SOLICITAR, PERFIL, ADMIN }

data class NexoraUiState(
    val baseUrl: String = NexoraDefaultBaseUrl,
    val profile: Profile? = null,
    val dashboard: Dashboard? = null,
    val community: List<SupportRequest> = emptyList(),
    val myRequests: List<SupportRequest> = emptyList(),
    val contributionHistory: List<ContributionHistory> = emptyList(),
    val adminUsers: List<AdminUser> = emptyList(),
    val adminRequests: List<AdminSupportRequest> = emptyList(),
    val adminContributions: List<AdminContribution> = emptyList(),
    val tab: MainTab = MainTab.PAINEL,
    val loading: Boolean = false,
    val loadingAction: String? = null,
    val actionInProgress: String? = null,
    val refreshing: Boolean = false,
    val refreshingAction: String? = null,
    val refreshingTabs: Set<MainTab> = emptySet(),
    val message: String? = null,
    val messageIsError: Boolean = false,
    val pixInstructions: List<PixInstruction> = emptyList(),
    val hasSavedSession: Boolean = false,
    val passwordResetComplete: Int = 0,
    val registrationEmail: String? = null,
    val sessionLocked: Boolean = false,
    val lockCountdown: Int = 0,
    val language: AppLanguage = AppLanguage.PT,
)

class NexoraViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("nexora", 0)
    private val defaultBaseUrl = NexoraDefaultBaseUrl
    private val savedBaseUrl = prefs.getString("base_url", null)?.trim()
    private val initialBaseUrl = savedBaseUrl
        ?.takeIf { it.isNotBlank() }
        ?.let(::normalizeApiBaseUrl)
        ?: defaultBaseUrl
    private val savedLanguage = AppLanguage.fromCode(prefs.getString("language", null))
    private val api = ApiClient(
        baseUrl = initialBaseUrl,
        token = prefs.getString("token", null),
    )

    private var lastActiveTime: Long = System.currentTimeMillis()
    private val autoLockTimeMs = 3 * 60 * 1000L // 3 minutes

    var state by mutableStateOf(NexoraUiState(baseUrl = api.baseUrl, hasSavedSession = !api.token.isNullOrBlank(), language = savedLanguage))
        private set

    init {
        if (savedBaseUrl != null && savedBaseUrl != api.baseUrl) {
            prefs.edit().putString("base_url", api.baseUrl).apply()
        }
        NexoraLanguageStore.current = savedLanguage
    }

    fun onAppForeground() {
        if (state.profile != null && !state.sessionLocked) {
            val backgroundTime = System.currentTimeMillis() - lastActiveTime
            if (backgroundTime > autoLockTimeMs) {
                lockSession()
            }
        }
    }

    fun onAppBackground() {
        lastActiveTime = System.currentTimeMillis()
    }

    private fun lockSession() {
        viewModelScope.launch {
            state = state.copy(sessionLocked = true, lockCountdown = 10)
            while (state.lockCountdown > 0 && state.sessionLocked) {
                delay(1000)
                state = state.copy(lockCountdown = state.lockCountdown - 1)
            }
            if (state.sessionLocked) {
                logout()
            }
        }
    }

    fun setTab(tab: MainTab) {
        if (state.tab == tab) return
        state = state.copy(tab = tab, message = null, messageIsError = false)
    }

    fun updateBaseUrl(value: String) {
        api.baseUrl = normalizeApiBaseUrl(value)
        prefs.edit().putString("base_url", api.baseUrl).apply()
        state = state.copy(baseUrl = api.baseUrl)
    }

    fun setLanguage(language: AppLanguage) {
        NexoraLanguageStore.current = language
        prefs.edit().putString("language", language.code).apply()
        state = state.copy(language = language)
    }

    fun register(name: String, email: String, cpf: String, birthdate: String, pixKey: String, password: String, inviteCode: String?) = launchUi {
        val registrationEmail = email.trim().lowercase()
        val result = api.register(name, registrationEmail, cpf, birthdate, pixKey, password, inviteCode)
        val message = if (result.all { it.isDigit() } && result.length == 6) {
            "Cadastro criado. Código dev: $result"
        } else {
            result
        }
        state = state.copy(message = message, messageIsError = false, registrationEmail = registrationEmail)
    }

    fun verifyEmail(email: String, code: String) = launchUi {
        val profile = api.verifyEmail(email, code)
        prefs.edit().putString("token", api.token).apply()
        state = state.copy(
            profile = profile,
            tab = MainTab.PAINEL,
            hasSavedSession = true,
            sessionLocked = false,
            message = "E-mail verificado. Entrada realizada.",
            messageIsError = false,
        )
        loadAll()
    }

    fun resendVerification(email: String) = launchUi {
        api.resendVerification(email)
        state = state.copy(message = "Se o cadastro existir, um novo código será enviado.", messageIsError = false)
    }

    fun recoverPassword(email: String) = launchUi {
        api.recoverPassword(email)
        state = state.copy(message = "Se o e-mail existir, enviaremos instruções de recuperação.", messageIsError = false)
    }

    fun resetPassword(email: String, code: String, newPassword: String) = launchUi {
        api.resetPassword(email, code, newPassword)
        val loginResult = runCatching { api.login(email, newPassword) }
        if (loginResult.isSuccess) {
            prefs.edit().putString("token", api.token).apply()
            state = state.copy(
                profile = loginResult.getOrThrow(),
                tab = MainTab.PAINEL,
                hasSavedSession = true,
                message = null,
                messageIsError = false,
            )
            loadAll()
            state = state.copy(message = "Senha atualizada. Entrada realizada.", messageIsError = false)
        } else {
            state = state.copy(
                passwordResetComplete = state.passwordResetComplete + 1,
                message = "Senha atualizada. Entre novamente com a nova senha.",
                messageIsError = false,
            )
        }
    }

    fun login(identifier: String, password: String) = launchUi {
        val profile = api.login(identifier, password)
        prefs.edit().putString("token", api.token).apply()
        state = state.copy(profile = profile, tab = MainTab.PAINEL, message = null, messageIsError = false, hasSavedSession = true)
        loadAll()
    }

    fun unlockSavedSession() = launchUi {
        if (api.token.isNullOrBlank()) throw ApiError("Sessão salva ausente.")
        loadAll()
        state = state.copy(tab = MainTab.PAINEL, message = null, messageIsError = false, hasSavedSession = true, sessionLocked = false)
    }

    fun logout() {
        viewModelScope.launch {
            state = state.copy(loading = true, loadingAction = "Saindo...", message = null, messageIsError = false)
            delay(350)
            api.token = null
            prefs.edit().remove("token").apply()
            state = NexoraUiState(baseUrl = api.baseUrl, hasSavedSession = false, language = state.language)
        }
    }

    fun refreshAll() = launchUi(refreshAction = "Atualizando painel", refreshTab = MainTab.PAINEL) {
        loadAll()
    }

    private suspend fun loadAll() = coroutineScope {
        val profileRequest = async { api.me() }
        val dashboardRequest = async { api.dashboard() }
        val communityRequest = async { api.community() }
        val mineRequest = async { api.myRequests() }
        val historyRequest = async { api.contributionHistory() }
        val profile = profileRequest.await()
        val dashboard = dashboardRequest.await()
        val community = communityRequest.await()
        val mine = mineRequest.await()
        val history = historyRequest.await()
        state = state.copy(
            profile = profile,
            dashboard = dashboard,
            community = community,
            myRequests = mine,
            contributionHistory = history,
            message = null,
            messageIsError = false,
            hasSavedSession = !api.token.isNullOrBlank(),
        )
        if (profile.role == "ADMIN" || profile.role == "SUPER_ADMIN") loadAdmin()
    }

    fun refreshCommunity() = launchUi(refreshAction = "Atualizando comunidade", refreshTab = MainTab.COMUNIDADE) {
        state = state.copy(community = api.community())
    }

    fun refreshProfileAndMine() = launchUi(refreshAction = "Atualizando perfil", refreshTab = MainTab.PERFIL) {
        loadProfileAndMine()
    }

    private suspend fun loadProfileAndMine() = coroutineScope {
        val profileRequest = async { api.me() }
        val mineRequest = async { api.myRequests() }
        val historyRequest = async { api.contributionHistory() }
        state = state.copy(
            profile = profileRequest.await(),
            myRequests = mineRequest.await(),
            contributionHistory = historyRequest.await(),
        )
    }

    fun createSupportRequest(amountCents: Long, dueDays: Int, description: String?) = launchUi {
        api.createSupportRequest(amountCents, dueDays, description)
        state = state.copy(message = "Solicitação enviada para validação manual.", messageIsError = false)
        loadAll()
    }

    fun createContribution(request: SupportRequest, amountCents: Long) = launchUi {
        val instruction = api.createContribution(request.id, amountCents)
        loadProfileAndMine()
        state = state.copy(pixInstructions = listOf(instruction), message = "Código Pix gerado.", messageIsError = false)
    }

    fun createContributionBatch(amountCents: Long) = launchUi {
        val instructions = api.createContributionBatch(amountCents)
        loadProfileAndMine()
        state = state.copy(pixInstructions = instructions, message = "Pix fracionado por ordem cronológica.", messageIsError = false)
    }

    fun submitReceipt(
        contributionId: String,
        amountCents: Long,
        transactionId: String,
        upload: ReceiptUpload,
        receiptDate: String,
        side: String,
    ) = launchUi {
        api.submitPixReceipt(contributionId, transactionId, upload, amountCents, receiptDate, side)
        state = state.copy(message = "Foto anexada. Aguarde validação administrativa.", messageIsError = false)
        loadProfileAndMine()
    }

    fun clearPixInstruction() {
        state = state.copy(pixInstructions = emptyList())
    }

    fun clearMessage() {
        state = state.copy(message = null, messageIsError = false)
    }

    fun clearRegistrationEmail() {
        state = state.copy(registrationEmail = null)
    }

    fun showValidationError(message: String) {
        state = state.copy(message = message, messageIsError = true)
    }

    fun refreshAdmin(silent: Boolean = false) = launchUi(
        silent = silent,
        refreshAction = if (silent) null else "Atualizando admin",
        refreshTab = MainTab.ADMIN,
    ) {
        loadAdmin()
    }

    private suspend fun loadAdmin() = coroutineScope {
        val usersRequest = async { api.adminUsers() }
        val supportRequestsRequest = async { api.adminSupportRequests() }
        val contributionsRequest = async { api.adminContributions() }
        state = state.copy(
            adminUsers = usersRequest.await(),
            adminRequests = supportRequestsRequest.await(),
            adminContributions = contributionsRequest.await(),
        )
    }

    fun adminApproveUser(id: String) = adminAction("/admin/users/$id/approve", "Usuário aprovado.")
    fun adminBlockUser(id: String) = adminAction("/admin/users/$id/block", "Usuário bloqueado.")
    fun adminUnblockUser(id: String) = adminAction("/admin/users/$id/unblock", "Usuário desbloqueado.")
    fun adminConfirmFee(id: String) = adminAction("user-fee-$id", "/admin/users/$id/confirm-admin-fee", "Taxa baixada.")
    fun adminApproveRequest(id: String) = adminAction("/admin/support-requests/$id/approve", "Solicitação aprovada.")
    fun adminRejectRequest(id: String) = adminAction("/admin/support-requests/$id/reject", "Solicitação recusada.")
    fun adminConfirmReturn(id: String) = adminAction("/admin/support-requests/$id/confirm-return", "Retorno validado.")
    fun adminConfirmContribution(id: String) = adminAction("contribution-confirm-$id", "/admin/contributions/$id/confirm", "Apoio validado.")
    fun adminRejectContribution(id: String) = adminAction("contribution-reject-$id", "/admin/contributions/$id/reject", "Apoio recusado.")

    private fun adminAction(path: String, success: String) = adminAction(path, path, success)

    private fun adminAction(actionKey: String, path: String, success: String) {
        viewModelScope.launch {
            state = state.copy(actionInProgress = actionKey, message = null, messageIsError = false)
            try {
                api.adminPost(path)
                state = state.copy(message = success, messageIsError = false)
                loadAdmin()
                loadAll()
            } catch (error: Exception) {
                state = state.copy(message = friendlyErrorMessage(error.message ?: "Erro inesperado."), messageIsError = true)
            } finally {
                state = state.copy(actionInProgress = null)
            }
        }
    }

    private fun launchUi(
        silent: Boolean = false,
        refreshAction: String? = null,
        refreshTab: MainTab? = null,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            if (!silent) {
                state = if (refreshAction != null) {
                    state.copy(
                        refreshing = true,
                        refreshingAction = refreshAction,
                        refreshingTabs = refreshTab?.let { state.refreshingTabs + it } ?: state.refreshingTabs,
                        message = null,
                        messageIsError = false,
                    )
                } else {
                    state.copy(
                        loading = true,
                        loadingAction = "Aguarde",
                        message = null,
                        messageIsError = false,
                    )
                }
            }
            try {
                block()
            } catch (error: Exception) {
                val cleanMessage = friendlyErrorMessage(error.message ?: "Erro inesperado.")
                if (error.message?.contains("Sessão", ignoreCase = true) == true ||
                    error.message?.contains("Token", ignoreCase = true) == true
                ) {
                    prefs.edit().remove("token").apply()
                    api.token = null
                    state = NexoraUiState(baseUrl = api.baseUrl, message = cleanMessage, messageIsError = true, hasSavedSession = false, language = state.language)
                } else {
                    state = state.copy(message = cleanMessage, messageIsError = true)
                }
            } finally {
                state = if (refreshAction != null) {
                    val remainingTabs = refreshTab?.let { state.refreshingTabs - it } ?: state.refreshingTabs
                    state.copy(
                        refreshing = remainingTabs.isNotEmpty(),
                        refreshingAction = if (remainingTabs.isEmpty()) null else state.refreshingAction,
                        refreshingTabs = remainingTabs,
                    )
                } else {
                    state.copy(loading = false, loadingAction = null)
                }
            }
        }
    }

    private fun friendlyError(message: String): String = when {
        message.contains("Nao ha solicitacoes", ignoreCase = true) ||
            message.contains("Não há solicitações", ignoreCase = true) ->
            "Não há solicitações abertas de outras pessoas para distribuir esse valor. Se a única solicitação é sua, ela não pode receber seu próprio Pix."
        message.contains("Acesso", ignoreCase = true) && message.contains("autorizado", ignoreCase = true) ->
            "CPF/e-mail ou senha incorretos. Confira os dados e tente novamente."
        message.contains("Código inválido", ignoreCase = true) ||
            message.contains("Codigo invalido", ignoreCase = true) ->
            "Código inválido ou expirado. Confira o e-mail ou solicite um novo código."
        else -> message
    }
}
