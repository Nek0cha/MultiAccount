package net.ny4n.multiaccount.common.protocol;

import java.util.List;
import java.util.UUID;

public record CommandResponse(UUID correlationId, boolean success, List<String> lines) {

    public static CommandResponse ok(UUID correlationId, List<String> lines) {
        return new CommandResponse(correlationId, true, lines);
    }

    public static CommandResponse denied(UUID correlationId) {
        return new CommandResponse(correlationId, false, List.of("この操作を実行する権限がありません。"));
    }

    public static CommandResponse error(UUID correlationId, String message) {
        return new CommandResponse(correlationId, false, List.of(message));
    }
}
