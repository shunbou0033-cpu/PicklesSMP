
package jp.pickles.smp;

import jp.pickles.smp.afk.AfkManager;
import jp.pickles.smp.combat.CombatManager;
import jp.pickles.smp.command.PicklesCommand;
import jp.pickles.smp.enchant.MaceEnchantListener;
import jp.pickles.smp.items.ItemRestrictionListener;
import jp.pickles.smp.items.RestrictedItems;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class PicklesSMP extends JavaPlugin {

    private AfkManager afkManager;
    private CombatManager combatManager;
    private RestrictedItems restrictedItems;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        // 各機能の初期化
        restrictedItems = new RestrictedItems(this);
        afkManager = new AfkManager(this);
        combatManager = new CombatManager(this, afkManager);

        // イベント登録
        getServer().getPluginManager().registerEvents(
                afkManager, this
        );

        getServer().getPluginManager().registerEvents(
                combatManager, this
        );

        getServer().getPluginManager().registerEvents(
                new ItemRestrictionListener(this, restrictedItems), this
        );

        getServer().getPluginManager().registerEvents(
                new MaceEnchantListener(this), this
        );

        // コマンド登録
        PluginCommand command = getCommand("picklessmp");

        if (command != null) {
            PicklesCommand executor = new PicklesCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().severe(
                    "picklessmp コマンドが plugin.yml に登録されていません。"
            );
        }

        // タスク開始
        afkManager.start();
        combatManager.start();

        getLogger().info("PicklesSMP が有効になりました。");
    }

    @Override
    public void onDisable() {

        if (combatManager != null) {
            combatManager.shutdown();
        }

        if (afkManager != null) {
            afkManager.shutdown();
        }

        getLogger().info("PicklesSMP が無効になりました。");
    }

    public void reloadPlugin() {

        reloadConfig();

        if (afkManager != null) {
            afkManager.reloadSettings();
        }

        if (combatManager != null) {
            combatManager.reloadSettings();
        }

        if (restrictedItems != null) {
            restrictedItems.reload();
        }
    }

    public AfkManager getAfkManager() {
        return afkManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public RestrictedItems getRestrictedItems() {
        return restrictedItems;
    }
}
