package com.attackmastery;

public enum MasteryPath {
    NONE,
    SWORD,
    AXE,
    BOW,
    CRIT;

    public static MasteryPath fromString(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        try {
            return MasteryPath.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return NONE;
        }
    }

    public String displayName() {
        return switch (this) {
            case SWORD -> "Sword Mastery";
            case AXE -> "Axe Mastery";
            case BOW -> "Archer Mastery";
            case CRIT -> "Critical Mastery";
            case NONE -> "Unchosen";
        };
    }
}