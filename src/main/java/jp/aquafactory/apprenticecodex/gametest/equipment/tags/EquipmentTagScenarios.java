package jp.aquafactory.apprenticecodex.gametest.equipment.tags;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.Tags;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spellchargedgreatsword.SpellchargedGreatswordGameTestScenarios.FORGE_BERRIES;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spellchargedgreatsword.SpellchargedGreatswordGameTestScenarios.FORGE_TOOLS_RANGED_WEAPON;

final class EquipmentTagScenarios {
    private EquipmentTagScenarios() {}

    static void commonItemTagsExposeIntendedCompatibilitySurface(GameTestHelper helper) {
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

            helper.assertTrue(new ItemStack(ItemRegistry.COMFORT_BERRIES.get()).is(FORGE_BERRIES),
                    "Comfort Berries should be classified as common berries");
            helper.assertFalse(new ItemStack(ItemRegistry.COMFORT_SANDWICH.get()).is(FORGE_BERRIES),
                    "Comfort Sandwich should not be classified as a berry");

            for (var shield : List.of(
                    ItemRegistry.REFLECTCAST_SHIELD.get(),
                    ItemRegistry.PARRYCAST_BUCKLER.get(),
                    ItemRegistry.BULWARK_GREATSHIELD.get()
            )) {
                helper.assertTrue(new ItemStack(shield).is(Tags.Items.TOOLS_SHIELDS),
                        "Apprentice shield should be classified as a common shield: " + shield);
            }

            var elementalBow = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            helper.assertTrue(elementalBow.is(Tags.Items.TOOLS_BOWS),
                    "Elemental Bow should be classified as a common bow");
            helper.assertTrue(elementalBow.is(FORGE_TOOLS_RANGED_WEAPON),
                    "Elemental Bow should be classified as a common ranged weapon");

            helper.assertFalse(new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get()).is(Tags.Items.TOOLS_BOWS),
                    "Focus Staffbow should remain outside the common bow tag");
            helper.assertFalse(new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get())
                            .is(FORGE_TOOLS_RANGED_WEAPON),
                    "Multipurpose Staffrifle should remain outside the common ranged weapon tag");
        });
    }
}
