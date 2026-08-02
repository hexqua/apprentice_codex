package jp.aquafactory.apprenticecodex.compat.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastFrameBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("unused")
public final class SpellDispenserSableCompat {
    private SpellDispenserSableCompat() {
    }

    public static void register() {
        SpellDispenserCastFrameBridge.registerProjector(SpellDispenserSableCompat::projectCastFrame);
    }

    private static SpellDispenserCastFrameBridge.CastFrame projectCastFrame(
            ServerLevel level,
            Vec3 castBasePosition,
            SpellDispenserCastFrameBridge.CastFrame frame
    ) {
        var subLevel = SableCompanion.INSTANCE.getContaining(level, castBasePosition);
        if (subLevel == null) {
            return frame;
        }

        var pose = subLevel.logicalPose();
        return new SpellDispenserCastFrameBridge.CastFrame(
                pose.transformPosition(frame.origin()),
                pose.transformNormal(frame.forward()).normalize()
        );
    }
}
