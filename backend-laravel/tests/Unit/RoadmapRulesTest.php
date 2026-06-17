<?php

namespace Tests\Unit;

use App\Services\RoadmapRules;
use PHPUnit\Framework\TestCase;

class RoadmapRulesTest extends TestCase
{
    public function test_roadmap_unlocks_capacity_from_veteran_level_counts(): void
    {
        $this->assertStep([], 1, 20);
        $this->assertStep([2 => 5], 2, 50);
        $this->assertStep([2 => 5, 3 => 5], 3, 100);
        $this->assertStep([2 => 10, 3 => 10, 4 => 5], 4, 200);
        $this->assertStep([8 => 10, 9 => 5, 10 => 5], 10, 5000);
    }

    private function assertStep(array $levelCounts, int $expectedStep, int $expectedCapacity): void
    {
        $step = RoadmapRules::currentStep($levelCounts);

        $this->assertSame($expectedStep, $step['step']);
        $this->assertSame($expectedCapacity, $step['capacity']);
    }
}
