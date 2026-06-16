<?php

namespace Tests\Feature;

use App\Services\SecurityService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Mail\Transport\ArrayTransport;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Mail;
use Illuminate\Support\Str;
use Symfony\Component\Mailer\SentMessage;
use Symfony\Component\Mime\Email as SymfonyEmail;
use Tests\TestCase;

class EmailVerificationTest extends TestCase
{
    use RefreshDatabase;

    public function test_email_verification_returns_session_and_accepts_pasted_code(): void
    {
        $security = app(SecurityService::class);
        $email = 'cliente@example.com';
        $code = '123456';
        $userId = $this->insertUnverifiedUser($security, $email, $code);

        $response = $this->postJson('/auth/verify-email', [
            'email' => ' CLIENTE@EXAMPLE.COM ',
            'code' => '123 456',
        ]);

        $response
            ->assertOk()
            ->assertJsonPath('message', 'E-mail verificado. Entrada realizada.')
            ->assertJsonPath('profile.email', $email)
            ->assertJsonStructure(['token', 'profile' => ['id', 'email', 'status']]);

        $this->assertDatabaseHas('users', [
            'id' => $userId,
            'email_verified' => true,
            'verification_code_hash' => null,
            'verification_expires_at' => null,
        ]);
        $this->assertSame(1, DB::table('auth_tokens')->where('user_id', $userId)->count());
    }

    public function test_failed_email_verification_keeps_user_unverified_and_does_not_create_session(): void
    {
        $security = app(SecurityService::class);
        $email = 'cliente@example.com';
        $userId = $this->insertUnverifiedUser($security, $email, '123456');

        $response = $this->postJson('/auth/verify-email', [
            'email' => $email,
            'code' => '000000',
        ]);

        $response->assertStatus(400);
        $this->assertDatabaseHas('users', [
            'id' => $userId,
            'email_verified' => false,
        ]);
        $this->assertSame(0, DB::table('auth_tokens')->where('user_id', $userId)->count());
    }

    public function test_registration_and_recovery_codes_are_sent_from_nexora_address(): void
    {
        $this->withoutMiddleware(\Illuminate\Routing\Middleware\ThrottleRequests::class);
        $transport = $this->configureArrayMail();

        $response = $this->postJson('/auth/register', [
            'name' => 'Cliente Teste',
            'email' => 'cadastro@example.com',
            'cpf' => '52998224725',
            'birthdate' => '1990-01-01',
            'pixKey' => '550e8400-e29b-41d4-a716-446655440000',
            'password' => 'SenhaTeste123',
        ]);

        $response->assertCreated();
        $this->assertCount(1, $transport->messages());
        $this->assertNexoraSender(
            $transport->messages()->last(),
            'cadastro@example.com',
            'Código de verificação Nexora'
        );

        $response = $this->postJson('/auth/recover-password', [
            'email' => 'cadastro@example.com',
        ]);

        $response->assertOk();
        $this->assertCount(2, $transport->messages());
        $this->assertNexoraSender(
            $transport->messages()->last(),
            'cadastro@example.com',
            'Recuperação de acesso Nexora'
        );
    }

    public function test_profile_returns_business_cnpj_pix_for_pending_admin_fee(): void
    {
        config(['nexora.admin_pix_key' => '67.018.679/0001-17']);

        $security = app(SecurityService::class);
        $token = 'profile-test-token';
        $userId = $this->insertVerifiedUser($security, 'taxa@example.com', adminFeeDueCents: 1200);
        DB::table('auth_tokens')->insert([
            'token_hash' => $security->hashToken($token),
            'user_id' => $userId,
            'expires_at' => $this->nowMs() + 3600000,
            'created_at_ms' => $this->nowMs(),
        ]);

        $response = $this->withHeader('Authorization', "Bearer {$token}")->getJson('/me');

        $response
            ->assertOk()
            ->assertJsonPath('adminPixKey', '67.018.679/0001-17');
    }

    private function insertUnverifiedUser(SecurityService $security, string $email, string $code): string
    {
        return $this->insertUser($security, $email, [
            'email_verified' => false,
            'verification_code_hash' => $security->hashVerificationCode($email, $code),
            'verification_expires_at' => $this->nowMs() + 30 * 60 * 1000,
        ]);
    }

    private function insertVerifiedUser(SecurityService $security, string $email, int $adminFeeDueCents = 0): string
    {
        return $this->insertUser($security, $email, [
            'email_verified' => true,
            'verification_code_hash' => null,
            'verification_expires_at' => null,
            'admin_fee_due_cents' => $adminFeeDueCents,
        ]);
    }

    private function insertUser(SecurityService $security, string $email, array $overrides): string
    {
        $id = (string) Str::uuid();
        $now = $this->nowMs();

        DB::table('users')->insert(array_merge([
            'id' => $id,
            'public_id' => 'NX-'.Str::upper(Str::random(8)),
            'name' => 'Cliente Teste',
            'email' => $email,
            'email_verified' => false,
            'verification_code_hash' => null,
            'verification_expires_at' => null,
            'cpf_hash' => $security->hashCpf('52998224725'),
            'cpf_cipher' => $security->encrypt('52998224725'),
            'pix_cipher' => $security->encrypt('550e8400-e29b-41d4-a716-446655440000'),
            'password_hash' => $security->hashPassword('SenhaTeste123'),
            'status' => 'PENDING_REVIEW',
            'role' => 'USER',
            'invite_code' => Str::upper(Str::random(8)),
            'created_at_ms' => $now,
            'admin_fee_due_cents' => 0,
        ], $overrides));

        return $id;
    }

    private function configureArrayMail(): ArrayTransport
    {
        config([
            'mail.default' => 'array',
            'mail.from.address' => 'nexora@nexoraappbr.com',
            'mail.from.name' => 'Nexora',
            'mail.mailers.smtp.username' => 'nexora@nexoraappbr.com',
            'mail.mailers.smtp.password' => 'test-password',
        ]);
        Mail::forgetMailers();

        $transport = Mail::mailer()->getSymfonyTransport();
        $this->assertInstanceOf(ArrayTransport::class, $transport);
        $transport->flush();

        return $transport;
    }

    private function assertNexoraSender(SentMessage $sentMessage, string $to, string $subject): void
    {
        $email = $sentMessage->getOriginalMessage();
        $this->assertInstanceOf(SymfonyEmail::class, $email);
        $this->assertSame('nexora@nexoraappbr.com', $email->getFrom()[0]->getAddress());
        $this->assertSame('Nexora', $email->getFrom()[0]->getName());
        $this->assertSame($to, $email->getTo()[0]->getAddress());
        $this->assertSame($subject, $email->getSubject());
    }

    private function nowMs(): int
    {
        return (int) floor(microtime(true) * 1000);
    }
}
