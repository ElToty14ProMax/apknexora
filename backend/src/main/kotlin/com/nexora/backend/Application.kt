package com.nexora.backend

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.net.URI
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class ApiException(val statusCode: HttpStatusCode, override val message: String) : RuntimeException(message)

fun main() {
    val config = AppConfig.load()
    embeddedServer(Netty, host = "0.0.0.0", port = config.port) {
        nexoraModule(config)
    }.start(wait = true)
}

fun Application.nexoraModule(config: AppConfig = AppConfig.load()) {
    val security = SecurityService(config)
    val database = NexoraDatabase(config, security)
    val emailService = EmailService(config)
    val authRateLimiter = RateLimiter()

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
    install(CORS) {
        if (!config.isProduction || "*" in config.corsAllowedOrigins) {
            anyHost()
        } else {
            config.corsAllowedOrigins.forEach { origin ->
                val uri = runCatching { URI(origin) }.getOrNull()
                val host = uri?.host ?: origin.removePrefix("https://").removePrefix("http://").trimEnd('/')
                val scheme = uri?.scheme ?: if (origin.startsWith("http://")) "http" else "https"
                allowHost(host, schemes = listOf(scheme))
            }
        }
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-Admin-Token")
    }
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.respond(cause.statusCode, ErrorResponse(cause.message ?: "Erro"))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Dados inválidos."))
        }
        exception<IllegalStateException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse(cause.message ?: "Operação indisponível."))
        }
        exception<Throwable> { call, cause ->
            cause.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erro interno."))
        }
    }
    if (config.isProduction) {
        install(createApplicationPlugin("RequireHttps") {
            onCall { call ->
                if (call.request.headers["X-Forwarded-Proto"] != "https") {
                    throw ApiException(HttpStatusCode.UpgradeRequired, "HTTPS obrigatório.")
                }
            }
        })
    }

    routing {
        get("/") {
            call.respondRedirect("/admin-web/index.html")
        }
        staticResources("/admin-web", "web")

        get("/health") {
            call.respond(OkResponse(message = "nexora-backend"))
        }

        route("/auth") {
            post("/register") {
                call.enforceRateLimit(authRateLimiter, "register", 5, 5L * 60L * 1000L)
                val request = call.receive<RegisterRequest>()
                val email = security.normalizeEmail(request.email)
                val name = request.name?.trim()?.takeIf { it.isNotBlank() }
                    ?: throw ApiException(HttpStatusCode.BadRequest, "Informe nome completo.")
                val cpfDigits = CpfValidator.digits(request.cpf)
                val pixKey = request.pixKey.trim()

                if (name.length !in 2..80) throw ApiException(HttpStatusCode.BadRequest, "Informe um nome válido.")
                if (!security.isValidEmail(email)) throw ApiException(HttpStatusCode.BadRequest, "Informe um e-mail válido.")
                if (!CpfValidator.isValid(cpfDigits)) throw ApiException(HttpStatusCode.BadRequest, "CPF inválido.")
                if (!security.isValidPixKey(pixKey)) {
                    throw ApiException(HttpStatusCode.BadRequest, "Informe uma chave Pix valida.")
                }
                if (request.password.length < 8) throw ApiException(HttpStatusCode.BadRequest, "A senha precisa ter pelo menos 8 caracteres.")

                val roadmap = RoadmapRules.currentStep(database.levelCounts())
                if (database.activeUsersCount() >= roadmap.capacity) {
                    throw ApiException(HttpStatusCode.Conflict, "A comunidade está no limite atual de participantes.")
                }
                if (database.findUserByEmail(email) != null) {
                    throw ApiException(HttpStatusCode.Conflict, "E-mail já cadastrado.")
                }
                if (database.findUserByCpfHash(security.hashCpf(cpfDigits)) != null) {
                    throw ApiException(HttpStatusCode.Conflict, "CPF já cadastrado.")
                }

                val inviter = request.inviteCode?.trim()?.takeIf { it.isNotBlank() }?.let {
                    database.findUserByInviteCode(it)
                        ?: throw ApiException(HttpStatusCode.BadRequest, "Código de convite inválido.")
                }
                if (email == config.superAdminEmail && cpfDigits != config.superAdminCpf) {
                    throw ApiException(HttpStatusCode.Forbidden, "Dados do fundador não conferem.")
                }
                val role = when {
                    email == config.superAdminEmail && cpfDigits == config.superAdminCpf -> "SUPER_ADMIN"
                    email in config.founderEmails -> "ADMIN"
                    else -> "USER"
                }
                val status = if (role in setOf("ADMIN", "SUPER_ADMIN")) "APPROVED" else "PENDING_REVIEW"
                val code = security.newVerificationCode()
                database.createUser(
                    name = name,
                    email = email,
                    cpf = cpfDigits,
                    pixKey = pixKey,
                    passwordHash = security.hashPassword(request.password),
                    verificationCodeHash = security.hashVerificationCode(email, code),
                    verificationExpiresAt = now() + 30L * 60L * 1000L,
                    invitedBy = inviter?.id,
                    role = role,
                    status = status,
                )
                emailService.sendVerificationCode(email, name, code)
                call.respond(
                    HttpStatusCode.Created,
                    RegisterResponse(
                        message = "Cadastro criado. Verifique o e-mail para continuar.",
                        devVerificationCode = if (!emailService.isConfigured() && !config.isProduction) code else null,
                    ),
                )
            }

            post("/resend-verification") {
                call.enforceRateLimit(authRateLimiter, "resend-verification", 3, 5L * 60L * 1000L)
                val request = call.receive<ResendVerificationRequest>()
                val email = security.normalizeEmail(request.email)
                val user = database.findUserByEmail(email)
                if (user != null && !user.emailVerified) {
                    val code = security.newVerificationCode()
                    database.updateVerificationCode(
                        email = email,
                        codeHash = security.hashVerificationCode(email, code),
                        expiresAt = now() + 30L * 60L * 1000L,
                    )
                    emailService.sendVerificationCode(email, user.name, code)
                }
                call.respond(OkResponse(message = "Se o cadastro existir, um novo código será enviado."))
            }

            post("/recover-password") {
                call.enforceRateLimit(authRateLimiter, "recover-password", 3, 5L * 60L * 1000L)
                val request = call.receive<RecoverPasswordRequest>()
                val email = security.normalizeEmail(request.email)
                val user = database.findUserByEmail(email)
                if (user != null) {
                    val code = security.newVerificationCode()
                    database.updatePasswordResetCode(
                        email = email,
                        codeHash = security.hashRecoveryCode(email, code),
                        expiresAt = now() + 30L * 60L * 1000L,
                    )
                    emailService.sendRecoveryCode(email, code)
                }
                call.respond(OkResponse(message = "Se o e-mail existir, enviaremos instruções de recuperação."))
            }

            post("/reset-password") {
                call.enforceRateLimit(authRateLimiter, "reset-password", 6, 5L * 60L * 1000L)
                val request = call.receive<ResetPasswordRequest>()
                val email = security.normalizeEmail(request.email)
                if (request.newPassword.length < 8) {
                    throw ApiException(HttpStatusCode.BadRequest, "A nova senha precisa ter pelo menos 8 caracteres.")
                }
                val ok = database.resetPassword(
                    email = email,
                    codeHash = security.hashRecoveryCode(email, request.code.trim()),
                    passwordHash = security.hashPassword(request.newPassword),
                )
                if (!ok) throw ApiException(HttpStatusCode.BadRequest, "Código inválido ou expirado.")
                call.respond(OkResponse(message = "Senha atualizada."))
            }

            post("/verify-email") {
                call.enforceRateLimit(authRateLimiter, "verify-email", 8, 5L * 60L * 1000L)
                val request = call.receive<VerifyEmailRequest>()
                val email = security.normalizeEmail(request.email)
                val ok = database.verifyEmail(email, security.hashVerificationCode(email, request.code.trim()))
                if (!ok) throw ApiException(HttpStatusCode.BadRequest, "Código inválido ou expirado.")
                call.respond(OkResponse(message = "E-mail verificado. Aguarde a validação manual se necessário."))
            }

            post("/login") {
                call.enforceRateLimit(authRateLimiter, "login", 12, 60L * 1000L)
                val request = call.receive<LoginRequest>()
                val identifier = request.identifier.trim()
                val user = if (identifier.contains("@")) {
                    database.findUserByEmail(security.normalizeEmail(identifier))
                } else {
                    val cpf = CpfValidator.digits(identifier)
                    if (cpf.length == 11) database.findUserByCpfHash(security.hashCpf(cpf)) else null
                } ?: throw ApiException(HttpStatusCode.Unauthorized, "CPF/e-mail ou senha incorretos.")

                if (!security.verifyPassword(request.password, user.passwordHash)) {
                    throw ApiException(HttpStatusCode.Unauthorized, "CPF/e-mail ou senha incorretos.")
                }
                if (!user.emailVerified) {
                    throw ApiException(HttpStatusCode.Forbidden, "Verifique seu e-mail antes de entrar.")
                }
                if (user.status == "BLOCKED") {
                    throw ApiException(HttpStatusCode.Forbidden, "Conta bloqueada para novas ações.")
                }
                val token = security.newToken()
                database.storeToken(security.hashToken(token), user.id, now() + 7L * 24L * 60L * 60L * 1000L)
                call.respond(LoginResponse(token, user.toProfile(database, security, config)))
            }
        }

        get("/me") {
            val user = call.requireUser(database, security)
            call.respond(user.toProfile(database, security, config))
        }

        get("/dashboard") {
            val user = call.requireUser(database, security)
            val stats = database.dashboardStats()
            val roadmap = RoadmapRules.currentStep(database.levelCounts())
            call.respond(
                DashboardResponse(
                    communityLiquidityCents = stats.liquidityCents,
                    inCirculationCents = stats.inCirculationCents,
                    completionPercent = stats.completionPercent,
                    activeRequests = stats.activeRequests,
                    completedOperations = stats.completedOperations,
                    activeUsers = stats.activeUsers,
                    userLimitCents = ReputationRules.supportLimitCents(user.level),
                    roadmapStep = roadmap.step,
                    roadmapCapacity = roadmap.capacity,
                ),
            )
        }

        get("/community") {
            val currentUser = call.requireUser(database, security)
            call.respond(
                database.listCommunityRequests()
                    .filter { (request, _) -> request.requesterId != currentUser.id }
                    .map { (request, user) ->
                        request.toSupportResponse(user, includeDescription = false)
                    },
            )
        }

        route("/support-requests") {
            get("/mine") {
                val user = call.requireUser(database, security)
                call.respond(database.listUserRequests(user.id).map { it.toSupportResponse(user, includeDescription = true) })
            }

            get("/contributions/mine") {
                val user = call.requireUser(database, security)
                call.respond(database.listUserContributionHistory(user.id).map { it.toHistoryResponse(user.id) })
            }

            post {
                val user = call.requireUser(database, security)
                val request = call.receive<CreateSupportRequest>()
                if (user.status != "APPROVED") {
                    throw ApiException(HttpStatusCode.Forbidden, "Conta aguardando validação manual.")
                }
                if (!ReputationRules.canRequestHelp(user)) {
                    throw ApiException(
                        HttpStatusCode.Forbidden,
                        "Para solicitar ajuda, e necessario estar no Nivel 2, com pelo menos 100 XP.",
                    )
                }
                if (request.amountCents <= 0) throw ApiException(HttpStatusCode.BadRequest, "Informe um valor válido.")
                if (request.dueDays !in 1..90) throw ApiException(HttpStatusCode.BadRequest, "Prazo precisa ficar entre 1 e 90 dias.")
                val limit = ReputationRules.supportLimitCents(user.level)
                if (request.amountCents > limit) {
                    throw ApiException(HttpStatusCode.BadRequest, "Valor acima do seu limite atual.")
                }
                val nextFee = ReputationRules.adminFeeFor(request.amountCents)
                val feeLimit = ReputationRules.adminFeeLimitCents(user.level)
                if (user.adminFeeDueCents >= feeLimit) {
                    throw ApiException(HttpStatusCode.Conflict, "Taxa administrativa pendente atingiu R$ 5,00. Aguarde baixa administrativa.")
                }
                if (user.adminFeeDueCents + nextFee > feeLimit) {
                    throw ApiException(HttpStatusCode.Conflict, "Taxa administrativa pendente atingiu o limite do seu nível.")
                }
                val cleanDescription = request.description?.trim()?.takeIf { it.isNotBlank() }?.let { it.take(500) }
                val record = database.createSupportRequest(user, request.copy(description = cleanDescription))
                call.respond(HttpStatusCode.Created, record.toSupportResponse(user, includeDescription = true))
            }

            post("/contributions/auto-split") {
                val donor = call.requireUser(database, security)
                if (donor.status != "APPROVED") {
                    throw ApiException(HttpStatusCode.Forbidden, "Conta aguardando validacao manual.")
                }
                val body = call.receive<CreateContributionBatchRequest>()
                if (body.amountCents <= 0) throw ApiException(HttpStatusCode.BadRequest, "Informe um valor valido.")
                val instructions = database.createChronologicalContributions(donor, body.amountCents).map {
                    it.toInstructionResponse(config, security)
                }
                val allocated = instructions.sumOf { it.amountCents }
                call.respond(
                    HttpStatusCode.Created,
                    ContributionBatchResponse(
                        requestedAmountCents = body.amountCents,
                        allocatedAmountCents = allocated,
                        unallocatedAmountCents = (body.amountCents - allocated).coerceAtLeast(0),
                        instructions = instructions,
                        message = "Pix fracionado por ordem cronologica. Use o codigo Pix da plataforma e envie os comprovantes depois da transferencia.",
                    ),
                )
            }

            post("/{id}/contributions") {
                val donor = call.requireUser(database, security)
                if (donor.status != "APPROVED") {
                    throw ApiException(HttpStatusCode.Forbidden, "Conta aguardando validação manual.")
                }
                val id = call.parameters["id"] ?: throw ApiException(HttpStatusCode.BadRequest, "Identificador ausente.")
                val support = database.findSupportRequest(id) ?: throw ApiException(HttpStatusCode.NotFound, "Solicitação não encontrada.")
                if (support.status !in setOf("OPEN")) {
                    throw ApiException(HttpStatusCode.Conflict, "Solicitação não está aberta para novos apoios.")
                }
                if (support.requesterId == donor.id) {
                    throw ApiException(HttpStatusCode.BadRequest, "Não é possível apoiar a própria solicitação.")
                }
                val body = call.receive<CreateContributionRequest>()
                val remaining = support.amountCents - support.fundedCents
                if (body.amountCents <= 0 || body.amountCents > remaining) {
                    throw ApiException(HttpStatusCode.BadRequest, "Valor de apoio inválido.")
                }
                val requester = database.findUserById(support.requesterId)
                    ?: throw ApiException(HttpStatusCode.NotFound, "Usuário solicitante não encontrado.")
                val contribution = database.createContribution(support, donor, body.amountCents)
                call.respond(HttpStatusCode.Created, ContributionInstructionRecord(contribution, support, requester).toInstructionResponse(config, security))
            }

            post("/contributions/{id}/receipt") {
                val user = call.requireUser(database, security)
                val id = call.parameters["id"] ?: throw ApiException(HttpStatusCode.BadRequest, "Identificador ausente.")
                val body = call.receive<SubmitPixReceiptRequest>()
                if (!security.isValidSha256(body.receiptHash)) {
                    throw ApiException(HttpStatusCode.BadRequest, "Hash do comprovante inválido.")
                }
                if (body.amountCents <= 0) throw ApiException(HttpStatusCode.BadRequest, "Valor inválido.")
                val receiptDate = java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")).toString()
                val cleanImageBase64 = validateReceiptImage(body.receiptImageBase64, body.receiptMimeType, body.receiptHash)
                val receipt = database.submitPixReceipt(
                    contributionId = id,
                    userId = user.id,
                    transactionId = body.transactionId,
                    side = body.side,
                    receiptHash = body.receiptHash,
                    receiptImageBase64 = cleanImageBase64,
                    receiptMimeType = body.receiptMimeType,
                    amountCents = body.amountCents,
                    receiptDate = receiptDate,
                )
                call.respond(
                    HttpStatusCode.Created,
                    PixReceiptResponse(
                        contributionId = receipt.contributionId,
                        transactionId = receipt.transactionId,
                        side = receipt.side,
                        receiptHash = receipt.receiptHash,
                        receiptImageBase64 = receipt.receiptImageBase64,
                        receiptMimeType = receipt.receiptMimeType,
                        amountCents = receipt.amountCents,
                        receiptDate = receipt.receiptDate,
                        submittedAt = receipt.submittedAt,
                        status = receipt.status,
                        hasSenderReceipt = receipt.hasSenderReceipt,
                        hasReceiverReceipt = receipt.hasReceiverReceipt,
                        evidenceComplete = receipt.evidenceComplete,
                    ),
                )
            }
        }

        route("/admin") {
            get("/overview") {
                call.requireAdmin(database, security, config)
                val overview = database.adminOverview()
                call.respond(
                    AdminOverviewResponse(
                        communityLiquidityCents = overview.communityLiquidityCents,
                        inCirculationCents = overview.inCirculationCents,
                        completionPercent = overview.completionPercent,
                        activeRequests = overview.activeRequests,
                        completedOperations = overview.completedOperations,
                        activeUsers = overview.activeUsers,
                        totalUsers = overview.totalUsers,
                        pendingUsers = overview.pendingUsers,
                        blockedUsers = overview.blockedUsers,
                        pendingRequests = overview.pendingRequests,
                        openRequests = overview.openRequests,
                        fundedRequests = overview.fundedRequests,
                        pendingContributions = overview.pendingContributions,
                        pendingReceipts = overview.pendingReceipts,
                        adminFeeDueCents = overview.adminFeeDueCents,
                        roadmapStep = overview.roadmapStep,
                        roadmapCapacity = overview.roadmapCapacity,
                        generatedAt = overview.generatedAt,
                    ),
                )
            }

            get("/audit-logs") {
                call.requireAdmin(database, security, config)
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 80
                call.respond(database.listAuditLogs(limit).map {
                    AuditLogResponse(
                        id = it.id,
                        actorPublicId = it.actorPublicId,
                        action = it.action,
                        target = it.target,
                        createdAt = it.createdAt,
                    )
                })
            }

            get("/users") {
                val actor = call.requireAdmin(database, security, config)
                call.respond(database.listUsers().map { it.toAdminUser() })
            }

            post("/users/{id}/approve") {
                val actor = call.requireAdmin(database, security, config)
                database.updateUserStatus(call.requiredId(), "APPROVED", actor?.id)
                call.respond(OkResponse(message = "Usuário aprovado."))
            }

            post("/users/{id}/block") {
                val actor = call.requireAdmin(database, security, config)
                database.updateUserStatus(call.requiredId(), "BLOCKED", actor?.id)
                call.respond(OkResponse(message = "Usuário bloqueado."))
            }

            post("/users/{id}/confirm-admin-fee") {
                val actor = call.requireAdmin(database, security, config)
                database.resetAdminFee(call.requiredId(), actor?.id)
                call.respond(OkResponse(message = "Taxa administrativa baixada."))
            }

            post("/users/{id}/role") {
                val actor = call.requireSuperAdmin(database, security, config)
                val body = call.receive<UpdateRoleRequest>()
                val role = body.role.trim().uppercase()
                if (role !in setOf("USER", "ADMIN", "SUPER_ADMIN")) {
                    throw ApiException(HttpStatusCode.BadRequest, "Role inválido.")
                }
                database.updateUserRole(call.requiredId(), role, actor?.id)
                call.respond(OkResponse(message = "Role atualizado."))
            }

            post("/users/{id}/reputation") {
                val actor = call.requireAdmin(database, security, config)
                val body = call.receive<UpdateReputationRequest>()
                database.updateUserReputation(
                    id = call.requiredId(),
                    xp = body.xp,
                    level = body.level,
                    buffBps = body.buffBps,
                    adminFeeDueCents = body.adminFeeDueCents,
                    actorId = actor?.id,
                )
                call.respond(OkResponse(message = "Reputação atualizada."))
            }

            post("/system/reset-database") {
                call.requireSuperAdmin(database, security, config)
                database.clearAllData()
                call.respond(OkResponse(message = "Base de dados limpa. O Super Admin foi recriado."))
            }

            get("/support-requests") {
                call.requireAdmin(database, security, config)
                call.respond(database.listSupportRequestsForAdmin().map { (request, requester) ->
                    request.toAdminSupportRequest(requester)
                })
            }

            post("/support-requests/{id}/approve") {
                val actor = call.requireAdmin(database, security, config)
                database.approveSupportRequest(call.requiredId(), actor?.id)
                call.respond(OkResponse(message = "Solicitação aprovada."))
            }

            post("/support-requests/{id}/reject") {
                val actor = call.requireAdmin(database, security, config)
                val body = runCatching { call.receive<RejectRequest>() }.getOrDefault(RejectRequest())
                database.rejectSupportRequest(call.requiredId(), body.reason, actor?.id)
                call.respond(OkResponse(message = "Solicitação recusada."))
            }

            post("/support-requests/{id}/confirm-return") {
                val actor = call.requireAdmin(database, security, config)
                val body = runCatching { call.receive<ConfirmReturnRequest>() }.getOrDefault(ConfirmReturnRequest())
                database.confirmReturn(call.requiredId(), body.returnedAt ?: now(), actor?.id)
                call.respond(OkResponse(message = "Retorno validado e XP atualizado."))
            }

            get("/contributions") {
                call.requireAdmin(database, security, config)
                call.respond(database.listContributionsForAdmin().map { (contribution, request, donor, requester) ->
                    AdminContributionResponse(
                        id = contribution.id,
                        requestPublicCode = request.publicCode,
                        donorPublicId = donor.publicId,
                        receiverPublicId = requester.publicId,
                        amountCents = contribution.amountCents,
                        status = contribution.status,
                        createdAt = contribution.createdAt,
                        confirmedAt = contribution.confirmedAt,
                        transactionId = contribution.transactionId,
                        senderReceiptHash = contribution.senderReceiptHash,
                        senderReceiptDate = contribution.senderReceiptDate,
                        senderReceiptImageBase64 = contribution.senderReceiptImageBase64,
                        senderReceiptMimeType = contribution.senderReceiptMimeType,
                        receiverReceiptHash = contribution.receiverReceiptHash,
                        receiverReceiptDate = contribution.receiverReceiptDate,
                        receiverReceiptImageBase64 = contribution.receiverReceiptImageBase64,
                        receiverReceiptMimeType = contribution.receiverReceiptMimeType,
                        hasSenderReceipt = contribution.hasSenderReceipt(),
                        hasReceiverReceipt = contribution.hasReceiverReceipt(),
                        evidenceComplete = contribution.evidenceComplete(),
                    )
                })
            }

            post("/contributions/{id}/confirm") {
                val actor = call.requireAdmin(database, security, config)
                database.confirmContribution(call.requiredId(), actor?.id)
                call.respond(OkResponse(message = "Apoio validado."))
            }
        }
    }
}

