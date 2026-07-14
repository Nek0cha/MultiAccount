package net.ny4n.multiaccount.velocity.bridge;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import net.ny4n.multiaccount.common.protocol.CommandRequest;
import net.ny4n.multiaccount.common.protocol.CommandResponse;
import net.ny4n.multiaccount.common.protocol.ProtocolCodec;
import net.ny4n.multiaccount.velocity.command.CommandDispatcher;
import net.ny4n.multiaccount.velocity.command.DispatchResult;
import net.ny4n.multiaccount.velocity.config.VelocityConfig;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Paper側から multiaccount:main チャンネル経由で転送されてきたコマンドを処理する。
 * 権限判定はペイロードの自己申告値ではなく、実際のプラグインメッセージ接続元
 * (ServerConnectionが保持するPlayer)から取得したUUIDで行う（なりすまし対策）。
 */
public final class PaperBridgeListener {

    private final ChannelIdentifier channel;
    private final VelocityConfig config;
    private final CommandDispatcher dispatcher;
    private final Logger logger;

    public PaperBridgeListener(ChannelIdentifier channel, VelocityConfig config,
                                CommandDispatcher dispatcher, Logger logger) {
        this.channel = channel;
        this.config = config;
        this.dispatcher = dispatcher;
        this.logger = logger;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(channel)) {
            return;
        }
        if (!(event.getSource() instanceof ServerConnection serverConnection)) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        CommandRequest request;
        try {
            request = ProtocolCodec.decodeRequest(event.getData());
        } catch (Exception e) {
            logger.warn("multiaccountブリッジのペイロード解析に失敗しました", e);
            return;
        }

        Player actualSender = serverConnection.getPlayer();
        UUID senderUuid = actualSender.getUniqueId();

        CommandResponse response;
        if (!config.admins().contains(senderUuid)) {
            logger.warn("{} が権限なしでmultiaccountブリッジコマンドを試行しました: {} {}",
                    actualSender.getUsername(), request.subcommand(), request.args());
            response = CommandResponse.denied(request.correlationId());
        } else {
            DispatchResult result = dispatcher.execute(request.subcommand(), request.args());
            response = new CommandResponse(request.correlationId(), result.success(), result.lines());
        }

        serverConnection.sendPluginMessage(channel, ProtocolCodec.encodeResponse(response));
    }
}
