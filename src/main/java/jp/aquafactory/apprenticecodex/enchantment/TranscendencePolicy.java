package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.world.item.Item;

/**
 * Transcendence の付与面とレベル適用経路をアイテム側で宣言するためのポリシー。
 */
public interface TranscendencePolicy {
    default Handling transcendenceHandling() {
        return Handling.EVENT;
    }

    default boolean isTranscendenceActiveWhileHeld() {
        return true;
    }

    default boolean supportsDirectTranscendenceApplication() {
        return transcendenceHandling() != Handling.DISABLED;
    }

    static boolean supportsDirectApplication(Item item) {
        return item instanceof TranscendencePolicy policy
                && policy.supportsDirectTranscendenceApplication();
    }

    enum Handling {
        EVENT,
        INTERNAL,
        DISABLED
    }
}
