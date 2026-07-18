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
    public EpicFightSpellgunCapability(CapabilityItem.Builder builder) {
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
        // 20.14.17 の BasicAttack は BASIC_ATTACK_EVENT 発火後に null を受け取ると、攻撃成功の入力処理を
        // 巻き戻さずモーション選択前に終了する。オフハンド同様に Iron's の詠唱表示だけを残す。
        // 1.21.1 側では null 時のイベント発火順と入力成功判定を再確認する。
        return null;
    }

    @Override
    public UseAnim getUseAnimation(LivingEntityPatch<?> entityPatch) {
        return UseAnim.NONE;
    }
}
