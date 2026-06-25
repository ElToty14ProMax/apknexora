<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('contributions', function (Blueprint $table) {
            $table->string('return_status', 40)->nullable()->index();
            $table->string('return_transaction_id', 80)->nullable()->unique();
            $table->string('return_receipt_hash', 64)->nullable();
            $table->longText('return_receipt_image_base64')->nullable();
            $table->string('return_receipt_mime_type')->nullable();
            $table->string('return_receipt_date')->nullable();
            $table->bigInteger('return_submitted_at')->nullable();
            $table->bigInteger('return_confirmed_at')->nullable();
        });
    }

    public function down(): void
    {
        Schema::table('contributions', function (Blueprint $table) {
            $table->dropUnique(['return_transaction_id']);
            $table->dropIndex(['return_status']);
            $table->dropColumn([
                'return_status',
                'return_transaction_id',
                'return_receipt_hash',
                'return_receipt_image_base64',
                'return_receipt_mime_type',
                'return_receipt_date',
                'return_submitted_at',
                'return_confirmed_at',
            ]);
        });
    }
};
