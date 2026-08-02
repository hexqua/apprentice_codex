package jp.aquafactory.apprenticecodex.block.spelldispenser;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Spell Dispenser のローカルな発動座標を、実際に魔法を実行する座標系へ投影する境界。
 */
public final class SpellDispenserCastFrameBridge {
    private static final Projector IDENTITY_PROJECTOR = (level, castBasePosition, frame) -> frame;
    private static Projector projector = IDENTITY_PROJECTOR;

    private SpellDispenserCastFrameBridge() {
    }

    public static CastFrame project(ServerLevel level, Vec3 castBasePosition, CastFrame frame) {
        return Objects.requireNonNull(projector.project(level, castBasePosition, frame));
    }

    public static void registerProjector(Projector newProjector) {
        projector = Objects.requireNonNull(newProjector);
    }

    public static ProjectorOverride useProjectorForGameTest(Projector testProjector) {
        var previous = projector;
        registerProjector(testProjector);
        return () -> projector = previous;
    }

    public record CastFrame(Vec3 origin, Vec3 forward) {
        public CastFrame {
            Objects.requireNonNull(origin);
            Objects.requireNonNull(forward);
        }
    }

    @FunctionalInterface
    public interface Projector {
        @NotNull CastFrame project(ServerLevel level, Vec3 castBasePosition, CastFrame frame);
    }

    @FunctionalInterface
    public interface ProjectorOverride extends AutoCloseable {
        @Override
        void close();
    }
}
