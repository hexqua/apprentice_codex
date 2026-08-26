package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.curios.manashieldcharm.ManaShieldCharm;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.damagesource.CombatRules;

import static jp.aquafactory.apprenticecodex.gametest.EnchantmentApplicationGameTestSupport.assertExactEnchantmentSurfaces;

final class ManaShieldCharmGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private ManaShieldCharmGameTestScenarios() {
    }

    static void manaShieldCharmUsesCharmSlotAndAppearsInCreativeTab(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            helper.assertTrue(stack.is(CURIOS_CHARM),
                    "Mana Shield Charm should be tagged for the Curios charm slot");
            helper.assertTrue(stack.getItem() instanceof ManaShieldCharm,
                    "Mana Shield Charm should resolve to the dedicated curio item implementation");
        });
    }

    static void manaShieldCharmKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> assertExactEnchantmentSurfaces(
                helper,
                new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()),
                registryIdSet(
                        EnchantmentRegistry.SHELL,
                        EnchantmentRegistry.SYNCHRONIZATION,
                        EnchantmentRegistry.NEUTRALIZATION
                ),
                "Mana Shield Charm"
        ));
    }

    static void manaShieldCharmExclusiveEnchantmentsStayMutuallyExclusive(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var shell = EnchantmentRegistry.SHELL.get();
            var synchronization = EnchantmentRegistry.SYNCHRONIZATION.get();
            var neutralization = EnchantmentRegistry.NEUTRALIZATION.get();

            helper.assertFalse(shell.isCompatibleWith(synchronization),
                    "Shell and Synchronization should stay mutually exclusive");
            helper.assertFalse(shell.isCompatibleWith(neutralization),
                    "Shell and Neutralization should stay mutually exclusive");
            helper.assertFalse(synchronization.isCompatibleWith(neutralization),
                    "Synchronization and Neutralization should stay mutually exclusive");
        });
    }

    static void manaShieldCharmFullyNegatesDamageAndPreservesArmorDurability(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_full_negate_test");
            equipCurio(player, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
            assertManaShieldCharmEquipped(helper, player, "full negate");

            var chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
            player.setItemSlot(EquipmentSlot.CHEST, chestplate);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Mana Shield Charm full negate test could not resolve player mana data");
            magicData.setMana(100.0F);
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();
            var event = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 2.0F);
            helper.assertTrue(event.isCanceled(),
                    "Mana Shield Charm should cancel the fully absorbed LivingAttackEvent");
            helper.assertTrue(Math.abs(player.getHealth() - initialHealth) < 1.0e-4F,
                    "Mana Shield Charm should keep health unchanged after fully negating damage");
            helper.assertTrue(Math.abs(magicData.getMana() - 50.0F) < 1.0e-4F,
                    "Mana Shield Charm should spend 50 mana to negate 2 damage but got " + magicData.getMana());
            helper.assertTrue(chestplate.getDamageValue() == 0,
                    "Mana Shield Charm should not damage armor durability on a fully negated hit");
            helper.assertFalse(getManaShieldCharmState(player).cooldownActive,
                    "Mana Shield Charm should stay active while mana remains after a fully negated hit");
        });
    }

    static void manaShieldCharmBurnedOutFullNegateCancelsHitAndStartsCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_burned_out_full_negate_test");
            equipCurio(player, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
            assertManaShieldCharmEquipped(helper, player, "burned out full negate");

            var chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
            player.setItemSlot(EquipmentSlot.CHEST, chestplate);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Mana Shield Charm burned-out full negate test could not resolve player mana data");
            magicData.setMana(25.0F);
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();

            var firstEvent = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 1.0F);
            helper.assertTrue(firstEvent.isCanceled(),
                    "Mana Shield Charm should cancel the hit even when the last full negate burns out the shield");
            helper.assertTrue(Math.abs(player.getHealth() - initialHealth) < 1.0e-4F,
                    "Mana Shield Charm should keep health unchanged when the last full negate burns out the shield");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Mana Shield Charm should clamp mana to zero after the last full negate but got " + magicData.getMana());
            helper.assertTrue(getManaShieldCharmState(player).cooldownActive,
                    "Mana Shield Charm should enter cooldown immediately after the last full negate burns out the shield");
            helper.assertTrue(player.invulnerableTime >= 20,
                    "Mana Shield Charm should still apply vanilla-style invulnerability time when the last full negate burns out the shield");
            helper.assertTrue(chestplate.getDamageValue() == 0,
                    "Mana Shield Charm should not damage armor durability when the burned-out hit is still fully negated");

            var secondEvent = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 1.0F);
            helper.assertFalse(secondEvent.isCanceled(),
                    "Mana Shield Charm should let damage through while its depletion cooldown is active");
            helper.assertTrue(Math.abs(player.getHealth() - initialHealth) < 1.0e-4F,
                    "Posting the cooldown-path event should not directly mutate health");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Mana Shield Charm should not spend additional mana during the burned-out full-negate i-frame but got " + magicData.getMana());
        });
    }

    static void manaShieldCharmLowManaBurnedOutFullNegateStillCancelsHit(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_low_mana_burnout_test");
            equipCurio(player, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
            assertManaShieldCharmEquipped(helper, player, "low mana burned out full negate");

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Low mana Mana Shield Charm test could not resolve player mana data");
            magicData.setMana(24.0F);
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();

            var event = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 1.0F);
            helper.assertTrue(event.isCanceled(),
                    "Mana Shield Charm should still cancel a 1 damage hit when only 24 mana remains before cooldown");
            helper.assertTrue(Math.abs(player.getHealth() - initialHealth) < 1.0e-4F,
                    "Low mana Mana Shield Charm burnout should still leave health unchanged");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Low mana Mana Shield Charm burnout should clamp mana to zero but got " + magicData.getMana());
            helper.assertTrue(getManaShieldCharmState(player).cooldownActive,
                    "Low mana Mana Shield Charm burnout should enter cooldown immediately");
            helper.assertTrue(player.invulnerableTime >= 20,
                    "Low mana Mana Shield Charm burnout should still apply vanilla-style invulnerability time");
        });
    }

    static void manaShieldCharmDoesNotRespendManaDuringVanillaStyleIFrame(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_iframe_test");
            equipCurio(player, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
            assertManaShieldCharmEquipped(helper, player, "iframe");

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Mana Shield Charm iframe test could not resolve player mana data");
            magicData.setMana(100.0F);

            var firstEvent = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 1.0F);
            helper.assertTrue(firstEvent.isCanceled(),
                    "Mana Shield Charm should cancel the first fully negated hit before starting its i-frame");
            helper.assertTrue(player.invulnerableTime >= 20,
                    "Mana Shield Charm should apply vanilla-style invulnerability time after a fully negated hit");
            helper.assertTrue(Math.abs(magicData.getMana() - 75.0F) < 1.0e-4F,
                    "Mana Shield Charm should spend 25 mana on the first fully negated hit but got " + magicData.getMana());

            var secondEvent = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 1.0F);
            helper.assertTrue(secondEvent.isCanceled(),
                    "Mana Shield Charm should also cancel repeated contact damage during its vanilla-style i-frame");
            helper.assertTrue(Math.abs(magicData.getMana() - 75.0F) < 1.0e-4F,
                    "Mana Shield Charm should not spend additional mana during its vanilla-style i-frame but got " + magicData.getMana());
        });
    }

    static void manaShieldCharmPartialReductionEntersCooldownAndKeepsArmorMitigation(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var armored = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_partial_armor_test");
            var unarmored = createTrackedEquipmentTestPlayer(helper, new BlockPos(3, 2, 0), "mana_shield_partial_plain_test");

            equipCurio(armored, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
            equipCurio(unarmored, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
            assertManaShieldCharmEquipped(helper, armored, "partial armored");
            assertManaShieldCharmEquipped(helper, unarmored, "partial unarmored");
            armored.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));

            var armoredMana = MagicData.getPlayerMagicData(armored);
            var unarmoredMana = MagicData.getPlayerMagicData(unarmored);
            helper.assertTrue(armoredMana != null && unarmoredMana != null,
                    "Mana Shield Charm partial reduction test could not resolve player mana data");
            armoredMana.setMana(40.0F);
            unarmoredMana.setMana(40.0F);
            var armoredEvent = postLivingAttackEventForGameTest(armored, helper.getLevel().damageSources().lava(), 3.0F);
            var unarmoredEvent = postLivingAttackEventForGameTest(unarmored, helper.getLevel().damageSources().lava(), 3.0F);
            var expectedRemainingMana = resolveExpectedBarrierManaAfterHitForGameTest(3.0F, 40.0F);

            helper.assertTrue(Math.abs(armoredMana.getMana() - expectedRemainingMana) < 1.0e-4F,
                    "Mana Shield Charm partial reduction should apply the one-hit low mana rescue consistently for the armored player"
                            + " expectedMana=" + expectedRemainingMana
                            + " actualMana=" + armoredMana.getMana());
            helper.assertTrue(Math.abs(unarmoredMana.getMana() - expectedRemainingMana) < 1.0e-4F,
                    "Mana Shield Charm partial reduction should apply the one-hit low mana rescue consistently for the unarmored player"
                            + " expectedMana=" + expectedRemainingMana
                            + " actualMana=" + unarmoredMana.getMana());
            helper.assertTrue(armoredEvent.isCanceled(),
                    "Mana Shield Charm partial reduction should cancel the original armored LivingAttackEvent");
            helper.assertTrue(unarmoredEvent.isCanceled(),
                    "Mana Shield Charm partial reduction should cancel the original unarmored LivingAttackEvent");
            helper.assertTrue(getManaShieldCharmState(armored).cooldownActive == (expectedRemainingMana <= 0.0F),
                    "Mana Shield Charm armored partial reduction cooldown should match the rescued remaining mana expectation");
            helper.assertTrue(getManaShieldCharmState(unarmored).cooldownActive == (expectedRemainingMana <= 0.0F),
                    "Mana Shield Charm unarmored partial reduction cooldown should match the rescued remaining mana expectation");
        });
    }

    static void manaShieldCharmCooldownRecoversAtOneHundredMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_recovery_threshold_test");
            equipCurio(player, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
            assertManaShieldCharmEquipped(helper, player, "recovery");

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Mana Shield Charm cooldown recovery test could not resolve player mana data");
            var state = getManaShieldCharmState(player);
            state.reset();
            state.cooldownActive = true;

            magicData.setMana(99.0F);
            var blockedEvent = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 1.0F);
            helper.assertTrue(state.cooldownActive,
                    "Mana Shield Charm should stay disabled below the 100 mana recovery threshold");
            helper.assertFalse(blockedEvent.isCanceled(),
                    "Mana Shield Charm should not cancel the hit while cooldown remains locked below 100 mana");
            helper.assertTrue(Math.abs(magicData.getMana() - 99.0F) < 1.0e-4F,
                    "Mana Shield Charm should not spend mana while cooldown remains locked below 100 mana");

            state.cooldownActive = true;
            magicData.setMana(100.0F);
            var recoveredEvent = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 1.0F);

            helper.assertFalse(state.cooldownActive,
                    "Mana Shield Charm should recover immediately once mana reaches 100");
            helper.assertTrue(recoveredEvent.isCanceled(),
                    "Mana Shield Charm should cancel the recovered hit once the cooldown is lifted");
            helper.assertTrue(Math.abs(magicData.getMana() - 75.0F) < 1.0e-4F,
                    "Mana Shield Charm should spend 25 mana after recovering at the threshold but got " + magicData.getMana());
        });
    }

    static void manaShieldCharmShellUsesAllArmorEffectsAndBypassSkipsThem(GameTestHelper helper) {
        var armored = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_shell_armored_test");
        var unarmored = createTrackedEquipmentTestPlayer(helper, new BlockPos(3, 2, 0), "mana_shield_shell_unarmored_test");
        var bypassArmor = createTrackedEquipmentTestPlayer(helper, new BlockPos(6, 2, 0), "mana_shield_shell_bypass_test");
        var shellCharm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
        shellCharm.enchant(EnchantmentRegistry.SHELL.get(), 1);
        equipCurio(armored, CuriosSlotConstants.CHARM, shellCharm.copy());
        equipCurio(unarmored, CuriosSlotConstants.CHARM, shellCharm.copy());
        equipCurio(bypassArmor, CuriosSlotConstants.CHARM, shellCharm.copy());

        var protection = net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION;
        var head = new ItemStack(Items.IRON_HELMET);
        var chest = new ItemStack(Items.IRON_CHESTPLATE);
        var legs = new ItemStack(Items.IRON_LEGGINGS);
        var boots = new ItemStack(Items.IRON_BOOTS);
        head.enchant(protection, 4);
        chest.enchant(protection, 4);
        legs.enchant(protection, 4);
        boots.enchant(protection, 4);
        armored.setItemSlot(EquipmentSlot.HEAD, head);
        armored.setItemSlot(EquipmentSlot.CHEST, chest);
        armored.setItemSlot(EquipmentSlot.LEGS, legs);
        armored.setItemSlot(EquipmentSlot.FEET, boots);

        var bypassChest = new ItemStack(Items.IRON_CHESTPLATE);
        bypassArmor.setItemSlot(EquipmentSlot.CHEST, bypassChest);

        var armoredMana = MagicData.getPlayerMagicData(armored);
        var unarmoredMana = MagicData.getPlayerMagicData(unarmored);
        var bypassMana = MagicData.getPlayerMagicData(bypassArmor);
        helper.assertTrue(armoredMana != null && unarmoredMana != null && bypassMana != null,
                "Mana Shield Charm Shell test could not resolve player mana data");

        helper.runAtTickTime(1, () -> {
            armoredMana.setMana(500.0F);
            unarmoredMana.setMana(500.0F);
            bypassMana.setMana(500.0F);
            var armoredAvailableMana = armoredMana.getMana();
            armored.invulnerableTime = 0;
            unarmored.invulnerableTime = 0;
            bypassArmor.invulnerableTime = 0;
            var normalSource = helper.getLevel().damageSources().lava();
            var armorReduced = CombatRules.getDamageAfterAbsorb(
                    armored,
                    8.0F,
                    normalSource,
                    getEquippedAttributeTotal(armored, Attributes.ARMOR),
                    getEquippedAttributeTotal(armored, Attributes.ARMOR_TOUGHNESS)
            );
            var fullyReduced = CombatRules.getDamageAfterMagicAbsorb(
                    armorReduced,
                    EnchantmentHelper.getDamageProtection(helper.getLevel(), armored, normalSource)
            );
            var armoredEvent = postLivingAttackEventForGameTest(armored, normalSource, 8.0F);
            var unarmoredEvent = postLivingAttackEventForGameTest(unarmored, normalSource, 8.0F);
            var bypassSource = jp.aquafactory.apprenticecodex.utility.CombatTools.getDamageSource(helper.getLevel(), bypassArmor, DamageTypes.UNITE_LUNA);
            var bypassEvent = postLivingAttackEventForGameTest(bypassArmor, bypassSource, 2.0F);
            helper.assertTrue(armoredEvent.isCanceled() && unarmoredEvent.isCanceled() && bypassEvent.isCanceled(),
                    "Mana Shield Charm Shell test should cancel all intercepted LivingAttackEvent instances");
            var expectedArmoredMana = armoredAvailableMana
                    - 50.0F
                    - 25.0F * countWholeDamageStepsForGameTest(fullyReduced);
            helper.assertTrue(Math.abs(armoredMana.getMana() - expectedArmoredMana) < 1.0e-4F,
                    "Shell should apply armor, toughness, and protection before charging barrier mana"
                            + " reducedDamage=" + fullyReduced
                            + " expectedMana=" + expectedArmoredMana
                            + " actualMana=" + armoredMana.getMana());
            helper.assertTrue(Math.abs(unarmoredMana.getMana()) < 1.0e-4F,
                    "Shell should charge its fixed activation cost and normal barrier cost without armor");
            helper.assertTrue(head.getDamageValue() == 2
                            && chest.getDamageValue() == 2
                            && legs.getDamageValue() == 2
                            && boots.getDamageValue() == 2,
                    "Shell should damage each armor piece by ceil(raw damage / 4)");
            helper.assertTrue(Math.abs(bypassMana.getMana()) < 1.0e-4F,
                    "Shell should charge its fixed cost but skip armor benefits on armor-bypass hits");
            helper.assertTrue(bypassChest.getDamageValue() == 0,
                    "Shell should not damage armor durability on armor-bypass hits");
            helper.succeed();
        });
    }

    static void manaShieldCharmShellActivationCostBurnoutPassesOriginalHit(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_shell_low_mana_test");
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(EnchantmentRegistry.SHELL.get(), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);

            var chestplate = new ItemStack(Items.IRON_CHESTPLATE);
            player.setItemSlot(EquipmentSlot.CHEST, chestplate);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Shell low mana test could not resolve player mana data");
            magicData.setMana(49.0F);
            player.invulnerableTime = 0;
            var source = helper.getLevel().damageSources().lava();
            var event = postLivingAttackEventForGameTest(player, source, 2.0F);
            helper.assertFalse(event.isCanceled(),
                    "Shell should pass the original hit when its forced activation cost depletes all mana");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Shell activation cost should clamp mana to zero");
            helper.assertTrue(getManaShieldCharmState(player).cooldownActive,
                    "Shell activation cost burnout should enter cooldown");
            helper.assertTrue(chestplate.getDamageValue() == 0,
                    "Shell should not damage armor when activation cost burnout prevents its benefit");
        });
    }

    static void manaShieldCharmShellExactActivationCostAppliesEffectAndBurnsOut(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var armored = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_shell_exact_cost_armored_test");
            var bypassArmor = createTrackedEquipmentTestPlayer(helper, new BlockPos(3, 2, 0), "mana_shield_shell_exact_cost_bypass_test");
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var shellCharm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            shellCharm.enchant(enchantmentLookup.getOrThrow(Enchantments.SHELL), 1);
            equipCurio(armored, CuriosSlotConstants.CHARM, shellCharm.copy());
            equipCurio(bypassArmor, CuriosSlotConstants.CHARM, shellCharm.copy());

            var armoredChestplate = new ItemStack(Items.IRON_CHESTPLATE);
            var bypassChestplate = new ItemStack(Items.IRON_CHESTPLATE);
            armored.setItemSlot(EquipmentSlot.CHEST, armoredChestplate);
            bypassArmor.setItemSlot(EquipmentSlot.CHEST, bypassChestplate);

            var armoredMana = MagicData.getPlayerMagicData(armored);
            var bypassMana = MagicData.getPlayerMagicData(bypassArmor);
            helper.assertTrue(armoredMana != null && bypassMana != null,
                    "Shell exact activation cost test could not resolve player mana data");
            armoredMana.setMana(50.0F);
            bypassMana.setMana(50.0F);
            armored.invulnerableTime = 0;
            bypassArmor.invulnerableTime = 0;

            var armoredEvent = postLivingAttackEventForGameTest(
                    armored, helper.getLevel().damageSources().lava(), 2.0F);
            var bypassSource = jp.aquafactory.apprenticecodex.utility.CombatTools.getDamageSource(
                    helper.getLevel(), bypassArmor, DamageTypes.UNITE_LUNA);
            var bypassEvent = postLivingAttackEventForGameTest(bypassArmor, bypassSource, 2.0F);

            helper.assertTrue(armoredEvent.isCanceled(),
                    "Shell should apply armor mitigation when mana exactly pays its activation cost");
            helper.assertTrue(armoredChestplate.getDamageValue() == 1,
                    "Shell should damage armor after activating at its exact mana cost");
            helper.assertTrue(Math.abs(armoredMana.getMana()) < 1.0e-4F,
                    "Shell exact activation cost should leave zero mana after applying armor mitigation");
            helper.assertTrue(getManaShieldCharmState(armored).cooldownActive,
                    "Shell should burn out after its exact activation cost leaves zero mana");

            helper.assertFalse(bypassEvent.isCanceled(),
                    "Shell should pass armor-bypass damage when no mana remains to negate it");
            helper.assertTrue(bypassChestplate.getDamageValue() == 0,
                    "Shell should not damage armor for an armor-bypass hit at its exact activation cost");
            helper.assertTrue(Math.abs(bypassMana.getMana()) < 1.0e-4F,
                    "Shell should remain at zero mana when it negates no armor-bypass damage");
            helper.assertTrue(getManaShieldCharmState(bypassArmor).cooldownActive,
                    "Shell should burn out at zero mana even when it negates no damage");
        });
    }

    static void manaShieldCharmShellChargesActivationCostForFractionalDamage(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_shell_fractional_cost_test");
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(EnchantmentRegistry.SHELL.get(), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Shell fractional cost test could not resolve player mana data");
            magicData.setMana(100.0F);
            player.invulnerableTime = 0;

            var event = postLivingAttackEventForGameTest(
                    player, helper.getLevel().damageSources().lava(), 0.5F);

            helper.assertFalse(event.isCanceled(),
                    "Shell should not cancel fractional damage when no armor or barrier step reduces it");
            helper.assertTrue(Math.abs(magicData.getMana() - 50.0F) < 1.0e-4F,
                    "Shell should charge its fixed activation cost even for fractional damage");
            helper.succeed();
        });
    }

    static void manaShieldCharmSynchronizationUsesHigherCostOnOrdinaryDamage(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_sync_cost_test");
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(EnchantmentRegistry.SYNCHRONIZATION.get(), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Synchronization cost test could not resolve player mana data");
            magicData.setMana(100.0F);
            player.invulnerableTime = 0;

            var source = helper.getLevel().damageSources().lava();
            var event = postLivingAttackEventForGameTest(player, source, 2.0F);
            helper.assertTrue(event.isCanceled(),
                    "Synchronization should cancel ordinary damage when enough mana remains");
            helper.assertTrue(Math.abs(magicData.getMana() - 20.0F) < 1.0e-4F,
                    "Synchronization should cost 40 mana per ordinary damage point by default");
            helper.succeed();
        });
    }

    static void manaShieldCharmSynchronizationDiscountsArmorBypassAndVoidDamage(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var bypassPlayer = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_sync_bypass_test");
            var voidPlayer = createTrackedEquipmentTestPlayer(helper, new BlockPos(3, 2, 0), "mana_shield_sync_void_test");
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(EnchantmentRegistry.SYNCHRONIZATION.get(), 1);
            equipCurio(bypassPlayer, CuriosSlotConstants.CHARM, charm.copy());
            equipCurio(voidPlayer, CuriosSlotConstants.CHARM, charm.copy());

            var bypassMana = MagicData.getPlayerMagicData(bypassPlayer);
            var voidMana = MagicData.getPlayerMagicData(voidPlayer);
            helper.assertTrue(bypassMana != null && voidMana != null,
                    "Synchronization discounted damage test could not resolve player mana data");
            bypassMana.setMana(100.0F);
            voidMana.setMana(100.0F);
            bypassPlayer.invulnerableTime = 0;
            voidPlayer.invulnerableTime = 0;
            var bypassSource = jp.aquafactory.apprenticecodex.utility.CombatTools.getDamageSource(
                    helper.getLevel(), bypassPlayer, DamageTypes.UNITE_LUNA);

            var bypassEvent = postLivingAttackEventForGameTest(bypassPlayer, bypassSource, 2.0F);
            var voidEvent = postLivingAttackEventForGameTest(
                    voidPlayer, helper.getLevel().damageSources().fellOutOfWorld(), 2.0F);

            helper.assertTrue(bypassEvent.isCanceled() && voidEvent.isCanceled(),
                    "Synchronization should intercept eligible armor-bypass and void damage");
            helper.assertTrue(Math.abs(bypassMana.getMana() - 80.0F) < 1.0e-4F
                            && Math.abs(voidMana.getMana() - 80.0F) < 1.0e-4F,
                    "Synchronization should cost 10 mana per eligible damage point by default"
                            + " bypassMana=" + bypassMana.getMana()
                            + " voidMana=" + voidMana.getMana());
            helper.succeed();
        });
    }

    static void manaShieldCharmSynchronizationDoesNotDiscountInvulnerabilityBypass(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_sync_invulnerability_bypass_test");
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(EnchantmentRegistry.SYNCHRONIZATION.get(), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Synchronization invulnerability-bypass test could not resolve player mana data");
            magicData.setMana(100.0F);
            player.invulnerableTime = 0;

            var event = postLivingAttackEventForGameTest(
                    player, helper.getLevel().damageSources().genericKill(), 2.0F);

            helper.assertTrue(event.isCanceled(),
                    "Synchronization should still intercept invulnerability-bypass damage");
            helper.assertTrue(Math.abs(magicData.getMana() - 20.0F) < 1.0e-4F,
                    "Synchronization should not discount invulnerability-bypass damage");
            helper.succeed();
        });
    }

    static void manaShieldCharmSynchronizationReductionDoesNotRecoverMana(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            try (var ignored = ApprenticeCodexServerConfig.useManaShieldCharmConfigOverrideForGameTest(
                    25.0D, 100, 50, 5.0D, 50.0D, 50, 100, 1, 20
            )) {
                var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_sync_zero_cost_test");
                var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
                charm.enchant(EnchantmentRegistry.SYNCHRONIZATION.get(), 1);
                equipCurio(player, CuriosSlotConstants.CHARM, charm);
                var magicData = MagicData.getPlayerMagicData(player);
                helper.assertTrue(magicData != null, "Synchronization zero-cost test could not resolve player mana data");
                magicData.setMana(10.0F);
                player.invulnerableTime = 0;
                var source = jp.aquafactory.apprenticecodex.utility.CombatTools.getDamageSource(
                        helper.getLevel(), player, DamageTypes.UNITE_LUNA);

                var event = postLivingAttackEventForGameTest(player, source, 2.0F);

                helper.assertTrue(event.isCanceled(),
                        "Synchronization should absorb eligible damage when its configured cost clamps to zero");
                helper.assertTrue(Math.abs(magicData.getMana() - 10.0F) < 1.0e-4F,
                        "Synchronization cost reduction should not turn into mana recovery");
                helper.assertFalse(getManaShieldCharmState(player).cooldownActive,
                        "Zero-cost Synchronization should not enter depletion cooldown");
                helper.succeed();
            }
        });
    }

    static void manaShieldCharmNeutralizationAbsorbsBypassArmorDamageDuringCooldown(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_neutralization_test");
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(EnchantmentRegistry.NEUTRALIZATION.get(), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Neutralization test could not resolve player mana data");
            magicData.setMana(10.0F);
            var state = getManaShieldCharmState(player);
            state.reset();
            state.cooldownActive = true;
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();
            var source = jp.aquafactory.apprenticecodex.utility.CombatTools.getDamageSource(helper.getLevel(), player, DamageTypes.UNITE_LUNA);

            var event = postLivingAttackEventForGameTest(player, source, 2.0F);

            helper.assertFalse(event.isCanceled(),
                    "Neutralization should not cancel armor-bypass damage while cooldown is active");
            helper.assertTrue(Math.abs(player.getHealth() - initialHealth) < 1.0e-4F,
                    "Neutralization should fully negate armor-bypass damage");
            helper.assertTrue(Math.abs(magicData.getMana() - 10.0F) < 1.0e-4F,
                    "Neutralization should no longer recover mana from armor-bypass damage");
            helper.assertTrue(state.cooldownActive,
                    "Neutralization should not clear cooldown until mana reaches the normal recovery threshold");
            helper.succeed();
        });
    }

    static void manaShieldCharmFreeManaCostConfigAbsorbsWithoutDepletionCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useManaShieldCharmConfigOverrideForGameTest(
                    0.0D,
                    100,
                    50,
                    15.0D,
                    30.0D,
                    50,
                    100,
                    1,
                    20
            )) {
                var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_free_cost_test");
                equipCurio(player, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
                var magicData = MagicData.getPlayerMagicData(player);
                helper.assertTrue(magicData != null, "Mana Shield Charm free-cost test could not resolve player mana data");
                magicData.setMana(0.0F);
                player.invulnerableTime = 0;
                var initialHealth = player.getHealth();

                var event = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 2.0F);

                helper.assertTrue(event.isCanceled(),
                        "Mana Shield Charm should absorb whole damage steps without mana when manaPerDamage is zero");
                helper.assertTrue(Math.abs(player.getHealth() - initialHealth) < 1.0e-4F,
                        "Free-cost Mana Shield Charm should keep health unchanged");
                helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                        "Free-cost Mana Shield Charm should not recover or spend mana");
                helper.assertFalse(getManaShieldCharmState(player).cooldownActive,
                        "Free-cost Mana Shield Charm should not enter depletion cooldown without spending mana");
            }
        });
    }

    static void manaShieldCharmZeroRecoveryThresholdDisablesDepletionCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useManaShieldCharmConfigOverrideForGameTest(
                    25.0D,
                    0,
                    50,
                    15.0D,
                    30.0D,
                    50,
                    100,
                    1,
                    20
            )) {
                var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_no_cooldown_test");
                equipCurio(player, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
                var magicData = MagicData.getPlayerMagicData(player);
                helper.assertTrue(magicData != null, "Mana Shield Charm zero-threshold test could not resolve player mana data");
                var state = getManaShieldCharmState(player);
                state.reset();
                state.cooldownActive = true;
                magicData.setMana(25.0F);
                player.invulnerableTime = 0;

                var event = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 1.0F);

                helper.assertTrue(event.isCanceled(),
                        "Mana Shield Charm should clear existing cooldown and absorb while recovery threshold is zero");
                helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                        "Mana Shield Charm should still spend mana when only depletion cooldown is disabled");
                helper.assertFalse(state.cooldownActive,
                        "Mana Shield Charm should not enter depletion cooldown when recovery threshold is zero");
            }
        });
    }

    static void manaShieldCharmSynchronizationManaCostUsesServerConfig(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            try (var ignored = ApprenticeCodexServerConfig.useManaShieldCharmConfigOverrideForGameTest(
                    25.0D,
                    100,
                    50,
                    5.0D,
                    10.0D,
                    50,
                    100,
                    1,
                    20
            )) {
                var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_sync_config_test");
                var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
                charm.enchant(EnchantmentRegistry.SYNCHRONIZATION.get(), 1);
                equipCurio(player, CuriosSlotConstants.CHARM, charm);
                var magicData = MagicData.getPlayerMagicData(player);
                helper.assertTrue(magicData != null, "Synchronization config test could not resolve player mana data");
                magicData.setMana(100.0F);
                var source = helper.getLevel().damageSources().lava();

                var event = postLivingAttackEventForGameTest(player, source, 2.0F);

                helper.assertTrue(event.isCanceled(),
                        "Synchronization config test should cancel the intercepted hit");
                helper.assertTrue(Math.abs(magicData.getMana() - 40.0F) < 1.0e-4F,
                        "Synchronization should add its configured ordinary per-damage cost");
                helper.succeed();
            }
        });
    }

    static void manaShieldCharmNeutralizationZeroRecoveryStillNullifies(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            try (var ignored = ApprenticeCodexServerConfig.useManaShieldCharmConfigOverrideForGameTest(
                    25.0D,
                    100,
                    50,
                    15.0D,
                    30.0D,
                    50,
                    0,
                    1,
                    20
            )) {
                var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_neutralization_zero_test");
                var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
                charm.enchant(EnchantmentRegistry.NEUTRALIZATION.get(), 1);
                equipCurio(player, CuriosSlotConstants.CHARM, charm);
                var magicData = MagicData.getPlayerMagicData(player);
                helper.assertTrue(magicData != null, "Neutralization zero-recovery test could not resolve player mana data");
                magicData.setMana(10.0F);
                player.invulnerableTime = 0;
                var event = new io.redspace.ironsspellbooks.api.events.CounterSpellEvent(player, player);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);

                helper.assertTrue(event.isCanceled(),
                        "Neutralization should cancel Counterspell when its configured mana cost is zero");
                helper.assertTrue(Math.abs(magicData.getMana() - 10.0F) < 1.0e-4F,
                        "Zero-cost Counterspell resistance should leave mana unchanged");
                helper.succeed();
            }
        });
    }

    static void manaShieldCharmShellArmorDurabilityDamageUsesServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useManaShieldCharmConfigOverrideForGameTest(
                    25.0D,
                    100,
                    50,
                    15.0D,
                    30.0D,
                    50,
                    100,
                    2,
                    20
            )) {
                var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_shell_durability_config_test");
                var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
                charm.enchant(EnchantmentRegistry.SHELL.get(), 1);
                equipCurio(player, CuriosSlotConstants.CHARM, charm);
                var chestplate = new ItemStack(Items.IRON_CHESTPLATE);
                player.setItemSlot(EquipmentSlot.CHEST, chestplate);
                var magicData = MagicData.getPlayerMagicData(player);
                helper.assertTrue(magicData != null, "Shell durability config test could not resolve player mana data");
                magicData.setMana(500.0F);
                player.invulnerableTime = 0;
                var source = helper.getLevel().damageSources().lava();

                var event = postLivingAttackEventForGameTest(player, source, 5.0F);

                helper.assertTrue(event.isCanceled(),
                        "Shell durability config test should still intercept normal damage");
                helper.assertTrue(chestplate.getDamageValue() == 3,
                        "Shell should damage armor by ceil(raw damage / 4 * configured multiplier)");
            }
        });
    }

    static void manaShieldCharmInvulnerableTimeUsesServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useManaShieldCharmConfigOverrideForGameTest(
                    25.0D,
                    100,
                    50,
                    15.0D,
                    30.0D,
                    50,
                    100,
                    1,
                    6
            )) {
                var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_iframe_config_test");
                equipCurio(player, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
                var magicData = MagicData.getPlayerMagicData(player);
                helper.assertTrue(magicData != null, "Mana Shield Charm i-frame config test could not resolve player mana data");
                magicData.setMana(100.0F);
                player.invulnerableTime = 0;

                var firstEvent = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 1.0F);
                helper.assertTrue(firstEvent.isCanceled(),
                        "Mana Shield Charm i-frame config test should cancel the first hit");
                helper.assertTrue(player.invulnerableTime >= 6,
                        "Mana Shield Charm should apply configured invulnerable time");
                helper.assertTrue(Math.abs(magicData.getMana() - 75.0F) < 1.0e-4F,
                        "Mana Shield Charm should spend mana on the first configured i-frame hit");

                var secondEvent = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), 1.0F);
                helper.assertTrue(secondEvent.isCanceled(),
                        "Mana Shield Charm should cancel repeated damage while configured i-frame gate is active");
                helper.assertTrue(Math.abs(magicData.getMana() - 75.0F) < 1.0e-4F,
                        "Mana Shield Charm should not spend mana again inside configured i-frame gate");
            }
        });
    }

    static void antiManaArrowNeutralizationConsumesFixedManaAndCancelsDamage(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            try (var ignored = ApprenticeCodexServerConfig.useManaShieldCharmConfigOverrideForGameTest(
                    25.0D, 100, 50, 15.0D, 30.0D, 50, 100, 1, 20
            )) {
                var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "anti_mana_arrow_neutralization");
                var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
                charm.enchant(EnchantmentRegistry.NEUTRALIZATION.get(), 1);
                equipCurio(player, CuriosSlotConstants.CHARM, charm);
                var magicData = MagicData.getPlayerMagicData(player);
                helper.assertTrue(magicData != null, "Anti Mana Arrow test could not resolve player mana data");
                magicData.setMana(100.0F);
                player.invulnerableTime = 0;
                var arrow = new jp.aquafactory.apprenticecodex.item.antimanaarrow.AntiManaArrowEntity(
                        helper.getLevel(), player);
                var event = postLivingAttackEventForGameTest(
                        player, helper.getLevel().damageSources().arrow(arrow, player), 2.0F);

                helper.assertTrue(event.isCanceled(), "Neutralization should cancel Anti Mana Arrow damage");
                helper.assertTrue(Math.abs(magicData.getMana() - 50.0F) < 1.0e-4F,
                        "Neutralization should spend the configured fixed Anti Mana Arrow cost");
                helper.assertFalse(player.hasEffect(jp.aquafactory.apprenticecodex.registry.EffectRegistry.INERT_MANA_SHIELD),
                        "Successful Anti Mana resistance should not apply Inert Mana Shield");
                helper.succeed();
            }
        });
    }

    static void antiManaArrowDisablesUnprotectedManaShieldAfterCurrentHit(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "anti_mana_arrow_inert");
            equipCurio(player, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Anti Mana Arrow inert test could not resolve player mana data");
            magicData.setMana(100.0F);
            player.invulnerableTime = 0;
            var arrow = new jp.aquafactory.apprenticecodex.item.antimanaarrow.AntiManaArrowEntity(
                    helper.getLevel(), player);
            var event = postLivingAttackEventForGameTest(
                    player, helper.getLevel().damageSources().arrow(arrow, player), 2.0F);

            helper.assertTrue(event.isCanceled(), "The current Anti Mana Arrow hit should still use the active barrier");
            helper.assertTrue(Math.abs(magicData.getMana() - 50.0F) < 1.0e-4F,
                    "The current Anti Mana Arrow hit should keep normal barrier mana consumption");
            var inert = player.getEffect(jp.aquafactory.apprenticecodex.registry.EffectRegistry.INERT_MANA_SHIELD);
            helper.assertTrue(inert != null && inert.getDuration() == 600,
                    "Anti Mana Arrow should apply Inert Mana Shield for 30 seconds");
            helper.assertTrue(inert != null && inert.getCurativeItems().isEmpty(),
                    "Inert Mana Shield should not have milk or other standard cures");
            helper.succeed();
        });
    }

    static void neutralizationCounterspellResistanceRequiresFullManaCost(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            try (var ignored = ApprenticeCodexServerConfig.useManaShieldCharmConfigOverrideForGameTest(
                    25.0D, 100, 50, 15.0D, 30.0D, 50, 100, 1, 20
            )) {
                var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "counterspell_neutralization");
                var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
                charm.enchant(EnchantmentRegistry.NEUTRALIZATION.get(), 1);
                equipCurio(player, CuriosSlotConstants.CHARM, charm);
                var magicData = MagicData.getPlayerMagicData(player);
                helper.assertTrue(magicData != null, "Counterspell resistance test could not resolve player mana data");

                magicData.setMana(99.0F);
                var insufficient = new io.redspace.ironsspellbooks.api.events.CounterSpellEvent(player, player);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(insufficient);
                helper.assertFalse(insufficient.isCanceled(), "Counterspell resistance should fail below its full cost");
                helper.assertTrue(Math.abs(magicData.getMana() - 99.0F) < 1.0e-4F,
                        "Failed Counterspell resistance should not spend mana");

                magicData.setMana(100.0F);
                var sufficient = new io.redspace.ironsspellbooks.api.events.CounterSpellEvent(player, player);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(sufficient);
                helper.assertTrue(sufficient.isCanceled(), "Counterspell resistance should activate at its full cost");
                helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                        "Counterspell resistance should spend exactly its configured cost");
                helper.assertTrue(getManaShieldCharmState(player).cooldownActive,
                        "Spending the last mana on Counterspell resistance should start recovery cooldown");
                helper.succeed();
            }
        });
    }
}
