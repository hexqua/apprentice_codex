package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sammy.malum.common.worldevent.DelayedDamageWorldEvent;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Pseudo
@Mixin(targets = "com.sammy.malum.common.worldevent.DelayedDamageWorldEvent", remap = false)
public abstract class MalumScytheDelayedDamageMixin {
    @Unique private ScytheThrowEntity apprenticecodex$origin;

    @Inject(method = "setAttacker(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Lcom/sammy/malum/common/worldevent/DelayedDamageWorldEvent;", at = @At("RETURN"))
    private void apprenticecodex$capture(Entity attacker, Entity projectile, CallbackInfoReturnable<DelayedDamageWorldEvent> cir) {
        apprenticecodex$origin = projectile instanceof ScytheThrowEntity scythe ? scythe : null;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getEntity(Ljava/util/UUID;)Lnet/minecraft/world/entity/Entity;"))
    private Entity apprenticecodex$retainOrigin(ServerLevel level, UUID id, Operation<Entity> original) {
        // 即時回収済みでも遅延斬撃の保持武器を失わない。短命なworld eventにだけ参照を保持する。
        if (apprenticecodex$origin != null && apprenticecodex$origin.getUUID().equals(id)) return apprenticecodex$origin;
        return original.call(level, id);
    }
}
