package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.item.ArcaneAnvilImbueBlockItem;
import jp.aquafactory.apprenticecodex.item.AbstractImbueShieldItem;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitItem;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.jumpcastcharm.JumpcastCharm;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmulet;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.item.offhand.PhotonSiphon;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class SpellCalibrationImbueHelper {
    private SpellCalibrationImbueHelper() {
    }

    public static boolean isVisibleImbueTarget(@NotNull ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof ArcaneAnvilImbueBlockItem) {
            return false;
        }

        if (ISpellContainer.isSpellContainer(stack)) {
            return Utils.canImbue(stack);
        }

        var probeStack = stack.copy();
        probeStack.setCount(1);
        prepareTarget(probeStack);
        return ISpellContainer.isSpellContainer(probeStack) && Utils.canImbue(probeStack);
    }

    public static boolean isSupportedTarget(@NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (isBlockedCalibrationTarget(stack)) {
            return false;
        }

        var probeStack = stack.copy();
        probeStack.setCount(1);
        prepareTarget(probeStack);
        if (!isAllowedExtractionItem(probeStack) || !ISpellContainer.isSpellContainer(probeStack)) {
            return false;
        }

        var spellContainer = ISpellContainer.get(probeStack);
        if (spellContainer == null || spellContainer.getMaxSpellCount() <= 0) {
            return false;
        }

        if (hasRemovableSpell(probeStack, spellContainer)) {
            return true;
        }

        if (probeStack.getItem() instanceof RestrictedSpellImbuableItem spellImbueItem) {
            return canAcceptAnyKnownSpell(probeStack, spellImbueItem);
        }

        return Utils.canImbue(probeStack);
    }

    public static void prepareTarget(@NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        repairExtractablePresetSpellContainerIfNeeded(stack);
        initializePresetSpellContainerIfNeeded(stack);
    }

    public static int getSpellSlotCount(@NotNull ItemStack stack) {
        if (!isVisibleImbueTarget(stack)) {
            return 0;
        }

        var spellContainer = ISpellContainer.get(stack);
        return spellContainer == null ? 0 : spellContainer.getMaxSpellCount();
    }

    public static @NotNull ItemStack createScrollForSlot(@NotNull ItemStack targetStack, int slot) {
        var spellData = getSpellDataAt(targetStack, slot);
        if (spellData == SpellData.EMPTY || !canRemoveSpell(targetStack, slot, spellData)) {
            return ItemStack.EMPTY;
        }

        return createScroll(spellData);
    }

    public static @NotNull ItemStack createLockedPreviewScrollForSlot(@NotNull ItemStack targetStack, int slot) {
        var spellData = getSpellDataAt(targetStack, slot);
        if (spellData == SpellData.EMPTY || canRemoveSpell(targetStack, slot, spellData)) {
            return ItemStack.EMPTY;
        }

        return createScroll(spellData);
    }

    public static boolean hasSpellAt(@NotNull ItemStack targetStack, int slot) {
        return getSpellDataAt(targetStack, slot) != SpellData.EMPTY;
    }

    public static boolean isMismatchedCastConditionAt(@NotNull ItemStack targetStack, int slot) {
        if (!(targetStack.getItem() instanceof RestrictedSpellImbuableItem spellImbueItem)) {
            return false;
        }

        var spellData = getSpellDataAt(targetStack, slot);
        return spellData != SpellData.EMPTY
                && spellData.getSpell() != null
                && !spellImbueItem.canImbueSpell(spellData);
    }

    public static @NotNull List<Component> getImbueRestrictionTooltipLines(@NotNull ItemStack targetStack) {
        if (targetStack.getItem() instanceof RestrictedSpellImbuableItem spellImbueItem) {
            return spellImbueItem.getImbueRestrictionTooltipLines();
        }
        return List.of();
    }

    public static boolean canPlaceScrollAt(@NotNull ItemStack targetStack, int slot, @NotNull ItemStack scrollStack) {
        var spellData = getScrollSpellData(scrollStack);
        if (spellData == SpellData.EMPTY || !isSupportedTarget(targetStack) || !isValidSpellSlot(targetStack, slot)) {
            return false;
        }

        if (targetStack.getItem() instanceof RestrictedSpellImbuableItem spellImbueItem
                && !spellImbueItem.canImbueSpell(spellData)) {
            return false;
        }

        if (!(targetStack.getItem() instanceof RestrictedSpellImbuableItem) && !Utils.canImbue(targetStack)) {
            return false;
        }

        var probeStack = targetStack.copy();
        if (!applySpellAt(probeStack, slot, spellData)) {
            return false;
        }
        return canRemoveSpell(probeStack, slot, getSpellDataAt(probeStack, slot));
    }

    public static boolean setScrollAt(@NotNull ItemStack targetStack, int slot, @NotNull ItemStack scrollStack) {
        if (!canPlaceScrollAt(targetStack, slot, scrollStack)) {
            return false;
        }

        var spellData = getScrollSpellData(scrollStack);
        if (!applySpellAt(targetStack, slot, spellData)) {
            return false;
        }

        rememberOverriddenPresetSpellState(targetStack, spellData);
        return true;
    }

    public static @NotNull ItemStack removeScrollAt(@NotNull ItemStack targetStack, int slot) {
        var scrollStack = createScrollForSlot(targetStack, slot);
        if (scrollStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var spellContainer = ISpellContainer.get(targetStack);
        if (spellContainer == null) {
            return ItemStack.EMPTY;
        }

        var mutable = spellContainer.mutableCopy();
        if (!mutable.removeSpellAtIndex(slot)) {
            return ItemStack.EMPTY;
        }

        ISpellContainer.set(targetStack, mutable.toImmutable());
        if (mutable.getActiveSpellCount() <= 0) {
            rememberClearedPresetSpellState(targetStack);
        }
        return scrollStack;
    }

    public static boolean isValidSpellSlot(@NotNull ItemStack targetStack, int slot) {
        var spellContainer = ISpellContainer.get(targetStack);
        return spellContainer != null && slot >= 0 && slot < spellContainer.getMaxSpellCount();
    }

    private static boolean canAcceptAnyKnownSpell(
            @NotNull ItemStack stack,
            @NotNull RestrictedSpellImbuableItem spellImbueItem
    ) {
        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getMaxSpellCount() <= 0) {
            return false;
        }

        for (var spell : io.redspace.ironsspellbooks.api.registry.SpellRegistry.getEnabledSpells()) {
            for (var level = spell.getMinLevel(); level <= spell.getMaxLevel(); ++level) {
                var spellData = new SpellData(spell, level);
                if (spellImbueItem.canImbueSpell(spellData) && canPlaceSpellAtAnySlot(stack, spellData)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean canPlaceSpellAtAnySlot(@NotNull ItemStack stack, @NotNull SpellData spellData) {
        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null) {
            return false;
        }

        for (var slot = 0; slot < spellContainer.getMaxSpellCount(); ++slot) {
            var probeStack = stack.copy();
            if (!applySpellAt(probeStack, slot, spellData)) {
                continue;
            }
            if (canRemoveSpell(probeStack, slot, getSpellDataAt(probeStack, slot))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRemovableSpell(@NotNull ItemStack stack, @NotNull ISpellContainer spellContainer) {
        for (var slot = 0; slot < spellContainer.getMaxSpellCount(); ++slot) {
            var spellData = spellContainer.getSpellAtIndex(slot);
            if (spellData != SpellData.EMPTY && canRemoveSpell(stack, slot, spellData)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canRemoveSpell(@NotNull ItemStack stack, int slot, @NotNull SpellData spellData) {
        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellData == SpellData.EMPTY) {
            return false;
        }

        if (stack.getItem() instanceof RestrictedSpellImbuableItem restrictedSpellImbuableItem) {
            return restrictedSpellImbuableItem.canRemoveWorkbenchSpell(stack, spellContainer, slot, spellData);
        }
        return spellData.canRemove();
    }

    private static boolean applySpellAt(@NotNull ItemStack stack, int slot, @NotNull SpellData spellData) {
        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || slot < 0 || slot >= spellContainer.getMaxSpellCount()
                || spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return false;
        }

        var mutable = spellContainer.mutableCopy();
        mutable.removeSpellAtIndex(slot);
        if (!mutable.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), slot, false)) {
            return false;
        }

        ISpellContainer.set(stack, mutable.toImmutable());
        return true;
    }

    private static @NotNull SpellData getSpellDataAt(@NotNull ItemStack stack, int slot) {
        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || slot < 0 || slot >= spellContainer.getMaxSpellCount()) {
            return SpellData.EMPTY;
        }

        var spellData = spellContainer.getSpellAtIndex(slot);
        // @NotNullなのにnullで返ってくることがあるみたいなのでこちらでもガード.
        //noinspection ConstantValue
        return spellData == null ? SpellData.EMPTY : spellData;
    }

    private static @NotNull SpellData getScrollSpellData(@NotNull ItemStack scrollStack) {
        if (scrollStack.isEmpty() || !(scrollStack.getItem() instanceof Scroll)) {
            return SpellData.EMPTY;
        }

        var scrollContainer = ISpellContainer.get(scrollStack);
        if (scrollContainer == null) {
            return SpellData.EMPTY;
        }

        var spellData = scrollContainer.getSpellAtIndex(0);
        // @NotNullなのにnullで返ってくることがあるみたいなのでこちらでもガード.
        //noinspection ConstantValue
        return spellData == null ? SpellData.EMPTY : spellData;
    }

    private static @NotNull ItemStack createScroll(@NotNull SpellData spellData) {
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return ItemStack.EMPTY;
        }

        var scrollStack = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spellData.getSpell(), spellData.getLevel(), scrollStack);
        return scrollStack;
    }

    private static boolean isAllowedExtractionItem(ItemStack stack) {
        var item = stack.getItem();
        return item instanceof RestrictedSpellImbuableItem
                || item instanceof AbstractSpellGunItem
                || item instanceof AbstractRightClickMagicWeaponItem
                || item instanceof AbstractImbueShieldItem
                || item instanceof MagiAgentSuitItem
                || item instanceof AbstractOffhandMagicItem
                || item instanceof AlchemistsFlask
                || stack.is(TagRegistry.Items.SPELLCASTER_WORKBENCH_EXTRACTABLE);
    }

    private static boolean isBlockedCalibrationTarget(ItemStack stack) {
        var item = stack.getItem();
        // UniqueItemは高レベル上書きが本来できるがCalibrationでは未対応に落とす.
        return item instanceof UniqueItem || item instanceof ArcaneAnvilImbueBlockItem;
    }

    private static void repairExtractablePresetSpellContainerIfNeeded(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        var item = stack.getItem();
        if (item instanceof AbstractSpellGunItem spellGunItem) {
            spellGunItem.repairPresetSpellContainerStateIfNeeded(stack);
        } else if (item instanceof AbstractRightClickMagicWeaponItem magicWeaponItem) {
            magicWeaponItem.repairPresetSpellContainerStateIfNeeded(stack);
        } else if (item instanceof AbstractImbueShieldItem imbueShieldItem) {
            imbueShieldItem.repairPresetSpellContainerStateIfNeeded(stack);
        } else if (item instanceof PhotonSiphon photonSiphon) {
            photonSiphon.repairWorkbenchLegacyLockedSpellIfNeeded(stack);
        }
    }

    private static void initializePresetSpellContainerIfNeeded(ItemStack stack) {
        if (stack.isEmpty() || ISpellContainer.isSpellContainer(stack)) {
            return;
        }

        if (stack.getItem() instanceof IPresetSpellContainer presetSpellContainer) {
            presetSpellContainer.initializeSpellContainer(stack);
        } else if (stack.getItem() instanceof AutocastAmulet autocastAmulet) {
            autocastAmulet.initializeSpellContainer(stack);
        } else if (stack.getItem() instanceof JumpcastCharm jumpcastCharm) {
            jumpcastCharm.initializeSpellContainer(stack);
        } else if (stack.getItem() instanceof SatelliteFollowcastAmulet satelliteFollowcastAmulet) {
            satelliteFollowcastAmulet.initializeSpellContainer(stack);
        }
    }

    private static void rememberOverriddenPresetSpellState(ItemStack stack, SpellData spellData) {
        var item = stack.getItem();
        if (item instanceof AbstractSpellGunItem
                || item instanceof AbstractSwingMagicItem
                || item instanceof AbstractImbueShieldItem) {
            PresetSpellContainerStateHelper.rememberOverridden(stack, spellData);
        }
    }

    private static void rememberClearedPresetSpellState(ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof AbstractSpellGunItem
                || item instanceof AbstractSwingMagicItem
                || item instanceof AbstractImbueShieldItem) {
            PresetSpellContainerStateHelper.rememberCleared(stack);
        }
    }

}
