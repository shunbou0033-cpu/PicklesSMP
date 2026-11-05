
package jp.pickles.smp.afk;

import jp.pickles.smp.PicklesSMP;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class AfkManager implements Listener {

    private final PicklesSMP plugin;

    private final Set<UUID> afkPlayers = new HashSet<>();

    private final Map<UUID, Long> lastActivity = new HashMap<>();

    private final Map<UUID, Component> originalTabNames = new HashMap<>();

    private final Map<UUID, Component> originalCustomNames = new HashMap<>();

    private final Map<UUID, Boolean> originalCustomNameVisibility = new HashMap<>();

    private int timeoutSeconds;

    private BukkitTask task;

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    public AfkManager(PicklesSMP plugin) {
        this.plugin = plugin;
        reloadSettings();
    }

    public void reloadSettings() {
        timeoutSeconds = Math.max(
                1,
                plugin.getConfig().getInt("afk.timeout-seconds", 180)
        );
    }

    public void start() {
        if (task != null) {
            task.cancel();
        }

        task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::checkPlayers,
                20L,
                20L
        );
    }

    public void shutdown() {

        if (task != null) {
            task.cancel();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            setAfk(player, false);
        }

        afkPlayers.clear();
        lastActivity.clear();
        originalTabNames.clear();
        originalCustomNames.clear();
        originalCustomNameVisibility.clear();
    }

    public boolean isAfk(Player player) {
        return afkPlayers.contains(player.getUniqueId());
    }

    public void markActivity(Player player) {

        UUID uuid = player.getUniqueId();

        lastActivity.put(uuid, System.currentTimeMillis());

        if (isAfk(player)) {
            setAfk(player, false);
        }
    }

    private void checkPlayers() {

        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {

            UUID uuid = player.getUniqueId();

            long last = lastActivity.computeIfAbsent(
                    uuid,
                    key -> now
            );

            if (!isAfk(player)
                    && now - last >= timeoutSeconds * 1000L) {

                setAfk(player, true);
            }
        }
    }

    private void setAfk(Player player, boolean value) {

        UUID uuid = player.getUniqueId();

        if (value) {

            if (!afkPlayers.add(uuid)) {
                return;
            }

            originalTabNames.put(uuid, player.playerListName());
            originalCustomNames.put(uuid, player.customName());
            originalCustomNameVisibility.put(
                    uuid,
                    player.isCustomNameVisible()
            );

            String suffix = plugin.getConfig().getString(
                    "afk.tab-suffix",
                    " §7[放置中]"
            );

            Component tabName = player.playerListName()
                    .append(LEGACY.deserialize(suffix));

            player.playerListName(tabName);

            String overhead = plugin.getConfig().getString(
                    "afk.overhead-name",
                    "§7放置中"
            );

            player.customName(LEGACY.deserialize(overhead));
            player.setCustomNameVisible(true);

            player.sendMessage("§7AFK状態になりました。");

        } else {

            if (!afkPlayers.remove(uuid)) {
                return;
            }

            Component originalTab = originalTabNames.remove(uuid);

            if (originalTab != null) {
                player.playerListName(originalTab);
            }

            Component originalCustom = originalCustomNames.remove(uuid);

            player.customName(originalCustom);

            boolean visible = originalCustomNameVisibility
                    .getOrDefault(uuid, false);

            originalCustomNameVisibility.remove(uuid);

            player.setCustomNameVisible(visible);

            lastActivity.put(uuid, System.currentTimeMillis());

            player.sendMessage("§aAFK状態が解除されました。");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        lastActivity.put(
                player.getUniqueId(),
                System.currentTimeMillis()
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        UUID uuid = event.getPlayer().getUniqueId();

        afkPlayers.remove(uuid);
        lastActivity.remove(uuid);
        originalTabNames.remove(uuid);
        originalCustomNames.remove(uuid);
        originalCustomNameVisibility.remove(uuid);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {

        if (event.getTo() == null) {
            return;
        }

        if (event.getFrom().getWorld() != event.getTo().getWorld()
                || event.getFrom().distanceSquared(event.getTo()) > 0.0001) {

            markActivity(event.getPlayer());
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {

        Bukkit.getScheduler().runTask(
                plugin,
                () -> markActivity(event.getPlayer())
        );
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        markActivity(event.getPlayer());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        markActivity(event.getPlayer());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        markActivity(event.getPlayer());
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        markActivity(event.getPlayer());
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        markActivity(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (event.getWhoClicked() instanceof Player player) {
            markActivity(player);
        }
    }

    @EventHandler
    public void onAnimation(PlayerAnimationEvent event) {
        markActivity(event.getPlayer());
    }

    // AFK中のプレイヤーはダメージを受けない
    @EventHandler
    public void onDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!plugin.getConfig().getBoolean(
                "afk.invulnerable",
                true
        )) {
            return;
        }

        if (isAfk(player)) {
            event.setCancelled(true);
        }
    }

    // AFK中のプレイヤーは攻撃できない
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {

        Player attacker = null;

        if (event.getDamager() instanceof Player player) {
            attacker = player;
        }

        if (event.getDamager() instanceof Projectile projectile) {

            ProjectileSource source = projectile.getShooter();

            if (source instanceof Player player) {
                attacker = player;
            }
        }

        if (attacker != null && isAfk(attacker)) {
            event.setCancelled(true);
        }
    }
}
