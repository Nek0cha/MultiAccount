package net.ny4n.multiaccount.common;

/**
 * suffix-regex にマッチした複製プロフィール名の分解結果。
 *
 * @param baseName suffix を取り除いたベース名（例: "Username"）
 * @param fullName 分解元のフルネーム（例: "Username_02"）
 */
public record ParsedVirtualName(String baseName, String fullName) {
}
