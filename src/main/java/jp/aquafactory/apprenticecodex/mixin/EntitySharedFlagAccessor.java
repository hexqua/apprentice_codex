package jp.aquafactory.apprenticecodex.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntitySharedFlagAccessor {
    @Invoker("getSharedFlag")
    boolean apprenticecodex$invokeGetSharedFlag(int flag);

    @Invoker("setSharedFlag")
    void apprenticecodex$invokeSetSharedFlag(int flag, boolean value);
}
