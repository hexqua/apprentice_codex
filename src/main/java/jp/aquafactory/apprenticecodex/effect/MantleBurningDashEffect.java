package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.effect.BurningDashEffect;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleElementalDash;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/** 通常詠唱のeffectとは別IDにし、解除と接触の所有者を外套へ限定する。 */
public final class MantleBurningDashEffect extends BurningDashEffect {
    public MantleBurningDashEffect() { super(MobEffectCategory.BENEFICIAL, 0xffaa55); }

    @Override
    public @NotNull String getDescriptionId() { return "spell.irons_spellbooks.burning_dash"; }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return true;
        if (!MantleElementalDash.canTick(entity, MantleElementalDash.FIRE)) return false;
        boolean remaining = super.applyEffectTick(entity, amplifier);
        if (!remaining && entity instanceof ServerPlayer player) ShootingStarMantleRuntime.state(player).elemental.contact(player);
        return remaining;
    }

    @Override
    public void onEffectRemoved(LivingEntity entity, int amplifier) {
        super.onEffectRemoved(entity, amplifier);
        MantleElementalDash.restoreSpin(entity);
    }
}
