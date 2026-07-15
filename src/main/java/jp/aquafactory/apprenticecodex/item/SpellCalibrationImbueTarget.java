package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface SpellCalibrationImbueTarget {
    @NotNull
    SpellCalibrationImbueState evaluateCalibrationImbue(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull SpellData spellData
    );

    default @NotNull SpellCalibrationImbueState evaluateCalibrationImbue(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull SpellData spellData,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        return evaluateCalibrationImbue(targetStack, slot, spellData);
    }
}
