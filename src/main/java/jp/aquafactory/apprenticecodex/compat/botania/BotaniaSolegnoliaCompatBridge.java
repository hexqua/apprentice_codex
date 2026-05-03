package jp.aquafactory.apprenticecodex.compat.botania;

import net.minecraft.world.entity.Entity;

public final class BotaniaSolegnoliaCompatBridge {
    private BotaniaSolegnoliaCompatBridge() {
    }

    public static boolean preventsAutoMagnetItemCollection(Entity owner, Entity item) {
        // Botania 1.21.1 API が未提供のため、依存を置かず現時点では通常回収を維持する。
        return false;
    }
}
