package net.ny4n.multiaccount.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import net.ny4n.multiaccount.velocity.session.DuplicateSessionManager;

/**
 * プレイヤー切断時にスロットを解放する。DisconnectEventはVelocity上で
 * 非同期・取りこぼしの可能性があるとされているため、DuplicateSessionManagerの
 * 定期リコンサイルと合わせて二重に整合性を担保する。
 */
public final class SessionLifecycleListener {

    private final DuplicateSessionManager sessionManager;

    public SessionLifecycleListener(DuplicateSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        sessionManager.release(event.getPlayer().getUniqueId());
    }
}
