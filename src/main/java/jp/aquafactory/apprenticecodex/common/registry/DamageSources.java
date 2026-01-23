package jp.aquafactory.apprenticecodex.common.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("removal")
public class DamageSources {
    public static final ResourceKey<DamageType> GENERAL_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(ApprenticeCodex.MODID, "codex_magic_general"));

    private DamageSources() {
        // do nothing.
    }

    public static DamageSource getGeneralDamageSource(Level level, Entity projectile, @Nullable Entity owner) {
        var reg = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);

        Holder<DamageType> holder = reg.getHolder(GENERAL_DAMAGE)
                .orElseGet(() -> (Holder.Reference<DamageType>) level.damageSources().genericKill().typeHolder());

        return new DamageSource(holder, owner, projectile);
    }
}
