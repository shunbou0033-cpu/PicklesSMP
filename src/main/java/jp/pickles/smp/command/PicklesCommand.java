
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

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (!sender.hasPermission("picklessmp.admin")) {
            sender.sendMessage("§cこのコマンドを実行する権限がありません。");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {

            case "reload" -> {
                plugin.reloadPlugin();
                sender.sendMessage("§aPicklesSMPの設定を再読み込みしました。");
            }

            case "item" -> handleItem(sender, args);

            default -> sender.sendMessage(
                    "§c不明なコマンドです。 /picklessmp help"
            );
        }

        return true;
    }

    private void handleItem(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sendItemHelp(sender);
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {

            case "add" -> {

                if (args.length < 3) {
                    sender.sendMessage(
                            "§c使用方法: /picklessmp item add <アイテムID>"
                    );
                    return;
                }

                Material material = Material.matchMaterial(args[2]);

                if (material == null || !material.isItem()) {
                    sender.sendMessage("§c存在しないアイテムです。");
                    return;
                }

                if (restrictedItems.add(material)) {
                    sender.sendMessage(
                            "§a" + material.name() + " を制限アイテムに追加しました。"
                    );
                } else {
                    sender.sendMessage(
                            "§eそのアイテムは既に制限されています。"
                    );
                }
            }

            case "remove" -> {

                if (args.length < 3) {
                    sender.sendMessage(
                            "§c使用方法: /picklessmp item remove <アイテムID>"
                    );
                    return;
                }

                Material material = Material.matchMaterial(args[2]);

                if (material == null || !material.isItem()) {
                    sender.sendMessage("§c存在しないアイテムです。");
                    return;
                }

                if (restrictedItems.remove(material)) {
                    sender.sendMessage(
                            "§a" + material.name() + " の制限を解除しました。"
                    );
                } else {
                    sender.sendMessage(
                            "§eそのアイテムは制限されていません。"
                    );
                }
            }

            case "list" -> {

                if (restrictedItems.getAll().isEmpty()) {
                    sender.sendMessage(
                            "§e現在、制限アイテムはありません。"
                    );
                    return;
                }

                sender.sendMessage("§6===== 制限アイテム一覧 =====");

                for (Material material : restrictedItems.getAll()) {
                    sender.sendMessage("§f- " + material.name());
                }
            }

            default -> sendItemHelp(sender);
        }
    }

    private void sendHelp(CommandSender sender) {

        sender.sendMessage("§6===== PicklesSMP =====");
        sender.sendMessage("§e/picklessmp help §7- ヘルプ");

        if (sender.hasPermission("picklessmp.admin")) {
            sender.sendMessage("§e/picklessmp reload §7- 設定再読み込み");
            sender.sendMessage("§e/picklessmp item add <ID> §7- アイテム制限追加");
            sender.sendMessage("§e/picklessmp item remove <ID> §7- 制限解除");
            sender.sendMessage("§e/picklessmp item list §7- 制限一覧");
        }
    }

    private void sendItemHelp(CommandSender sender) {

        sender.sendMessage("§6===== アイテム制限コマンド =====");
        sender.sendMessage("§e/picklessmp item add <ID>");
        sender.sendMessage("§e/picklessmp item remove <ID>");
        sender.sendMessage("§e/picklessmp item list");
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        if (!sender.hasPermission("picklessmp.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return filter(List.of("help", "reload", "item"), args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("item")) {
            return filter(List.of("add", "remove", "list"), args[1]);
        }

        if (args.length == 3
                && args[0].equalsIgnoreCase("item")
                && (args[1].equalsIgnoreCase("add")
                || args[1].equalsIgnoreCase("remove"))) {

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

        return choices.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT)
                        .startsWith(input.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }
}
