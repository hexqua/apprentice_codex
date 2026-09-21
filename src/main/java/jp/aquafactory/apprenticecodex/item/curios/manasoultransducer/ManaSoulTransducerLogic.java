package jp.aquafactory.apprenticecodex.item.curios.manasoultransducer;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.world.entity.LivingEntity;

public final class ManaSoulTransducerLogic {
    private ManaSoulTransducerLogic() {}
    public static double durationModifier(double attribute, double transferRate) {
        return -transferRate * Math.max(0D, Utils.softCapFormula(Math.max(1D, attribute)) - 1D);
    }
    public static int chargeTicks(float base, double attribute, double rate) {
        return Math.max(1, (int) Math.ceil(base * (1 + durationModifier(attribute, rate))));
    }
    public static float chargeDuration(float base, LivingEntity entity) {
        if (!ManaSoulTransducerEvents.isEquipped(entity)) return base;
        double rate = entity.level().isClientSide ? ManaSoulTransducerConfigState.castRate()
                : ApprenticeCodexServerConfig.manaSoulTransducerCastRate();
        return chargeTicks(base, entity.getAttributeValue(AttributeRegistry.CAST_TIME_REDUCTION.get()), rate);
    }
}
