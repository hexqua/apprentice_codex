package jp.aquafactory.apprenticecodex.gametest.equipment.scrollcastergauntlet;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static jp.aquafactory.apprenticecodex.gametest.equipment.rightclickweapon.RightClickMagicWeaponGameTests.createIronAutoloaderCrossbowStack;
import static jp.aquafactory.apprenticecodex.gametest.equipment.scrollcastergauntlet.ScrollcasterGauntletScenarios.assertScrollcasterGauntletOffhandUseCasts;
import static jp.aquafactory.apprenticecodex.gametest.equipment.scrollcastergauntlet.ScrollcasterGauntletScenarios.assertScrollcasterGauntletOffhandUseDefersToMainhandSpellItem;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class ScrollcasterEquipmentGameTestScenarios {
    private ScrollcasterEquipmentGameTestScenarios() {}

    static void scrollcasterGauntletOffhandUseCastsSelectedScrollWhenMainHandDoesNotConsumeUse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();

            assertScrollcasterGauntletOffhandUseCasts(
                    helper,
                    ItemStack.EMPTY,
                    spell,
                    "scrollcaster_gauntlet_offhand_empty_mainhand_test"
            );
            assertScrollcasterGauntletOffhandUseCasts(
                    helper,
                    new ItemStack(Items.STONE_SWORD),
                    spell,
                    "scrollcaster_gauntlet_offhand_stone_sword_test"
            );
            assertScrollcasterGauntletOffhandUseDefersToMainhandSpellItem(
                    helper,
                    new ItemStack(ItemRegistry.PASTEL_STAFF.get()),
                    spell,
                    "scrollcaster_gauntlet_offhand_mainhand_staff_test"
            );
            assertScrollcasterGauntletOffhandUseDefersToMainhandSpellItem(
                    helper,
                    new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get()),
                    spell,
                    "scrollcaster_gauntlet_offhand_mainhand_casting_item_test"
            );

            var emptyGauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            var emptyPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "scrollcaster_gauntlet_offhand_empty_selection_test");
            emptyPlayer.setItemInHand(InteractionHand.OFF_HAND, emptyGauntlet);
            var emptyResult = emptyGauntlet.getItem().use(helper.getLevel(), emptyPlayer, InteractionHand.OFF_HAND);
            helper.assertTrue(emptyResult.getResult() == InteractionResult.PASS,
                    "Scrollcaster Gauntlet offhand use without a selected scroll should pass but got "
                            + emptyResult.getResult());
        });
    }

    static void scrollcasterGauntletMainhandPrioritizesSupportedOffhandUseItems(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertScrollcasterGauntletMainhandPrioritizesOffhandUse(
                    helper,
                    new ItemStack(Items.SHIELD),
                    "scrollcaster_gauntlet_mainhand_offhand_shield_test"
            );
            assertScrollcasterGauntletMainhandPrioritizesOffhandUse(
                    helper,
                    new ItemStack(ItemRegistry.ELEMENTAL_BOW.get()),
                    "scrollcaster_gauntlet_mainhand_offhand_elemental_bow_test"
            );
            assertScrollcasterGauntletMainhandPrioritizesOffhandUse(
                    helper,
                    createIronAutoloaderCrossbowStack(helper),
                    "scrollcaster_gauntlet_mainhand_offhand_autoloader_crossbow_test"
            );
            assertScrollcasterGauntletMainhandPrioritizesOffhandUse(
                    helper,
                    new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()),
                    "scrollcaster_gauntlet_mainhand_offhand_spellgun_test"
            );
        });
    }

    static void assertScrollcasterGauntletMainhandPrioritizesOffhandUse(
            GameTestHelper helper,
            ItemStack offhandStack,
            String profileName
    ) {
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(spell));
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        player.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
        player.setItemInHand(InteractionHand.OFF_HAND, offhandStack.copy());
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null,
                "Scrollcaster Gauntlet mainhand offhand priority test could not resolve player mana data");
        magicData.setMana(100.0F);

        var result = gauntlet.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getResult() == InteractionResult.PASS,
                "Scrollcaster Gauntlet mainhand use should pass to supported offhand use item "
                        + offhandStack + " but got " + result.getResult());
        helper.assertFalse(magicData.isCasting(),
                "Scrollcaster Gauntlet mainhand use should not cast before supported offhand use item "
                        + offhandStack);
    }
}
