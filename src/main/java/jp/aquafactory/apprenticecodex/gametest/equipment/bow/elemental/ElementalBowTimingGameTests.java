package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowTimingGameTests {
    private ElementalBowTimingGameTests() {}

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.elemental_bow_overheat")
    public static void elementalBowCooldownHelperIgnoresWeaponMultiplierButKeepsPlayerCooldownReduction(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowSpellConfig(helper)) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "elemental_cooldown_reduction");
                var spell = SpellRegistry.FIRE_ARROW_SPELL.get();
                var attribute = player.getAttribute(AttributeRegistry.COOLDOWN_REDUCTION);
                attribute.addPermanentModifier(new AttributeModifier(ResourceLocation.fromNamespaceAndPath("apprenticecodex", "bow_cdr"),
                        0.25D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                var previousSwordMultiplier = ServerConfigs.SWORDS_CD_MULTIPLIER.get();
                try {
                    ServerConfigs.SWORDS_CD_MULTIPLIER.set(3.0D);
                    ServerConfigs.SWORDS_CD_MULTIPLIER.clearCache();
                    int actual = ElementalBowOverheatManager.resolveCooldownTicks(spell, player);
                    int expected = (int) (spell.getSpellCooldown() * 0.75D);
                    helper.assertTrue(actual == expected, "Cooling must keep player CDR and exclude the non-neutral weapon multiplier");
                } finally {
                    ServerConfigs.SWORDS_CD_MULTIPLIER.set(previousSwordMultiplier);
                    ServerConfigs.SWORDS_CD_MULTIPLIER.clearCache();
                }
            }
        });
    }

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.elemental_bow_draw_config")
    public static void elementalBowMagicDrawTicksUseProfileAndServerMultiplier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowConfigOverrideForGameTest(
                    1.5D,
                    0.20D,
                    0.08D,
                    1.0D,
                    0,
                    0)) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "elemental_bow_draw_config_test");
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
                stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - 29);
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
                stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - 30);
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
