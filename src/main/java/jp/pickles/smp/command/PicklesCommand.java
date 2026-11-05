
package jp.pickles.smp.command;

import jp.pickles.smp.PicklesSMP;
import jp.pickles.smp.items.RestrictedItems;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class PicklesCommand implements CommandExecutor, TabCompleter {

    private final PicklesSMP plugin;
    private final RestrictedItems restrictedItems;

    public PicklesCommand(PicklesSMP plugin) {
        this.plugin = plugin;
        this.restrictedItems = plugin.getRestrictedItems();
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (!sender.hasPermission("picklessmp.admin")) {
            sender.sendMessage("§cこのコマンドを使用する権限がありません。");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {

            case "reload" -> {
                plugin.reloadPlugin();
                sender.sendMessage("§aPicklesSMPの設定を再読み込みしました。");
            }

            case "item" -> handleItemCommand(sender, args);

            default -> {
                sender.sendMessage("§c不明なコマンドです。");
                sendHelp(sender);
            }
        }

        return true;
    }

    private void handleItemCommand(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sendItemHelp(sender);
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {

            case "limit" -> {

                if (args.length < 4) {
                    sender.sendMessage("§c使用方法: /picklessmp item limit <ID> <個数>");
                    return;
                }

                Material material = Material.matchMaterial(args[2]);

                if (material == null || !material.isItem()) {
                    sender.sendMessage("§c存在しないアイテムです。");
                    return;
                }

                int amount;

                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException exception) {
                    sender.sendMessage("§c個数は数字で指定してください。");
                    return;
                }

                if (amount < 0) {
                    sender.sendMessage("§c個数は0以上にしてください。");
                    return;
                }

                restrictedItems.setLimit(material, amount);

                sender.sendMessage(
                        "§a" + material.name() + " の所持上限を "
                                + amount + " 個に設定しました。"
                );
            }

            case "add" -> {

                if (args.length < 3) {
                    sender.sendMessage("§c使用方法: /picklessmp item add <ID>");
                    return;
                }

                Material material = Material.matchMaterial(args[2]);

                if (material == null || !material.isItem()) {
                    sender.sendMessage("§c存在しないアイテムです。");
                    return;
                }

                restrictedItems.setLimit(material, 0);

                sender.sendMessage(
                        "§a" + material.name() + " の所持上限を0個に設定しました。"
                );
            }

            case "remove" -> {

                if (args.length < 3) {
                    sender.sendMessage("§c使用方法: /picklessmp item remove <ID>");
                    return;
                }

                Material material = Material.matchMaterial(args[2]);

                if (material == null || !material.isItem()) {
                    sender.sendMessage("§c存在しないアイテムです。");
                    return;
                }

                if (!restrictedItems.remove(material)) {
                    sender.sendMessage("§cそのアイテムには上限が設定されていません。");
                    return;
                }

                sender.sendMessage(
                        "§a" + material.name() + " の所持上限を解除しました。"
                );
            }

            case "list" -> {

                Map<Material, Integer> limits = restrictedItems.getAll();

                if (limits.isEmpty()) {
                    sender.sendMessage("§e現在、アイテムの所持上限は設定されていません。");
                    return;
                }

                sender.sendMessage("§6===== アイテム所持上限 =====");

                for (Map.Entry<Material, Integer> entry : limits.entrySet()) {
                    sender.sendMessage(
                            "§f" + entry.getKey().name()
                                    + " §7: §e" + entry.getValue() + "個"
                    );
                }
            }

            default -> sendItemHelp(sender);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6===== PicklesSMP =====");
        sender.sendMessage("§e/picklessmp help §7- ヘルプ");
        sender.sendMessage("§e/picklessmp item §7- アイテム上限設定");
        sender.sendMessage("§e/picklessmp reload §7- 設定再読み込み");
    }

    private void sendItemHelp(CommandSender sender) {
        sender.sendMessage("§6===== アイテム上限コマンド =====");
        sender.sendMessage("§e/picklessmp item limit <ID> <個数>");
        sender.sendMessage("§e/picklessmp item add <ID> §7- 上限0個");
        sender.sendMessage("§e/picklessmp item remove <ID> §7- 上限解除");
        sender.sendMessage("§e/picklessmp item list §7- 一覧表示");
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        if (!sender.hasPermission("picklessmp.admin")) {
            if (args.length == 1) {
                return filter(List.of("help"), args[0]);
            }
            return List.of();
        }

        if (args.length == 1) {
            return filter(
                    List.of("help", "item", "reload"),
                    args[0]
            );
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("item")) {
            return filter(
                    List.of("limit", "add", "remove", "list"),
                    args[1]
            );
        }

        if (args.length == 3
                && args[0].equalsIgnoreCase("item")
                && !args[1].equalsIgnoreCase("list")) {

            return filter(
                    java.util.Arrays.stream(Material.values())
                            .filter(Material::isItem)
                            .map(Material::name)
                            .collect(Collectors.toList()),
                    args[2]
            );
        }

        return new ArrayList<>();
    }

    private List<String> filter(List<String> choices, String input) {
        String lowerInput = input.toLowerCase(Locale.ROOT);

        return choices.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lowerInput))
                .collect(Collectors.toList());
    }
}