private class RateLimiter {
    private val attempts = ConcurrentHashMap<String, MutableList<Long>>()

    fun allow(key: String, limit: Int, windowMs: Long, nowMs: Long = now()): Boolean {
        val bucket = attempts.computeIfAbsent(key) { mutableListOf() }
        synchronized(bucket) {
            bucket.removeAll { it <= nowMs - windowMs }
            if (bucket.size >= limit) return false
            bucket += nowMs
            return true
        }
    }
}

private fun ApplicationCall.enforceRateLimit(
    limiter: RateLimiter,
    action: String,
    limit: Int,
    windowMs: Long,
) {
    val forwarded = request.headers["X-Forwarded-For"]?.substringBefore(",")?.trim()
    val client = forwarded?.takeIf { it.isNotBlank() }
        ?: request.headers["X-Real-IP"]?.trim()?.takeIf { it.isNotBlank() }
        ?: "local"
    if (!limiter.allow("$action:$client", limit, windowMs)) {
        throw ApiException(HttpStatusCode.TooManyRequests, "Muitas tentativas. Aguarde alguns minutos e tente novamente.")
    }
}

private suspend fun ApplicationCall.requireUser(database: NexoraDatabase, security: SecurityService): UserRecord {
    val auth = request.headers[HttpHeaders.Authorization]
        ?: throw ApiException(HttpStatusCode.Unauthorized, "Token ausente.")
    val token = auth.removePrefix("Bearer").trim()
    if (token.isBlank() || token == auth) throw ApiException(HttpStatusCode.Unauthorized, "Token inválido.")
    return database.findUserByToken(security.hashToken(token))
        ?: throw ApiException(HttpStatusCode.Unauthorized, "Sessão inválida.")
}

