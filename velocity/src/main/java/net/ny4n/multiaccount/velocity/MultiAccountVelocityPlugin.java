package net.ny4n.multiaccount.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.ny4n.multiaccount.velocity.bridge.PaperBridgeListener;
import net.ny4n.multiaccount.velocity.command.CommandDispatcher;
import net.ny4n.multiaccount.velocity.command.MultiAccountCommand;
import net.ny4n.multiaccount.velocity.config.VelocityConfig;
import net.ny4n.multiaccount.velocity.config.WhitelistStore;
import net.ny4n.multiaccount.velocity.listener.AuthenticationListener;
import net.ny4n.multiaccount.velocity.listener.SessionLifecycleListener;
import net.ny4n.multiaccount.velocity.session.DuplicateSessionManager;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Duration;

@Plugin(
        id = "multiaccount",
        name = "MultiAccount",
        version = "1.0.0",
        description = "同一Mojangアカウントによるデバッグ用多重ログインを許可するプラグイン",
        authors = {"ny4n"}
)
public final class MultiAccountVelocityPlugin {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public MultiAccountVelocityPlugin(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        VelocityConfig config = VelocityConfig.load(dataDirectory, logger);
        WhitelistStore whitelistStore = WhitelistStore.load(dataDirectory, config.whitelistFile(), logger);
        DuplicateSessionManager sessionManager = new DuplicateSessionManager(
                proxyServer, config.maxDuplicates(), config.suffixFormat(), logger);
        CommandDispatcher dispatcher = new CommandDispatcher(sessionManager, whitelistStore, proxyServer, logger);

        proxyServer.getEventManager().register(this,
                new AuthenticationListener(config, whitelistStore, sessionManager, logger));
        proxyServer.getEventManager().register(this, new SessionLifecycleListener(sessionManager));

        MinecraftChannelIdentifier channel = MinecraftChannelIdentifier.from(config.bridgeChannel());
        proxyServer.getChannelRegistrar().register(channel);
        proxyServer.getEventManager().register(this,
                new PaperBridgeListener(channel, config, dispatcher, logger));

        proxyServer.getCommandManager().register(
                proxyServer.getCommandManager().metaBuilder("multiaccount").aliases("ma").build(),
                new MultiAccountCommand(dispatcher, config));

        proxyServer.getScheduler().buildTask(this, sessionManager::reconcile)
                .delay(Duration.ofSeconds(config.reconcileIntervalSeconds()))
                .repeat(Duration.ofSeconds(config.reconcileIntervalSeconds()))
                .schedule();

        logger.info("MultiAccount(Velocity) を初期化しました。max-duplicates={}, whitelist={}件",
                config.maxDuplicates(), whitelistStore.asMap().size());
    }
}
