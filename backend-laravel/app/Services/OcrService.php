<?php

declare(strict_types=1);

namespace App\Services;

use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class OcrService
{
    private string $provider;
    private string $googleApiKey;
    private string $openAiApiKey;
    private string $ocrSpaceApiKey;
    private string $tesseractCmd;
    private string $tesseractLanguage;
    private string $tesseractPsm;
    private string $ocrSpaceLanguage;
    private string $ocrSpaceEngine;

    public function __construct()
    {
        $this->googleApiKey = (string) config('services.ocr.google_api_key', '');
        $this->openAiApiKey = (string) config('services.ocr.openai_api_key', '');
        $this->ocrSpaceApiKey = (string) config('services.ocr.ocrspace_api_key', '');
        $this->tesseractCmd = (string) config('services.ocr.tesseract_cmd', 'tesseract');
        $this->tesseractLanguage = (string) config('services.ocr.tesseract_language', 'por');
        $this->tesseractPsm = (string) config('services.ocr.tesseract_psm', '3');
        $this->ocrSpaceLanguage = (string) config('services.ocr.ocrspace_language', 'por');
        $this->ocrSpaceEngine = (string) config('services.ocr.ocrspace_engine', '2');
        $this->provider = $this->resolveProvider();
    }

    private function resolveProvider(): string
    {
        $configured = strtolower(trim((string) config('services.ocr.provider', '')));

        if ($configured === 'tesseract') {
            return 'tesseract';
        }
        if ($configured === 'google' && $this->googleApiKey !== '') {
            return 'google';
        }
        if ($configured === 'ocrspace' && $this->ocrSpaceApiKey !== '') {
            return 'ocrspace';
        }
        if ($configured === 'openai' && $this->openAiApiKey !== '') {
            return 'openai';
        }
        if ($configured === 'mock' && $this->mockAllowed()) {
            return 'mock';
        }

        return '';
    }

    public function isConfigured(): bool
    {
        return in_array($this->provider, ['tesseract', 'google', 'ocrspace', 'openai', 'mock'], true);
    }

    public function getProvider(): string
    {
        return $this->provider;
    }

    public function extractText(string $imageBase64, string $mimeType = 'image/jpeg'): string
    {
        if ($this->provider === '') {
            Log::error('OCR provider not configured. Set OCR_PROVIDER to tesseract, google, ocrspace, or openai in .env');

            return '';
        }

        return match ($this->provider) {
            'tesseract' => $this->tesseractOcr($imageBase64, $mimeType),
            'google' => $this->googleVision($imageBase64),
            'ocrspace' => $this->ocrSpace($imageBase64, $mimeType),
            'openai' => $this->openAiVision($imageBase64, $mimeType),
            'mock' => $this->mockExtract(),
            default => '',
        };
    }

    private function tesseractOcr(string $imageBase64, string $mimeType): string
    {
        try {
            $extension = match ($mimeType) {
                'image/png' => 'png',
                'image/webp' => 'webp',
                default => 'jpg',
            };

            $filename = 'ocr_'.uniqid('', true).'.'.$extension;
            $path = storage_path('app/temp/'.$filename);

            if (! is_dir(storage_path('app/temp'))) {
                mkdir(storage_path('app/temp'), 0755, true);
            }

            $imageData = base64_decode($imageBase64, true);
            if ($imageData === false) {
                Log::warning('Tesseract: failed to decode base64 image');

                return '';
            }

            file_put_contents($path, $imageData);

            $cmd = sprintf(
                '%s %s stdout -l %s --psm %s 2>&1',
                escapeshellcmd($this->tesseractCmd),
                escapeshellarg($path),
                escapeshellarg($this->tesseractLanguage),
                escapeshellarg($this->tesseractPsm)
            );

            $output = shell_exec($cmd);
            @unlink($path);

            if (empty($output)) {
                Log::warning('Tesseract: no text extracted');

                return '';
            }

            return trim($output);
        } catch (\Throwable $e) {
            Log::error('Tesseract OCR exception', ['error' => $e->getMessage()]);

            return '';
        }
    }

    private function googleVision(string $imageBase64): string
    {
        try {
            $response = Http::post("https://vision.googleapis.com/v1/images:annotate?key={$this->googleApiKey}", [
                'requests' => [[
                    'image' => ['content' => $imageBase64],
                    'features' => [['type' => 'TEXT_DETECTION', 'maxResults' => 1]],
                ]],
            ]);

            if ($response->failed()) {
                Log::warning('Google Vision API error', ['status' => $response->status(), 'body' => $response->body()]);

                return '';
            }

            $data = $response->json();

            return (string) ($data['responses'][0]['textAnnotations'][0]['description'] ?? '');
        } catch (\Throwable $e) {
            Log::error('Google Vision API exception', ['error' => $e->getMessage()]);

            return '';
        }
    }

    private function ocrSpace(string $imageBase64, string $mimeType): string
    {
        try {
            $response = Http::withHeaders([
                'apikey' => $this->ocrSpaceApiKey,
            ])->asForm()->timeout(25)->post('https://api.ocr.space/parse/image', [
                'base64Image' => "data:{$mimeType};base64,{$imageBase64}",
                'language' => $this->ocrSpaceLanguage,
                'isOverlayRequired' => 'false',
                'detectOrientation' => 'true',
                'scale' => 'true',
                'OCREngine' => $this->ocrSpaceEngine,
            ]);

            if ($response->failed()) {
                Log::warning('OCR.space API error', ['status' => $response->status(), 'body' => $response->body()]);

                return '';
            }

            $data = $response->json();
            if (isset($data['error']) || isset($data['details'])) {
                Log::warning('OCR.space API rejected the request', [
                    'error' => $data['error'] ?? null,
                    'details' => $data['details'] ?? null,
                ]);

                return '';
            }
            if (($data['IsErroredOnProcessing'] ?? false) === true) {
                Log::warning('OCR.space processing error', ['message' => $data['ErrorMessage'] ?? null]);

                return '';
            }

            return trim((string) ($data['ParsedResults'][0]['ParsedText'] ?? ''));
        } catch (\Throwable $e) {
            Log::error('OCR.space API exception', ['error' => $e->getMessage()]);

            return '';
        }
    }

    private function openAiVision(string $imageBase64, string $mimeType): string
    {
        try {
            $imageDataUrl = "data:{$mimeType};base64,{$imageBase64}";
            $response = Http::withToken($this->openAiApiKey)->post('https://api.openai.com/v1/chat/completions', [
                'model' => 'gpt-4o-mini',
                'messages' => [[
                    'role' => 'user',
                    'content' => [
                        ['type' => 'text', 'text' => 'Extraia apenas o texto literalmente visivel na imagem do comprovante Pix. Nao invente, complete ou corrija IDs, datas, nomes ou valores. Se uma parte estiver ilegivel, omita essa parte. Retorne somente o texto extraido, sem comentarios.'],
                        ['type' => 'image_url', 'image_url' => ['url' => $imageDataUrl, 'detail' => 'high']],
                    ],
                ]],
                'max_tokens' => 1000,
            ]);

            if ($response->failed()) {
                Log::warning('OpenAI Vision API error', ['status' => $response->status(), 'body' => $response->body()]);

                return '';
            }

            $data = $response->json();

            return (string) ($data['choices'][0]['message']['content'] ?? '');
        } catch (\Throwable $e) {
            Log::error('OpenAI Vision API exception', ['error' => $e->getMessage()]);

            return '';
        }
    }

    private function mockExtract(): string
    {
        return (string) config('services.ocr.mock_text', '');
    }

    private function mockAllowed(): bool
    {
        return app()->environment(['local', 'testing'])
            || filter_var(config('services.ocr.allow_mock', false), FILTER_VALIDATE_BOOLEAN);
    }
}
