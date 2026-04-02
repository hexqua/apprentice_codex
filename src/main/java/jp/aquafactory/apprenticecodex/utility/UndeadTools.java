package jp.aquafactory.apprenticecodex.utility;

import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;

public final class UndeadTools {
    private UndeadTools() {
    }

    public static boolean isUndead(LivingEntity entity) {
        // 1.20.1 では種族判定が主流なので最優先にし、回復反転と独自タグで取りこぼしを補完する。
        if (entity.getMobType() == MobType.UNDEAD) {
            return true;
        }
        if (entity.isInvertedHealAndHarm()) {
            return true;
        }
        return entity.getType().is(TagRegistry.EntityTypes.COUNTS_AS_UNDEAD);
    }

    public static boolean isUndead(ServerLevel level, EntityType<?> entityType) {
        var previewEntity = entityType.create(level);
        return previewEntity instanceof LivingEntity livingEntity && isUndead(livingEntity);
    }
}