private suspend fun ApplicationCall.requireAdmin(
    database: NexoraDatabase,
    security: SecurityService,
    config: AppConfig,
): UserRecord? {
    val adminHeader = request.headers["X-Admin-Token"]
    if (!adminHeader.isNullOrBlank() && constantTimeTokenEquals(adminHeader, config.adminToken)) return null
    val user = requireUser(database, security)
    if (user.role !in setOf("ADMIN", "SUPER_ADMIN")) {
        throw ApiException(HttpStatusCode.Forbidden, "Acesso administrativo restrito.")
    }
    return user
}

private fun constantTimeTokenEquals(actual: String, expected: String): Boolean =
    MessageDigest.isEqual(actual.toByteArray(Charsets.UTF_8), expected.toByteArray(Charsets.UTF_8))

private suspend fun ApplicationCall.requireSuperAdmin(
    database: NexoraDatabase,
    security: SecurityService,
    config: AppConfig,
): UserRecord? {
    val user = requireAdmin(database, security, config)
    if (user == null) return null
    if (user.role != "SUPER_ADMIN") {
        throw ApiException(HttpStatusCode.Forbidden, "Acesso de super admin restrito.")
    }
    return user
}

private fun ApplicationCall.requiredId(): String =
    parameters["id"] ?: throw ApiException(HttpStatusCode.BadRequest, "Identificador ausente.")

