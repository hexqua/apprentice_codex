package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import net.minecraft.core.registries.BuiltInRegistries;
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

    public static float getMirroredAttackSpeed(PlayerPatch<?> playerPatch) {
        var player = (Player) playerPatch.getOriginal();
        var baseAttackSpeed = (float) player.getAttributeValue(Attributes.ATTACK_SPEED);
        return playerPatch.getModifiedAttackSpeedOfItem(
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
