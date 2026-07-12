package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.event.KnockbackControlEvent;
import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshield;
import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshieldRuntime;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

final class BulwarkGreatshieldGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private BulwarkGreatshieldGameTestScenarios() {
    }

    static void bulwarkGreatshieldKeepsCoreItemAndEnchantmentContract(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.BULWARK_GREATSHIELD.get());
            var item = (BulwarkGreatshield) stack.getItem();
            helper.assertTrue(stack.getMaxDamage() == BulwarkGreatshield.DURABILITY,
                    "Bulwark Greatshield durability should be 2031");
            helper.assertTrue(item.getEnchantmentValue(stack) == BulwarkGreatshield.ENCHANTMENT_VALUE,
                    "Bulwark Greatshield enchantment value should be 15");
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
        });
    }

    static void bulwarkGreatshieldDurabilityAndManaRateLimitsStayMemoryOnly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.BULWARK_GREATSHIELD.get());
            BulwarkGreatshield.rememberDurabilityConsumed(stack, 100L);
            helper.assertTrue(BulwarkGreatshield.isDurabilityConsumptionSuppressed(stack, 120L),
                    "Bulwark durability should be suppressed for 20 ticks");
            helper.assertFalse(BulwarkGreatshield.isDurabilityConsumptionSuppressed(stack, 121L),
                    "Bulwark durability suppression should expire after 20 ticks");

            var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "bulwark_greatshield_mana_rate_test");
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
