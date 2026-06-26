package com.nexora.app

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.provider.MediaStore
import android.content.ContentValues
import android.os.Build
import java.io.OutputStream
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ZoomIn
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.animation.core.*
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
private const val NexoraDownloadUrl = "https://nexora-web-mauve.vercel.app"
private const val NexoraContactEmail = "nexora@nexoraappbr.com"
private const val NexoraContactPhone = "+5511913463247"
private const val MinContributionCents = 500L

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
                    
                    DisposableEffect(lifecycle) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_START -> viewModel.onAppForeground()
                                Lifecycle.Event.ON_STOP -> viewModel.onAppBackground()
                                else -> Unit
                            }
                        }
                        lifecycle.addObserver(observer)
                        onDispose { lifecycle.removeObserver(observer) }
                    }

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
            .setTitle(uiText("biometricTitle"))
            .setSubtitle(uiText("biometricSubtitle"))
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
    val context = LocalContext.current
    val introPrefs = remember { context.getSharedPreferences("nexora_intro", 0) }
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarIsError by remember { mutableStateOf(false) }
    var showIntro by rememberSaveable { mutableStateOf(!introPrefs.getBoolean("dismissed", false)) }

    fun dismissIntro() {
        introPrefs.edit().putBoolean("dismissed", true).apply()
        showIntro = false
    }

    fun openDownloadPage() {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(NexoraDownloadUrl)))
    }

    SideEffect {
        NexoraLanguageStore.current = state.language
    }

    LaunchedEffect(state.message, state.messageIsError) {
        state.message?.let { message ->
            snackbarIsError = state.messageIsError
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(runtimeText(message, state.language))
            viewModel.clearMessage()
        }
    }

    Box(Modifier.fillMaxSize().background(NexoraBlack)) {
        if (state.profile == null) {
            AuthScreen(
                state = state,
                viewModel = viewModel,
                biometricAvailable = biometricAvailable,
                onBiometricLogin = onBiometricLogin,
                onShowIntro = { showIntro = true },
            )
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
            LoadingOverlay(runtimeText(state.loadingAction ?: uiText("wait", state.language), state.language))
        }

        if (state.sessionLocked) {
            SessionLockOverlay(
                countdown = state.lockCountdown,
                biometricAvailable = biometricAvailable,
                onUnlock = onBiometricLogin,
                onLogout = viewModel::logout
            )
        }

        if (showIntro && !state.sessionLocked) {
            TutorialDialog(
                language = state.language,
                onLanguageSelect = viewModel::setLanguage,
                onDownload = ::openDownloadPage,
                onDismiss = ::dismissIntro,
            )
        }
    }
}

