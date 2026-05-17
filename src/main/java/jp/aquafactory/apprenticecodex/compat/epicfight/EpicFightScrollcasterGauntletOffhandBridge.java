package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

public final class EpicFightScrollcasterGauntletOffhandBridge {
    private EpicFightScrollcasterGauntletOffhandBridge() {
    }

    public static boolean shouldMirrorMainhand(LivingEntityPatch<?> entityPatch, InteractionHand hand) {
        if (hand != InteractionHand.OFF_HAND) {
            return false;
        }

        // 実アイテムがオフハンドにある場合は Epic Fight 本来の判定を優先する.
        var livingEntity = entityPatch.getOriginal();
        return livingEntity.getMainHandItem().getItem() instanceof ScrollcasterGauntlet
                && livingEntity.getOffhandItem().isEmpty();
    }

    public static CapabilityItem getMirroredCapability(LivingEntityPatch<?> entityPatch) {
        return EpicFightCapabilities.getItemStackCapability(getMirroredStack(entityPatch));
    }

    public static ItemStack getMirroredStack(LivingEntityPatch<?> entityPatch) {
        return entityPatch.getOriginal().getMainHandItem();
    }

    public static ItemStack getExtraRenderedOffhandStack(LivingEntityPatch<?> entityPatch) {
        var livingEntity = entityPatch.getOriginal();
        var mainHandStack = livingEntity.getMainHandItem();
        var offhandStack = livingEntity.getOffhandItem();
        if (mainHandStack.getItem() instanceof ScrollcasterGauntlet && offhandStack.isEmpty()) {
            return mainHandStack;
        }

        if (offhandStack.getItem() instanceof ScrollcasterGauntlet && !entityPatch.isOffhandItemValid()) {
            return offhandStack;
        }

        return ItemStack.EMPTY;
    }

    public static float getMirroredAttackSpeed(PlayerPatch<?> playerPatch) {
        var player = (Player) playerPatch.getOriginal();
        var baseAttackSpeed = (float) player.getAttributeValue(Attributes.ATTACK_SPEED);
        return playerPatch.getModifiedAttackSpeed(
                playerPatch.getAdvancedHoldingItemCapability(InteractionHand.MAIN_HAND),
                baseAttackSpeed
        );
    }

    public static EpicFightDamageSource getMirroredDamageSource(
            PlayerPatch<?> playerPatch,
            AnimationAccessor<? extends StaticAnimation> animation
    ) {
        return playerPatch.getDamageSource(animation, InteractionHand.MAIN_HAND);
    }

    public static AttackResult attackWithMirroredMainhand(
            PlayerPatch<?> playerPatch,
            EpicFightDamageSource damageSource,
            Entity target
    ) {
        return playerPatch.attack(damageSource, target, InteractionHand.MAIN_HAND);
    }
}
