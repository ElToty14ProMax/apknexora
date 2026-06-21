<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Services\SecurityService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use Tests\TestCase;

final class ReceiptSubmissionFallbackTest extends TestCase
{
    use RefreshDatabase;

    public function test_receipt_is_saved_for_manual_review_when_ocr_cannot_read_it(): void
    {
        config([
            'services.ocr.provider' => 'mock',
            'services.ocr.allow_mock' => true,
            'services.ocr.mock_text' => '',
        ]);

        [$donorId, $token] = $this->seedPendingContribution();
        $image = 'valid-image-bytes-for-fallback';
        $hash = hash('sha256', $image);

        $response = $this
            ->withHeader('Authorization', "Bearer {$token}")
            ->postJson('/support-requests/contributions/contribution-fallback/receipt', [
                'side' => 'SENDER',
                'amountCents' => 100,
                'receiptHash' => $hash,
                'receiptImageBase64' => base64_encode($image),
                'receiptMimeType' => 'image/jpeg',
            ]);

        $response
            ->assertCreated()
            ->assertJsonPath('contributionId', 'contribution-fallback')
            ->assertJsonPath('hasSenderReceipt', true)
            ->assertJsonPath('transactionId', null);

        $this->assertDatabaseHas('contributions', [
            'id' => 'contribution-fallback',
            'donor_id' => $donorId,
            'sender_receipt_hash' => $hash,
            'transaction_id' => null,
        ]);
    }

    public function test_admin_can_confirm_a_pending_contribution_without_receipt_photos(): void
    {
        [, , $adminToken] = $this->seedPendingContribution(withAdmin: true);

        $response = $this
            ->withHeader('Authorization', "Bearer {$adminToken}")
            ->postJson('/admin/contributions/contribution-fallback/confirm');

        $response->assertOk();
        $this->assertDatabaseHas('contributions', [
            'id' => 'contribution-fallback',
            'status' => 'CONFIRMED',
        ]);
        $this->assertDatabaseHas('support_requests', [
            'id' => 'support-fallback',
            'funded_cents' => 100,
            'status' => 'FUNDED',
        ]);
    }

    /**
     * @return array{0: string, 1: string, 2?: string}
     */
    private function seedPendingContribution(bool $withAdmin = false): array
    {
        $security = app(SecurityService::class);
        $now = (int) floor(microtime(true) * 1000);
        $requesterId = (string) Str::uuid();
        $donorId = (string) Str::uuid();
        $donorToken = 'donor-fallback-token';

        DB::table('users')->insert([
            $this->userPayload($security, $requesterId, 'requester-fallback@example.com', '52998224725', 'USER', $now),
            $this->userPayload($security, $donorId, 'donor-fallback@example.com', '11144477735', 'USER', $now),
        ]);

        DB::table('auth_tokens')->insert([
            'token_hash' => $security->hashToken($donorToken),
            'user_id' => $donorId,
            'expires_at' => $now + 3600000,
            'created_at_ms' => $now,
        ]);

        DB::table('support_requests')->insert([
            'id' => 'support-fallback',
            'requester_id' => $requesterId,
            'public_code' => 'AP-FALLBACK',
            'amount_cents' => 100,
            'funded_cents' => 0,
            'due_days' => 7,
            'status' => 'OPEN',
            'created_at_ms' => $now,
            'approved_at' => $now,
        ]);

        DB::table('contributions')->insert([
            'id' => 'contribution-fallback',
            'request_id' => 'support-fallback',
            'donor_id' => $donorId,
            'amount_cents' => 100,
            'status' => 'PENDING_ADMIN',
            'created_at_ms' => $now,
            'verification_status' => 'pending_verification',
            'admin_review_required' => false,
            'has_sender_receipt' => false,
            'has_receiver_receipt' => false,
        ]);

        if (! $withAdmin) {
            return [$donorId, $donorToken];
        }

        $adminId = (string) Str::uuid();
        $adminToken = 'admin-fallback-token';
        DB::table('users')->insert(
            $this->userPayload($security, $adminId, 'admin-fallback@example.com', '39053344705', 'ADMIN', $now)
        );
        DB::table('auth_tokens')->insert([
            'token_hash' => $security->hashToken($adminToken),
            'user_id' => $adminId,
            'expires_at' => $now + 3600000,
            'created_at_ms' => $now,
        ]);

        return [$donorId, $donorToken, $adminToken];
    }

    private function userPayload(
        SecurityService $security,
        string $id,
        string $email,
        string $cpf,
        string $role,
        int $now,
    ): array {
        return [
            'id' => $id,
            'public_id' => 'NX-'.substr(strtoupper(str_replace('-', '', $id)), 0, 8),
            'name' => 'Pessoa Teste',
            'email' => $email,
            'email_verified' => true,
            'cpf_hash' => $security->hashCpf($cpf),
            'cpf_cipher' => $security->encrypt($cpf),
            'birthdate' => '1990-01-01',
            'pix_cipher' => $security->encrypt((string) Str::uuid()),
            'password_hash' => $security->hashPassword('SenhaTeste123'),
            'status' => 'APPROVED',
            'role' => $role,
            'invite_code' => substr(strtoupper(str_replace('-', '', $id)), 0, 8),
            'created_at_ms' => $now,
        ];
    }
}
