package jp.aquafactory.apprenticecodex.compat.bettercombat;

import jp.aquafactory.apprenticecodex.event.client.ClientSwingMagicAttackTrigger;
import jp.aquafactory.apprenticecodex.event.client.ClientMultipurposeStaffrifleInputEvent;
import jp.aquafactory.apprenticecodex.event.client.MultipurposeStaffrifleClientAdsState;
import jp.aquafactory.apprenticecodex.item.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.item.MultipurposeStaffrifle;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.client.BetterCombatClientEvents;
import net.bettercombat.client.animation.AnimationRegistry;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.logic.AnimatedHand;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

import java.util.List;

public final class BetterCombatClientCompat {
    public static final String MOD_ID = "bettercombat";
    private static final String STAFFRIFLE_SHOOT_ANIMATION = "apprenticecodex:staffrifle_shoot";
    private static final float STAFFRIFLE_SHOOT_DURATION_TICKS = 9.0F;
    private static final float STAFFRIFLE_SHOOT_UPSWING = 0.5F;

    private static boolean registered;

    private BetterCombatClientCompat() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        // Better Combat 側で実際に攻撃判定を出す瞬間へ合わせ、演出や当たり判定とのズレを避ける。
        BetterCombatClientEvents.ATTACK_HIT.register(BetterCombatClientCompat::onAttackHit);
        registered = true;
    }

    public static boolean usesBetterCombatAttackTiming(LocalPlayer player) {
        if (player == null) {
            return false;
        }

        var attributes = WeaponRegistry.getAttributes(player.getMainHandItem());
        return hasAttackSequence(attributes);
    }

    public static void playStaffrifleShootAnimation(Entity entity) {
        var animationLoaded = AnimationRegistry.animations.containsKey(STAFFRIFLE_SHOOT_ANIMATION);
        if (!(entity instanceof PlayerAttackAnimatable animatable) || !animationLoaded) {
            return;
        }

        animatable.playAttackAnimation(
                STAFFRIFLE_SHOOT_ANIMATION,
                AnimatedHand.TWO_HANDED,
                STAFFRIFLE_SHOOT_DURATION_TICKS,
                STAFFRIFLE_SHOOT_UPSWING
        );
    }

    private static void onAttackHit(LocalPlayer player, AttackHand attackHand, List<Entity> targets, Entity cursorTarget) {
        var minecraft = Minecraft.getInstance();
        if (player != minecraft.player) {
            return;
        }

        var hand = resolveHand(attackHand);
        if (!attackHand.isOffHand()
                && player.getMainHandItem().getItem() instanceof MultipurposeStaffrifle
                && !MultipurposeStaffrifleClientAdsState.isLocalAdsKeyHeld(player)) {
            ClientMultipurposeStaffrifleInputEvent.trySendNonAdsSpecialCast(minecraft);
        }
        if (CrystalBladedStaff.isCrystalBladedStaff(player.getItemInHand(hand))) {
            if (targets.isEmpty()) {
                ClientSwingMagicAttackTrigger.trySendForBetterCombat(minecraft, hand);
            }
            return;
        }

        ClientSwingMagicAttackTrigger.trySendForBetterCombat(minecraft, hand);
    }

    private static InteractionHand resolveHand(AttackHand attackHand) {
        return attackHand.isOffHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private static boolean hasAttackSequence(WeaponAttributes attributes) {
        return attributes != null && attributes.attacks() != null;
    }
}
