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
import jp.aquafactory.apprenticecodex.item.continuouscast.ContinuousCastDurationSimulation;
import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshield;
import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshieldRuntime;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.ArrayList;

final class BulwarkGreatshieldGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private BulwarkGreatshieldGameTestScenarios() {
    }

    static void bulwarkGreatshieldKeepsCoreItemAndEnchantmentContract(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.BULWARK_GREATSHIELD.get());
            var item = (BulwarkGreatshield) stack.getItem();
            var tooltipLines = new ArrayList<Component>();
            item.appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltipLines, TooltipFlag.Default.NORMAL);
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
            var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            helper.assertTrue(item.supportsEnchantment(stack, enchantments.getOrThrow(Enchantments.UNBREAKING)),
                    "Bulwark Greatshield should accept shield durability enchantments");
            helper.assertTrue(item.supportsEnchantment(stack,
                            enchantments.getOrThrow(jp.aquafactory.apprenticecodex.enchantment.Enchantments.TRANSCENDENCE)),
                    "Bulwark Greatshield should accept Transcendence");
            helper.assertTrue(item.supportsEnchantment(stack,
                            enchantments.getOrThrow(jp.aquafactory.apprenticecodex.enchantment.Enchantments.WISDOM)),
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

            var modifiers = stack.getAttributeModifiers().modifiers();
            helper.assertTrue(modifiers.stream()
                            .anyMatch(entry -> entry.slot().equals(EquipmentSlotGroup.OFFHAND)
                                    && entry.attribute().equals(AttributeRegistry.SPELL_RESIST)
                                    && entry.modifier().amount() == BulwarkGreatshield.GENERIC_SPELL_RESIST
                                    && entry.modifier().operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    "Bulwark Greatshield should grant generic spell resist in offhand");
            helper.assertFalse(modifiers.stream()
                            .anyMatch(entry -> entry.slot().equals(EquipmentSlotGroup.MAINHAND)
                                    && entry.attribute().equals(AttributeRegistry.SPELL_RESIST)
                                    && entry.modifier().amount() == BulwarkGreatshield.GENERIC_SPELL_RESIST),
                    "Bulwark Greatshield should not grant generic spell resist in mainhand");
        });
    }

    static void bulwarkGreatshieldCalibrationSupportsThreeDistinctSchoolRunes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "bulwark_greatshield_calibration_test");
            var stack = new ItemStack(ItemRegistry.BULWARK_GREATSHIELD.get());
            var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
            var iceRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.ICE_RUNE.get());
            var holyRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.HOLY_RUNE.get());
            var wisdomShard = new ItemStack(ItemRegistry.WISDOM_SHARD.get());
            var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
            menu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).set(stack);

            helper.assertTrue(menu.isAdjustmentSlotEnabled(0), "Bulwark adjustment slot 0 should be enabled");
            helper.assertTrue(menu.isAdjustmentSlotEnabled(1), "Bulwark adjustment slot 1 should be enabled");
            helper.assertTrue(menu.isAdjustmentSlotEnabled(2), "Bulwark adjustment slot 2 should be enabled");
            helper.assertFalse(menu.isAdjustmentSlotEnabled(3), "Bulwark should expose exactly three adjustment slots");
            var adjustmentSlot = menu.getSlot(SpellCalibrationBenchMenu.ADJUSTMENT_MENU_SLOT_START);
            helper.assertTrue(adjustmentSlot.mayPlace(fireRune), "Bulwark should accept a school rune");
            helper.assertTrue(adjustmentSlot.mayPlace(wisdomShard), "Bulwark should accept Wisdom Shard");

            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(stack, 0, fireRune),
                    "Bulwark should store its first school rune");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(stack, 1, fireRune),
                    "Bulwark should reject a rune for an already inserted School ID");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(stack, 1, iceRune),
                    "Bulwark should accept a rune for a different School ID");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(stack, 2, holyRune),
                    "Bulwark should fill its third slot with another School ID");
            helper.assertTrue(BulwarkGreatshield.getResolvedCalibrationSchools(stack).size() == 3,
                    "Bulwark should resolve all three inserted school runes");

            var modifiers = stack.getAttributeModifiers().modifiers();
            for (var school : BulwarkGreatshield.getResolvedCalibrationSchools(stack)) {
                var schoolResist = MagicTools.resolveSchoolResistAttribute(school);
                helper.assertTrue(schoolResist != null, "Inserted rune should resolve a school resist attribute");
                helper.assertTrue(modifiers.stream()
                                .anyMatch(entry -> entry.slot().equals(EquipmentSlotGroup.OFFHAND)
                                        && entry.attribute().value() == schoolResist
                                        && entry.modifier().amount() == BulwarkGreatshield.SCHOOL_SPELL_RESIST
                                        && entry.modifier().operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                        "Each school rune should add 0.5 school spell resist");
            }
            helper.assertTrue(modifiers.stream()
                            .anyMatch(entry -> entry.slot().equals(EquipmentSlotGroup.OFFHAND)
                                    && entry.attribute().equals(AttributeRegistry.SPELL_RESIST)
                                    && entry.modifier().amount() == BulwarkGreatshield.GENERIC_SPELL_RESIST),
                    "School runes must not replace generic spell resist");

            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(stack, 2, wisdomShard),
                    "Bulwark should allow Wisdom Shard to replace a school rune");
            helper.assertTrue(BulwarkGreatshield.hasWisdomShardAdjustment(stack),
                    "Wisdom Shard should be detected outside the first adjustment slot");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(stack, 1, wisdomShard),
                    "Bulwark should reject a duplicate Wisdom Shard");
            var tooltipLines = new ArrayList<Component>();
            stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltipLines, TooltipFlag.Default.NORMAL);
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
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        stack.enchant(enchantments.getOrThrow(jp.aquafactory.apprenticecodex.enchantment.Enchantments.TRANSCENDENCE), 1);
        stack.enchant(enchantments.getOrThrow(jp.aquafactory.apprenticecodex.enchantment.Enchantments.WISDOM), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        var spellLevelEvent = new ModifySpellLevelEvent(spell, player, 1, 1);
        TranscendenceSpellLevelEvent.onModifySpellLevel(spellLevelEvent);
        helper.assertTrue(spellLevelEvent.getLevel() == 2,
                itemName + " Transcendence should increase the imbued spell level from 1 to 2");

        var experienceEvent = new BlockDropsEvent(
                helper.getLevel(),
                new BlockPos(5, 2, 0),
                Blocks.STONE.defaultBlockState(),
                null,
                new ArrayList<>(),
                player,
                stack
        );
        experienceEvent.setDroppedExperience(5);
        WisdomExperienceDropEvent.onBlockDrops(experienceEvent);
        helper.assertTrue(experienceEvent.getDroppedExperience() == 6,
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

    static void continuousShieldCastDurationsProgressMonotonically(GameTestHelper helper) {
        var spell = SpellRegistry.FIRE_BREATH_SPELL.get();
        var expectedCastDuration = ContinuousCastDurationSimulation.normalizeCastDuration(spell.getCastTime(1));
        helper.assertTrue(ContinuousCastDurationSimulation.computeRemaining(20, 25L) == -5,
                "Managed continuous cast duration should continue below zero after the normal duration");

        var bulwarkPlayer = BowGameTestSupport.createEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "bulwark_continuous_duration_test"
        );
        var bulwarkStack = createContinuousCastShieldStack(ItemRegistry.BULWARK_GREATSHIELD.get());
        bulwarkPlayer.setItemInHand(InteractionHand.OFF_HAND, bulwarkStack);
        bulwarkStack.getItem().use(helper.getLevel(), bulwarkPlayer, InteractionHand.OFF_HAND);
        var bulwarkMagicData = MagicData.getPlayerMagicData(bulwarkPlayer);
        helper.assertTrue(bulwarkMagicData != null, "Bulwark duration test requires MagicData");
        bulwarkMagicData.setMana(10000.0F);

        var reflectcastPlayer = BowGameTestSupport.createEquipmentTestPlayer(
                helper, new BlockPos(2, 2, 0), "reflectcast_continuous_duration_test"
        );
        var reflectcastStack = createContinuousCastShieldStack(ItemRegistry.REFLECTCAST_SHIELD.get());
        SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                reflectcastStack,
                0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
        );
        reflectcastPlayer.setItemInHand(InteractionHand.OFF_HAND, reflectcastStack);
        reflectcastStack.getItem().use(helper.getLevel(), reflectcastPlayer, InteractionHand.OFF_HAND);
        var reflectcastMagicData = MagicData.getPlayerMagicData(reflectcastPlayer);
        helper.assertTrue(reflectcastMagicData != null, "Reflectcast duration test requires MagicData");
        reflectcastMagicData.setMana(10000.0F);

        var startedAt = helper.getLevel().getGameTime();
        BulwarkGreatshieldRuntime.tryStartContinuousCast(
                bulwarkPlayer, bulwarkStack, InteractionHand.OFF_HAND
        );
        helper.assertTrue(ReflectcastShieldRuntime.tryTriggerSpell(
                        reflectcastPlayer, reflectcastStack, InteractionHand.OFF_HAND),
                "Reflectcast duration test should start its continuous cast");
        assertSimulatedCastDuration(
                helper, "Bulwark", bulwarkMagicData, expectedCastDuration, 0L
        );
        assertSimulatedCastDuration(
                helper, "Reflectcast", reflectcastMagicData, expectedCastDuration, 0L
        );

        helper.runAtTickTime(10, () -> {
            var elapsedTicks = helper.getLevel().getGameTime() - startedAt;
            BulwarkGreatshieldRuntime.tickContinuousCast(bulwarkPlayer, bulwarkStack);
            ReflectcastShieldRuntime.tickContinuousCast(reflectcastPlayer, reflectcastStack);
            assertSimulatedCastDuration(
                    helper, "Bulwark", bulwarkMagicData, expectedCastDuration, elapsedTicks
            );
            assertSimulatedCastDuration(
                    helper, "Reflectcast", reflectcastMagicData, expectedCastDuration, elapsedTicks
            );
        });
        helper.runAtTickTime(20, () -> {
            var elapsedTicks = helper.getLevel().getGameTime() - startedAt;
            BulwarkGreatshieldRuntime.tickContinuousCast(bulwarkPlayer, bulwarkStack);
            ReflectcastShieldRuntime.tickContinuousCast(reflectcastPlayer, reflectcastStack);
            assertSimulatedCastDuration(
                    helper, "Bulwark", bulwarkMagicData, expectedCastDuration, elapsedTicks
            );
            assertSimulatedCastDuration(
                    helper, "Reflectcast", reflectcastMagicData, expectedCastDuration, elapsedTicks
            );
            BulwarkGreatshieldRuntime.finishUse(bulwarkPlayer);
            ReflectcastShieldRuntime.finishUse(reflectcastPlayer);
            helper.succeed();
        });
    }

    static void continuousShieldCastCleanupPreservesUseAndClearsLogoutState(GameTestHelper helper) {
        var bulwarkPlayer = BowGameTestSupport.createEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "bulwark_continuous_cleanup_test"
        );
        var bulwarkStack = createContinuousCastShieldStack(ItemRegistry.BULWARK_GREATSHIELD.get());
        var bulwarkBeforeCast = bulwarkStack.copy();
        bulwarkPlayer.setItemInHand(InteractionHand.OFF_HAND, bulwarkStack);
        bulwarkStack.getItem().use(helper.getLevel(), bulwarkPlayer, InteractionHand.OFF_HAND);
        var bulwarkMagicData = MagicData.getPlayerMagicData(bulwarkPlayer);
        helper.assertTrue(bulwarkMagicData != null, "Bulwark cleanup test requires MagicData");
        bulwarkMagicData.setMana(1000.0F);
        BulwarkGreatshieldRuntime.tryStartContinuousCast(
                bulwarkPlayer, bulwarkStack, InteractionHand.OFF_HAND
        );
        helper.assertTrue(bulwarkMagicData.isCasting(), "Bulwark should start its continuous cast");
        bulwarkMagicData.setMana(0.0F);

        var reflectcastPlayer = BowGameTestSupport.createEquipmentTestPlayer(
                helper, new BlockPos(2, 2, 0), "reflectcast_continuous_cleanup_test"
        );
        var reflectcastStack = createContinuousCastShieldStack(ItemRegistry.REFLECTCAST_SHIELD.get());
        SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                reflectcastStack,
                0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
        );
        var reflectcastBeforeCast = reflectcastStack.copy();
        reflectcastPlayer.setItemInHand(InteractionHand.OFF_HAND, reflectcastStack);
        reflectcastStack.getItem().use(helper.getLevel(), reflectcastPlayer, InteractionHand.OFF_HAND);
        var reflectcastMagicData = MagicData.getPlayerMagicData(reflectcastPlayer);
        helper.assertTrue(reflectcastMagicData != null, "Reflectcast cleanup test requires MagicData");
        reflectcastMagicData.setMana(1000.0F);
        helper.assertTrue(ReflectcastShieldRuntime.tryTriggerSpell(
                        reflectcastPlayer, reflectcastStack, InteractionHand.OFF_HAND),
                "Reflectcast should start its continuous cast");
        reflectcastMagicData.setMana(0.0F);

        helper.runAfterDelay(10, () -> {
            BulwarkGreatshieldRuntime.tickContinuousCast(bulwarkPlayer, bulwarkStack);
            ReflectcastShieldRuntime.tickContinuousCast(reflectcastPlayer, reflectcastStack);

            helper.assertFalse(bulwarkMagicData.isCasting(),
                    "Bulwark mana exhaustion should end only its continuous cast");
            helper.assertTrue(bulwarkPlayer.isUsingItem(),
                    "Bulwark mana exhaustion should keep the shield raised");
            helper.assertFalse(reflectcastMagicData.isCasting(),
                    "Reflectcast mana exhaustion should end only its continuous cast");
            helper.assertTrue(reflectcastPlayer.isUsingItem(),
                    "Reflectcast mana exhaustion should keep the shield raised");
            helper.assertTrue(Utils.isSameItemSameComponentsIgnoreDurability(bulwarkBeforeCast, bulwarkStack),
                    "Bulwark continuous cleanup must not rewrite shield NBT");
            helper.assertTrue(Utils.isSameItemSameComponentsIgnoreDurability(reflectcastBeforeCast, reflectcastStack),
                    "Reflectcast continuous cleanup must not rewrite shield NBT");

            bulwarkPlayer.stopUsingItem();
            bulwarkMagicData.getPlayerCooldowns().clearCooldowns();
            bulwarkMagicData.setMana(1000.0F);
            bulwarkStack.getItem().use(helper.getLevel(), bulwarkPlayer, InteractionHand.OFF_HAND);
            BulwarkGreatshieldRuntime.tryStartContinuousCast(
                    bulwarkPlayer, bulwarkStack, InteractionHand.OFF_HAND
            );
            helper.assertTrue(bulwarkMagicData.isCasting(),
                    "Bulwark logout cleanup test should start from an active cast");
            BulwarkGreatshieldRuntime.onLogout(new PlayerEvent.PlayerLoggedOutEvent(bulwarkPlayer));
            helper.assertFalse(bulwarkMagicData.isCasting(),
                    "Bulwark logout should clear the persisted casting state");

            reflectcastPlayer.stopUsingItem();
            reflectcastMagicData.getPlayerCooldowns().clearCooldowns();
            reflectcastMagicData.setMana(1000.0F);
            reflectcastStack.getItem().use(helper.getLevel(), reflectcastPlayer, InteractionHand.OFF_HAND);
            helper.assertTrue(ReflectcastShieldRuntime.tryTriggerSpell(
                            reflectcastPlayer, reflectcastStack, InteractionHand.OFF_HAND),
                    "Reflectcast logout cleanup test should start from an active cast");
            ReflectcastShieldRuntime.onLogout(new PlayerEvent.PlayerLoggedOutEvent(reflectcastPlayer));
            helper.assertFalse(reflectcastMagicData.isCasting(),
                    "Reflectcast logout should clear the persisted casting state");
            helper.succeed();
        });
    }

    static void continuousShieldDeathClearsRuntimeWithNormalCooldown(GameTestHelper helper) {
        var spell = SpellRegistry.FIRE_BREATH_SPELL.get();
        var bulwarkPlayer = BowGameTestSupport.createEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "bulwark_continuous_death_test"
        );
        var bulwarkStack = createContinuousCastShieldStack(ItemRegistry.BULWARK_GREATSHIELD.get());
        bulwarkPlayer.setItemInHand(InteractionHand.OFF_HAND, bulwarkStack);
        bulwarkStack.getItem().use(helper.getLevel(), bulwarkPlayer, InteractionHand.OFF_HAND);
        var bulwarkMagicData = MagicData.getPlayerMagicData(bulwarkPlayer);
        helper.assertTrue(bulwarkMagicData != null, "Bulwark death cleanup test requires MagicData");
        bulwarkMagicData.setMana(1000.0F);
        BulwarkGreatshieldRuntime.tryStartContinuousCast(
                bulwarkPlayer, bulwarkStack, InteractionHand.OFF_HAND
        );
        helper.assertTrue(bulwarkMagicData.isCasting(), "Bulwark death cleanup should start from an active cast");
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(
                bulwarkPlayer, helper.getLevel().damageSources().generic()
        ));
        helper.assertFalse(bulwarkMagicData.isCasting(), "Bulwark death should end its simulated casting state");
        helper.assertTrue(bulwarkMagicData.getPlayerCooldowns().getSpellCooldowns().containsKey(spell.getSpellId()),
                "Bulwark death should retain Iron's normal continuous spell cooldown");

        bulwarkMagicData.getPlayerCooldowns().clearCooldowns();
        bulwarkStack.getItem().use(helper.getLevel(), bulwarkPlayer, InteractionHand.OFF_HAND);
        BulwarkGreatshieldRuntime.tryStartContinuousCast(
                bulwarkPlayer, bulwarkStack, InteractionHand.OFF_HAND
        );
        helper.assertTrue(bulwarkMagicData.isCasting(),
                "Bulwark should start a fresh continuous cast after death cleanup");
        BulwarkGreatshieldRuntime.finishUse(bulwarkPlayer);

        var reflectcastPlayer = BowGameTestSupport.createEquipmentTestPlayer(
                helper, new BlockPos(2, 2, 0), "reflectcast_continuous_death_test"
        );
        var reflectcastStack = createContinuousCastShieldStack(ItemRegistry.REFLECTCAST_SHIELD.get());
        SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                reflectcastStack,
                0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
        );
        reflectcastPlayer.setItemInHand(InteractionHand.OFF_HAND, reflectcastStack);
        reflectcastStack.getItem().use(helper.getLevel(), reflectcastPlayer, InteractionHand.OFF_HAND);
        var reflectcastMagicData = MagicData.getPlayerMagicData(reflectcastPlayer);
        helper.assertTrue(reflectcastMagicData != null, "Reflectcast death cleanup test requires MagicData");
        reflectcastMagicData.setMana(1000.0F);
        helper.assertTrue(ReflectcastShieldRuntime.tryTriggerSpell(
                        reflectcastPlayer, reflectcastStack, InteractionHand.OFF_HAND),
                "Reflectcast death cleanup should start from an active cast");
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(
                reflectcastPlayer, helper.getLevel().damageSources().generic()
        ));
        helper.assertFalse(reflectcastMagicData.isCasting(),
                "Reflectcast death should end its simulated casting state");
        helper.assertTrue(reflectcastMagicData.getPlayerCooldowns().getSpellCooldowns().containsKey(spell.getSpellId()),
                "Reflectcast death should retain Iron's normal continuous spell cooldown");

        reflectcastMagicData.getPlayerCooldowns().clearCooldowns();
        reflectcastStack.getItem().use(helper.getLevel(), reflectcastPlayer, InteractionHand.OFF_HAND);
        helper.assertTrue(ReflectcastShieldRuntime.tryTriggerSpell(
                        reflectcastPlayer, reflectcastStack, InteractionHand.OFF_HAND),
                "Reflectcast should start a fresh continuous cast after death cleanup");
        ReflectcastShieldRuntime.finishUse(reflectcastPlayer);
        helper.succeed();
    }

    static void continuousShieldCreativeFinishSkipsCooldown(GameTestHelper helper) {
        var spell = SpellRegistry.FIRE_BREATH_SPELL.get();
        var bulwarkPlayer = BowGameTestSupport.createEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "bulwark_creative_continuous_finish_test"
        );
        bulwarkPlayer.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
        helper.assertTrue(bulwarkPlayer.isCreative(), "Bulwark creative cooldown test requires creative mode");
        var bulwarkStack = createContinuousCastShieldStack(ItemRegistry.BULWARK_GREATSHIELD.get());
        bulwarkPlayer.setItemInHand(InteractionHand.OFF_HAND, bulwarkStack);
        bulwarkStack.getItem().use(helper.getLevel(), bulwarkPlayer, InteractionHand.OFF_HAND);
        var bulwarkMagicData = MagicData.getPlayerMagicData(bulwarkPlayer);
        helper.assertTrue(bulwarkMagicData != null, "Bulwark creative finish test requires MagicData");
        bulwarkMagicData.setMana(1000.0F);
        BulwarkGreatshieldRuntime.tryStartContinuousCast(
                bulwarkPlayer, bulwarkStack, InteractionHand.OFF_HAND
        );
        helper.assertTrue(bulwarkMagicData.isCasting(),
                "Bulwark creative finish test should start from an active cast");
        helper.assertFalse(bulwarkMagicData.getPlayerCooldowns().isOnCooldown(spell),
                "Bulwark creative finish test should not begin with a cooldown");
        var originalCreativeCooldown = io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.get();
        try {
            io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.set(false);
            io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.clearCache();
            BulwarkGreatshieldRuntime.finishUse(bulwarkPlayer);
        } finally {
            io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.set(originalCreativeCooldown);
            io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.clearCache();
        }
        helper.assertFalse(bulwarkMagicData.getPlayerCooldowns().isOnCooldown(spell),
                "Bulwark creative finish should respect disabled creative cooldowns");

        var reflectcastPlayer = BowGameTestSupport.createEquipmentTestPlayer(
                helper, new BlockPos(2, 2, 0), "reflectcast_creative_continuous_finish_test"
        );
        reflectcastPlayer.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
        helper.assertTrue(reflectcastPlayer.isCreative(), "Reflectcast creative cooldown test requires creative mode");
        var reflectcastStack = createContinuousCastShieldStack(ItemRegistry.REFLECTCAST_SHIELD.get());
        SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                reflectcastStack,
                0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
        );
        reflectcastPlayer.setItemInHand(InteractionHand.OFF_HAND, reflectcastStack);
        reflectcastStack.getItem().use(helper.getLevel(), reflectcastPlayer, InteractionHand.OFF_HAND);
        var reflectcastMagicData = MagicData.getPlayerMagicData(reflectcastPlayer);
        helper.assertTrue(reflectcastMagicData != null, "Reflectcast creative finish test requires MagicData");
        reflectcastMagicData.setMana(1000.0F);
        helper.assertTrue(ReflectcastShieldRuntime.tryTriggerSpell(
                        reflectcastPlayer, reflectcastStack, InteractionHand.OFF_HAND),
                "Reflectcast creative finish test should start from an active cast");
        helper.assertFalse(reflectcastMagicData.getPlayerCooldowns().isOnCooldown(spell),
                "Reflectcast creative finish test should not begin with a cooldown");
        try {
            io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.set(false);
            io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.clearCache();
            ReflectcastShieldRuntime.finishUse(reflectcastPlayer);
        } finally {
            io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.set(originalCreativeCooldown);
            io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.clearCache();
        }
        helper.assertFalse(reflectcastMagicData.getPlayerCooldowns().isOnCooldown(spell),
                "Reflectcast creative finish should respect disabled creative cooldowns");
        helper.succeed();
    }

    private static ItemStack createContinuousCastShieldStack(net.minecraft.world.item.Item shieldItem) {
        var stack = new ItemStack(shieldItem);
        var spellContainer = ISpellContainer.create(1, false, false).mutableCopy();
        spellContainer.addSpellAtIndex(SpellRegistry.FIRE_BREATH_SPELL.get(), 1, 0, false);
        ISpellContainer.set(stack, spellContainer.toImmutable());
        return stack;
    }

    private static void assertSimulatedCastDuration(
            GameTestHelper helper,
            String itemName,
            MagicData magicData,
            int expectedCastDuration,
            long elapsedTicks
    ) {
        var expectedRemaining = ContinuousCastDurationSimulation.computeRemaining(
                expectedCastDuration, elapsedTicks
        );
        helper.assertTrue(magicData.getCastDuration() == expectedCastDuration,
                itemName + " continuous cast should expose the spell's base cast duration: "
                        + magicData.getCastDuration());
        helper.assertTrue(magicData.getCastDurationRemaining() == expectedRemaining,
                itemName + " continuous cast remaining duration should decrease monotonically: "
                        + magicData.getCastDurationRemaining() + " expected " + expectedRemaining);
    }
}
