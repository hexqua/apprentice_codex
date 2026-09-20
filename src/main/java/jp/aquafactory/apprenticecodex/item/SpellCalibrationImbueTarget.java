package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface SpellCalibrationImbueTarget {
    // 受付には対象Stackを渡さず、調整による使用可否が挿入条件へ混ざることを防ぐ。
    boolean acceptsCalibrationSpell(@NotNull SpellData spellData);

    boolean isCalibrationSlotAvailable(@NotNull ItemStack targetStack, int slot);

    default boolean isCalibrationSlotAvailable(@NotNull ItemStack targetStack, int slot,
                                               @NotNull HolderLookup.Provider lookupProvider) {
        return isCalibrationSlotAvailable(targetStack, slot);
    }

    default boolean isCalibrationSpellUsable(@NotNull ItemStack targetStack, @NotNull SpellData spellData) {
        return true;
    }

    default boolean isCalibrationSpellUsable(@NotNull ItemStack targetStack, @NotNull SpellData spellData,
                                             @NotNull HolderLookup.Provider lookupProvider) {
        return isCalibrationSpellUsable(targetStack, spellData);
    }

    default @NotNull SpellCalibrationImbueState evaluateCalibrationImbue(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull SpellData spellData
    );
}
