package jp.aquafactory.apprenticecodex.gametest.support;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.item.AbstractImbuedMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.shield.AbstractImbueShieldItem;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class SpellContainerTestSupport {
    private SpellContainerTestSupport() {}

    public static void assertStackHasSpell(
            GameTestHelper helper,
            ItemStack stack,
            AbstractSpell expectedSpell,
            int expectedLevel,
            String message
    ) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, message + ": missing spell container");
        if (spellContainer == null) {
            return;
        }

        for (var index = 0; index < spellContainer.getMaxSpellCount(); ++index) {
            var spellData = spellContainer.getSpellAtIndex(index);
            if (spellData != SpellData.EMPTY
                    && spellData.getSpell() == expectedSpell
                    && spellData.getLevel() == expectedLevel) {
                return;
            }
        }

        helper.assertTrue(false, message + ": expected spell was not found");
    }

    public static void applyRestrictedImbueNormalization(
            GameTestHelper helper,
            ItemStack stack,
            RestrictedSpellImbuableItem item,
            AbstractSpell spell,
            int spellLevel
    ) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Missing spell container before restricted imbue normalization test");

        var mutable = spellContainer.mutableCopy();
        if (mutable.getSpellAtIndex(0) != SpellData.EMPTY) {
            helper.assertTrue(mutable.removeSpellAtIndex(0),
                    "Failed to clear existing spell before restricted imbue normalization test");
        }
        helper.assertTrue(mutable.addSpellAtIndex(spell, spellLevel, 0, false),
                "Failed to prepare unlocked spell data before restricted imbue normalization test");
        ISpellContainer.set(stack, mutable.toImmutable());
        item.normalizeImbuedSpellContainer(stack);
    }

    public static void setSingleUnlockedSpell(GameTestHelper helper, ItemStack stack, AbstractSpell spell, int spellLevel) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Missing spell container before Focus Staffbow spell setup");

        var mutable = spellContainer.mutableCopy();
        if (mutable.getSpellAtIndex(0) != SpellData.EMPTY) {
            helper.assertTrue(mutable.removeSpellAtIndex(0),
                    "Failed to clear existing spell before Focus Staffbow spell setup");
        }
        helper.assertTrue(mutable.addSpellAtIndex(spell, spellLevel, 0, false),
                "Failed to prepare Focus Staffbow spell setup");
        ISpellContainer.set(stack, mutable.toImmutable());
    }

    public static void applyPresetSpellExtraction(GameTestHelper helper, ItemStack stack) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Missing spell container before preset extraction test");

        var mutable = spellContainer.mutableCopy();
        if (mutable.getSpellAtIndex(0) != SpellData.EMPTY) {
            helper.assertTrue(mutable.removeSpellAtIndex(0),
                    "Failed to clear existing spell before preset extraction test");
        }
        ISpellContainer.set(stack, mutable.toImmutable());
        PresetSpellContainerStateHelper.rememberCleared(stack);
    }

    public static void applyLegacyLockedReplacement(GameTestHelper helper, ItemStack stack, AbstractSpell spell, int spellLevel) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, "Missing spell container before legacy replacement test");

        var mutable = spellContainer.mutableCopy();
        if (mutable.getSpellAtIndex(0) != SpellData.EMPTY) {
            helper.assertTrue(mutable.removeSpellAtIndex(0),
                    "Failed to clear existing spell before legacy replacement test");
        }
        helper.assertTrue(mutable.addSpellAtIndex(spell, spellLevel, 0, true),
                "Failed to prepare legacy locked replacement spell");
        ISpellContainer.set(stack, mutable.toImmutable());
        PresetSpellContainerStateHelper.clearRememberedState(stack);
    }

    public static ItemStack roundTripItemStack(GameTestHelper helper, ItemStack stack) {
        return ItemStack.parseOptional(
                helper.getLevel().registryAccess(),
                (CompoundTag) stack.saveOptional(helper.getLevel().registryAccess())
        );
    }

    public static void repairPresetSpellContainerStateIfNeeded(ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof AbstractSpellGunItem spellGunItem) {
            spellGunItem.repairPresetSpellContainerStateIfNeeded(stack);
        } else if (item instanceof AbstractImbuedMagicWeaponItem magicWeaponItem) {
            magicWeaponItem.repairPresetSpellContainerStateIfNeeded(stack);
        } else if (item instanceof AbstractImbueShieldItem imbueShieldItem) {
            imbueShieldItem.repairPresetSpellContainerStateIfNeeded(stack);
        }
    }

    public static void assertClearedSpellContainer(GameTestHelper helper, ItemStack stack, String message) {
        var spellContainer = ISpellContainer.get(stack);
        helper.assertTrue(spellContainer != null, message + ": spell container is null");
        helper.assertTrue(spellContainer.getActiveSpellCount() <= 0,
                message + ": expected no active spells but got " + spellContainer.getActiveSpellCount());
        helper.assertTrue(spellContainer.getSpellAtIndex(0) == SpellData.EMPTY,
                message + ": slot 0 unexpectedly contains " + spellContainer.getSpellAtIndex(0).getSpell().getSpellResource());
    }
}
