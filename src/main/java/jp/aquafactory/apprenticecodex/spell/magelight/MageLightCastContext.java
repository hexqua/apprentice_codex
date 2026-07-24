package jp.aquafactory.apprenticecodex.spell.magelight;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BooleanSupplier;

public final class MageLightCastContext {
    private static final ThreadLocal<Entry> CURRENT = new ThreadLocal<>();

    private MageLightCastContext() {
    }

    public static boolean withTarget(LivingEntity caster, MageLight.CastTarget target, BooleanSupplier action) {
        var previous = CURRENT.get();
        CURRENT.set(new Entry(caster.getUUID(), target));
        try {
            return action.getAsBoolean();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    @Nullable
    static MageLight.CastTarget targetFor(LivingEntity caster) {
        var entry = CURRENT.get();
        return entry != null && entry.casterId.equals(caster.getUUID()) ? entry.target : null;
    }

    private record Entry(UUID casterId, MageLight.CastTarget target) {
    }
}
