package net.ny4n.multiaccount.paper.config;

import net.ny4n.multiaccount.common.MultiAccountConstants;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.logging.Logger;

/**
 * paper/config.yml のロード結果。判定ロジックの主導権はVelocity側にあり、
 * こちらはOP同期とコマンド転送の受け口のみを担当する。
 */
public final class PaperConfig {

    private final Pattern suffixRegex;
    private final boolean opSyncEnabled;
    private final String bridgeChannel;
    private final int bridgeTimeoutSeconds;

    private PaperConfig(Pattern suffixRegex, boolean opSyncEnabled, String bridgeChannel, int bridgeTimeoutSeconds) {
        this.suffixRegex = suffixRegex;
        this.opSyncEnabled = opSyncEnabled;
        this.bridgeChannel = bridgeChannel;
        this.bridgeTimeoutSeconds = bridgeTimeoutSeconds;
    }

    public static PaperConfig load(FileConfiguration config, Logger logger) {
        String regexRaw = config.getString("suffix-regex", MultiAccountConstants.DEFAULT_SUFFIX_REGEX);
        Pattern pattern;
        try {
            pattern = Pattern.compile(regexRaw);
        } catch (PatternSyntaxException e) {
            logger.warning("suffix-regex '" + regexRaw + "' が不正なため既定値を使用します: " + e.getMessage());
            pattern = Pattern.compile(MultiAccountConstants.DEFAULT_SUFFIX_REGEX);
        }
        boolean opSyncEnabled = config.getBoolean("enable-op-sync", true);
        String bridgeChannel = config.getString("bridge-channel", MultiAccountConstants.CHANNEL);
        int bridgeTimeoutSeconds = config.getInt("bridge-timeout-seconds",
                MultiAccountConstants.DEFAULT_BRIDGE_TIMEOUT_SECONDS);
        return new PaperConfig(pattern, opSyncEnabled, bridgeChannel, bridgeTimeoutSeconds);
    }

    public Pattern suffixRegex() {
        return suffixRegex;
    }

    public boolean opSyncEnabled() {
        return opSyncEnabled;
    }

    public String bridgeChannel() {
        return bridgeChannel;
    }

    public int bridgeTimeoutSeconds() {
        return bridgeTimeoutSeconds;
    }
}
