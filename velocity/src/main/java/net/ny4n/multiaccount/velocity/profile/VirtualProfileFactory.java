package net.ny4n.multiaccount.velocity.profile;

import com.velocitypowered.api.util.GameProfile;

/**
 * 仮想プロフィール（Username_02等）のGameProfileを組み立てる。
 * UUIDはVelocity標準のオフラインアルゴリズム({@link GameProfile#forOfflinePlayer(String)})に
 * 委譲し、スキンは認証済みプロフィールが持つtextures propertyをそのまま複製する。
 * Mojangの署名はproperty値そのもの（profileId/profileNameを含むJSON）に対するものであり
 * 外側のUUID/名前との一致は検証されないため、UUIDが異なっていてもスキンは正しく表示される。
 */
public final class VirtualProfileFactory {

    private VirtualProfileFactory() {
    }

    public static GameProfile createVirtual(GameProfile effectiveProfile, String virtualName) {
        return GameProfile.forOfflinePlayer(virtualName)
                .withProperties(effectiveProfile.getProperties());
    }
}
