package jp.aquafactory.apprenticecodex.compat.epicfight;

import net.minecraft.world.item.UseAnim;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.RangedWeaponCapability;
import yesman.epicfight.world.capabilities.item.Style;

import java.util.List;

public final class EpicFightSpellgunCapability extends RangedWeaponCapability {
    public EpicFightSpellgunCapability(RangedWeaponCapability.Builder builder) {
        super(builder);
    }

    @Override
    public Style getStyle(LivingEntityPatch<?> entityPatch) {
        return CapabilityItem.Styles.ONE_HAND;
    }

    @Override
    public boolean canBePlacedOffhand() {
        return true;
    }

    @Override
    public List<AnimationManager.AnimationAccessor<? extends AttackAnimation>> getAutoAttackMotion(
            PlayerPatch<?> playerPatch
    ) {
        // ComboAttacks は COMBO_ATTACK 発火後に null を受け取るとモーション選択前に終了するため、
        // オフハンド同様に Iron's の詠唱表示だけを残す。
        return null;
    }

    @Override
    public UseAnim getUseAnimation(LivingEntityPatch<?> entityPatch) {
        return UseAnim.NONE;
    }
}
