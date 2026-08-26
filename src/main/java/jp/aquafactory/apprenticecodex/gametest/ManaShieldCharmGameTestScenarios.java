package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.manashieldcharm.ManaShieldCharm;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

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
    static void manaShieldCharmShellUsesArmorOnlyOnNormalDamageAndWearsArmor(GameTestHelper helper) {
        var armored = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_shell_armored_test");
        var unarmored = createTrackedEquipmentTestPlayer(helper, new BlockPos(3, 2, 0), "mana_shield_shell_unarmored_test");
        var bypassArmor = createTrackedEquipmentTestPlayer(helper, new BlockPos(6, 2, 0), "mana_shield_shell_bypass_test");

        var shellCharm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
        shellCharm.enchant(EnchantmentRegistry.SHELL.get(), 1);
        equipCurio(armored, CuriosSlotConstants.CHARM, shellCharm.copy());
        equipCurio(unarmored, CuriosSlotConstants.CHARM, shellCharm.copy());
        equipCurio(bypassArmor, CuriosSlotConstants.CHARM, shellCharm.copy());

        var head = new ItemStack(Items.IRON_HELMET);
        var chest = new ItemStack(Items.IRON_CHESTPLATE);
        var legs = new ItemStack(Items.IRON_LEGGINGS);
        var boots = new ItemStack(Items.IRON_BOOTS);
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
            armoredMana.setMana(50.0F);
            unarmoredMana.setMana(50.0F);
            bypassMana.setMana(50.0F);
            armored.invulnerableTime = 0;
            unarmored.invulnerableTime = 0;
            bypassArmor.invulnerableTime = 0;
            var armoredInitialHealth = armored.getHealth();
            var unarmoredInitialHealth = unarmored.getHealth();
            var bypassInitialHealth = bypassArmor.getHealth();
            var armoredEvent = postLivingAttackEventForGameTest(armored, helper.getLevel().damageSources().lava(), 3.0F);
            var unarmoredEvent = postLivingAttackEventForGameTest(unarmored, helper.getLevel().damageSources().lava(), 3.0F);
            var bypassSource = jp.aquafactory.apprenticecodex.utility.CombatTools.getDamageSource(helper.getLevel(), bypassArmor, DamageTypes.UNITE_LUNA);
            var bypassEvent = postLivingAttackEventForGameTest(bypassArmor, bypassSource, 2.0F);
            helper.assertTrue(armoredEvent.isCanceled() && unarmoredEvent.isCanceled() && bypassEvent.isCanceled(),
                    "Mana Shield Charm Shell test should cancel all intercepted LivingAttackEvent instances");
            helper.assertTrue(armored.getHealth() > unarmored.getHealth(),
                    "Shell should apply armor reduction before the normal mana shoulder path"
                            + " armoredHealth=" + armored.getHealth()
                            + " unarmoredHealth=" + unarmored.getHealth()
                            + " armoredMana=" + armoredMana.getMana()
                            + " unarmoredMana=" + unarmoredMana.getMana());
            helper.assertTrue(armoredMana.getMana() > unarmoredMana.getMana(),
                    "Shell should reduce barrier mana consumption when armor mitigates the intercepted hit"
                            + " armoredMana=" + armoredMana.getMana()
                            + " unarmoredMana=" + unarmoredMana.getMana());
            helper.assertTrue(Math.abs(unarmoredMana.getMana()) < 1.0e-4F,
                    "Shell should still burn out the unarmored player at 50 mana");
            helper.assertTrue(head.getDamageValue() == 1
                            && chest.getDamageValue() == 1
                            && legs.getDamageValue() == 1
                            && boots.getDamageValue() == 1,
                    "Shell should spend one durability on each equipped armor piece");
            helper.assertTrue(Math.abs(bypassArmor.getHealth() - bypassInitialHealth) < 1.0e-4F,
                    "Shell should not leak armor-bypass damage when base shield mana fully negates it");
            helper.assertTrue(Math.abs(bypassMana.getMana()) < 1.0e-4F,
                    "Shell should fall back to the normal 25 mana per damage path on armor-bypass hits");
            helper.assertTrue(bypassChest.getDamageValue() == 0,
                    "Shell should not damage armor durability on armor-bypass hits");
            helper.assertTrue(armored.getHealth() < armoredInitialHealth && unarmored.getHealth() < unarmoredInitialHealth,
                    "Shell normal damage test should leave residual health damage on both players");
            helper.succeed();
        });
    }
    static void manaShieldCharmShellLowManaBurnoutStillUsesArmorPath(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_shell_low_mana_test");
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(EnchantmentRegistry.SHELL.get(), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);

            var chestplate = new ItemStack(Items.IRON_CHESTPLATE);
            player.setItemSlot(EquipmentSlot.CHEST, chestplate);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Shell low mana test could not resolve player mana data");
            magicData.setMana(24.0F);
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();
            var armor = getEquippedAttributeTotal(player, Attributes.ARMOR);
            var toughness = getEquippedAttributeTotal(player, Attributes.ARMOR_TOUGHNESS);
            var incomingDamage = findDamageForArmorReducedTarget(armor, toughness, 1.0F);
            var reducedDamage = CombatRules.getDamageAfterAbsorb(incomingDamage, armor, toughness);

            helper.assertTrue(Math.abs(reducedDamage - 1.0F) < 1.0e-3F,
                    "Shell low mana test should configure an armor-reduced hit worth exactly one barrier step"
                            + " reducedDamage=" + reducedDamage
                            + " incomingDamage=" + incomingDamage);

            var event = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), incomingDamage);
            helper.assertTrue(event.isCanceled(),
                    "Shell should still cancel the hit when only the last armor-reduced barrier step can be rescued");
            helper.assertTrue(Math.abs(player.getHealth() - initialHealth) < 1.0e-3F,
                    "Shell low mana rescue should still keep health unchanged");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Shell low mana rescue should clamp mana to zero");
            helper.assertTrue(getManaShieldCharmState(player).cooldownActive,
                    "Shell low mana rescue should enter cooldown");
            helper.assertTrue(player.invulnerableTime >= 20,
                    "Shell low mana rescue should still apply vanilla-style invulnerability time");
            helper.assertTrue(chestplate.getDamageValue() == 1,
                    "Shell low mana rescue should preserve armor durability loss on the armor path");
        });
    }
    static void manaShieldCharmSynchronizationChargesEnchantReductionBeforeNormalBarrier(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_sync_cost_test");
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(EnchantmentRegistry.SYNCHRONIZATION.get(), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);

            for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                var armorStack = switch (slot) {
                    case HEAD -> new ItemStack(Items.IRON_HELMET);
                    case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
                    case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
                    case FEET -> new ItemStack(Items.IRON_BOOTS);
                    default -> ItemStack.EMPTY;
                };
                armorStack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
                player.setItemSlot(slot, armorStack);
            }

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Synchronization cost test could not resolve player mana data");
            magicData.setMana(120.0F);
            var availableMana = magicData.getMana();
            player.invulnerableTime = 0;

            var source = helper.getLevel().damageSources().lava();
            var protection = EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source);
            var reducedDamage = CombatRules.getDamageAfterMagicAbsorb(5.0F, protection);
            var expectedRemainingMana = resolveExpectedSynchronizationManaAfterHitForGameTest(5.0F, availableMana, protection);

            var event = postLivingAttackEventForGameTest(player, source, 5.0F);
            helper.assertTrue(event.isCanceled(),
                    "Synchronization should cancel the original LivingAttackEvent when it intercepts the hit");
            helper.assertTrue(Math.abs(magicData.getMana() - expectedRemainingMana) < 1.0e-4F,
                    "Synchronization should charge enchant mitigation before the normal barrier stage"
                            + " protection=" + protection
                            + " reducedDamage=" + reducedDamage
                            + " expectedMana=" + expectedRemainingMana
                            + " actualMana=" + magicData.getMana());
            helper.assertTrue(getManaShieldCharmState(player).cooldownActive == (expectedRemainingMana <= 0.0F),
                    "Synchronization cooldown state did not match the remaining mana expectation"
                            + " expectedRemainingMana=" + expectedRemainingMana
                            + " cooldown=" + getManaShieldCharmState(player).cooldownActive);
            helper.succeed();
        });
    }
    static void manaShieldCharmSynchronizationBurnoutStopsAfterEnchantReduction(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_sync_burnout_test");
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(EnchantmentRegistry.SYNCHRONIZATION.get(), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);

            for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                var armorStack = switch (slot) {
                    case HEAD -> new ItemStack(Items.IRON_HELMET);
                    case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
                    case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
                    case FEET -> new ItemStack(Items.IRON_BOOTS);
                    default -> ItemStack.EMPTY;
                };
                armorStack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
                player.setItemSlot(slot, armorStack);
            }

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Synchronization burnout test could not resolve player mana data");
            magicData.setMana(20.0F);
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();
            var expectedArmor = getEquippedAttributeTotal(player, Attributes.ARMOR);
            var expectedToughness = getEquippedAttributeTotal(player, Attributes.ARMOR_TOUGHNESS);
            var source = helper.getLevel().damageSources().lava();
            var protection = EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source);

            var event = postLivingAttackEventForGameTest(player, source, 5.0F);
            var expectedHealthLoss = CombatRules.getDamageAfterAbsorb(
                    CombatRules.getDamageAfterMagicAbsorb(5.0F, protection),
                    expectedArmor,
                    expectedToughness
            );

            helper.assertTrue(event.isCanceled(),
                    "Synchronization burnout test should still cancel the original LivingAttackEvent");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Synchronization burnout should clamp mana to zero");
            helper.assertTrue(getManaShieldCharmState(player).cooldownActive,
                    "Synchronization burnout should enter cooldown during the enchant-reduction stage");
            helper.assertTrue(Math.abs((initialHealth - player.getHealth()) - expectedHealthLoss) < 1.0e-3F,
                    "Synchronization burnout should stop before the normal barrier stage and leave only enchant-reduced damage"
                            + " actualLoss=" + (initialHealth - player.getHealth())
                            + " expectedLoss=" + expectedHealthLoss
                            + " mana=" + magicData.getMana());
            helper.succeed();
        });
    }
    static void manaShieldCharmSynchronizationLowManaBurnoutStopsAfterEnchantStage(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_sync_low_mana_stage_test");
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(EnchantmentRegistry.SYNCHRONIZATION.get(), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);

            equipProtectionIvIronArmor(player);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Synchronization low mana enchant-stage test could not resolve player mana data");
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();
            var expectedArmor = getEquippedAttributeTotal(player, Attributes.ARMOR);
            var expectedToughness = getEquippedAttributeTotal(player, Attributes.ARMOR_TOUGHNESS);
            var source = helper.getLevel().damageSources().lava();
            var protection = EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source);
            var reducedDamage = CombatRules.getDamageAfterMagicAbsorb(5.0F, protection);
            var synchronizationSteps = countWholeDamageStepsForGameTest(5.0F - reducedDamage);
            helper.assertTrue(synchronizationSteps > 0,
                    "Synchronization low mana enchant-stage test should require at least one enchant mitigation cost step");
            magicData.setMana(synchronizationSteps * ApprenticeCodexServerConfig.manaShieldCharmSynchronizationManaPerDamage() - 1.0F);

            var event = postLivingAttackEventForGameTest(player, source, 5.0F);
            var expectedHealthLoss = CombatRules.getDamageAfterAbsorb(
                    reducedDamage,
                    expectedArmor,
                    expectedToughness
            );

            helper.assertTrue(event.isCanceled(),
                    "Synchronization low mana enchant-stage test should still cancel the original LivingAttackEvent");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Synchronization low mana enchant-stage rescue should clamp mana to zero");
            helper.assertTrue(getManaShieldCharmState(player).cooldownActive,
                    "Synchronization low mana enchant-stage rescue should enter cooldown");
            helper.assertTrue(Math.abs((initialHealth - player.getHealth()) - expectedHealthLoss) < 1.0e-3F,
                    "Synchronization low mana enchant-stage rescue should stop before the normal barrier stage"
                            + " actualLoss=" + (initialHealth - player.getHealth())
                            + " expectedLoss=" + expectedHealthLoss
                            + " reducedDamage=" + reducedDamage
                            + " mana=" + magicData.getMana());
            helper.succeed();
        });
    }
    static void manaShieldCharmSynchronizationLowManaBurnoutAfterBarrierStage(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_sync_low_mana_barrier_test");
            var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
            charm.enchant(EnchantmentRegistry.SYNCHRONIZATION.get(), 1);
            equipCurio(player, CuriosSlotConstants.CHARM, charm);

            equipProtectionIvIronArmor(player);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Synchronization low mana barrier-stage test could not resolve player mana data");
            player.invulnerableTime = 0;
            var initialHealth = player.getHealth();
            var source = helper.getLevel().damageSources().lava();
            var protection = EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source);
            var incomingDamage = findDamageForMagicReducedTarget(protection, 1.0F);
            var reducedDamage = CombatRules.getDamageAfterMagicAbsorb(incomingDamage, protection);
            var synchronizationSteps = countWholeDamageStepsForGameTest(incomingDamage - reducedDamage);

            helper.assertTrue(Math.abs(reducedDamage - 1.0F) < 1.0e-3F,
                    "Synchronization low mana barrier-stage test should configure exactly one normal barrier step"
                            + " reducedDamage=" + reducedDamage
                            + " incomingDamage=" + incomingDamage);
            helper.assertTrue(synchronizationSteps > 0,
                    "Synchronization low mana barrier-stage test should still require enchant mitigation cost before the barrier");

            magicData.setMana(synchronizationSteps * ApprenticeCodexServerConfig.manaShieldCharmSynchronizationManaPerDamage()
                    + ApprenticeCodexServerConfig.manaShieldCharmManaPerDamage() - 1.0F);
            var event = postLivingAttackEventForGameTest(player, source, incomingDamage);

            helper.assertTrue(event.isCanceled(),
                    "Synchronization low mana barrier-stage rescue should still cancel the original LivingAttackEvent");
            helper.assertTrue(Math.abs(player.getHealth() - initialHealth) < 1.0e-3F,
                    "Synchronization low mana barrier-stage rescue should keep health unchanged");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Synchronization low mana barrier-stage rescue should clamp mana to zero");
            helper.assertTrue(getManaShieldCharmState(player).cooldownActive,
                    "Synchronization low mana barrier-stage rescue should enter cooldown");
            helper.assertTrue(player.invulnerableTime >= 20,
                    "Synchronization low mana barrier-stage rescue should still apply vanilla-style invulnerability time");
            helper.succeed();
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
                    30.0D,
                    25.0D,
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
                    30.0D,
                    25.0D,
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
                    10.0D,
                    25.0D,
                    1,
                    20
            )) {
                var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_shield_sync_config_test");
                var charm = new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get());
                charm.enchant(EnchantmentRegistry.SYNCHRONIZATION.get(), 1);
                equipCurio(player, CuriosSlotConstants.CHARM, charm);
                for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    var armorStack = switch (slot) {
                        case HEAD -> new ItemStack(Items.IRON_HELMET);
                        case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
                        case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
                        case FEET -> new ItemStack(Items.IRON_BOOTS);
                        default -> ItemStack.EMPTY;
                    };
                    armorStack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 10);
                    player.setItemSlot(slot, armorStack);
                }

                var magicData = MagicData.getPlayerMagicData(player);
                helper.assertTrue(magicData != null, "Synchronization config test could not resolve player mana data");
                magicData.setMana(100.0F);
                var source = helper.getLevel().damageSources().lava();
                var protection = EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source);
                var incomingDamage = 1.5F;
                var reducedDamage = CombatRules.getDamageAfterMagicAbsorb(incomingDamage, protection);
                helper.assertTrue(reducedDamage < 1.0F,
                        "Synchronization config test should isolate the enchant-reduction cost before the barrier stage"
                                + " protection=" + protection
                                + " reducedDamage=" + reducedDamage);
                var expectedRemainingMana = resolveExpectedSynchronizationManaAfterHitForGameTest(incomingDamage, 100.0F, protection);

                var event = postLivingAttackEventForGameTest(player, source, incomingDamage);

                helper.assertTrue(event.isCanceled(),
                        "Synchronization config test should cancel the intercepted hit");
                helper.assertTrue(Math.abs(magicData.getMana() - expectedRemainingMana) < 1.0e-4F,
                        "Synchronization should use configured mana cost"
                                + " expectedMana=" + expectedRemainingMana
                                + " actualMana=" + magicData.getMana());
                helper.succeed();
            }
        });
    }
    static void manaShieldCharmNeutralizationZeroRecoveryStillNullifies(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            try (var ignored = ApprenticeCodexServerConfig.useManaShieldCharmConfigOverrideForGameTest(
                    25.0D,
                    100,
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
                    30.0D,
                    25.0D,
                    0,
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
                magicData.setMana(100.0F);
                player.invulnerableTime = 0;
                var armor = getEquippedAttributeTotal(player, Attributes.ARMOR);
                var toughness = getEquippedAttributeTotal(player, Attributes.ARMOR_TOUGHNESS);
                var incomingDamage = findDamageForArmorReducedTarget(armor, toughness, 1.0F);

                var event = postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().lava(), incomingDamage);

                helper.assertTrue(event.isCanceled(),
                        "Shell durability config test should still intercept normal damage");
                helper.assertTrue(chestplate.getDamageValue() == 0,
                        "Shell should not damage armor when configured durability damage is zero");
            }
        });
    }
    static void manaShieldCharmInvulnerableTimeUsesServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useManaShieldCharmConfigOverrideForGameTest(
                    25.0D,
                    100,
                    30.0D,
                    25.0D,
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
                    25.0D, 100, 30.0D, 50, 100, 1, 20
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
                    25.0D, 100, 30.0D, 50, 100, 1, 20
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
