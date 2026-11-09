package jp.aquafactory.apprenticecodex.gametest.equipment.broom.calibration;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.item.broom.AbstractBroomItem;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class BroomCalibrationTestSupport {
    private BroomCalibrationTestSupport() {}

    static ItemStack calibratedBroomStack(Item broomItem, int enabledSlots) {
        var stack = new ItemStack(broomItem);
        installBroomCalibration(stack, enabledSlots);
        return stack;
    }

    public static ItemStack backCurioBroomStack(Item broomItem) {
        var stack = new ItemStack(broomItem);
        if (!SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                stack,
                2,
                new ItemStack(Items.SADDLE)
        )) {
            throw new IllegalStateException("Broom Curios calibration was rejected");
        }
        return stack;
    }

    public static ItemStack backCurioCalibratedBroomStack(Item broomItem, int enabledSlots) {
        if (enabledSlots < 0 || enabledSlots > 2) {
            throw new IllegalArgumentException("Back Curio broom supports zero to two scroll slot upgrades");
        }
        var stack = calibratedBroomStack(broomItem, enabledSlots);
        if (!SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                stack,
                2,
                new ItemStack(Items.SADDLE)
        )) {
            throw new IllegalStateException("Broom Curios calibration was rejected");
        }
        return stack;
    }

    public static void installBroomCalibration(ItemStack stack, int enabledSlots) {
        installBroomScrolls(stack);
        installBroomUpgrades(stack, enabledSlots);
    }

    static void installBroomScrolls(ItemStack stack) {
        var namedScroll = createBroomScroll(SpellRegistry.MANTIS_LEAP.get());
        namedScroll.setHoverName(Component.literal("Calibrated Mantis Leap"));
        AbstractBroomItem.setCalibrationScroll(stack, 0, namedScroll);
        AbstractBroomItem.setCalibrationScroll(stack, 1, createBroomScroll(SpellRegistry.WIZARDLAMP.get()));
        AbstractBroomItem.setCalibrationScroll(stack, 2, createBroomScroll(SpellRegistry.MANA_CHARGE.get()));
    }

    static void installBroomUpgrades(ItemStack stack, int count) {
        for (var slot = 0; slot < count; ++slot) {
            var accepted = SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    stack,
                    slot,
                    new ItemStack(ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
            );
            if (!accepted) {
                throw new IllegalStateException("Broom slot upgrade was rejected at slot " + slot);
            }
        }
    }

    static ItemStack createBroomScroll(AbstractSpell spell) {
        var scroll = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, 1, scroll);
        return scroll;
    }

    public static void assertCalibrationContents(
            GameTestHelper helper,
            ItemStack stack,
            int enabledSlots,
            String subject
    ) {
        helper.assertTrue(AbstractBroomItem.getEnabledCalibrationScrollSlotCount(stack) == enabledSlots,
                subject + " should preserve enabled scroll slot count");
        var expectedSpells = List.of(
                SpellRegistry.MANTIS_LEAP.get(),
                SpellRegistry.WIZARDLAMP.get(),
                SpellRegistry.MANA_CHARGE.get()
        );
        for (var slot = 0; slot < expectedSpells.size(); ++slot) {
            helper.assertTrue(AbstractBroomItem.getCalibrationSpellData(stack, slot).getSpell() == expectedSpells.get(slot),
                    subject + " should preserve calibration scroll " + slot);
        }
        helper.assertTrue(Component.literal("Calibrated Mantis Leap").equals(
                        AbstractBroomItem.getCalibrationScroll(stack, 0).getHoverName()),
                subject + " should preserve the full stored scroll ItemStack");
    }
}
