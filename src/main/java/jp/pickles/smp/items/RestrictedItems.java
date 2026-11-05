
package jp.pickles.smp.items;

import jp.pickles.smp.PicklesSMP;
import org.bukkit.Material;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class RestrictedItems {

    private final PicklesSMP plugin;
    private final Map<Material, Integer> limits = new HashMap<>();

    public RestrictedItems(PicklesSMP plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        limits.clear();

        if (!plugin.getConfig().isConfigurationSection("items.limits")) {
            return;
        }

        for (String key : plugin.getConfig()
                .getConfigurationSection("items.limits").getKeys(false)) {

            Material material = Material.matchMaterial(key);
            int limit = plugin.getConfig().getInt("items.limits." + key);

            if (material == null || !material.isItem() || limit < 0) {
                plugin.getLogger().warning("無効なアイテム上限設定: " + key);
                continue;
            }

            limits.put(material, limit);
        }
    }

    public boolean setLimit(Material material, int amount) {
        if (material == null || !material.isItem() || amount < 0) {
            return false;
        }

        limits.put(material, amount);
        save();
        return true;
    }

    public boolean remove(Material material) {
        if (!limits.containsKey(material)) {
            return false;
        }

        limits.remove(material);
        save();
        return true;
    }

    public boolean isRestricted(Material material) {
        return limits.containsKey(material);
    }

    public int getLimit(Material material) {
        return limits.getOrDefault(material, -1);
    }

    public Map<Material, Integer> getAll() {
        return Collections.unmodifiableMap(limits);
    }

    private void save() {
        plugin.getConfig().set("items.limits", null);

        for (Map.Entry<Material, Integer> entry : limits.entrySet()) {
            plugin.getConfig().set(
                    "items.limits." + entry.getKey().name(),
                    entry.getValue()
            );
        }

        plugin.saveConfig();
    }
}
