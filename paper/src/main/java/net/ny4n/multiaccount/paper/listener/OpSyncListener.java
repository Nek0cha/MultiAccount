package net.ny4n.multiaccount.paper.listener;

import net.ny4n.multiaccount.common.ParsedVirtualName;
import net.ny4n.multiaccount.common.SuffixScheme;
import net.ny4n.multiaccount.paper.config.PaperConfig;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 複製プロフィールでログインしたプレイヤーに、ベースアカウントのOP状態を同期する。
 * ops.jsonへの恒久登録は行わず、ログインのたびに再判定する（付与・剥奪の両方向）。
 *
 * PlayerLoginEventはPaper 1.21.6でPlayerオブジェクトの早期生成を招くとして非推奨化されたため、
 * Playerオブジェクトを必要としないAsyncPlayerPreLoginEventを使用する。OP状態は
 * Bukkit.getOfflinePlayer(uuid).setOp(...) でも(オンライン/オフライン問わず)即座にops.jsonへ反映される。
 *
 * 仮想プレイヤーの判定は「suffix-regexへの名前一致」だけでなく
 * 「オフライン方式UUIDとの一致」も必須とすることで、たまたま同様の名前を持つ
 * 実アカウントを誤って複製として扱わないようにしている。
 */
public final class OpSyncListener implements Listener {

    private final PaperConfig config;
    private final Logger logger;

    public OpSyncListener(PaperConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!config.opSyncEnabled()) {
            return;
        }

        String name = event.getName();
        UUID uuid = event.getUniqueId();

        Optional<ParsedVirtualName> parsed = SuffixScheme.parse(name, config.suffixRegex());
        if (parsed.isEmpty()) {
            return;
        }
        if (!SuffixScheme.offlineUuid(name).equals(uuid)) {
            return;
        }

        String baseName = parsed.get().baseName();
        boolean baseIsOp = isBaseOp(baseName);

        OfflinePlayer virtualPlayer = Bukkit.getOfflinePlayer(uuid);
        if (virtualPlayer.isOp() != baseIsOp) {
            virtualPlayer.setOp(baseIsOp);
            logger.info(name + " のOP状態をベースアカウント " + baseName + " (op=" + baseIsOp + ") に同期しました");
        }
    }

    private boolean isBaseOp(String baseName) {
        for (OfflinePlayer op : Bukkit.getOperators()) {
            if (baseName.equalsIgnoreCase(op.getName())) {
                return true;
            }
        }
        return false;
    }
}
