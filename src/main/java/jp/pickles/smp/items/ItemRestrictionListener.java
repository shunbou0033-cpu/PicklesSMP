
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;

import java.util.HashMap;
import java.util.Map;

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

    // バンドル内部も含めてアイテム数を数える
    private void countItems(
            ItemStack item,
            Map<Material, Integer> counts
    ) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        Material material = item.getType();

        if (restricted.isRestricted(material)) {
            counts.merge(material, item.getAmount(), Integer::sum);
        }

        if (material == Material.BUNDLE
                && item.getItemMeta() instanceof BundleMeta meta) {

            for (ItemStack inside : meta.getItems()) {
                countItems(inside, counts);
            }
        }
    }

    // プレイヤーの所持数を取得
    private Map<Material, Integer> getPlayerCounts(Player player) {

        Map<Material, Integer> counts = new HashMap<>();

        for (ItemStack item : player.getInventory().getContents()) {
            countItems(item, counts);
        }

        return counts;
    }

    // 追加後に上限を超えるか確認
    private boolean exceedsLimit(
            Player player,
            ItemStack addedItem
    ) {
        Map<Material, Integer> counts = getPlayerCounts(player);

        Map<Material, Integer> additions = new HashMap<>();
        countItems(addedItem, additions);

        for (Map.Entry<Material, Integer> entry : additions.entrySet()) {

            Material material = entry.getKey();

            int current = counts.getOrDefault(material, 0);
            int added = entry.getValue();
            int limit = restricted.getLimit(material);

            if (limit >= 0 && current + added > limit) {
                return true;
            }
        }

        return false;
    }

    private void deny(Player player, Material material) {

        player.sendMessage(
                "§c" + material.name()
                        + " の所持上限を超えるため操作できません。"
        );
    }

    // アイテム拾得
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack item = event.getItem().getItemStack();

        if (exceedsLimit(player, item)) {
            event.setCancelled(true);
            deny(player, item.getType());
        }
    }

    // インベントリ操作
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clicked = event.getClickedInventory();

        if (clicked == null) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // プレイヤーインベントリからチェストへの移動は許可
        if (clicked != player.getInventory()
                && event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return;
        }

        // チェストからプレイヤーへの移動を確認
        if (clicked != player.getInventory()) {

            if (event.isShiftClick() && current != null) {

                if (exceedsLimit(player, current)) {
                    event.setCancelled(true);
                    deny(player, current.getType());
                }
            }

            return;
        }

        // カーソルからプレイヤーインベントリへ移す場合
        if (cursor != null && !cursor.getType().isAir()) {

            if (exceedsLimit(player, cursor)) {

                event.setCancelled(true);
                deny(player, cursor.getType());
            }
        }

        // ホットバーキー交換
        if (event.getClick() == ClickType.NUMBER_KEY) {

            int slot = event.getHotbarButton();

            if (slot >= 0) {

                ItemStack hotbarItem =
                        player.getInventory().getItem(slot);

                if (hotbarItem != null
                        && exceedsLimit(player, hotbarItem)) {

                    event.setCancelled(true);
                    deny(player, hotbarItem.getType());
                }
            }
        }
    }

    // ドラッグ操作
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack cursor = event.getOldCursor();

        if (cursor == null || cursor.getType().isAir()) {
            return;
        }

        boolean touchesPlayerInventory = event.getRawSlots()
                .stream()
                .anyMatch(slot ->
                        slot >= event.getView().getTopInventory().getSize());

        if (touchesPlayerInventory && exceedsLimit(player, cursor)) {

            event.setCancelled(true);
            deny(player, cursor.getType());
        }
    }

    // ドロップ禁止：上限管理対象アイテムを超過状態で持っている場合でも、
    // ドロップ自体は許可する
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        // ドロップは許可
    }

    // 手持ち交換
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {

        ItemStack offhand = event.getOffHandItem();

        if (offhand != null && exceedsLimit(event.getPlayer(), offhand)) {
            event.setCancelled(true);
            deny(event.getPlayer(), offhand.getType());
        }
    }

    // クラフト結果の取得
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack result = event.getRecipe().getResult();

        if (exceedsLimit(player, result)) {
            event.setCancelled(true);
            deny(player, result.getType());
        }
    }

    // クリエイティブインベントリ操作
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreative(InventoryCreativeEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack cursor = event.getCursor();

        if (cursor != null && exceedsLimit(player, cursor)) {
            event.setCancelled(true);
            deny(player, cursor.getType());
        }
    }

    // バンドルなどを右クリックで使用する場合
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {

        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.BUNDLE) {
            return;
        }

        if (exceedsLimit(event.getPlayer(), item)) {
            event.setCancelled(true);
            deny(event.getPlayer(), item.getType());
        }
    }
}
