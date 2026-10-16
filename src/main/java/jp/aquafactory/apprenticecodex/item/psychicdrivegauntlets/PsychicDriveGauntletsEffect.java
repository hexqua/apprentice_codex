package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record PsychicDriveGauntletsEffect(boolean right, Kind kind, Vec3 position, Vec3 direction, float charge) {
    public enum Kind {
        CHARGE_START, CHARGE_READY, LAUNCH, IMPACT, BLOCK_IMPACT, DRILL_START, DRILL_TICK, FINISH, REJECTED;

        public boolean delayed() {
            return switch (this) {
                case IMPACT, BLOCK_IMPACT, DRILL_START, DRILL_TICK, FINISH, REJECTED -> true;
                default -> false;
            };
        }
    }

    public @Nullable SoundEvent sound() {
        return switch (kind) {
            case CHARGE_START -> SoundRegistry.PSYCHIC_DRIVE_GAUNTLETS_CHARGE_START.get();
            case CHARGE_READY -> SoundRegistry.PSYCHIC_DRIVE_GAUNTLETS_CHARGE_READY.get();
            case LAUNCH -> charge >= 1 ? SoundRegistry.PSYCHIC_DRIVE_GAUNTLETS_FULL_LAUNCH.get()
                    : SoundRegistry.PSYCHIC_DRIVE_GAUNTLETS_LAUNCH.get();
            case IMPACT -> SoundRegistry.PSYCHIC_DRIVE_GAUNTLETS_IMPACT.get();
            case BLOCK_IMPACT -> charge >= 1 ? SoundRegistry.PSYCHIC_DRIVE_GAUNTLETS_FULL_IMPACT.get()
                    : SoundRegistry.PSYCHIC_DRIVE_GAUNTLETS_IMPACT.get();
            case DRILL_START -> SoundRegistry.PSYCHIC_DRIVE_GAUNTLETS_FULL_IMPACT.get();
            case FINISH -> SoundRegistry.PSYCHIC_DRIVE_GAUNTLETS_FINISH.get();
            case DRILL_TICK, REJECTED -> null;
        };
    }
}
