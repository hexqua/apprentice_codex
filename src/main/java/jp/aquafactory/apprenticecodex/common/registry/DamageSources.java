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

@SuppressWarnings("removal")
public class DamageSources {
    private DamageSources() {
        // do nothing.
    }

    public static DamageSource getDamageSource(Level level, Entity projectile, Entity owner, String typeName) {
        var reg = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        var type = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(ApprenticeCodex.MODID, typeName));
        var holder = reg.getHolder(type)
                .orElseGet(() -> (Holder.Reference<DamageType>) level.damageSources().genericKill().typeHolder());

        return new DamageSource(holder, projectile, owner);
    }
}