private fun UserRecord.toProfile(database: NexoraDatabase, security: SecurityService, config: AppConfig): ProfileResponse =
    ProfileResponse(
        id = id,
        publicId = publicId,
        name = name,
        email = email,
        status = status,
        role = role,
        level = level,
        xp = xp,
        xpIntoLevel = ReputationRules.xpIntoLevel(xp),
        xpRequiredThisLevel = ReputationRules.xpRequiredForLevel(level),
        buffBps = buffBps,
        supportLimitCents = ReputationRules.supportLimitCents(level),
        inviteCode = inviteCode,
        invitedCount = database.invitedCount(id),
        adminFeeDueCents = adminFeeDueCents,
        adminFeeLimitCents = ReputationRules.adminFeeLimitCents(level),
        pixKeyMasked = maskPix(security.decrypt(pixCipher)),
        adminPixKey = config.adminPixKey?.takeIf { adminFeeDueCents > 0 },
    )

private fun UserRecord.toAdminUser(): AdminUserResponse =
    AdminUserResponse(
        id = id,
        publicId = publicId,
        name = name,
        email = email,
        status = status,
        role = role,
        level = level,
        xp = xp,
        buffBps = buffBps,
        supportLimitCents = ReputationRules.supportLimitCents(level),
        adminFeeDueCents = adminFeeDueCents,
        adminFeeLimitCents = ReputationRules.adminFeeLimitCents(level),
        createdAt = createdAt,
    )

