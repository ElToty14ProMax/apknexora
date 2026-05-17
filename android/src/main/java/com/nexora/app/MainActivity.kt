package com.nexora.app

import android.os.Bundle
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val NexoraGreen = Color(0xFF00E060)
private val NexoraDarkGreen = Color(0xFF002B16)
private val NexoraBlack = Color(0xFF000000)
private val NexoraCard = Color(0xFF171717)
private val NexoraField = Color(0xFF101010)
private val NexoraText = Color(0xFFF4F4F4)
private val NexoraMuted = Color(0xFF8A8A93)
private val NexoraRed = Color(0xFFFF4D4D)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val biometricAvailable = canUseBiometrics()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = NexoraGreen,
                    background = NexoraBlack,
                    surface = NexoraCard,
                    onPrimary = NexoraBlack,
                    onBackground = NexoraText,
                    onSurface = NexoraText,
                ),
            ) {
                Surface(color = NexoraBlack) {
                    val viewModel: NexoraViewModel = viewModel()
                    NexoraApp(
                        viewModel = viewModel,
                        biometricAvailable = biometricAvailable,
                        onBiometricLogin = {
                            showBiometricPrompt { viewModel.unlockSavedSession() }
                        },
                    )
                }
            }
        }
    }

    private fun canUseBiometrics(): Boolean {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Entrar com digital")
            .setSubtitle("Desbloqueie sua sessao Nexora salva")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }
}

