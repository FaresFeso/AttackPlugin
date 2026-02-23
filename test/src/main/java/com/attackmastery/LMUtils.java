package com.attackmastery;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class LMUtils {
    private static boolean available = false;
    
    public static boolean init() {
        try {
            org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("LevelledMobs");
            available = (plugin != null);
            return available;
        } catch (Throwable e) {
            available = false;
            return false;
        }
    }
    
    public static boolean isAvailable() {
        return available;
    }
    
    public static boolean isLevelled(LivingEntity entity) {
        return false;
    }
    
    public static int getMobLevel(LivingEntity entity) {
        return 0;
    }
}
