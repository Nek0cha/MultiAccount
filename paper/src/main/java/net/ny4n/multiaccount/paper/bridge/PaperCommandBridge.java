package net.ny4n.multiaccount.paper.bridge;

import net.ny4n.multiaccount.common.protocol.CommandRequest;
import net.ny4n.multiaccount.common.protocol.CommandResponse;
import net.ny4n.multiaccount.common.protocol.ProtocolCodec;
import net.ny4n.multiaccount.paper.config.PaperConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Paper -&gt; Velocity のコマンド転送とその応答受信を担う。
 * プラグインメッセージはプレイヤーの接続経由でしか送信できないため、
 * コンソールからのコマンド実行には対応しない。
 */
public final class PaperCommandBridge implements PluginMessageListener {

    private record PendingRequest(CommandSender sender, BukkitTask timeoutTask) {
    }

    private final JavaPlugin plugin;
    private final PaperConfig config;
    private final Map<UUID, PendingRequest> pending = new ConcurrentHashMap<>();

    public PaperCommandBridge(JavaPlugin plugin, PaperConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void send(Player carrier, CommandSender sender, String subcommand, List<String> args) {
        UUID correlationId = UUID.randomUUID();
        CommandRequest request = new CommandRequest(correlationId, subcommand, args);

        BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingRequest removed = pending.remove(correlationId);
            if (removed != null) {
                removed.sender().sendMessage("Velocityへの問い合わせがタイムアウトしました。");
            }
        }, config.bridgeTimeoutSeconds() * 20L);

        pending.put(correlationId, new PendingRequest(sender, timeoutTask));
        carrier.sendPluginMessage(plugin, config.bridgeChannel(), ProtocolCodec.encodeRequest(request));
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(config.bridgeChannel())) {
            return;
        }
        CommandResponse response;
        try {
            response = ProtocolCodec.decodeResponse(message);
        } catch (Exception e) {
            plugin.getLogger().warning("multiaccountブリッジの応答解析に失敗しました: " + e.getMessage());
            return;
        }

        PendingRequest request = pending.remove(response.correlationId());
        if (request == null) {
            return;
        }
        request.timeoutTask().cancel();

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (String line : response.lines()) {
                request.sender().sendMessage(line);
            }
        });
    }
}
