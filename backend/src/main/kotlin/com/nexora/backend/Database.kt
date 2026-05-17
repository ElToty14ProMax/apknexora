package com.nexora.backend

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.time.Instant
import java.util.UUID

data class UserRecord(
    val id: String,
    val publicId: String,
    val name: String,
    val email: String,
    val emailVerified: Boolean,
    val verificationCodeHash: String?,
    val verificationExpiresAt: Long?,
    val cpfHash: String,
    val cpfCipher: String,
    val pixCipher: String,
    val passwordHash: String,
    val status: String,
    val role: String,
    val xp: Long,
    val level: Int,
    val buffBps: Int,
    val onTimeReturnedCents: Long,
    val earlyReturnedCents: Long,
    val invitedBy: String?,
    val inviteCode: String,
    val createdAt: Long,
    val adminFeeDueCents: Long,
)

data class SupportRecord(
    val id: String,
    val requesterId: String,
    val publicCode: String,
    val amountCents: Long,
    val fundedCents: Long,
    val dueDays: Int,
    val dueAt: Long?,
    val description: String?,
    val status: String,
    val createdAt: Long,
    val approvedAt: Long?,
    val returnedAt: Long?,
    val rejectedReason: String?,
)

data class ContributionRecord(
    val id: String,
    val requestId: String,
    val donorId: String,
    val amountCents: Long,
    val status: String,
    val createdAt: Long,
    val confirmedAt: Long?,
    val transactionId: String?,
    val senderReceiptHash: String?,
    val senderReceiptImageBase64: String?,
    val senderReceiptMimeType: String?,
    val senderReceiptDate: String?,
    val senderReceiptSubmittedAt: Long?,
    val receiverReceiptHash: String?,
    val receiverReceiptImageBase64: String?,
    val receiverReceiptMimeType: String?,
    val receiverReceiptDate: String?,
    val receiverReceiptSubmittedAt: Long?,
)

data class PixReceiptRecord(
    val contributionId: String,
    val transactionId: String,
    val side: String,
    val receiptHash: String,
    val receiptImageBase64: String,
    val receiptMimeType: String,
    val amountCents: Long,
    val receiptDate: String,
    val submittedAt: Long,
    val status: String,
    val hasSenderReceipt: Boolean,
    val hasReceiverReceipt: Boolean,
    val evidenceComplete: Boolean,
)

data class ContributionInstructionRecord(
    val contribution: ContributionRecord,
    val request: SupportRecord,
    val requester: UserRecord,
)

data class ContributionHistoryRecord(
    val contribution: ContributionRecord,
    val request: SupportRecord,
    val donor: UserRecord,
    val requester: UserRecord,
)

data class AuditLogRecord(
    val id: String,
    val actorPublicId: String?,
    val action: String,
    val target: String,
    val createdAt: Long,
)

