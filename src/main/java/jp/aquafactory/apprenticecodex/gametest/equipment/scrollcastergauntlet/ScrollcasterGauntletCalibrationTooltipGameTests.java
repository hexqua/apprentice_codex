package jp.aquafactory.apprenticecodex.gametest.equipment.scrollcastergauntlet;

import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentTooltip;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.createEnchantedBook;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.createSpellScroll;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ScrollcasterGauntletCalibrationTooltipGameTests {
    private ScrollcasterGauntletCalibrationTooltipGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletTooltipExcludesCalibrationScrolls(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var lookupProvider = helper.getLevel().registryAccess();
            var enchantments = lookupProvider.lookupOrThrow(Registries.ENCHANTMENT);
            var storedBook = createEnchantedBook(
                    enchantments.getOrThrow(Enchantments.SHARPNESS),
                    1
            );
            storedBook.set(DataComponents.CUSTOM_NAME, Component.literal("Tooltip component book"));
            var expectedBook = storedBook.copyWithCount(1);
            var slotUpgrade = new ItemStack(
                    io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()
            );
            var target = (SpellCalibrationAdjustmentTarget) gauntlet.getItem();
            CalibrationAdjustmentStorage.set(
                    gauntlet,
                    0,
                    ScrollcasterGauntlet.CALIBRATION_ADJUSTMENT_SLOT_COUNT,
                    storedBook,
                    lookupProvider
            );
            helper.assertTrue(target.trySetCalibrationAdjustment(gauntlet, 2, slotUpgrade, lookupProvider),
                    "Tooltip test should store a slot upgrade without filling the middle adjustment slot");

            ScrollcasterGauntlet.setCalibrationScroll(
                    gauntlet,
                    0,
                    createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get()),
                    lookupProvider
            );
            var populatedPayload = gauntlet.getItem().getTooltipImage(gauntlet).orElseThrow();
            helper.assertTrue(populatedPayload instanceof CalibrationAdjustmentTooltip,
                    "Populated Scrollcaster Gauntlet should keep calibration adjustment tooltip data");
            var populatedItems = ((CalibrationAdjustmentTooltip) populatedPayload).items();
            helper.assertTrue(ItemStack.isSameItemSameComponents(populatedItems.get(0), expectedBook),
                    "Calibration tooltip should retain adjustment item components");
            helper.assertTrue(populatedItems.get(1).isEmpty(),
                    "Calibration tooltip should preserve an empty middle adjustment slot");
            helper.assertTrue(populatedItems.get(2).is(slotUpgrade.getItem()),
                    "Calibration tooltip should preserve adjustment slot order");
            helper.assertTrue(populatedItems.stream()
                            .noneMatch(stack -> stack.getItem() instanceof Scroll),
                    "Calibration tooltip should not include Scrollcaster Gauntlet scroll slots");
        });
    }
}
