package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.enchantment.TranscendenceSpellLevelEvent;
import jp.aquafactory.apprenticecodex.enchantment.WisdomExperienceDropEvent;
import jp.aquafactory.apprenticecodex.event.KnockbackControlEvent;
import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshield;
import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshieldRuntime;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.level.BlockEvent;

import java.util.ArrayList;

final class BulwarkGreatshieldGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private BulwarkGreatshieldGameTestScenarios() {
    }

    static void bulwarkGreatshieldKeepsCoreItemAndEnchantmentContract(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.BULWARK_GREATSHIELD.get());
            var item = (BulwarkGreatshield) stack.getItem();
            var tooltipLines = new ArrayList<Component>();
            item.appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
            assertTooltipKeyAt(helper, tooltipLines, 0,
                    "item.apprenticecodex.bulwark_greatshield.desc");
            assertTooltipKeyAt(helper, tooltipLines, 1,
                    "item.apprenticecodex.bulwark_greatshield.cast_default");
            helper.assertTrue(stack.getMaxDamage() == BulwarkGreatshield.DURABILITY,
                    "Bulwark Greatshield durability should be 2031");
            helper.assertTrue(item.getEnchantmentValue(stack) == BulwarkGreatshield.ENCHANTMENT_VALUE,
                    "Bulwark Greatshield enchantment value should be 15");
            helper.assertTrue(item.isValidRepairItem(stack,
                            new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())),
                    "Bulwark Greatshield should repair with arcane ingot");
            helper.assertFalse(item.isValidRepairItem(stack, new ItemStack(Items.DIAMOND)),
                    "Bulwark Greatshield should not repair with diamond");
            helper.assertTrue(item.canApplyAtEnchantingTable(stack, Enchantments.UNBREAKING),
                    "Bulwark Greatshield should accept shield durability enchantments");
            helper.assertTrue(item.canApplyAtEnchantingTable(stack, EnchantmentRegistry.TRANSCENDENCE.get()),
                    "Bulwark Greatshield should accept Transcendence");
            helper.assertTrue(item.canApplyAtEnchantingTable(stack, EnchantmentRegistry.WISDOM.get()),
                    "Bulwark Greatshield should accept Wisdom");
            helper.assertTrue(item.canImbueSpell(SpellRegistry.FIRE_BREATH_SPELL.get(), 1),
                    "Bulwark Greatshield should accept continuous spells");
            helper.assertFalse(item.canImbueSpell(SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1),
                    "Bulwark Greatshield should reject non-continuous spells");

            item.initializeSpellContainer(stack);
            var hiddenContainer = ISpellContainer.get(stack);
            helper.assertTrue(hiddenContainer != null && !hiddenContainer.isSpellWheel(),
                    "Bulwark imbued spell should stay hidden from the spell wheel");
            var legacyVisibleContainer = ISpellContainer.create(1, true, false).mutableCopy();
            legacyVisibleContainer.addSpellAtIndex(SpellRegistry.FIRE_BREATH_SPELL.get(), 1, 0, false);
            ISpellContainer.set(stack, legacyVisibleContainer.toImmutable());
            item.initializeSpellContainer(stack);
            var repairedContainer = ISpellContainer.get(stack);
            helper.assertTrue(repairedContainer != null && !repairedContainer.isSpellWheel(),
                    "Existing Bulwark stacks should repair the spell wheel visibility flag");
            helper.assertTrue(repairedContainer != null
                            && repairedContainer.getSpellAtIndex(0).getSpell() == SpellRegistry.FIRE_BREATH_SPELL.get(),
                    "Repairing Bulwark spell wheel visibility should preserve the imbued spell");

            var offhand = stack.getAttributeModifiers(EquipmentSlot.OFFHAND);
            var mainhand = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
            helper.assertTrue(offhand.get(AttributeRegistry.SPELL_RESIST.get()).stream()
                            .anyMatch(modifier -> modifier.getAmount() == BulwarkGreatshield.GENERIC_SPELL_RESIST
                                    && modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE),
                    "Bulwark Greatshield should grant generic spell resist in offhand");
            helper.assertFalse(mainhand.get(AttributeRegistry.SPELL_RESIST.get()).stream()
                            .anyMatch(modifier -> modifier.getAmount() == BulwarkGreatshield.GENERIC_SPELL_RESIST),
                    "Bulwark Greatshield should not grant generic spell resist in mainhand");
        });
    }

    static void bulwarkGreatshieldCalibrationSupportsOneRuneOrWisdomShard(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "bulwark_greatshield_calibration_test");
            var stack = new ItemStack(ItemRegistry.BULWARK_GREATSHIELD.get());
            var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
            var wisdomShard = new ItemStack(ItemRegistry.WISDOM_SHARD.get());
            var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
            menu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).set(stack);

            helper.assertTrue(menu.isAdjustmentSlotEnabled(0), "Bulwark adjustment slot 0 should be enabled");
            helper.assertFalse(menu.isAdjustmentSlotEnabled(1), "Bulwark should expose only one adjustment slot");
            var adjustmentSlot = menu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START);
            helper.assertTrue(adjustmentSlot.mayPlace(fireRune), "Bulwark should accept a school rune");
            helper.assertTrue(adjustmentSlot.mayPlace(wisdomShard), "Bulwark should accept Wisdom Shard");

            BulwarkGreatshield.setCalibrationAdjustment(stack, 0, fireRune);
            var schoolResist = MagicTools.resolveSchoolResistAttribute(BulwarkGreatshield.getResolvedCalibrationSchool(stack));
            helper.assertTrue(schoolResist != null, "Fire rune should resolve a school resist attribute");
            helper.assertTrue(stack.getAttributeModifiers(EquipmentSlot.OFFHAND).get(schoolResist).stream()
                            .anyMatch(modifier -> modifier.getAmount() == BulwarkGreatshield.SCHOOL_SPELL_RESIST
                                    && modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE),
                    "School rune should add 0.5 school spell resist");
            helper.assertTrue(stack.getAttributeModifiers(EquipmentSlot.OFFHAND).get(AttributeRegistry.SPELL_RESIST.get()).stream()
                            .anyMatch(modifier -> modifier.getAmount() == BulwarkGreatshield.GENERIC_SPELL_RESIST),
                    "School rune must not replace generic spell resist");

            BulwarkGreatshield.setCalibrationAdjustment(stack, 0, wisdomShard);
            helper.assertTrue(BulwarkGreatshield.hasWisdomShardAdjustment(stack),
                    "Wisdom Shard adjustment should be stored on Bulwark");
            var tooltipLines = new ArrayList<Component>();
            stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
            assertTooltipKeyAt(helper, tooltipLines, 0,
                    "item.apprenticecodex.bulwark_greatshield.desc");
            assertTooltipKeyAt(helper, tooltipLines, 1,
                    "item.apprenticecodex.bulwark_greatshield.cast_wisdom");
        });
    }

    static void imbueShieldsApplyTranscendenceAndWisdomEffects(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "imbue_shield_enchantment_effect_test");
            assertImbueShieldEnchantmentEffects(
                    helper,
                    player,
                    new ItemStack(ItemRegistry.BULWARK_GREATSHIELD.get()),
                    "Bulwark Greatshield"
            );
            assertImbueShieldEnchantmentEffects(
                    helper,
                    player,
                    new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get()),
                    "Reflectcast Shield"
            );
        });
    }

    private static void assertImbueShieldEnchantmentEffects(
            GameTestHelper helper,
            net.minecraft.server.level.ServerPlayer player,
            ItemStack stack,
            String itemName
    ) {
        var spell = SpellRegistry.FIRE_BREATH_SPELL.get();
        var spellContainer = ISpellContainer.create(1, false, false).mutableCopy();
        spellContainer.addSpellAtIndex(spell, 1, 0, false);
        ISpellContainer.set(stack, spellContainer.toImmutable());
        stack.enchant(EnchantmentRegistry.TRANSCENDENCE.get(), 1);
        stack.enchant(EnchantmentRegistry.WISDOM.get(), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        var spellLevelEvent = new ModifySpellLevelEvent(spell, player, 1, 1);
        TranscendenceSpellLevelEvent.onModifySpellLevel(spellLevelEvent);
        helper.assertTrue(spellLevelEvent.getLevel() == 2,
                itemName + " Transcendence should increase the imbued spell level from 1 to 2");

        var experienceEvent = new BlockEvent.BreakEvent(
                helper.getLevel(),
                new BlockPos(5, 2, 0),
                Blocks.STONE.defaultBlockState(),
                player
        );
        experienceEvent.setExpToDrop(5);
        WisdomExperienceDropEvent.onBlockBreak(experienceEvent);
        helper.assertTrue(experienceEvent.getExpToDrop() == 6,
                itemName + " Wisdom should increase block experience from 5 to 6");
    }

    private static void assertTooltipKeyAt(GameTestHelper helper, java.util.List<Component> lines, int index,
                                           String expectedKey) {
        helper.assertTrue(lines.size() > index, "Bulwark tooltip line is missing at index " + index);
        var contents = lines.get(index).getContents();
        helper.assertTrue(contents instanceof TranslatableContents translatable
                        && expectedKey.equals(translatable.getKey()),
                "Unexpected Bulwark tooltip at index " + index + ": " + lines.get(index));
    }

    static void bulwarkGreatshieldDurabilityAndManaRateLimitsStayMemoryOnly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "bulwark_greatshield_rate_limit_test");
            var stack = new ItemStack(ItemRegistry.BULWARK_GREATSHIELD.get());
            var beforeDurabilityConsumption = stack.copy();
            stack.setDamageValue(1);
            BulwarkGreatshieldRuntime.rememberDurabilityConsumed(player, 100L);
            helper.assertTrue(BulwarkGreatshieldRuntime.isDurabilityConsumptionSuppressed(player, 120L),
                    "Bulwark durability should be suppressed for 20 ticks");
            helper.assertFalse(BulwarkGreatshieldRuntime.isDurabilityConsumptionSuppressed(player, 121L),
                    "Bulwark durability suppression should expire after 20 ticks");
            helper.assertTrue(Utils.isSameItemSameComponentsIgnoreDurability(beforeDurabilityConsumption, stack),
                    "Bulwark durability rate limit must not add NBT that interrupts continuous casts");

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Bulwark mana recovery test requires MagicData");
            magicData.setMana(0.0F);
            helper.assertTrue(BulwarkGreatshieldRuntime.tryRecoverMana(player),
                    "First Bulwark block should recover mana");
            var recoveredMana = magicData.getMana();
            helper.assertTrue(recoveredMana > 0.0F, "Bulwark block should recover 10% maximum mana");
            helper.assertFalse(BulwarkGreatshieldRuntime.tryRecoverMana(player),
                    "Second Bulwark block in the same cooldown should not recover mana");
            helper.assertTrue(magicData.getMana() == recoveredMana,
                    "Rate-limited Bulwark block should not change mana");

            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            KnockbackControlEvent.markIgnoreKnockbackThisTick(player);
            player.push(1.0D, 0.2D, 0.0D);
            helper.assertTrue(player.getDeltaMovement().lengthSqr() == 0.0D,
                    "Bulwark block tick should suppress direct push velocity");
            ++player.tickCount;
            player.push(1.0D, 0.2D, 0.0D);
            helper.assertTrue(player.getDeltaMovement().lengthSqr() > 0.0D,
                    "Bulwark push suppression should expire on the next tick");

            var castStack = new ItemStack(ItemRegistry.BULWARK_GREATSHIELD.get());
            var castContainer = ISpellContainer.create(1, false, false).mutableCopy();
            castContainer.addSpellAtIndex(SpellRegistry.FIRE_BREATH_SPELL.get(), 1, 0, false);
            ISpellContainer.set(castStack, castContainer.toImmutable());
            player.setItemInHand(InteractionHand.OFF_HAND, castStack);
            player.startUsingItem(InteractionHand.OFF_HAND);
            magicData.setMana(1000.0F);
            BulwarkGreatshieldRuntime.beginUse(player);
            BulwarkGreatshieldRuntime.tryStartContinuousCast(player, castStack, InteractionHand.OFF_HAND);
            helper.assertTrue(player.isUsingItem(),
                    "Bulwark partial continuous cast should preserve shield use");
            helper.assertTrue(BulwarkGreatshieldRuntime.shouldBypassMagicManager(magicData),
                    "Bulwark continuous cast should bypass Iron's standard cast tick");
            BulwarkGreatshieldRuntime.finishUse(player);
            helper.assertFalse(magicData.isCasting(),
                    "Releasing Bulwark should clear its simulated casting state");
            player.stopUsingItem();
            BulwarkGreatshieldRuntime.clear(player);
        });
    }
}
