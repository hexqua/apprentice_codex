package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.entity.mobs.frozen_humanoid.FrozenHumanoid;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = FrozenHumanoid.class, remap = false)
public interface FrozenHumanoidShatterInvoker {
    @Invoker("doPuffDamage")
    void apprenticecodex$doPuffDamage();

    @Invoker("spawnIcicleShards")
    void apprenticecodex$spawnIcicleShards(Vec3 origin, float damage);
}
