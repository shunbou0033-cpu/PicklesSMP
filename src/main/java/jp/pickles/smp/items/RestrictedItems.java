
package jp.pickles.smp.items;

import jp.pickles.smp.PicklesSMP;

import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class RestrictedItems {

    private final PicklesSMP plugin;

    private final Set<Material> restrictedMaterials =
            EnumSet.noneOf(Material.class);

    public RestrictedItems(PicklesSMP plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {

        restrictedMaterials.clear();

        List<String> materials =
                plugin.getConfig().getStringList(
                        "items.restricted-materials"
                );

        for (String name : materials) {

            Material material = Material.matchMaterial(name);

            if (material == null || !material.isItem()) {

                plugin.getLogger().warning(
                        "不明な制限アイテム: " + name
                );

                continue;
            }

            restrictedMaterials.add(material);
        }
    }

    public boolean isRestricted(Material material) {

        return material != null
                && restrictedMaterials.contains(material);
    }

    public boolean add(Material material) {

        if (material == null || !material.isItem()) {
            return false;
        }

        if (!restrictedMaterials.add(material)) {
            return false;
        }

        save();
        return true;
    }

    public boolean remove(Material material) {

        if (!restrictedMaterials.remove(material)) {
            return false;
        }

        save();
        return true;
    }

    public Set<Material> getAll() {

        return Collections.unmodifiableSet(
                restrictedMaterials
        );
    }

    private void save() {

        List<String> names = restrictedMaterials.stream()
                .map(Material::name)
                .sorted()
                .toList();

        plugin.getConfig().set(
                "items.restricted-materials",
                names
        );

        plugin.saveConfig();
    }
}
