package net.ny4n.multiaccount.velocity.session;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.ny4n.multiaccount.common.MultiAccountConstants;
import net.ny4n.multiaccount.common.SuffixScheme;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ベースアカウント（実UUID）ごとのスロット使用状況をメモリ上で管理する。
 * Velocityプロキシ全体でシングルトンとして保持され、GameProfileRequestEventと
 * DisconnectEventの双方から呼び出される。
 */
public final class DuplicateSessionManager {

    private record ActiveSession(UUID baseUuid, String baseName, int slot, String virtualName) {
    }

    private final ProxyServer proxyServer;
    private final int maxDuplicates;
    private final String suffixFormat;
    private final Logger logger;

    private final Map<UUID, boolean[]> slotsByBase = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveSession> sessionsByVirtual = new ConcurrentHashMap<>();

    public DuplicateSessionManager(ProxyServer proxyServer, int maxDuplicates, String suffixFormat, Logger logger) {
        this.proxyServer = proxyServer;
        this.maxDuplicates = maxDuplicates;
        this.suffixFormat = suffixFormat;
        this.logger = logger;
    }

    /**
     * ベースアカウントに対して空いているスロットを1つ確保する。
     * スロット1（本人枠）が空いていれば無改変のSessionAllocationを返す。
     * 全スロット使用中であれば空を返す（呼び出し側はVelocity標準の重複接続処理に委ねる）。
     */
    public synchronized Optional<SessionAllocation> tryAllocate(UUID baseUuid, String baseName) {
        boolean[] slots = slotsByBase.computeIfAbsent(baseUuid, k -> new boolean[maxDuplicates]);

        for (int i = 0; i < slots.length; i++) {
            if (slots[i]) {
                continue;
            }
            int slotNumber = i + 1;

            if (slotNumber == 1) {
                slots[i] = true;
                sessionsByVirtual.put(baseUuid, new ActiveSession(baseUuid, baseName, 1, baseName));
                return Optional.of(new SessionAllocation(1, baseUuid, baseName));
            }

            String virtualName = SuffixScheme.buildVirtualName(baseName, suffixFormat, slotNumber);
            if (virtualName.length() > MultiAccountConstants.MAX_PLAYER_NAME_LENGTH) {
                logger.warn("複製名 '{}' がMinecraftの名前長制限(16文字)を超えるためスロット{}をスキップします",
                        virtualName, slotNumber);
                continue;
            }

            slots[i] = true;
            UUID virtualUuid = SuffixScheme.offlineUuid(virtualName);
            sessionsByVirtual.put(virtualUuid, new ActiveSession(baseUuid, baseName, slotNumber, virtualName));
            return Optional.of(new SessionAllocation(slotNumber, virtualUuid, virtualName));
        }
        return Optional.empty();
    }

    /**
     * 切断されたプレイヤーの仮想UUID(本人枠ならベースUUID)を渡してスロットを解放する。
     */
    public synchronized void release(UUID virtualOrBaseUuid) {
        ActiveSession session = sessionsByVirtual.remove(virtualOrBaseUuid);
        if (session == null) {
            return;
        }
        boolean[] slots = slotsByBase.get(session.baseUuid());
        if (slots != null && session.slot() - 1 < slots.length) {
            slots[session.slot() - 1] = false;
        }
    }

    /**
     * DisconnectEventの取りこぼしに備えた自己修復。実際にオンラインなプレイヤー集合と
     * 突合し、既に切断済みのセッションを解放する。
     */
    public synchronized void reconcile() {
        Set<UUID> online = proxyServer.getAllPlayers().stream()
                .map(Player::getUniqueId)
                .collect(Collectors.toSet());
        for (UUID virtualUuid : List.copyOf(sessionsByVirtual.keySet())) {
            if (!online.contains(virtualUuid)) {
                logger.debug("リコンサイル: {} は非アクティブのためスロットを解放します", virtualUuid);
                release(virtualUuid);
            }
        }
    }

    public List<SessionSnapshot> snapshot() {
        List<SessionSnapshot> result = new ArrayList<>();
        for (Map.Entry<UUID, ActiveSession> entry : sessionsByVirtual.entrySet()) {
            UUID virtualUuid = entry.getKey();
            ActiveSession session = entry.getValue();
            String serverName = proxyServer.getPlayer(virtualUuid)
                    .flatMap(Player::getCurrentServer)
                    .map(sc -> sc.getServerInfo().getName())
                    .orElse("-");
            result.add(new SessionSnapshot(session.baseUuid(), session.baseName(), session.slot(),
                    virtualUuid, session.virtualName(), serverName));
        }
        result.sort((a, b) -> {
            int cmp = a.baseName().compareToIgnoreCase(b.baseName());
            return cmp != 0 ? cmp : Integer.compare(a.slot(), b.slot());
        });
        return result;
    }
}
