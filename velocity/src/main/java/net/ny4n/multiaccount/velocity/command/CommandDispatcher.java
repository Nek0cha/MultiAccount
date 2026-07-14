package net.ny4n.multiaccount.velocity.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.ny4n.multiaccount.velocity.config.WhitelistStore;
import net.ny4n.multiaccount.velocity.session.DuplicateSessionManager;
import net.ny4n.multiaccount.velocity.session.SessionSnapshot;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * /multiaccount サブコマンドの実処理本体。Velocityのスラッシュコマンド経路と
 * Paperからのプラグインメッセージブリッジ経路の両方から共通で呼び出される。
 */
public final class CommandDispatcher {

    private record ResolvedPlayer(UUID uuid, String name) {
    }

    private final DuplicateSessionManager sessionManager;
    private final WhitelistStore whitelistStore;
    private final ProxyServer proxyServer;
    private final Logger logger;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public CommandDispatcher(DuplicateSessionManager sessionManager, WhitelistStore whitelistStore,
                              ProxyServer proxyServer, Logger logger) {
        this.sessionManager = sessionManager;
        this.whitelistStore = whitelistStore;
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    public DispatchResult execute(String subcommand, List<String> args) {
        return switch (subcommand.toLowerCase(Locale.ROOT)) {
            case "list" -> list();
            case "whitelist" -> whitelist(args);
            case "help" -> help();
            default -> DispatchResult.error("不明なサブコマンドです。/multiaccount help を参照してください。");
        };
    }

    private DispatchResult list() {
        List<SessionSnapshot> snapshots = sessionManager.snapshot();
        if (snapshots.isEmpty()) {
            return DispatchResult.ok(List.of("現在、多重ログイン中のユーザーはいません。"));
        }
        List<String> lines = new ArrayList<>();
        lines.add("=== 多重ログイン中セッション ===");
        for (SessionSnapshot s : snapshots) {
            String label = s.slot() == 1 ? "本人" : "複製 slot " + s.slot();
            lines.add(String.format("- %s (%s) -> server: %s", s.virtualName(), label, s.serverName()));
        }
        return DispatchResult.ok(lines);
    }

    private DispatchResult whitelist(List<String> args) {
        if (args.isEmpty()) {
            return DispatchResult.error("使用法: /multiaccount whitelist <add|remove|list> [player|uuid]");
        }
        String action = args.get(0).toLowerCase(Locale.ROOT);
        return switch (action) {
            case "list" -> whitelistList();
            case "add" -> args.size() < 2
                    ? DispatchResult.error("使用法: /multiaccount whitelist add <player|uuid>")
                    : whitelistAdd(args.get(1));
            case "remove" -> args.size() < 2
                    ? DispatchResult.error("使用法: /multiaccount whitelist remove <player|uuid>")
                    : whitelistRemove(args.get(1));
            default -> DispatchResult.error("使用法: /multiaccount whitelist <add|remove|list> [player|uuid]");
        };
    }

    private DispatchResult whitelistList() {
        Map<UUID, String> map = whitelistStore.asMap();
        if (map.isEmpty()) {
            return DispatchResult.ok(List.of("ホワイトリストは空です。"));
        }
        List<String> lines = new ArrayList<>();
        lines.add("=== 多重ログイン許可ユーザー ===");
        map.forEach((uuid, name) -> lines.add("- " + name + " (" + uuid + ")"));
        return DispatchResult.ok(lines);
    }

    private DispatchResult whitelistAdd(String target) {
        Optional<ResolvedPlayer> resolved = resolvePlayer(target);
        if (resolved.isEmpty()) {
            return DispatchResult.error("プレイヤー '" + target + "' のUUIDを解決できませんでした。UUIDを直接指定してください。");
        }
        ResolvedPlayer r = resolved.get();
        boolean added = whitelistStore.add(r.uuid(), r.name());
        String verb = added ? "追加しました" : "更新しました（既に登録済み）";
        return DispatchResult.ok(List.of(verb + ": " + r.name() + " (" + r.uuid() + ")"));
    }

    private DispatchResult whitelistRemove(String target) {
        UUID uuid;
        try {
            uuid = UUID.fromString(target);
        } catch (IllegalArgumentException e) {
            uuid = whitelistStore.asMap().entrySet().stream()
                    .filter(entry -> entry.getValue().equalsIgnoreCase(target))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
        }
        if (uuid == null) {
            return DispatchResult.error("ホワイトリストに '" + target + "' は登録されていません。");
        }
        boolean removed = whitelistStore.remove(uuid);
        return removed
                ? DispatchResult.ok(List.of("削除しました: " + uuid))
                : DispatchResult.error("ホワイトリストに登録されていません: " + uuid);
    }

    private DispatchResult help() {
        return DispatchResult.ok(List.of(
                "/multiaccount list - 多重ログイン中のセッション一覧を表示",
                "/multiaccount whitelist list - ホワイトリスト一覧を表示",
                "/multiaccount whitelist add <player|uuid> - ホワイトリストに追加",
                "/multiaccount whitelist remove <player|uuid> - ホワイトリストから削除"
        ));
    }

    private Optional<ResolvedPlayer> resolvePlayer(String target) {
        try {
            UUID uuid = UUID.fromString(target);
            String name = proxyServer.getPlayer(uuid).map(Player::getUsername).orElse(target);
            return Optional.of(new ResolvedPlayer(uuid, name));
        } catch (IllegalArgumentException ignored) {
            // UUID直接指定ではない場合は名前として扱う
        }

        Optional<Player> online = proxyServer.getPlayer(target);
        if (online.isPresent()) {
            Player p = online.get();
            return Optional.of(new ResolvedPlayer(p.getUniqueId(), p.getUsername()));
        }

        return lookupMojangApi(target);
    }

    private Optional<ResolvedPlayer> lookupMojangApi(String name) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + name))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String idRaw = json.get("id").getAsString();
            String resolvedName = json.get("name").getAsString();
            UUID uuid = dashUuid(idRaw);
            return Optional.of(new ResolvedPlayer(uuid, resolvedName));
        } catch (Exception e) {
            logger.warn("Mojang APIへの問い合わせに失敗しました ({}): {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    private static UUID dashUuid(String undashed) {
        if (undashed.contains("-")) {
            return UUID.fromString(undashed);
        }
        String dashed = undashed.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
        return UUID.fromString(dashed);
    }
}
