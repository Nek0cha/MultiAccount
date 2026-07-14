package net.ny4n.multiaccount.velocity.session;

import java.util.UUID;

/**
 * {@link DuplicateSessionManager#tryAllocate} が返すスロット割当結果。
 *
 * @param slot        1始まりのスロット番号（1 = 本人枠、無改変）
 * @param virtualUuid slot==1 の場合はベースUUIDそのもの、それ以外はオフライン方式UUID
 * @param virtualName slot==1 の場合はベース名そのもの、それ以外は "Base_02" 等
 */
public record SessionAllocation(int slot, UUID virtualUuid, String virtualName) {

    public boolean isPrimary() {
        return slot == 1;
    }
}
