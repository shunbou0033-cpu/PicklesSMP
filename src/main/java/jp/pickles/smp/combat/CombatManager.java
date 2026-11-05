
package jp.pickles.smp.combat;

import jp.pickles.smp.PicklesSMP;
import jp.pickles.smp.afk.AfkManager;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class CombatManager implements Listener {

    private final PicklesSMP plugin;
    private final AfkManager afkManager;

    private final Map<UUID, Long> combatUntil = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    private BukkitTask task;

    private int duration;

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    public CombatManager(PicklesSMP plugin, AfkManager afkManager) {
        this.plugin = plugin;
        this.afkManager = afkManager;
        reloadSettings();
    }

    public void reloadSettings() {
        duration = Math.max(
                1,
                plugin.getConfig().getInt("combat.duration-seconds", 30)
        );
    }

    public void start() {

        if (task != null) {
            task.cancel();
        }

        task = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::updateCombat,
                1L,
                5L
        );
    }

    public void shutdown() {

        if (task != null) {
            task.cancel();
        }

        for (UUID uuid : new HashSet<>(bossBars.keySet())) {
            removeBossBar(uuid);
        }

        combatUntil.clear();
    }

    public boolean isInCombat(Player player) {

        return combatUntil.getOrDefault(
                player.getUniqueId(),
                0L
        ) > System.currentTimeMillis();
    }

    private Player getAttacker(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Player player) {
            return player;
        }

        if (event.getDamager() instanceof Projectile projectile) {

            ProjectileSource source = projectile.getShooter();

            if (source instanceof Player player) {
                return player;
            }
        }

        return null;
    }

    private void startCombat(Player player) {

        UUID uuid = player.getUniqueId();

        long until = System.currentTimeMillis() + duration * 1000L;

        combatUntil.put(uuid, until);

        BossBar bar = bossBars.get(uuid);

        if (bar == null) {

            bar = BossBar.bossBar(
                    Component.text("戦闘中"),
                    1.0f,
                    BossBar.Color.RED,
                    BossBar.Overlay.PROGRESS
            );

            bossBars.put(uuid, bar);

            player.showBossBar(bar);
        }

        updateBossBar(player, until);
    }

    private void updateBossBar(Player player, long until) {

        BossBar bar = bossBars.get(player.getUniqueId());

        if (bar == null) {
            return;
        }

        long remaining = Math.max(
                0,
                (until - System.currentTimeMillis() + 999) / 1000
        );

        String title = plugin.getConfig().getString(
                "combat.bossbar-title",
                "§c戦闘中 §f残り §e{time}秒"
        );

        title = title.replace(
                "{time}",
                String.valueOf(remaining)
        );

        bar.name(LEGACY.deserialize(title));

        float progress = Math.max(
                0.0f,
                Math.min(
                        1.0f,
                        (float) (until - System.currentTimeMillis())
                                / (duration * 1000.0f)
                )
        );

        bar.progress(progress);
    }

    private void removeBossBar(UUID uuid) {

        BossBar bar = bossBars.remove(uuid);

        Player player = Bukkit.getPlayer(uuid);

        if (bar != null && player != null) {
            player.hideBossBar(bar);
        }
    }

    private void updateCombat() {

        long now = System.currentTimeMillis();

        for (UUID uuid : new HashSet<>(combatUntil.keySet())) {

            long until = combatUntil.getOrDefault(uuid, 0L);

            Player player = Bukkit.getPlayer(uuid);

            if (player == null || until <= now) {

                combatUntil.remove(uuid);

                removeBossBar(uuid);

                if (player != null) {
                    player.sendMessage(
                            "§aCombat状態が解除されました。"
                    );
                }

                continue;
            }

            updateBossBar(player, until);
        }
    }

    // プレイヤーへの攻撃が成功した場合、双方のCombat時間を更新
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {

        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = getAttacker(event);

        if (attacker == null || attacker.equals(victim)) {
            return;
        }

        if (afkManager.isAfk(attacker)
                || afkManager.isAfk(victim)) {
            return;
        }

        // ダメージが実際に発生した場合のみ更新
        if (event.getFinalDamage() <= 0) {
            return;
        }

        startCombat(attacker);
        startCombat(victim);
    }

    // Combat中に指定されたコマンドを禁止
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {

        Player player = event.getPlayer();

        if (!isInCombat(player)) {
            return;
        }

        String message = event.getMessage();

        if (!message.startsWith("/")) {
            return;
        }

        String commandName = message.substring(1)
                .split("\\s+")[0]
                .toLowerCase(Locale.ROOT);

        if (commandName.contains(":")) {
            commandName = commandName.substring(
                    commandName.lastIndexOf(':') + 1
            );
        }

        final String root = commandName;

        List<String> blockedCommands =
                plugin.getConfig().getStringList(
                        "combat.blocked-commands"
                );

        boolean blocked = blockedCommands.stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(root::equals);

        if (blocked) {

            event.setCancelled(true);

            player.sendMessage(
                    "§cCombat中はこのコマンドを使用できません。"
            );
        }
    }

    // Combat中にログアウトすると死亡扱い
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        UUID uuid = player.getUniqueId();

        if (isInCombat(player)
                && plugin.getConfig().getBoolean(
                        "combat.kill-on-logout",
                        true
                )) {

            player.setHealth(0.0);

            player.sendMessage(
                    "§cCombat中にログアウトしたため死亡扱いになりました。"
            );
        }

        combatUntil.remove(uuid);

        removeBossBar(uuid);
    }
}
