package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.ImbueShieldBlockCastEvent;
import jp.aquafactory.apprenticecodex.item.shield.ParrycastBuckler;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import jp.aquafactory.apprenticecodex.utility.ScrollcasterSchoolRuneResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;

import java.util.ArrayList;

final class ParrycastBucklerGameTestScenarios {
    private ParrycastBucklerGameTestScenarios() {}

    static void parrycastBucklerKeepsCoreContract(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.PARRYCAST_BUCKLER.get());
            var item = (ParrycastBuckler) stack.getItem();
            var tooltipLines = new ArrayList<Component>();
            item.appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
            assertTooltipKeyAt(helper, tooltipLines, 0,
                    "item.apprenticecodex.parrycast_buckler.desc");
            assertTooltipKeyAt(helper, tooltipLines, 1,
                    "item.apprenticecodex.parrycast_buckler.cast_default");
            helper.assertTrue(stack.getMaxDamage() == 1561, "Parrycast Buckler durability should be 1561");
            helper.assertTrue(item.getEnchantmentValue(stack) == 22, "Parrycast Buckler enchantment value should be 22");
            helper.assertTrue(item.canApplyAtEnchantingTable(stack, Enchantments.UNBREAKING), "Parrycast should accept shield enchantments");
            helper.assertTrue(item.canApplyAtEnchantingTable(stack, EnchantmentRegistry.TENSE.get()), "Parrycast should accept Tense");
            helper.assertTrue(item.canApplyAtEnchantingTable(stack, EnchantmentRegistry.ALACRITY.get()), "Parrycast should accept Alacrity");
            helper.assertTrue(item.canApplyAtEnchantingTable(stack, EnchantmentRegistry.TRANSCENDENCE.get()), "Parrycast should accept Transcendence");
            helper.assertTrue(item.canApplyAtEnchantingTable(stack, EnchantmentRegistry.WISDOM.get()), "Parrycast should accept Wisdom");
            helper.assertTrue(item.canImbueSpell(SpellRegistry.SENSE_EVIL.get(), 1), "Parrycast should accept instant no-recast spells");
            helper.assertTrue(item.canImbueSpell(SpellRegistry.MANTIS_LEAP.get(), 1),
                    "Parrycast should always accept long no-recast spells for imbue");
            helper.assertFalse(item.canUseConfiguredSpell(stack, SpellRegistry.MANTIS_LEAP.get(), 1), "Long spell should require Silver Ring");
            var longScroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(SpellRegistry.MANTIS_LEAP.get(), 1, longScroll);
            helper.assertTrue(SpellCalibrationImbueHelper.canPlaceScrollAt(stack, 0, longScroll),
                    "Long scroll placement should not require Silver Ring");
            helper.assertTrue(SpellCalibrationImbueHelper.setScrollAt(stack, 0, longScroll),
                    "Long scroll should be insertable without Silver Ring");
            helper.assertTrue(item.isMismatchedCastConditionAt(stack, 0),
                    "Inserted long spell should warn while Silver Ring is absent");
            assertFirstRestrictionKey(helper, item.getImbueRestrictionTooltipLines(stack),
                    "item.apprenticecodex.spellgun.tooltip.restrict_restrict_instant_only");
            ParrycastBuckler.setCalibrationAdjustment(stack, 0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()));
            helper.assertTrue(item.canUseConfiguredSpell(stack, SpellRegistry.MANTIS_LEAP.get(), 1), "Silver Ring should allow long spells");
            helper.assertFalse(item.isMismatchedCastConditionAt(stack, 0),
                    "Silver Ring should clear the inserted long spell warning");
            assertFirstRestrictionKey(helper, item.getImbueRestrictionTooltipLines(stack),
                    "item.apprenticecodex.spellgun.tooltip.restrict_restrict_not_continuous");
        });
    }

    static void parrycastBucklerSupportsThreeAdjustmentsAndSchoolDeduplication(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "parrycast_calibration_test");
            var stack = new ItemStack(ItemRegistry.PARRYCAST_BUCKLER.get());
            var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
            menu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).set(stack);
            for (int i = 0; i < 3; i++) helper.assertTrue(menu.isAdjustmentSlotEnabled(i), "Parrycast adjustment slot should be enabled: " + i);

            var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
            ParrycastBuckler.setCalibrationAdjustment(stack, 0, fireRune);
            ParrycastBuckler.setCalibrationAdjustment(stack, 1, fireRune);
            ParrycastBuckler.setCalibrationAdjustment(stack, 2, new ItemStack(ItemRegistry.WISDOM_SHARD.get()));
            helper.assertTrue(ParrycastBuckler.hasWisdomShard(stack), "Wisdom Shard should be stored");
            var tooltipLines = new ArrayList<Component>();
            stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
            assertTooltipKeyAt(helper, tooltipLines, 0,
                    "item.apprenticecodex.parrycast_buckler.desc");
            assertTooltipKeyAt(helper, tooltipLines, 1,
                    "item.apprenticecodex.parrycast_buckler.cast_wisdom");
            var school = ScrollcasterSchoolRuneResolver.resolveSchool(fireRune).orElse(null);
            var power = MagicTools.resolveSchoolPowerAttribute(school);
            long matching = stack.getAttributeModifiers(EquipmentSlot.OFFHAND).get(power).stream()
                    .filter(modifier -> modifier.getAmount() == 0.1D
                            && modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE).count();
            helper.assertTrue(matching == 1, "Duplicate school runes should grant one school power modifier");
            helper.assertTrue(stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(power).isEmpty(),
                    "School rune power should be limited to offhand");
        });
    }

    static void parrycastBucklerKeepsPerfectGuardWindowAndDurabilityRateLimit(GameTestHelper helper) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "parrycast_guard_test");
        var stack = new ItemStack(ItemRegistry.PARRYCAST_BUCKLER.get());
        player.setItemInHand(InteractionHand.OFF_HAND, stack);
        stack.getItem().use(helper.getLevel(), player, InteractionHand.OFF_HAND);
        helper.assertTrue(ApprenticeCodexServerConfig.parrycastBucklerPerfectGuardTicks() == 10,
                "Parrycast perfect guard config should default to ten ticks");
        helper.assertTrue(ParrycastBuckler.isPerfectGuard(player), "Use start should enter perfect guard window");
        helper.assertTrue(ParrycastBuckler.resolveDurabilityCost(12.0F, true) == 1, "Perfect guard should cap durability cost at one");
        helper.assertTrue(ParrycastBuckler.resolveDurabilityCost(12.0F, false) == 13, "Normal guard should keep vanilla durability cost");
        ParrycastBuckler.rememberDurabilityConsumed(stack, 100L);
        helper.assertTrue(ParrycastBuckler.isDurabilitySuppressed(stack, 110L), "Durability should be suppressed through tick ten");
        helper.assertFalse(ParrycastBuckler.isDurabilitySuppressed(stack, 111L), "Durability suppression should expire after tick ten");
        helper.assertTrue(ParrycastBuckler.resolveCooldownReductionTicks(101, 40) == 11,
                "Known maximum cooldown should reduce by a rounded-up ten percent");
        helper.assertTrue(ParrycastBuckler.resolveCooldownReductionTicks(0, 21) == 5,
                "Unknown maximum cooldown should reduce rounded-up twenty percent of remaining time");
        helper.assertFalse(stack.getOrCreateTag().contains("ApprenticeCodexParrycastBucklerAnimationState"),
                "Animation state should not be persisted in the item stack");

        helper.runAfterDelay(11, () -> {
            helper.assertFalse(ParrycastBuckler.isPerfectGuard(player),
                    "Perfect guard window should expire without an animation-driven use restart");
            var event = new ShieldBlockEvent(player, helper.getLevel().damageSources().generic(), 4.0F);
            ImbueShieldBlockCastEvent.onParrycastBucklerBlock(event);
            helper.assertFalse(player.isUsingItem(), "Normal guard should stop Parrycast Buckler use");
            helper.assertTrue(player.getCooldowns().isOnCooldown(stack.getItem()),
                    "Normal guard should apply the release cooldown before stopping use");
            player.gameMode.useItem(player, helper.getLevel(), stack, InteractionHand.OFF_HAND);
            helper.assertFalse(player.isUsingItem(),
                    "Held use input should not restart Parrycast Buckler while the cooldown is active");
            helper.assertFalse(stack.getOrCreateTag().contains("ApprenticeCodexParrycastBucklerAnimationState"),
                    "Stopping use should not persist animation state in the item stack");
            helper.succeed();
        });
    }

    private static void assertFirstRestrictionKey(GameTestHelper helper, java.util.List<net.minecraft.network.chat.Component> lines,
                                                  String expectedKey) {
        helper.assertFalse(lines.isEmpty(), "Parrycast restriction tooltip should not be empty");
        var contents = lines.get(0).getContents();
        helper.assertTrue(contents instanceof TranslatableContents translatable && expectedKey.equals(translatable.getKey()),
                "Unexpected Parrycast restriction tooltip: " + lines.get(0));
    }

    private static void assertTooltipKeyAt(GameTestHelper helper, java.util.List<Component> lines, int index,
                                           String expectedKey) {
        helper.assertTrue(lines.size() > index, "Parrycast tooltip line is missing at index " + index);
        var contents = lines.get(index).getContents();
        helper.assertTrue(contents instanceof TranslatableContents translatable
                        && expectedKey.equals(translatable.getKey()),
                "Unexpected Parrycast tooltip at index " + index + ": " + lines.get(index));
    }
}
