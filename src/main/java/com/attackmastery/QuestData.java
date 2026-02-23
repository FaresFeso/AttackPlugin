package com.attackmastery;

public class QuestData {
    private long lastDailyReset;
    private long lastWeeklyReset;
    
    // Daily quest progress
    private int dailyProgress;
    private boolean dailyCompleted;
    
    // Weekly quest progress
    private int weeklyProgress1;
    private int weeklyProgress2;
    private int weeklyProgress3;
    private boolean weeklyCompleted1;
    private boolean weeklyCompleted2;
    private boolean weeklyCompleted3;
    
    // Special quest progress
    private int specialProgress;
    private boolean specialCompleted;
    
    public QuestData() {
        this.lastDailyReset = 0;
        this.lastWeeklyReset = 0;
        this.dailyProgress = 0;
        this.dailyCompleted = false;
        this.weeklyProgress1 = 0;
        this.weeklyProgress2 = 0;
        this.weeklyProgress3 = 0;
        this.weeklyCompleted1 = false;
        this.weeklyCompleted2 = false;
        this.weeklyCompleted3 = false;
        this.specialProgress = 0;
        this.specialCompleted = false;
    }
    
    public long getLastDailyReset() { return lastDailyReset; }
    public void setLastDailyReset(long time) { this.lastDailyReset = time; }
    
    public long getLastWeeklyReset() { return lastWeeklyReset; }
    public void setLastWeeklyReset(long time) { this.lastWeeklyReset = time; }
    
    public int getDailyProgress() { return dailyProgress; }
    public void setDailyProgress(int progress) { this.dailyProgress = progress; }
    
    public boolean isDailyCompleted() { return dailyCompleted; }
    public void setDailyCompleted(boolean completed) { this.dailyCompleted = completed; }
    
    public int getWeeklyProgress1() { return weeklyProgress1; }
    public void setWeeklyProgress1(int progress) { this.weeklyProgress1 = progress; }
    
    public int getWeeklyProgress2() { return weeklyProgress2; }
    public void setWeeklyProgress2(int progress) { this.weeklyProgress2 = progress; }
    
    public int getWeeklyProgress3() { return weeklyProgress3; }
    public void setWeeklyProgress3(int progress) { this.weeklyProgress3 = progress; }
    
    public boolean isWeeklyCompleted1() { return weeklyCompleted1; }
    public void setWeeklyCompleted1(boolean completed) { this.weeklyCompleted1 = completed; }
    
    public boolean isWeeklyCompleted2() { return weeklyCompleted2; }
    public void setWeeklyCompleted2(boolean completed) { this.weeklyCompleted2 = completed; }
    
    public boolean isWeeklyCompleted3() { return weeklyCompleted3; }
    public void setWeeklyCompleted3(boolean completed) { this.weeklyCompleted3 = completed; }
    
    public int getSpecialProgress() { return specialProgress; }
    public void setSpecialProgress(int progress) { this.specialProgress = progress; }
    
    public boolean isSpecialCompleted() { return specialCompleted; }
    public void setSpecialCompleted(boolean completed) { this.specialCompleted = completed; }
    
    public void resetDaily() {
        this.dailyProgress = 0;
        this.dailyCompleted = false;
    }
    
    public void resetWeekly() {
        this.weeklyProgress1 = 0;
        this.weeklyProgress2 = 0;
        this.weeklyProgress3 = 0;
        this.weeklyCompleted1 = false;
        this.weeklyCompleted2 = false;
        this.weeklyCompleted3 = false;
    }
}