private fun SupportRecord.toSupportResponse(requester: UserRecord, includeDescription: Boolean): SupportRequestResponse =
    SupportRequestResponse(
        id = id,
        publicCode = publicCode,
        requesterPublicId = requester.publicId,
        requesterLevel = requester.level,
        amountCents = amountCents,
        fundedCents = fundedCents,
        dueDays = dueDays,
        status = status,
        description = if (includeDescription) description else null,
        createdAt = createdAt,
    )

private fun SupportRecord.toAdminSupportRequest(requester: UserRecord): AdminSupportRequestResponse =
    AdminSupportRequestResponse(
        id = id,
        publicCode = publicCode,
        requesterPublicId = requester.publicId,
        requesterName = requester.name,
        amountCents = amountCents,
        fundedCents = fundedCents,
        dueDays = dueDays,
        status = status,
        adminFeeCents = ReputationRules.adminFeeFor(amountCents),
        description = description,
        createdAt = createdAt,
    )

private fun ContributionInstructionRecord.toInstructionResponse(config: AppConfig, security: SecurityService): ContributionInstructionResponse {
    val reference = security.paymentReference(contribution.id)
    val pixCode = config.adminPixKey
        ?.takeIf { it.isNotBlank() }
        ?.let { PixCopyCode.build(it, contribution.amountCents, reference) }
        ?: reference
    val message = if (config.adminPixKey.isNullOrBlank()) {
        "Codigo interno gerado. Configure a chave Pix da plataforma para gerar um Pix copia e cola bancario."
    } else {
        "Copie o codigo Pix da plataforma. Depois da transferencia, anexe o ID e a foto no historico."
    }
    return ContributionInstructionResponse(
        contributionId = contribution.id,
        requestPublicCode = request.publicCode,
        receiverIdentifier = request.publicCode,
        receiverPixKey = "",
        pixCopyCode = pixCode,
        amountCents = contribution.amountCents,
        message = message,
    )
}

