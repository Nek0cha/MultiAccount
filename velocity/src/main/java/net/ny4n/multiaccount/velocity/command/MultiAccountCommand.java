package net.ny4n.multiaccount.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.ny4n.multiaccount.velocity.config.VelocityConfig;

import java.util.Arrays;
import java.util.List;

public final class MultiAccountCommand implements SimpleCommand {

    private final CommandDispatcher dispatcher;
    private final VelocityConfig config;

    public MultiAccountCommand(CommandDispatcher dispatcher, VelocityConfig config) {
        this.dispatcher = dispatcher;
        this.config = config;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!hasPermission(invocation)) {
            source.sendPlainMessage("この操作を実行する権限がありません。");
            return;
        }

        String[] args = invocation.arguments();
        String subcommand = args.length > 0 ? args[0] : "help";
        List<String> rest = args.length > 1 ? Arrays.asList(args).subList(1, args.length) : List.of();

        DispatchResult result = dispatcher.execute(subcommand, rest);
        result.lines().forEach(source::sendPlainMessage);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        CommandSource source = invocation.source();
        if (source instanceof Player player) {
            return config.admins().contains(player.getUniqueId());
        }
        return true;
    }
}
