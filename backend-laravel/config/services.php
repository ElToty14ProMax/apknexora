<?php

return [

    /*
    |--------------------------------------------------------------------------
    | Third Party Services
    |--------------------------------------------------------------------------
    |
    | This file is for storing the credentials for third party services such
    | as Mailgun, Postmark, AWS and more. This file provides the de facto
    | location for this type of information, allowing packages to have
    | a conventional file to locate the various service credentials.
    |
    */

    'postmark' => [
        'key' => env('POSTMARK_API_KEY'),
    ],

    'resend' => [
        'key' => env('RESEND_API_KEY'),
    ],

    'ses' => [
        'key' => env('AWS_ACCESS_KEY_ID'),
        'secret' => env('AWS_SECRET_ACCESS_KEY'),
        'region' => env('AWS_DEFAULT_REGION', 'us-east-1'),
    ],

    'slack' => [
        'notifications' => [
            'bot_user_oauth_token' => env('SLACK_BOT_USER_OAUTH_TOKEN'),
            'channel' => env('SLACK_BOT_USER_DEFAULT_CHANNEL'),
        ],
    ],

    'ocr' => [
        'provider' => env('OCR_PROVIDER', ''),
        'google_api_key' => env('GOOGLE_VISION_API_KEY', ''),
        'openai_api_key' => env('OPENAI_API_KEY', ''),
        'ocrspace_api_key' => env('OCR_SPACE_API_KEY', ''),
        'ocrspace_language' => env('OCR_SPACE_LANGUAGE', 'por'),
        'ocrspace_engine' => env('OCR_SPACE_ENGINE', '2'),
        'tesseract_cmd' => env('TESSERACT_CMD', 'tesseract'),
        'tesseract_language' => env('TESSERACT_LANGUAGE', 'por'),
        'tesseract_psm' => env('TESSERACT_PSM', '3'),
        'allow_mock' => env('OCR_ALLOW_MOCK', false),
        'mock_text' => env('OCR_MOCK_TEXT', ''),
    ],

];
