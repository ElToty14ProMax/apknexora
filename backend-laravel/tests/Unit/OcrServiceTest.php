<?php

declare(strict_types=1);

namespace Tests\Unit;

use App\Services\OcrService;
use Tests\TestCase;

final class OcrServiceTest extends TestCase
{
    public function test_it_reads_ocr_settings_from_laravel_configuration(): void
    {
        config([
            'services.ocr.provider' => 'ocrspace',
            'services.ocr.ocrspace_api_key' => 'configured-key',
        ]);

        $service = new OcrService;

        $this->assertTrue($service->isConfigured());
        $this->assertSame('ocrspace', $service->getProvider());
    }
}
