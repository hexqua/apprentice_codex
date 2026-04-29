package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RecastInstance.class, remap = false)
public interface RecastInstanceAccessor {
    @Accessor("remainingTicks")
    void apprenticecodex$setRemainingTicks(int remainingTicks);
}
