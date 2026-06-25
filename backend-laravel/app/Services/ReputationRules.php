<?php

namespace App\Services;

class ReputationRules
{
    public const MIN_HELP_LEVEL = 2;

    public const MIN_HELP_XP = 100;

    public const ADMIN_FEE_BLOCK_LIMIT_CENTS = 500;

    public const MIN_CONTRIBUTION_CENTS = 500;

    private const MAX_BUFF_BPS = 10000;

    private const SUPPORT_LIMIT_ANCHORS_REAIS = [
        2 => 20.0,
        3 => 30.0,
        5 => 63.0,
        10 => 160.0,
        20 => 410.0,
        50 => 7000.0,
        100 => 820000.0,
    ];

    public static function xpRequiredForLevel(int $level): int
    {
        return (int) round(100.0 * (1.105 ** ($level - 1)));
    }

    public static function totalXpRequiredToEnterLevel(int $level): int
    {
        $total = 0;
        for ($current = 1; $current < $level; $current++) {
            $total += self::xpRequiredForLevel($current);
        }

        return $total;
    }

    public static function levelForXp(int $totalXp): int
    {
        $level = 1;
        $remaining = $totalXp;
        while ($remaining >= self::xpRequiredForLevel($level) && $level < 1000) {
            $remaining -= self::xpRequiredForLevel($level);
            $level++;
        }

        return $level;
    }

    public static function xpIntoLevel(int $totalXp): int
    {
        return $totalXp - self::totalXpRequiredToEnterLevel(self::levelForXp($totalXp));
    }

    public static function supportLimitCents(int $level): int
    {
        if ($level < self::MIN_HELP_LEVEL) {
            return 0;
        }

        $amountReais = self::supportLimitReais($level);
        if ($amountReais > intdiv(PHP_INT_MAX, 100)) {
            return PHP_INT_MAX;
        }

        return (int) round($amountReais) * 100;
    }

    private static function supportLimitReais(int $level): float
    {
        if (array_key_exists($level, self::SUPPORT_LIMIT_ANCHORS_REAIS)) {
            return self::SUPPORT_LIMIT_ANCHORS_REAIS[$level];
        }

        $anchorLevel = self::MIN_HELP_LEVEL;
        $amountReais = self::SUPPORT_LIMIT_ANCHORS_REAIS[self::MIN_HELP_LEVEL];
        foreach (self::SUPPORT_LIMIT_ANCHORS_REAIS as $currentLevel => $currentAmountReais) {
            if ($currentLevel > $level) {
                break;
            }
            $anchorLevel = $currentLevel;
            $amountReais = $currentAmountReais;
        }

        for ($targetLevel = $anchorLevel + 1; $targetLevel <= $level; $targetLevel++) {
            $amountReais *= self::supportLimitMultiplierForLevel($targetLevel);
            if ($amountReais > intdiv(PHP_INT_MAX, 100)) {
                return (float) intdiv(PHP_INT_MAX, 100);
            }
        }

        return $amountReais;
    }

    private static function supportLimitMultiplierForLevel(int $level): float
    {
        if ($level <= 3) {
            return 1.5;
        }
        if ($level <= 5) {
            return 1.4;
        }
        if ($level <= 7) {
            return 1.3;
        }
        if ($level <= 9) {
            return 1.2;
        }

        return 1.1;
    }

    public static function adminFeeFor(int $amountCents): int
    {
        return max(intdiv($amountCents, 100), 0);
    }

    public static function adminFeeLimitCents(int $level): int
    {
        return self::ADMIN_FEE_BLOCK_LIMIT_CENTS;
    }

    public static function canRequestHelp(object|array $user): bool
    {
        $level = (int) (is_array($user) ? $user['level'] : $user->level);
        $xp = (int) (is_array($user) ? $user['xp'] : $user->xp);

        return $level >= self::MIN_HELP_LEVEL && $xp >= self::MIN_HELP_XP;
    }

    public static function xpForConfirmedContribution(int $amountCents, int $buffBps): int
    {
        $baseXp = max(intdiv($amountCents, 100), 0);

        return intdiv($baseXp * (10000 + $buffBps), 10000);
    }

    public static function recalculateBuffBps(int $onTimeReturnedCents, int $earlyReturnedCents, int $guestsAtLevelFive): int
    {
        $onTimeBps = intdiv($onTimeReturnedCents, 100000) * 10;
        $earlyBps = intdiv($earlyReturnedCents, 100000) * 30;
        $guestBps = $guestsAtLevelFive * 10;

        return min($onTimeBps + $earlyBps + $guestBps, self::MAX_BUFF_BPS);
    }
}
