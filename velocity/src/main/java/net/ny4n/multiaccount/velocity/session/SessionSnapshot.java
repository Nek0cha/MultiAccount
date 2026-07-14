package net.ny4n.multiaccount.velocity.session;

import java.util.UUID;

/**
 * /multiaccount list 表示用の1セッション分のスナップショット。
 */
public record SessionSnapshot(UUID baseUuid, String baseName, int slot, UUID virtualUuid,
                               String virtualName, String serverName) {
}
