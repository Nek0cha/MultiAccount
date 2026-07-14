package net.ny4n.multiaccount.common;

public final class MultiAccountConstants {

    public static final String CHANNEL = "multiaccount:main";
    public static final int DEFAULT_MAX_DUPLICATES = 10;
    public static final String DEFAULT_SUFFIX_FORMAT = "_%02d";
    public static final String DEFAULT_SUFFIX_REGEX = "_(\\d{2})$";
    public static final int DEFAULT_BRIDGE_TIMEOUT_SECONDS = 5;
    public static final int DEFAULT_RECONCILE_INTERVAL_SECONDS = 30;
    public static final int MAX_PLAYER_NAME_LENGTH = 16;

    private MultiAccountConstants() {
    }
}
