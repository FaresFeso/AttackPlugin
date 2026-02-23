package com.attackmastery;

import org.bukkit.boss.BossBar;

public class PlayerData {
    private int level;
    private double xp;
    private double xpNeeded;
    private BossBar bossBar;
    
    public PlayerData(int level, double xp) {
        this.level = level;
        this.xp = xp;
        this.xpNeeded = 0;
        this.bossBar = null;
    }
    
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    
    public double getXp() { return xp; }
    public void setXp(double xp) { this.xp = xp; }
    
    public double getXpNeeded() { return xpNeeded; }
    public void setXpNeeded(double xpNeeded) { this.xpNeeded = xpNeeded; }
    
    public BossBar getBossBar() { return bossBar; }
    public void setBossBar(BossBar bossBar) { this.bossBar = bossBar; }
}
