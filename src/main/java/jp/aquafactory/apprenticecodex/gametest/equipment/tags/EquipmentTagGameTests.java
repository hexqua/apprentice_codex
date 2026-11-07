package jp.aquafactory.apprenticecodex.gametest.equipment.tags;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentTagGameTests {
    private EquipmentTagGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void commonItemTagsExposeIntendedCompatibilitySurface(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertTrue(new ItemStack(ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get()).is(Tags.Items.INGOTS),
                    "Spell-stained Arcane Ingot should be classified as a common ingot");
            var emberstainedNetherite = new ItemStack(ItemRegistry.EMBERSTAINED_NETHERITE_INGOT.get());
            helper.assertTrue(emberstainedNetherite.is(Tags.Items.INGOTS),
                    "Ember-stained Netherite Ingot should be classified as a common ingot");
            helper.assertFalse(emberstainedNetherite.is(Tags.Items.INGOTS_NETHERITE),
                    "Ember-stained Netherite Ingot should not substitute for ordinary netherite");

            var spellstainedDiamond = new ItemStack(ItemRegistry.SPELLSTAINED_DIAMOND.get());
            helper.assertTrue(spellstainedDiamond.is(Tags.Items.GEMS),
                    "Spell-stained Diamond should be classified as a common gem");
            helper.assertFalse(spellstainedDiamond.is(Tags.Items.GEMS_DIAMOND),
                    "Spell-stained Diamond should not substitute for ordinary diamonds");

            var comfortBerries = new ItemStack(ItemRegistry.COMFORT_BERRIES.get());
            helper.assertTrue(comfortBerries.is(Tags.Items.FOODS_BERRY),
                    "Comfort Berries should be classified as common berries");
            helper.assertTrue(comfortBerries.is(Tags.Items.FOODS),
                    "Common berry classification should expose Comfort Berries as food");
            var comfortSandwich = new ItemStack(ItemRegistry.COMFORT_SANDWICH.get());
            helper.assertTrue(comfortSandwich.is(Tags.Items.FOODS),
                    "Comfort Sandwich should be classified as common food");
            helper.assertFalse(comfortSandwich.is(Tags.Items.FOODS_BERRY),
                    "Comfort Sandwich should not be classified as a berry");

            for (var shield : List.of(
                    ItemRegistry.REFLECTCAST_SHIELD.get(),
                    ItemRegistry.PARRYCAST_BUCKLER.get(),
                    ItemRegistry.BULWARK_GREATSHIELD.get()
            )) {
                helper.assertTrue(new ItemStack(shield).is(Tags.Items.TOOLS_SHIELD),
                        "Apprentice shield should be classified as a common shield: " + shield);
            }

            var elementalBow = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            helper.assertTrue(elementalBow.is(Tags.Items.TOOLS_BOW),
                    "Elemental Bow should be classified as a common bow");
            helper.assertTrue(elementalBow.is(Tags.Items.RANGED_WEAPON_TOOLS),
                    "Elemental Bow should be classified as a common ranged weapon");

            helper.assertFalse(new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get()).is(Tags.Items.TOOLS_BOW),
                    "Focus Staffbow should remain outside the common bow tag");
            helper.assertFalse(new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get())
                            .is(Tags.Items.RANGED_WEAPON_TOOLS),
                    "Multipurpose Staffrifle should remain outside the common ranged weapon tag");
            helper.assertFalse(new ItemStack(ItemRegistry.PASTEL_STAFF.get()).is(Tags.Items.MELEE_WEAPON_TOOLS),
                    "Staff items should remain outside the common melee weapon tag");
        });
    }


}
