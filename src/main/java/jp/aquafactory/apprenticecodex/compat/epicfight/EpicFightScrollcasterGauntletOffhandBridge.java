package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
        return hasMainhandGauntletAndEmptyOffhand(livingEntity.getMainHandItem(), livingEntity.getOffhandItem());
    }

    public static boolean shouldMirrorMainhand(Player player, InteractionHand hand) {
        if (hand != InteractionHand.OFF_HAND) {
            return false;
        }

        return hasMainhandGauntletAndEmptyOffhand(player.getMainHandItem(), player.getOffhandItem());
    }

    public static InteractionHand resolveSwingMagicHand(Player player, InteractionHand hand) {
        // Epic Fight のミラーOFF_HANDは実アイテムが空なので、詠唱元と選択スロットは実体のあるMAIN_HANDへ寄せる。
        return shouldMirrorMainhand(player, hand) ? InteractionHand.MAIN_HAND : hand;
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
        if (isOffhandVisualDisabledByMainhandItem(mainHandStack)
                || isOffhandVisualDisabledByMainhandCategory(mainHandStack)) {
            return ItemStack.EMPTY;
        }

        if (mainHandStack.getItem() instanceof ScrollcasterGauntlet && offhandStack.isEmpty()) {
            return mainHandStack;
        }

        if (offhandStack.getItem() instanceof ScrollcasterGauntlet && !entityPatch.isOffhandItemValid()) {
            return offhandStack;
        }

        return ItemStack.EMPTY;
    }

    private static boolean hasMainhandGauntletAndEmptyOffhand(ItemStack mainHandStack, ItemStack offhandStack) {
        return mainHandStack.getItem() instanceof ScrollcasterGauntlet && offhandStack.isEmpty();
    }

    private static boolean isOffhandVisualDisabledByMainhandItem(ItemStack mainHandStack) {
        var mainHandItemId = BuiltInRegistries.ITEM.getKey(mainHandStack.getItem());
        return mainHandItemId != null
                && ApprenticeCodexClientConfig.isScrollcasterGauntletOffhandVisualDisabledForMainhandItem(
                        mainHandItemId.toString()
                );
    }

    private static boolean isOffhandVisualDisabledByMainhandCategory(ItemStack mainHandStack) {
        var mainHandCapability = EpicFightCapabilities.getItemStackCapability(mainHandStack);
        if (mainHandCapability.isEmpty()) {
            return false;
        }

        var weaponCategory = mainHandCapability.getWeaponCategory();
        return weaponCategory != null
                && ApprenticeCodexClientConfig.isScrollcasterGauntletOffhandVisualDisabledForMainhandCategory(
                        weaponCategory.toString()
                );
    }

    public static float getMirroredAttackSpeed(LivingEntityPatch<?> entityPatch) {
        var livingEntity = (LivingEntity) entityPatch.getOriginal();
        var baseAttackSpeed = (float) livingEntity.getAttributeValue(Attributes.ATTACK_SPEED);
        return entityPatch.getModifiedAttackSpeedOfItem(
                entityPatch.getAdvancedHoldingItemCapability(InteractionHand.MAIN_HAND),
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
