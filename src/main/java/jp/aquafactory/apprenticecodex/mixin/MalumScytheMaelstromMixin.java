package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sammy.malum.common.entity.scythe.AbstractScytheProjectileEntity;
import jp.aquafactory.apprenticecodex.compat.malum.MalumScytheMaelstromCompat;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.Predicate;

@Pseudo
@Mixin(targets = "com.sammy.malum.common.item.curiosities.curios.sets.scythe.CurioHowlingMaelstromRing", remap = false)
public abstract class MalumScytheMaelstromMixin {
    @WrapOperation(method = "handleMaelstrom", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private static List<Entity> apprenticecodex$filterStorm(ServerLevel level, Entity excluded, AABB bounds,
            Predicate<? super Entity> predicate, Operation<List<Entity>> original,
            ServerLevel serverLevel, LivingEntity owner, AbstractScytheProjectileEntity storm) {
        if (storm.getPersistentData().getBoolean(MalumScytheMaelstromCompat.ORIGIN_MARKER)) {
            return original.call(level, excluded, bounds, (Predicate<Entity>) entity -> predicate.test(entity)
                    && CombatTools.isValidCombatTarget(entity, owner));
        }
        return original.call(level, excluded, bounds, predicate);
    }
}
