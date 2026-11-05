
package jp.pickles.smp.items;

import jp.pickles.smp.PicklesSMP;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;

import java.util.HashSet;
import java.util.Set;

public final class ItemRestrictionListener implements Listener {

    private final PicklesSMP plugin;
    private final RestrictedItems restricted;

    public ItemRestrictionListener(
            PicklesSMP plugin,
            RestrictedItems restricted
    ) {
        this.plugin = plugin;
        this.restricted = restricted;
    }

    private boolean isRestricted(ItemStack item) {

        if (item == null || item.getType().isAir()) {
            return false;
        }

        if (restricted.isRestricted(item.getType())) {
            return true;
        }

        if (item.getType() == Material.BUNDLE
                && item.getItemMeta() instanceof BundleMeta meta) {

            return meta.getItems().stream()
                    .anyMatch(this::isRestricted);
        }

        return false;
    }

    private void deny(Player player) {

        player.sendMessage(
                "§cそのアイテムは所持できません。"
        );
    }

    // 拾得禁止
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickup(EntityPickupItemEvent event) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (isRestricted(event.getItem().getItemStack())) {

            event.setCancelled(true);
            deny(player);
        }
    }

    // インベントリ操作制限
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (isRestricted(current) || isRestricted(cursor)) {

            event.setCancelled(true);
            deny(player);
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY) {

            int slot = event.getHotbarButton();

            if (slot >= 0
                    && isRestricted(
                    player.getInventory().getItem(slot))) {

                event.setCancelled(true);
                deny(player);
            }
        }

        if (event.getClick() == ClickType.SWAP_OFFHAND
                && isRestricted(
                player.getInventory().getItemInOffHand())) {

            event.setCancelled(true);
            deny(player);
        }
    }

    // ドラッグ操作制限
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (isRestricted(event.getOldCursor())) {

            event.setCancelled(true);
            deny(player);
        }
    }

    // ドロップ禁止
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {

        if (isRestricted(
                event.getItemDrop().getItemStack())) {

            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    // 手持ち交換禁止
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwap(PlayerSwapHandItemsEvent event) {

        if (isRestricted(event.getMainHandItem())
                || isRestricted(event.getOffHandItem())) {

            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    // クラフト禁止
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraft(CraftItemEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (isRestricted(event.getRecipe().getResult())) {

            event.setCancelled(true);
            deny(player);
        }
    }

    // バンドルへの収納・取り出しを含む操作を制限
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBundleClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (isRestricted(event.getCursor())
                || isRestricted(event.getCurrentItem())) {

            event.setCancelled(true);
            deny(player);
        }
    }

    // ログイン時に所持している制限アイテムを除去
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        boolean removed = false;

        for (int slot = 0;
             slot < player.getInventory().getSize();
             slot++) {

            ItemStack item = player.getInventory().getItem(slot);

            if (isRestricted(item)) {

                player.getInventory().setItem(slot, null);
                removed = true;
            }
        }

        if (removed) {

            player.updateInventory();

            player.sendMessage(
                    "§c所持禁止アイテムを回収しました。"
            );
        }
    }

    // 制限アイテムを使う操作を防止
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {

        if (isRestricted(event.getItem())) {

            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }
}
