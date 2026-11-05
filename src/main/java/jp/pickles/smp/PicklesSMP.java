
package jp.pickles.smp;

import jp.pickles.smp.afk.AfkManager;
import jp.pickles.smp.combat.CombatManager;
import jp.pickles.smp.enchant.MaceEnchantListener;
import jp.pickles.smp.items.ItemRestrictionListener;
import jp.pickles.smp.items.RestrictedItems;
import jp.pickles.smp.command.PicklesCommand;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class PicklesSMP extends JavaPlugin {

    private AfkManager afkManager;
    private CombatManager combatManager;
    private RestrictedItems restrictedItems;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        getLogger().info("================================");
        getLogger().info("       PicklesSMP Enabled       ");
        getLogger().info("================================");

        restrictedItems = new RestrictedItems(this);

        afkManager = new AfkManager(this);
        combatManager = new CombatManager(this, afkManager);

        Bukkit.getPluginManager().registerEvents(afkManager, this);
        Bukkit.getPluginManager().registerEvents(combatManager, this);

        Bukkit.getPluginManager().registerEvents(
                new ItemRestrictionListener(this, restrictedItems), this
        );

        Bukkit.getPluginManager().registerEvents(
                new MaceEnchantListener(this), this
        );

        PicklesCommand picklesCommand = new PicklesCommand(
                this,
                restrictedItems,
                afkManager,
                combatManager
        );

        if (getCommand("picklessmp") != null) {
            getCommand("picklessmp").setExecutor(picklesCommand);
            getCommand("picklessmp").setTabCompleter(picklesCommand);
        }

        afkManager.start();
        combatManager.start();

        getLogger().info("AFK system loaded.");
        getLogger().info("Combat system loaded.");
        getLogger().info("Item restriction system loaded.");
        getLogger().info("Mace enchant restriction loaded.");

    }

    @Override
    public void onDisable() {

        if (afkManager != null) {
            afkManager.shutdown();
        }

        if (combatManager != null) {
            combatManager.shutdown();
        }

        getLogger().info("PicklesSMP disabled.");

    }

    public void reloadPlugin() {

        reloadConfig();

        if (restrictedItems != null) {
            restrictedItems.reload();
        }

        if (afkManager != null) {
            afkManager.reloadSettings();
        }

        if (combatManager != null) {
            combatManager.reloadSettings();
        }

        getLogger().info("PicklesSMP configuration reloaded.");

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
