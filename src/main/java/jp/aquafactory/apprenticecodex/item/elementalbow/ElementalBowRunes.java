package jp.aquafactory.apprenticecodex.item.elementalbow;

import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ElementalBowRunes {
    private ElementalBowRunes() {}

    @Nullable
    public static SchoolType school(ItemStack bow) {
        if (!(bow.getItem() instanceof ElementalBow)) return null;
        for (int slot = 0; slot < 3; slot++) {
            var school = ScrollcasterSchoolRuneResolver.resolveSchool(
                    CalibrationAdjustmentStorage.get(bow, slot, 3, ElementalBow.serializationLookup()));
            if (school.isPresent()) return school.get();
        }
        return null;
    }

    public static boolean separatesOverheat(ItemStack bow) {
        if (!(bow.getItem() instanceof ElementalBow)) return false;
        for (int slot = 0; slot < 3; slot++) {
            if (CalibrationAdjustmentStorage.get(bow, slot, 3, ElementalBow.serializationLookup())
                    .is(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get())) return true;
        }
        return false;
    }

    public static double configuredManaMultiplier(boolean clientSide) {
        return clientSide ? ElementalBowClientConfigState.schoolRuneManaCostMultiplier()
                : ApprenticeCodexServerConfig.elementalBowSchoolRuneManaCostMultiplier();
    }

    public static double manaMultiplier(ItemStack bow, Player player) {
        return school(bow) == null ? 1.0D : configuredManaMultiplier(player.level().isClientSide);
    }

    public static int scaleMana(int mana, double multiplier) {
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(Math.max(0, mana) * multiplier));
    }

    public static int baseManaCost(ItemStack bow, Player player, int mana) {
        return scaleMana(mana, manaMultiplier(bow, player));
    }
}