@Composable
private fun SessionLockOverlay(
    countdown: Int,
    biometricAvailable: Boolean,
    onUnlock: () -> Unit,
    onLogout: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "countdown")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = NexoraGreen,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Sessão Suspensa",
                color = NexoraText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                uiText("sessionLockedMessage"),
                color = NexoraMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(Modifier.height(48.dp))
            
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { countdown / 10f },
                    modifier = Modifier.size(100.dp),
                    color = NexoraGreen,
                    strokeWidth = 8.dp,
                    trackColor = NexoraDarkGreen
                )
                Text(
                    "$countdown",
                    color = NexoraText.copy(alpha = alpha),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(48.dp))
            
            if (biometricAvailable) {
                NexoraButton(
                    text = uiText("unlock"),
                    onClick = onUnlock
                )
            }
            
            TextButton(
                onClick = onLogout,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(uiText("logoutNow"), color = NexoraRed)
            }
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
    onShowIntro: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(AuthMode.LOGIN) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var cpf by rememberSaveable { mutableStateOf("") }
    var birthdate by rememberSaveable { mutableStateOf("") }
    var birthdateField by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
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
    val language = state.language

    LaunchedEffect(state.registrationEmail) {
        state.registrationEmail?.let {
            mode = AuthMode.VERIFY
            email = it
            viewModel.clearRegistrationEmail()
        }
    }

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

    fun validBirthdate(): Boolean {
        val parsed = parseBirthdateInput(birthdate)
            ?: return fail("birthdate", uiText("birthdateInvalid", language))
        val today = LocalDate.now()
        return when {
            parsed.isAfter(today.minusYears(13)) -> fail("birthdate", uiText("minimumAge", language))
            parsed.isBefore(today.minusYears(120)) -> fail("birthdate", uiText("birthdateInvalid", language))
            else -> true
        }
    }

    fun validateAuthAction(): Boolean = when (mode) {
        AuthMode.LOGIN -> when {
            cpf.isBlank() -> fail("identifier", uiText("identifierRequired", language))
            password.isBlank() -> fail("password", uiText("passwordRequired", language))
            else -> {
                invalidFields = emptySet()
                true
            }
        }
        AuthMode.REGISTER -> when {
            name.trim().length < 2 -> fail("name", uiText("nameRequired", language))
            email.isBlank() || !email.contains("@") -> fail("email", uiText("emailInvalid", language))
            cpf.filter(Char::isDigit).length != 11 -> fail("cpf", uiText("cpfRequired", language))
            birthdate.isBlank() -> fail("birthdate", uiText("birthdateRequired", language))
            !validBirthdate() -> false
            pixKey.isBlank() -> fail("pixKey", uiText("pixRequired", language))
            !isRandomPixKey(pixKey) -> fail("pixKey", uiText("pixRandomRequired", language))
            password.length < 8 -> fail("password", uiText("passwordMin", language))
            else -> {
                invalidFields = emptySet()
                true
            }
        }
        AuthMode.VERIFY -> when {
            email.isBlank() || !email.contains("@") -> fail("email", uiText("registrationEmailRequired", language))
            code.filter(Char::isDigit).length != 6 -> fail("code", uiText("sixDigitCodeRequired", language))
            else -> {
                invalidFields = emptySet()
                true
            }
        }
        AuthMode.RECOVER_SEND -> when {
            email.isBlank() || !email.contains("@") -> fail("email", uiText("accountEmailRequired", language))
            else -> {
                invalidFields = emptySet()
                true
            }
        }
        AuthMode.RECOVER_RESET -> when {
            email.isBlank() || !email.contains("@") -> fail("email", uiText("accountEmailRequired", language))
            code.filter(Char::isDigit).length != 6 -> fail("code", uiText("sixDigitCodeRequired", language))
            newPassword.length < 8 -> fail("newPassword", uiText("newPasswordMin", language))
            confirmNewPassword != newPassword -> fail("confirmNewPassword", uiText("passwordsDontMatch", language))
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
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.align(Alignment.TopCenter)) {
                NexoraLogo()
            }
            Box(Modifier.align(Alignment.TopEnd)) {
                LanguageSelector(language = language, onSelect = viewModel::setLanguage)
            }
        }
        Spacer(Modifier.height(48.dp))
        
        Text(
            text = when (mode) {
                AuthMode.LOGIN -> uiText("loginTitle", language)
                AuthMode.REGISTER -> uiText("registerTitle", language)
                AuthMode.VERIFY -> uiText("verifyTitle", language)
                AuthMode.RECOVER_SEND -> uiText("recoverTitle", language)
                AuthMode.RECOVER_RESET -> uiText("newPasswordTitle", language)
            },
            color = NexoraText,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )
        Text(
            text = when (mode) {
                AuthMode.LOGIN -> uiText("loginSubtitle", language)
                AuthMode.REGISTER -> uiText("registerSubtitle", language)
                AuthMode.VERIFY -> uiText("verifySubtitle", language)
                AuthMode.RECOVER_SEND -> uiText("recoverSubtitle", language)
                AuthMode.RECOVER_RESET -> if (code.length == 6) uiText("newPasswordSubtitle", language) else uiText("recoverCodeSubtitle", language)
            },
            color = NexoraMuted,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 32.dp),
            textAlign = TextAlign.Start,
        )
        NexoraPanel {
            if (mode == AuthMode.REGISTER) {
                NexoraInput(uiText("fullName", language), name, { name = it; clearInvalid("name") }, isError = "name" in invalidFields)
                Spacer(Modifier.height(16.dp))
                NexoraInput(uiText("email", language), email, { email = it; clearInvalid("email") }, keyboardType = KeyboardType.Email, isError = "email" in invalidFields)
                Spacer(Modifier.height(16.dp))
                NexoraInput("CPF", cpf, { if (it.length <= 11) cpf = it; clearInvalid("cpf") }, keyboardType = KeyboardType.Number, visualTransformation = CpfVisualTransformation(), isError = "cpf" in invalidFields)
                Spacer(Modifier.height(16.dp))
                NexoraInput(
                    uiText("birthdate", language),
                    birthdateField,
                    {
                        val formatted = formatBirthdateInput(it.text, it.selection.end)
                        birthdate = formatted.text
                        birthdateField = TextFieldValue(
                            text = formatted.text,
                            selection = TextRange(formatted.cursor),
                        )
                        clearInvalid("birthdate")
                    },
                    placeholder = uiText("datePlaceholder", language),
                    keyboardType = KeyboardType.Number,
                    isError = "birthdate" in invalidFields,
                )
                Spacer(Modifier.height(16.dp))
                NexoraInput(uiText("pixKey", language), pixKey, { pixKey = it.trim(); clearInvalid("pixKey") }, placeholder = uiText("pixPlaceholder", language), isError = "pixKey" in invalidFields)
                Spacer(Modifier.height(16.dp))
                NexoraInput(uiText("inviteCode", language), invite, { invite = it })
                Spacer(Modifier.height(16.dp))
            } else {
                when (mode) {
                    AuthMode.LOGIN -> {
                        NexoraInput(uiText("cpfOrEmail", language), cpf, { cpf = it; clearInvalid("identifier") }, isError = "identifier" in invalidFields)
                        Spacer(Modifier.height(16.dp))
                    }
                    AuthMode.VERIFY -> {
                        NexoraInput(uiText("email", language), email, { email = it; clearInvalid("email") }, keyboardType = KeyboardType.Email, isError = "email" in invalidFields)
                        Spacer(Modifier.height(16.dp))
                        NexoraInput(uiText("code", language), code, { code = it.filter(Char::isDigit).take(6); clearInvalid("code") }, keyboardType = KeyboardType.Number, isError = "code" in invalidFields)
                        Spacer(Modifier.height(16.dp))
                    }
                    AuthMode.RECOVER_SEND -> {
                        NexoraInput(uiText("email", language), email, { email = it; clearInvalid("email") }, keyboardType = KeyboardType.Email, isError = "email" in invalidFields)
                        Spacer(Modifier.height(16.dp))
                    }
                    else -> {
                        NexoraInput(uiText("email", language), email, { email = it; clearInvalid("email") }, keyboardType = KeyboardType.Email, isError = "email" in invalidFields)
                        Spacer(Modifier.height(16.dp))
                        NexoraInput(
                            uiText("code", language),
                            code,
                            {
                                code = it.filter(Char::isDigit).take(6)
                                clearInvalid("code")
                                if (code.length < 6) {
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
                                uiText("newPassword", language),
                                newPassword,
                                { newPassword = it; clearInvalid("newPassword") },
                                password = true,
                                isError = "newPassword" in invalidFields,
                            )
                            Spacer(Modifier.height(16.dp))
                            NexoraInput(
                                uiText("confirmNewPassword", language),
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
                    label = uiText("password", language),
                    value = password,
                    onValueChange = { password = it; clearInvalid("password") },
                    password = true,
                    isError = "password" in invalidFields,
                )
                Spacer(Modifier.height(32.dp))
            }

            NexoraButton(
                text = when (mode) {
                    AuthMode.LOGIN -> uiText("loginButton", language)
                    AuthMode.REGISTER -> uiText("registerButton", language)
                    AuthMode.VERIFY -> uiText("verifyButton", language)
                    AuthMode.RECOVER_SEND -> uiText("sendCodeButton", language)
                    AuthMode.RECOVER_RESET -> if (code.length == 6) uiText("updatePasswordButton", language) else uiText("enterCodeButton", language)
                },
                loading = state.loading,
                enabled = mode != AuthMode.RECOVER_RESET || code.length == 6,
                onClick = {
                    if (validateAuthAction()) {
                        when (mode) {
                            AuthMode.LOGIN -> viewModel.login(cpf, password)
                            AuthMode.REGISTER -> viewModel.register(
                                name,
                                email,
                                cpf,
                                birthdateInputToIso(birthdate) ?: birthdate,
                                pixKey,
                                password,
                                invite.takeIf { it.isNotBlank() },
                            )
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
                        AuthMode.LOGIN -> uiText("noAccount", language)
                        AuthMode.REGISTER -> uiText("hasAccount", language)
                        AuthMode.VERIFY -> uiText("noCode", language)
                        AuthMode.RECOVER_SEND -> uiText("rememberedPassword", language)
                        AuthMode.RECOVER_RESET -> uiText("passwordUpdatedQuestion", language)
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
                            AuthMode.LOGIN -> uiText("signUp", language)
                            AuthMode.REGISTER -> uiText("loginTitle", language)
                            AuthMode.VERIFY -> uiText("back", language)
                            AuthMode.RECOVER_SEND -> uiText("loginTitle", language)
                            AuthMode.RECOVER_RESET -> uiText("loginTitle", language)
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
                                !state.hasSavedSession -> uiText("biometricAfterLogin", language)
                                else -> uiText("biometricEnableDevice", language)
                            }
                            viewModel.showValidationError(reason)
                        }
                    },
                    enabled = !state.loading,
                    border = BorderStroke(1.dp, if (state.hasSavedSession && biometricAvailable) NexoraGreen else NexoraMuted),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text(uiText("biometricButton", language), color = if (state.hasSavedSession && biometricAvailable) NexoraGreen else NexoraMuted, fontWeight = FontWeight.Black)
                }
                Text(
                    text = if (state.hasSavedSession) {
                        uiText("biometricSavedHelp", language)
                    } else {
                        uiText("biometricFirstLoginHelp", language)
                    },
                    color = NexoraMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = { mode = AuthMode.VERIFY }) {
                        Text(uiText("verifyEmailLink", language), color = NexoraMuted)
                    }
                    TextButton(onClick = { mode = AuthMode.RECOVER_SEND }) {
                        Text(uiText("forgotPassword", language), color = NexoraMuted)
                    }
                }
            }

            if (mode == AuthMode.VERIFY) {
                TextButton(
                    enabled = !state.loading,
                    onClick = { viewModel.resendVerification(email) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(uiText("resendCodeEmail", language), color = NexoraGreen, fontWeight = FontWeight.Bold)
                }
            }

            if (mode == AuthMode.RECOVER_RESET) {
                TextButton(
                    enabled = !state.loading && email.contains("@"),
                    onClick = { viewModel.recoverPassword(email) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(uiText("resendCodeEmail", language), color = NexoraGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        if (showServerConfig) {
            NexoraInput(uiText("apiServer", language), server, { server = it; viewModel.updateBaseUrl(it) })
            Spacer(Modifier.height(8.dp))
        }

        TextButton(onClick = onShowIntro) {
            Text(uiText("tutorialFaq", language), color = NexoraGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        
        TextButton(onClick = { showServerConfig = !showServerConfig }) {
            Text(if (showServerConfig) uiText("hideConfig", language) else uiText("serverConfig", language), color = NexoraMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MainShell(state: NexoraUiState, viewModel: NexoraViewModel) {
    Scaffold(
        containerColor = NexoraBlack,
        bottomBar = { NexoraBottomBar(state, viewModel) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(NexoraBlack),
        ) {
            MainTabContent(state, viewModel)
        }
    }
}

@Composable
private fun MainTabContent(state: NexoraUiState, viewModel: NexoraViewModel) {
    when (state.tab) {
        MainTab.PAINEL -> DashboardScreen(state, viewModel)
        MainTab.COMUNIDADE -> CommunityScreen(state, viewModel)
        MainTab.SOLICITAR -> RequestScreen(state, viewModel)
        MainTab.RETORNOS -> RepaymentScreen(state, viewModel)
        MainTab.PERFIL -> ProfileScreen(state, viewModel)
        MainTab.ADMIN -> AdminScreen(state, viewModel)
    }
}

@Composable
private fun NexoraBottomBar(state: NexoraUiState, viewModel: NexoraViewModel) {
    val profile = state.profile
    val items = buildList {
        add(Triple(MainTab.PAINEL, uiText("tabPanel", state.language), Icons.Filled.Dashboard))
        add(Triple(MainTab.COMUNIDADE, uiText("tabCommunity", state.language), Icons.Filled.Groups))
        add(Triple(MainTab.SOLICITAR, uiText("tabRequest", state.language), Icons.Filled.Add))
        add(Triple(MainTab.RETORNOS, uiText("tabReturns", state.language), Icons.Filled.Replay))
        add(Triple(MainTab.PERFIL, uiText("tabProfile", state.language), Icons.Filled.Person))
        if (profile?.role in setOf("ADMIN", "SUPER_ADMIN")) add(Triple(MainTab.ADMIN, uiText("tabAdmin", state.language), Icons.Filled.AdminPanelSettings))
    }
    val tabOrder = items.map { it.first }

    Surface(
        color = Color(0xFF0A0A0A),
        contentColor = NexoraText,
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .pointerInput(tabOrder) {
                        detectTapGestures { offset ->
                            bottomTabForTap(tabOrder, size.width, offset.x)?.let(viewModel::setTab)
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { (tab, label, icon) ->
                    val selected = state.tab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(80.dp)
                            .semantics {
                                this.selected = selected
                                contentDescription = label
                                onClick {
                                    viewModel.setTab(tab)
                                    true
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (selected) NexoraGreen else NexoraMuted,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = label,
                                color = if (selected) NexoraGreen else NexoraMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Clip,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars),
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
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
            item {
                HeaderRow(
                    profile = profile,
                    refreshing = MainTab.PAINEL in state.refreshingTabs,
                    onRefresh = viewModel::refreshAll,
                    language = state.language,
                    onLanguageSelect = viewModel::setLanguage,
                )
            }
            if (state.repayments.summary.pendingCount > 0) {
                item {
                    NexoraPanel(
                        modifier = Modifier.clickable { viewModel.setTab(MainTab.RETORNOS) },
                        border = state.repayments.summary.overdueCount > 0,
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (state.repayments.summary.overdueCount > 0) uiText("overdueReturnsTitle", state.language) else uiText("pendingReturnsTitle", state.language),
                                    color = if (state.repayments.summary.overdueCount > 0) NexoraRed else NexoraGreen,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                )
                                Text(formatMoney(state.repayments.summary.pendingAmountCents), color = NexoraText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                state.repayments.summary.nextDueAt?.let { Text("${uiText("nextDue", state.language)}: ${formatTimestamp(it)}", color = NexoraMuted) }
                            }
                            Icon(Icons.Filled.Replay, contentDescription = null, tint = NexoraGreen)
                        }
                    }
                }
            }
            item {
                NexoraPanel(border = true) {
                    Text(uiText("communityLiquidity", state.language), color = NexoraMuted, fontSize = 14.sp, letterSpacing = 3.sp)
                    Text(
                        text = formatMoney(dashboard?.communityLiquidityCents ?: 0),
                        color = NexoraGreen,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    HorizontalDivider(Modifier.padding(vertical = 20.dp), color = Color(0xFF252525))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatText(uiText("inCirculation", state.language), formatMoney(dashboard?.inCirculationCents ?: 0), NexoraText)
                        StatText(uiText("completion", state.language), String.format(Locale.getDefault(), "%.1f%%", dashboard?.completionPercent ?: 100.0), NexoraGreen)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard(uiText("activeRequests", state.language), "${dashboard?.activeRequests ?: 0}", Modifier.weight(1f))
                    MetricCard(uiText("completedOperations", state.language), "${dashboard?.completedOperations ?: 0}", Modifier.weight(1f), highlight = true)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard(uiText("activeUsers", state.language), "${dashboard?.activeUsers ?: 0}", Modifier.weight(1f))
                    MetricCard(uiText("yourLimit", state.language), formatMoney(dashboard?.userLimitCents ?: profile.supportLimitCents), Modifier.weight(1f), highlight = true)
                }
            }
            item {
                NexoraPanel {
                    Text(uiText("howItWorks", state.language), color = NexoraGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        uiText("howItWorksBody", state.language),
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
    var historyFilter by rememberSaveable { mutableStateOf("ALL") }
    val filteredHistory = remember(state.contributionHistory, historyFilter) {
        filterContributionHistory(state.contributionHistory, historyFilter)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
            item {
                ScreenTitle(
                    uiText("communityTitle", state.language),
                    uiText("communitySubtitle", state.language),
                    viewModel::refreshCommunity,
                    refreshing = MainTab.COMUNIDADE in state.refreshingTabs,
                    language = state.language,
                    onLanguageSelect = viewModel::setLanguage,
                )
            }
            item {
                NexoraPanel(border = true) {
                    Text(uiText("communityPixOrder", state.language), color = NexoraGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NexoraInput(uiText("totalValue", state.language), batchAmount, { batchAmount = it }, Modifier.weight(1f), "R$ 0,00", KeyboardType.Decimal)
                        Button(
                            onClick = {
                                val cents = parseMoneyToCents(batchAmount)
                                when {
                                    cents == null || cents <= 0 -> viewModel.showValidationError(uiText("enterDistributionValue", state.language))
                                    cents < MinContributionCents -> viewModel.showValidationError(uiText("minimumContribution", state.language))
                                    state.community.isEmpty() -> viewModel.showValidationError(uiText("noOpenRequests", state.language))
                                    else -> viewModel.createContributionBatch(cents)
                                }
                            },
                            enabled = !state.loading,
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraGreen, contentColor = NexoraBlack),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.width(96.dp).height(56.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                        ) {
                            Text(uiText("generate", state.language), fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            }
            if (state.community.isEmpty()) {
                item { EmptyState(uiText("communityEmptyTitle", state.language), uiText("communityEmptySubtitle", state.language)) }
            } else {
                items(state.community, key = { it.id }) { request ->
                    CommunityRequestCard(request = request)
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
    val limitText = formatMoney(profile.supportLimitCents)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
    ) {
        ScreenTitle(
            uiText("requestTitle", state.language),
            uiText("requestSubtitle", state.language),
            language = state.language,
            onLanguageSelect = viewModel::setLanguage,
        )
        RequestLimitPanel(label = uiText("yourLimitUpper", state.language), value = limitText)
        if (!eligible) {
            Spacer(Modifier.height(12.dp))
            MessageStrip(uiText("requestLocked", state.language))
        }
        Spacer(Modifier.height(22.dp))
        NexoraInput(uiText("requestedValue", state.language), amount, { amount = it; if (invalidField == "amount") invalidField = null }, keyboardType = KeyboardType.Decimal, placeholder = "R$ 0,00", isError = invalidField == "amount")
        Spacer(Modifier.height(14.dp))
        NexoraInput(uiText("returnDeadlineDays", state.language), days, { days = it; if (invalidField == "days") invalidField = null }, keyboardType = KeyboardType.Number, isError = invalidField == "days")
        Spacer(Modifier.height(14.dp))
        NexoraInput(uiText("optionalDescription", state.language), description, { description = it }, minLines = 4)
        Spacer(Modifier.height(22.dp))
        NexoraButton(uiText("requestSupportButton", state.language), loading = state.loading) {
            val cents = parseMoneyToCents(amount)
            val dueDays = days.toIntOrNull()
            when {
                !eligible -> viewModel.showValidationError(uiText("requestLockedShort", state.language))
                cents == null || cents <= 0 -> {
                    invalidField = "amount"
                    viewModel.showValidationError(uiText("enterValidValue", state.language))
                }
                cents > profile.supportLimitCents -> {
                    invalidField = "amount"
                    viewModel.showValidationError(uiText("valueAboveLimit", state.language))
                }
                dueDays == null -> {
                    invalidField = "days"
                    viewModel.showValidationError(uiText("enterDeadlineDays", state.language))
                }
                dueDays !in 1..30 -> {
                    invalidField = "days"
                    viewModel.showValidationError(uiText("deadlineRange", state.language))
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
private fun RequestLimitPanel(label: String, value: String) {
    NexoraPanel(border = true, background = NexoraDarkGreen) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                label,
                color = NexoraGreen,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 2.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
            )
            AutoFitSingleLineText(
                text = value,
                color = NexoraGreen,
                maxFontSize = 30.sp,
                minFontSize = 16.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AutoFitSingleLineText(
    text: String,
    color: Color,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(density) { maxWidth.roundToPx() }
        val fittedFontSize = remember(text, maxWidthPx, maxFontSize, minFontSize, fontWeight, density.density, density.fontScale) {
            if (maxWidthPx <= 0) {
                minFontSize
            } else {
                generateSequence(maxFontSize.value) { current ->
                    (current - 1f).takeIf { it >= minFontSize.value }
                }.firstOrNull { size ->
                    val result = textMeasurer.measure(
                        text = text,
                        style = TextStyle(fontSize = size.sp, fontWeight = fontWeight),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        constraints = Constraints(maxWidth = maxWidthPx),
                    )
                    !result.didOverflowWidth
                }?.sp ?: minFontSize
            }
        }

        Text(
            text,
            color = color,
            fontSize = fittedFontSize,
            lineHeight = fittedFontSize,
            fontWeight = fontWeight,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RepaymentScreen(state: NexoraUiState, viewModel: NexoraViewModel) {
    val workspace = state.repayments
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenTitle(
                uiText("returnsTitle", state.language),
                uiText("returnsSubtitle", state.language),
                onRefresh = viewModel::refreshRepayments,
                refreshing = MainTab.RETORNOS in state.refreshingTabs,
            )
        }
        item {
            NexoraPanel(border = workspace.summary.overdueCount > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatText(uiText("pendingReturns", state.language), "${workspace.summary.pendingCount}", if (workspace.summary.overdueCount > 0) NexoraRed else NexoraGreen)
                    StatText(uiText("pendingValue", state.language), formatMoney(workspace.summary.pendingAmountCents), NexoraText)
                }
                workspace.summary.nextDueAt?.let {
                    Spacer(Modifier.height(12.dp))
                    Text("${uiText("nextDue", state.language)}: ${formatTimestamp(it)}", color = NexoraMuted)
                }
                if (workspace.summary.overdueCount > 0) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = NexoraRed)
                        Text(uiText("overduePenalty", state.language), color = NexoraRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { Text(uiText("needToReturn", state.language), color = NexoraText, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        if (workspace.owed.isEmpty()) {
            item { EmptyState(uiText("noReturns", state.language), uiText("noReturnsHelp", state.language)) }
        } else {
            items(workspace.owed, key = { "owed-${it.id}" }) { repayment ->
                RepaymentCard(repayment, state, viewModel)
            }
        }
        item { Text(uiText("toReceive", state.language), color = NexoraText, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        if (workspace.receivable.isEmpty()) {
            item { Text(uiText("nothingToReceive", state.language), color = NexoraMuted) }
        } else {
            items(workspace.receivable, key = { "receive-${it.id}" }) { repayment ->
                RepaymentCard(repayment, state, viewModel)
            }
        }
    }
}

@Composable
private fun RepaymentCard(repayment: Repayment, state: NexoraUiState, viewModel: NexoraViewModel) {
    val clipboard = LocalClipboardManager.current
    val owed = repayment.direction == "OWED"
    val statusText = when (repayment.status) {
        "PROOF_SUBMITTED" -> uiText("returnProofSent", state.language)
        "CONFIRMED" -> uiText("returnConfirmed", state.language)
        else -> uiText("returnPending", state.language)
    }
    NexoraPanel(border = repayment.overdue) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(formatMoney(repayment.amountCents), color = if (repayment.overdue) NexoraRed else NexoraGreen, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(repayment.requestPublicCode, color = NexoraMuted)
            }
            StatusPill(if (repayment.overdue) "BLOCKED" else if (repayment.status == "CONFIRMED") "RETURNED" else "PENDING")
        }
        Spacer(Modifier.height(12.dp))
        DetailLine(if (owed) uiText("returnTo", state.language) else uiText("receiveFrom", state.language), "${repayment.counterpartyName} · ${repayment.counterpartyPublicId}")
        DetailLine(uiText("returnStatus", state.language), statusText)
        DetailLine(uiText("returnDue", state.language), repayment.dueAt?.let(::formatTimestamp) ?: uiText("waitingFunding", state.language))
        repayment.daysRemaining?.takeIf { repayment.status != "CONFIRMED" }?.let {
            Text(
                if (repayment.overdue) "${kotlin.math.abs(it)} ${uiText("daysLate", state.language)}" else "$it ${uiText("daysRemaining", state.language)}",
                color = if (repayment.overdue) NexoraRed else NexoraMuted,
                fontWeight = if (repayment.overdue) FontWeight.Bold else FontWeight.Normal,
            )
        }
        repayment.pixKeyMasked?.let { DetailLine(uiText("destinationPix", state.language), it) }
        repayment.transactionId?.let { DetailLine(uiText("transactionId", state.language), it) }

        if (owed && repayment.status == "PENDING") {
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = { repayment.pixCopyCode?.let { clipboard.setText(AnnotatedString(it)) } },
                enabled = repayment.pixCopyCode != null,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, NexoraGreen),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = NexoraGreen)
                Spacer(Modifier.width(8.dp))
                Text(uiText("copyReturnPix", state.language), color = NexoraGreen)
            }
            Spacer(Modifier.height(10.dp))
            RepaymentProofControls(repayment.id, state.loading, viewModel::submitRepaymentProof)
        }
        if (owed && repayment.status == "PROOF_SUBMITTED") {
            Spacer(Modifier.height(10.dp))
            Text(uiText("waitingReturnConfirmation", state.language), color = NexoraMuted)
        }
        if (!owed && repayment.status == "PROOF_SUBMITTED") {
            Spacer(Modifier.height(12.dp))
            NexoraButton(uiText("confirmReturnReceipt", state.language).uppercase(), loading = state.loading) {
                viewModel.confirmRepayment(repayment.id)
            }
        }
        repayment.penaltyMessage?.let {
            Spacer(Modifier.height(10.dp))
            Text(uiText("overduePenalty", state.language), color = NexoraRed, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RepaymentProofControls(
    repaymentId: String,
    loading: Boolean,
    onSubmit: (String, String, ReceiptUpload) -> Unit,
) {
    val context = LocalContext.current
    var transactionId by rememberSaveable(repaymentId) { mutableStateOf("") }
    var upload by remember(repaymentId) { mutableStateOf<ReceiptUpload?>(null) }
    var error by remember(repaymentId) { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { receiptUploadFromUri(context, uri) }
                .onSuccess { upload = it; error = null }
                .onFailure { error = it.message }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NexoraInput(uiText("returnTransactionId"), transactionId, { transactionId = it })
        OutlinedButton(
            onClick = { launcher.launch("image/*") },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            border = BorderStroke(1.dp, NexoraGreen),
        ) {
            Icon(if (upload == null) Icons.Filled.AddAPhoto else Icons.Filled.Check, contentDescription = null, tint = NexoraGreen)
            Spacer(Modifier.width(8.dp))
            Text(if (upload == null) uiText("attachReturnProof") else uiText("photoReady"), color = NexoraGreen)
        }
        NexoraButton(uiText("sendReturnProof").uppercase(), loading = loading) {
            val current = upload
            if (transactionId.trim().length < 6 || current == null) {
                error = uiText("returnProofRequired")
            } else {
                error = null
                onSubmit(repaymentId, transactionId.trim(), current)
            }
        }
        error?.let { Text(it, color = NexoraRed, fontSize = 13.sp) }
    }
}

@Composable
private fun ProfileScreen(state: NexoraUiState, viewModel: NexoraViewModel) {
    val profile = state.profile ?: return
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val progress = (profile.xpIntoLevel.toFloat() / profile.xpRequiredThisLevel.toFloat()).coerceIn(0f, 1f)
    val inviteLink = "$NexoraDownloadUrl?invite=${profile.inviteCode}"
    val inviteText = uiText("inviteShareText", state.language)
        .replace("{code}", profile.inviteCode)
        .replace("{link}", inviteLink)
    fun shareInvite() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, inviteText)
        }
        context.startActivity(Intent.createChooser(intent, uiText("shareInvite", state.language)))
    }
    var historyFilter by rememberSaveable { mutableStateOf("ALL") }
    val filteredHistory = remember(state.contributionHistory, historyFilter) {
        filterContributionHistory(state.contributionHistory, historyFilter)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenTitle(
                uiText("profileTitle", state.language),
                profile.publicId,
                viewModel::refreshProfileAndMine,
                refreshing = MainTab.PERFIL in state.refreshingTabs,
                language = state.language,
                onLanguageSelect = viewModel::setLanguage,
            )
        }
        item {
            NexoraPanel {
                Text(profile.name, color = NexoraText, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(profile.email, color = NexoraMuted, fontSize = 14.sp)
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text(uiText("level", state.language).uppercase(), color = NexoraMuted, letterSpacing = 2.sp)
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
            ProfileStatsPanel(profile = profile, language = state.language)
        }
        item {
            NexoraPanel {
                LabelValue(uiText("pixKey", state.language), profile.pixKeyMasked, onCopy = {
                    clipboard.setText(AnnotatedString(profile.pixKeyMasked))
                    Toast.makeText(context, uiText("pixCopied", state.language), Toast.LENGTH_SHORT).show()
                })
                Spacer(Modifier.height(18.dp))
                Text(uiText("inviteCode", state.language), color = NexoraMuted, fontSize = 16.sp, letterSpacing = 1.sp)
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(profile.inviteCode))
                            Toast.makeText(context, uiText("inviteCopied", state.language), Toast.LENGTH_SHORT).show()
                        },
                        border = BorderStroke(1.dp, NexoraGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = uiText("copyCode", state.language), tint = NexoraGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(uiText("copyCode", state.language), color = NexoraGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                    }
                    Button(
                        onClick = { shareInvite() },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraGreen, contentColor = NexoraBlack),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = uiText("shareInvite", state.language), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(uiText("share", state.language), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
        item {
            NexoraPanel {
                LabelValue(uiText("adminFeeAccumulated", state.language), "${formatMoney(profile.adminFeeDueCents)} / ${formatMoney(profile.adminFeeLimitCents)}")
                profile.adminPixKey?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(uiText("sendAdminFeeHelp", state.language), color = NexoraMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    LabelValue(uiText("adminFeePix", state.language), it, onCopy = {
                        clipboard.setText(AnnotatedString(it))
                        Toast.makeText(context, uiText("pixCopied", state.language), Toast.LENGTH_SHORT).show()
                    })
                } ?: Text(uiText("noPendingFee", state.language), color = NexoraMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
                }
        }
        item {
            FaqContactPanel(
                language = state.language,
                onCopyEmail = {
                    clipboard.setText(AnnotatedString(NexoraContactEmail))
                    Toast.makeText(context, uiText("contactCopied", state.language), Toast.LENGTH_SHORT).show()
                },
                onCopyPhone = {
                    clipboard.setText(AnnotatedString(NexoraContactPhone))
                    Toast.makeText(context, uiText("contactCopied", state.language), Toast.LENGTH_SHORT).show()
                },
            )
        }
        item {
            Text(uiText("myRequests", state.language), color = NexoraText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        if (state.myRequests.isEmpty()) {
            item { EmptyState(uiText("noRequestsTitle", state.language), uiText("noRequestsSubtitle", state.language)) }
        } else {
            items(state.myRequests, key = { it.id }) { request ->
                CompactRequestCard(request)
            }
        }
        item {
            Column {
                Text(uiText("transactionHistory", state.language), color = NexoraText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "ACTIVE" to uiText("active", state.language),
                        "CANCELLED" to uiText("cancelled", state.language),
                        "ALL" to uiText("all", state.language),
                    ).forEach { (id, label) ->
                        FilterChip(
                            selected = historyFilter == id,
                            onClick = { historyFilter = id },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NexoraDarkGreen,
                                selectedLabelColor = NexoraGreen,
                                labelColor = NexoraMuted,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = historyFilter == id,
                                borderColor = Color(0xFF282828),
                                selectedBorderColor = NexoraGreen,
                            ),
                        )
                    }
                }
            }
        }
        if (filteredHistory.isEmpty()) {
            item { EmptyState(uiText("noTransactionsTitle", state.language), uiText("noTransactionsSubtitle", state.language)) }
        } else {
            items(filteredHistory, key = { it.id }) { contribution ->
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
                Text(uiText("logout", state.language), color = NexoraRed)
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
                ScreenTitle(
                    uiText("adminTitle", state.language),
                    uiText("adminSubtitle", state.language),
                    { viewModel.refreshAdmin() },
                    refreshing = MainTab.ADMIN in state.refreshingTabs,
                    language = state.language,
                    onLanguageSelect = viewModel::setLanguage,
                )
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
                        Text(uiText("adminUsers", state.language), modifier = Modifier.padding(16.dp), color = if (section == 0) NexoraGreen else NexoraMuted, fontWeight = FontWeight.Bold)
                    }
                    Tab(selected = section == 1, onClick = { section = 1 }) {
                        Text(uiText("adminRequests", state.language), modifier = Modifier.padding(16.dp), color = if (section == 1) NexoraGreen else NexoraMuted, fontWeight = FontWeight.Bold)
                    }
                    Tab(selected = section == 2, onClick = { section = 2 }) {
                        Text(uiText("adminContributions", state.language), modifier = Modifier.padding(16.dp), color = if (section == 2) NexoraGreen else NexoraMuted, fontWeight = FontWeight.Bold)
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
                    language = state.language,
                )
            }
            when (section) {
                0 -> {
                    if (filteredUsers.isEmpty()) item { EmptyState(uiText("noUsersTitle", state.language), uiText("noUsersSubtitle", state.language)) }
                    items(filteredUsers, key = { it.id }) { user ->
                        AdminUserCard(user, viewModel, onDetails = { selectedUser = user })
                    }
                }
                1 -> {
                    if (filteredRequests.isEmpty()) item { EmptyState(uiText("noAdminRequestsTitle", state.language), uiText("noAdminRequestsSubtitle", state.language)) }
                    items(filteredRequests, key = { it.id }) { request ->
                        AdminRequestCard(request, viewModel, onDetails = { selectedRequest = request })
                    }
                }
                2 -> {
                    if (filteredContributions.isEmpty()) item { EmptyState(uiText("noContributionsTitle", state.language), uiText("noContributionsSubtitle", state.language)) }
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

@Composable
private fun FilterInput(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 12.sp) },
        modifier = modifier.height(48.dp),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = NexoraText,
            unfocusedTextColor = NexoraText,
            focusedBorderColor = NexoraGreen,
            unfocusedBorderColor = Color(0xFF282828),
            focusedContainerColor = NexoraField,
            unfocusedContainerColor = NexoraField,
        ),
    )
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
    language: AppLanguage,
) {
    NexoraPanel {
        FilterInput(uiText("search", language), query, onQueryChange, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text(uiText("status", language), color = NexoraMuted, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            statusOptions.forEach { status ->
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { onStatusChange(status) },
                    label = { Text(if (status == "ALL") uiText("all", language) else statusLabel(status), fontSize = 11.sp) },
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
        if (showReceipt) {
            Spacer(Modifier.height(10.dp))
            Text(uiText("receipts", language), color = NexoraMuted, fontSize = 12.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    "ALL" to uiText("all", language),
                    "complete" to uiText("completePlural", language),
                    "missing" to uiText("pendingPlural", language),
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = receiptFilter == value,
                        onClick = { onReceiptChange(value) },
                        label = { Text(label, fontSize = 11.sp) },
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
        Text(uiText("period", language), color = NexoraMuted, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            FilterInput(uiText("from", language), fromDate, onFromDateChange, Modifier.weight(1f))
            FilterInput(uiText("to", language), toDate, onToDateChange, Modifier.weight(1f))
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
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(width = 58.dp, height = 46.dp),
    ) {
        Icon(Icons.Filled.Visibility, contentDescription = uiText("viewDetails"), tint = NexoraGreen, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AdminUserDetailDialog(user: AdminUser, onDismiss: () -> Unit) {
    DetailDialog(title = user.name, subtitle = uiText("fullUserData"), onDismiss = onDismiss) {
        DetailLine("Public ID", user.publicId)
        DetailLine("E-mail", user.email)
        DetailLine("CPF", user.cpf)
        DetailLine(uiText("pixKey"), user.pixKey)
        DetailLine(uiText("status"), statusLabel(user.status))
        DetailLine(uiText("role"), roleLabel(user.role))
        DetailLine(uiText("level"), "${user.level}")
        DetailLine("XP", "${user.xp}")
        DetailLine(uiText("buff"), formatBuff(user.buffBps))
        DetailLine(uiText("limit"), formatMoney(user.supportLimitCents))
        DetailLine(uiText("accumulatedFee"), "${formatMoney(user.adminFeeDueCents)} / ${formatMoney(user.adminFeeLimitCents)}")
        DetailLine(uiText("adminRandomPix"), user.adminPixKey ?: "-")
        DetailLine(uiText("invite"), user.inviteCode)
        DetailLine(uiText("invitedBy"), user.invitedByPublicId ?: "-")
        DetailLine(uiText("invitedUsers"), "${user.invitedCount}")
        DetailLine(uiText("createdAt"), formatTimestamp(user.createdAt))
    }
}

@Composable
private fun AdminRequestDetailDialog(
    request: AdminSupportRequest,
    contributions: List<AdminContribution>,
    onContributionSelected: (AdminContribution) -> Unit,
    onDismiss: () -> Unit,
) {
    DetailDialog(title = request.publicCode, subtitle = uiText("requestDetails"), onDismiss = onDismiss) {
        DetailLine(uiText("requester"), request.requesterName)
        DetailLine("Public ID", request.requesterPublicId)
        DetailLine("E-mail", request.requesterEmail)
        DetailLine("CPF", request.requesterCpf)
        DetailLine(uiText("pixKey"), request.requesterPixKey)
        DetailLine(uiText("status"), statusLabel(request.status))
        DetailLine(uiText("value"), "${formatMoney(request.fundedCents)} / ${formatMoney(request.amountCents)}")
        DetailLine(uiText("adminFee"), formatMoney(request.adminFeeCents))
        DetailLine(uiText("deadline"), formatDays(request.dueDays))
        DetailLine(uiText("createdAt"), formatTimestamp(request.createdAt))
        DetailLine(uiText("description"), request.description ?: "-")
        Spacer(Modifier.height(10.dp))
        Text(uiText("linkedContributions"), color = NexoraGreen, fontWeight = FontWeight.Bold)
        if (contributions.isEmpty()) {
            Text(uiText("noLinkedContributions"), color = NexoraMuted, fontSize = 13.sp)
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
    DetailDialog(title = contribution.requestPublicCode, subtitle = uiText("pixSupportDetails"), onDismiss = onDismiss) {
        DetailLine(uiText("support"), contribution.id)
        DetailLine(uiText("request"), "${contribution.requestPublicCode} (${statusLabel(contribution.requestStatus)})")
        DetailLine(uiText("value"), formatMoney(contribution.amountCents))
        DetailLine(uiText("transactionId"), contribution.transactionId ?: "-")
        DetailLine(uiText("status"), statusLabel(contribution.status))
        DetailLine(uiText("createdAt"), formatTimestamp(contribution.createdAt))
        DetailLine(uiText("donor"), "${contribution.donorName.ifBlank { contribution.donorPublicId }} · ${contribution.donorEmail}")
        DetailLine(uiText("receiver"), "${contribution.receiverName.ifBlank { contribution.receiverPublicId }} · ${contribution.receiverEmail}")
        
        contribution.ocrComparisonResult?.let { result ->
            Spacer(Modifier.height(16.dp))
            OCRStatusSection(result, contribution.ocrComparisonNotes, contribution.hasSenderReceipt, contribution.hasReceiverReceipt)
        } ?: run {
            Spacer(Modifier.height(16.dp))
            OCRStatusSection(null, null, contribution.hasSenderReceipt, contribution.hasReceiverReceipt)
        }

        Spacer(Modifier.height(16.dp))
        ReceiptPreview(
            title = uiText("donorReceipt"),
            date = contribution.senderReceiptDate,
            submittedAt = contribution.senderReceiptSubmittedAt,
            imageBase64 = contribution.senderReceiptImageBase64,
            ocrAmount = contribution.senderOcrAmountCents,
            ocrConfidence = contribution.senderOcrConfidence,
        )
        Spacer(Modifier.height(12.dp))
        ReceiptPreview(
            title = uiText("receiverReceipt"),
            date = contribution.receiverReceiptDate,
            submittedAt = contribution.receiverReceiptSubmittedAt,
            imageBase64 = contribution.receiverReceiptImageBase64,
            ocrAmount = contribution.receiverOcrAmountCents,
            ocrConfidence = contribution.receiverOcrConfidence,
        )
    }
}

@Composable
private fun OCRStatusSection(result: String?, notes: String?, hasSenderReceipt: Boolean, hasReceiverReceipt: Boolean) {
    val evidenceComplete = hasSenderReceipt && hasReceiverReceipt
    val statusKey = when {
        !hasSenderReceipt && !hasReceiverReceipt -> "statusNoReceipts"
        hasSenderReceipt && !hasReceiverReceipt -> "statusWaitingReceiverPhoto"
        !hasSenderReceipt && hasReceiverReceipt -> "statusWaitingDonorPhoto"
        result?.uppercase() == "MATCH" -> "statusMatch"
        result?.uppercase() == "NO_MATCH" || result?.uppercase() == "MISMATCH" -> "statusMismatch"
        result?.uppercase() == "PENDING" -> "statusReview"
        else -> "statusAnalyzing"
    }
    val helpKey = when {
        !hasSenderReceipt && !hasReceiverReceipt -> "statusNoReceiptsHelp"
        hasSenderReceipt && !hasReceiverReceipt -> "statusWaitingReceiverHelp"
        !hasSenderReceipt && hasReceiverReceipt -> "statusWaitingDonorHelp"
        !evidenceComplete -> "statusWaitingBothHelp"
        else -> null
    }
    val (color, icon, label) = when {
        !evidenceComplete -> Triple(NexoraMuted, Icons.Filled.Info, uiText(statusKey))
        result?.uppercase() == "MATCH" -> Triple(NexoraGreen, Icons.Filled.CheckCircle, uiText(statusKey))
        result?.uppercase() == "NO_MATCH" || result?.uppercase() == "MISMATCH" -> Triple(NexoraRed, Icons.Filled.Warning, uiText(statusKey))
        result?.uppercase() == "PENDING" -> Triple(Color.Yellow, Icons.Filled.Info, uiText(statusKey))
        else -> Triple(NexoraMuted, Icons.Filled.Info, uiText(statusKey))
    }

    NexoraPanel(background = color.copy(alpha = 0.1f), border = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = color, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        notes?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = NexoraText.copy(alpha = 0.8f), fontSize = 13.sp)
        }
        helpKey?.let {
            Spacer(Modifier.height(4.dp))
            Text(uiText(it), color = NexoraMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DetailDialog(title: String, subtitle: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(uiText("close"), color = NexoraGreen, fontWeight = FontWeight.Bold)
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
private fun ReceiptPreview(
    title: String,
    date: String?,
    submittedAt: Long?,
    imageBase64: String?,
    ocrAmount: Long? = null,
    ocrConfidence: String? = null
) {
    val context = LocalContext.current
    var showFullScreen by remember { mutableStateOf(false) }

    NexoraPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = NexoraGreen, fontWeight = FontWeight.Bold)
            if (imageBase64 != null) {
                Row {
                    IconButton(onClick = { showFullScreen = true }) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = "Ver Grande", tint = NexoraGreen)
                    }
                    IconButton(onClick = { downloadImage(context, imageBase64, "comprovante_${System.currentTimeMillis()}.png") }) {
                        Icon(Icons.Filled.Download, contentDescription = "Download", tint = NexoraGreen)
                    }
                }
            }
        }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(NexoraField, RoundedCornerShape(8.dp))
                    .clickable { showFullScreen = true },
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(140.dp).background(NexoraField, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(uiText("noPhotoAttached"), color = NexoraMuted)
            }
        }
        
        if (ocrAmount != null || ocrConfidence != null) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().background(NexoraBlack.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(8.dp)) {
                Column {
                    Text(uiText("autoRead"), color = NexoraGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    ocrAmount?.let { Text("${uiText("receiptAmount")}: ${formatMoney(it)}", color = NexoraText, fontSize = 12.sp) }
                    ocrConfidence?.let { Text("${uiText("ocrConfidence")}: $it", color = NexoraMuted, fontSize = 11.sp) }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        if (imageBase64 != null) {
            DetailLine(uiText("receiptDate"), date ?: "-")
            DetailLine(uiText("sentAt"), submittedAt?.let { formatTimestamp(it) } ?: "-")
            DetailLine(uiText("receiptIntegrityOk"), "OK")
        }
    }

    if (showFullScreen && imageBase64 != null) {
        FullScreenImageDialog(imageBase64, onDismiss = { showFullScreen = false })
    }
}

@Composable
private fun FullScreenImageDialog(imageBase64: String, onDismiss: () -> Unit) {
    val bitmap = remember(imageBase64) {
        runCatching {
            val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    if (bitmap == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            var scale by remember { mutableStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                scale *= zoomChange
                offset += offsetChange
            }

            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale.coerceIn(1f, 5f),
                        scaleY = scale.coerceIn(1f, 5f),
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = state)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset += pan
                        }
                    },
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                Icon(Icons.Filled.Close, contentDescription = uiText("close"), tint = Color.White)
            }
        }
    }
}

private fun downloadImage(context: android.content.Context, base64: String, filename: String) {
    runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            }
            Toast.makeText(context, uiText("imageSaved"), Toast.LENGTH_SHORT).show()
        } ?: throw Exception("Failed to create URI")
    }.onFailure {
        Toast.makeText(context, "${uiText("imageSaveError")}: ${it.message}", Toast.LENGTH_SHORT).show()
    }
}

private data class FaqEntry(val question: String, val answer: String)

@Composable
private fun TutorialDialog(
    language: AppLanguage,
    onLanguageSelect: (AppLanguage) -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NexoraCard,
        icon = {
            NexoraLogoImage(
                modifier = Modifier
                    .size(92.dp)
                    .padding(bottom = 4.dp),
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(uiText("tutorialTitle", language), color = NexoraText, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                LanguageSelector(language = language, onSelect = onLanguageSelect)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(uiText("tutorialIntro", language), color = NexoraMuted, lineHeight = 20.sp)
                tutorialSteps(language).forEachIndexed { index, step ->
                    TutorialStep(number = index + 1, text = step)
                }
                HorizontalDivider(color = Color(0xFF2A2A2A))
                Text(uiText("faqTitle", language), color = NexoraGreen, fontWeight = FontWeight.Black)
                faqEntries(language).forEach { entry ->
                    FaqItem(entry)
                }
                HorizontalDivider(color = Color(0xFF2A2A2A))
                Text(uiText("downloadHint", language), color = NexoraMuted, fontSize = 13.sp, lineHeight = 18.sp)
                OutlinedButton(
                    onClick = onDownload,
                    border = BorderStroke(1.dp, NexoraGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, tint = NexoraGreen)
                    Spacer(Modifier.width(8.dp))
                    Text(uiText("openDownloadPage", language), color = NexoraGreen, fontWeight = FontWeight.Bold)
                }
                Text(
                    "${uiText("contactTitle", language)}: $NexoraContactEmail · $NexoraContactPhone",
                    color = NexoraMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NexoraGreen, contentColor = NexoraBlack),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(uiText("startUsing", language), fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(uiText("skipTutorial", language), color = NexoraMuted)
            }
        },
    )
}

@Composable
private fun TutorialStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(NexoraDarkGreen, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = NexoraGreen, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
        Text(text, color = NexoraText, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FaqItem(entry: FaqEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(entry.question, color = NexoraText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(entry.answer, color = NexoraMuted, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun FaqContactPanel(
    language: AppLanguage,
    onCopyEmail: () -> Unit,
    onCopyPhone: () -> Unit,
) {
    NexoraPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = NexoraGreen)
            Text(uiText("faqTitle", language), color = NexoraGreen, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(14.dp))
        faqEntries(language).forEach { entry ->
            FaqItem(entry)
            Spacer(Modifier.height(12.dp))
        }
        HorizontalDivider(color = Color(0xFF2A2A2A))
        Spacer(Modifier.height(12.dp))
        Text(uiText("contactBody", language), color = NexoraMuted, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(10.dp))
        LabelValue("E-mail", NexoraContactEmail, onCopy = onCopyEmail)
        Spacer(Modifier.height(8.dp))
        LabelValue(uiText("phoneContact", language), NexoraContactPhone, onCopy = onCopyPhone)
    }
}

@Composable
private fun HeaderRow(
    profile: Profile,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    language: AppLanguage,
    onLanguageSelect: (AppLanguage) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            NexoraLogo(compact = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                LanguageSelector(language = language, onSelect = onLanguageSelect)
                RefreshIconButton(refreshing = refreshing, onRefresh = onRefresh, tint = NexoraGreen)
            }
        }
        Text(uiText("tagline"), color = NexoraMuted, fontSize = 14.sp, letterSpacing = 1.sp)
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
                Text("${uiText("levelShort")} ${profile.level}", color = NexoraGreen, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(profile.name, color = NexoraText, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Clip)
            }
        }
    }
}

@Composable
private fun ScreenTitle(
    title: String,
    subtitle: String,
    onRefresh: (() -> Unit)? = null,
    refreshing: Boolean = false,
    language: AppLanguage? = null,
    onLanguageSelect: ((AppLanguage) -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(title, color = NexoraText, fontSize = 32.sp, fontWeight = FontWeight.Black, maxLines = 3, overflow = TextOverflow.Clip)
            Text(subtitle, color = NexoraMuted, fontSize = 18.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (language != null && onLanguageSelect != null) {
                LanguageSelector(language = language, onSelect = onLanguageSelect)
            }
            onRefresh?.let {
                RefreshIconButton(refreshing = refreshing, onRefresh = it, tint = NexoraGreen)
            }
        }
    }
}

@Composable
private fun RefreshIconButton(refreshing: Boolean, onRefresh: () -> Unit, tint: Color) {
    val rotation = if (refreshing) {
        val transition = rememberInfiniteTransition(label = "refresh")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(animation = tween(durationMillis = 750, easing = LinearEasing)),
            label = "refreshAngle",
        )
        angle
    } else {
        0f
    }
    IconButton(onClick = onRefresh, enabled = !refreshing) {
        Icon(
            Icons.Filled.Refresh,
            contentDescription = uiText("refresh"),
            tint = tint,
            modifier = Modifier.graphicsLayer(rotationZ = rotation),
        )
    }
}

@Composable
private fun NexoraLogo(compact: Boolean = false) {
    Column(horizontalAlignment = if (compact) Alignment.Start else Alignment.CenterHorizontally) {
        NexoraLogoImage(
            modifier = Modifier
                .size(if (compact) 54.dp else 120.dp)
                .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = NexoraGreen, spotColor = NexoraGreen),
        )
        Text(
            "NEXORA",
            color = NexoraGreen,
            fontWeight = FontWeight.Black,
            fontSize = if (compact) 18.sp else 32.sp,
            letterSpacing = if (compact) 1.sp else 4.sp,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(top = if (compact) 2.dp else 8.dp),
        )
        if (!compact) {
            Text(uiText("tagline"), color = Color(0xFFB6B6BD), fontStyle = FontStyle.Italic, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun NexoraLogoImage(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.nexora_logo_centered_384),
        contentDescription = "Nexora",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
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
private fun LanguageSelector(language: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            border = BorderStroke(1.dp, NexoraGreen),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoraGreen),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier.height(38.dp),
        ) {
            Text(language.flag, fontSize = 20.sp, maxLines = 1)
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = uiText("language", language),
                tint = NexoraGreen,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = NexoraCard,
        ) {
        AppLanguage.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(option.flag, fontSize = 20.sp)
                            Text(option.label, color = if (option == language) NexoraGreen else NexoraText, fontSize = 14.sp)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
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

@Composable
private fun NexoraInput(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    isError: Boolean = false,
    readOnly: Boolean = false,
    enabled: Boolean = true,
) {
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
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
            Text(uiText("wait").uppercase(), fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 16.sp, maxLines = 1)
        } else {
            Text(text, fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 16.sp, maxLines = 1)
        }
    }
}

@Composable
private fun StatText(label: String, value: String, color: Color) {
    Column {
        Text(label, color = NexoraMuted, fontSize = 14.sp, letterSpacing = 1.sp)
        Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ProfileStatsPanel(profile: Profile, language: AppLanguage) {
    NexoraPanel {
        ProfileStatRow(uiText("buff", language), formatBuff(profile.buffBps))
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFF252525))
        ProfileStatRow(uiText("limit", language), formatMoney(profile.supportLimitCents))
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFF252525))
        ProfileStatRow(uiText("invites", language), "${profile.invitedCount}")
    }
}

@Composable
private fun ProfileStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = NexoraMuted, fontSize = 14.sp, letterSpacing = 1.sp)
        Text(value, color = NexoraGreen, fontSize = 26.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.End)
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val valueSize = when {
        value.length >= 12 -> 16.sp
        value.length >= 9 -> 18.sp
        value.length >= 6 -> 21.sp
        else -> 28.sp
    }
    Card(
        modifier = modifier.heightIn(min = 118.dp),
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
                maxLines = 2,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun RoadmapCard(dashboard: Dashboard?) {
    NexoraPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(uiText("expansion"), color = NexoraGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("${uiText("stage")} ${dashboard?.roadmapStep ?: 1}", color = NexoraText, fontWeight = FontWeight.Bold)
        }
        Text(
            "${uiText("currentCapacity")}: ${dashboard?.roadmapCapacity ?: 20} ${uiText("participants")}",
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
private fun CommunityRequestCard(request: SupportRequest) {
    NexoraPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(request.publicCode, color = NexoraText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("${request.requesterPublicId} · ${uiText("levelShort")} ${request.requesterLevel}", color = NexoraMuted)
                Text(formatTimestamp(request.createdAt), color = NexoraMuted, fontSize = 12.sp)
            }
            StatusPill(request.status)
        }
        Spacer(Modifier.height(12.dp))
        Text(uiText("chronologicalHelp"), color = NexoraMuted, fontSize = 13.sp)
    }
}

@Composable
private fun CompactRequestCard(request: SupportRequest) {
    NexoraPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(request.publicCode, color = NexoraText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("${uiText("levelShort")} ${request.requesterLevel}", color = NexoraMuted)
                Text(formatTimestamp(request.createdAt), color = NexoraMuted, fontSize = 12.sp)
            }
            StatusPill(request.status)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().background(NexoraDarkGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(uiText("waitingSupporters"), color = NexoraGreen, fontWeight = FontWeight.Medium)
        }
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
                    if (contribution.direction == "SENT") "${uiText("sentTo")} ${contribution.receiverPublicId}" else "${uiText("receivedFrom")} ${contribution.donorPublicId}",
                    color = NexoraMuted,
                )
                Text(formatTimestamp(contribution.createdAt), color = NexoraMuted, fontSize = 12.sp)
            }
            StatusPill(contribution.status)
        }
        Spacer(Modifier.height(10.dp))
        LabelValue(uiText("transactionId"), contribution.transactionId ?: uiText("waitingReceipt"))
        Spacer(Modifier.height(10.dp))
        Text(
            "${uiText("sentReceipt")}: ${if (contribution.hasSenderReceipt) uiText("attachedPhoto") else uiText("pending")} · ${uiText("receivedReceipt")}: ${if (contribution.hasReceiverReceipt) uiText("attachedPhoto") else uiText("pending")}",
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
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        if (showUpload) Icons.Filled.Close else Icons.Filled.AttachFile,
                        contentDescription = if (showUpload) uiText("hide") else uiText("attach"),
                        tint = NexoraGreen
                    )
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
    var receiptDate by rememberSaveable(contributionId, side) { mutableStateOf(LocalDate.now().toString()) }
    var upload by remember(contributionId, side) { mutableStateOf<ReceiptUpload?>(null) }
    var localError by remember(contributionId, side) { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { receiptUploadFromUri(context, uri) }
                .onSuccess {
                    upload = it
                    localError = null
                }
                .onFailure { localError = it.message ?: uiText("photoAttachError") }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NexoraInput(uiText("receiptDate"), receiptDate, { receiptDate = it }, readOnly = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { launcher.launch("image/*") },
                enabled = !loading,
                border = BorderStroke(1.dp, NexoraGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(56.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    if (upload == null) Icons.Filled.AddAPhoto else Icons.Filled.Check,
                    contentDescription = if (upload == null) uiText("attachPhoto") else uiText("photoReady"),
                    tint = NexoraGreen
                )
            }
            Button(
                onClick = {
                    val currentUpload = upload
                    if (currentUpload == null) {
                        localError = uiText("attachReceiptPhoto")
                    } else {
                        localError = null
                        // We send blank transactionId because the backend will extract it from OCR
                        onSubmit(contributionId, amountCents, "", currentUpload, receiptDate, side)
                    }
                },
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = NexoraGreen, contentColor = NexoraBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(56.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = uiText("send"))
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
        "BLOCKED", "REJECTED", "CANCELLED", "EXPIRED" -> NexoraRed
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

private fun tutorialSteps(language: AppLanguage): List<String> = listOf(
    uiText("tutorialStepPanel", language),
    uiText("tutorialStepCommunity", language),
    uiText("tutorialStepProfile", language),
)

private fun faqEntries(language: AppLanguage): List<FaqEntry> = listOf(
    FaqEntry(uiText("faqInviteQuestion", language), uiText("faqInviteAnswer", language)),
    FaqEntry(uiText("faqPixQuestion", language), uiText("faqPixAnswer", language)),
    FaqEntry(uiText("faqReceiptQuestion", language), uiText("faqReceiptAnswer", language)),
)

private fun uiText(key: String, language: AppLanguage = NexoraLanguageStore.current): String {
    val pt = mapOf(
        "language" to "Idioma",
        "tabPanel" to "Painel",
        "tabCommunity" to "Comunidade",
        "tabRequest" to "Solicitar",
        "tabReturns" to "Devolver",
        "tabProfile" to "Perfil",
        "returnsTitle" to "Devoluções",
        "pendingReturnsTitle" to "Você tem devoluções pendentes",
        "overdueReturnsTitle" to "Você tem devoluções em atraso",
        "returnsSubtitle" to "Valores, destinatários, prazos e comprovantes em um só lugar",
        "pendingReturns" to "PENDENTES",
        "pendingValue" to "VALOR PENDENTE",
        "nextDue" to "Próximo vencimento",
        "overduePenalty" to "Novas solicitações ficam bloqueadas até regularizar as devoluções atrasadas.",
        "needToReturn" to "Preciso devolver",
        "toReceive" to "Tenho a receber",
        "noReturns" to "Nenhuma devolução",
        "noReturnsHelp" to "Quando uma solicitação for totalmente financiada, cada destinatário aparecerá aqui.",
        "nothingToReceive" to "Nenhuma devolução para confirmar.",
        "returnTo" to "Devolver para",
        "receiveFrom" to "Receber de",
        "returnStatus" to "Situação",
        "returnDue" to "Prazo",
        "returnPending" to "Pagamento pendente",
        "returnProofSent" to "Comprovante enviado",
        "returnConfirmed" to "Devolução confirmada",
        "waitingFunding" to "Aguardando financiamento completo",
        "daysLate" to "dia(s) em atraso",
        "daysRemaining" to "dia(s) restantes",
        "destinationPix" to "Pix do destinatário",
        "copyReturnPix" to "Copiar Pix para devolver",
        "waitingReturnConfirmation" to "Aguardando o destinatário confirmar o recebimento.",
        "confirmReturnReceipt" to "Confirmar que recebi",
        "returnTransactionId" to "ID da transação Pix",
        "attachReturnProof" to "Anexar comprovante",
        "sendReturnProof" to "Enviar comprovante",
        "returnProofRequired" to "Informe o ID da transação e anexe o comprovante.",
        "tabAdmin" to "Admin",
        "tutorialFaq" to "Tutorial e FAQ",
        "tutorialTitle" to "Primeiros passos",
        "tutorialIntro" to "Um guia curto para usar a Nexora sem complicar.",
        "tutorialStepPanel" to "Painel: veja sua liquidez, limite, nível e histórico em um só lugar.",
        "tutorialStepCommunity" to "Comunidade: gere o Pix em ordem cronológica para apoiar solicitações abertas.",
        "tutorialStepProfile" to "Perfil: compartilhe apenas seu código, envie comprovantes e acompanhe suas taxas.",
        "faqTitle" to "Perguntas frequentes",
        "faqInviteQuestion" to "O que devo copiar para convidar alguém?",
        "faqInviteAnswer" to "Copie só o código de convite. O link fica apenas para compartilhar a página ou instalar a app.",
        "faqPixQuestion" to "Qual Pix devo usar?",
        "faqPixAnswer" to "Use sempre a chave Pix aleatória cadastrada pela pessoa solicitante. O nome dela não aparece no app.",
        "faqReceiptQuestion" to "Quando envio comprovante?",
        "faqReceiptAnswer" to "Depois da transferência, anexe a foto do comprovante no histórico para validação.",
        "contactTitle" to "Contato",
        "contactBody" to "Para dúvidas simples ou suporte, use os contatos abaixo. Toque para copiar.",
        "phoneContact" to "Telefone",
        "contactCopied" to "Contato copiado!",
        "downloadHint" to "Você também pode abrir a página da Nexora para acessar ou instalar a aplicação.",
        "openDownloadPage" to "Abrir página",
        "startUsing" to "COMEÇAR",
        "skipTutorial" to "Sair do tutorial",
        "inviteShareText" to "Entre na Nexora com meu código {code}. Acesse ou instale pela página: {link}",
        "communityTitle" to "Comunidade",
        "communitySubtitle" to "Solicitações ativas para apoiar",
        "communityPixOrder" to "Pix por ordem cronológica",
        "totalValue" to "Valor total",
        "generate" to "GERAR",
        "communityEmptyTitle" to "Comunidade estável",
        "communityEmptySubtitle" to "Nenhuma solicitação de outros usuários ativa no momento",
        "requestTitle" to "Solicitar Apoio",
        "requestSubtitle" to "A comunidade contribui via Pix até completar o valor",
        "profileTitle" to "Perfil",
        "adminTitle" to "Admin Nexora",
        "adminSubtitle" to "Painel de controle dos fundadores",
        "buff" to "BUFF",
        "limit" to "LIMITE",
        "invites" to "CONVITES",
        "myRequests" to "Minhas solicitações",
        "noRequestsTitle" to "Sem solicitações",
        "noRequestsSubtitle" to "Quando criar uma solicitação, ela aparece aqui",
        "transactionHistory" to "Histórico de transações",
        "active" to "Ativas",
        "cancelled" to "Canceladas",
        "all" to "Todas",
        "noTransactionsTitle" to "Sem transações",
        "noTransactionsSubtitle" to "Nenhuma transação encontrada nesta categoria",
        "statusNoReceipts" to "SEM COMPROVANTES",
        "statusWaitingReceiverPhoto" to "AGUARDANDO FOTO DO RECEBEDOR",
        "statusWaitingDonorPhoto" to "AGUARDANDO FOTO DO DOADOR",
        "statusMatch" to "SISTEMA: DADOS CONFEREM",
        "statusMismatch" to "SISTEMA: DIVERGÊNCIA ENCONTRADA",
        "statusReview" to "SISTEMA: REVISÃO MANUAL",
        "statusAnalyzing" to "ANÁLISE EM CURSO",
        "statusNoReceiptsHelp" to "Ainda não há fotos anexadas para comparar.",
        "statusWaitingReceiverHelp" to "Falta a foto de quem recebeu o Pix.",
        "statusWaitingDonorHelp" to "Falta a foto de quem enviou o Pix.",
        "statusWaitingBothHelp" to "O sistema só compara os dados quando os dois comprovantes forem enviados.",
        "donorReceipt" to "Comprovante do Doador",
        "receiverReceipt" to "Comprovante do Recebedor",
        "noPhotoAttached" to "Sem foto anexada",
        "autoRead" to "Leitura automática",
        "receiptAmount" to "Valor",
        "ocrConfidence" to "Confiança",
        "receiptDate" to "Data no comprovante",
        "sentAt" to "Enviado em",
        "receiptIntegrityOk" to "Comprovante anexado",
        "sentTo" to "Enviado para",
        "receivedFrom" to "Recebido de",
        "transactionId" to "ID da transação",
        "waitingReceipt" to "aguardando comprovante",
        "sentReceipt" to "Envio",
        "receivedReceipt" to "Recebimento",
        "attachedPhoto" to "foto anexada",
        "pending" to "pendente",
        "tagline" to "Karma que conecta pessoas",
        "levelShort" to "Nv",
        "level" to "Nível",
        "communityLiquidity" to "LIQUIDEZ COMUNITÁRIA",
        "inCirculation" to "Em circulação",
        "completion" to "Cumprimento",
        "activeRequests" to "Solicitações ativas",
        "completedOperations" to "Operações concluídas",
        "activeUsers" to "Usuários ativos",
        "yourLimit" to "Seu limite",
        "yourLimitUpper" to "SEU LIMITE",
        "howItWorks" to "Como funciona",
        "howItWorksBody" to "A comunidade contribui via Pix até completar sua solicitação. Quem apoia recebe 1 XP por real; quem devolve acumula buff e mantém o limite saudável.",
        "requestLocked" to "Solicitação liberada a partir do Nível 2 com 100 XP e limite disponível.",
        "requestedValue" to "Valor solicitado",
        "returnDeadlineDays" to "Prazo para retorno (dias)",
        "optionalDescription" to "Descrição opcional",
        "requestSupportButton" to "SOLICITAR APOIO",
        "requestLockedShort" to "Você ainda não pode solicitar: precisa estar no Nível 2 com 100 XP.",
        "enterValidValue" to "Informe um valor válido.",
        "minimumContribution" to "Doação mínima de R$ 5,00.",
        "valueAboveLimit" to "Valor acima do seu limite atual.",
        "enterDeadlineDays" to "Informe o prazo em dias.",
        "deadlineRange" to "O prazo precisa ficar entre 1 e 30 dias.",
        "pixCopied" to "Chave Pix copiada!",
        "inviteCopied" to "Código de convite copiado!",
        "copyLink" to "Copiar link",
        "copy" to "Copiar",
        "shareInvite" to "Compartilhar convite",
        "share" to "Compartilhar",
        "adminFeeAccumulated" to "Taxa administrativa acumulada",
        "sendAdminFeeHelp" to "Envie a taxa para a conta PJ da Nexora e aguarde a baixa administrativa.",
        "adminFeePix" to "Pix da taxa administrativa (CNPJ PJ)",
        "noPendingFee" to "Sem taxa pendente para envio.",
        "logout" to "Sair",
        "wait" to "Aguarde",
        "refresh" to "Atualizar",
        "adminUsers" to "Usuários",
        "adminRequests" to "Solicitações",
        "adminContributions" to "Apoios",
        "noUsersTitle" to "Sem usuários",
        "noUsersSubtitle" to "Nenhum usuário encontrado com os filtros atuais",
        "noAdminRequestsTitle" to "Sem solicitações",
        "noAdminRequestsSubtitle" to "Nenhuma solicitação encontrada com os filtros atuais",
        "noContributionsTitle" to "Sem apoios",
        "noContributionsSubtitle" to "Nenhum apoio encontrado com os filtros atuais",
        "search" to "Buscar...",
        "status" to "Status",
        "receipts" to "Comprovantes",
        "completePlural" to "Completos",
        "pendingPlural" to "Pendentes",
        "period" to "Período (AAAA-MM-DD)",
        "from" to "Desde",
        "to" to "Até",
        "viewDetails" to "Ver detalhes",
        "fullUserData" to "Dados completos do usuário",
        "role" to "Função",
        "accumulatedFee" to "Taxa acumulada",
        "adminRandomPix" to "Pix administrativo da conta PJ",
        "invite" to "Convite",
        "invitedBy" to "Convidado por",
        "invitedUsers" to "Convidados",
        "createdAt" to "Criado em",
        "requestDetails" to "Detalhes da solicitação",
        "requester" to "Solicitante",
        "value" to "Valor",
        "adminFee" to "Taxa administrativa",
        "deadline" to "Prazo",
        "description" to "Descrição",
        "linkedContributions" to "Apoios vinculados",
        "noLinkedContributions" to "Nenhum apoio para esta solicitação.",
        "pixSupportDetails" to "Detalhes do apoio Pix",
        "support" to "Apoio",
        "request" to "Solicitação",
        "donor" to "Doador",
        "receiver" to "Recebedor",
        "pendingFee" to "Taxa pendente",
        "approve" to "Aprovar",
        "activate" to "Ativar",
        "block" to "Bloquear",
        "settleFee" to "Baixar taxa",
        "reject" to "Recusar",
        "validateReturn" to "Validar retorno",
        "receipt" to "Recibo",
        "photo" to "foto",
        "validate" to "Validar",
        "waitingPhotos" to "Aguardando fotos",
        "confirm" to "Confirmar",
        "cancel" to "Cancelar",
        "confirmAction" to "Confirmar ação",
        "expansion" to "Expansão",
        "stage" to "Etapa",
        "currentCapacity" to "Capacidade atual",
        "participants" to "participantes",
        "activeRequest" to "Solicitação ativa",
        "chronologicalHelp" to "Será atendida pelo botão principal de Pix por ordem cronológica.",
        "waitingSupporters" to "Aguardando apoiadores",
        "hide" to "Ocultar",
        "attach" to "Anexar",
        "photoAttachError" to "Não foi possível anexar a foto.",
        "attachPhoto" to "Anexar foto",
        "photoReady" to "Foto pronta",
        "attachReceiptPhoto" to "Anexe a foto do comprovante.",
        "send" to "Enviar",
        "day" to "dia",
        "days" to "dias",
        "superAdmin" to "Super admin",
        "adminRole" to "Admin",
        "userRole" to "Usuário",
        "registrationEmailRequired" to "Informe o e-mail usado no cadastro.",
        "sixDigitCodeRequired" to "Digite o código de 6 dígitos.",
        "accountEmailRequired" to "Informe o e-mail da conta.",
        "newPasswordMin" to "A nova senha precisa ter pelo menos 8 caracteres.",
        "passwordsDontMatch" to "As senhas não conferem.",
        "sessionLockedMessage" to "Por segurança, sua sessão foi bloqueada devido à inatividade.",
        "unlock" to "DESBLOQUEAR",
        "logoutNow" to "Sair agora",
        "biometricAfterLogin" to "A digital aparece depois do primeiro login salvo. Se tocar em Sair, a sessão salva é removida.",
        "biometricEnableDevice" to "Ative digital, face ou bloqueio de tela no aparelho para usar este acesso.",
        "biometricButton" to "ENTRAR COM DIGITAL",
        "biometricSavedHelp" to "Use a digital para desbloquear a sessão salva.",
        "biometricFirstLoginHelp" to "Entre uma vez e feche o app sem tocar em Sair para liberar a digital.",
        "verifyEmailLink" to "Verificar e-mail",
        "forgotPassword" to "Esqueci a senha",
        "resendCodeEmail" to "Reenviar código por e-mail",
        "apiServer" to "Servidor da API",
        "hideConfig" to "Ocultar config",
        "serverConfig" to "Configuração do servidor",
        "close" to "Fechar",
        "pixInstruction" to "Instrução Pix",
        "pixCodeForTransfer" to "Chave Pix aleatória para esta transferência:",
        "pixCodeCopied" to "Chave Pix copiada!",
        "identifier" to "Identificador",
        "pixCode" to "Chave Pix aleatória",
        "protectedPixCodeTitle" to "Chave Pix aleatória",
        "protectedPixCodeHelp" to "Por privacidade, a chave completa não aparece na tela. Use o botão para copiar e pagar no banco.",
        "codeUnavailable" to "Código indisponível",
        "copyCode" to "Copiar chave",
        "imageSaved" to "Imagem salva na galeria!",
        "imageSaveError" to "Erro ao salvar imagem",
        "datePlaceholder" to "DD/MM/AAAA",
        "enterDistributionValue" to "Informe um valor válido para distribuir.",
        "noOpenRequests" to "Não há solicitações abertas de outras pessoas no momento.",
        "biometricTitle" to "Entrar com digital",
        "biometricSubtitle" to "Desbloqueie sua sessão Nexora salva",
        "registerCreatedDev" to "Cadastro criado. Código dev",
        "emailVerifiedLogin" to "E-mail verificado. Entrada realizada.",
        "newCodeSent" to "Se o cadastro existir, um novo código será enviado.",
        "recoverySent" to "Se o e-mail existir, enviaremos instruções de recuperação.",
        "passwordUpdatedSignedIn" to "Senha atualizada. Entrada realizada.",
        "passwordUpdatedLoginAgain" to "Senha atualizada. Entre novamente com a nova senha.",
        "requestSentReview" to "Solicitação enviada para validação manual.",
        "pixCodeGenerated" to "Código Pix gerado.",
        "pixSplitGenerated" to "Pix fracionado por ordem cronológica.",
        "photoAttachedWait" to "Foto anexada. Aguarde validação administrativa.",
        "userApproved" to "Usuário aprovado.",
        "userBlocked" to "Usuário bloqueado.",
        "userUnblocked" to "Usuário desbloqueado.",
        "feeSettled" to "Taxa baixada.",
        "requestApproved" to "Solicitação aprovada.",
        "requestRejected" to "Solicitação recusada.",
        "returnValidated" to "Retorno validado.",
        "supportValidated" to "Apoio validado.",
        "supportRejected" to "Apoio recusado.",
        "loggingOut" to "Saindo...",
        "refreshingPanel" to "Atualizando painel",
        "refreshingCommunity" to "Atualizando comunidade",
        "refreshingProfile" to "Atualizando perfil",
        "refreshingAdmin" to "Atualizando admin",
        "noOpenRequestsForDistribution" to "Não há solicitações abertas de outras pessoas para distribuir esse valor. Se a única solicitação é sua, ela não pode receber seu próprio Pix.",
        "badCredentials" to "CPF/e-mail ou senha incorretos. Confira os dados e tente novamente.",
        "invalidCode" to "Código inválido ou expirado. Confira o e-mail ou solicite um novo código.",
        "sessionInvalid" to "Sessão inválida. Entre novamente.",
        "encryptedDataInvalid" to "Dados seguros da conta foram atualizados. Entre novamente.",
        "loginTitle" to "Entrar",
        "registerTitle" to "Cadastro",
        "verifyTitle" to "Verificar e-mail",
        "recoverTitle" to "Recuperar senha",
        "newPasswordTitle" to "Nova senha",
        "loginSubtitle" to "Acesse com CPF ou e-mail",
        "registerSubtitle" to "Preencha seus dados para começar",
        "verifySubtitle" to "Digite o código enviado ao e-mail",
        "recoverSubtitle" to "Informe o e-mail para receber um código",
        "newPasswordSubtitle" to "Defina e confirme a nova senha",
        "recoverCodeSubtitle" to "Digite o código recebido por e-mail",
        "fullName" to "Nome completo",
        "email" to "E-mail",
        "birthdate" to "Data de nascimento",
        "pixKey" to "Chave Pix aleatória",
        "pixPlaceholder" to "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx",
        "inviteCode" to "Código de convite",
        "cpfOrEmail" to "CPF ou e-mail",
        "code" to "Código",
        "newPassword" to "Nova senha",
        "confirmNewPassword" to "Confirmar nova senha",
        "password" to "Senha",
        "loginButton" to "ENTRAR",
        "registerButton" to "CRIAR CONTA",
        "verifyButton" to "VERIFICAR",
        "sendCodeButton" to "ENVIAR CÓDIGO",
        "updatePasswordButton" to "ATUALIZAR SENHA",
        "enterCodeButton" to "DIGITE O CÓDIGO",
        "noAccount" to "Ainda não tem conta?",
        "hasAccount" to "Já tem conta?",
        "noCode" to "Não recebeu o código?",
        "rememberedPassword" to "Lembrou a senha?",
        "passwordUpdatedQuestion" to "Senha atualizada?",
        "signUp" to "Cadastre-se",
        "back" to "Voltar",
        "identifierRequired" to "Informe seu CPF ou e-mail.",
        "passwordRequired" to "Informe sua senha.",
        "nameRequired" to "Informe seu nome completo.",
        "emailInvalid" to "Informe um e-mail válido.",
        "cpfRequired" to "Informe um CPF com 11 dígitos.",
        "birthdateRequired" to "Informe sua data de nascimento.",
        "birthdateInvalid" to "Data de nascimento inválida. Use DD/MM/AAAA.",
        "minimumAge" to "Precisa ter pelo menos 13 anos para se cadastrar.",
        "pixRequired" to "Informe sua chave Pix aleatória.",
        "pixRandomRequired" to "Use apenas a chave Pix aleatória gerada pelo banco. CPF, e-mail e telefone não são aceitos.",
        "passwordMin" to "A senha precisa ter pelo menos 8 caracteres.",
    )
    val es = mapOf(
        "language" to "Idioma",
        "tabPanel" to "Panel",
        "tabCommunity" to "Comunidad",
        "tabRequest" to "Solicitar",
        "tabReturns" to "Devolver",
        "tabProfile" to "Perfil",
        "returnsTitle" to "Devoluciones",
        "pendingReturnsTitle" to "Tienes devoluciones pendientes",
        "overdueReturnsTitle" to "Tienes devoluciones atrasadas",
        "returnsSubtitle" to "Montos, destinatarios, fechas y comprobantes en un solo lugar",
        "pendingReturns" to "PENDIENTES",
        "pendingValue" to "VALOR PENDIENTE",
        "nextDue" to "Próximo vencimiento",
        "overduePenalty" to "Las nuevas solicitudes quedan bloqueadas hasta regularizar las devoluciones atrasadas.",
        "needToReturn" to "Debo devolver",
        "toReceive" to "Debo recibir",
        "noReturns" to "Sin devoluciones",
        "noReturnsHelp" to "Cuando una solicitud quede totalmente financiada, cada destinatario aparecerá aquí.",
        "nothingToReceive" to "No hay devoluciones para confirmar.",
        "returnTo" to "Devolver a",
        "receiveFrom" to "Recibir de",
        "returnStatus" to "Estado",
        "returnDue" to "Fecha límite",
        "returnPending" to "Pago pendiente",
        "returnProofSent" to "Comprobante enviado",
        "returnConfirmed" to "Devolución confirmada",
        "waitingFunding" to "Esperando financiación completa",
        "daysLate" to "día(s) de atraso",
        "daysRemaining" to "día(s) restantes",
        "destinationPix" to "Pix del destinatario",
        "copyReturnPix" to "Copiar Pix para devolver",
        "waitingReturnConfirmation" to "Esperando que el destinatario confirme la recepción.",
        "confirmReturnReceipt" to "Confirmar que recibí",
        "returnTransactionId" to "ID de la transacción Pix",
        "attachReturnProof" to "Adjuntar comprobante",
        "sendReturnProof" to "Enviar comprobante",
        "returnProofRequired" to "Informa el ID de la transacción y adjunta el comprobante.",
        "tabAdmin" to "Admin",
        "tutorialFaq" to "Tutorial y FAQ",
        "tutorialTitle" to "Primeros pasos",
        "tutorialIntro" to "Una guía corta para usar Nexora sin complicarte.",
        "tutorialStepPanel" to "Panel: mira tu liquidez, límite, nivel e historial en un solo lugar.",
        "tutorialStepCommunity" to "Comunidad: genera Pix por orden cronológico para apoyar solicitudes abiertas.",
        "tutorialStepProfile" to "Perfil: comparte solo tu código, sube comprobantes y revisa tus tasas.",
        "faqTitle" to "Preguntas frecuentes",
        "faqInviteQuestion" to "¿Qué copio para invitar a alguien?",
        "faqInviteAnswer" to "Copia solo el código de invitación. El enlace queda para compartir la página o instalar la app.",
        "faqPixQuestion" to "¿Qué Pix debo usar?",
        "faqPixAnswer" to "Usa siempre la clave Pix aleatoria registrada por la persona solicitante. Su nombre no aparece en la app.",
        "faqReceiptQuestion" to "¿Cuándo envío el comprobante?",
        "faqReceiptAnswer" to "Después de la transferencia, sube la foto del comprobante en el historial para validación.",
        "contactTitle" to "Contacto",
        "contactBody" to "Para dudas simples o soporte, usa los contactos de abajo. Toca para copiar.",
        "phoneContact" to "Teléfono",
        "contactCopied" to "¡Contacto copiado!",
        "downloadHint" to "También puedes abrir la página de Nexora para acceder o instalar la aplicación.",
        "openDownloadPage" to "Abrir página",
        "startUsing" to "EMPEZAR",
        "skipTutorial" to "Salir del tutorial",
        "inviteShareText" to "Entra a Nexora con mi código {code}. Accede o instala desde la página: {link}",
        "communityTitle" to "Comunidad",
        "communitySubtitle" to "Solicitudes activas para apoyar",
        "communityPixOrder" to "Pix por orden cronológico",
        "totalValue" to "Valor total",
        "generate" to "GENERAR",
        "communityEmptyTitle" to "Comunidad estable",
        "communityEmptySubtitle" to "No hay solicitudes activas de otros usuarios en este momento",
        "requestTitle" to "Solicitar apoyo",
        "requestSubtitle" to "La comunidad contribuye por Pix hasta completar el valor",
        "profileTitle" to "Perfil",
        "adminTitle" to "Admin Nexora",
        "adminSubtitle" to "Panel de control de fundadores",
        "buff" to "BUFF",
        "limit" to "LÍMITE",
        "invites" to "INVITACIONES",
        "myRequests" to "Mis solicitudes",
        "noRequestsTitle" to "Sin solicitudes",
        "noRequestsSubtitle" to "Cuando crees una solicitud, aparecerá aquí",
        "transactionHistory" to "Historial de transacciones",
        "active" to "Activas",
        "cancelled" to "Canceladas",
        "all" to "Todas",
        "noTransactionsTitle" to "Sin transacciones",
        "noTransactionsSubtitle" to "No hay transacciones en esta categoría",
        "statusNoReceipts" to "SIN COMPROBANTES",
        "statusWaitingReceiverPhoto" to "ESPERANDO FOTO DEL RECEPTOR",
        "statusWaitingDonorPhoto" to "ESPERANDO FOTO DEL DONADOR",
        "statusMatch" to "SISTEMA: DATOS COINCIDEN",
        "statusMismatch" to "SISTEMA: DIVERGENCIA ENCONTRADA",
        "statusReview" to "SISTEMA: REVISIÓN MANUAL",
        "statusAnalyzing" to "ANÁLISIS EN CURSO",
        "statusNoReceiptsHelp" to "Todavía no hay fotos anexadas para comparar.",
        "statusWaitingReceiverHelp" to "Falta la foto de quien recibió el Pix.",
        "statusWaitingDonorHelp" to "Falta la foto de quien envió el Pix.",
        "statusWaitingBothHelp" to "El sistema solo compara los datos cuando se envían los dos comprobantes.",
        "donorReceipt" to "Comprobante del donador",
        "receiverReceipt" to "Comprobante del receptor",
        "noPhotoAttached" to "Sin foto anexada",
        "autoRead" to "Lectura automática",
        "receiptAmount" to "Valor",
        "ocrConfidence" to "Confianza",
        "receiptDate" to "Fecha del comprobante",
        "sentAt" to "Enviado el",
        "receiptIntegrityOk" to "Comprobante anexado",
        "sentTo" to "Enviado a",
        "receivedFrom" to "Recibido de",
        "transactionId" to "ID de transacción",
        "waitingReceipt" to "esperando comprobante",
        "sentReceipt" to "Envío",
        "receivedReceipt" to "Recepción",
        "attachedPhoto" to "foto anexada",
        "pending" to "pendiente",
        "tagline" to "Karma que conecta personas",
        "levelShort" to "Nv",
        "level" to "Nivel",
        "communityLiquidity" to "LIQUIDEZ COMUNITARIA",
        "inCirculation" to "En circulación",
        "completion" to "Cumplimiento",
        "activeRequests" to "Solicitudes activas",
        "completedOperations" to "Operaciones completadas",
        "activeUsers" to "Usuarios activos",
        "yourLimit" to "Tu límite",
        "yourLimitUpper" to "TU LÍMITE",
        "howItWorks" to "Cómo funciona",
        "howItWorksBody" to "La comunidad contribuye por Pix hasta completar tu solicitud. Quien apoya recibe 1 XP por real; quien devuelve acumula buff y mantiene el límite saludable.",
        "requestLocked" to "Solicitud liberada desde el Nivel 2 con 100 XP y límite disponible.",
        "requestedValue" to "Valor solicitado",
        "returnDeadlineDays" to "Plazo de retorno (días)",
        "optionalDescription" to "Descripción opcional",
        "requestSupportButton" to "SOLICITAR APOYO",
        "requestLockedShort" to "Aún no puedes solicitar: necesitas estar en Nivel 2 con 100 XP.",
        "enterValidValue" to "Informa un valor válido.",
        "minimumContribution" to "Donación mínima de R$ 5,00.",
        "valueAboveLimit" to "Valor por encima de tu límite actual.",
        "enterDeadlineDays" to "Informa el plazo en días.",
        "deadlineRange" to "El plazo debe estar entre 1 y 30 días.",
        "pixCopied" to "¡Clave Pix copiada!",
        "inviteCopied" to "¡Código de invitación copiado!",
        "copyLink" to "Copiar enlace",
        "copy" to "Copiar",
        "shareInvite" to "Compartir invitación",
        "share" to "Compartir",
        "adminFeeAccumulated" to "Tasa administrativa acumulada",
        "sendAdminFeeHelp" to "Envía la tasa a la cuenta PJ de Nexora y espera la baja administrativa.",
        "adminFeePix" to "Pix de la tasa administrativa (CNPJ PJ)",
        "noPendingFee" to "Sin tasa pendiente para enviar.",
        "logout" to "Salir",
        "wait" to "Espera",
        "refresh" to "Actualizar",
        "adminUsers" to "Usuarios",
        "adminRequests" to "Solicitudes",
        "adminContributions" to "Apoyos",
        "noUsersTitle" to "Sin usuarios",
        "noUsersSubtitle" to "No se encontraron usuarios con los filtros actuales",
        "noAdminRequestsTitle" to "Sin solicitudes",
        "noAdminRequestsSubtitle" to "No se encontraron solicitudes con los filtros actuales",
        "noContributionsTitle" to "Sin apoyos",
        "noContributionsSubtitle" to "No se encontraron apoyos con los filtros actuales",
        "search" to "Buscar...",
        "status" to "Estado",
        "receipts" to "Comprobantes",
        "completePlural" to "Completos",
        "pendingPlural" to "Pendientes",
        "period" to "Período (AAAA-MM-DD)",
        "from" to "Desde",
        "to" to "Hasta",
        "viewDetails" to "Ver detalles",
        "fullUserData" to "Datos completos del usuario",
        "role" to "Función",
        "accumulatedFee" to "Tasa acumulada",
        "adminRandomPix" to "Pix administrativo de la cuenta PJ",
        "invite" to "Invitación",
        "invitedBy" to "Invitado por",
        "invitedUsers" to "Invitados",
        "createdAt" to "Creado el",
        "requestDetails" to "Detalles de la solicitud",
        "requester" to "Solicitante",
        "value" to "Valor",
        "adminFee" to "Tasa administrativa",
        "deadline" to "Plazo",
        "description" to "Descripción",
        "linkedContributions" to "Apoyos vinculados",
        "noLinkedContributions" to "No hay apoyos para esta solicitud.",
        "pixSupportDetails" to "Detalles del apoyo Pix",
        "support" to "Apoyo",
        "request" to "Solicitud",
        "donor" to "Donador",
        "receiver" to "Receptor",
        "pendingFee" to "Tasa pendiente",
        "approve" to "Aprobar",
        "activate" to "Activar",
        "block" to "Bloquear",
        "settleFee" to "Dar baja tasa",
        "reject" to "Rechazar",
        "validateReturn" to "Validar retorno",
        "receipt" to "Recibo",
        "photo" to "foto",
        "validate" to "Validar",
        "waitingPhotos" to "Esperando fotos",
        "confirm" to "Confirmar",
        "cancel" to "Cancelar",
        "confirmAction" to "Confirmar acción",
        "expansion" to "Expansión",
        "stage" to "Etapa",
        "currentCapacity" to "Capacidad actual",
        "participants" to "participantes",
        "activeRequest" to "Solicitud activa",
        "chronologicalHelp" to "Será atendida por el botón principal de Pix en orden cronológico.",
        "waitingSupporters" to "Esperando apoyadores",
        "hide" to "Ocultar",
        "attach" to "Anexar",
        "photoAttachError" to "No fue posible anexar la foto.",
        "attachPhoto" to "Anexar foto",
        "photoReady" to "Foto lista",
        "attachReceiptPhoto" to "Anexa la foto del comprobante.",
        "send" to "Enviar",
        "day" to "día",
        "days" to "días",
        "superAdmin" to "Super admin",
        "adminRole" to "Admin",
        "userRole" to "Usuario",
        "registrationEmailRequired" to "Informa el e-mail usado en el registro.",
        "sixDigitCodeRequired" to "Escribe el código de 6 dígitos.",
        "accountEmailRequired" to "Informa el e-mail de la cuenta.",
        "newPasswordMin" to "La nueva contraseña debe tener al menos 8 caracteres.",
        "passwordsDontMatch" to "Las contraseñas no coinciden.",
        "sessionLockedMessage" to "Por seguridad, tu sesión fue bloqueada por inactividad.",
        "unlock" to "DESBLOQUEAR",
        "logoutNow" to "Salir ahora",
        "biometricAfterLogin" to "La huella aparece después del primer login guardado. Si tocas Salir, la sesión guardada se elimina.",
        "biometricEnableDevice" to "Activa huella, rostro o bloqueo de pantalla en el dispositivo para usar este acceso.",
        "biometricButton" to "ENTRAR CON HUELLA",
        "biometricSavedHelp" to "Usa la huella para desbloquear la sesión guardada.",
        "biometricFirstLoginHelp" to "Entra una vez y cierra la app sin tocar Salir para liberar la huella.",
        "verifyEmailLink" to "Verificar e-mail",
        "forgotPassword" to "Olvidé la contraseña",
        "resendCodeEmail" to "Reenviar código por e-mail",
        "apiServer" to "Servidor de API",
        "hideConfig" to "Ocultar config",
        "serverConfig" to "Configuración del servidor",
        "close" to "Cerrar",
        "pixInstruction" to "Instrucción Pix",
        "pixCodeForTransfer" to "Clave Pix aleatoria para esta transferencia:",
        "pixCodeCopied" to "¡Clave Pix copiada!",
        "identifier" to "Identificador",
        "pixCode" to "Clave Pix aleatoria",
        "protectedPixCodeTitle" to "Clave Pix aleatoria",
        "protectedPixCodeHelp" to "Por privacidad, la clave completa no aparece en pantalla. Usa el botón para copiar y pagar en el banco.",
        "codeUnavailable" to "Código no disponible",
        "copyCode" to "Copiar clave",
        "imageSaved" to "¡Imagen guardada en la galería!",
        "imageSaveError" to "Error al guardar imagen",
        "datePlaceholder" to "DD/MM/AAAA",
        "enterDistributionValue" to "Informa un valor válido para distribuir.",
        "noOpenRequests" to "No hay solicitudes abiertas de otras personas en este momento.",
        "biometricTitle" to "Entrar con huella",
        "biometricSubtitle" to "Desbloquea tu sesión Nexora guardada",
        "registerCreatedDev" to "Registro creado. Código dev",
        "emailVerifiedLogin" to "E-mail verificado. Entrada realizada.",
        "newCodeSent" to "Si el registro existe, se enviará un nuevo código.",
        "recoverySent" to "Si el e-mail existe, enviaremos instrucciones de recuperación.",
        "passwordUpdatedSignedIn" to "Contraseña actualizada. Entrada realizada.",
        "passwordUpdatedLoginAgain" to "Contraseña actualizada. Entra otra vez con la nueva contraseña.",
        "requestSentReview" to "Solicitud enviada para validación manual.",
        "pixCodeGenerated" to "Código Pix generado.",
        "pixSplitGenerated" to "Pix fraccionado por orden cronológico.",
        "photoAttachedWait" to "Foto anexada. Espera la validación administrativa.",
        "userApproved" to "Usuario aprobado.",
        "userBlocked" to "Usuario bloqueado.",
        "userUnblocked" to "Usuario desbloqueado.",
        "feeSettled" to "Tasa dada de baja.",
        "requestApproved" to "Solicitud aprobada.",
        "requestRejected" to "Solicitud rechazada.",
        "returnValidated" to "Retorno validado.",
        "supportValidated" to "Apoyo validado.",
        "supportRejected" to "Apoyo rechazado.",
        "loggingOut" to "Saliendo...",
        "refreshingPanel" to "Actualizando panel",
        "refreshingCommunity" to "Actualizando comunidad",
        "refreshingProfile" to "Actualizando perfil",
        "refreshingAdmin" to "Actualizando admin",
        "noOpenRequestsForDistribution" to "No hay solicitudes abiertas de otras personas para distribuir ese valor. Si la única solicitud es tuya, no puede recibir tu propio Pix.",
        "badCredentials" to "CPF/e-mail o contraseña incorrectos. Revisa los datos e intenta de nuevo.",
        "invalidCode" to "Código inválido o expirado. Revisa el e-mail o solicita un nuevo código.",
        "sessionInvalid" to "Sesión inválida. Entra nuevamente.",
        "encryptedDataInvalid" to "Los datos seguros de la cuenta fueron actualizados. Entra de nuevo.",
        "loginTitle" to "Entrar",
        "registerTitle" to "Registro",
        "verifyTitle" to "Verificar e-mail",
        "recoverTitle" to "Recuperar contraseña",
        "newPasswordTitle" to "Nueva contraseña",
        "loginSubtitle" to "Accede con CPF o e-mail",
        "registerSubtitle" to "Completa tus datos para empezar",
        "verifySubtitle" to "Escribe el código enviado al e-mail",
        "recoverSubtitle" to "Informa el e-mail para recibir un código",
        "newPasswordSubtitle" to "Define y confirma la nueva contraseña",
        "recoverCodeSubtitle" to "Escribe el código recibido por e-mail",
        "fullName" to "Nombre completo",
        "email" to "E-mail",
        "birthdate" to "Fecha de nacimiento",
        "pixKey" to "Clave Pix aleatoria",
        "pixPlaceholder" to "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx",
        "inviteCode" to "Código de invitación",
        "cpfOrEmail" to "CPF o e-mail",
        "code" to "Código",
        "newPassword" to "Nueva contraseña",
        "confirmNewPassword" to "Confirmar nueva contraseña",
        "password" to "Contraseña",
        "loginButton" to "ENTRAR",
        "registerButton" to "CREAR CUENTA",
        "verifyButton" to "VERIFICAR",
        "sendCodeButton" to "ENVIAR CÓDIGO",
        "updatePasswordButton" to "ACTUALIZAR CONTRASEÑA",
        "enterCodeButton" to "ESCRIBE EL CÓDIGO",
        "noAccount" to "¿Aún no tienes cuenta?",
        "hasAccount" to "¿Ya tienes cuenta?",
        "noCode" to "¿No recibiste el código?",
        "rememberedPassword" to "¿Recordaste la contraseña?",
        "passwordUpdatedQuestion" to "¿Contraseña actualizada?",
        "signUp" to "Registrarse",
        "back" to "Volver",
        "identifierRequired" to "Informa tu CPF o e-mail.",
        "passwordRequired" to "Informa tu contraseña.",
        "nameRequired" to "Informa tu nombre completo.",
        "emailInvalid" to "Informa un e-mail válido.",
        "cpfRequired" to "Informa un CPF con 11 dígitos.",
        "birthdateRequired" to "Informa tu fecha de nacimiento.",
        "birthdateInvalid" to "Fecha de nacimiento inválida. Usa DD/MM/AAAA.",
        "minimumAge" to "Debes tener al menos 13 años para registrarte.",
        "pixRequired" to "Informa tu clave Pix aleatoria.",
        "pixRandomRequired" to "Usa solo la clave Pix aleatoria generada por el banco. CPF, e-mail y teléfono no se aceptan.",
        "passwordMin" to "La contraseña debe tener al menos 8 caracteres.",
    )
    val en = mapOf(
        "language" to "Language",
        "tabPanel" to "Panel",
        "tabCommunity" to "Community",
        "tabRequest" to "Request",
        "tabReturns" to "Return",
        "tabProfile" to "Profile",
        "returnsTitle" to "Repayments",
        "pendingReturnsTitle" to "You have pending repayments",
        "overdueReturnsTitle" to "You have overdue repayments",
        "returnsSubtitle" to "Amounts, recipients, deadlines, and receipts in one place",
        "pendingReturns" to "PENDING",
        "pendingValue" to "PENDING AMOUNT",
        "nextDue" to "Next due date",
        "overduePenalty" to "New requests are blocked until overdue repayments are settled.",
        "needToReturn" to "I need to repay",
        "toReceive" to "I am owed",
        "noReturns" to "No repayments",
        "noReturnsHelp" to "Once a request is fully funded, each recipient will appear here.",
        "nothingToReceive" to "No repayments to confirm.",
        "returnTo" to "Repay",
        "receiveFrom" to "Receive from",
        "returnStatus" to "Status",
        "returnDue" to "Due date",
        "returnPending" to "Payment pending",
        "returnProofSent" to "Receipt submitted",
        "returnConfirmed" to "Repayment confirmed",
        "waitingFunding" to "Waiting for full funding",
        "daysLate" to "day(s) overdue",
        "daysRemaining" to "day(s) remaining",
        "destinationPix" to "Recipient Pix",
        "copyReturnPix" to "Copy Pix to repay",
        "waitingReturnConfirmation" to "Waiting for the recipient to confirm receipt.",
        "confirmReturnReceipt" to "Confirm receipt",
        "returnTransactionId" to "Pix transaction ID",
        "attachReturnProof" to "Attach receipt",
        "sendReturnProof" to "Send receipt",
        "returnProofRequired" to "Enter the transaction ID and attach the receipt.",
        "tabAdmin" to "Admin",
        "tutorialFaq" to "Tutorial and FAQ",
        "tutorialTitle" to "First steps",
        "tutorialIntro" to "A short guide to use Nexora without friction.",
        "tutorialStepPanel" to "Panel: see your liquidity, limit, level, and history in one place.",
        "tutorialStepCommunity" to "Community: generate Pix by chronological order to support open requests.",
        "tutorialStepProfile" to "Profile: share only your code, upload receipts, and track your fees.",
        "faqTitle" to "Frequently Asked Questions",
        "faqInviteQuestion" to "What do I copy to invite someone?",
        "faqInviteAnswer" to "Copy only the invite code. The link is for sharing the page or installing the app.",
        "faqPixQuestion" to "Which Pix should I use?",
        "faqPixAnswer" to "Always use the random Pix key registered by the requester. Their name is not shown in the app.",
        "faqReceiptQuestion" to "When do I send the receipt?",
        "faqReceiptAnswer" to "After the transfer, upload the receipt photo in history for validation.",
        "contactTitle" to "Contact",
        "contactBody" to "For simple questions or support, use the contacts below. Tap to copy.",
        "phoneContact" to "Phone",
        "contactCopied" to "Contact copied!",
        "downloadHint" to "You can also open the Nexora page to access or install the application.",
        "openDownloadPage" to "Open page",
        "startUsing" to "START",
        "skipTutorial" to "Exit tutorial",
        "inviteShareText" to "Join Nexora with my code {code}. Access or install it from the page: {link}",
        "communityTitle" to "Community",
        "communitySubtitle" to "Active requests to support",
        "communityPixOrder" to "Pix by chronological order",
        "totalValue" to "Total value",
        "generate" to "GENERATE",
        "communityEmptyTitle" to "Stable community",
        "communityEmptySubtitle" to "No active requests from other users right now",
        "requestTitle" to "Request Support",
        "requestSubtitle" to "The community contributes through Pix until the amount is complete",
        "profileTitle" to "Profile",
        "adminTitle" to "Admin Nexora",
        "adminSubtitle" to "Founder control panel",
        "buff" to "BUFF",
        "limit" to "LIMIT",
        "invites" to "INVITES",
        "myRequests" to "My requests",
        "noRequestsTitle" to "No requests",
        "noRequestsSubtitle" to "When you create a request, it will appear here",
        "transactionHistory" to "Transaction history",
        "active" to "Active",
        "cancelled" to "Cancelled",
        "all" to "All",
        "noTransactionsTitle" to "No transactions",
        "noTransactionsSubtitle" to "No transactions found in this category",
        "statusNoReceipts" to "NO RECEIPTS",
        "statusWaitingReceiverPhoto" to "WAITING FOR RECEIVER PHOTO",
        "statusWaitingDonorPhoto" to "WAITING FOR DONOR PHOTO",
        "statusMatch" to "SYSTEM: DATA MATCHES",
        "statusMismatch" to "SYSTEM: MISMATCH FOUND",
        "statusReview" to "SYSTEM: MANUAL REVIEW",
        "statusAnalyzing" to "ANALYSIS IN PROGRESS",
        "statusNoReceiptsHelp" to "There are no attached photos to compare yet.",
        "statusWaitingReceiverHelp" to "The receiver's Pix photo is still missing.",
        "statusWaitingDonorHelp" to "The sender's Pix photo is still missing.",
        "statusWaitingBothHelp" to "The system compares data only after both receipts are sent.",
        "donorReceipt" to "Donor receipt",
        "receiverReceipt" to "Receiver receipt",
        "noPhotoAttached" to "No photo attached",
        "autoRead" to "Automatic reading",
        "receiptAmount" to "Amount",
        "ocrConfidence" to "Confidence",
        "receiptDate" to "Receipt date",
        "sentAt" to "Sent at",
        "receiptIntegrityOk" to "Receipt attached",
        "sentTo" to "Sent to",
        "receivedFrom" to "Received from",
        "transactionId" to "Transaction ID",
        "waitingReceipt" to "waiting for receipt",
        "sentReceipt" to "Sent",
        "receivedReceipt" to "Received",
        "attachedPhoto" to "photo attached",
        "pending" to "pending",
        "tagline" to "Karma that connects people",
        "levelShort" to "Lv",
        "level" to "Level",
        "communityLiquidity" to "COMMUNITY LIQUIDITY",
        "inCirculation" to "In circulation",
        "completion" to "Completion",
        "activeRequests" to "Active requests",
        "completedOperations" to "Completed operations",
        "activeUsers" to "Active users",
        "yourLimit" to "Your limit",
        "yourLimitUpper" to "YOUR LIMIT",
        "howItWorks" to "How it works",
        "howItWorksBody" to "The community contributes through Pix until your request is complete. Supporters earn 1 XP per real; returning funds builds the cumulative buff and keeps limits healthy.",
        "requestLocked" to "Requests unlock at Level 2 with 100 XP and available limit.",
        "requestedValue" to "Requested amount",
        "returnDeadlineDays" to "Return deadline (days)",
        "optionalDescription" to "Optional description",
        "requestSupportButton" to "REQUEST SUPPORT",
        "requestLockedShort" to "You cannot request yet: you need Level 2 with 100 XP.",
        "enterValidValue" to "Enter a valid amount.",
        "minimumContribution" to "Minimum contribution is R$ 5.00.",
        "valueAboveLimit" to "Amount above your current limit.",
        "enterDeadlineDays" to "Enter the deadline in days.",
        "deadlineRange" to "The deadline must be between 1 and 30 days.",
        "pixCopied" to "Pix key copied!",
        "inviteCopied" to "Invite code copied!",
        "copyLink" to "Copy link",
        "copy" to "Copy",
        "shareInvite" to "Share invite",
        "share" to "Share",
        "adminFeeAccumulated" to "Accumulated admin fee",
        "sendAdminFeeHelp" to "Send the fee to Nexora's business account and wait for admin clearance.",
        "adminFeePix" to "Admin fee Pix (business CNPJ)",
        "noPendingFee" to "No pending fee to send.",
        "logout" to "Log out",
        "wait" to "Wait",
        "refresh" to "Refresh",
        "adminUsers" to "Users",
        "adminRequests" to "Requests",
        "adminContributions" to "Supports",
        "noUsersTitle" to "No users",
        "noUsersSubtitle" to "No users found with the current filters",
        "noAdminRequestsTitle" to "No requests",
        "noAdminRequestsSubtitle" to "No requests found with the current filters",
        "noContributionsTitle" to "No supports",
        "noContributionsSubtitle" to "No supports found with the current filters",
        "search" to "Search...",
        "status" to "Status",
        "receipts" to "Receipts",
        "completePlural" to "Complete",
        "pendingPlural" to "Pending",
        "period" to "Period (YYYY-MM-DD)",
        "from" to "From",
        "to" to "To",
        "viewDetails" to "View details",
        "fullUserData" to "Full user data",
        "role" to "Role",
        "accumulatedFee" to "Accumulated fee",
        "adminRandomPix" to "Business account admin Pix",
        "invite" to "Invite",
        "invitedBy" to "Invited by",
        "invitedUsers" to "Invited users",
        "createdAt" to "Created at",
        "requestDetails" to "Request details",
        "requester" to "Requester",
        "value" to "Amount",
        "adminFee" to "Admin fee",
        "deadline" to "Deadline",
        "description" to "Description",
        "linkedContributions" to "Linked supports",
        "noLinkedContributions" to "No supports for this request.",
        "pixSupportDetails" to "Pix support details",
        "support" to "Support",
        "request" to "Request",
        "donor" to "Donor",
        "receiver" to "Receiver",
        "pendingFee" to "Pending fee",
        "approve" to "Approve",
        "activate" to "Activate",
        "block" to "Block",
        "settleFee" to "Settle fee",
        "reject" to "Reject",
        "validateReturn" to "Validate return",
        "receipt" to "Receipt",
        "photo" to "photo",
        "validate" to "Validate",
        "waitingPhotos" to "Waiting for photos",
        "confirm" to "Confirm",
        "cancel" to "Cancel",
        "confirmAction" to "Confirm action",
        "expansion" to "Expansion",
        "stage" to "Stage",
        "currentCapacity" to "Current capacity",
        "participants" to "participants",
        "activeRequest" to "Active request",
        "chronologicalHelp" to "It will be handled by the main Pix button in chronological order.",
        "waitingSupporters" to "Waiting for supporters",
        "hide" to "Hide",
        "attach" to "Attach",
        "photoAttachError" to "Could not attach the photo.",
        "attachPhoto" to "Attach photo",
        "photoReady" to "Photo ready",
        "attachReceiptPhoto" to "Attach the receipt photo.",
        "send" to "Send",
        "day" to "day",
        "days" to "days",
        "superAdmin" to "Super admin",
        "adminRole" to "Admin",
        "userRole" to "User",
        "registrationEmailRequired" to "Enter the email used at registration.",
        "sixDigitCodeRequired" to "Enter the 6-digit code.",
        "accountEmailRequired" to "Enter the account email.",
        "newPasswordMin" to "The new password must be at least 8 characters.",
        "passwordsDontMatch" to "Passwords do not match.",
        "sessionLockedMessage" to "For security, your session was locked due to inactivity.",
        "unlock" to "UNLOCK",
        "logoutNow" to "Log out now",
        "biometricAfterLogin" to "Fingerprint appears after the first saved login. If you tap Log out, the saved session is removed.",
        "biometricEnableDevice" to "Enable fingerprint, face, or screen lock on the device to use this access.",
        "biometricButton" to "SIGN IN WITH FINGERPRINT",
        "biometricSavedHelp" to "Use fingerprint to unlock the saved session.",
        "biometricFirstLoginHelp" to "Sign in once and close the app without tapping Log out to enable fingerprint.",
        "verifyEmailLink" to "Verify email",
        "forgotPassword" to "Forgot password",
        "resendCodeEmail" to "Resend code by email",
        "apiServer" to "API server",
        "hideConfig" to "Hide config",
        "serverConfig" to "Server configuration",
        "close" to "Close",
        "pixInstruction" to "Pix instruction",
        "pixCodeForTransfer" to "Random Pix key for this transfer:",
        "pixCodeCopied" to "Pix key copied!",
        "identifier" to "Identifier",
        "pixCode" to "Random Pix key",
        "protectedPixCodeTitle" to "Random Pix key",
        "protectedPixCodeHelp" to "For privacy, the full key is not shown on screen. Use the button to copy it and pay in your bank app.",
        "codeUnavailable" to "Code unavailable",
        "copyCode" to "Copy key",
        "imageSaved" to "Image saved to gallery!",
        "imageSaveError" to "Error saving image",
        "datePlaceholder" to "DD/MM/YYYY",
        "enterDistributionValue" to "Enter a valid amount to distribute.",
        "noOpenRequests" to "There are no open requests from other people right now.",
        "biometricTitle" to "Sign in with fingerprint",
        "biometricSubtitle" to "Unlock your saved Nexora session",
        "registerCreatedDev" to "Account created. Dev code",
        "emailVerifiedLogin" to "Email verified. Signed in.",
        "newCodeSent" to "If the account exists, a new code will be sent.",
        "recoverySent" to "If the email exists, we will send recovery instructions.",
        "passwordUpdatedSignedIn" to "Password updated. Signed in.",
        "passwordUpdatedLoginAgain" to "Password updated. Sign in again with the new password.",
        "requestSentReview" to "Request sent for manual validation.",
        "pixCodeGenerated" to "Pix code generated.",
        "pixSplitGenerated" to "Pix split by chronological order.",
        "photoAttachedWait" to "Photo attached. Wait for admin validation.",
        "userApproved" to "User approved.",
        "userBlocked" to "User blocked.",
        "userUnblocked" to "User unblocked.",
        "feeSettled" to "Fee settled.",
        "requestApproved" to "Request approved.",
        "requestRejected" to "Request rejected.",
        "returnValidated" to "Return validated.",
        "supportValidated" to "Support validated.",
        "supportRejected" to "Support rejected.",
        "loggingOut" to "Logging out...",
        "refreshingPanel" to "Refreshing panel",
        "refreshingCommunity" to "Refreshing community",
        "refreshingProfile" to "Refreshing profile",
        "refreshingAdmin" to "Refreshing admin",
        "noOpenRequestsForDistribution" to "There are no open requests from other people to distribute this amount. If the only request is yours, it cannot receive your own Pix.",
        "badCredentials" to "CPF/email or password is incorrect. Check the data and try again.",
        "invalidCode" to "Invalid or expired code. Check your email or request a new code.",
        "sessionInvalid" to "Invalid session. Sign in again.",
        "encryptedDataInvalid" to "Secure account data was updated. Sign in again.",
        "loginTitle" to "Sign in",
        "registerTitle" to "Create account",
        "verifyTitle" to "Verify email",
        "recoverTitle" to "Recover password",
        "newPasswordTitle" to "New password",
        "loginSubtitle" to "Use your CPF or email",
        "registerSubtitle" to "Fill in your details to start",
        "verifySubtitle" to "Enter the code sent to your email",
        "recoverSubtitle" to "Enter your email to receive a code",
        "newPasswordSubtitle" to "Set and confirm your new password",
        "recoverCodeSubtitle" to "Enter the code received by email",
        "fullName" to "Full name",
        "email" to "Email",
        "birthdate" to "Birth date",
        "pixKey" to "Random Pix key",
        "pixPlaceholder" to "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx",
        "inviteCode" to "Invite code",
        "cpfOrEmail" to "CPF or email",
        "code" to "Code",
        "newPassword" to "New password",
        "confirmNewPassword" to "Confirm new password",
        "password" to "Password",
        "loginButton" to "SIGN IN",
        "registerButton" to "CREATE ACCOUNT",
        "verifyButton" to "VERIFY",
        "sendCodeButton" to "SEND CODE",
        "updatePasswordButton" to "UPDATE PASSWORD",
        "enterCodeButton" to "ENTER CODE",
        "noAccount" to "No account yet?",
        "hasAccount" to "Already have an account?",
        "noCode" to "Did not receive the code?",
        "rememberedPassword" to "Remembered your password?",
        "passwordUpdatedQuestion" to "Password updated?",
        "signUp" to "Sign up",
        "back" to "Back",
        "identifierRequired" to "Enter your CPF or email.",
        "passwordRequired" to "Enter your password.",
        "nameRequired" to "Enter your full name.",
        "emailInvalid" to "Enter a valid email.",
        "cpfRequired" to "Enter an 11-digit CPF.",
        "birthdateRequired" to "Enter your birth date.",
        "birthdateInvalid" to "Invalid birth date. Use DD/MM/YYYY.",
        "minimumAge" to "You must be at least 13 years old to sign up.",
        "pixRequired" to "Enter your random Pix key.",
        "pixRandomRequired" to "Use only the bank-generated random Pix key. CPF, email, and phone are not accepted.",
        "passwordMin" to "Password must be at least 8 characters.",
    )
    return when (language) {
        AppLanguage.PT -> pt[key]
        AppLanguage.ES -> es[key]
        AppLanguage.EN -> en[key]
    } ?: pt[key] ?: key
}

private fun runtimeText(message: String, language: AppLanguage = NexoraLanguageStore.current): String {
    val normalized = message
        .lowercase()
        .replace("ã", "a")
        .replace("á", "a")
        .replace("à", "a")
        .replace("â", "a")
        .replace("é", "e")
        .replace("ê", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ô", "o")
        .replace("õ", "o")
        .replace("ú", "u")
        .replace("ç", "c")
    val key = when {
        normalized.contains("cadastro criado") && normalized.contains("codigo dev") -> "registerCreatedDev"
        normalized.contains("email verificado") || normalized.contains("e-mail verificado") -> "emailVerifiedLogin"
        normalized.contains("novo codigo") || normalized.contains("novo c") && normalized.contains("enviado") -> "newCodeSent"
        normalized.contains("instrucoes de recuperacao") || normalized.contains("instruc") && normalized.contains("recuper") -> "recoverySent"
        normalized.contains("senha atualizada") && normalized.contains("entrada realizada") -> "passwordUpdatedSignedIn"
        normalized.contains("senha atualizada") -> "passwordUpdatedLoginAgain"
        normalized.contains("solicitacao enviada") -> "requestSentReview"
        normalized.contains("codigo pix gerado") -> "pixCodeGenerated"
        normalized.contains("pix fracionado") -> "pixSplitGenerated"
        normalized.contains("foto anexada") -> "photoAttachedWait"
        normalized.contains("usuario aprovado") -> "userApproved"
        normalized.contains("usuario bloqueado") -> "userBlocked"
        normalized.contains("usuario desbloqueado") -> "userUnblocked"
        normalized.contains("taxa baixada") -> "feeSettled"
        normalized.contains("solicitacao aprovada") -> "requestApproved"
        normalized.contains("solicitacao recusada") -> "requestRejected"
        normalized.contains("retorno validado") -> "returnValidated"
        normalized.contains("apoio validado") -> "supportValidated"
        normalized.contains("apoio recusado") -> "supportRejected"
        normalized.contains("saindo") -> "loggingOut"
        normalized.contains("aguarde") -> "wait"
        normalized.contains("atualizando") && normalized.contains("painel") -> "refreshingPanel"
        normalized.contains("atualizando") && normalized.contains("comunidade") -> "refreshingCommunity"
        normalized.contains("atualizando") && normalized.contains("perfil") -> "refreshingProfile"
        normalized.contains("atualizando") && normalized.contains("admin") -> "refreshingAdmin"
        normalized.contains("nao ha solicitacoes") || normalized.contains("não há solicitações") -> "noOpenRequestsForDistribution"
        normalized.contains("cpf/e-mail") || normalized.contains("senha incorret") -> "badCredentials"
        normalized.contains("codigo invalido") || normalized.contains("código inválido") -> "invalidCode"
        normalized.contains("payload criptografado") ||
            normalized.contains("descriptografar dados") ||
            normalized.contains("dados seguros da conta") -> "encryptedDataInvalid"
        normalized.contains("sessao") || normalized.contains("sessão") || normalized.contains("token") -> "sessionInvalid"
        else -> null
    }
    val translated = key?.let { uiText(it, language) }
    return if (translated != null && key == "registerCreatedDev") {
        val code = Regex("(\\d{6})").find(message)?.value
        if (code != null) "$translated: $code" else translated
    } else {
        translated ?: message
    }
}

private fun statusLabel(status: String): String {
    val labels = when (NexoraLanguageStore.current) {
        AppLanguage.PT -> mapOf(
            "PENDING_REVIEW" to "aguardando admin",
            "PENDING_ADMIN" to "aguardando admin",
            "PENDING_RECEIPTS" to "aguardando comprovantes",
            "PENDING" to "pendente",
            "APPROVED" to "aprovado",
            "BLOCKED" to "bloqueado",
            "OPEN" to "ativo",
            "FUNDED" to "completo",
            "RETURNED" to "concluído",
            "CONFIRMED" to "validado",
            "REJECTED" to "recusado",
            "CANCELLED" to "cancelado",
            "EXPIRED" to "expirado",
        )
        AppLanguage.ES -> mapOf(
            "PENDING_REVIEW" to "en revisión",
            "PENDING_ADMIN" to "esperando admin",
            "PENDING_RECEIPTS" to "esperando comprobantes",
            "PENDING" to "pendiente",
            "APPROVED" to "aprobado",
            "BLOCKED" to "bloqueado",
            "OPEN" to "activo",
            "FUNDED" to "completo",
            "RETURNED" to "devuelto",
            "CONFIRMED" to "validado",
            "REJECTED" to "rechazado",
            "CANCELLED" to "cancelado",
            "EXPIRED" to "expirado",
        )
        AppLanguage.EN -> mapOf(
            "PENDING_REVIEW" to "under review",
            "PENDING_ADMIN" to "awaiting admin",
            "PENDING_RECEIPTS" to "awaiting receipts",
            "PENDING" to "pending",
            "APPROVED" to "approved",
            "BLOCKED" to "blocked",
            "OPEN" to "active",
            "FUNDED" to "complete",
            "RETURNED" to "returned",
            "CONFIRMED" to "validated",
            "REJECTED" to "rejected",
            "CANCELLED" to "cancelled",
            "EXPIRED" to "expired",
        )
    }

    return labels[status] ?: status.lowercase()
}

private fun formatDays(days: Int): String = "$days ${uiText(if (days == 1) "day" else "days")}"

private fun roleLabel(role: String): String = when (role.uppercase()) {
    "SUPER_ADMIN" -> uiText("superAdmin")
    "ADMIN" -> uiText("adminRole")
    "USER" -> uiText("userRole")
    else -> role
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(user.name, color = NexoraText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("${user.publicId} · ${user.email}", color = NexoraMuted, fontSize = 13.sp)
            }
            StatusPill(user.status)
        }
        
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFF222222))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LabelValue("CPF", user.cpf, compact = true)
            LabelValue("${uiText("levelShort")} / XP", "${user.level} / ${user.xp}", compact = true)
            LabelValue(uiText("buff"), formatBuff(user.buffBps), compact = true)
        }
        
        Spacer(Modifier.height(12.dp))
        LabelValue(uiText("pixKey"), user.pixKey, compact = true)
        
        if (user.adminFeeDueCents > 0) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(NexoraRed.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = NexoraRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("${uiText("pendingFee")}: ${formatMoney(user.adminFeeDueCents)}", color = NexoraRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailAction(onDetails)
            if (user.status == "PENDING_REVIEW") {
                SmallAction(uiText("approve"), NexoraGreen, loading = viewModel.state.actionInProgress == "/admin/users/${user.id}/approve") { viewModel.adminApproveUser(user.id) }
            }
            if (user.status == "BLOCKED") {
                SmallAction(uiText("activate"), NexoraGreen, loading = viewModel.state.actionInProgress == "/admin/users/${user.id}/unblock") { viewModel.adminUnblockUser(user.id) }
            } else {
                SmallAction(uiText("block"), NexoraRed, loading = viewModel.state.actionInProgress == "/admin/users/${user.id}/block") { viewModel.adminBlockUser(user.id) }
            }
            if (user.adminFeeDueCents > 0) {
                SmallAction(uiText("settleFee"), NexoraMuted, loading = viewModel.state.actionInProgress == "user-fee-${user.id}") { viewModel.adminConfirmFee(user.id) }
            }
        }
    }
}

@Composable
private fun AdminRequestCard(request: AdminSupportRequest, viewModel: NexoraViewModel, onDetails: () -> Unit) {
    NexoraPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(formatMoney(request.amountCents), color = NexoraGreen, fontSize = 22.sp, fontWeight = FontWeight.Black)
            StatusPill(request.status)
        }
        
        Spacer(Modifier.height(8.dp))
        Text(request.publicCode, color = NexoraMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)

        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFF222222))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(uiText("requester"), color = NexoraMuted, fontSize = 11.sp)
                Text(request.requesterName, color = NexoraText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(uiText("deadline"), color = NexoraMuted, fontSize = 11.sp)
                Text(formatDays(request.dueDays), color = NexoraText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        request.description?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Color(0xFFB8B8B8), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailAction(onDetails)
            if (request.status == "PENDING_ADMIN") {
                SmallAction(uiText("approve"), NexoraGreen, loading = viewModel.state.actionInProgress == "/admin/support-requests/${request.id}/approve") { viewModel.adminApproveRequest(request.id) }
                SmallAction(uiText("reject"), NexoraRed, loading = viewModel.state.actionInProgress == "/admin/support-requests/${request.id}/reject") { viewModel.adminRejectRequest(request.id) }
            }
            if (request.status == "FUNDED") {
                SmallAction(uiText("validateReturn"), NexoraGreen, loading = viewModel.state.actionInProgress == "/admin/support-requests/${request.id}/confirm-return") { viewModel.adminConfirmReturn(request.id) }
            }
        }
    }
}

@Composable
private fun AdminContributionCard(contribution: AdminContribution, viewModel: NexoraViewModel, onDetails: () -> Unit) {
    NexoraPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(formatMoney(contribution.amountCents), color = NexoraGreen, fontSize = 24.sp, fontWeight = FontWeight.Black)
            StatusPill(contribution.status)
        }
        
        Spacer(Modifier.height(8.dp))
        Text(contribution.requestPublicCode, color = NexoraMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFF222222))
        
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = NexoraMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${contribution.donorPublicId} -> ${contribution.receiverPublicId}",
                    color = NexoraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                "${contribution.donorName.ifBlank { uiText("donor") }} -> ${contribution.receiverName.ifBlank { uiText("receiver") }}",
                color = NexoraMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 24.dp)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = NexoraMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${uiText("transactionId")}: ${contribution.transactionId ?: uiText("pending")}",
                    color = NexoraText,
                    fontSize = 13.sp
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = if (contribution.evidenceComplete) NexoraGreen else NexoraMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${uiText("sentReceipt")}: ${if (contribution.hasSenderReceipt) uiText("photo") else uiText("pending")} · ${uiText("receipt")}: ${if (contribution.hasReceiverReceipt) uiText("photo") else uiText("pending")}",
                    color = if (contribution.evidenceComplete) NexoraGreen else NexoraMuted,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            DetailAction(onDetails)
            if (contribution.status == "PENDING_ADMIN") {
                SmallAction(uiText("validate"), NexoraGreen, loading = viewModel.state.actionInProgress == "contribution-confirm-${contribution.id}") { viewModel.adminConfirmContribution(contribution.id) }
                SmallAction(uiText("reject"), NexoraRed, loading = viewModel.state.actionInProgress == "contribution-reject-${contribution.id}") { viewModel.adminRejectContribution(contribution.id) }
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
                    Text(uiText("confirm"), color = NexoraGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) {
                    Text(uiText("cancel"), color = NexoraMuted, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = NexoraCard,
            title = { Text(uiText("confirmAction"), color = NexoraText, fontWeight = FontWeight.Black) },
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
                color == NexoraGreen ->
                    Icon(Icons.Filled.CheckCircle, contentDescription = text, tint = color)
                color == NexoraMuted ->
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
                Text(uiText("close"), color = NexoraGreen, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = NexoraCard,
        title = { Text(uiText("pixInstruction"), color = NexoraText, fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                instructions.forEachIndexed { index, instruction ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 16.dp), color = Color(0xFF252525))
                    Text(uiText("pixCodeForTransfer"), color = NexoraMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    PixCodeBox(
                        code = instruction.pixCopyCode,
                        onCopy = {
                            clipboard.setText(AnnotatedString(instruction.pixCopyCode))
                            Toast.makeText(context, uiText("pixCodeCopied"), Toast.LENGTH_SHORT).show()
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("${uiText("identifier")}: ${instruction.contributionId.take(8)}", color = NexoraMuted, fontSize = 12.sp)
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
        Text(uiText("protectedPixCodeTitle"), color = NexoraText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            if (code.isBlank()) uiText("codeUnavailable") else uiText("protectedPixCodeHelp"),
            color = NexoraMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        OutlinedButton(
            onClick = onCopy,
            enabled = code.isNotBlank(),
            border = BorderStroke(1.dp, NexoraGreen),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = NexoraGreen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(uiText("copyCode"), color = NexoraGreen, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val instant = Instant.ofEpochMilli(millis)
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
