package net.ny4n.multiaccount.paper;

import net.ny4n.multiaccount.paper.bridge.PaperCommandBridge;
import net.ny4n.multiaccount.paper.command.PaperMultiAccountCommand;
import net.ny4n.multiaccount.paper.config.PaperConfig;
import net.ny4n.multiaccount.paper.listener.OpSyncListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MultiAccountPaperPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        PaperConfig config = PaperConfig.load(getConfig(), getLogger());

        getServer().getPluginManager().registerEvents(new OpSyncListener(config, getLogger()), this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, config.bridgeChannel());
        PaperCommandBridge bridge = new PaperCommandBridge(this, config);
        getServer().getMessenger().registerIncomingPluginChannel(this, config.bridgeChannel(), bridge);

        PluginCommand command = getCommand("multiaccount");
        if (command != null) {
            command.setExecutor(new PaperMultiAccountCommand(bridge));
        } else {
            getLogger().warning("plugin.yml に multiaccount コマンドが見つかりません");
        }

        getLogger().info("MultiAccount(Paper) を初期化しました。op-sync=" + config.opSyncEnabled());
    }
}
