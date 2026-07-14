package net.ny4n.multiaccount.velocity.config;

import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 重複ログインを許可するユーザーのホワイトリスト（whitelist.yml）。
 * UUIDを一次キーとして管理し、コマンドで変更されるたびに即座にファイルへ保存する。
 */
public final class WhitelistStore {

    private final Path file;
    private final Logger logger;
    private final Map<UUID, String> entries = new ConcurrentHashMap<>();
    private final Yaml yaml;

    private WhitelistStore(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        this.yaml = new Yaml(options);
    }

    @SuppressWarnings("unchecked")
    public static WhitelistStore load(Path dataDirectory, String whitelistFileName, Logger logger) {
        Path file = dataDirectory.resolve(whitelistFileName);
        WhitelistStore store = new WhitelistStore(file, logger);
        try {
            Files.createDirectories(dataDirectory);
            if (!Files.exists(file)) {
                try (InputStream in = WhitelistStore.class.getResourceAsStream("/whitelist.yml")) {
                    if (in != null) {
                        Files.copy(in, file);
                    } else {
                        store.save();
                    }
                }
            }
            Map<String, Object> root;
            try (InputStream in = Files.newInputStream(file)) {
                root = (Map<String, Object>) new Yaml().load(in);
            }
            if (root != null && root.get("entries") instanceof List<?> list) {
                for (Object raw : list) {
                    if (raw instanceof Map<?, ?> entry) {
                        Object uuidRaw = entry.get("uuid");
                        Object nameRaw = entry.get("name");
                        if (uuidRaw == null) {
                            continue;
                        }
                        try {
                            UUID uuid = UUID.fromString(String.valueOf(uuidRaw).trim());
                            store.entries.put(uuid, nameRaw != null ? String.valueOf(nameRaw) : "");
                        } catch (IllegalArgumentException e) {
                            logger.warn("whitelist.yml に不正なUUIDが含まれています: {}", uuidRaw);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("whitelist.yml の読み込みに失敗しました", e);
        }
        return store;
    }

    public boolean contains(UUID uuid) {
        return entries.containsKey(uuid);
    }

    public Optional<String> nameOf(UUID uuid) {
        return Optional.ofNullable(entries.get(uuid));
    }

    public synchronized boolean add(UUID uuid, String name) {
        boolean isNew = !entries.containsKey(uuid);
        entries.put(uuid, name);
        save();
        return isNew;
    }

    public synchronized boolean remove(UUID uuid) {
        boolean removed = entries.remove(uuid) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public Map<UUID, String> asMap() {
        return Map.copyOf(entries);
    }

    private void save() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<UUID, String> e : entries.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("uuid", e.getKey().toString());
            item.put("name", e.getValue());
            list.add(item);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("entries", list);

        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("# /multiaccount whitelist コマンドで自動生成・更新されます。手動編集も可。\n");
            yaml.dump(root, writer);
        } catch (IOException e) {
            logger.error("whitelist.yml の保存に失敗しました", e);
        }
    }
}
