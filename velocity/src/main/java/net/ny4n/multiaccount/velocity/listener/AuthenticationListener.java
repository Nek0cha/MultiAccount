package net.ny4n.multiaccount.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.util.GameProfile;
import net.ny4n.multiaccount.velocity.config.VelocityConfig;
import net.ny4n.multiaccount.velocity.config.WhitelistStore;
import net.ny4n.multiaccount.velocity.profile.VirtualProfileFactory;
import net.ny4n.multiaccount.velocity.session.DuplicateSessionManager;
import net.ny4n.multiaccount.velocity.session.SessionAllocation;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

/**
 * Mojang本人認証が完了した直後（GameProfileRequestEvent）にフックし、
 * ホワイトリスト対象ユーザーの多重ログインをスロット割当のうえ仮想プロフィールへ差し替える。
 * このイベントはVelocity本体の重複接続チェック(canRegisterConnection)より前に発火するため、
 * ここでUUID/名前を書き換えれば標準の重複検出ロジックを一切変更せずに済む。
 */
public final class AuthenticationListener {

    private final VelocityConfig config;
    private final WhitelistStore whitelistStore;
    private final DuplicateSessionManager sessionManager;
    private final Logger logger;

    public AuthenticationListener(VelocityConfig config, WhitelistStore whitelistStore,
                                   DuplicateSessionManager sessionManager, Logger logger) {
        this.config = config;
        this.whitelistStore = whitelistStore;
        this.sessionManager = sessionManager;
        this.logger = logger;
    }

    // priority()は数値が大きいほど早く呼ばれる仕様のため、PostOrder.LATE相当
    // (Short.MIN_VALUE / 2)を明示して他プラグインのプロフィール改変を尊重してから確定させる。
    @Subscribe(priority = Short.MIN_VALUE / 2)
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        GameProfile original = event.getOriginalProfile();
        UUID baseUuid = original.getId();
        String baseName = original.getName();

        if (!whitelistStore.contains(baseUuid)) {
            return;
        }

        Optional<SessionAllocation> allocation = sessionManager.tryAllocate(baseUuid, baseName);
        if (allocation.isEmpty()) {
            logger.info("{} の多重ログイン上限({})に達しているため通常の重複接続処理に委ねます",
                    baseName, config.maxDuplicates());
            return;
        }

        SessionAllocation a = allocation.get();
        if (a.isPrimary()) {
            return;
        }

        GameProfile virtual = VirtualProfileFactory.createVirtual(event.getGameProfile(), a.virtualName());
        event.setGameProfile(virtual);
        logger.info("{} -> 仮想プロフィール {} ({}) を割り当てました", baseName, a.virtualName(), virtual.getId());
    }
}
