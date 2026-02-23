package com.attackmastery;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("deprecation")
public class QuestListener implements Listener {
    private final AttackMastery plugin;
    
    public QuestListener(AttackMastery plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (title.contains("Attack Mastery Quests")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            
            String name = clicked.getItemMeta().getDisplayName();
            if (name.contains("Daily Quests")) {
                plugin.getQuestManager().openDailyQuests(player);
            } else if (name.contains("Weekly Quests")) {
                plugin.getQuestManager().openWeeklyQuests(player);
            } else if (name.contains("Special Quests")) {
                plugin.getQuestManager().openSpecialQuests(player);
            }
        } else if (title.contains("Daily Quests") || title.contains("Weekly Quests") || title.contains("Special Quests")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            
            String name = clicked.getItemMeta().getDisplayName();
            if (name.contains("Back")) {
                plugin.getQuestManager().openQuestMenu(player);
            }
        }
    }
}
