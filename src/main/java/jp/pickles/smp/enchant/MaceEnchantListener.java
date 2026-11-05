
package jp.pickles.smp.enchant;

import jp.pickles.smp.PicklesSMP;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class MaceEnchantListener implements Listener {

    private final PicklesSMP plugin;

    public MaceEnchantListener(PicklesSMP plugin) {
        this.plugin = plugin;
    }

    // エンチャントテーブルの候補表示を禁止
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {

        if (!plugin.getConfig().getBoolean(
                "mace.block-enchanting-table", true)) {
            return;
        }

        if (event.getItem().getType() == Material.MACE) {
            event.setCancelled(true);
        }
    }

    // エンチャントテーブルでの付与を禁止
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnchant(EnchantItemEvent event) {

        if (!plugin.getConfig().getBoolean(
                "mace.block-enchanting-table", true)) {
            return;
        }

        if (event.getItem().getType() == Material.MACE) {
            event.setCancelled(true);
        }
    }

    // 金床で新しいエンチャントが付く場合は禁止
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {

        if (!plugin.getConfig().getBoolean(
                "mace.block-anvil-enchanting", true)) {
            return;
        }

        ItemStack firstItem =
                event.getInventory().getFirstItem();

        ItemStack result = event.getResult();

        if (firstItem == null
                || firstItem.getType() != Material.MACE
                || result == null
                || result.getType() != Material.MACE) {
            return;
        }

        Map<Enchantment, Integer> original =
                firstItem.getEnchantments();

        Map<Enchantment, Integer> output =
                result.getEnchantments();

        for (Map.Entry<Enchantment, Integer> entry
                : output.entrySet()) {

            int oldLevel = original.getOrDefault(
                    entry.getKey(), 0
            );

            if (entry.getValue() > oldLevel) {
                event.setResult(null);
                return;
            }
        }
    }
}
