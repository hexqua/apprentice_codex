package jp.aquafactory.apprenticecodex.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityDamageMemoryAccessor {
    @Accessor("lastHurt") float apprenticecodex$getLastHurt();
    @Accessor("lastHurt") void apprenticecodex$setLastHurt(float amount);
}
