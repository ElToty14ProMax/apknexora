<?php

namespace Tests\Feature;

use App\Services\SecurityService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use Tests\TestCase;

class RepaymentFlowTest extends TestCase
{
    use RefreshDatabase;

    public function test_requester_can_submit_proof_and_donor_confirmation_finishes_the_request(): void
    {
        [$requesterId, $donorId, $requesterToken, $donorToken, $now] = $this->seedFundedRequest(false);

        $list = $this->withToken($requesterToken)->getJson('/repayments/mine');
        $list->assertOk()
            ->assertJsonPath('summary.pendingCount', 1)
            ->assertJsonPath('owed.0.counterpartyPublicId', 'NX-DONOR')
            ->assertJsonPath('owed.0.amountCents', 2000)
            ->assertJsonPath('owed.0.status', 'PENDING');
        $repaymentPixCode = (string) $list->json('owed.0.pixCopyCode');
        $this->assertStringContainsString('01369f4c2c7e-7f9b-45c0-8c33-0fa84fb8867b', $repaymentPixCode);
        $this->assertStringContainsString('540520.00', $repaymentPixCode);
        $this->assertMatchesRegularExpression('/6304[0-9A-F]{4}$/', $repaymentPixCode);

        $png = base64_decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=', true);
        $proof = $this->withToken($requesterToken)->postJson('/repayments/contribution-repayment/proof', [
            'transactionId' => 'RETURN-TX-123456',
            'receiptHash' => hash('sha256', $png),
            'receiptImageBase64' => base64_encode($png),
            'receiptMimeType' => 'image/png',
        ]);
        $proof->assertCreated()->assertJsonPath('status', 'PROOF_SUBMITTED');

        $receivable = $this->withToken($donorToken)->getJson('/repayments/mine');
        $receivable->assertOk()
            ->assertJsonPath('receivable.0.counterpartyPublicId', 'NX-REQUESTER')
            ->assertJsonPath('receivable.0.status', 'PROOF_SUBMITTED');

        $confirmed = $this->withToken($donorToken)->postJson('/repayments/contribution-repayment/confirm');
        $confirmed->assertOk()->assertJsonPath('supportCompleted', true);

        $this->assertDatabaseHas('support_requests', ['id' => 'support-repayment', 'status' => 'RETURNED']);
        $this->assertDatabaseHas('contributions', ['id' => 'contribution-repayment', 'return_status' => 'CONFIRMED']);
        $this->assertSame(100, (int) DB::table('users')->where('id', $requesterId)->value('xp'));
        $this->assertSame(2000, (int) DB::table('users')->where('id', $requesterId)->value('early_returned_cents'));
        $this->assertNotNull(DB::table('contributions')->where('id', 'contribution-repayment')->value('return_confirmed_at'));
        $this->assertNotNull(DB::table('support_requests')->where('id', 'support-repayment')->value('returned_at'));
    }

    public function test_overdue_repayment_blocks_new_support_requests(): void
    {
        [, , $requesterToken] = $this->seedFundedRequest(true);

        $this->withToken($requesterToken)->postJson('/support-requests', [
            'amountCents' => 2000,
            'dueDays' => 7,
            'description' => 'Nova solicitação',
        ])->assertStatus(409)->assertJsonFragment(['error' => 'Você possui devoluções em atraso. Regularize-as antes de criar uma nova solicitação.']);
    }

    private function seedFundedRequest(bool $overdue): array
    {
        $security = app(SecurityService::class);
        $now = (int) floor(microtime(true) * 1000);
        $requesterId = (string) Str::uuid();
        $donorId = (string) Str::uuid();
        $requesterToken = 'requester-repayment-token';
        $donorToken = 'donor-repayment-token';

        DB::table('users')->insert([
            $this->userPayload($security, $requesterId, 'NX-REQUESTER', 'requester-repayment@example.com', '52998224725', '550e8400-e29b-41d4-a716-446655440000', $now, 100, 2),
            $this->userPayload($security, $donorId, 'NX-DONOR', 'donor-repayment@example.com', '11144477735', '9f4c2c7e-7f9b-45c0-8c33-0fa84fb8867b', $now, 0, 1),
        ]);
        DB::table('auth_tokens')->insert([
            ['token_hash' => $security->hashToken($requesterToken), 'user_id' => $requesterId, 'expires_at' => $now + 3600000, 'created_at_ms' => $now],
            ['token_hash' => $security->hashToken($donorToken), 'user_id' => $donorId, 'expires_at' => $now + 3600000, 'created_at_ms' => $now],
        ]);
        DB::table('support_requests')->insert([
            'id' => 'support-repayment',
            'requester_id' => $requesterId,
            'public_code' => 'AP-RETURN',
            'amount_cents' => 2000,
            'funded_cents' => 2000,
            'due_days' => 7,
            'due_at' => $overdue ? $now - 1000 : $now + 86400000,
            'status' => 'FUNDED',
            'created_at_ms' => $now - 86400000,
            'approved_at' => $now - 86400000,
        ]);
        DB::table('contributions')->insert([
            'id' => 'contribution-repayment',
            'request_id' => 'support-repayment',
            'donor_id' => $donorId,
            'amount_cents' => 2000,
            'status' => 'CONFIRMED',
            'return_status' => 'PENDING',
            'created_at_ms' => $now - 86400000,
            'confirmed_at' => $now - 86400000,
            'verification_status' => 'match',
            'admin_review_required' => false,
            'has_sender_receipt' => true,
            'has_receiver_receipt' => true,
        ]);

        return [$requesterId, $donorId, $requesterToken, $donorToken, $now];
    }

    private function userPayload(SecurityService $security, string $id, string $publicId, string $email, string $cpf, string $pix, int $now, int $xp, int $level): array
    {
        return [
            'id' => $id,
            'public_id' => $publicId,
            'name' => $publicId === 'NX-DONOR' ? 'Pessoa Doadora' : 'Pessoa Solicitante',
            'email' => $email,
            'email_verified' => true,
            'cpf_hash' => $security->hashCpf($cpf),
            'cpf_cipher' => $security->encrypt($cpf),
            'birthdate' => '1990-01-01',
            'pix_cipher' => $security->encrypt($pix),
            'password_hash' => $security->hashPassword('SenhaTeste123'),
            'status' => 'APPROVED',
            'role' => 'USER',
            'xp' => $xp,
            'level' => $level,
            'invite_code' => substr(str_replace('-', '', $publicId).'ABCDEFGH', 0, 8),
            'created_at_ms' => $now,
        ];
    }
}
