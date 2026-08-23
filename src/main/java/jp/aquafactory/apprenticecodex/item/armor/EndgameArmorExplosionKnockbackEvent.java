package jp.aquafactory.apprenticecodex.item.armor;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;

/** vanilla属性の下限0では表現できない、負の爆発ノックバック耐性だけを補正する。 */
@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class EndgameArmorExplosionKnockbackEvent {
    private EndgameArmorExplosionKnockbackEvent() {
    }

    @SubscribeEvent
    public static void onExplosionKnockback(ExplosionKnockbackEvent event) {
        if (!(event.getAffectedEntity() instanceof LivingEntity livingEntity)
                || !EndgameArmorCalibration.hasWindAccumulationWeave(livingEntity)) {
            return;
        }

        var attribute = livingEntity.getAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
        if (attribute == null) {
            return;
        }
        var unclampedResistance = calculateUnclampedValue(attribute);
        if (unclampedResistance < 0.0D) {
            // vanillaは属性値を0へ丸めた後なので、失われた負値分を速度へ戻す。
            event.setKnockbackVelocity(event.getKnockbackVelocity().scale(1.0D - unclampedResistance));
        }
    }

    public static double calculateUnclampedValue(AttributeInstance attribute) {
        var baseValue = attribute.getBaseValue();
        var value = baseValue;
        for (var modifier : attribute.getModifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                value += modifier.amount();
            }
        }

        var valueAfterAddition = value;
        for (var modifier : attribute.getModifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                value += valueAfterAddition * modifier.amount();
            }
        }
        for (var modifier : attribute.getModifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                value *= 1.0D + modifier.amount();
            }
        }
        return value;
    }
}
