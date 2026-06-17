<?php

namespace Tests\Unit;

use App\Services\ReputationRules;
use PHPUnit\Framework\TestCase;

class ReputationRulesTest extends TestCase
{
    public function test_support_limits_match_nexora_level_milestones(): void
    {
        $expectedLimits = [
            1 => 0,
            2 => 2000,
            3 => 3000,
            5 => 6300,
            10 => 16000,
            20 => 41000,
            50 => 700000,
            100 => 82000000,
        ];

        foreach ($expectedLimits as $level => $limitCents) {
            $this->assertSame($limitCents, ReputationRules::supportLimitCents($level), "Level {$level}");
        }
    }

    public function test_xp_requirements_use_ten_point_five_percent_growth(): void
    {
        foreach ([1, 2, 3, 4, 5, 10, 20, 50, 100] as $level) {
            $expectedXp = (int) round(100.0 * (1.105 ** ($level - 1)));

            $this->assertSame($expectedXp, ReputationRules::xpRequiredForLevel($level), "Level {$level}");
        }
    }

    public function test_xp_buffs_are_additive_and_capped_at_one_hundred_percent(): void
    {
        $this->assertSame(10, ReputationRules::recalculateBuffBps(100000, 0, 0));
        $this->assertSame(30, ReputationRules::recalculateBuffBps(0, 100000, 0));
        $this->assertSame(10, ReputationRules::recalculateBuffBps(0, 0, 1));
        $this->assertSame(50, ReputationRules::recalculateBuffBps(100000, 100000, 1));
        $this->assertSame(10000, ReputationRules::recalculateBuffBps(1000000000, 1000000000, 1000));
    }
}