@Composable
private fun NexoraApp(
    viewModel: NexoraViewModel,
    biometricAvailable: Boolean,
    onBiometricLogin: () -> Unit,
) {
    val state = viewModel.state
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarIsError by remember { mutableStateOf(false) }

    LaunchedEffect(state.message, state.messageIsError) {
        state.message?.let { message ->
            snackbarIsError = state.messageIsError
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Box(Modifier.fillMaxSize().background(NexoraBlack)) {
        if (state.profile == null) {
            AuthScreen(state, viewModel, biometricAvailable, onBiometricLogin)
        } else {
            MainShell(state, viewModel)
        }

        if (state.pixInstructions.isNotEmpty()) {
            PixDialog(
                instructions = state.pixInstructions,
                loading = state.loading,
                onSubmitReceipt = viewModel::submitReceipt,
                onDismiss = viewModel::clearPixInstruction,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
            snackbar = { data ->
                NoticeSnackbar(message = data.visuals.message, isError = snackbarIsError)
            },
        )

        if (state.loading) {
            LoadingOverlay(state.loadingAction ?: "Aguarde")
        }
    }
}

private enum class AuthMode { LOGIN, REGISTER, VERIFY, RECOVER_SEND, RECOVER_RESET }

@Composable
private fun AuthScreen(
    state: NexoraUiState,
    viewModel: NexoraViewModel,
    biometricAvailable: Boolean,
    onBiometricLogin: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(AuthMode.LOGIN) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var cpf by rememberSaveable { mutableStateOf("") }
    var pixKey by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmNewPassword by rememberSaveable { mutableStateOf("") }
    var invite by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var server by rememberSaveable(state.baseUrl) { mutableStateOf(state.baseUrl) }
    var showServerConfig by rememberSaveable { mutableStateOf(false) }
    var invalidFields by remember { mutableStateOf(emptySet<String>()) }
    var handledPasswordReset by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(state.passwordResetComplete) {
        if (state.passwordResetComplete > handledPasswordReset) {
            handledPasswordReset = state.passwordResetComplete
            mode = AuthMode.LOGIN
            password = ""
            newPassword = ""
            confirmNewPassword = ""
            code = ""
            invalidFields = emptySet()
        }
    }

    fun clearInvalid(field: String) {
        if (field in invalidFields) invalidFields = invalidFields - field
    }

    fun fail(field: String, message: String): Boolean {
        invalidFields = setOf(field)
        viewModel.showValidationError(message)
        return false
    }

    fun validateAuthAction(): Boolean = when (mode) {
        AuthMode.LOGIN -> when {
            cpf.isBlank() -> fail("identifier", "Informe seu CPF ou e-mail.")
            password.isBlank() -> fail("password", "Informe sua senha.")
            else -> {
                invalidFields = emptySet()
                true
            }
        }
        AuthMode.REGISTER -> when {
            name.trim().length < 2 -> fail("name", "Informe seu nome completo.")
            email.isBlank() || !email.contains("@") -> fail("email", "Informe um e-mail valido.")
            cpf.filter(Char::isDigit).length != 11 -> fail("cpf", "Informe um CPF com 11 dígitos.")
            pixKey.isBlank() -> fail("pixKey", "Informe sua chave Pix.")
            password.length < 8 -> fail("password", "A senha precisa ter pelo menos 8 caracteres.")
            else -> {
                invalidFields = emptySet()
                true
            }
        }
        AuthMode.VERIFY -> when {
            email.isBlank() || !email.contains("@") -> fail("email", "Informe o e-mail usado no cadastro.")
            code.length != 6 -> fail("code", "Digite o código de 6 dígitos.")
            else -> {
                invalidFields = emptySet()
                true
            }
        }
        AuthMode.RECOVER_SEND -> when {
            email.isBlank() || !email.contains("@") -> fail("email", "Informe o e-mail da conta.")
            else -> {
                invalidFields = emptySet()
                true
            }
        }
        AuthMode.RECOVER_RESET -> when {
            email.isBlank() || !email.contains("@") -> fail("email", "Informe o e-mail da conta.")
            code.length != 6 -> fail("code", "Digite o código de 6 dígitos.")
            newPassword.length < 8 -> fail("newPassword", "A nova senha precisa ter pelo menos 8 caracteres.")
            confirmNewPassword != newPassword -> fail("confirmNewPassword", "As senhas não conferem.")
            else -> {
                invalidFields = emptySet()
                true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NexoraLogo()
        Spacer(Modifier.height(48.dp))
        
        Text(
            text = when (mode) {
                AuthMode.LOGIN -> "Entrar"
                AuthMode.REGISTER -> "Cadastro"
                AuthMode.VERIFY -> "Verificar e-mail"
                AuthMode.RECOVER_SEND -> "Recuperar senha"
                AuthMode.RECOVER_RESET -> "Nova senha"
            },
            color = NexoraText,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )
        Text(
            text = when (mode) {
                AuthMode.LOGIN -> "Acesse com CPF ou e-mail"
                AuthMode.REGISTER -> "Preencha seus dados para começar"
                AuthMode.VERIFY -> "Digite o código enviado ao e-mail"
                AuthMode.RECOVER_SEND -> "Informe o e-mail para receber um código"
                AuthMode.RECOVER_RESET -> if (code.length == 6) "Defina e confirme a nova senha" else "Digite o código recebido por e-mail"
            },
            color = NexoraMuted,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 32.dp),
            textAlign = TextAlign.Start,
        )

        NexoraPanel {
            if (mode == AuthMode.REGISTER) {
                NexoraInput("Nome completo", name, { name = it; clearInvalid("name") }, isError = "name" in invalidFields)
                Spacer(Modifier.height(16.dp))
                NexoraInput("E-mail", email, { email = it; clearInvalid("email") }, keyboardType = KeyboardType.Email, isError = "email" in invalidFields)
                Spacer(Modifier.height(16.dp))
                NexoraInput("CPF", cpf, { if (it.length <= 11) cpf = it; clearInvalid("cpf") }, keyboardType = KeyboardType.Number, visualTransformation = CpfVisualTransformation(), isError = "cpf" in invalidFields)
                Spacer(Modifier.height(16.dp))
                NexoraInput("Chave Pix", pixKey, { pixKey = it; clearInvalid("pixKey") }, placeholder = "Chave Pix (CPF, e-mail ou aleatória)", isError = "pixKey" in invalidFields)
                Spacer(Modifier.height(16.dp))
                NexoraInput("Código de convite", invite, { invite = it })
                Spacer(Modifier.height(16.dp))
            } else {
                when (mode) {
                    AuthMode.LOGIN -> {
                        NexoraInput("CPF ou e-mail", cpf, { cpf = it; clearInvalid("identifier") }, isError = "identifier" in invalidFields)
                        Spacer(Modifier.height(16.dp))
                    }
                    AuthMode.VERIFY -> {
                        NexoraInput("E-mail", email, { email = it; clearInvalid("email") }, keyboardType = KeyboardType.Email, isError = "email" in invalidFields)
                        Spacer(Modifier.height(16.dp))
                        NexoraInput("Código", code, { if (it.length <= 6) code = it; clearInvalid("code") }, keyboardType = KeyboardType.Number, isError = "code" in invalidFields)
                        Spacer(Modifier.height(16.dp))
                    }
                    AuthMode.RECOVER_SEND -> {
                        NexoraInput("E-mail", email, { email = it; clearInvalid("email") }, keyboardType = KeyboardType.Email, isError = "email" in invalidFields)
                        Spacer(Modifier.height(16.dp))
                    }
                    else -> {
                        NexoraInput("E-mail", email, { email = it; clearInvalid("email") }, keyboardType = KeyboardType.Email, isError = "email" in invalidFields)
                        Spacer(Modifier.height(16.dp))
                        NexoraInput(
                            "Código",
                            code,
                            {
                                if (it.length <= 6) code = it
                                clearInvalid("code")
                                if (it.length < 6) {
                                    newPassword = ""
                                    confirmNewPassword = ""
                                    invalidFields = invalidFields - "newPassword" - "confirmNewPassword"
                                }
                            },
                            keyboardType = KeyboardType.Number,
                            isError = "code" in invalidFields,
                        )
                        Spacer(Modifier.height(16.dp))
                        if (code.length == 6) {
                            NexoraInput(
                                "Nova senha",
                                newPassword,
                                { newPassword = it; clearInvalid("newPassword") },
                                password = true,
                                isError = "newPassword" in invalidFields,
                            )
                            Spacer(Modifier.height(16.dp))
                            NexoraInput(
                                "Confirmar nova senha",
                                confirmNewPassword,
                                { confirmNewPassword = it; clearInvalid("confirmNewPassword") },
                                password = true,
                                isError = "confirmNewPassword" in invalidFields,
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }

            if (mode == AuthMode.LOGIN || mode == AuthMode.REGISTER) {
                NexoraInput(
                    label = "Senha",
                    value = password,
                    onValueChange = { password = it; clearInvalid("password") },
                    password = true,
                    isError = "password" in invalidFields,
                )
                Spacer(Modifier.height(32.dp))
            }

            NexoraButton(
                text = when (mode) {
                    AuthMode.LOGIN -> "ENTRAR"
                    AuthMode.REGISTER -> "CRIAR CONTA"
                    AuthMode.VERIFY -> "VERIFICAR"
                    AuthMode.RECOVER_SEND -> "ENVIAR CÓDIGO"
                    AuthMode.RECOVER_RESET -> if (code.length == 6) "ATUALIZAR SENHA" else "DIGITE O CÓDIGO"
                },
                loading = state.loading,
                enabled = mode != AuthMode.RECOVER_RESET || code.length == 6,
                onClick = {
                    if (validateAuthAction()) {
                        when (mode) {
                            AuthMode.LOGIN -> viewModel.login(cpf, password)
                            AuthMode.REGISTER -> {
                                viewModel.register(name, email, cpf, pixKey, password, invite.takeIf { it.isNotBlank() } )
                                mode = AuthMode.VERIFY
                            }
                            AuthMode.VERIFY -> viewModel.verifyEmail(email, code)
                            AuthMode.RECOVER_SEND -> {
                                viewModel.recoverPassword(email)
                                mode = AuthMode.RECOVER_RESET
                            }
                            AuthMode.RECOVER_RESET -> viewModel.resetPassword(email, code, newPassword)
                        }
                    }
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when (mode) {
                        AuthMode.LOGIN -> "Ainda não tem conta?"
                        AuthMode.REGISTER -> "Já tem conta?"
                        AuthMode.VERIFY -> "Não recebeu o código?"
                        AuthMode.RECOVER_SEND -> "Lembrou a senha?"
                        AuthMode.RECOVER_RESET -> "Senha atualizada?"
                    },
                    color = NexoraMuted,
                )
                TextButton(onClick = {
                    invalidFields = emptySet()
                    mode = when (mode) {
                        AuthMode.LOGIN -> AuthMode.REGISTER
                        AuthMode.REGISTER -> AuthMode.LOGIN
                        AuthMode.VERIFY -> AuthMode.LOGIN
                        AuthMode.RECOVER_SEND -> AuthMode.LOGIN
                        AuthMode.RECOVER_RESET -> AuthMode.LOGIN
                    }
                }) {
                    Text(
                        text = when (mode) {
                            AuthMode.LOGIN -> "Cadastre-se"
                            AuthMode.REGISTER -> "Entrar"
                            AuthMode.VERIFY -> "Voltar"
                            AuthMode.RECOVER_SEND -> "Entrar"
                            AuthMode.RECOVER_RESET -> "Entrar"
                        },
                        color = NexoraGreen,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (mode == AuthMode.LOGIN) {
                OutlinedButton(
                    onClick = {
                        if (state.hasSavedSession && biometricAvailable) {
                            onBiometricLogin()
                        } else {
                            val reason = when {
                                !state.hasSavedSession -> "A digital aparece depois do primeiro login salvo. Se tocar em Sair, a sessão salva é removida."
                                else -> "Ative digital, face ou bloqueio de tela no aparelho para usar este acesso."
                            }
                            viewModel.showValidationError(reason)
                        }
                    },
                    enabled = !state.loading,
                    border = BorderStroke(1.dp, if (state.hasSavedSession && biometricAvailable) NexoraGreen else NexoraMuted),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text("ENTRAR COM DIGITAL", color = if (state.hasSavedSession && biometricAvailable) NexoraGreen else NexoraMuted, fontWeight = FontWeight.Black)
                }
                Text(
                    text = if (state.hasSavedSession) {
                        "Use a digital para desbloquear a sessao salva."
                    } else {
                        "Entre uma vez e feche o app sem tocar em Sair para liberar a digital."
                    },
                    color = NexoraMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = { mode = AuthMode.VERIFY }) {
                        Text("Verificar e-mail", color = NexoraMuted)
                    }
                    TextButton(onClick = { mode = AuthMode.RECOVER_SEND }) {
                        Text("Esqueci a senha", color = NexoraMuted)
                    }
                }
            }

            if (mode == AuthMode.VERIFY) {
                TextButton(
                    enabled = !state.loading,
                    onClick = { viewModel.resendVerification(email) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reenviar código por e-mail", color = NexoraGreen, fontWeight = FontWeight.Bold)
                }
            }

            if (mode == AuthMode.RECOVER_RESET) {
                TextButton(
                    enabled = !state.loading && email.contains("@"),
                    onClick = { viewModel.recoverPassword(email) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reenviar código por e-mail", color = NexoraGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        if (showServerConfig) {
            NexoraInput("Servidor da API", server, { server = it; viewModel.updateBaseUrl(it) })
            Spacer(Modifier.height(8.dp))
        }
        
        TextButton(onClick = { showServerConfig = !showServerConfig }) {
            Text(if (showServerConfig) "Ocultar config" else "Configuração do servidor", color = NexoraMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MainShell(state: NexoraUiState, viewModel: NexoraViewModel) {
    Scaffold(
        containerColor = NexoraBlack,
        bottomBar = { NexoraBottomBar(state, viewModel) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(NexoraBlack),
        ) {
            when (state.tab) {
                MainTab.PAINEL -> DashboardScreen(state, viewModel)
                MainTab.COMUNIDADE -> CommunityScreen(state, viewModel)
                MainTab.SOLICITAR -> RequestScreen(state, viewModel)
                MainTab.PERFIL -> ProfileScreen(state, viewModel)
                MainTab.ADMIN -> AdminScreen(state, viewModel)
            }
        }
    }
}

@Composable
private fun NexoraBottomBar(state: NexoraUiState, viewModel: NexoraViewModel) {
    val profile = state.profile
    val items = buildList {
        add(Triple(MainTab.PAINEL, "Painel", Icons.Filled.Dashboard))
        add(Triple(MainTab.COMUNIDADE, "Comunidade", Icons.Filled.Groups))
        add(Triple(MainTab.SOLICITAR, "Solicitar", Icons.Filled.Add))
        add(Triple(MainTab.PERFIL, "Perfil", Icons.Filled.Person))
        if (profile?.role in setOf("ADMIN", "SUPER_ADMIN")) add(Triple(MainTab.ADMIN, "Admin", Icons.Filled.AdminPanelSettings))
    }
    NavigationBar(
        containerColor = Color(0xFF0A0A0A),
        contentColor = NexoraText,
        modifier = Modifier.navigationBarsPadding(),
    ) {
        items.forEach { (tab, label, icon) ->
            NavigationBarItem(
                selected = state.tab == tab,
                onClick = { viewModel.setTab(tab) },
                icon = { Icon(icon, contentDescription = label) },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NexoraGreen,
                    selectedTextColor = NexoraGreen,
                    unselectedIconColor = NexoraMuted,
                    unselectedTextColor = NexoraMuted,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(state: NexoraUiState, viewModel: NexoraViewModel) {
    val profile = state.profile ?: return
    val dashboard = state.dashboard
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
            item {
                HeaderRow(profile = profile, refreshing = state.refreshing, onRefresh = viewModel::refreshAll)
            }
            item {
                NexoraPanel(border = true) {
                    Text("LIQUIDEZ COMUNITÁRIA", color = NexoraMuted, fontSize = 14.sp, letterSpacing = 5.sp)
                    Text(
                        text = formatMoney(dashboard?.communityLiquidityCents ?: 0),
                        color = NexoraGreen,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    HorizontalDivider(Modifier.padding(vertical = 20.dp), color = Color(0xFF252525))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatText("Em circulação", formatMoney(dashboard?.inCirculationCents ?: 0), NexoraText)
                        StatText("Cumprimento", String.format(Locale.getDefault(), "%.1f%%", dashboard?.completionPercent ?: 100.0), NexoraGreen)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard("Solicitações ativas", "${dashboard?.activeRequests ?: 0}", Modifier.weight(1f))
                    MetricCard("Operações concluídas", "${dashboard?.completedOperations ?: 0}", Modifier.weight(1f), highlight = true)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard("Usuários ativos", "${dashboard?.activeUsers ?: 0}", Modifier.weight(1f))
                    MetricCard("Seu limite", formatMoney(dashboard?.userLimitCents ?: profile.supportLimitCents), Modifier.weight(1f), highlight = true)
                }
            }
            item {
                NexoraPanel {
                    Text("Como funciona", color = NexoraGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "A comunidade contribui com pequenos valores via Pix até completar sua solicitação. Você registra o retorno no prazo e ganha XP, buffs e maior limite.",
                        color = Color(0xFFB6B6BD),
                        fontSize = 18.sp,
                        lineHeight = 28.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
            item {
                RoadmapCard(dashboard)
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityScreen(state: NexoraUiState, viewModel: NexoraViewModel) {
    var batchAmount by rememberSaveable { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
            item {
                ScreenTitle("Comunidade", "Solicitações ativas para apoiar", viewModel::refreshCommunity, refreshing = state.refreshing)
            }
            item {
                NexoraPanel(border = true) {
                    Text("Pix por ordem cronológica", color = NexoraGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NexoraInput("Valor total", batchAmount, { batchAmount = it }, Modifier.weight(1f), "R$ 0,00", KeyboardType.Decimal)
                        Button(
                            onClick = {
                                val cents = parseMoneyToCents(batchAmount)
                                when {
                                    cents == null || cents <= 0 -> viewModel.showValidationError("Informe um valor valido para distribuir.")
                                    state.community.isEmpty() -> viewModel.showValidationError("Não há solicitações abertas de outras pessoas no momento.")
                                    else -> viewModel.createContributionBatch(cents)
                                }
                            },
                            enabled = !state.loading,
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraGreen, contentColor = NexoraBlack),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(56.dp),
                        ) {
                            Text("GERAR", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            if (state.community.isEmpty()) {
                item { EmptyState("Comunidade estável", "Nenhuma solicitação de outros usuários ativa no momento") }
            } else {
                items(state.community, key = { it.id }) { request ->
                    CommunityRequestCard(
                        request = request,
                        onSupport = { cents -> viewModel.createContribution(request, cents) },
                        onError = viewModel::showValidationError,
                    )
                }
            }
    }
}

@Composable
private fun RequestScreen(state: NexoraUiState, viewModel: NexoraViewModel) {
    val profile = state.profile ?: return
    var amount by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("15") }
    var description by remember { mutableStateOf("") }
    var invalidField by remember { mutableStateOf<String?>(null) }
    val eligible = profile.level >= 2 && profile.xp >= 100

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
    ) {
        ScreenTitle("Solicitar Apoio", "A comunidade contribui via Pix até completar o valor")
        NexoraPanel(border = true, background = NexoraDarkGreen) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SEU LIMITE", color = NexoraGreen, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                Text(formatMoney(profile.supportLimitCents), color = NexoraGreen, fontSize = 30.sp, fontWeight = FontWeight.Black)
            }
        }
        if (!eligible) {
            Spacer(Modifier.height(12.dp))
            MessageStrip("Solicitação liberada a partir do Nível 2 com 100 XP e limite disponível.")
        }
        Spacer(Modifier.height(22.dp))
        NexoraInput("Valor solicitado", amount, { amount = it; if (invalidField == "amount") invalidField = null }, keyboardType = KeyboardType.Decimal, placeholder = "R$ 0,00", isError = invalidField == "amount")
        Spacer(Modifier.height(14.dp))
        NexoraInput("Prazo para retorno (dias)", days, { days = it; if (invalidField == "days") invalidField = null }, keyboardType = KeyboardType.Number, isError = invalidField == "days")
        Spacer(Modifier.height(14.dp))
        NexoraInput("Descrição opcional", description, { description = it }, minLines = 4)
        Spacer(Modifier.height(22.dp))
        NexoraButton("SOLICITAR APOIO", loading = state.loading) {
            val cents = parseMoneyToCents(amount)
            val dueDays = days.toIntOrNull()
            when {
                !eligible -> viewModel.showValidationError("Você ainda não pode solicitar: precisa estar no Nível 2 com 100 XP.")
                cents == null || cents <= 0 -> {
                    invalidField = "amount"
                    viewModel.showValidationError("Informe um valor válido.")
                }
                cents > profile.supportLimitCents -> {
                    invalidField = "amount"
                    viewModel.showValidationError("Valor acima do seu limite atual.")
                }
                dueDays == null -> {
                    invalidField = "days"
                    viewModel.showValidationError("Informe o prazo em dias.")
                }
                dueDays !in 1..90 -> {
                    invalidField = "days"
                    viewModel.showValidationError("O prazo precisa ficar entre 1 e 90 dias.")
                }
                else -> {
                    invalidField = null
                    viewModel.createSupportRequest(cents, dueDays, description.takeIf { it.isNotBlank() })
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(state: NexoraUiState, viewModel: NexoraViewModel) {
    val profile = state.profile ?: return
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val progress = (profile.xpIntoLevel.toFloat() / profile.xpRequiredThisLevel.toFloat()).coerceIn(0f, 1f)
    val inviteLink = "https://nexora-web-mauve.vercel.app?invite=${profile.inviteCode}"
    val inviteText = "Entre na Nexora com meu convite ${profile.inviteCode}: $inviteLink"
    fun shareInvite() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, inviteText)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar convite"))
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenTitle("Perfil", profile.publicId, viewModel::refreshProfileAndMine, refreshing = state.refreshing)
        }
        item {
            NexoraPanel {
                Text(profile.name, color = NexoraText, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(profile.email, color = NexoraMuted, fontSize = 14.sp)
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text("NÍVEL", color = NexoraMuted, letterSpacing = 4.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("${profile.level}", color = NexoraGreen, fontSize = 52.sp, fontWeight = FontWeight.Black)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = NexoraGreen,
                    trackColor = Color(0xFF050505),
                )
                Text(
                    "${profile.xpIntoLevel} / ${profile.xpRequiredThisLevel} XP",
                    color = NexoraMuted,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("BUFF", formatBuff(profile.buffBps), Modifier.weight(1f), highlight = true)
                MetricCard("LIMITE", formatMoney(profile.supportLimitCents), Modifier.weight(1f), highlight = true)
                MetricCard("CONVITES", "${profile.invitedCount}", Modifier.weight(1f), highlight = true)
            }
        }
        item {
            NexoraPanel {
                LabelValue("Chave Pix", profile.pixKeyMasked, onCopy = {
                    clipboard.setText(AnnotatedString(profile.pixKeyMasked))
                    Toast.makeText(context, "Chave Pix copiada!", Toast.LENGTH_SHORT).show()
                })
                Spacer(Modifier.height(18.dp))
                Text("Código de convite", color = NexoraMuted, fontSize = 16.sp, letterSpacing = 2.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(Color(0xFF111111), RoundedCornerShape(8.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    SelectionContainer {
                        Text(profile.inviteCode, color = NexoraGreen, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(inviteLink))
                            Toast.makeText(context, "Código de convite copiado!", Toast.LENGTH_SHORT).show()
                        },
                        border = BorderStroke(1.dp, NexoraGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = NexoraGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Copiar link", color = NexoraGreen, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = { shareInvite() },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraGreen, contentColor = NexoraBlack),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Compartilhar convite", fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        item {
            NexoraPanel {
                LabelValue("Taxa administrativa acumulada", "${formatMoney(profile.adminFeeDueCents)} / ${formatMoney(profile.adminFeeLimitCents)}")
                profile.adminPixKey?.let {
                    Spacer(Modifier.height(10.dp))
                    Text("Envie a taxa para o Pix do admin e aguarde a baixa administrativa.", color = NexoraMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    LabelValue("Pix da taxa administrativa", it, onCopy = {
                        clipboard.setText(AnnotatedString(it))
                        Toast.makeText(context, "Chave Pix copiada!", Toast.LENGTH_SHORT).show()
                    })
                } ?: Text("Sem taxa pendente para envio.", color = NexoraMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
                }
        }
        item {
            Text("Minhas solicitações", color = NexoraText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        if (state.myRequests.isEmpty()) {
            item { EmptyState("Sem solicitações", "Quando criar uma solicitação, ela aparece aqui") }
        } else {
            items(state.myRequests, key = { it.id }) { request ->
                CompactRequestCard(request)
            }
        }
        item {
            Text("Histórico de transações", color = NexoraText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        if (state.contributionHistory.isEmpty()) {
            item { EmptyState("Sem transações", "Transferências Pix aparecem aqui uma única vez por ID") }
        } else {
            items(state.contributionHistory, key = { it.id }) { contribution ->
                ContributionHistoryCard(contribution, viewModel)
            }
        }
        item {
            OutlinedButton(
                onClick = viewModel::logout,
                border = BorderStroke(1.dp, NexoraRed),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Sair", color = NexoraRed)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminScreen(state: NexoraUiState, viewModel: NexoraViewModel) {
    var section by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf("ALL") }
    var receiptFilter by rememberSaveable { mutableStateOf("ALL") }
    var fromDate by rememberSaveable { mutableStateOf("") }
    var toDate by rememberSaveable { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<AdminUser?>(null) }
    var selectedRequest by remember { mutableStateOf<AdminSupportRequest?>(null) }
    var selectedContribution by remember { mutableStateOf<AdminContribution?>(null) }

    LaunchedEffect(section) {
        statusFilter = "ALL"
        receiptFilter = "ALL"
    }

    fun matchesQuery(vararg values: String?): Boolean {
        val clean = query.trim().lowercase()
        if (clean.isBlank()) return true
        return values.any { it.orEmpty().lowercase().contains(clean) }
    }

    fun dateMs(value: String, endOfDay: Boolean): Long? = runCatching {
        val date = LocalDate.parse(value)
        val time = if (endOfDay) date.plusDays(1).atStartOfDay() else date.atStartOfDay()
        time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - if (endOfDay) 1 else 0
    }.getOrNull()

    fun inDateRange(createdAt: Long): Boolean {
        val from = dateMs(fromDate, false)
        val to = dateMs(toDate, true)
        return (from == null || createdAt >= from) && (to == null || createdAt <= to)
    }

    val filteredUsers = remember(state.adminUsers, query, statusFilter, fromDate, toDate) {
        state.adminUsers.filter { user ->
            (statusFilter == "ALL" || user.status == statusFilter) &&
                inDateRange(user.createdAt) &&
                matchesQuery(user.name, user.email, user.publicId, user.cpf, user.pixKey, user.inviteCode, user.role)
        }
    }
    val filteredRequests = remember(state.adminRequests, query, statusFilter, fromDate, toDate) {
        state.adminRequests.filter { request ->
            (statusFilter == "ALL" || request.status == statusFilter) &&
                inDateRange(request.createdAt) &&
                matchesQuery(request.publicCode, request.requesterName, request.requesterEmail, request.requesterPublicId, request.requesterCpf, request.requesterPixKey)
        }
    }
    val filteredContributions = remember(state.adminContributions, query, statusFilter, receiptFilter, fromDate, toDate) {
        state.adminContributions.filter { contribution ->
            (statusFilter == "ALL" || contribution.status == statusFilter) &&
                (receiptFilter == "ALL" ||
                    (receiptFilter == "complete" && contribution.evidenceComplete) ||
                    (receiptFilter == "missing" && !contribution.evidenceComplete)) &&
                inDateRange(contribution.createdAt) &&
                matchesQuery(
                    contribution.id,
                    contribution.requestPublicCode,
                    contribution.transactionId,
                    contribution.donorPublicId,
                    contribution.donorName,
                    contribution.donorEmail,
                    contribution.receiverPublicId,
                    contribution.receiverName,
                    contribution.receiverEmail,
                )
        }
    }
    val statusOptions = when (section) {
        0 -> listOf("ALL", "PENDING_REVIEW", "APPROVED", "BLOCKED")
        1 -> listOf("ALL", "PENDING_ADMIN", "OPEN", "FUNDED", "RETURNED", "REJECTED")
        else -> listOf("ALL", "PENDING_ADMIN", "CONFIRMED")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
            item {
                ScreenTitle("Admin Nexora", "Painel de controle dos fundadores", { viewModel.refreshAdmin() }, refreshing = state.refreshing)
                ScrollableTabRow(
                    selectedTabIndex = section,
                    containerColor = NexoraBlack,
                    contentColor = NexoraGreen,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[section]),
                            color = NexoraGreen
                        )
                    },
                    divider = {}
                ) {
                    Tab(selected = section == 0, onClick = { section = 0 }) {
                        Text("Usuários", modifier = Modifier.padding(16.dp), color = if (section == 0) NexoraGreen else NexoraMuted, fontWeight = FontWeight.Bold)
                    }
                    Tab(selected = section == 1, onClick = { section = 1 }) {
                        Text("Solicitações", modifier = Modifier.padding(16.dp), color = if (section == 1) NexoraGreen else NexoraMuted, fontWeight = FontWeight.Bold)
                    }
                    Tab(selected = section == 2, onClick = { section = 2 }) {
                        Text("Apoios", modifier = Modifier.padding(16.dp), color = if (section == 2) NexoraGreen else NexoraMuted, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                AdminFilterPanel(
                    query = query,
                    onQueryChange = { query = it },
                    statusFilter = statusFilter,
                    onStatusChange = { statusFilter = it },
                    statusOptions = statusOptions,
                    receiptFilter = receiptFilter,
                    onReceiptChange = { receiptFilter = it },
                    fromDate = fromDate,
                    onFromDateChange = { fromDate = it },
                    toDate = toDate,
                    onToDateChange = { toDate = it },
                    showReceipt = section == 2,
                )
            }
            when (section) {
                0 -> {
                    if (filteredUsers.isEmpty()) item { EmptyState("Sem usuários", "Nenhum usuário encontrado com os filtros atuais") }
                    items(filteredUsers, key = { it.id }) { user ->
                        AdminUserCard(user, viewModel, onDetails = { selectedUser = user })
                    }
                }
                1 -> {
                    if (filteredRequests.isEmpty()) item { EmptyState("Sem solicitações", "Nenhuma solicitação encontrada com os filtros atuais") }
                    items(filteredRequests, key = { it.id }) { request ->
                        AdminRequestCard(request, viewModel, onDetails = { selectedRequest = request })
                    }
                }
                2 -> {
                    if (filteredContributions.isEmpty()) item { EmptyState("Sem apoios", "Nenhum apoio encontrado com os filtros atuais") }
                    items(filteredContributions, key = { it.id }) { contribution ->
                        AdminContributionCard(contribution, viewModel, onDetails = { selectedContribution = contribution })
                    }
                }
            }
    }

    selectedUser?.let { user ->
        AdminUserDetailDialog(user = user, onDismiss = { selectedUser = null })
    }
    selectedRequest?.let { request ->
        AdminRequestDetailDialog(
            request = request,
            contributions = state.adminContributions.filter { it.requestPublicCode == request.publicCode },
            onContributionSelected = { selectedContribution = it },
            onDismiss = { selectedRequest = null },
        )
    }
    selectedContribution?.let { contribution ->
        AdminContributionDetailDialog(contribution = contribution, onDismiss = { selectedContribution = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminFilterPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    statusFilter: String,
    onStatusChange: (String) -> Unit,
    statusOptions: List<String>,
    receiptFilter: String,
    onReceiptChange: (String) -> Unit,
    fromDate: String,
    onFromDateChange: (String) -> Unit,
    toDate: String,
    onToDateChange: (String) -> Unit,
    showReceipt: Boolean,
) {
    NexoraPanel {
        NexoraInput("Buscar por nome, CPF, Pix, ID ou código", query, onQueryChange)
        Spacer(Modifier.height(12.dp))
        Text("Status", color = NexoraMuted, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            statusOptions.take(4).forEach { status ->
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { onStatusChange(status) },
                    label = { Text(if (status == "ALL") "Todos" else statusLabel(status), maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NexoraDarkGreen,
                        selectedLabelColor = NexoraGreen,
                        labelColor = NexoraMuted,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = statusFilter == status,
                        borderColor = Color(0xFF282828),
                        selectedBorderColor = NexoraGreen,
                    ),
                )
            }
        }
        if (statusOptions.size > 4) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                statusOptions.drop(4).forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { onStatusChange(status) },
                        label = { Text(statusLabel(status), maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NexoraDarkGreen,
                            selectedLabelColor = NexoraGreen,
                            labelColor = NexoraMuted,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = statusFilter == status,
                            borderColor = Color(0xFF282828),
                            selectedBorderColor = NexoraGreen,
                        ),
                    )
                }
            }
        }
        if (showReceipt) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("ALL" to "Todos", "complete" to "Completos", "missing" to "Pendentes").forEach { (value, label) ->
                    FilterChip(
                        selected = receiptFilter == value,
                        onClick = { onReceiptChange(value) },
                        label = { Text(label, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NexoraDarkGreen,
                            selectedLabelColor = NexoraGreen,
                            labelColor = NexoraMuted,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = receiptFilter == value,
                            borderColor = Color(0xFF282828),
                            selectedBorderColor = NexoraGreen,
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            NexoraInput("De (AAAA-MM-DD)", fromDate, onFromDateChange, modifier = Modifier.weight(1f))
            NexoraInput("Até (AAAA-MM-DD)", toDate, onToDateChange, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DetailAction(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, NexoraGreen),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoraGreen),
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier.height(46.dp),
    ) {
        Icon(Icons.Filled.Visibility, contentDescription = null, tint = NexoraGreen, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Detalhes", color = NexoraGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun AdminUserDetailDialog(user: AdminUser, onDismiss: () -> Unit) {
    DetailDialog(title = user.name, subtitle = "Dados completos do usuário", onDismiss = onDismiss) {
        DetailLine("Public ID", user.publicId)
        DetailLine("E-mail", user.email)
        DetailLine("CPF", user.cpf)
        DetailLine("Chave Pix", user.pixKey)
        DetailLine("Status", statusLabel(user.status))
        DetailLine("Função", user.role)
        DetailLine("Nível", "${user.level}")
        DetailLine("XP", "${user.xp}")
        DetailLine("Buff", formatBuff(user.buffBps))
        DetailLine("Limite", formatMoney(user.supportLimitCents))
        DetailLine("Taxa acumulada", "${formatMoney(user.adminFeeDueCents)} / ${formatMoney(user.adminFeeLimitCents)}")
        DetailLine("Pix do admin", user.adminPixKey ?: "-")
        DetailLine("Convite", user.inviteCode)
        DetailLine("Convidado por", user.invitedByPublicId ?: "-")
        DetailLine("Convidados", "${user.invitedCount}")
        DetailLine("Criado em", formatTimestamp(user.createdAt))
    }
}

@Composable
private fun AdminRequestDetailDialog(
    request: AdminSupportRequest,
    contributions: List<AdminContribution>,
    onContributionSelected: (AdminContribution) -> Unit,
    onDismiss: () -> Unit,
) {
    DetailDialog(title = request.publicCode, subtitle = "Detalhes da solicitação", onDismiss = onDismiss) {
        DetailLine("Solicitante", request.requesterName)
        DetailLine("Public ID", request.requesterPublicId)
        DetailLine("E-mail", request.requesterEmail)
        DetailLine("CPF", request.requesterCpf)
        DetailLine("Chave Pix", request.requesterPixKey)
        DetailLine("Status", statusLabel(request.status))
        DetailLine("Valor", "${formatMoney(request.fundedCents)} / ${formatMoney(request.amountCents)}")
        DetailLine("Taxa administrativa", formatMoney(request.adminFeeCents))
        DetailLine("Prazo", "${request.dueDays} dias")
        DetailLine("Criada em", formatTimestamp(request.createdAt))
        DetailLine("Descrição", request.description ?: "-")
        Spacer(Modifier.height(10.dp))
        Text("Apoios vinculados", color = NexoraGreen, fontWeight = FontWeight.Bold)
        if (contributions.isEmpty()) {
            Text("Nenhum apoio para esta solicitação.", color = NexoraMuted, fontSize = 13.sp)
        } else {
            contributions.forEach { contribution ->
                TextButton(onClick = { onContributionSelected(contribution) }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${contribution.donorPublicId} -> ${contribution.receiverPublicId} · ${formatMoney(contribution.amountCents)} · ${statusLabel(contribution.status)}",
                        color = NexoraText,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminContributionDetailDialog(contribution: AdminContribution, onDismiss: () -> Unit) {
    DetailDialog(title = contribution.requestPublicCode, subtitle = "Detalhes do apoio Pix", onDismiss = onDismiss) {
        DetailLine("Apoio", contribution.id)
        DetailLine("Solicitação", "${contribution.requestPublicCode} (${statusLabel(contribution.requestStatus)})")
        DetailLine("Valor", formatMoney(contribution.amountCents))
        DetailLine("ID da transação", contribution.transactionId ?: "-")
        DetailLine("Status", statusLabel(contribution.status))
        DetailLine("Criado em", formatTimestamp(contribution.createdAt))
        DetailLine("Doador", "${contribution.donorName.ifBlank { contribution.donorPublicId }} · ${contribution.donorEmail}")
        DetailLine("Recebedor", "${contribution.receiverName.ifBlank { contribution.receiverPublicId }} · ${contribution.receiverEmail}")
        Spacer(Modifier.height(12.dp))
        ReceiptPreview(
            title = "Foto enviada por quem pagou",
            date = contribution.senderReceiptDate,
            submittedAt = contribution.senderReceiptSubmittedAt,
            hash = contribution.senderReceiptHash,
            imageBase64 = contribution.senderReceiptImageBase64,
        )
        Spacer(Modifier.height(12.dp))
        ReceiptPreview(
            title = "Foto enviada por quem recebeu",
            date = contribution.receiverReceiptDate,
            submittedAt = contribution.receiverReceiptSubmittedAt,
            hash = contribution.receiverReceiptHash,
            imageBase64 = contribution.receiverReceiptImageBase64,
        )
    }
}

@Composable
private fun DetailDialog(title: String, subtitle: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = NexoraGreen, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = NexoraCard,
        title = {
            Column {
                Text(title, color = NexoraText, fontWeight = FontWeight.Black)
                Text(subtitle, color = NexoraMuted, fontSize = 13.sp)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), content = content)
        },
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = NexoraMuted, fontSize = 12.sp)
        SelectionContainer {
            Text(value, color = NexoraText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReceiptPreview(title: String, date: String?, submittedAt: Long?, hash: String?, imageBase64: String?) {
    NexoraPanel {
        Text(title, color = NexoraGreen, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        val bitmap = remember(imageBase64) {
            imageBase64?.let {
                runCatching {
                    val bytes = Base64.decode(it, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }.getOrNull()
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(220.dp).background(NexoraField, RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(140.dp).background(NexoraField, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("Sem foto anexada", color = NexoraMuted)
            }
        }
        Spacer(Modifier.height(8.dp))
        DetailLine("Data", date ?: "-")
        DetailLine("Enviado em", submittedAt?.let { formatTimestamp(it) } ?: "-")
        DetailLine("Hash", hash ?: "-")
    }
}

@Composable
private fun HeaderRow(profile: Profile, refreshing: Boolean, onRefresh: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            NexoraLogo(compact = true)
            IconButton(
                onClick = onRefresh,
                enabled = !refreshing,
            ) {
                if (refreshing) {
                    CircularProgressIndicator(color = NexoraGreen, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = "Atualizar", tint = NexoraMuted)
                }
            }
        }
        Text("Karma que conecta pessoas", color = NexoraMuted, fontSize = 14.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NexoraCard, RoundedCornerShape(24.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Lv ${profile.level}", color = NexoraGreen, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(profile.name, color = NexoraText, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (refreshing) Text("Atualizando", color = NexoraGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ScreenTitle(title: String, subtitle: String, onRefresh: (() -> Unit)? = null, refreshing: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(title, color = NexoraText, fontSize = 32.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = NexoraMuted, fontSize = 18.sp)
        }
        onRefresh?.let {
            TextButton(
                onClick = it,
                enabled = !refreshing,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                if (refreshing) {
                    CircularProgressIndicator(color = NexoraGreen, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Atualizando", color = NexoraGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = "Atualizar", tint = NexoraGreen)
                }
            }
        }
    }
}

@Composable
private fun NexoraLogo(compact: Boolean = false) {
    Column(horizontalAlignment = if (compact) Alignment.Start else Alignment.CenterHorizontally) {
        Text(
            "NEXORA",
            color = NexoraGreen,
            fontWeight = FontWeight.Black,
            fontSize = if (compact) 28.sp else 44.sp,
            letterSpacing = if (compact) 2.sp else 6.sp,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.shadow(10.dp, RoundedCornerShape(1.dp), ambientColor = NexoraGreen, spotColor = NexoraGreen),
        )
        if (!compact) {
            Text("Karma que conecta pessoas", color = Color(0xFFB6B6BD), fontStyle = FontStyle.Italic, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun NexoraPanel(
    modifier: Modifier = Modifier,
    border: Boolean = false,
    background: Color = NexoraCard,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        border = if (border) BorderStroke(1.dp, NexoraGreen.copy(alpha = 0.8f)) else BorderStroke(1.dp, Color(0xFF252525)),
    ) {
        Column(Modifier.padding(22.dp), content = content)
    }
}

@Composable
private fun NexoraInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    readOnly: Boolean = false,
    enabled: Boolean = true,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val activeAccent = if (isError) NexoraRed else NexoraGreen
    val enabledField = enabled && !readOnly

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        enabled = enabledField,
        readOnly = readOnly,
        isError = isError,
        visualTransformation = when {
            password && !passwordVisible -> PasswordVisualTransformation()
            visualTransformation != VisualTransformation.None -> visualTransformation
            else -> VisualTransformation.None
        },
        trailingIcon = if (password) {
            {
                val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(icon, contentDescription = "Toggle password visibility", tint = NexoraMuted)
                }
            }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = NexoraText,
            unfocusedTextColor = NexoraText,
            disabledTextColor = NexoraText,
            focusedBorderColor = activeAccent,
            unfocusedBorderColor = if (isError) NexoraRed else Color(0xFF282828),
            disabledBorderColor = if (isError) NexoraRed else Color(0xFF282828),
            focusedLabelColor = activeAccent,
            unfocusedLabelColor = if (isError) NexoraRed else NexoraMuted,
            disabledLabelColor = if (isError) NexoraRed else NexoraMuted,
            cursorColor = activeAccent,
            focusedContainerColor = NexoraField,
            unfocusedContainerColor = NexoraField,
            disabledContainerColor = NexoraField,
        ),
    )
}

private class CpfVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }
        val out = StringBuilder()
        for (i in digits.indices) {
            out.append(digits[i])
            if (i == 2 || i == 5) out.append(".")
            if (i == 8) out.append("-")
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset + 1
                if (offset <= 8) return offset + 2
                if (offset <= 11) return offset + 3
                return 14
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 7) return offset - 1
                if (offset <= 11) return offset - 2
                if (offset <= 14) return offset - 3
                return 11
            }
        }

        return TransformedText(AnnotatedString(out.toString()), offsetMapping)
    }
}

@Composable
private fun NexoraButton(text: String, loading: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = Modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NexoraGreen, contentColor = NexoraBlack),
    ) {
        if (loading) {
            CircularProgressIndicator(color = NexoraBlack, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text("AGUARDE", fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 16.sp, maxLines = 1)
        } else {
            Text(text, fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 16.sp, maxLines = 1)
        }
    }
}

@Composable
private fun StatText(label: String, value: String, color: Color) {
    Column {
        Text(label, color = NexoraMuted, fontSize = 14.sp, letterSpacing = 2.sp)
        Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val valueSize = when {
        value.length >= 12 -> 18.sp
        value.length >= 9 -> 20.sp
        value.length >= 6 -> 23.sp
        else -> 28.sp
    }
    Card(
        modifier = modifier.heightIn(min = 112.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = NexoraCard),
        border = BorderStroke(1.dp, Color(0xFF242424)),
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = NexoraMuted, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 1.sp, maxLines = 2)
            Text(
                value,
                color = if (highlight) NexoraGreen else NexoraText,
                fontSize = valueSize,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun RoadmapCard(dashboard: Dashboard?) {
    NexoraPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Expansão", color = NexoraGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Etapa ${dashboard?.roadmapStep ?: 1}", color = NexoraText, fontWeight = FontWeight.Bold)
        }
        Text(
            "Capacidade atual: ${dashboard?.roadmapCapacity ?: 20} participantes",
            color = NexoraMuted,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = NexoraGreen, modifier = Modifier.size(54.dp))
        Text(title, color = NexoraText, fontSize = 26.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 22.dp))
        Text(subtitle, color = NexoraMuted, fontSize = 17.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CommunityRequestCard(request: SupportRequest, onSupport: (Long) -> Unit, onError: (String) -> Unit) {
    var amount by remember(request.id) { mutableStateOf("") }
    NexoraPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(request.publicCode, color = NexoraText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("${request.requesterPublicId} · Lv ${request.requesterLevel}", color = NexoraMuted)
                Text(formatTimestamp(request.createdAt), color = NexoraMuted, fontSize = 12.sp)
            }
            StatusPill(request.status)
        }
        Spacer(Modifier.height(12.dp))
        ProgressLine(request.fundedCents, request.amountCents)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LabelValue("Total", formatMoney(request.amountCents), compact = true)
            LabelValue("Falta", formatMoney((request.amountCents - request.fundedCents).coerceAtLeast(0)), compact = true)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NexoraInput("Valor", amount, { amount = it }, Modifier.weight(1f), "R$ 0,00", KeyboardType.Decimal)
            Button(
                onClick = {
                    val cents = parseMoneyToCents(amount)
                    if (cents == null || cents <= 0) {
                        onError("Informe um valor valido para apoiar.")
                    } else {
                        onSupport(cents)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NexoraGreen, contentColor = NexoraBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(56.dp),
            ) {
                Text("APOIAR", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun CompactRequestCard(request: SupportRequest) {
    NexoraPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(formatMoney(request.amountCents), color = NexoraText, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text(request.publicCode, color = NexoraMuted)
                Text(formatTimestamp(request.createdAt), color = NexoraMuted, fontSize = 12.sp)
            }
            StatusPill(request.status)
        }
        Spacer(Modifier.height(12.dp))
        ProgressLine(request.fundedCents, request.amountCents)
    }
}

@Composable
private fun ContributionHistoryCard(contribution: ContributionHistory, viewModel: NexoraViewModel) {
    val neededSide = when {
        contribution.direction == "SENT" && !contribution.hasSenderReceipt -> "SENDER"
        contribution.direction == "RECEIVED" && !contribution.hasReceiverReceipt -> "RECEIVER"
        else -> null
    }
    var showUpload by rememberSaveable(contribution.id, neededSide) { mutableStateOf(false) }
    NexoraPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(formatMoney(contribution.amountCents), color = NexoraText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(contribution.requestPublicCode, color = NexoraMuted)
                Text(
                    if (contribution.direction == "SENT") "Enviado para ${contribution.receiverPublicId}" else "Recebido de ${contribution.donorPublicId}",
                    color = NexoraMuted,
                )
                Text(formatTimestamp(contribution.createdAt), color = NexoraMuted, fontSize = 12.sp)
            }
            StatusPill(contribution.status)
        }
        Spacer(Modifier.height(10.dp))
        LabelValue("ID da transação", contribution.transactionId ?: "aguardando comprovante")
        Spacer(Modifier.height(10.dp))
        Text(
            "Envio: ${if (contribution.hasSenderReceipt) "foto anexada" else "pendente"} · Recebimento: ${if (contribution.hasReceiverReceipt) "foto anexada" else "pendente"}",
            color = if (contribution.evidenceComplete) NexoraGreen else NexoraMuted,
            fontSize = 14.sp,
        )
        neededSide?.let { side ->
            if (contribution.status == "PENDING_ADMIN") {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showUpload = !showUpload },
                    border = BorderStroke(1.dp, NexoraGreen),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(if (showUpload) "Ocultar comprovante" else "Anexar comprovante", color = NexoraGreen, fontWeight = FontWeight.Bold)
                }
                if (showUpload) {
                    Spacer(Modifier.height(10.dp))
                    ReceiptUploadControls(
                        contributionId = contribution.id,
                        amountCents = contribution.amountCents,
                        side = side,
                        loading = viewModel.state.loading,
                        onSubmit = viewModel::submitReceipt,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceiptUploadControls(
    contributionId: String,
    amountCents: Long,
    side: String,
    loading: Boolean,
    onSubmit: (String, Long, String, ReceiptUpload, String, String) -> Unit,
) {
    val context = LocalContext.current
    var transactionId by rememberSaveable(contributionId, side) { mutableStateOf("") }
    var receiptDate by rememberSaveable(contributionId, side) { mutableStateOf(LocalDate.now().toString()) }
    var upload by remember(contributionId, side) { mutableStateOf<ReceiptUpload?>(null) }
    var localError by remember(contributionId, side) { mutableStateOf<String?>(null) }
    var transactionIdError by remember(contributionId, side) { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { receiptUploadFromUri(context, uri) }
                .onSuccess {
                    upload = it
                    localError = null
                    transactionIdError = false
                }
                .onFailure { localError = it.message ?: "Não foi possível anexar a foto." }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NexoraInput("ID da transação Pix", transactionId, { transactionId = it; transactionIdError = false }, isError = transactionIdError)
        NexoraInput("Data do comprovante", receiptDate, { receiptDate = it }, readOnly = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { launcher.launch("image/*") },
                enabled = !loading,
                border = BorderStroke(1.dp, NexoraGreen),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(if (upload == null) "Anexar foto" else "Foto pronta", color = NexoraGreen, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    val currentUpload = upload
                    when {
                        transactionId.isBlank() -> {
                            transactionIdError = true
                            localError = "Informe o ID da transação Pix."
                        }
                        currentUpload == null -> localError = "Anexe a foto do comprovante."
                        else -> {
                            transactionIdError = false
                            localError = null
                            onSubmit(contributionId, amountCents, transactionId, currentUpload, receiptDate, side)
                        }
                    }
                },
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = NexoraGreen, contentColor = NexoraBlack),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("ENVIAR", fontWeight = FontWeight.Black)
            }
        }
        localError?.let { Text(it, color = NexoraRed, fontSize = 13.sp) }
    }
}

@Composable
private fun ProgressLine(current: Long, total: Long) {
    val progress = if (total <= 0) 0f else (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(8.dp),
        color = NexoraGreen,
        trackColor = Color(0xFF050505),
    )
}

@Composable
private fun StatusPill(status: String) {
    val color = when (status) {
        "APPROVED", "OPEN", "FUNDED", "RETURNED", "CONFIRMED" -> NexoraGreen
        "BLOCKED", "REJECTED" -> NexoraRed
        else -> NexoraMuted
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.13f), RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(statusLabel(status), color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

private fun statusLabel(status: String): String = when (status) {
    "PENDING_REVIEW" -> "aguardando admin"
    "PENDING_ADMIN" -> "aguardando admin"
    "APPROVED" -> "aprovado"
    "BLOCKED" -> "bloqueado"
    "OPEN" -> "ativo"
    "FUNDED" -> "completo"
    "RETURNED" -> "concluído"
    "CONFIRMED" -> "validado"
    "REJECTED" -> "recusado"
    else -> status.lowercase()
}

@Composable
private fun LabelValue(label: String, value: String, compact: Boolean = false, onCopy: (() -> Unit)? = null) {
    Column(
        modifier = if (onCopy != null) Modifier.clickable { onCopy() } else Modifier
    ) {
        Text(label, color = NexoraMuted, fontSize = if (compact) 12.sp else 15.sp, letterSpacing = 1.sp)
        Text(value, color = NexoraText, fontSize = if (compact) 16.sp else 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AdminUserCard(user: AdminUser, viewModel: NexoraViewModel, onDetails: () -> Unit) {
    NexoraPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(user.name, color = NexoraText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("${user.publicId} · ${user.email}", color = NexoraMuted)
                Text("CPF ${user.cpf} · Pix ${user.pixKey}", color = NexoraMuted, fontSize = 13.sp)
                Text("Lv ${user.level} · XP ${user.xp} · Buff ${formatBuff(user.buffBps)}", color = NexoraMuted)
                Text("Criado: ${formatTimestamp(user.createdAt)}", color = NexoraMuted, fontSize = 12.sp)
                if (user.adminFeeDueCents > 0) Text("Taxa: ${formatMoney(user.adminFeeDueCents)}", color = NexoraRed)
            }
            StatusPill(user.status)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailAction(onDetails)
            SmallAction("Aprovar usuário", NexoraGreen, loading = viewModel.state.actionInProgress == "/admin/users/${user.id}/approve") { viewModel.adminApproveUser(user.id) }
            SmallAction("Bloquear usuário", NexoraRed, loading = viewModel.state.actionInProgress == "/admin/users/${user.id}/block") { viewModel.adminBlockUser(user.id) }
            if (user.adminFeeDueCents > 0) SmallAction("Baixar taxa", NexoraMuted, loading = viewModel.state.actionInProgress == "user-fee-${user.id}") { viewModel.adminConfirmFee(user.id) }
        }
    }
}

@Composable
private fun AdminRequestCard(request: AdminSupportRequest, viewModel: NexoraViewModel, onDetails: () -> Unit) {
    NexoraPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(request.requesterName, color = NexoraText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("${request.publicCode} · ${request.requesterPublicId}", color = NexoraMuted)
                Text(request.requesterEmail, color = NexoraMuted, fontSize = 13.sp)
                Text("${formatMoney(request.amountCents)} · taxa ${formatMoney(request.adminFeeCents)}", color = NexoraMuted)
                Text("Criado: ${formatTimestamp(request.createdAt)}", color = NexoraMuted, fontSize = 12.sp)
            }
            StatusPill(request.status)
        }
        request.description?.let {
            Text(it, color = Color(0xFFB8B8B8), modifier = Modifier.padding(top = 10.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailAction(onDetails)
            if (request.status == "PENDING_ADMIN") {
                SmallAction("Aprovar solicitação", NexoraGreen, loading = viewModel.state.actionInProgress == "/admin/support-requests/${request.id}/approve") { viewModel.adminApproveRequest(request.id) }
                SmallAction("Recusar solicitação", NexoraRed, loading = viewModel.state.actionInProgress == "/admin/support-requests/${request.id}/reject") { viewModel.adminRejectRequest(request.id) }
            }
            if (request.status == "FUNDED") {
                SmallAction("Validar retorno", NexoraGreen, loading = viewModel.state.actionInProgress == "/admin/support-requests/${request.id}/confirm-return") { viewModel.adminConfirmReturn(request.id) }
            }
        }
    }
}

@Composable
private fun AdminContributionCard(contribution: AdminContribution, viewModel: NexoraViewModel, onDetails: () -> Unit) {
    NexoraPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(formatMoney(contribution.amountCents), color = NexoraText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("${contribution.requestPublicCode} · ${contribution.donorPublicId} -> ${contribution.receiverPublicId}", color = NexoraMuted)
                Text("${contribution.donorName.ifBlank { "Doador" }} -> ${contribution.receiverName.ifBlank { "Recebedor" }}", color = NexoraMuted, fontSize = 13.sp)
                Text("ID: ${contribution.transactionId ?: "pendente"}", color = NexoraMuted)
                Text("Data: ${formatTimestamp(contribution.createdAt)}", color = NexoraMuted, fontSize = 12.sp)
                Text(
                    "Envio: ${if (contribution.hasSenderReceipt) "foto" else "pendente"} · Recebimento: ${if (contribution.hasReceiverReceipt) "foto" else "pendente"}",
                    color = if (contribution.evidenceComplete) NexoraGreen else NexoraMuted,
                )
            }
            StatusPill(contribution.status)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailAction(onDetails)
        }
        if (contribution.status == "PENDING_ADMIN") {
            Spacer(Modifier.height(12.dp))
            if (contribution.evidenceComplete) {
                SmallAction("Validar Pix", NexoraGreen, loading = viewModel.state.actionInProgress == "contribution-confirm-${contribution.id}") { viewModel.adminConfirmContribution(contribution.id) }
            } else {
                Text("Aguarde as duas fotos antes de validar.", color = NexoraMuted, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SmallAction(text: String, color: Color, loading: Boolean = false, onClick: () -> Unit) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onClick()
                }) {
                    Text("Confirmar", color = NexoraGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) {
                    Text("Cancelar", color = NexoraMuted, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = NexoraCard,
            title = { Text("Confirmar acao", color = NexoraText, fontWeight = FontWeight.Black) },
            text = { Text(text, color = NexoraMuted) },
        )
    }
    OutlinedButton(
        onClick = { confirm = true },
        enabled = !loading,
        border = BorderStroke(1.dp, color),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(width = 58.dp, height = 46.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(color = color, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
            when {
                text.contains("Aprovar", ignoreCase = true) || text.contains("Validar", ignoreCase = true) ->
                    Icon(Icons.Filled.CheckCircle, contentDescription = text, tint = color)
                text.contains("Baixar", ignoreCase = true) ->
                    Icon(Icons.Filled.ContentCopy, contentDescription = text, tint = color)
                else -> Icon(Icons.Filled.Warning, contentDescription = text, tint = color)
            }
        }
    }
}

@Composable
private fun MessageStrip(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .background(Color(0xFF111111), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = NexoraGreen)
        Spacer(Modifier.width(10.dp))
        Text(message, color = NexoraText, fontSize = 14.sp)
    }
}

@Composable
private fun NoticeSnackbar(message: String, isError: Boolean) {
    val accent = if (isError) NexoraRed else NexoraGreen
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101010)),
        border = BorderStroke(1.dp, accent),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = accent)
            Spacer(Modifier.width(12.dp))
            Text(message, color = NexoraText, fontSize = 15.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun LoadingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.46f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = NexoraCard),
            border = BorderStroke(1.dp, NexoraGreen),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(color = NexoraGreen, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                Spacer(Modifier.width(14.dp))
                Text(message, color = NexoraText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PixDialog(
    instructions: List<PixInstruction>,
    loading: Boolean,
    onSubmitReceipt: (String, Long, String, ReceiptUpload, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = NexoraGreen, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = NexoraCard,
        title = { Text("Instrução Pix", color = NexoraText, fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                instructions.forEachIndexed { index, instruction ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 16.dp), color = Color(0xFF252525))
                    LabelValue("Transferência", instruction.contributionId.take(8))
                    Spacer(Modifier.height(10.dp))
                    LabelValue("Referência", instruction.receiverIdentifier)
                    Spacer(Modifier.height(10.dp))
                    LabelValue("Valor", formatMoney(instruction.amountCents))
                    Spacer(Modifier.height(10.dp))
                    Text(instruction.message, color = NexoraMuted)
                    Spacer(Modifier.height(12.dp))
                    PixCodeBox(
                        code = instruction.pixCopyCode,
                        onCopy = {
                            clipboard.setText(AnnotatedString(instruction.pixCopyCode))
                            Toast.makeText(context, "Código Pix copiado!", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun PixCodeBox(code: String, onCopy: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NexoraField, RoundedCornerShape(8.dp))
            .padding(14.dp),
    ) {
        Text("Código Pix", color = NexoraMuted, fontSize = 13.sp, letterSpacing = 1.sp)
        SelectionContainer {
            Text(
                code.ifBlank { "Código indisponível" },
                color = NexoraText,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        OutlinedButton(
            onClick = onCopy,
            enabled = code.isNotBlank(),
            border = BorderStroke(1.dp, NexoraGreen),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = NexoraGreen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Copiar código", color = NexoraGreen, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val instant = Instant.ofEpochMilli(millis)
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
