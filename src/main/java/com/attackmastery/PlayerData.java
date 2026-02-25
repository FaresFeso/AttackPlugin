package com.attackmastery;

import org.bukkit.boss.BossBar;

public class PlayerData {
    private int level;
    private double xp;
    private double xpNeeded;
    private BossBar bossBar;
    private MasteryPath masteryPath;
    private int masteryLevel;
    private double masteryXp;
    private String masteryObjective;
    private int masteryObjectiveProgress;
    private int masteryObjectiveTarget;
    private int masteryRespecs;
    private String cosmeticTitle;
    private int comboHits;
    
    public PlayerData(int level, double xp) {
        this.level = level;
        this.xp = xp;
        this.xpNeeded = 0;
        this.bossBar = null;
        this.masteryPath = MasteryPath.NONE;
        this.masteryLevel = 0;
        this.masteryXp = 0;
        this.masteryObjective = "";
        this.masteryObjectiveProgress = 0;
        this.masteryObjectiveTarget = 0;
        this.masteryRespecs = 0;
        this.cosmeticTitle = "";
        this.comboHits = 0;
    }
    
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    
    public double getXp() { return xp; }
    public void setXp(double xp) { this.xp = xp; }
    
    public double getXpNeeded() { return xpNeeded; }
    public void setXpNeeded(double xpNeeded) { this.xpNeeded = xpNeeded; }
    
    public BossBar getBossBar() { return bossBar; }
    public void setBossBar(BossBar bossBar) { this.bossBar = bossBar; }

    public MasteryPath getMasteryPath() { return masteryPath; }
    public void setMasteryPath(MasteryPath masteryPath) { this.masteryPath = masteryPath; }

    public int getMasteryLevel() { return masteryLevel; }
    public void setMasteryLevel(int masteryLevel) { this.masteryLevel = masteryLevel; }

    public double getMasteryXp() { return masteryXp; }
    public void setMasteryXp(double masteryXp) { this.masteryXp = masteryXp; }

    public String getMasteryObjective() { return masteryObjective; }
    public void setMasteryObjective(String masteryObjective) { this.masteryObjective = masteryObjective; }

    public int getMasteryObjectiveProgress() { return masteryObjectiveProgress; }
    public void setMasteryObjectiveProgress(int masteryObjectiveProgress) { this.masteryObjectiveProgress = masteryObjectiveProgress; }

    public int getMasteryObjectiveTarget() { return masteryObjectiveTarget; }
    public void setMasteryObjectiveTarget(int masteryObjectiveTarget) { this.masteryObjectiveTarget = masteryObjectiveTarget; }

    public int getMasteryRespecs() { return masteryRespecs; }
    public void setMasteryRespecs(int masteryRespecs) { this.masteryRespecs = masteryRespecs; }

    public String getCosmeticTitle() { return cosmeticTitle; }
    public void setCosmeticTitle(String cosmeticTitle) { this.cosmeticTitle = cosmeticTitle; }

    public int getComboHits() { return comboHits; }
    public void setComboHits(int comboHits) { this.comboHits = comboHits; }
}
