package jp.aquafactory.apprenticecodex.gametest.equipment.armor.calibration;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.gametest.crafting.support.WorkbenchMenuScope;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationImbueState;
import jp.aquafactory.apprenticecodex.item.armor.EndgameArmorCalibration;
import jp.aquafactory.apprenticecodex.item.armor.EndgameArmorSpellSelectionEvents;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import jp.aquafactory.apprenticecodex.utility.SpellSelectionStackResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellContainerTestSupport.roundTripItemStack;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createSpellCalibrationBenchMenuWithTarget;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ScrollwovenArmorGameTests {
    private ScrollwovenArmorGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void storedScrollsSurviveDisabledSlotsAndRemainExtractable(GameTestHelper helper) {
        helper.succeedIf(() -> {

            var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var scroll = createSpellScroll(spell);
            scroll.set(DataComponents.CUSTOM_NAME, Component.literal("Stored armor scroll"));
            var eligibleArmor = new ItemStack[]{
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get()),
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get()),
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_LEGGINGS.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()),
                    new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get()),
                    new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get()),
                    new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get())
            };
            for (var armorStack : eligibleArmor) {
                helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                                armorStack, 0, new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get())),
                        "Eligible endgame armor should accept Scrollwoven Parchment: "
                                + BuiltInRegistries.ITEM.getKey(armorStack.getItem()));
                helper.assertTrue(EndgameArmorCalibration.getEnabledStoredScrollSlotCount(armorStack) == 1,
                        "Scrollwoven Parchment should enable one stored scroll slot");
                helper.assertTrue(SpellCalibrationImbueHelper.evaluateScrollAt(armorStack, 0, scroll, helper.getLevel().registryAccess())
                                == SpellCalibrationImbueState.ACCEPTED_USABLE,
                        "Enabled endgame armor should accept an unrestricted spell scroll");
                EndgameArmorCalibration.setStoredScroll(armorStack, 0, scroll, helper.getLevel().registryAccess());
                helper.assertTrue(ItemStack.matches(scroll, EndgameArmorCalibration.getStoredScroll(
                                armorStack, 0, helper.getLevel().registryAccess())),
                        "Stored armor scroll should preserve full components before serialization");
                var restored = roundTripItemStack(helper, armorStack);
                helper.assertTrue(ItemStack.matches(scroll, EndgameArmorCalibration.getStoredScroll(
                                restored, 0, helper.getLevel().registryAccess())),
                        "Stored armor scroll should survive serialization with full components");
                helper.assertTrue(EndgameArmorCalibration.getStoredSpellData(armorStack).getSpell() == spell,
                        "Endgame armor should preserve the stored scroll spell");
                helper.assertFalse(ISpellContainer.isSpellContainer(armorStack),
                        "Non-chest endgame armor should not become a spell container");
            }

            var dormantArmor = eligibleArmor[0];
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            dormantArmor, 0, ItemStack.EMPTY),
                    "Scrollwoven Parchment should be removable while a scroll is stored");
            helper.assertTrue(EndgameArmorCalibration.getEnabledStoredScrollSlotCount(dormantArmor) == 0,
                    "Removing Scrollwoven Parchment should disable the stored slot");
            helper.assertTrue(EndgameArmorCalibration.getStoredSpellData(dormantArmor).getSpell() == spell,
                    "Removing Scrollwoven Parchment should preserve the dormant scroll");
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "scrollwoven_armor_test");
            try (var menus = new WorkbenchMenuScope()) {
                var dormantMenu = menus.track(createSpellCalibrationBenchMenuWithTarget(player, dormantArmor.copy()), player);
                helper.assertFalse(dormantMenu.isScrollSlotEnabled(0),
                        "A dormant armor scroll slot should remain disabled");
                helper.assertFalse(dormantMenu.getScrollItem(0).isEmpty(),
                        "A dormant stored scroll should remain visible at the Calibration Bench");
                helper.assertTrue(dormantMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).mayPickup(player),
                        "A dormant stored scroll should remain extractable");
                helper.assertFalse(dormantMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).mayPlace(scroll),
                        "A dormant armor scroll slot should reject insertion");
                var dormantSlot = dormantMenu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START);
                var removed = dormantSlot.remove(1);
                dormantSlot.onTake(player, removed);
                helper.assertTrue(ItemStack.matches(scroll, removed), "Disabled slot should return the complete stored scroll");
                helper.assertTrue(dormantMenu.getScrollItem(0).isEmpty(), "Disabled scroll extraction should clear storage");
                dormantSlot.set(scroll.copy());
                helper.assertTrue(dormantMenu.getScrollItem(0).isEmpty(), "Disabled slot should reject actual reinsertion");
            }
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            dormantArmor, 0, new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get())),
                    "Scrollwoven Parchment should be reinstallable");
            helper.assertTrue(EndgameArmorCalibration.getEnabledStoredScrollSlotCount(dormantArmor) == 1
                            && EndgameArmorCalibration.getStoredSpellData(dormantArmor).getSpell() == spell,
                    "Reinstalling Scrollwoven Parchment should reactivate the stored scroll");

            var chestArmor = new ItemStack[]{
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_COAT.get()),
                    new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get())
            };
            for (var armorStack : chestArmor) {
                helper.assertFalse(EndgameArmorCalibration.usesStoredCalibrationScrolls(armorStack),
                        "Chest armor should preserve its native spell container path");
                helper.assertTrue(EndgameArmorCalibration.getEnabledStoredScrollSlotCount(armorStack) == 0,
                        "Chest armor should not expose an auxiliary stored scroll slot");
            }

        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void equippedArmorSpellsKeepSourceIdentityAndSelectionOrder(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "armor_selection");
            var eligibleArmor = new ItemStack[]{new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get()),
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get()),
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get())};
            for (var stack : eligibleArmor) {
                helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(stack, 0,
                                new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get())), "Selection fixture should accept parchment");
                EndgameArmorCalibration.setStoredScroll(stack, 0, createSpellScroll(spell), helper.getLevel().registryAccess());
            }
            var head = eligibleArmor[0];
            var legs = eligibleArmor[1];
            var feet = eligibleArmor[2];
            player.setItemSlot(EquipmentSlot.HEAD, head);
            player.setItemSlot(EquipmentSlot.LEGS, legs);
            player.setItemSlot(EquipmentSlot.FEET, feet);
            var staff = new ItemStack(ItemRegistry.DIAMOND_SWINGCAST_STAFF.get());
            var mutable = ISpellContainer.create(1, false, false).mutableCopy();
            mutable.addSpellAtIndex(spell, 1, 0, false);
            ISpellContainer.set(staff, mutable.toImmutable());
            helper.assertTrue(((AbstractSwingcastStaffItem) staff.getItem()).allowImbuedSpellInSpellWheel(staff),
                    "Diamond Swingcast Staff should provide the NORMAL-priority comparison option");
            player.setItemInHand(InteractionHand.MAIN_HAND, staff);

            var options = new SpellSelectionManager(player).getAllSpells();
            helper.assertTrue(options.size() >= 4,
                    "Three armor slots and Swingcast Staff should all remain independent selections");
            helper.assertTrue(EndgameArmorSpellSelectionEvents.HEAD_SLOT.equals(options.get(0).slot)
                            && EndgameArmorSpellSelectionEvents.LEGS_SLOT.equals(options.get(1).slot)
                            && EndgameArmorSpellSelectionEvents.FEET_SLOT.equals(options.get(2).slot),
                    "HIGH-priority armor selections should use stable head, legs, feet ordering");
            helper.assertTrue(SpellSelectionManager.MAINHAND.equals(options.get(3).slot),
                    "NORMAL-priority Swingcast Staff selection should follow HIGH-priority armor selections");
            helper.assertTrue(ItemStack.isSameItemSameComponents(
                            SpellSelectionStackResolver.resolveSelectionStack(player, options.get(0).slot), head),
                    "Scrollwoven head selection should resolve to the equipped armor stack");
            helper.assertTrue(options.subList(0, 4).stream().allMatch(option -> option.spellData.getSpell() == spell),
                    "Duplicate event-provided spells should remain independent by equipment source");
        });
    }
}
