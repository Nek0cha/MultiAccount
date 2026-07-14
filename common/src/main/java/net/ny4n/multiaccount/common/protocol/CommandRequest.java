package net.ny4n.multiaccount.common.protocol;

import java.util.List;
import java.util.UUID;

/**
 * Paper -&gt; Velocity へのコマンド転送リクエスト。
 * 送信元の権限判定はペイロードの自己申告ではなく、Velocity側で
 * 実際のプラグインメッセージ接続元（ServerConnectionのPlayer）から行うため、
 * ここには送信元情報を含めない。
 */
public record CommandRequest(UUID correlationId, String subcommand, List<String> args) {
}
