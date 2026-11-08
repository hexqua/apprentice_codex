package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import jp.aquafactory.apprenticecodex.enchantment.PlunderLootingLevelEvent;
import jp.aquafactory.apprenticecodex.enchantment.WisdomExperienceDropEvent;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.level.BlockEvent;

import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.ElementalBowStateTestSupport.setElementalBowShotSelection;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.BowGameTestSupport.assertElementalBowSelection;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowTestSupport.*;

final class ElementalBowLegacyScenarios {
    private ElementalBowLegacyScenarios() {}

    static void elementalBowInsufficientArrowErrorUsesDedicatedTranslationKey(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var message = ElementalBow.createInsufficientArrowMessage();
            assertTranslatableKey(
                    helper,
                    message,
                    "ui.apprenticecodex.elemental_bow.insufficient_arrow",
                    "Elemental Bow insufficient arrow error should use its dedicated translation key"
            );
            helper.assertTrue(
                    message.getStyle().getColor() != null
                            && "red".equals(message.getStyle().getColor().serialize()),
                    "Elemental Bow insufficient arrow error should be red"
            );
        });
    }

    static void elementalBowHeldWisdomAndPlunderWorkInBothHands(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var state = Blocks.DIAMOND_ORE.defaultBlockState();

            var mainhandPlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "elemental_bow_mainhand_held_enchant_test"));
            var mainhandBow = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            mainhandBow.enchant(EnchantmentRegistry.WISDOM.get(), 1);
            mainhandBow.enchant(EnchantmentRegistry.PLUNDER.get(), 2);
            mainhandPlayer.setItemInHand(InteractionHand.MAIN_HAND, mainhandBow);

            var mainhandExperience = new BlockEvent.BreakEvent(level, new BlockPos(3, 2, 0), state, mainhandPlayer);
            mainhandExperience.setExpToDrop(3);
            WisdomExperienceDropEvent.onBlockBreak(mainhandExperience);
            helper.assertTrue(mainhandExperience.getExpToDrop() == 4,
                    "Elemental Bow mainhand Wisdom should increase block experience from 3 to 4 but got " + mainhandExperience.getExpToDrop());

            var mainhandLootingEvent = new LootingLevelEvent(
                    helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1)),
                    mainhandPlayer.damageSources().playerAttack(mainhandPlayer),
                    0
            );
            PlunderLootingLevelEvent.onLootingLevel(mainhandLootingEvent);
            helper.assertTrue(mainhandLootingEvent.getLootingLevel() == 2,
                    "Elemental Bow mainhand Plunder should set looting level to 2 but got " + mainhandLootingEvent.getLootingLevel());

            var offhandPlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "elemental_bow_offhand_held_enchant_test"));
            var offhandBow = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            offhandBow.enchant(EnchantmentRegistry.WISDOM.get(), 1);
            offhandBow.enchant(EnchantmentRegistry.PLUNDER.get(), 3);
            offhandPlayer.setItemInHand(InteractionHand.OFF_HAND, offhandBow);

            var offhandExperience = new BlockEvent.BreakEvent(level, new BlockPos(4, 2, 0), state, offhandPlayer);
            offhandExperience.setExpToDrop(3);
            WisdomExperienceDropEvent.onBlockBreak(offhandExperience);
            helper.assertTrue(offhandExperience.getExpToDrop() == 4,
                    "Elemental Bow offhand Wisdom should increase block experience from 3 to 4 but got " + offhandExperience.getExpToDrop());

            var offhandLootingEvent = new LootingLevelEvent(
                    helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 1)),
                    offhandPlayer.damageSources().playerAttack(offhandPlayer),
                    0
            );
            PlunderLootingLevelEvent.onLootingLevel(offhandLootingEvent);
            helper.assertTrue(offhandLootingEvent.getLootingLevel() == 3,
                    "Elemental Bow offhand Plunder should set looting level to 3 but got " + offhandLootingEvent.getLootingLevel());
        });
    }

    static void elementalBowSpecialModeInfinityKeepsSelectionAndAllowsEmptyReuse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_special_arrow_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(Enchantments.INFINITY_ARROWS, 1);
            setElementalBowShotSelection(stack, "special", ResourceLocation.tryParse("minecraft:spectral_arrow"));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW));

            var firstUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(firstUse.getResult().consumesAction(),
                    "Elemental Bow special mode should start drawing while the selected arrow exists: " + firstUse.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - 20);
            helper.assertTrue(player.getInventory().getItem(1).isEmpty(),
                    "Elemental Bow special mode should consume the selected arrow even with Infinity");

            var secondUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(secondUse.getResult().consumesAction(),
                    "Elemental Bow special mode should start drawing again with Infinity after the selected arrow runs out: " + secondUse.getResult());
            helper.assertTrue(player.isUsingItem(),
                    "Elemental Bow special mode should enter use state again while keeping its empty selection");
            assertElementalBowSelection(helper, stack, "special", ResourceLocation.tryParse("minecraft:spectral_arrow"),
                    "Elemental Bow special mode should keep the selected arrow after ammo loss");
        });
    }

    static void elementalBowSynthesisTooltipKeepsExpectedState(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            assertTooltipKeyAt(helper, stack, 0, "item.apprenticecodex.elemental_bow.mode",
                    "Elemental Bow should always show the current mode tooltip line");
            assertTooltipKeyUsesColor(helper, stack, "item.apprenticecodex.elemental_bow.desc", ChatFormatting.GRAY,
                    "Elemental Bow should always show the description tooltip line");
            assertTooltipKeyAbsent(helper, stack, "item.apprenticecodex.elemental_bow.spell.no_enchantment",
                    "Elemental Bow should not show spell ammo tooltip while not in magic mode");
            assertTooltipKeyAbsent(helper, stack, "item.apprenticecodex.elemental_bow.spell.with_infinity",
                    "Elemental Bow should not show Infinity spell tooltip while not in magic mode");
            assertTooltipKeyAbsent(helper, stack, "item.apprenticecodex.elemental_bow.spell.with_synthesis",
                    "Elemental Bow should not show Synthesis spell tooltip while not in magic mode");

            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            assertTooltipKeyAt(helper, stack, 1, "item.apprenticecodex.elemental_bow.desc",
                    "Elemental Bow should show the description below the mode tooltip line");
            assertTooltipKeyAbsent(helper, stack, "item.apprenticecodex.elemental_bow.spell.no_enchantment", "Spellgun details must omit the ammo section");

            var infinityStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(infinityStack, "magic", SchoolRegistry.FIRE_RESOURCE);
            infinityStack.enchant(Enchantments.INFINITY_ARROWS, 1);
            assertTooltipKeyAbsent(helper, infinityStack, "item.apprenticecodex.elemental_bow.spell.with_infinity", "Spellgun details must omit the ammo section");

            var synthesisStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(synthesisStack, "magic", SchoolRegistry.FIRE_RESOURCE);
            synthesisStack.enchant(EnchantmentRegistry.SYNTHESIS.get(), 1);
            assertTooltipKeyAbsent(helper, synthesisStack, "item.apprenticecodex.elemental_bow.spell.with_synthesis", "Spellgun details must omit the ammo section");
            assertTooltipKeyAbsent(helper, synthesisStack, "item.apprenticecodex.elemental_bow.with_synthesis",
                    "Elemental Bow should no longer show the legacy Synthesis tooltip key");

            synthesisStack.enchant(Enchantments.INFINITY_ARROWS, 1);
            assertTooltipKeyAbsent(helper, synthesisStack, "item.apprenticecodex.elemental_bow.spell.with_synthesis", "Spellgun details must omit the ammo section");
            assertTooltipKeyAbsent(helper, synthesisStack, "item.apprenticecodex.elemental_bow.spell.with_infinity",
                    "Elemental Bow should not show the Infinity spell tooltip when Synthesis is also present");
        });
    }
}
