package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatsword;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = Collider.class, remap = false)
public abstract class EpicFightColliderMixin {
    @Inject(method = "updateAndSelectCollideEntity", at = @At("RETURN"), cancellable = true)
    private void apprenticecodex$extendSpellchargedGreatswordOverchargeYHitbox(
            LivingEntityPatch<?> entityPatch,
            AttackAnimation attackAnimation,
            float prevElapsedTime,
            float elapsedTime,
            Joint joint,
            float attackSpeed,
            CallbackInfoReturnable<List<Entity>> callback
    ) {
        var owner = entityPatch.getOriginal();
        if (!(owner instanceof Player player)
                || !SpellchargedGreatsword.isOverchargeActive(player.getMainHandItem())) {
            return;
        }

        var originalTargets = callback.getReturnValue();
        var expandedTargets = new ArrayList<>(originalTargets);
        var reach = Math.max(1.0D, entityPatch.getReach(InteractionHand.MAIN_HAND));
        var horizontalReachSqr = (reach + SpellchargedGreatsword.ENTITY_REACH_BONUS);
        horizontalReachSqr *= horizontalReachSqr;
        var verticalPadding = Math.max(0.25D, player.getBbHeight() * 0.25D);
        var candidateBox = player.getBoundingBox().inflate(reach, verticalPadding, reach);
        var eye = player.getEyePosition();
        for (var rawTarget : player.level().getEntities(player, candidateBox)) {
            var target = CombatTools.resolutePartEntity(rawTarget);
            if (!CombatTools.isValidCombatTarget(target, player) || expandedTargets.contains(target)) {
                continue;
            }

            var targetCenter = target.getBoundingBox().getCenter();
            var horizontalDelta = new Vec3(targetCenter.x - eye.x, 0.0D, targetCenter.z - eye.z);
            if (horizontalDelta.lengthSqr() <= horizontalReachSqr) {
                // Epic Fight 側の元 collider はそのまま使い、overcharge 中だけY方向に拾える候補を少し増やす。
                expandedTargets.add(target);
            }
        }
        callback.setReturnValue(expandedTargets);
    }
}
