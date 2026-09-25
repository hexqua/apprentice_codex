package jp.aquafactory.apprenticecodex.spell.lockonray;

import io.redspace.ironsspellbooks.api.spells.ICastData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class LockOnRayCastData implements ICastData {
    private UUID targetId;
    private final ResourceKey<Level> dimension;
    private int elapsedTicks;

    public LockOnRayCastData(Entity target) {
        targetId = target.getUUID();
        dimension = target.level().dimension();
    }

    public Entity resolve(ServerLevel level) {
        if (targetId == null || !dimension.equals(level.dimension())) return null;
        var target = level.getEntity(targetId);
        return isLoadedTarget(level, target) ? target : null;
    }

    public static boolean isLoadedTarget(ServerLevel level, Entity target) {
        // Entity参照が残っていても、アンロード済みのチャンクを再ロードしない。
        return target != null && target.isAlive() && !target.isRemoved() && target.level() == level
                && level.hasChunkAt(target.blockPosition());
    }

    public boolean tickAndShouldFire() {
        return ++elapsedTicks >= 20 && (elapsedTicks - 20) % 4 == 0;
    }

    @Override public void reset() { targetId = null; }
}
