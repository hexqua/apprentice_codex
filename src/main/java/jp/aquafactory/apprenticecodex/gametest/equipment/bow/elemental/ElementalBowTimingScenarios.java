package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.useElementalBowSpellConfig;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.ElementalBowStateTestSupport.setElementalBowShotSelection;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.ElementalBowStateTestSupport.useElementalBowConfigOverrideForGameTest;

final class ElementalBowTimingScenarios {
    private ElementalBowTimingScenarios() {}

    static void elementalBowCooldownHelperIgnoresWeaponMultiplierButKeepsPlayerCooldownReduction(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowSpellConfig(helper)) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_cooldown_reduction");
                var spell = SpellRegistry.FIRE_ARROW_SPELL.get();
                var attribute = player.getAttribute(AttributeRegistry.COOLDOWN_REDUCTION.get());
                attribute.addPermanentModifier(new AttributeModifier(UUID.randomUUID(), "bow_cdr",
                        0.35D, AttributeModifier.Operation.MULTIPLY_BASE));
                int actual = ElementalBowOverheatManager.resolveCooldownTicks(spell, player);
                int expected = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(spell, player, CastSource.SPELLBOOK);
                helper.assertTrue(actual == expected && actual < spell.getSpellCooldown(), "Cooling must keep player CDR and exclude weapon multiplier");
            }
        });
    }

    static void elementalBowMagicDrawTicksUseProfileAndServerMultiplier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowConfigOverrideForGameTest(
                    1.5D,
                    0.20D,
                    0.08D,
                    1.0D,
                    0,
                    0)) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_draw_config_test");
                var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
                setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                player.getInventory().setItem(1, new ItemStack(Items.ARROW, 2));

                var magicData = MagicData.getPlayerMagicData(player);
                helper.assertTrue(magicData != null, "Elemental Bow draw config test could not resolve player mana data");
                magicData.setMana(300.0F);
                var initialMana = magicData.getMana();

                helper.assertTrue(ElementalBow.resolveMagicRequiredDrawTicks(stack) == 30,
                        "Elemental Bow required draw ticks should use profile ticks and server multiplier");
                var shortUseResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(shortUseResult.getResult().consumesAction(),
                        "Elemental Bow draw config test should start drawing: " + shortUseResult.getResult());
                stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - 29);
                player.stopUsingItem();
                helper.assertTrue(stack.getDamageValue() == 0,
                        "Elemental Bow should not fire before configured draw ticks");
                helper.assertTrue(player.getInventory().getItem(1).getCount() == 2,
                        "Elemental Bow should not consume arrows before configured draw ticks");
                helper.assertTrue(magicData.getMana() == initialMana,
                        "Elemental Bow should not consume mana before configured draw ticks");

                var readyUseResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(readyUseResult.getResult().consumesAction(),
                        "Elemental Bow draw config test should restart drawing: " + readyUseResult.getResult());
                stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - 30);
                player.stopUsingItem();
                helper.assertTrue(stack.getDamageValue() == 1,
                        "Elemental Bow should fire at configured draw ticks");
                helper.assertTrue(player.getInventory().getItem(1).getCount() == 1,
                        "Elemental Bow should consume one arrow after configured draw ticks");
                helper.assertTrue(magicData.getMana() < initialMana,
                        "Elemental Bow should consume spell mana after configured draw ticks");
            }
        });
    }
}
