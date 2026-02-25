package com.attackmastery;

public final class PathEvolution {
    private PathEvolution() {
    }

    public static int tierForAttackLevel(int attackLevel) {
        if (attackLevel >= 150) return 4;
        if (attackLevel >= 100) return 3;
        if (attackLevel >= 60) return 2;
        if (attackLevel >= 30) return 1;
        return 0;
    }

    public static String tierName(int tier) {
        return switch (tier) {
            case 4 -> "Ascendant";
            case 3 -> "Elite";
            case 2 -> "Veteran";
            case 1 -> "Awakened";
            default -> "Base";
        };
    }
}