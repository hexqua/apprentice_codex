package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SpellExtractionHelper {
    private SpellExtractionHelper() {
    }

    public static @NotNull Evaluation evaluate(@NotNull ItemStack targetStack) {
        if (targetStack.isEmpty()) {
            return Evaluation.notApplicable();
        }
        if (targetStack.getItem() instanceof UniqueItem) {
            return Evaluation.blocked(BlockReason.UNIQUE_ITEM);
        }
        if (!ISpellContainer.isSpellContainer(targetStack)) {
            return Evaluation.notApplicable();
        }
        if (SpellCalibrationImbueHelper.canExtractAnyScroll(targetStack)) {
            return Evaluation.blocked(BlockReason.CALIBRATION_EXTRACTABLE);
        }

        var spellContainer = ISpellContainer.get(targetStack);
        if (spellContainer == null
                || spellContainer.getMaxSpellCount() != 1
                || spellContainer.getActiveSpellCount() != 1) {
            return Evaluation.blocked(BlockReason.NOT_TARGET);
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        // Iron's 側の @NotNull 契約に反して null が返る場合も、破壊処理では安全側へ倒す。
        if (spellData == null || spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return Evaluation.blocked(BlockReason.NOT_TARGET);
        }
        if (spellData.isLocked()
                && (!spellData.getSpell().allowLooting() || !targetStack.is(TagRegistry.Items.SPELL_DISMANTLEABLE))) {
            return Evaluation.blocked(BlockReason.LOCKED);
        }

        var scrollStack = SpellCalibrationImbueHelper.createScroll(spellData);
        return scrollStack.isEmpty()
                ? Evaluation.blocked(BlockReason.NOT_TARGET)
                : Evaluation.success(scrollStack);
    }

    public enum BlockReason {
        UNIQUE_ITEM("ui.apprenticecodex.spellcaster_workbench.cant_extract_unique_item"),
        CALIBRATION_EXTRACTABLE("ui.apprenticecodex.spellcaster_workbench.cant_extract_extractable"),
        LOCKED("ui.apprenticecodex.spellcaster_workbench.cant_extract_locked"),
        NOT_TARGET("ui.apprenticecodex.spellcaster_workbench.cant_extract_not_target");

        private final String translationKey;

        BlockReason(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    public record Evaluation(@NotNull ItemStack resultStack, @Nullable BlockReason blockReason, boolean applicable) {
        private static @NotNull Evaluation success(@NotNull ItemStack resultStack) {
            return new Evaluation(resultStack, null, true);
        }

        private static @NotNull Evaluation blocked(@NotNull BlockReason blockReason) {
            return new Evaluation(ItemStack.EMPTY, blockReason, true);
        }

        private static @NotNull Evaluation notApplicable() {
            return new Evaluation(ItemStack.EMPTY, null, false);
        }

        public boolean isSuccess() {
            return !resultStack.isEmpty();
        }
    }
}
