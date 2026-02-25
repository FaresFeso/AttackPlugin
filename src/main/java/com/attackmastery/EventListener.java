package com.attackmastery;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import java.util.IllegalFormatException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class EventListener implements Listener {
    private final AttackMastery plugin;
    
    public EventListener(AttackMastery plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        
        Player player = null;
        if (event.getDamager() instanceof Player) {
            player = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                player = (Player) proj.getShooter();
            }
        }
        
        if (player == null) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        LivingEntity entity = (LivingEntity) event.getEntity();
        double damage = event.getFinalDamage();
        
        plugin.trackDamage(entity.getUniqueId(), player.getUniqueId(), damage);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        if (data.getLevel() > 100) {
            int levelsAbove100 = data.getLevel() - 100;
            double reduction = levelsAbove100 * plugin.getConfig().getDouble("damage-reduction-per-level-after-100", 0.005);
            reduction = Math.min(reduction, 0.5); // Cap at 50% reduction
            event.setDamage(event.getDamage() * (1.0 - reduction));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageBonus(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        Player player = resolveAttacker(event);
        
        if (player == null) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        if (data.getLevel() > 0) {
            double damageMultiplier = 1.0 + (data.getLevel() * plugin.getConfig().getDouble("damage-per-level", 0.05));
            event.setDamage(event.getDamage() * damageMultiplier);
        }

        applyMasteryPerks(event, player, (LivingEntity) event.getEntity(), data);
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        
        if (!(entity instanceof Monster || entity instanceof Slime || entity instanceof Phantom || entity instanceof EnderDragon || entity instanceof Wither)) return;
        
        int mobLevel = getMobLevel(entity);
        if (mobLevel <= 0) return;
        
        double baseXp = mobLevel * plugin.getConfig().getDouble("xp-multiplier", 1.5);
        
        Map<UUID, Double> damageMap = plugin.getDamageTracking(entity.getUniqueId());
        
        if (damageMap != null && !damageMap.isEmpty()) {
            double totalDamage = damageMap.values().stream().mapToDouble(Double::doubleValue).sum();
            if (totalDamage <= 0.0) {
                totalDamage = 1.0;
            }
            for (Map.Entry<UUID, Double> entry : damageMap.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    double contribution = Math.max(0.0, entry.getValue()) / totalDamage;
                    double xpShare = baseXp * contribution;
                    if (xpShare <= 0.0) {
                        continue;
                    }
                    giveXp(player, xpShare);
                    processMasteryKill(player, entity, xpShare);
                    plugin.getQuestManager().trackMobKill(player, entity);
                    plugin.getQuestManager().trackPathQuestKill(player, entity);
                }
            }
        } else {
            Player killer = entity.getKiller();
            if (killer != null) {
                giveXp(killer, baseXp);
                processMasteryKill(killer, entity, baseXp);
                plugin.getQuestManager().trackMobKill(killer, entity);
                plugin.getQuestManager().trackPathQuestKill(killer, entity);
            }
        }
    }

    public void grantXp(Player player, double xpGain, boolean showXpGainMessage) {
        giveXp(player, xpGain, showXpGainMessage);
    }

    public void grantMasteryXp(Player player, double masteryXpGain, boolean announce) {
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        if (data.getMasteryPath() == MasteryPath.NONE || masteryXpGain <= 0.0) {
            return;
        }

        int maxMasteryLevel = plugin.getConfig().getInt("mastery.max-level", 30);
        data.setMasteryXp(data.getMasteryXp() + masteryXpGain);
        boolean leveled = false;

        while (data.getMasteryLevel() < maxMasteryLevel) {
            double needed = masteryXpNeeded(data.getMasteryLevel());
            if (data.getMasteryXp() < needed) {
                break;
            }
            data.setMasteryXp(data.getMasteryXp() - needed);
            data.setMasteryLevel(data.getMasteryLevel() + 1);
            leveled = true;

            String title = titleForLevel(data.getMasteryPath(), data.getMasteryLevel());
            if (!title.isEmpty() && !title.equals(data.getCosmeticTitle())) {
                data.setCosmeticTitle(title);
                player.sendMessage("§6New Title Unlocked: §e" + title);
            }

            player.sendMessage("§b§lMastery Level Up! §f" + data.getMasteryPath().displayName() + " §7→ §b" + data.getMasteryLevel());
        }

        plugin.markDirty(player.getUniqueId());
        updateBossBar(player, data);

        if (announce) {
            player.sendMessage("§b+" + (int) masteryXpGain + " Mastery XP");
        }
        if (leveled) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        }

        refreshLeaderboards();
    }
    
    private void giveXp(Player player, double xpGain) {
        giveXp(player, xpGain, true);
    }

    private void giveXp(Player player, double xpGain, boolean showXpGainMessage) {
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        data.setXp(data.getXp() + xpGain);
        
        int maxLevel = plugin.getConfig().getInt("max-level", 200);
        int base = plugin.getConfig().getInt("xp-base", 200);
        int increment = plugin.getConfig().getInt("xp-increment", 30);
        int levelsGained = 0;
        
        while (data.getLevel() < maxLevel) {
            double needed = base + (data.getLevel() * increment);
            if (data.getXp() < needed) break;
            
            data.setXp(data.getXp() - needed);
            data.setLevel(data.getLevel() + 1);
            levelsGained++;
            
            applyDamageModifier(player, data.getLevel());
            applyHealthBonus(player, data.getLevel());

            double damagePercent = data.getLevel() * plugin.getConfig().getDouble("damage-per-level", 0.05) * 100;
            int hearts = Math.min(data.getLevel() / 10, 10);
            String msg = plugin.getConfig().getString("messages.level-up", "&6Attack Mastery Level Up! &eLevel %d &7(+%.0f%% Damage, +%d Hearts)");
            player.sendMessage(formatLevelUpMessage(msg, data.getLevel(), damagePercent, hearts).replace('&', '§'));
        }
        
        data.setXpNeeded(base + (data.getLevel() * increment));
        
        if (levelsGained > 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        } else if (showXpGainMessage) {
            String msg = plugin.getConfig().getString("messages.xp-gain", "&a+%.1f XP (%d/%d)");
            player.sendMessage(formatXpGainMessage(msg, xpGain, data.getLevel(), maxLevel).replace('&', '§'));
        }
        
        updateBossBar(player, data);
        plugin.markDirty(player.getUniqueId());
        refreshLeaderboards();
    }
    
    private int getMobLevel(LivingEntity entity) {
        AttributeInstance maxHealthAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : Math.max(entity.getHealth(), 1.0);
        int baseLevel = (int) Math.max(1, maxHealth / 4);
        
        // Rarity multipliers
        if (entity instanceof org.bukkit.entity.EnderDragon) return (int) (20000 / plugin.getConfig().getDouble("xp-multiplier", 1.5)); // Divine - 20k XP
        if (entity instanceof org.bukkit.entity.Wither) return (int) (10000 / plugin.getConfig().getDouble("xp-multiplier", 1.5)); // Mythic - 10k XP
        if (entity instanceof org.bukkit.entity.Warden) return baseLevel * 10; // Legendary
        if (entity instanceof org.bukkit.entity.ElderGuardian) return baseLevel * 8; // Legendary
        if (entity instanceof org.bukkit.entity.Evoker) return baseLevel * 6; // Epic
        if (entity instanceof org.bukkit.entity.Ravager) return baseLevel * 6; // Epic
        if (entity instanceof org.bukkit.entity.Piglin || entity instanceof org.bukkit.entity.PiglinBrute) return baseLevel * 5; // Epic
        if (entity instanceof org.bukkit.entity.Enderman) return baseLevel * 5; // Epic
        if (entity instanceof org.bukkit.entity.Blaze) return baseLevel * 4; // Rare
        if (entity instanceof org.bukkit.entity.Witch) return baseLevel * 4; // Rare
        if (entity instanceof org.bukkit.entity.Guardian) return baseLevel * 3; // Rare
        if (entity instanceof org.bukkit.entity.Vindicator) return baseLevel * 3; // Rare
        if (entity instanceof org.bukkit.entity.Pillager) return baseLevel * 2; // Uncommon
        if (entity instanceof org.bukkit.entity.Creeper) return baseLevel * 2; // Uncommon
        if (entity instanceof org.bukkit.entity.Spider) return baseLevel * 2; // Uncommon
        
        return baseLevel; // Common (Zombie, Skeleton, etc.)
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        refreshPlayerStats(event.getPlayer());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        
        if (data.getBossBar() != null) {
            data.getBossBar().removeAll();
            data.setBossBar(null);
        }
        
        plugin.savePlayerData(player.getUniqueId());
        plugin.unloadPlayerData(player.getUniqueId());
    }

    public void refreshPlayerStats(Player player) {
        PlayerData data = plugin.getPlayerData(player.getUniqueId());

        data.setXpNeeded(calculateXpNeeded(data.getLevel()));
        applyDamageModifier(player, data.getLevel());
        applyHealthBonus(player, data.getLevel());

        if (data.getBossBar() == null) {
            BossBar bossBar = Bukkit.createBossBar("", BarColor.PINK, BarStyle.SEGMENTED_10);
            bossBar.addPlayer(player);
            data.setBossBar(bossBar);
        }

        updateBossBar(player, data);
        updateSidebar(player, data);
    }

    public String getEvolutionLabel(PlayerData data) {
        return PathEvolution.tierName(PathEvolution.tierForAttackLevel(data.getLevel()));
    }
    
    private double calculateXpNeeded(int level) {
        int base = plugin.getConfig().getInt("xp-base", 200);
        int increment = plugin.getConfig().getInt("xp-increment", 30);
        return base + (level * increment);
    }
    
    private void applyHealthBonus(Player player, int level) {
        try {
            AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attr == null) return;
            
            NamespacedKey modKey = new NamespacedKey(plugin, "attackmastery_health");
            
            for (AttributeModifier modifier : new ArrayList<>(attr.getModifiers())) {
                if (modifier.getKey().equals(modKey)) {
                    attr.removeModifier(modifier.getKey());
                }
            }
            
            if (level > 0) {
                int hearts = Math.min(level / 10, 10);
                if (hearts > 0) {
                    double healthBonus = hearts * plugin.getConfig().getDouble("health-per-10-levels", 2.0);
                    AttributeModifier mod = new AttributeModifier(modKey, healthBonus, 
                        AttributeModifier.Operation.ADD_NUMBER, org.bukkit.inventory.EquipmentSlotGroup.ANY);
                    attr.addModifier(mod);
                    player.setHealth(Math.min(player.getHealth(), attr.getValue()));
                }
            }
        } catch (Exception e) {
            // Silently ignore attribute errors
        }
    }
    
    private void applyDamageModifier(Player player, int level) {
        try {
            AttributeInstance attr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (attr == null) return;
            
            NamespacedKey modKey = new NamespacedKey(plugin, "attackmastery_bonus");
            
            // Remove existing modifier
            attr.getModifiers().stream()
                .filter(m -> m.getKey().equals(modKey))
                .findFirst()
                .ifPresent(m -> attr.removeModifier(m.getKey()));
            
            if (level > 0) {
                double bonus = level * plugin.getConfig().getDouble("damage-per-level", 0.05);
                AttributeModifier mod = new AttributeModifier(modKey, bonus, 
                    AttributeModifier.Operation.ADD_SCALAR, org.bukkit.inventory.EquipmentSlotGroup.MAINHAND);
                attr.addModifier(mod);
            }
        } catch (Exception e) {
            // Silently ignore attribute errors
        }
    }
    
    private void updateBossBar(Player player, PlayerData data) {
        BossBar bar = data.getBossBar();
        if (bar == null) return;
        
        int maxLevel = plugin.getConfig().getInt("max-level", 200);
        bar.setTitle(String.format("§cAttack Mastery §f%d/%d §7[XP: %.0f/%.0f] §c❤%d", 
            data.getLevel(), maxLevel, data.getXp(), data.getXpNeeded(), Math.min(data.getLevel() / 10, 10)));
        
        double progress = data.getXpNeeded() > 0 ? Math.min(data.getXp() / data.getXpNeeded(), 1.0) : 1.0;
        bar.setProgress(progress);
    }

    private String formatLevelUpMessage(String template, int level, double damagePercent, int hearts) {
        try {
            return String.format(template, level, damagePercent, hearts);
        } catch (IllegalFormatException e) {
            return String.format("&6Attack Mastery Level Up! &eLevel %d &7(+%.0f%% Damage, +%d Hearts)", level, damagePercent, hearts);
        }
    }

    private String formatXpGainMessage(String template, double xpGain, int level, int maxLevel) {
        try {
            return String.format(template, xpGain, level, maxLevel);
        } catch (IllegalFormatException e) {
            return String.format("&a+%.1f XP (%d/%d)", xpGain, level, maxLevel);
        }
    }

    private void applyMasteryPerks(EntityDamageByEntityEvent event, Player player, LivingEntity target, PlayerData data) {
        if (data.getMasteryPath() == MasteryPath.NONE) {
            data.setComboHits(0);
            data.setPrecisionStreak(0);
            data.setCritMomentum(0);
            return;
        }

        Material mainHand = player.getInventory().getItemInMainHand().getType();
        boolean projectileAttack = event.getDamager() instanceof Projectile;
        int tier = PathEvolution.tierForAttackLevel(data.getLevel());

        switch (data.getMasteryPath()) {
            case SWORD -> {
                if (!projectileAttack && isSword(mainHand)) {
                    int newCombo = Math.min(data.getComboHits() + 1, 6);
                    data.setComboHits(newCombo);
                    double chainBonus = newCombo * plugin.getConfig().getDouble("mastery.sword.chain-bonus-per-hit", 0.04);
                    chainBonus += tier * 0.01;
                    event.setDamage(event.getDamage() * (1.0 + chainBonus));

                    if (tier >= 2 && newCombo >= 4) {
                        double splash = event.getDamage() * 0.20;
                        for (LivingEntity nearby : target.getLocation().getNearbyLivingEntities(2.5)) {
                            if (nearby.equals(target) || nearby.equals(player)) continue;
                            nearby.damage(splash, player);
                        }
                    }

                    if (tier >= 3 && newCombo >= 6) {
                        event.setDamage(event.getDamage() * 1.25);
                        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 18, 0.4, 0.3, 0.4, 0.05);
                        data.setComboHits(0);
                    }
                } else {
                    data.setComboHits(0);
                }
            }
            case AXE -> {
                if (!projectileAttack && isAxe(mainHand)) {
                    double chance = plugin.getConfig().getDouble("mastery.axe.bleed-chance", 0.25);
                    if (ThreadLocalRandom.current().nextDouble() <= chance) {
                        double bleedBonus = event.getDamage() * plugin.getConfig().getDouble("mastery.axe.bleed-bonus-damage-multiplier", 0.20);
                        bleedBonus *= (1.0 + (tier * 0.12));
                        event.setDamage(event.getDamage() + bleedBonus);
                        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.02);

                        if (tier >= 2) {
                            for (LivingEntity nearby : target.getLocation().getNearbyLivingEntities(2.0)) {
                                if (nearby.equals(target) || nearby.equals(player)) continue;
                                nearby.damage(bleedBonus * 0.35, player);
                            }
                        }

                        if (tier >= 3 && isElite(target)) {
                            event.setDamage(event.getDamage() * 1.20);
                        }
                    }
                }
            }
            case BOW -> {
                if (projectileAttack && isBow(mainHand)) {
                    double distance = player.getLocation().distance(target.getLocation());
                    double precisionDistance = plugin.getConfig().getDouble("mastery.bow.precision-distance", 18.0);
                    if (distance >= precisionDistance) {
                        double precisionBonus = plugin.getConfig().getDouble("mastery.bow.precision-bonus", 0.25);
                        int streak = Math.min(5, data.getPrecisionStreak() + 1);
                        data.setPrecisionStreak(streak);
                        event.setDamage(event.getDamage() * (1.0 + precisionBonus + (tier * 0.04)));

                        if (tier >= 2 && streak >= 2) {
                            event.setDamage(event.getDamage() * (1.0 + (streak * 0.06)));
                        }

                        if (tier >= 3 && streak >= 3) {
                            event.setDamage(event.getDamage() * 1.30);
                            target.getWorld().spawnParticle(Particle.FIREWORK, target.getLocation().add(0, 1, 0), 16, 0.4, 0.3, 0.4, 0.02);
                            data.setPrecisionStreak(0);
                        }
                    } else {
                        data.setPrecisionStreak(0);
                    }
                } else {
                    data.setPrecisionStreak(0);
                }
            }
            case CRIT -> {
                double executeThreshold = plugin.getConfig().getDouble("mastery.crit.execute-threshold", 0.30)
                    + (data.getCritMomentum() * 0.02);
                executeThreshold = Math.min(executeThreshold, 0.55);

                boolean executeWindow = healthPercent(target) <= executeThreshold;
                if (executeWindow) {
                    double executeBonus = plugin.getConfig().getDouble("mastery.crit.execute-bonus", 0.35);
                    executeBonus += tier * 0.08;
                    event.setDamage(event.getDamage() * (1.0 + executeBonus));
                    data.setCritMomentum(Math.min(5, data.getCritMomentum() + 1));
                }

                double critChance = plugin.getConfig().getDouble("mastery.crit.random-crit-chance", 0.20)
                    + (data.getCritMomentum() * 0.03);
                if (ThreadLocalRandom.current().nextDouble() <= critChance) {
                    double critMult = plugin.getConfig().getDouble("mastery.crit.random-crit-multiplier", 1.60);
                    if (tier >= 2) critMult += 0.15;
                    event.setDamage(event.getDamage() * critMult);
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.05);
                    if (!executeWindow) {
                        data.setCritMomentum(Math.min(5, data.getCritMomentum() + 1));
                    }
                } else if (!executeWindow) {
                    data.setCritMomentum(Math.max(0, data.getCritMomentum() - 1));
                }

                if (tier >= 3 && data.getCritMomentum() >= 4) {
                    event.setDamage(event.getDamage() * 1.20);
                }
            }
            case NONE -> {
            }
        }

        plugin.markDirty(player.getUniqueId());
    }

    private void processMasteryKill(Player player, LivingEntity entity, double xpShare) {
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        if (data.getMasteryPath() == MasteryPath.NONE) {
            return;
        }

        if (data.getMasteryObjectiveTarget() <= 0 || data.getMasteryObjective().isBlank()) {
            plugin.getQuestManager().initializeMasteryObjective(data);
        }

        if (!isObjectiveMatch(data.getMasteryPath(), entity)) {
            return;
        }

        data.setMasteryObjectiveProgress(data.getMasteryObjectiveProgress() + 1);
        double masteryKillXp = Math.max(8.0, xpShare * plugin.getConfig().getDouble("mastery.xp-from-kill-multiplier", 0.12));
        grantMasteryXp(player, masteryKillXp, false);

        if (data.getMasteryObjectiveProgress() >= data.getMasteryObjectiveTarget()) {
            player.sendMessage("§6Objective Complete: §e" + data.getMasteryObjective());
            grantMasteryXp(player, plugin.getConfig().getDouble("mastery.objective-complete-xp", 120.0), true);
            data.setMasteryObjectiveProgress(0);
            data.setMasteryObjectiveTarget(data.getMasteryObjectiveTarget() + 3);
        }

        plugin.markDirty(player.getUniqueId());
    }

    private double masteryXpNeeded(int masteryLevel) {
        return plugin.getConfig().getDouble("mastery.xp-base", 220.0)
            + (masteryLevel * plugin.getConfig().getDouble("mastery.xp-increment", 130.0));
    }

    private String titleForLevel(MasteryPath path, int masteryLevel) {
        if (masteryLevel >= 25) {
            return switch (path) {
                case SWORD -> "Blade Sovereign";
                case AXE -> "Bloodbreaker";
                case BOW -> "Storm Ranger";
                case CRIT -> "Void Finisher";
                case NONE -> "";
            };
        }
        if (masteryLevel >= 15) {
            return switch (path) {
                case SWORD -> "Duelist";
                case AXE -> "Executioner";
                case BOW -> "Sharpshooter";
                case CRIT -> "Night Hunter";
                case NONE -> "";
            };
        }
        if (masteryLevel >= 5) {
            return switch (path) {
                case SWORD -> "Sword Adept";
                case AXE -> "Axe Adept";
                case BOW -> "Archer Adept";
                case CRIT -> "Crit Adept";
                case NONE -> "";
            };
        }
        return "";
    }

    private boolean isObjectiveMatch(MasteryPath path, LivingEntity entity) {
        return switch (path) {
            case SWORD -> entity instanceof Zombie || entity instanceof Skeleton || entity instanceof Spider;
            case AXE -> entity instanceof Pillager || entity instanceof Vindicator || entity instanceof Evoker || entity instanceof Ravager;
            case BOW -> entity instanceof Skeleton || entity instanceof Phantom || entity instanceof Ghast;
            case CRIT -> entity instanceof Enderman || entity instanceof Blaze || entity instanceof Witch || entity instanceof Warden || entity instanceof Wither;
            case NONE -> false;
        };
    }

    private boolean isSword(Material material) {
        return material == Material.WOODEN_SWORD
            || material == Material.STONE_SWORD
            || material == Material.IRON_SWORD
            || material == Material.GOLDEN_SWORD
            || material == Material.DIAMOND_SWORD
            || material == Material.NETHERITE_SWORD;
    }

    private boolean isAxe(Material material) {
        return material == Material.WOODEN_AXE
            || material == Material.STONE_AXE
            || material == Material.IRON_AXE
            || material == Material.GOLDEN_AXE
            || material == Material.DIAMOND_AXE
            || material == Material.NETHERITE_AXE;
    }

    private boolean isBow(Material material) {
        return material == Material.BOW || material == Material.CROSSBOW;
    }

    private boolean isElite(LivingEntity entity) {
        return entity instanceof Warden
            || entity instanceof Wither
            || entity instanceof EnderDragon
            || entity instanceof ElderGuardian
            || entity instanceof Ravager
            || entity instanceof Evoker;
    }

    private double healthPercent(LivingEntity entity) {
        AttributeInstance maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double max = maxHealth != null ? maxHealth.getValue() : Math.max(entity.getHealth(), 1.0);
        if (max <= 0.0) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, entity.getHealth() / max));
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            return attacker;
        }
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player attacker) {
            return attacker;
        }
        return null;
    }

    private void refreshLeaderboards() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getPlayerData(online.getUniqueId());
            updateSidebar(online, data);
        }
    }

    private void updateSidebar(Player player, PlayerData data) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }

        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("attackboard", Criteria.DUMMY, Component.text("Attack Mastery"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int line = 15;
        objective.getScore("§7").setScore(line--);
        objective.getScore("§fPlayer: §e" + truncate(player.getName(), 10)).setScore(line--);
        objective.getScore("§fLevel: §a" + data.getLevel()).setScore(line--);
        objective.getScore("§fXP: §b" + (int) data.getXp() + "/" + (int) data.getXpNeeded()).setScore(line--);
        objective.getScore("§fPath: §d" + truncate(pathShort(data.getMasteryPath()), 10)).setScore(line--);
        objective.getScore("§fMastery: §b" + data.getMasteryLevel()).setScore(line--);
        objective.getScore("§8 ").setScore(line--);
        objective.getScore("§6§lTop Attack").setScore(line--);

        List<AttackMastery.LeaderboardEntry> top = plugin.getTopPlayers(3);
        if (top.isEmpty()) {
            objective.getScore("§7No data").setScore(line--);
        } else {
            int rank = 1;
            for (AttackMastery.LeaderboardEntry entry : top) {
                String prefix = rank == 1 ? "§e" : rank == 2 ? "§7" : "§6";
                String label = prefix + rank + ". §f" + truncate(entry.getName(), 8) + " §a" + entry.getLevel();
                objective.getScore(label).setScore(line--);
                rank++;
            }
        }

        player.setScoreboard(scoreboard);
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return "Unknown";
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    private String pathShort(MasteryPath path) {
        return switch (path) {
            case SWORD -> "Sword";
            case AXE -> "Axe";
            case BOW -> "Bow";
            case CRIT -> "Crit";
            case NONE -> "None";
        };
    }
}