class NexoraDatabase(
    private val config: AppConfig,
    private val security: SecurityService,
) {
    private val jdbcUrl = "jdbc:sqlite:${config.databasePath}"

    init {
        File(config.databasePath).parentFile?.mkdirs()
        Class.forName("org.sqlite.JDBC")
        connect().use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA journal_mode=WAL")
                statement.execute("PRAGMA foreign_keys=ON")
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT PRIMARY KEY,
                        public_id TEXT UNIQUE NOT NULL,
                        name TEXT NOT NULL,
                        email TEXT UNIQUE NOT NULL,
                        email_verified INTEGER NOT NULL,
                        verification_code_hash TEXT,
                        verification_expires_at INTEGER,
                        cpf_hash TEXT UNIQUE NOT NULL,
                        cpf_cipher TEXT NOT NULL,
                        pix_cipher TEXT NOT NULL,
                        password_hash TEXT NOT NULL,
                        status TEXT NOT NULL,
                        role TEXT NOT NULL,
                        xp INTEGER NOT NULL,
                        level INTEGER NOT NULL,
                        buff_bps INTEGER NOT NULL,
                        on_time_returned_cents INTEGER NOT NULL,
                        early_returned_cents INTEGER NOT NULL,
                        invited_by TEXT REFERENCES users(id),
                        invite_code TEXT UNIQUE NOT NULL,
                        created_at INTEGER NOT NULL,
                        admin_fee_due_cents INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                addColumnIfMissing(statement, "users", "password_reset_code_hash TEXT")
                addColumnIfMissing(statement, "users", "password_reset_expires_at INTEGER")
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS auth_tokens (
                        token_hash TEXT PRIMARY KEY,
                        user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        expires_at INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS support_requests (
                        id TEXT PRIMARY KEY,
                        requester_id TEXT NOT NULL REFERENCES users(id),
                        public_code TEXT UNIQUE NOT NULL,
                        amount_cents INTEGER NOT NULL,
                        funded_cents INTEGER NOT NULL,
                        due_days INTEGER NOT NULL,
                        due_at INTEGER,
                        description TEXT,
                        status TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        approved_at INTEGER,
                        returned_at INTEGER,
                        rejected_reason TEXT
                    )
                    """.trimIndent(),
                )
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS contributions (
                        id TEXT PRIMARY KEY,
                        request_id TEXT NOT NULL REFERENCES support_requests(id),
                        donor_id TEXT NOT NULL REFERENCES users(id),
                        amount_cents INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        confirmed_at INTEGER,
                        transaction_id TEXT,
                        sender_receipt_hash TEXT,
                        sender_receipt_image_base64 TEXT,
                        sender_receipt_mime_type TEXT,
                        sender_receipt_date TEXT,
                        sender_receipt_submitted_at INTEGER,
                        receiver_receipt_hash TEXT,
                        receiver_receipt_image_base64 TEXT,
                        receiver_receipt_mime_type TEXT,
                        receiver_receipt_date TEXT,
                        receiver_receipt_submitted_at INTEGER
                    )
                    """.trimIndent(),
                )
                addColumnIfMissing(statement, "contributions", "transaction_id TEXT")
                addColumnIfMissing(statement, "contributions", "sender_receipt_hash TEXT")
                addColumnIfMissing(statement, "contributions", "sender_receipt_image_base64 TEXT")
                addColumnIfMissing(statement, "contributions", "sender_receipt_mime_type TEXT")
                addColumnIfMissing(statement, "contributions", "sender_receipt_date TEXT")
                addColumnIfMissing(statement, "contributions", "sender_receipt_submitted_at INTEGER")
                addColumnIfMissing(statement, "contributions", "receiver_receipt_hash TEXT")
                addColumnIfMissing(statement, "contributions", "receiver_receipt_image_base64 TEXT")
                addColumnIfMissing(statement, "contributions", "receiver_receipt_mime_type TEXT")
                addColumnIfMissing(statement, "contributions", "receiver_receipt_date TEXT")
                addColumnIfMissing(statement, "contributions", "receiver_receipt_submitted_at INTEGER")
                statement.execute(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_contributions_transaction_id
                    ON contributions(transaction_id)
                    WHERE transaction_id IS NOT NULL
                    """.trimIndent(),
                )
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS audit_logs (
                        id TEXT PRIMARY KEY,
                        actor_user_id TEXT,
                        action TEXT NOT NULL,
                        target TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS pix_receipts (
                        id TEXT PRIMARY KEY,
                        contribution_id TEXT NOT NULL REFERENCES contributions(id),
                        receipt_hash TEXT UNIQUE NOT NULL,
                        amount_cents INTEGER NOT NULL,
                        receipt_date TEXT NOT NULL,
                        submitted_at INTEGER NOT NULL,
                        status TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }
        ensureBootstrapSuperAdmin()
    }

    fun createUser(
        name: String,
        email: String,
        cpf: String,
        pixKey: String,
        passwordHash: String,
        verificationCodeHash: String,
        verificationExpiresAt: Long,
        invitedBy: String?,
        role: String,
        status: String,
    ): UserRecord {
        val now = now()
        val user = UserRecord(
            id = UUID.randomUUID().toString(),
            publicId = uniquePublicId(),
            name = name,
            email = email,
            emailVerified = false,
            verificationCodeHash = verificationCodeHash,
            verificationExpiresAt = verificationExpiresAt,
            cpfHash = security.hashCpf(cpf),
            cpfCipher = security.encrypt(CpfValidator.digits(cpf)),
            pixCipher = security.encrypt(pixKey.trim()),
            passwordHash = passwordHash,
            status = status,
            role = role,
            xp = 0,
            level = 1,
            buffBps = 0,
            onTimeReturnedCents = 0,
            earlyReturnedCents = 0,
            invitedBy = invitedBy,
            inviteCode = uniqueInviteCode(),
            createdAt = now,
            adminFeeDueCents = 0,
        )
        write { connection ->
            connection.prepare(
                """
                INSERT INTO users (
                    id, public_id, name, email, email_verified, verification_code_hash, verification_expires_at,
                    cpf_hash, cpf_cipher, pix_cipher, password_hash, status, role, xp, level, buff_bps,
                    on_time_returned_cents, early_returned_cents, invited_by, invite_code, created_at, admin_fee_due_cents
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                user.id,
                user.publicId,
                user.name,
                user.email,
                if (user.emailVerified) 1 else 0,
                user.verificationCodeHash,
                user.verificationExpiresAt,
                user.cpfHash,
                user.cpfCipher,
                user.pixCipher,
                user.passwordHash,
                user.status,
                user.role,
                user.xp,
                user.level,
                user.buffBps,
                user.onTimeReturnedCents,
                user.earlyReturnedCents,
                user.invitedBy,
                user.inviteCode,
                user.createdAt,
                user.adminFeeDueCents,
            ).use { it.executeUpdate() }
            audit(connection, null, "USER_REGISTERED", user.id)
        }
        return user
    }

    fun findUserByEmail(email: String): UserRecord? = readOne(
        "SELECT * FROM users WHERE email = ?",
        email,
        mapper = { it.toUserRecord() },
    )

    fun findUserByCpfHash(cpfHash: String): UserRecord? = readOne(
        "SELECT * FROM users WHERE cpf_hash = ?",
        cpfHash,
        mapper = { it.toUserRecord() },
    )

    fun findUserById(id: String): UserRecord? = readOne(
        "SELECT * FROM users WHERE id = ?",
        id,
        mapper = { it.toUserRecord() },
    )

    fun findUserByInviteCode(inviteCode: String): UserRecord? = readOne(
        "SELECT * FROM users WHERE invite_code = ?",
        inviteCode.trim().uppercase(),
        mapper = { it.toUserRecord() },
    )

    fun verifyEmail(email: String, codeHash: String): Boolean = write { connection ->
        val updated = connection.prepare(
            """
            UPDATE users
            SET email_verified = 1, verification_code_hash = NULL, verification_expires_at = NULL
            WHERE email = ?
              AND verification_code_hash = ?
              AND verification_expires_at >= ?
            """.trimIndent(),
            email,
            codeHash,
            now(),
        ).use { it.executeUpdate() }
        updated == 1
    }

    fun updateVerificationCode(email: String, codeHash: String, expiresAt: Long): Boolean = write { connection ->
        val updated = connection.prepare(
            """
            UPDATE users
            SET verification_code_hash = ?, verification_expires_at = ?
            WHERE email = ? AND email_verified = 0
            """.trimIndent(),
            codeHash,
            expiresAt,
            email,
        ).use { it.executeUpdate() }
        updated == 1
    }

    fun updatePasswordResetCode(email: String, codeHash: String, expiresAt: Long): Boolean = write { connection ->
        val updated = connection.prepare(
            """
            UPDATE users
            SET password_reset_code_hash = ?, password_reset_expires_at = ?
            WHERE email = ?
            """.trimIndent(),
            codeHash,
            expiresAt,
            email,
        ).use { it.executeUpdate() }
        updated == 1
    }

    fun resetPassword(email: String, codeHash: String, passwordHash: String): Boolean = write { connection ->
        val updated = connection.prepare(
            """
            UPDATE users
            SET password_hash = ?, password_reset_code_hash = NULL, password_reset_expires_at = NULL
            WHERE email = ?
              AND password_reset_code_hash = ?
              AND password_reset_expires_at >= ?
            """.trimIndent(),
            passwordHash,
            email,
            codeHash,
            now(),
        ).use { it.executeUpdate() }
        if (updated == 1) audit(connection, null, "PASSWORD_RESET", "email:${email.hashCode()}")
        updated == 1
    }

    fun storeToken(tokenHash: String, userId: String, expiresAt: Long) {
        write { connection ->
            connection.prepare(
                "INSERT INTO auth_tokens (token_hash, user_id, expires_at, created_at) VALUES (?, ?, ?, ?)",
                tokenHash,
                userId,
                expiresAt,
                now(),
            ).use { it.executeUpdate() }
        }
    }

    fun findUserByToken(tokenHash: String): UserRecord? = readOne(
        """
        SELECT users.*
        FROM users
        JOIN auth_tokens ON auth_tokens.user_id = users.id
        WHERE auth_tokens.token_hash = ? AND auth_tokens.expires_at >= ?
        """.trimIndent(),
        tokenHash,
        now(),
        mapper = { it.toUserRecord() },
    )

    fun updateUserStatus(id: String, status: String, actorId: String?) {
        write { connection ->
            connection.prepare("UPDATE users SET status = ? WHERE id = ?", status, id).use { it.executeUpdate() }
            audit(connection, actorId, "USER_STATUS_$status", id)
        }
    }

    fun updateUserRole(id: String, role: String, actorId: String?) {
        write { connection ->
            connection.prepare("UPDATE users SET role = ? WHERE id = ?", role, id).use { it.executeUpdate() }
            audit(connection, actorId, "USER_ROLE_$role", id)
        }
    }

    fun updateUserReputation(
        id: String,
        xp: Long?,
        level: Int?,
        buffBps: Int?,
        adminFeeDueCents: Long?,
        actorId: String?,
    ) {
        write { connection ->
            val user = selectUser(connection, id) ?: error("Usuário não encontrado.")
            val nextXp = xp?.coerceAtLeast(0) ?: user.xp
            val nextLevel = level?.coerceIn(1, 1000) ?: ReputationRules.levelForXp(nextXp)
            val nextBuff = buffBps?.coerceIn(0, 10_000) ?: user.buffBps
            val nextFee = adminFeeDueCents?.coerceAtLeast(0) ?: user.adminFeeDueCents
            connection.prepare(
                "UPDATE users SET xp = ?, level = ?, buff_bps = ?, admin_fee_due_cents = ? WHERE id = ?",
                nextXp,
                nextLevel,
                nextBuff,
                nextFee,
                id,
            ).use { it.executeUpdate() }
            audit(connection, actorId, "USER_REPUTATION_UPDATED", id)
        }
    }

    fun listUsers(): List<UserRecord> = readList("SELECT * FROM users ORDER BY created_at DESC", mapper = { it.toUserRecord() })

    fun invitedCount(userId: String): Int = readScalar(
        "SELECT COUNT(*) FROM users WHERE invited_by = ?",
        userId,
    ).toInt()

    fun guestsAtLevelFive(userId: String): Int = readScalar(
        "SELECT COUNT(*) FROM users WHERE invited_by = ? AND level >= 5",
        userId,
    ).toInt()

    fun levelCounts(): Map<Int, Int> = read { connection ->
        connection.prepareStatement("SELECT level, COUNT(*) FROM users WHERE status = 'APPROVED' GROUP BY level").use { statement ->
            statement.executeQuery().use { rs ->
                buildMap {
                    while (rs.next()) put(rs.getInt(1), rs.getInt(2))
                }
            }
        }
    }

    fun activeUsersCount(): Int = readScalar("SELECT COUNT(*) FROM users WHERE status = 'APPROVED'").toInt()

    fun createSupportRequest(user: UserRecord, request: CreateSupportRequest): SupportRecord {
        val record = SupportRecord(
            id = UUID.randomUUID().toString(),
            requesterId = user.id,
            publicCode = uniqueSupportCode(),
            amountCents = request.amountCents,
            fundedCents = 0,
            dueDays = request.dueDays,
            dueAt = null,
            description = request.description?.trim()?.takeIf { it.isNotBlank() },
            status = "PENDING_ADMIN",
            createdAt = now(),
            approvedAt = null,
            returnedAt = null,
            rejectedReason = null,
        )
        write { connection ->
            connection.prepare(
                """
                INSERT INTO support_requests (
                    id, requester_id, public_code, amount_cents, funded_cents, due_days, due_at,
                    description, status, created_at, approved_at, returned_at, rejected_reason
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                record.id,
                record.requesterId,
                record.publicCode,
                record.amountCents,
                record.fundedCents,
                record.dueDays,
                record.dueAt,
                record.description,
                record.status,
                record.createdAt,
                record.approvedAt,
                record.returnedAt,
                record.rejectedReason,
            ).use { it.executeUpdate() }
            audit(connection, user.id, "SUPPORT_REQUEST_CREATED", record.id)
        }
        return record
    }

    fun findSupportRequest(id: String): SupportRecord? = readOne(
        "SELECT * FROM support_requests WHERE id = ?",
        id,
        mapper = { it.toSupportRecord() },
    )

    fun listCommunityRequests(): List<Pair<SupportRecord, UserRecord>> = read { connection ->
        connection.prepareStatement(
            """
            SELECT support_requests.*, users.*
            FROM support_requests
            JOIN users ON users.id = support_requests.requester_id
            WHERE support_requests.status IN ('OPEN', 'FUNDED')
            ORDER BY COALESCE(support_requests.approved_at, support_requests.created_at) ASC, support_requests.created_at ASC
            """.trimIndent(),
        ).use { statement ->
            statement.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(rs.toSupportRecord() to rs.toUserRecord(offset = 13))
                }
            }
        }
    }

    fun listUserRequests(userId: String): List<SupportRecord> = readList(
        "SELECT * FROM support_requests WHERE requester_id = ? ORDER BY created_at ASC",
        userId,
        mapper = { it.toSupportRecord() },
    )

    fun listSupportRequestsForAdmin(): List<Pair<SupportRecord, UserRecord>> = read { connection ->
        connection.prepareStatement(
            """
            SELECT support_requests.*, users.*
            FROM support_requests
            JOIN users ON users.id = support_requests.requester_id
            ORDER BY support_requests.created_at DESC
            """.trimIndent(),
        ).use { statement ->
            statement.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(rs.toSupportRecord() to rs.toUserRecord(offset = 13))
                }
            }
        }
    }

    fun approveSupportRequest(id: String, actorId: String?) {
        write { connection ->
            val support = selectSupport(connection, id) ?: error("Solicitação não encontrada.")
            val requester = selectUser(connection, support.requesterId) ?: error("Usuário não encontrado.")
            if (support.status != "PENDING_ADMIN") error("Solicitação não está aguardando aprovação.")
            val fee = ReputationRules.adminFeeFor(support.amountCents)
            val feeLimit = ReputationRules.adminFeeLimitCents(requester.level)
            val nextFeeDue = requester.adminFeeDueCents + fee
            if (requester.adminFeeDueCents >= feeLimit) {
                connection.prepare("UPDATE users SET status = 'BLOCKED' WHERE id = ?", requester.id).use { it.executeUpdate() }
                error("Taxa administrativa pendente atingiu o limite do usuario.")
            }
            if (nextFeeDue > feeLimit) {
                error("Taxa administrativa pendente excede o limite do usuario.")
            }
            val approvalTime = now()
            val dueAt = approvalTime + support.dueDays * 24L * 60L * 60L * 1000L
            connection.prepare(
                """
                UPDATE support_requests
                SET status = 'OPEN', approved_at = ?, due_at = ?
                WHERE id = ?
                """.trimIndent(),
                approvalTime,
                dueAt,
                id,
            ).use { it.executeUpdate() }
            connection.prepare(
                "UPDATE users SET admin_fee_due_cents = ?, status = ? WHERE id = ?",
                nextFeeDue,
                if (nextFeeDue >= feeLimit) "BLOCKED" else requester.status,
                requester.id,
            ).use { it.executeUpdate() }
            audit(connection, actorId, "SUPPORT_REQUEST_APPROVED", id)
            if (nextFeeDue >= feeLimit) audit(connection, actorId, "ADMIN_FEE_LIMIT_BLOCKED", requester.id)
        }
    }

    fun rejectSupportRequest(id: String, reason: String?, actorId: String?) {
        write { connection ->
            connection.prepare(
                "UPDATE support_requests SET status = 'REJECTED', rejected_reason = ? WHERE id = ? AND status = 'PENDING_ADMIN'",
                reason?.take(280),
                id,
            ).use { it.executeUpdate() }
            audit(connection, actorId, "SUPPORT_REQUEST_REJECTED", id)
        }
    }

    fun createContribution(request: SupportRecord, donor: UserRecord, amountCents: Long): ContributionRecord {
        val contribution = ContributionRecord(
            id = UUID.randomUUID().toString(),
            requestId = request.id,
            donorId = donor.id,
            amountCents = amountCents,
            status = "PENDING_ADMIN",
            createdAt = now(),
            confirmedAt = null,
            transactionId = null,
            senderReceiptHash = null,
            senderReceiptImageBase64 = null,
            senderReceiptMimeType = null,
            senderReceiptDate = null,
            senderReceiptSubmittedAt = null,
            receiverReceiptHash = null,
            receiverReceiptImageBase64 = null,
            receiverReceiptMimeType = null,
            receiverReceiptDate = null,
            receiverReceiptSubmittedAt = null,
        )
        write { connection ->
            val current = selectSupport(connection, request.id) ?: error("Solicitacao nao encontrada.")
            if (current.status != "OPEN") error("Solicitacao nao esta aberta para novos apoios.")
            val available = availableContributionCents(connection, current)
            if (amountCents <= 0 || amountCents > available) {
                if (available <= 0) error("Esta solicitação já está totalmente reservada ou concluída por outros usuários.")
                else error("Valor de apoio inválido. O máximo disponível para esta solicitação no momento é R$ ${String.format("%.2f", available / 100.0)}.")
            }
            connection.prepare(
                """
                INSERT INTO contributions (id, request_id, donor_id, amount_cents, status, created_at, confirmed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                contribution.id,
                contribution.requestId,
                contribution.donorId,
                contribution.amountCents,
                contribution.status,
                contribution.createdAt,
                contribution.confirmedAt,
            ).use { it.executeUpdate() }
            audit(connection, donor.id, "CONTRIBUTION_CREATED", contribution.id)
        }
        return contribution
    }

    fun createChronologicalContributions(donor: UserRecord, totalAmountCents: Long): List<ContributionInstructionRecord> =
        write { connection ->
            if (totalAmountCents <= 0) error("Informe um valor valido.")
            var remainingTotal = totalAmountCents
            val created = mutableListOf<ContributionInstructionRecord>()
            connection.prepareStatement(
                """
                SELECT support_requests.*, users.*
                FROM support_requests
                JOIN users ON users.id = support_requests.requester_id
                WHERE support_requests.status = 'OPEN'
                  AND support_requests.requester_id <> ?
                ORDER BY COALESCE(support_requests.approved_at, support_requests.created_at) ASC, support_requests.created_at ASC
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, donor.id)
                statement.executeQuery().use { rs ->
                    while (rs.next() && remainingTotal > 0) {
                        val support = rs.toSupportRecord()
                        val requester = rs.toUserRecord(offset = 13)
                        val available = availableContributionCents(connection, support)
                        if (available <= 0) continue
                        val amount = minOf(remainingTotal, available)
                        val contribution = insertContribution(connection, support.id, donor.id, amount)
                        created += ContributionInstructionRecord(contribution, support, requester)
                        remainingTotal -= amount
                    }
                }
            }
            if (created.isEmpty()) {
                error("Nao ha solicitacoes abertas de outras pessoas para distribuir esse valor. A sua propria solicitacao nao pode receber seu Pix.")
            }
            created.forEach { audit(connection, donor.id, "CONTRIBUTION_CREATED", it.contribution.id) }
            created
        }

    fun listContributionsForAdmin(): List<ContributionHistoryRecord> = read { connection ->
        connection.prepareStatement(
            """
            SELECT contributions.*, support_requests.*, donor.*, requester.*
            FROM contributions
            JOIN support_requests ON support_requests.id = contributions.request_id
            JOIN users donor ON donor.id = contributions.donor_id
            JOIN users requester ON requester.id = support_requests.requester_id
            ORDER BY contributions.created_at DESC
            """.trimIndent(),
        ).use { statement ->
            statement.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(
                            ContributionHistoryRecord(
                                contribution = rs.toContributionRecord(),
                                request = rs.toSupportRecord(offset = 18),
                                donor = rs.toUserRecord(offset = 31),
                                requester = rs.toUserRecord(offset = 55),
                            ),
                        )
                    }
                }
            }
        }
    }

    fun listUserContributionHistory(userId: String): List<ContributionHistoryRecord> = read { connection ->
        connection.prepare(
            """
            SELECT contributions.*, support_requests.*, donor.*, requester.*
            FROM contributions
            JOIN support_requests ON support_requests.id = contributions.request_id
            JOIN users donor ON donor.id = contributions.donor_id
            JOIN users requester ON requester.id = support_requests.requester_id
            WHERE contributions.donor_id = ? OR support_requests.requester_id = ?
            ORDER BY contributions.created_at ASC, contributions.id ASC
            """.trimIndent(),
            userId,
            userId,
        ).use { statement ->
            statement.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(
                            ContributionHistoryRecord(
                                contribution = rs.toContributionRecord(),
                                request = rs.toSupportRecord(offset = 18),
                                donor = rs.toUserRecord(offset = 31),
                                requester = rs.toUserRecord(offset = 55),
                            ),
                        )
                    }
                }
            }
        }
    }

    fun confirmContribution(id: String, actorId: String?) {
        write { connection ->
            val contribution = selectContribution(connection, id) ?: error("Apoio não encontrado.")
            if (contribution.status != "PENDING_ADMIN") error("Apoio não está aguardando validação.")
            if (!contribution.evidenceComplete()) {
                error("Validacao exige as duas fotos do Pix: envio e recebimento.")
            }
            val support = selectSupport(connection, contribution.requestId) ?: error("Solicitação não encontrada.")
            if (support.status !in setOf("OPEN", "FUNDED")) error("Solicitação não está ativa.")
            val remaining = (support.amountCents - support.fundedCents).coerceAtLeast(0)
            if (contribution.amountCents > remaining) error("Valor excede o saldo restante da solicitação.")
            val confirmedAt = now()
            val newFunded = support.fundedCents + contribution.amountCents
            val newStatus = if (newFunded >= support.amountCents) "FUNDED" else "OPEN"
            connection.prepare(
                "UPDATE contributions SET status = 'CONFIRMED', confirmed_at = ? WHERE id = ?",
                confirmedAt,
                id,
            ).use { it.executeUpdate() }
            connection.prepare(
                "UPDATE support_requests SET funded_cents = ?, status = ? WHERE id = ?",
                newFunded,
                newStatus,
                support.id,
            ).use { it.executeUpdate() }
            audit(connection, actorId, "CONTRIBUTION_CONFIRMED", id)
        }
    }

    fun submitPixReceipt(
        contributionId: String,
        userId: String,
        transactionId: String,
        side: String,
        receiptHash: String,
        receiptImageBase64: String,
        receiptMimeType: String,
        amountCents: Long,
        receiptDate: String,
    ): PixReceiptRecord = write { connection ->
        val contribution = selectContribution(connection, contributionId)
            ?: error("Apoio nao encontrado.")
        val support = selectSupport(connection, contribution.requestId) ?: error("Solicitacao nao encontrada.")
        val cleanSide = side.trim().uppercase()
        if (cleanSide !in setOf("SENDER", "RECEIVER")) error("Tipo de comprovante invalido.")
        if (cleanSide == "SENDER" && contribution.donorId != userId) {
            error("Apenas quem enviou o Pix pode anexar a foto de envio.")
        }
        if (cleanSide == "RECEIVER" && support.requesterId != userId) {
            error("Apenas quem recebeu o Pix pode anexar a foto de recebimento.")
        }
        if (contribution.amountCents != amountCents) error("Valor do comprovante nao confere.")
        val cleanTransactionId = normalizeTransactionId(transactionId)
        if (cleanTransactionId.length !in 6..80) error("ID da transacao invalido.")
        if (contribution.transactionId != null && contribution.transactionId != cleanTransactionId) {
            error("Este apoio ja possui outro ID de transacao.")
        }
        val duplicated = selectContributionByTransactionId(connection, cleanTransactionId)
        if (duplicated != null && duplicated.id != contribution.id) {
            error("ID de transacao ja cadastrado. Ele aparece apenas uma vez no historico.")
        }
        val cleanHash = receiptHash.lowercase()
        val cleanMime = receiptMimeType.trim().lowercase()
        val submittedAt = now()
        when (cleanSide) {
            "SENDER" -> connection.prepare(
                """
                UPDATE contributions
                SET transaction_id = ?,
                    sender_receipt_hash = ?,
                    sender_receipt_image_base64 = ?,
                    sender_receipt_mime_type = ?,
                    sender_receipt_date = ?,
                    sender_receipt_submitted_at = ?
                WHERE id = ?
                """.trimIndent(),
                cleanTransactionId,
                cleanHash,
                receiptImageBase64,
                cleanMime,
                receiptDate,
                submittedAt,
                contributionId,
            ).use { it.executeUpdate() }
            "RECEIVER" -> connection.prepare(
                """
                UPDATE contributions
                SET transaction_id = ?,
                    receiver_receipt_hash = ?,
                    receiver_receipt_image_base64 = ?,
                    receiver_receipt_mime_type = ?,
                    receiver_receipt_date = ?,
                    receiver_receipt_submitted_at = ?
                WHERE id = ?
                """.trimIndent(),
                cleanTransactionId,
                cleanHash,
                receiptImageBase64,
                cleanMime,
                receiptDate,
                submittedAt,
                contributionId,
            ).use { it.executeUpdate() }
        }
        val updated = selectContribution(connection, contributionId) ?: error("Apoio nao encontrado.")
        val record = PixReceiptRecord(
            contributionId = contributionId,
            transactionId = cleanTransactionId,
            side = cleanSide,
            receiptHash = cleanHash,
            receiptImageBase64 = receiptImageBase64,
            receiptMimeType = cleanMime,
            amountCents = amountCents,
            receiptDate = receiptDate,
            submittedAt = submittedAt,
            status = "PENDING_ADMIN",
            hasSenderReceipt = updated.hasSenderReceipt(),
            hasReceiverReceipt = updated.hasReceiverReceipt(),
            evidenceComplete = updated.evidenceComplete(),
        )
        audit(connection, userId, "PIX_${cleanSide}_RECEIPT_SUBMITTED", contributionId)
        record
    }

    fun confirmReturn(requestId: String, returnedAt: Long, actorId: String?) {
        write { connection ->
            val support = selectSupport(connection, requestId) ?: error("Solicitação não encontrada.")
            if (support.status != "FUNDED") error("Solicitação precisa estar completa antes do retorno.")
            val requester = selectUser(connection, support.requesterId) ?: error("Usuário não encontrado.")
            val timingColumn = if (support.dueAt != null && returnedAt < support.dueAt) {
                "early_returned_cents"
            } else {
                "on_time_returned_cents"
            }
            val gainedXp = ReputationRules.xpForCompletedReturn(support.amountCents, requester.buffBps)
            val newXp = requester.xp + gainedXp
            val newLevel = ReputationRules.levelForXp(newXp)
            connection.prepare(
                """
                UPDATE support_requests
                SET status = 'RETURNED', returned_at = ?
                WHERE id = ?
                """.trimIndent(),
                returnedAt,
                requestId,
            ).use { it.executeUpdate() }
            connection.prepare(
                """
                UPDATE users
                SET xp = ?, level = ?, $timingColumn = $timingColumn + ?
                WHERE id = ?
                """.trimIndent(),
                newXp,
                newLevel,
                support.amountCents,
                requester.id,
            ).use { it.executeUpdate() }
            recalculateBuff(connection, requester.id)
            requester.invitedBy?.let { recalculateBuff(connection, it) }
            audit(connection, actorId, "SUPPORT_RETURN_CONFIRMED", requestId)
        }
    }

    fun resetAdminFee(userId: String, actorId: String?) {
        write { connection ->
            connection.prepare("UPDATE users SET admin_fee_due_cents = 0, status = 'APPROVED' WHERE id = ?", userId).use { it.executeUpdate() }
            audit(connection, actorId, "ADMIN_FEE_RESET", userId)
        }
    }

    fun clearAllData() {
        write { connection ->
            connection.createStatement().use { statement ->
                statement.execute("DELETE FROM auth_tokens")
                statement.execute("DELETE FROM pix_receipts")
                statement.execute("DELETE FROM contributions")
                statement.execute("DELETE FROM support_requests")
                statement.execute("DELETE FROM audit_logs")
                statement.execute("DELETE FROM users")
            }
        }
        ensureBootstrapSuperAdmin()
    }

    fun dashboardStats(): DashboardStats = read { connection ->
        val liquidity = scalar(connection, "SELECT COALESCE(SUM(funded_cents), 0) FROM support_requests").toLong()
        val inCirculation = scalar(
            connection,
            "SELECT COALESCE(SUM(funded_cents), 0) FROM support_requests WHERE status IN ('OPEN', 'FUNDED')",
        ).toLong()
        val activeRequests = scalar(connection, "SELECT COUNT(*) FROM support_requests WHERE status IN ('OPEN', 'FUNDED')").toInt()
        val completed = scalar(connection, "SELECT COUNT(*) FROM support_requests WHERE status = 'RETURNED'").toInt()
        val dueNow = now()
        val delayed = scalar(
            connection,
            "SELECT COUNT(*) FROM support_requests WHERE status = 'FUNDED' AND due_at IS NOT NULL AND due_at < ?",
            dueNow,
        ).toInt()
        val activeUsers = scalar(connection, "SELECT COUNT(*) FROM users WHERE status = 'APPROVED'").toInt()
        DashboardStats(
            liquidityCents = liquidity,
            inCirculationCents = inCirculation,
            completionPercent = if (completed + delayed == 0) 100.0 else completed * 100.0 / (completed + delayed),
            activeRequests = activeRequests,
            completedOperations = completed,
            activeUsers = activeUsers,
        )
    }

    fun adminOverview(): AdminOverviewStats = read { connection ->
        val stats = dashboardStats()
        val roadmap = RoadmapRules.currentStep(levelCounts())
        AdminOverviewStats(
            communityLiquidityCents = stats.liquidityCents,
            inCirculationCents = stats.inCirculationCents,
            completionPercent = stats.completionPercent,
            activeRequests = stats.activeRequests,
            completedOperations = stats.completedOperations,
            activeUsers = stats.activeUsers,
            totalUsers = scalar(connection, "SELECT COUNT(*) FROM users").toInt(),
            pendingUsers = scalar(connection, "SELECT COUNT(*) FROM users WHERE status = 'PENDING_REVIEW'").toInt(),
            blockedUsers = scalar(connection, "SELECT COUNT(*) FROM users WHERE status = 'BLOCKED'").toInt(),
            pendingRequests = scalar(connection, "SELECT COUNT(*) FROM support_requests WHERE status = 'PENDING_ADMIN'").toInt(),
            openRequests = scalar(connection, "SELECT COUNT(*) FROM support_requests WHERE status = 'OPEN'").toInt(),
            fundedRequests = scalar(connection, "SELECT COUNT(*) FROM support_requests WHERE status = 'FUNDED'").toInt(),
            pendingContributions = scalar(connection, "SELECT COUNT(*) FROM contributions WHERE status = 'PENDING_ADMIN'").toInt(),
            pendingReceipts = scalar(
                connection,
                """
                SELECT COUNT(*)
                FROM contributions
                WHERE status = 'PENDING_ADMIN'
                  AND (
                    sender_receipt_hash IS NULL OR sender_receipt_image_base64 IS NULL OR
                    receiver_receipt_hash IS NULL OR receiver_receipt_image_base64 IS NULL
                  )
                """.trimIndent(),
            ).toInt(),
            adminFeeDueCents = scalar(connection, "SELECT COALESCE(SUM(admin_fee_due_cents), 0) FROM users"),
            roadmapStep = roadmap.step,
            roadmapCapacity = roadmap.capacity,
            generatedAt = now(),
        )
    }

    fun listAuditLogs(limit: Int = 80): List<AuditLogRecord> = read { connection ->
        connection.prepare(
            """
            SELECT audit_logs.id, users.public_id, audit_logs.action, audit_logs.target, audit_logs.created_at
            FROM audit_logs
            LEFT JOIN users ON users.id = audit_logs.actor_user_id
            ORDER BY audit_logs.created_at DESC
            LIMIT ?
            """.trimIndent(),
            limit.coerceIn(1, 250),
        ).use { statement ->
            statement.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(
                            AuditLogRecord(
                                id = rs.getString(1),
                                actorPublicId = rs.getString(2),
                                action = rs.getString(3),
                                target = rs.getString(4),
                                createdAt = rs.getLong(5),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun ensureBootstrapSuperAdmin() {
        val password = config.superAdminPassword ?: return
        val email = security.normalizeEmail(config.superAdminEmail)
        val cpf = config.superAdminCpf
        if (!CpfValidator.isValid(cpf)) return
        val existing = findUserByEmail(email)
        if (existing != null) {
            if (existing.role != "SUPER_ADMIN" || existing.status != "APPROVED") {
                updateUserRole(existing.id, "SUPER_ADMIN", null)
                updateUserStatus(existing.id, "APPROVED", null)
            }
            return
        }
        val codeHash = security.hashVerificationCode(email, "000000")
        val user = createUser(
            name = "Fundador Nexora",
            email = email,
            cpf = cpf,
            pixKey = config.adminPixKey ?: cpf,
            passwordHash = security.hashPassword(password),
            verificationCodeHash = codeHash,
            verificationExpiresAt = now() + 1,
            invitedBy = null,
            role = "SUPER_ADMIN",
            status = "APPROVED",
        )
        write { connection ->
            connection.prepare(
                "UPDATE users SET email_verified = 1, verification_code_hash = NULL, verification_expires_at = NULL WHERE id = ?",
                user.id,
            ).use { it.executeUpdate() }
            audit(connection, user.id, "SUPER_ADMIN_BOOTSTRAPPED", user.id)
        }
    }

    private fun recalculateBuff(connection: Connection, userId: String) {
        val user = selectUser(connection, userId) ?: return
        val guestsAtLevelFive = scalar(
            connection,
            "SELECT COUNT(*) FROM users WHERE invited_by = ? AND level >= 5",
            userId,
        ).toInt()
        val buff = ReputationRules.recalculateBuffBps(
            onTimeReturnedCents = user.onTimeReturnedCents,
            earlyReturnedCents = user.earlyReturnedCents,
            guestsAtLevelFive = guestsAtLevelFive,
        )
        connection.prepare("UPDATE users SET buff_bps = ? WHERE id = ?", buff, userId).use { it.executeUpdate() }
    }

    private fun uniquePublicId(): String {
        repeat(20) {
            val code = security.publicId()
            if (readScalar("SELECT COUNT(*) FROM users WHERE public_id = ?", code) == 0L) return code
        }
        error("Could not generate unique public id.")
    }

    private fun uniqueInviteCode(): String {
        repeat(20) {
            val code = security.inviteCode()
            if (readScalar("SELECT COUNT(*) FROM users WHERE invite_code = ?", code) == 0L) return code
        }
        error("Could not generate unique invite code.")
    }

    private fun uniqueSupportCode(): String {
        repeat(20) {
            val code = security.supportCode()
            if (readScalar("SELECT COUNT(*) FROM support_requests WHERE public_code = ?", code) == 0L) return code
        }
        error("Could not generate unique support code.")
    }

    private fun selectUser(connection: Connection, id: String): UserRecord? =
        connection.prepare("SELECT * FROM users WHERE id = ?", id).use { statement ->
            statement.executeQuery().use { rs -> if (rs.next()) rs.toUserRecord() else null }
        }

    private fun selectSupport(connection: Connection, id: String): SupportRecord? =
        connection.prepare("SELECT * FROM support_requests WHERE id = ?", id).use { statement ->
            statement.executeQuery().use { rs -> if (rs.next()) rs.toSupportRecord() else null }
        }

    private fun selectContribution(connection: Connection, id: String): ContributionRecord? =
        connection.prepare("SELECT * FROM contributions WHERE id = ?", id).use { statement ->
            statement.executeQuery().use { rs -> if (rs.next()) rs.toContributionRecord() else null }
        }

    private fun selectContributionByTransactionId(connection: Connection, transactionId: String): ContributionRecord? =
        connection.prepare("SELECT * FROM contributions WHERE transaction_id = ?", transactionId).use { statement ->
            statement.executeQuery().use { rs -> if (rs.next()) rs.toContributionRecord() else null }
        }

    private fun availableContributionCents(connection: Connection, support: SupportRecord): Long {
        val reserved = scalar(
            connection,
            """
            SELECT COALESCE(SUM(amount_cents), 0)
            FROM contributions
            WHERE request_id = ?
              AND status IN ('PENDING_ADMIN', 'CONFIRMED')
            """.trimIndent(),
            support.id,
        )
        return (support.amountCents - reserved).coerceAtLeast(0)
    }

    private fun insertContribution(
        connection: Connection,
        requestId: String,
        donorId: String,
        amountCents: Long,
    ): ContributionRecord {
        val contribution = ContributionRecord(
            id = UUID.randomUUID().toString(),
            requestId = requestId,
            donorId = donorId,
            amountCents = amountCents,
            status = "PENDING_ADMIN",
            createdAt = now(),
            confirmedAt = null,
            transactionId = null,
            senderReceiptHash = null,
            senderReceiptImageBase64 = null,
            senderReceiptMimeType = null,
            senderReceiptDate = null,
            senderReceiptSubmittedAt = null,
            receiverReceiptHash = null,
            receiverReceiptImageBase64 = null,
            receiverReceiptMimeType = null,
            receiverReceiptDate = null,
            receiverReceiptSubmittedAt = null,
        )
        connection.prepare(
            """
            INSERT INTO contributions (id, request_id, donor_id, amount_cents, status, created_at, confirmed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            contribution.id,
            contribution.requestId,
            contribution.donorId,
            contribution.amountCents,
            contribution.status,
            contribution.createdAt,
            contribution.confirmedAt,
        ).use { it.executeUpdate() }
        return contribution
    }

    private fun audit(connection: Connection, actorUserId: String?, action: String, target: String) {
        connection.prepare(
            "INSERT INTO audit_logs (id, actor_user_id, action, target, created_at) VALUES (?, ?, ?, ?, ?)",
            UUID.randomUUID().toString(),
            actorUserId,
            action,
            target,
            now(),
        ).use { it.executeUpdate() }
    }

    private fun addColumnIfMissing(statement: Statement, table: String, columnDefinition: String) {
        try {
            statement.execute("ALTER TABLE $table ADD COLUMN $columnDefinition")
        } catch (_: Exception) {
            // SQLite has no IF NOT EXISTS for ADD COLUMN on older versions.
        }
    }

    private fun connect(): Connection = DriverManager.getConnection(jdbcUrl).also { connection ->
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA foreign_keys=ON")
        }
    }

    private fun <T> read(block: (Connection) -> T): T = connect().use(block)

    private fun <T> write(block: (Connection) -> T): T = connect().use { connection ->
        connection.autoCommit = false
        try {
            val result = block(connection)
            connection.commit()
            result
        } catch (error: Throwable) {
            connection.rollback()
            throw error
        }
    }

    private fun <T> readOne(sql: String, vararg args: Any?, mapper: (ResultSet) -> T): T? = read { connection ->
        connection.prepare(sql, *args).use { statement ->
            statement.executeQuery().use { rs -> if (rs.next()) mapper(rs) else null }
        }
    }

    private fun <T> readList(sql: String, vararg args: Any?, mapper: (ResultSet) -> T): List<T> = read { connection ->
        connection.prepare(sql, *args).use { statement ->
            statement.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(mapper(rs))
                }
            }
        }
    }

    private fun readScalar(sql: String, vararg args: Any?): Long = read { connection -> scalar(connection, sql, *args) }

    private fun scalar(connection: Connection, sql: String, vararg args: Any?): Long =
        connection.prepare(sql, *args).use { statement ->
            statement.executeQuery().use { rs -> if (rs.next()) rs.getLong(1) else 0L }
        }

    private fun Connection.prepare(sql: String, vararg args: Any?): PreparedStatement =
        prepareStatement(sql).apply {
            args.forEachIndexed { index, value ->
                val position = index + 1
                when (value) {
                    null -> setObject(position, null)
                    is Int -> setInt(position, value)
                    is Long -> setLong(position, value)
                    is Boolean -> setInt(position, if (value) 1 else 0)
                    else -> setString(position, value.toString())
                }
            }
        }

    private fun ResultSet.toUserRecord(offset: Int = 0): UserRecord = UserRecord(
        id = getString(offset + 1),
        publicId = getString(offset + 2),
        name = getString(offset + 3),
        email = getString(offset + 4),
        emailVerified = getInt(offset + 5) == 1,
        verificationCodeHash = getString(offset + 6),
        verificationExpiresAt = getNullableLong(offset + 7),
        cpfHash = getString(offset + 8),
        cpfCipher = getString(offset + 9),
        pixCipher = getString(offset + 10),
        passwordHash = getString(offset + 11),
        status = getString(offset + 12),
        role = getString(offset + 13),
        xp = getLong(offset + 14),
        level = getInt(offset + 15),
        buffBps = getInt(offset + 16),
        onTimeReturnedCents = getLong(offset + 17),
        earlyReturnedCents = getLong(offset + 18),
        invitedBy = getString(offset + 19),
        inviteCode = getString(offset + 20),
        createdAt = getLong(offset + 21),
        adminFeeDueCents = getLong(offset + 22),
    )

    private fun ResultSet.toSupportRecord(offset: Int = 0): SupportRecord = SupportRecord(
        id = getString(offset + 1),
        requesterId = getString(offset + 2),
        publicCode = getString(offset + 3),
        amountCents = getLong(offset + 4),
        fundedCents = getLong(offset + 5),
        dueDays = getInt(offset + 6),
        dueAt = getNullableLong(offset + 7),
        description = getString(offset + 8),
        status = getString(offset + 9),
        createdAt = getLong(offset + 10),
        approvedAt = getNullableLong(offset + 11),
        returnedAt = getNullableLong(offset + 12),
        rejectedReason = getString(offset + 13),
    )

    private fun ResultSet.toContributionRecord(offset: Int = 0): ContributionRecord = ContributionRecord(
        id = getString(offset + 1),
        requestId = getString(offset + 2),
        donorId = getString(offset + 3),
        amountCents = getLong(offset + 4),
        status = getString(offset + 5),
        createdAt = getLong(offset + 6),
        confirmedAt = getNullableLong(offset + 7),
        transactionId = getString(offset + 8),
        senderReceiptHash = getString(offset + 9),
        senderReceiptImageBase64 = getString(offset + 10),
        senderReceiptMimeType = getString(offset + 11),
        senderReceiptDate = getString(offset + 12),
        senderReceiptSubmittedAt = getNullableLong(offset + 13),
        receiverReceiptHash = getString(offset + 14),
        receiverReceiptImageBase64 = getString(offset + 15),
        receiverReceiptMimeType = getString(offset + 16),
        receiverReceiptDate = getString(offset + 17),
        receiverReceiptSubmittedAt = getNullableLong(offset + 18),
    )

    private fun ResultSet.getNullableLong(columnIndex: Int): Long? {
        val value = getLong(columnIndex)
        return if (wasNull()) null else value
    }
}

