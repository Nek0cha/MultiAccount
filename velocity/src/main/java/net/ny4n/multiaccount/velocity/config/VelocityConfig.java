package net.ny4n.multiaccount.velocity.config;

import net.ny4n.multiaccount.common.MultiAccountConstants;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * velocity/config.yml のロード結果。ホワイトリスト（whitelist.yml）は
 * {@link WhitelistStore} が別途管理する。
 */
public final class VelocityConfig {

    private final int maxDuplicates;
    private final String suffixFormat;
    private final String whitelistFile;
    private final Set<UUID> admins;
    private final String bridgeChannel;
    private final int bridgeTimeoutSeconds;
    private final int reconcileIntervalSeconds;
    private final String messagePrefix;
    private final String messageDuplicateDenied;
    private final String messageLimitReached;

    private VelocityConfig(int maxDuplicates, String suffixFormat, String whitelistFile,
                            Set<UUID> admins, String bridgeChannel, int bridgeTimeoutSeconds,
                            int reconcileIntervalSeconds, String messagePrefix,
                            String messageDuplicateDenied, String messageLimitReached) {
        this.maxDuplicates = maxDuplicates;
        this.suffixFormat = suffixFormat;
        this.whitelistFile = whitelistFile;
        this.admins = admins;
        this.bridgeChannel = bridgeChannel;
        this.bridgeTimeoutSeconds = bridgeTimeoutSeconds;
        this.reconcileIntervalSeconds = reconcileIntervalSeconds;
        this.messagePrefix = messagePrefix;
        this.messageDuplicateDenied = messageDuplicateDenied;
        this.messageLimitReached = messageLimitReached;
    }

    @SuppressWarnings("unchecked")
    public static VelocityConfig load(Path dataDirectory, Logger logger) {
        Path configPath = dataDirectory.resolve("config.yml");
        try {
            Files.createDirectories(dataDirectory);
            if (!Files.exists(configPath)) {
                try (InputStream in = VelocityConfig.class.getResourceAsStream("/config.yml")) {
                    if (in == null) {
                        throw new IllegalStateException("同梱の config.yml リソースが見つかりません");
                    }
                    Files.copy(in, configPath);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("config.yml の準備に失敗しました", e);
        }

        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(configPath)) {
            root = (Map<String, Object>) new Yaml().load(in);
        } catch (IOException e) {
            throw new IllegalStateException("config.yml の読み込みに失敗しました", e);
        }
        if (root == null) {
            root = Map.of();
        }

        int maxDuplicates = asInt(root.get("max-duplicates"), MultiAccountConstants.DEFAULT_MAX_DUPLICATES);
        String suffixFormat = asString(root.get("suffix-format"), MultiAccountConstants.DEFAULT_SUFFIX_FORMAT);
        String whitelistFile = asString(root.get("whitelist-file"), "whitelist.yml");
        String bridgeChannel = asString(root.get("bridge-channel"), MultiAccountConstants.CHANNEL);
        int bridgeTimeoutSeconds = asInt(root.get("bridge-timeout-seconds"),
                MultiAccountConstants.DEFAULT_BRIDGE_TIMEOUT_SECONDS);
        int reconcileIntervalSeconds = asInt(root.get("reconcile-interval-seconds"),
                MultiAccountConstants.DEFAULT_RECONCILE_INTERVAL_SECONDS);

        Set<UUID> admins = new LinkedHashSet<>();
        Object adminsRaw = root.get("admins");
        if (adminsRaw instanceof List<?> list) {
            for (Object entry : list) {
                try {
                    admins.add(UUID.fromString(String.valueOf(entry).trim()));
                } catch (IllegalArgumentException e) {
                    logger.warn("config.yml の admins に不正なUUIDが含まれています: {}", entry);
                }
            }
        }

        Map<String, Object> messages = root.get("messages") instanceof Map<?, ?> m
                ? (Map<String, Object>) m
                : Map.of();
        String prefix = asString(messages.get("prefix"), "<gray>[MultiAccount]</gray> ");
        String duplicateDenied = asString(messages.get("duplicate-denied"),
                "<red>このアカウントは多重ログインが許可されていません。");
        String limitReached = asString(messages.get("limit-reached"),
                "<red>多重ログインの上限に達しています。");

        return new VelocityConfig(maxDuplicates, suffixFormat, whitelistFile, admins, bridgeChannel,
                bridgeTimeoutSeconds, reconcileIntervalSeconds, prefix, duplicateDenied, limitReached);
    }

    private static int asInt(Object value, int fallback) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return fallback;
    }

    private static String asString(Object value, String fallback) {
        return value != null ? String.valueOf(value) : fallback;
    }

    public int maxDuplicates() {
        return maxDuplicates;
    }

    public String suffixFormat() {
        return suffixFormat;
    }

    public String whitelistFile() {
        return whitelistFile;
    }

    public Set<UUID> admins() {
        return admins;
    }

    public String bridgeChannel() {
        return bridgeChannel;
    }

    public int bridgeTimeoutSeconds() {
        return bridgeTimeoutSeconds;
    }

    public int reconcileIntervalSeconds() {
        return reconcileIntervalSeconds;
    }

    public String messagePrefix() {
        return messagePrefix;
    }

    public String messageDuplicateDenied() {
        return messageDuplicateDenied;
    }

    public String messageLimitReached() {
        return messageLimitReached;
    }
}
