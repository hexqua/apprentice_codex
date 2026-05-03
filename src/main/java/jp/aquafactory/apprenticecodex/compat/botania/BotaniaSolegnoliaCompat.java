package jp.aquafactory.apprenticecodex.compat.botania;

import net.minecraft.world.entity.Entity;
import vazkii.botania.api.BotaniaAPI;

final class BotaniaSolegnoliaCompat {
    private BotaniaSolegnoliaCompat() {
    }

    static boolean hasSolegnoliaAround(Entity entity) {
        // Botania 1.20.1 専用 API。1.21.1 へ forward-port する場合は依存を置かず false 固定にする。
        return BotaniaAPI.instance().hasSolegnoliaAround(entity);
    }
}