fun ContributionRecord.hasSenderReceipt(): Boolean =
    !senderReceiptHash.isNullOrBlank() && !senderReceiptImageBase64.isNullOrBlank()

fun ContributionRecord.hasReceiverReceipt(): Boolean =
    !receiverReceiptHash.isNullOrBlank() && !receiverReceiptImageBase64.isNullOrBlank()

fun ContributionRecord.evidenceComplete(): Boolean =
    transactionId != null && hasSenderReceipt() && hasReceiverReceipt()

fun normalizeTransactionId(value: String): String =
    value.trim().uppercase().filter { it.isLetterOrDigit() || it in "-_./" }

data class DashboardStats(
    val liquidityCents: Long,
    val inCirculationCents: Long,
    val completionPercent: Double,
    val activeRequests: Int,
    val completedOperations: Int,
    val activeUsers: Int,
)

data class AdminOverviewStats(
    val communityLiquidityCents: Long,
    val inCirculationCents: Long,
    val completionPercent: Double,
    val activeRequests: Int,
    val completedOperations: Int,
    val activeUsers: Int,
    val totalUsers: Int,
    val pendingUsers: Int,
    val blockedUsers: Int,
    val pendingRequests: Int,
    val openRequests: Int,
    val fundedRequests: Int,
    val pendingContributions: Int,
    val pendingReceipts: Int,
    val adminFeeDueCents: Long,
    val roadmapStep: Int,
    val roadmapCapacity: Int,
    val generatedAt: Long,
)

fun now(): Long = Instant.now().toEpochMilli()
