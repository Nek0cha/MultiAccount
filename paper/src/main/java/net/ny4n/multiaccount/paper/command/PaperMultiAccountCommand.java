package net.ny4n.multiaccount.paper.command;

import net.ny4n.multiaccount.paper.bridge.PaperCommandBridge;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public final class PaperMultiAccountCommand implements CommandExecutor {

    private final PaperCommandBridge bridge;

    public PaperMultiAccountCommand(PaperCommandBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはゲーム内から実行してください（コンソール非対応）。");
            return true;
        }
        if (!player.hasPermission("multiaccount.command")) {
            player.sendMessage("この操作を実行する権限がありません。");
            return true;
        }

        String subcommand = args.length > 0 ? args[0] : "help";
        List<String> rest = args.length > 1 ? Arrays.asList(args).subList(1, args.length) : List.of();
        bridge.send(player, player, subcommand, rest);
        return true;
    }
}
