package jp.aquafactory.apprenticecodex.item.zenithstaff;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.item.zenithstaff.ZenithStaff;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ZenithStaffPowerHelper {
    private static final double BASE_POWER = 1.0D;
    private static final double EPSILON = 1.0e-9D;

    private ZenithStaffPowerHelper() {
    }

    public static double resolveSchoolPower(SchoolType requestedSchool, LivingEntity caster, double requestedPower) {
        if (requestedSchool == null || caster == null || !isActive(caster)) {
            return requestedPower;
        }

        var snapshot = resolvePowerSnapshot(caster);
        if (!snapshot.hasSchoolBonus()) {
            return requestedPower;
        }
        return Math.max(requestedPower, snapshot.maxPower());
    }

    public static boolean shouldIncreaseManaCost(@Nullable LivingEntity caster, @Nullable SchoolType requestedSchool) {
        if (caster == null || requestedSchool == null || !isActive(caster)) {
            return false;
        }

        var snapshot = resolvePowerSnapshot(caster);
        return snapshot.hasSchoolBonus() && !snapshot.isStrongest(requestedSchool);
    }

    public static PowerSnapshot resolvePowerSnapshot(@Nullable LivingEntity caster) {
        if (caster == null) {
            return PowerSnapshot.empty();
        }

        var strongestSchools = new ArrayList<SchoolType>();
        var maxPower = BASE_POWER;
        for (var school : SchoolRegistry.REGISTRY) {
            if (school == null) {
                continue;
            }

            var power = school.getPowerFor(caster);
            if (power > maxPower + EPSILON) {
                maxPower = power;
                strongestSchools.clear();
                strongestSchools.add(school);
            } else if (Math.abs(power - maxPower) <= EPSILON && power > BASE_POWER + EPSILON) {
                strongestSchools.add(school);
            }
        }

        if (maxPower <= BASE_POWER + EPSILON || strongestSchools.isEmpty()) {
            return PowerSnapshot.empty();
        }
        return new PowerSnapshot(maxPower, List.copyOf(strongestSchools));
    }

    public static boolean isHeldBy(@Nullable LivingEntity entity) {
        return entity != null && (isZenithStaff(entity.getMainHandItem()) || isZenithStaff(entity.getOffhandItem()));
    }

    private static boolean isActive(LivingEntity caster) {
        return isHeldBy(caster) && !caster.hasEffect(EffectRegistry.DIVINE_POSSESSION);
    }

    private static boolean isZenithStaff(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ZenithStaff;
    }

    public record PowerSnapshot(double maxPower, List<SchoolType> strongestSchools) {
        private static PowerSnapshot empty() {
            return new PowerSnapshot(BASE_POWER, List.of());
        }

        public boolean hasSchoolBonus() {
            return maxPower > BASE_POWER + EPSILON && !strongestSchools.isEmpty();
        }

        public boolean isStrongest(SchoolType schoolType) {
            return strongestSchools.stream().anyMatch(school -> school == schoolType || school.getId().equals(schoolType.getId()));
        }

        public int bonusPercent() {
            return (int) Math.round((maxPower - BASE_POWER) * 100.0D);
        }
    }
}
