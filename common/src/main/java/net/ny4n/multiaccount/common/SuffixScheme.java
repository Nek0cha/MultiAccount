package net.ny4n.multiaccount.common;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 複製プロフィール名の生成・解析ロジック。Velocity側とPaper側の双方から
 * 同一アルゴリズムで参照できるように common に置く。
 */
public final class SuffixScheme {

    private SuffixScheme() {
    }

    /**
     * Mojangのオフラインモードと同じアルゴリズムでUUIDを決定的に生成する。
     * Velocity側では {@code GameProfile.forOfflinePlayer(name)} が同等の処理を
     * 行うため、Paper側での再検証用にこちらを利用する。
     */
    public static UUID offlineUuid(String name) {
        byte[] bytes = ("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8);
        return UUID.nameUUIDFromBytes(bytes);
    }

    /**
     * {@code suffix-format}（例: "_%02d"）にスロット番号を埋め込んだ複製名を組み立てる。
     */
    public static String buildVirtualName(String baseName, String suffixFormat, int slot) {
        return baseName + String.format(suffixFormat, slot);
    }

    /**
     * suffix-regex に一致するフルネームを baseName / fullName に分解する。
     * 一致しない場合（=複製プロフィールではない）は空を返す。
     */
    public static Optional<ParsedVirtualName> parse(String fullName, Pattern suffixRegex) {
        Matcher matcher = suffixRegex.matcher(fullName);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String baseName = fullName.substring(0, matcher.start());
        if (baseName.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ParsedVirtualName(baseName, fullName));
    }
}
