ionpackage com.attackmastery;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
import java.util.Map;
import java.util.UUID;

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
        
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        if (data.getLevel() > 0) {
            double damageMultiplier = 1.0 + (data.getLevel() * plugin.getConfig().getDouble("damage-per-level", 0.05));
            event.setDamage(event.getDamage() * damageMultiplier);
        }
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
            for (Map.Entry<UUID, Double> entry : damageMap.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    giveXp(player, baseXp);
                    plugin.getQuestManager().trackMobKill(player, entity);
                }
            }
        } else {
            Player killer = entity.getKiller();
            if (killer != null) {
                giveXp(killer, baseXp);
                plugin.getQuestManager().trackMobKill(killer, entity);
            }
        }
    }
    
    private void giveXp(Player player, double xpGain) {
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
            player.sendMessage(String.format(msg, data.getLevel(), damagePercent, hearts).replace('&', '§'));
        }
        
        data.setXpNeeded(base + (data.getLevel() * increment));
        
        if (levelsGained > 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        } else {
            String msg = plugin.getConfig().getString("messages.xp-gain", "&a+%.1f XP (%d/%d)");
            player.sendMessage(String.format(msg, xpGain, data.getLevel(), maxLevel).replace('&', '§'));
        }
        
        updateBossBar(player, data);
        plugin.markDirty(player.getUniqueId());
    }
    
    private int getMobLevel(LivingEntity entity) {
        int baseLevel = (int) Math.max(1, entity.getMaxHealth() / 4);
        
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
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        
        data.setXpNeeded(calculateXpNeeded(data.getLevel()));
        applyDamageModifier(player, data.getLevel());
        applyHealthBonus(player, data.getLevel());
        
        BossBar bossBar = Bukkit.createBossBar("", BarColor.PINK, BarStyle.SEGMENTED_10);
        bossBar.addPlayer(player);
        data.setBossBar(bossBar);
        
        updateBossBar(player, data);
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
            
            UUID modId = UUID.nameUUIDFromBytes("AttackMastery.Health".getBytes());
            
            attr.getModifiers().stream()
                .filter(m -> m.getKey().toString().equals(modId.toString()))
                .findFirst()
                .ifPresent(m -> attr.removeModifier(m.getKey()));
            
            if (level > 0) {
                int hearts = Math.min(level / 10, 10); // Cap at 10 bonus hearts (20 total)
                plugin.getLogger().info("[DEBUG] Applying health bonus - Level: " + level + ", Hearts: " + hearts);
                if (hearts > 0) {
                    double healthBonus = hearts * plugin.getConfig().getDouble("health-per-10-levels", 2.0);
                    plugin.getLogger().info("[DEBUG] Health bonus amount: " + healthBonus);

                   
                    AttributeModifier mod = new AttributeModifier(modId, "AttackMastery.Health", healthBonus, 
                        AttributeModifier.Operation.ADD_NUMBER, org.bukkit.inventory.EquipmentSlotGroup.ANY);
                    attr.addModifier(mod);
                    plugin.getLogger().info("[DEBUG] Max health after: " + attr.getValue());
                    player.setHealth(Math.min(player.getHealth(), attr.getValue()));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[DEBUG] Error applying health bonus: " + e.getMessage());
        }
    }
    
    private void applyDamageModifier(Player player, int level) {
        try {
            AttributeInstance attr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (attr == null) return;
            
            UUID modId = UUID.nameUUIDFromBytes("AttackMastery.Bonus".getBytes());
            
            // Remove existing modifier
            attr.getModifiers().stream()
                .filter(m -> m.getKey().toString().equals(modId.toString()))
                .findFirst()
                .ifPresent(m -> attr.removeModifier(m.getKey()));
            
            if (level > 0) {
                double bonus = level * plugin.getConfig().getDouble("damage-per-level", 0.05);
                AttributeModifier mod = new AttributeModifier(modId, "AttackMastery.Bonus", bonus, 
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
}