private fun ContributionHistoryRecord.toHistoryResponse(currentUserId: String): ContributionHistoryResponse =
    ContributionHistoryResponse(
        id = contribution.id,
        transactionId = contribution.transactionId,
        requestPublicCode = request.publicCode,
        donorPublicId = donor.publicId,
        receiverPublicId = requester.publicId,
        direction = if (contribution.donorId == currentUserId) "SENT" else "RECEIVED",
        amountCents = contribution.amountCents,
        status = contribution.status,
        createdAt = contribution.createdAt,
        confirmedAt = contribution.confirmedAt,
        senderReceiptDate = contribution.senderReceiptDate,
        receiverReceiptDate = contribution.receiverReceiptDate,
        hasSenderReceipt = contribution.hasSenderReceipt(),
        hasReceiverReceipt = contribution.hasReceiverReceipt(),
        evidenceComplete = contribution.evidenceComplete(),
    )

private fun validateReceiptImage(imageBase64: String, mimeType: String, expectedSha256: String): String {
    val cleanMime = mimeType.trim().lowercase()
    if (cleanMime !in setOf("image/jpeg", "image/png", "image/webp")) {
        throw ApiException(HttpStatusCode.BadRequest, "Foto do comprovante precisa ser JPG, PNG ou WebP.")
    }
    val cleanBase64 = imageBase64.substringAfter("base64,", imageBase64).trim()
    if (cleanBase64.isBlank()) throw ApiException(HttpStatusCode.BadRequest, "Foto do comprovante ausente.")
    val bytes = try {
        Base64.getDecoder().decode(cleanBase64)
    } catch (_: IllegalArgumentException) {
        throw ApiException(HttpStatusCode.BadRequest, "Foto do comprovante invalida.")
    }
    if (bytes.isEmpty() || bytes.size > 2_500_000) {
        throw ApiException(HttpStatusCode.BadRequest, "Foto do comprovante deve ter ate 2,5 MB.")
    }
    val actualHash = sha256Hex(bytes)
    if (actualHash != expectedSha256.lowercase()) {
        throw ApiException(HttpStatusCode.BadRequest, "Hash da foto nao confere com o comprovante enviado.")
    }
    return cleanBase64
}

private fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

private fun maskPix(value: String): String {
    val clean = value.trim()
    if (clean.length <= 6) return "***"
    return clean.take(min(3, clean.length)) + "***" + clean.takeLast(min(3, clean.length))
}
