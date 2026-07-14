package net.ny4n.multiaccount.velocity.command;

import java.util.List;

public record DispatchResult(boolean success, List<String> lines) {

    public static DispatchResult ok(List<String> lines) {
        return new DispatchResult(true, lines);
    }

    public static DispatchResult error(String message) {
        return new DispatchResult(false, List.of(message));
    }
}
