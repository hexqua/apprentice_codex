package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ChargecastCatalystbookServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationImbueState;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbook;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbookCastEvents;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbookClientCastIntent;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbookPresentationResolver;
import jp.aquafactory.apprenticecodex.item.SneakSelectionState;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbookStartSoundContext;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.IChargecastStaffbowIncompatibleSpell;
import jp.aquafactory.apprenticecodex.spell.lethalassault.LethalAssaultRifleEntity;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Set;
import java.util.List;

final class ChargecastCatalystbookGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private ChargecastCatalystbookGameTestScenarios() {
    }

    static void storesOnlyInstantSpellsAndExpandsToFourSlots(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var book = ItemRegistry.CHARGECAST_CATALYSTBOOK.get().getDefaultInstance();
            var item = (ChargecastCatalystbook) book.getItem();
            var instant = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var longSpell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANTIS_LEAP.get();

            helper.assertTrue(ChargecastCatalystbook.getEnabledCalibrationScrollSlotCount(book) == 1,
                    "Chargecast Catalystbook should start with one spell slot");
            helper.assertFalse(item.isSneakSelectionUiEnabled(book),
                    "One-slot Chargecast Catalystbook should leave sneak and hotbar scrolling untouched");
            helper.assertTrue(item.evaluateCalibrationImbue(book, 0, new SpellData(instant, 1))
                            == SpellCalibrationImbueState.ACCEPTED_USABLE,
                    "Chargecast Catalystbook should accept instant spells");
            helper.assertTrue(item.evaluateCalibrationImbue(book, 0, new SpellData(longSpell, 1))
                            == SpellCalibrationImbueState.REJECTED,
                    "Chargecast Catalystbook should reject non-instant spells regardless of recast");
            helper.assertTrue(book.getItem() instanceof RestrictedSpellImbuableItem,
                    "Chargecast Catalystbook should expose the shared imbue restriction interface");
            helper.assertTrue(jp.aquafactory.apprenticecodex.utility.SpellGunSpellValidator
                            .isUnsupportedArcaneAnvilSpell(book, createSpellScroll(instant)),
                    "Arcane Anvil should not imbue Chargecast Catalystbook");

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "chargecast_catalystbook_calibration_test");
            var menu = createSpellCalibrationBenchMenuWithTarget(player, book);
            helper.assertFalse(menu.getImbueRestrictionTooltipLines().isEmpty(),
                    "Chargecast Catalystbook should expose Calibration Bench restriction warnings");
            helper.assertFalse(menu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START).mayPlace(
                            createSpellScroll(longSpell)
                    ),
                    "Chargecast Catalystbook should reject non-instant scrolls in the Calibration Bench");

            for (var slot = 0; slot < ChargecastCatalystbook.CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
                helper.assertTrue(item.trySetCalibrationAdjustment(
                                book, slot,
                                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
                        ),
                        "Each Lesser Spell Slot Upgrade should be accepted");
                if (slot == 0) {
                    helper.assertTrue(item.isSneakSelectionUiEnabled(book),
                            "The first slot upgrade should enable the sneak selection UI");
                }
            }
            helper.assertTrue(ChargecastCatalystbook.getEnabledCalibrationScrollSlotCount(book) == 4,
                    "Three slot upgrades should expand the book to four spell slots");

            ChargecastCatalystbook.setCalibrationScroll(book, 3, createSpellScroll(instant));
            ChargecastCatalystbook.setSelectedScrollIndex(book, 3);
            helper.assertTrue(ChargecastCatalystbook.getSelectedScrollIndex(book) == 3,
                    "The internal selected spell should be stored independently");
            helper.assertTrue(ChargecastCatalystbook.getSelectedSpellData(book).getSpell() == instant,
                    "Only the selected internal spell should be projected");

            var firebolt = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBOLT_SPELL.get();
            var icicle = io.redspace.ironsspellbooks.api.registry.SpellRegistry.ICICLE_SPELL.get();
            ChargecastCatalystbook.setCalibrationScroll(book, 0, createSpellScroll(firebolt));
            ChargecastCatalystbook.setCalibrationScroll(book, 2, createSpellScroll(icicle));
            var selectionViews = ChargecastCatalystbook.getSelectionViews(book);
            helper.assertTrue(selectionViews.get(0).displayName().getString().endsWith(" 1"),
                    "Chargecast Catalystbook selection label should append the spell level number");
            helper.assertTrue(java.util.Objects.equals(
                            selectionViews.get(0).displayName().getStyle().getColor(),
                            firebolt.getSchoolType().getDisplayName().getStyle().getColor()
                    ),
                    "Chargecast Catalystbook selection label should use the spell school color");
            var selectionState = SneakSelectionState.open(
                    net.minecraft.world.InteractionHand.MAIN_HAND,
                    selectionViews,
                    ChargecastCatalystbook.getSelectedScrollIndex(book)
            );
            helper.assertTrue(selectionState.selectedItemIndex() == 3,
                    "Selection state should start from the item selection");
            selectionState = selectionState.move(1);
            helper.assertTrue(selectionState.selectedItemIndex() == 0,
                    "Selection state should wrap forward");
            selectionState = selectionState.move(1);
            helper.assertTrue(selectionState.selectedItemIndex() == 2,
                    "Selection state should skip empty slots");
            selectionState = selectionState.move(1);
            helper.assertTrue(selectionState.selectedItemIndex() == 3,
                    "Repeated wheel input should continue from the updated cursor");
            selectionState = selectionState.move(-1);
            helper.assertTrue(selectionState.selectedItemIndex() == 2,
                    "Selection state should move backward from the current cursor");

            ChargecastCatalystbook.setSelectedScrollIndex(book, 0);
            selectionState = selectionState.refresh(
                    ChargecastCatalystbook.getSelectionViews(book),
                    ChargecastCatalystbook.getSelectedScrollIndex(book)
            );
            helper.assertTrue(selectionState.selectedItemIndex() == 0,
                    "Selection refresh should follow the ItemStack rather than a stale UI cursor");

            ChargecastCatalystbookClientCastIntent.mark(player.getUUID(), book, instant);
            var otherCasterId = java.util.UUID.randomUUID();
            helper.assertFalse(ChargecastCatalystbookClientCastIntent.activateIfMatches(otherCasterId, book, instant),
                    "Another player's cast-start must not consume the local pending cast");
            helper.assertFalse(ChargecastCatalystbookClientCastIntent.isActive(otherCasterId, book, instant),
                    "Another player's cast must not become the local active chargecast");
            helper.assertTrue(ChargecastCatalystbookClientCastIntent.activateIfMatches(
                            player.getUUID(), book, instant
                    ),
                    "The matching local cast-start must activate the pending cast");
            helper.assertTrue(ChargecastCatalystbookClientCastIntent.isActive(player.getUUID(), book, instant),
                    "The activated local cast must be available to HUD and recast presentation");
            helper.assertFalse(item.shouldOverrideCastStartAnimation(book, instant),
                    "The identity-free Item API must not decide whether a cast belongs to the local player");
            helper.assertFalse(item.shouldOverrideCastFinishAnimation(book, instant),
                    "The identity-free Item API must not decide whether a completion belongs to the local player");
            helper.assertTrue(item.getCastFinishAnimation(book, instant, false)
                            .equals(instant.getCastStartAnimation()),
                    "Chargecast completion should replay the instant spell's own animation");
            helper.assertTrue(ChargecastCatalystbookClientCastIntent.isActive(player.getUUID(), book, instant),
                    "Resolving a completion animation must not mutate the local active cast");
            ChargecastCatalystbookClientCastIntent.finishIfMatches(otherCasterId, instant.getSpellId());
            helper.assertTrue(ChargecastCatalystbookClientCastIntent.isActive(player.getUUID(), book, instant),
                    "Another player's completion must not clear the local active chargecast");

            var mageLight = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT.get();
            helper.assertTrue(item.getCastFinishAnimation(book, mageLight, false)
                            .equals(mageLight.getCastFinishAnimation()),
                    "Chargecast completion should prefer a concrete finish animation");
            helper.assertTrue(ChargecastCatalystbookPresentationResolver.shouldDeferStartSound(mageLight),
                    "A start-only instant spell sound should be deferred until chargecast completion");

            var manaSlash = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
            var noCompletionAnimation = item.getCastFinishAnimation(book, manaSlash, false);
            helper.assertTrue(noCompletionAnimation.getForPlayer().isEmpty() && !noCompletionAnimation.isPass,
                    "Chargecast completion should explicitly stop when the spell has no concrete animation");

            var autoMagnet = jp.aquafactory.apprenticecodex.registry.SpellRegistry.AUTO_MAGNET.get();
            helper.assertFalse(ChargecastCatalystbookPresentationResolver.shouldDeferStartSound(autoMagnet),
                    "A spell with both start and finish sounds should keep its start sound timing");
            var soundWasSuppressed = ChargecastCatalystbookStartSoundContext.callSuppressed(
                    player.getUUID(),
                    () -> ChargecastCatalystbookStartSoundContext.shouldSuppress(mageLight, player)
            );
            helper.assertTrue(soundWasSuppressed,
                    "The deferred start sound should be suppressed inside the chargecast context");
            helper.assertFalse(ChargecastCatalystbookStartSoundContext.shouldSuppress(mageLight, player),
                    "The deferred start sound suppression should not leak after initiation");

            ChargecastCatalystbookClientCastIntent.finishIfMatches(player.getUUID(), mageLight.getSpellId());
            helper.assertTrue(ChargecastCatalystbookClientCastIntent.isActive(player.getUUID(), book, instant),
                    "Another spell's completion must not clear the active chargecast");
            ChargecastCatalystbookClientCastIntent.finishIfMatches(player.getUUID(), instant.getSpellId());
            helper.assertFalse(ChargecastCatalystbookClientCastIntent.isActive(player.getUUID(), book, instant),
                    "The matching completion must clear the active chargecast after an item swap");

            ChargecastCatalystbookClientCastIntent.mark(player.getUUID(), book, instant);
            ChargecastCatalystbookClientCastIntent.mark(player.getUUID(), book, mageLight);
            helper.assertFalse(ChargecastCatalystbookClientCastIntent.activateIfMatches(
                            player.getUUID(), book, instant
                    ),
                    "A newer input must replace a stale pending cast");
            helper.assertTrue(ChargecastCatalystbookClientCastIntent.activateIfMatches(
                            player.getUUID(), book, mageLight
                    ),
                    "The replacement pending cast must still activate");
            ChargecastCatalystbookClientCastIntent.finishIfMatches(player.getUUID(), mageLight.getSpellId());

            ChargecastCatalystbookClientCastIntent.mark(player.getUUID(), book, instant);
            ChargecastCatalystbookClientCastIntent.clear();
            helper.assertFalse(ChargecastCatalystbookClientCastIntent.activateIfMatches(
                            player.getUUID(), book, instant
                    ),
                    "Explicit cleanup must discard a pending cast");
        });
    }

    static void appliesAdjustmentAndAttributePolicies(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ChargecastCatalystbook) ItemRegistry.CHARGECAST_CATALYSTBOOK.get();
            var book = item.getDefaultInstance();
            helper.assertTrue(book.is(io.redspace.ironsspellbooks.util.ModTags.CAN_BE_UPGRADED),
                    "Chargecast Catalystbook should accept upgrade orbs");
            helper.assertTrue(item.getEnchantmentValue(book) == 22,
                    "Chargecast Catalystbook enchantability should be 22");
            helper.assertTrue(item.directlyApplicableAttributeEnchantments().equals(Set.of(
                            AttributeEnchantmentType.ALACRITY,
                            AttributeEnchantmentType.REFLUX,
                            AttributeEnchantmentType.RESERVOIR,
                            AttributeEnchantmentType.TENSE
                    )),
                    "Chargecast Catalystbook should expose exactly four attribute enchantments");

            var mainhand = item.getDefaultAttributeModifiers(book);
            helper.assertTrue(mainhand.modifiers().stream().anyMatch(entry ->
                            entry.slot().test(EquipmentSlot.MAINHAND)
                                    && entry.attribute().equals(AttributeRegistry.SPELL_POWER)
                                    && entry.modifier().operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                                    && Math.abs(entry.modifier().amount() - 0.10D) < 0.000001D),
                    "The default book should grant +10% generic spell power in mainhand");

            var amplified = item.getDefaultInstance();
            helper.assertTrue(item.trySetCalibrationAdjustment(
                            amplified, 0, new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get())),
                    "Silver Spell Amplifier should be accepted once");
            helper.assertFalse(item.trySetCalibrationAdjustment(
                            amplified, 1, new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get())),
                    "Silver Spell Amplifier should reject duplicates");
            helper.assertTrue(item.getDefaultAttributeModifiers(amplified).modifiers().stream().noneMatch(entry ->
                            entry.slot().test(EquipmentSlot.MAINHAND)
                                    && entry.attribute().equals(AttributeRegistry.SPELL_POWER)),
                    "Silver Spell Amplifier should remove the mainhand spell power modifier");
            helper.assertTrue(item.getDefaultAttributeModifiers(amplified).modifiers().stream().anyMatch(entry ->
                            entry.slot().test(EquipmentSlot.OFFHAND)
                                    && entry.attribute().equals(AttributeRegistry.SPELL_POWER)
                                    && Math.abs(entry.modifier().amount() - 0.10D) < 0.000001D),
                    "Silver Spell Amplifier should move spell power to offhand");

            var schoolTuned = item.getDefaultInstance();
            helper.assertTrue(item.trySetCalibrationAdjustment(
                            schoolTuned, 0,
                            new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get())
                    ),
                    "One school rune should be accepted");
            var schoolModifiers = item.getDefaultAttributeModifiers(schoolTuned);
            var firePower = MagicTools.resolveSchoolPowerAttribute(
                    io.redspace.ironsspellbooks.api.registry.SchoolRegistry.FIRE.get()
            );
            helper.assertTrue(schoolModifiers.modifiers().stream().noneMatch(entry ->
                            entry.attribute().equals(AttributeRegistry.SPELL_POWER)),
                    "A school rune should remove generic spell power");
            helper.assertTrue(firePower != null && schoolModifiers.modifiers().stream().anyMatch(entry ->
                            entry.attribute().equals(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(firePower))
                                    && entry.modifier().operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                                    && Math.abs(entry.modifier().amount() - 0.15D) < 0.000001D),
                    "A school rune should grant +15% spell power for its school");
        });
    }

    static void rejectsPreCastSpellPowerDependentSpells(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var mageLight = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MAGE_LIGHT.get();
            var linearBuild = jp.aquafactory.apprenticecodex.registry.SpellRegistry.LINEAR_BUILD.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "chargecast_precast_power_reject_test");
            helper.assertTrue(mageLight instanceof IChargecastStaffbowIncompatibleSpell,
                    "Mage Light should opt out of delayed spell-power casts");
            helper.assertTrue(linearBuild instanceof IChargecastStaffbowIncompatibleSpell,
                    "Linear Build should opt out of delayed spell-power casts");
            assertTranslatableKey(
                    helper,
                    ChargecastCatalystbook.createRejectedSpellMessage(mageLight.getDisplayName(player)),
                    "ui.apprenticecodex.chargecast.reject_spell",
                    "Chargecast Catalystbook should use its permanent rejection message"
            );

            var book = new ItemStack(ItemRegistry.CHARGECAST_CATALYSTBOOK.get());
            ChargecastCatalystbook.setCalibrationScroll(book, 0, createSpellScroll(mageLight));
            player.setItemInHand(InteractionHand.MAIN_HAND, book);
            MagicData.getPlayerMagicData(player).setMana(100.0F);

            var result = book.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Chargecast Catalystbook should reject Mage Light but got " + result.getResult());
            helper.assertFalse(MagicData.getPlayerMagicData(player).isCasting(),
                    "Rejected Mage Light should not begin a managed cast");
        });
    }

    static void spellDenylistRejectsConfiguredSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var values = ChargecastCatalystbookServerConfig.Values.DEFAULT;
            try (var ignored = ApprenticeCodexServerConfig.useChargecastCatalystbookConfigOverrideForGameTest(
                    new ChargecastCatalystbookServerConfig.Values(
                            values.castTimeTicks(),
                            values.spellPowerMultiplier(),
                            values.silverRingCastTimeBonusFactor(),
                            List.of(spell.getSpellResource())
                    )
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                        "chargecast_denylist_test");
                var book = new ItemStack(ItemRegistry.CHARGECAST_CATALYSTBOOK.get());
                ChargecastCatalystbook.setCalibrationScroll(book, 0, createSpellScroll(spell));
                player.setItemInHand(InteractionHand.MAIN_HAND, book);
                MagicData.getPlayerMagicData(player).setMana(100.0F);

                var result = book.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                        "Chargecast Catalystbook should reject denylisted spells but got " + result.getResult());
                helper.assertFalse(MagicData.getPlayerMagicData(player).isCasting(),
                        "A denylisted Chargecast spell should not begin casting");
                assertTranslatableKey(
                        helper,
                        ChargecastCatalystbook.createSpellDenylistedMessage(spell.getDisplayName(player)),
                        "ui.apprenticecodex.chargecast.spell_denylisted",
                        "Chargecast Catalystbook should use its denylist message"
                );
            }
        });
    }

    static void lethalAssaultWaitsForChargecastCompletionBeforeFiring(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "chargecast_lethal_assault_wait_test");
        var book = new ItemStack(ItemRegistry.CHARGECAST_CATALYSTBOOK.get());
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.LETHAL_ASSAULT.get();
        ChargecastCatalystbook.setCalibrationScroll(book, 0, createSpellScroll(spell));
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        MagicData.getPlayerMagicData(player).setMana(1000.0F);

        helper.runAtTickTime(1, () -> {
            var result = book.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Chargecast Lethal Assault should start charging but got " + result.getResult());
        });
        helper.runAtTickTime(12, () -> {
            var rifles = BowGameTestSupport.getOwnedSummonWeapons(helper, player, LethalAssaultRifleEntity.class);
            helper.assertTrue(rifles.size() == 1,
                    "Chargecast Lethal Assault should keep one pre-cast rifle while charging");
            helper.assertFalse(rifles.get(0).hasStartedFiringForGameTest(),
                    "Chargecast Lethal Assault rifle should not enter its firing state before completion");
        });
        helper.runAtTickTime(13, () -> {
            var magicData = MagicData.getPlayerMagicData(player);
            ChargecastCatalystbookCastEvents.castWithPowerBonus(
                    spell, helper.getLevel(), 1, player, CastSource.SWORD, true
            );
            spell.onServerCastComplete(helper.getLevel(), 1, player, magicData, false);

            var rifles = BowGameTestSupport.getOwnedSummonWeapons(helper, player, LethalAssaultRifleEntity.class);
            helper.assertTrue(rifles.size() == 1 && rifles.get(0).hasStartedFiringForGameTest(),
                    "Chargecast Lethal Assault rifle should start firing when the charged cast completes");
            helper.succeed();
        });
    }

    static void lethalAssaultCancellationRemovesPreCastRifle(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "chargecast_lethal_assault_cancel_test");
        var book = new ItemStack(ItemRegistry.CHARGECAST_CATALYSTBOOK.get());
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.LETHAL_ASSAULT.get();
        ChargecastCatalystbook.setCalibrationScroll(book, 0, createSpellScroll(spell));
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        MagicData.getPlayerMagicData(player).setMana(1000.0F);

        helper.runAtTickTime(1, () -> book.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND));
        helper.runAtTickTime(3, () -> Utils.serverSideCancelCast(player));
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(BowGameTestSupport.getOwnedSummonWeapons(
                            helper, player, LethalAssaultRifleEntity.class
                    ).isEmpty(),
                    "Cancelling Chargecast Lethal Assault should remove its idle pre-cast rifle");
            helper.succeed();
        });
    }

    static void wisdomWheelCastOnlyRequiresHeldBookForExternalSpell(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "chargecast_wisdom_swap_test");
        var book = new ItemStack(ItemRegistry.CHARGECAST_CATALYSTBOOK.get());
        var internalSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var externalSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBOLT_SPELL.get();
        ChargecastCatalystbook.setCalibrationScroll(book, 0, createSpellScroll(internalSpell));
        var item = (ChargecastCatalystbook) book.getItem();
        helper.assertTrue(item.trySetCalibrationAdjustment(
                        book, 0, new ItemStack(ItemRegistry.WISDOM_SHARD.get())
                ), "Wisdom Shard should be accepted for the swap-cancellation test");

        var magicData = MagicData.getPlayerMagicData(player);
        magicData.getSyncedData();
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        magicData.initiateCast(externalSpell, 1, 20, CastSource.SWORD,
                io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND);
        magicData.setPlayerCastingItem(book.copy());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        ChargecastCatalystbookCastEvents.onPlayerTick(
                new PlayerTickEvent.Post(player)
        );
        helper.assertFalse(magicData.isCasting(),
                "Switching away should cancel a Wisdom cast borrowed from another wheel source");

        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        magicData.initiateCast(internalSpell, 1, 20, CastSource.SWORD,
                io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND);
        magicData.setPlayerCastingItem(book.copy());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        ChargecastCatalystbookCastEvents.onPlayerTick(
                new PlayerTickEvent.Post(player)
        );
        helper.assertTrue(magicData.isCasting(),
                "The book's own projected wheel spell should remain governed by Iron's standard cancellation");
        Utils.serverSideCancelCast(player);
        helper.succeed();
    }
}
