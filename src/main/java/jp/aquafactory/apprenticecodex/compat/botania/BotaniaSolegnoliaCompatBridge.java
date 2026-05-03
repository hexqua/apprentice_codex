package jp.aquafactory.apprenticecodex.compat.botania;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

public final class BotaniaSolegnoliaCompatBridge {
    private static final String BOTANIA_MOD_ID = "botania";

    private BotaniaSolegnoliaCompatBridge() {
    }

    public static boolean preventsAutoMagnetItemCollection(Entity owner, Entity item) {
        if (!ModList.get().isLoaded(BOTANIA_MOD_ID)) {
            return false;
        }

        return BotaniaSolegnoliaCompat.hasSolegnoliaAround(owner)
                || BotaniaSolegnoliaCompat.hasSolegnoliaAround(item);
    }
}
