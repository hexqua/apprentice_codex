package jp.aquafactory.apprenticecodex.spell.divinepossession;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.item.zenithstaff.ZenithStaffPowerHelper;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.world.entity.LivingEntity;

public final class DivinePossessionPowerHelper {
    private DivinePossessionPowerHelper() {
    }

    public static double resolveSchoolPower(SchoolType requestedSchool, LivingEntity caster) {
        var requestedPower = requestedSchool.getPowerFor(caster);
        if (!caster.hasEffect(EffectRegistry.DIVINE_POSSESSION)) {
            return ZenithStaffPowerHelper.resolveSchoolPower(requestedSchool, caster, requestedPower);
        }

        var maxPower = requestedPower;
        for (var school : SchoolRegistry.REGISTRY) {
            if (school != null) {
                maxPower = Math.max(maxPower, school.getPowerFor(caster));
            }
        }
        return maxPower;
    }
}
