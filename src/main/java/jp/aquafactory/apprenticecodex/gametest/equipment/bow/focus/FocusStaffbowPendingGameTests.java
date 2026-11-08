package jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.support.BowTestScope;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.artisansmash.ArtisanSmash;
import jp.aquafactory.apprenticecodex.spell.artisansmash.ArtisanSmashShellEntity;
import jp.aquafactory.apprenticecodex.spell.higanbana.HiganbanaKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.lethalassault.LethalAssaultRifleEntity;
import jp.aquafactory.apprenticecodex.spell.mantisleap.MantisLeapBladeEntity;
import jp.aquafactory.apprenticecodex.spell.precisionjack.PrecisionJackKnifeEntity;
import jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.setSingleUnlockedSpell;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus.FocusStaffbowTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FocusStaffbowPendingGameTests {
    private FocusStaffbowPendingGameTests() {}

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowShowsLongSummonWeaponDuringPendingCast(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_pending_summon_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, SpellRegistry.SLASH_BLADE.get(), 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        MagicData.getPlayerMagicData(player).setMana(100.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow should enter pending cast for LONG summon spells but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            helper.assertTrue(spellData != null, "Focus Staffbow pending summon test could not resolve spell data capability");
            helper.assertTrue(spellData != null
                            && spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow should keep a pending cast state while charging");
            helper.assertTrue(getOwnedSummonWeapons(helper, player, SlashBladeKatanaEntity.class).size() == 1,
                    "Focus Staffbow should expose the summon weapon during pending charge");
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                    "Focus Staffbow should not consume its catalyst arrow before the LONG cast completes");
            helper.assertTrue(player.isUsingItem(), "Focus Staffbow should still be in use while the summon weapon is pending");
        });
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - SpellRegistry.SLASH_BLADE.get().getEffectiveCastTime(1, player)
                )
        );
        helper.succeedWhen(() -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow pending summon test lost spell data capability");
            helper.assertTrue(spellData != null
                            && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow pending state should clear after the charged cast completes");
            helper.assertTrue(magicData.getAdditionalCastData() == null,
                    "Focus Staffbow charged cast should clear simulated additional cast data after completion");
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 0,
                    "Focus Staffbow should consume exactly one catalyst arrow after the LONG cast completes");
        });
    }

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowUpdatesArtisanSmashSplashRadiusOnChargedRelease(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_artisan_radius_test");
        var spell = (ArtisanSmash) SpellRegistry.ARTISAN_SMASH.get();
        var spellLevel = 1;
        var magicData = MagicData.getPlayerMagicData(player);
        var baseSpellPower = spell.getSpellPower(spellLevel, player);
        var baseSplashRadius = Math.min(2.0F + baseSpellPower / 600.0F, 8.0F);

        helper.succeedIf(() -> {
            var launcher = spell.onCastNoWeapon(helper.getLevel(), spellLevel, player, magicData);
            var spellPowerAttribute = player.getAttribute(AttributeRegistry.SPELL_POWER);
            helper.assertTrue(spellPowerAttribute != null,
                    "Focus Staffbow Artisan Smash test could not resolve spell power attribute");
            var modifier = new AttributeModifier(
                    FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID,
                    2.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            var expectedSplashRadius = -1.0F;
            try {
                if (spellPowerAttribute != null) {
                    spellPowerAttribute.removeModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
                    spellPowerAttribute.addTransientModifier(modifier);
                }
                expectedSplashRadius = Math.min(2.0F + spell.getSpellPower(spellLevel, player) / 600.0F, 8.0F);
                spell.onCastCompleteWithWeapon(helper.getLevel(), spellLevel, player, magicData, false, launcher);
            } finally {
                if (spellPowerAttribute != null) {
                    spellPowerAttribute.removeModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
                }
            }

            var shells = BowTestScope.of(helper).entities(ArtisanSmashShellEntity.class);
            shells.forEach(BowTestScope.of(helper)::assertInside);
            helper.assertTrue(shells.size() == 1,
                    "Focus Staffbow Artisan Smash test should spawn exactly one shell but got " + shells.size());
            var actualSplashRadius = shells.get(0).getSplashRadius();
            helper.assertTrue(actualSplashRadius > baseSplashRadius + 0.01F,
                    "Artisan Smash splash radius should not stay at the pre-charge value: " + actualSplashRadius);
            helper.assertTrue(Math.abs(actualSplashRadius - expectedSplashRadius) < 0.01F,
                    "Artisan Smash splash radius should use charged spell power. expected="
                            + expectedSplashRadius + ", actual=" + actualSplashRadius);
        });
    }

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowReevaluatesSummonWeaponAttackValuesOnChargedCast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();

            var mantisPlayer = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12),
                    "focus_staffbow_mantis_leap_power_test");
            var mantisSpell = SpellRegistry.MANTIS_LEAP.get();
            var mantisMagicData = MagicData.getPlayerMagicData(mantisPlayer);
            mantisSpell.onServerPreCast(level, 1, mantisPlayer, mantisMagicData);
            var mantisBlades = getOwnedSummonWeapons(helper, mantisPlayer, MantisLeapBladeEntity.class);
            helper.assertTrue(mantisBlades.size() == 1,
                    "Focus Staffbow Mantis Leap test should create one pre-cast blade");
            var mantisBlade = mantisBlades.get(0);
            var baseMantisDamage = mantisBlade.getDamageForGameTest();
            castWithDoubledSpellPower(mantisPlayer,
                    () -> mantisSpell.castSpell(level, 1, mantisPlayer, CastSource.SWORD, true));
            helper.assertTrue(mantisBlade.getDamageForGameTest() > baseMantisDamage * 1.5F,
                    "Mantis Leap should reevaluate damage with charged spell power at cast time");

            var slashPlayer = createEquipmentTestPlayer(helper, new BlockPos(15, 2, 12),
                    "focus_staffbow_slash_blade_power_test");
            var slashSpell = SpellRegistry.SLASH_BLADE.get();
            var slashMagicData = MagicData.getPlayerMagicData(slashPlayer);
            slashSpell.onServerPreCast(level, 1, slashPlayer, slashMagicData);
            var katanas = getOwnedSummonWeapons(helper, slashPlayer, SlashBladeKatanaEntity.class);
            helper.assertTrue(katanas.size() == 1,
                    "Focus Staffbow Slash Blade test should create one pre-cast katana");
            var katana = katanas.get(0);
            var baseSlashDamage = katana.getDamageForGameTest();
            castWithDoubledSpellPower(slashPlayer,
                    () -> slashSpell.castSpell(level, 1, slashPlayer, CastSource.SWORD, true));
            helper.assertTrue(katana.getDamageForGameTest() > baseSlashDamage * 1.5F,
                    "Slash Blade should reevaluate damage with charged spell power at cast time");

            var precisionPlayer = createEquipmentTestPlayer(helper, new BlockPos(18, 2, 12),
                    "focus_staffbow_precision_jack_power_test");
            var precisionSpell = SpellRegistry.PRECISION_JACK.get();
            var precisionMagicData = MagicData.getPlayerMagicData(precisionPlayer);
            precisionSpell.onServerPreCast(level, 1, precisionPlayer, precisionMagicData);
            var knives = getOwnedSummonWeapons(helper, precisionPlayer, PrecisionJackKnifeEntity.class);
            helper.assertTrue(knives.size() == 1,
                    "Focus Staffbow Precision Jack test should create one pre-cast knife");
            var knife = knives.get(0);
            var baseLootingBonus = knife.getLootingBonus();
            var baseDuplicateChance = knife.getDuplicateDropChancePercent();
            castWithDoubledSpellPower(precisionPlayer,
                    () -> precisionSpell.castSpell(level, 1, precisionPlayer, CastSource.SWORD, true));
            helper.assertTrue(knife.getLootingBonus() > baseLootingBonus,
                    "Precision Jack should reevaluate looting with charged spell power at cast time");
            helper.assertTrue(knife.getDuplicateDropChancePercent() > baseDuplicateChance,
                    "Precision Jack should reevaluate duplicate chance with charged spell power at cast time");
        });
    }

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowCancelsPendingSummonWeaponBeforeRequiredCharge(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "focus_staffbow_pending_cancel_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, SpellRegistry.SLASH_BLADE.get(), 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        MagicData.getPlayerMagicData(player).setMana(100.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow cancel test should start a pending cast but got " + result.getResult());
        });
        helper.runAtTickTime(2, () ->
                helper.assertTrue(
                        getOwnedSummonWeapons(helper, player, SlashBladeKatanaEntity.class).size() == 1,
                        "Focus Staffbow cancel test should spawn the summon weapon during pending charge"
                )
        );
        helper.runAtTickTime(3, () ->
                bowStack.getItem().releaseUsing(
                        bowStack,
                        helper.getLevel(),
                        player,
                        bowStack.getUseDuration(player) - (SpellRegistry.SLASH_BLADE.get().getEffectiveCastTime(1, player) - 1)
                )
        );
        helper.succeedWhen(() -> {
            var spellData = Capabilities.getSpellDataOrNull(player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spellData != null, "Focus Staffbow cancel test lost spell data capability");
            helper.assertTrue(spellData != null
                            && !spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE).isActive(),
                    "Focus Staffbow should clear the pending state when released before the required charge");
            helper.assertTrue(getOwnedSummonWeapons(helper, player, SlashBladeKatanaEntity.class).isEmpty(),
                    "Focus Staffbow should remove the simulated summon weapon when the charge is cancelled");
            helper.assertTrue(magicData.getAdditionalCastData() == null,
                    "Focus Staffbow should clear simulated additional cast data when the charge is cancelled");
            helper.assertTrue(getFocusStaffbowArrowCount(player) == 1,
                    "Focus Staffbow should keep its catalyst arrow when the LONG cast is cancelled early");
        });
    }

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowLethalAssaultWaitsForReleaseBeforeFiring(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12),
                "focus_staffbow_lethal_assault_wait_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = SpellRegistry.LETHAL_ASSAULT.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        MagicData.getPlayerMagicData(player).setMana(1000.0F);

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow Lethal Assault should start charging but got " + result.getResult());
        });
        helper.runAtTickTime(12, () -> {
            var rifles = getOwnedSummonWeapons(helper, player, LethalAssaultRifleEntity.class);
            helper.assertTrue(rifles.size() == 1,
                    "Focus Staffbow should keep one Lethal Assault rifle visible while charging");
            helper.assertFalse(rifles.get(0).hasStartedFiringForGameTest(),
                    "Focus Staffbow Lethal Assault rifle should stay idle before release");
        });
        helper.runAtTickTime(13, () -> bowStack.getItem().releaseUsing(
                bowStack,
                helper.getLevel(),
                player,
                bowStack.getUseDuration(player) - 12
        ));
        helper.runAtTickTime(14, () -> {
            var rifles = getOwnedSummonWeapons(helper, player, LethalAssaultRifleEntity.class);
            helper.assertTrue(rifles.size() == 1 && rifles.get(0).hasStartedFiringForGameTest(),
                    "Focus Staffbow Lethal Assault rifle should start firing after release completes the cast");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.focus_staffbow_continuous")
    public static void focusStaffbowHiganbanaWaitsForReleaseBeforeSlashing(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12),
                "focus_staffbow_higanbana_wait_test");
        var bowStack = new ItemStack(ItemRegistry.FOCUS_STAFFBOW.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        var spell = SpellRegistry.HIGANBANA.get();
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, bowStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
        setFocusStaffbowArrowCatalyst(player, new ItemStack(Items.ARROW, 1));
        MagicData.getPlayerMagicData(player).setMana(1000.0F);
        var baseDamage = (1.0F + spell.getSpellPower(1, player) / 100.0F)
                * ApprenticeCodexServerConfig.damageMultiplier(
                DamageMultiplierKey.HIGANBANA);
        var summonedPosition = new Vec3[1];
        var summonedYaw = new float[1];

        helper.runAtTickTime(1, () -> {
            var result = bowStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Focus Staffbow Higanbana should start charging but got " + result.getResult());
        });
        helper.runAtTickTime(2, () -> {
            var katanas = getOwnedSummonWeapons(helper, player, HiganbanaKatanaEntity.class);
            helper.assertTrue(katanas.size() == 1,
                    "Focus Staffbow Higanbana should create one pre-cast katana");
            summonedPosition[0] = katanas.get(0).position();
            summonedYaw[0] = katanas.get(0).getYRot();
            player.setPos(player.getX() + 4.0D, player.getY(), player.getZ() + 4.0D);
            player.setYRot(player.getYRot() + 90.0F);
        });
        helper.runAtTickTime(30, () -> {
            var katanas = getOwnedSummonWeapons(helper, player, HiganbanaKatanaEntity.class);
            helper.assertTrue(katanas.size() == 1,
                    "Focus Staffbow should keep one Higanbana katana visible while charging");
            var katana = katanas.get(0);
            helper.assertTrue(katana.getRemainingSlashCount() == 0,
                    "Focus Staffbow Higanbana should stay idle before release");
            helper.assertTrue(katana.position().distanceTo(summonedPosition[0]) < 1.0E-6D
                            && Math.abs(katana.getYRot() - summonedYaw[0]) < 1.0E-4F,
                    "Focus Staffbow Higanbana should stay at its summoned position while charging");
        });
        helper.runAtTickTime(45, () -> bowStack.getItem().releaseUsing(
                bowStack,
                helper.getLevel(),
                player,
                bowStack.getUseDuration(player) - 44
        ));
        helper.runAtTickTime(46, () -> {
            var katanas = getOwnedSummonWeapons(helper, player, HiganbanaKatanaEntity.class);
            helper.assertTrue(katanas.size() == 1 && katanas.get(0).getRemainingSlashCount() == 4,
                    "Focus Staffbow Higanbana should start its four-slash sequence after release");
            helper.assertTrue(katanas.get(0).getDamageForGameTest() > baseDamage,
                    "Focus Staffbow Higanbana should use charged spell power for damage");
            helper.succeed();
        });
    }

    private static void castWithDoubledSpellPower(LivingEntity caster, Runnable cast) {
        var spellPowerAttribute = caster.getAttribute(AttributeRegistry.SPELL_POWER);
        if (spellPowerAttribute == null) {
            throw new IllegalStateException("Spell power attribute is unavailable");
        }
        var modifier = new AttributeModifier(
                FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID,
                1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        spellPowerAttribute.removeModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
        spellPowerAttribute.addTransientModifier(modifier);
        try {
            cast.run();
        } finally {
            spellPowerAttribute.removeModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
        }
    }
}
