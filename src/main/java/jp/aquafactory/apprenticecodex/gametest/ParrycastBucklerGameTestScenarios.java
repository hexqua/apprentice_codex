package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.overlays.SpellSelection;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.shield.ImbueShieldBlockCastEvent;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.shield.ParrycastBuckler;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
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
            helper.assertTrue(item.isValidRepairItem(stack,
                            new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())),
                    "Parrycast Buckler should repair with arcane ingot");
            helper.assertFalse(item.isValidRepairItem(stack, new ItemStack(Items.DIAMOND)),
                    "Parrycast Buckler should not repair with diamond");
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
            helper.assertFalse(item.evaluateCalibrationImbue(stack, 0, new SpellData(SpellRegistry.MANTIS_LEAP.get(), 1))
                            .isUsable(),
                    "Inserted long spell should warn while Silver Ring is absent");
            assertFirstRestrictionKey(helper, item.getImbueRestrictionTooltipLines(stack),
                    "item.apprenticecodex.spellgun.tooltip.restrict_restrict_instant_only");
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(stack, 0,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()));
            helper.assertTrue(item.canUseConfiguredSpell(stack, SpellRegistry.MANTIS_LEAP.get(), 1), "Silver Ring should allow long spells");
            helper.assertTrue(item.evaluateCalibrationImbue(stack, 0, new SpellData(SpellRegistry.MANTIS_LEAP.get(), 1))
                            .isUsable(),
                    "Silver Ring should clear the inserted long spell warning");
            assertFirstRestrictionKey(helper, item.getImbueRestrictionTooltipLines(stack),
                    "item.apprenticecodex.spellgun.tooltip.restrict_restrict_not_continuous");
        });
    }

    static void parrycastBucklerKeepsThreeAdjustmentsWithoutSchoolRunePower(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "parrycast_calibration_test");
            var stack = new ItemStack(ItemRegistry.PARRYCAST_BUCKLER.get());
            var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
            menu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).set(stack);
            for (int i = 0; i < 3; i++) helper.assertTrue(menu.isAdjustmentSlotEnabled(i), "Parrycast adjustment slot should be enabled: " + i);
            helper.assertFalse(menu.isAdjustmentSlotEnabled(3), "Parrycast should expose exactly three adjustment slots");

            var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(stack, 0, fireRune),
                    "Parrycast should reject School Runes");
            var calibration = stack.getOrCreateTagElement("ParrycastBucklerCalibration");
            var legacyAdjustments = new net.minecraft.nbt.ListTag();
            var legacyItems = new ItemStack[]{
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()),
                    new ItemStack(ItemRegistry.WISDOM_SHARD.get()),
                    fireRune
            };
            for (var slot = 0; slot < legacyItems.length; ++slot) {
                var legacyEntry = new CompoundTag();
                legacyEntry.putInt("Slot", slot);
                legacyEntry.put("Item", legacyItems[slot].save(new CompoundTag()));
                legacyAdjustments.add(legacyEntry);
            }
            calibration.put("Adjustments", legacyAdjustments);
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.getCalibrationAdjustment(stack, 2).is(fireRune.getItem()),
                    "Legacy School Rune should remain readable for removal");
            helper.assertTrue(stack.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.OFFHAND)
                            .get(AttributeRegistry.FIRE_SPELL_POWER.get()).isEmpty(),
                    "Legacy School Rune should not grant school spell power");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack,
                            0,
                            new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())),
                    "Parrycast should still accept Silver Ring and migrate all legacy slots");
            helper.assertTrue(ParrycastBuckler.hasWisdomShard(stack),
                    "Untouched Wisdom Shard should survive the first legacy mutation");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 2, ItemStack.EMPTY),
                    "Legacy School Rune should remain removable");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.getCalibrationAdjustment(stack, 2).isEmpty(),
                    "Removing a legacy School Rune should clear its slot");

            var tooltipLines = new ArrayList<Component>();
            stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
            assertTooltipKeyAt(helper, tooltipLines, 0,
                    "item.apprenticecodex.parrycast_buckler.desc");
            assertTooltipKeyAt(helper, tooltipLines, 1,
                    "item.apprenticecodex.parrycast_buckler.cast_wisdom");
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

    static void parrycastWisdomOnlyReducesAllCooldownsWhenSelectedSpellIsCoolingDown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var otherSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();

            var ready = createWisdomParryContext(
                    helper,
                    new BlockPos(0, 2, 0),
                    "parrycast_wisdom_ready_test",
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get()
            );
            MagicHelper.MAGIC_MANAGER.addCooldown(ready.player(), otherSpell, ready.castSource());
            int readyOtherBefore = cooldownRemaining(ready.magicData(), otherSpell);
            triggerPerfectGuard(ready);
            helper.assertTrue(cooldownRemaining(ready.magicData(), otherSpell) == readyOtherBefore,
                    "Wisdom should not reduce other cooldowns while the selected spell is ready");

            var invalid = createWisdomParryContext(
                    helper,
                    new BlockPos(2, 2, 0),
                    "parrycast_wisdom_invalid_test",
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get()
            );
            MagicHelper.MAGIC_MANAGER.addCooldown(invalid.player(), otherSpell, invalid.castSource());
            int invalidOtherBefore = cooldownRemaining(invalid.magicData(), otherSpell);
            triggerPerfectGuard(invalid);
            helper.assertTrue(cooldownRemaining(invalid.magicData(), otherSpell) == invalidOtherBefore,
                    "Wisdom should not reduce other cooldowns when the selected spell cannot be used");

            var coolingDown = createWisdomParryContext(
                    helper,
                    new BlockPos(4, 2, 0),
                    "parrycast_wisdom_cooldown_test",
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get()
            );
            MagicHelper.MAGIC_MANAGER.addCooldown(
                    coolingDown.player(), coolingDown.selectedSpell(), coolingDown.castSource()
            );
            MagicHelper.MAGIC_MANAGER.addCooldown(coolingDown.player(), otherSpell, coolingDown.castSource());
            int selectedBefore = cooldownRemaining(coolingDown.magicData(), coolingDown.selectedSpell());
            int otherBefore = cooldownRemaining(coolingDown.magicData(), otherSpell);
            triggerPerfectGuard(coolingDown);
            helper.assertTrue(cooldownRemaining(coolingDown.magicData(), coolingDown.selectedSpell()) < selectedBefore,
                    "Wisdom should reduce the selected spell cooldown while it is active");
            helper.assertTrue(cooldownRemaining(coolingDown.magicData(), otherSpell) < otherBefore,
                    "Wisdom should reduce all cooldowns when the selected spell is cooling down");
        });
    }

    private static WisdomParryContext createWisdomParryContext(
            GameTestHelper helper,
            BlockPos position,
            String name,
            AbstractSpell selectedSpell
    ) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(helper, position, name);
        var buckler = new ItemStack(ItemRegistry.PARRYCAST_BUCKLER.get());
        SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(buckler, 0, new ItemStack(ItemRegistry.WISDOM_SHARD.get()));
        var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, BowGameTestSupport.createSpellScroll(selectedSpell));
        ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, buckler);
        player.setItemInHand(InteractionHand.OFF_HAND, gauntlet);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Wisdom Parrycast test requires MagicData");
        magicData.setMana(1000.0F);
        magicData.getSyncedData().setSpellSelection(new SpellSelection(SpellSelectionManager.OFFHAND, 0));
        var selection = new SpellSelectionManager(player).getSelection();
        helper.assertTrue(selection != null && selection.spellData.getSpell() == selectedSpell,
                "Wisdom Parrycast test should resolve the selected offhand spell");
        return new WisdomParryContext(player, buckler, magicData, selectedSpell, selection.getCastSource());
    }

    private static void triggerPerfectGuard(WisdomParryContext context) {
        context.buckler().getItem().use(context.player().level(), context.player(), InteractionHand.MAIN_HAND);
        ((ParrycastBuckler) context.buckler().getItem()).handlePerfectGuard(
                context.player(), context.buckler(), InteractionHand.MAIN_HAND
        );
    }

    private static int cooldownRemaining(MagicData magicData, AbstractSpell spell) {
        var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
        return cooldown == null ? 0 : cooldown.getCooldownRemaining();
    }

    private record WisdomParryContext(
            ServerPlayer player,
            ItemStack buckler,
            MagicData magicData,
            AbstractSpell selectedSpell,
            io.redspace.ironsspellbooks.api.spells.CastSource castSource
    ) {
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
