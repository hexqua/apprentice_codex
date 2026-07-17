package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;

import java.util.UUID;

import jp.aquafactory.apprenticecodex.enchantment.WisdomExperienceDropEvent;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.item.shield.AbstractImbueShieldItem;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

final class OffhandAndBetterCombatGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private OffhandAndBetterCombatGameTestScenarios() {
    }

    static void copperSpellAmplifierStartsWithBallLightningAndStacksAttunement(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);

            helper.assertTrue(ISpellContainer.isSpellContainer(stack), "Copper Spell Amplifier did not initialize a spell container");

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Copper Spell Amplifier spell container is null");

            var spellData = spellContainer.getSpellAtIndex(0);
            helper.assertTrue(spellData != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                    "Copper Spell Amplifier has no preset spell");
            helper.assertTrue(spellData.getSpell() == SpellRegistry.SHOCK.get(),
                    "Copper Spell Amplifier preset spell mismatch: " + spellData.getSpell().getSpellResource());
            helper.assertTrue(spellData.getLevel() == 1,
                    "Copper Spell Amplifier preset spell level mismatch: " + spellData.getLevel());

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null, "Copper Spell Amplifier imbued school could not be resolved");

            // ここでは school ID の厳密一致ではなく、
            // 実装が解決した spell power 属性へ bonus / Attunement が正しく合算されることを回帰検知する.
            var resolvedSpellPower = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(resolvedSpellPower != null,
                    "Copper Spell Amplifier could not resolve spell power attribute for additive stacking: " + imbuedSchool.getId());

            assertModifierAmount(helper, item, stack, resolvedSpellPower, 0.10D, AttributeModifier.Operation.MULTIPLY_BASE,
                    "Copper Spell Amplifier additive spell power bonus regression");

            stack.enchant(EnchantmentRegistry.ATTUNEMENT.get(), 1);
            assertModifierAmount(helper, item, stack, resolvedSpellPower,
                    0.10D + AttributeEnchantmentType.ATTUNEMENT.amountPerLevel(),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Copper Spell Amplifier + Attunement stacking regression");
        });
    }
    static void reflectcastShieldImbuedSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractImbueShieldItem) ItemRegistry.REFLECTCAST_SHIELD.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Reflectcast Shield normalized spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Reflectcast Shield imbued spell should be removable");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Reflectcast Shield imbued spell should remain extractable in Spellcaster Workbench");
        });
    }
    static void reflectcastShieldImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractImbueShieldItem) ItemRegistry.REFLECTCAST_SHIELD.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var restored = roundTripItemStack(stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Reflectcast Shield save/load spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Reflectcast Shield imbued spell should remain removable after save/load");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Reflectcast Shield imbued spell should remain extractable after save/load");
        });
    }
    static void reflectcastShieldDurabilityRulesMatchGuardTuning(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get());
            var beforeRuntimeState = stack.copy();
            helper.assertTrue(stack.getMaxDamage() == ReflectcastShield.DURABILITY,
                    "Reflectcast Shield durability should be " + ReflectcastShield.DURABILITY + " but got " + stack.getMaxDamage());
            helper.assertTrue(ReflectcastShield.resolveBlockedDurabilityCost(2.9F) == 0,
                    "Reflectcast Shield should keep sub-threshold guard durability at zero");
            helper.assertTrue(ReflectcastShield.resolveBlockedDurabilityCost(3.0F) == 4,
                    "Reflectcast Shield should use vanilla shield durability cost");
            helper.assertTrue(ReflectcastShield.resolveBlockedDurabilityCost(12.75F) == 13,
                    "Reflectcast Shield should use vanilla high-damage durability cost");

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "reflectcast_shield_durability_runtime_test");
            ReflectcastShieldRuntime.clear(player);
            helper.assertFalse(ReflectcastShieldRuntime.isDurabilityConsumptionSuppressed(player, 100L),
                    "Reflectcast Shield should not suppress durability before a cost is recorded");
            ReflectcastShieldRuntime.rememberDurabilityConsumed(player, 100L);
            helper.assertTrue(ReflectcastShieldRuntime.isDurabilityConsumptionSuppressed(player, 100L),
                    "Reflectcast Shield should suppress durability on the recorded tick");
            helper.assertTrue(ReflectcastShieldRuntime.isDurabilityConsumptionSuppressed(player, 110L),
                    "Reflectcast Shield should suppress durability through the ten tick window");
            helper.assertFalse(ReflectcastShieldRuntime.isDurabilityConsumptionSuppressed(player, 111L),
                    "Reflectcast Shield should allow durability after the ten tick window");
            helper.assertTrue(Utils.isSameItemSameComponentsIgnoreDurability(beforeRuntimeState, stack),
                    "Reflectcast Shield durability runtime should not write item NBT");
            ReflectcastShieldRuntime.clear(player);
            ReflectcastShieldRuntime.rememberSpellTriggered(player, 200L);
            helper.assertTrue(ReflectcastShieldRuntime.isSpellTriggerSuppressed(player, 209L),
                    "Reflectcast Shield should suppress repeated spell triggers for nine following ticks");
            helper.assertFalse(ReflectcastShieldRuntime.isSpellTriggerSuppressed(player, 210L),
                    "Reflectcast Shield should allow another spell trigger on tick ten");
            ReflectcastShieldRuntime.clear(player);
            helper.assertFalse(ReflectcastShieldRuntime.isSpellTriggerSuppressed(player, 200L),
                    "Reflectcast Shield trigger suppression should clear with its non-persistent runtime");
        });
    }
    static void diamondAndNetheriteSpellAmplifierExposeNewAttributeBonuses(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var diamondItem = (AbstractOffhandMagicItem) ItemRegistry.DIAMOND_SPELL_AMPLIFIER.get();
            var diamondStack = new ItemStack(diamondItem);
            assertModifierAmount(
                    helper,
                    diamondItem,
                    diamondStack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CASTING_MOVESPEED.get(),
                    0.25D,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Diamond Spell Amplifier casting move speed bonus regression"
            );

            var netheriteItem = (AbstractOffhandMagicItem) ItemRegistry.NETHERITE_SPELL_AMPLIFIER.get();
            var netheriteStack = new ItemStack(netheriteItem);
            assertModifierAmount(
                    helper,
                    netheriteItem,
                    netheriteStack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CASTING_MOVESPEED.get(),
                    0.50D,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Netherite Spell Amplifier casting move speed bonus regression"
            );
        });
    }
    static void upgradeWhitelistCoversTargetAbstractItems(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertUpgradeable(helper, new ItemStack(ItemRegistry.ENDER_GRIMOIRE.get()),
                    "Ender Grimoire should remain upgradeable via explicit whitelist entry");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get()),
                    "Archivist's Grimoire should remain upgradeable via explicit whitelist entry");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.ELEMENTAL_BOW.get()),
                    "Elemental Bow should remain upgradeable via explicit whitelist entry");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get()),
                    "AbstractOffhandMagicItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.PHOTON_SIPHON.get()),
                    "Direct AbstractOffhandMagicItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()),
                    "AbstractSpellGunItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get()),
                    "AbstractRightClickMagicWeaponItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.ILLUMINATE_STELLAR_STAFF.get()),
                    "Indirect AbstractRightClickMagicWeaponItem descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.UNITE_LUNA_STAFF.get()),
                    "New swing magic weapon descendants should be upgradeable");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                    "Charged Twin Blade Staff should be upgradeable via explicit whitelist entry");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.MANA_FORCE_BLADE.get()),
                    "Mana Force Blade should be upgradeable via explicit whitelist entry");
            assertUpgradeable(helper, new ItemStack(ItemRegistry.SPELL_SIDE_EDGE.get()),
                    "Spell Side Edge should be upgradeable via explicit whitelist entry");

            var shieldStack = new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get());
            helper.assertFalse(shieldStack.is(io.redspace.ironsspellbooks.util.ModTags.CAN_BE_UPGRADED),
                    "Reflectcast Shield should not be in the upgrade whitelist");
            helper.assertFalse(Utils.canBeUpgraded(shieldStack),
                    "Reflectcast Shield should remain excluded from the upgrade system");
        });
    }
    static void uniteLunaStaffStartsWithUniteLunaAndExpectedMainhandBonuses(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (jp.aquafactory.apprenticecodex.item.UniteLunaStaff) ItemRegistry.UNITE_LUNA_STAFF.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);

            helper.assertTrue(ISpellContainer.isSpellContainer(stack), "Unite Luna Staff did not initialize a spell container");
            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Unite Luna Staff spell container is null");

            var spellData = spellContainer.getSpellAtIndex(0);
            helper.assertTrue(spellData != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                    "Unite Luna Staff has no preset spell");
            helper.assertTrue(spellData.getSpell() == jp.aquafactory.apprenticecodex.registry.SpellRegistry.UNITE_LUNA.get(),
                    "Unite Luna Staff preset spell mismatch: " + spellData.getSpell().getSpellResource());
            helper.assertTrue(spellData.getLevel() == 1,
                    "Unite Luna Staff preset spell level mismatch: " + spellData.getLevel());

            var modifiers = item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack);
            helper.assertTrue(Math.abs(sumModifierAmount(modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), AttributeModifier.Operation.ADDITION) - 12.0D) < 1.0e-9D,
                    "Unite Luna Staff attack damage regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED), AttributeModifier.Operation.ADDITION) - (-3.2D)) < 1.0e-9D,
                    "Unite Luna Staff attack speed regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(modifiers.get(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get()), AttributeModifier.Operation.ADDITION) - 0.5D) < 1.0e-9D,
                    "Unite Luna Staff entity reach regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()), AttributeModifier.Operation.MULTIPLY_BASE) - 0.05D) < 1.0e-9D,
                    "Unite Luna Staff spell power regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.HOLY_SPELL_POWER.get()), AttributeModifier.Operation.MULTIPLY_BASE) - 0.10D) < 1.0e-9D,
                    "Unite Luna Staff holy spell power regression: " + describeModifiers(modifiers));
        });
    }
    static void offhandUpgradeBridgeAppliesMainhandStoredUpgradeData(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get());
            var upgradeData = createUpgradeData(
                    helper.getLevel().registryAccess(),
                    stack,
                    io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.MANA,
                    EquipmentSlot.MAINHAND.getName()
            );

            var event = new ItemAttributeModifierEvent(
                    stack,
                    EquipmentSlot.OFFHAND,
                    ItemRegistry.COPPER_SPELL_AMPLIFIER.get().getAttributeModifiers(EquipmentSlot.OFFHAND, stack)
            );
            MinecraftForge.EVENT_BUS.post(event);

            var maxManaAmount = sumModifierAmount(
                    event.getModifiers().get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get()),
                    AttributeModifier.Operation.ADDITION
            );
            helper.assertTrue(Math.abs(maxManaAmount - 50.0D) < 1.0e-9D,
                    "Offhand upgrade bridge regression: expected +50 max mana from mainhand-stored upgrade but got "
                            + maxManaAmount + " upgradeData=" + upgradeData
                            + " modifiers=" + describeModifiers(event.getModifiers()));
        });
    }
    static void betterCombatSpellbreakerIsTwoHandedAndAmplifierHasOffhandSpellPower(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var spellbreaker = ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spellbreaker")
            );
            helper.assertTrue(spellbreaker != null, "Missing irons_spellbooks:spellbreaker for Better Combat regression test");
            var spellbreakerAttributes = net.bettercombat.logic.WeaponRegistry.getAttributes(new ItemStack(spellbreaker));
            helper.assertTrue(spellbreakerAttributes != null && spellbreakerAttributes.isTwoHanded(),
                    "Better Combat spellbreaker should resolve as a two-handed weapon but got " + spellbreakerAttributes);

            var amplifierStack = new ItemStack(ItemRegistry.IRON_SPELL_AMPLIFIER.get());
            var amplifierEvent = new ItemAttributeModifierEvent(
                    amplifierStack,
                    EquipmentSlot.OFFHAND,
                    ItemRegistry.IRON_SPELL_AMPLIFIER.get().getAttributeModifiers(EquipmentSlot.OFFHAND, amplifierStack)
            );
            MinecraftForge.EVENT_BUS.post(amplifierEvent);

            var spellPowerBonus = sumModifierAmount(
                    amplifierEvent.getModifiers().get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(spellPowerBonus - 0.05D) < 1.0e-9D,
                    "Iron Spell Amplifier should expose +0.05 spell power in offhand modifiers but got "
                            + spellPowerBonus + " modifiers=" + describeModifiers(amplifierEvent.getModifiers()));
        });
    }
    static void betterCombatOffhandOnlyGauntletDoesNotForceDualWielding(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var swordStack = new ItemStack(Items.DIAMOND_SWORD);
            var gauntletStack = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            helper.assertTrue(net.bettercombat.logic.WeaponRegistry.getAttributes(swordStack) != null,
                    "Better Combat diamond sword attributes should be present for offhand Gauntlet test");
            helper.assertTrue(net.bettercombat.logic.WeaponRegistry.getAttributes(gauntletStack) != null,
                    "Better Combat Scrollcaster Gauntlet attributes should be present for offhand Gauntlet test");

            var swordMainPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "better_combat_offhand_gauntlet_no_dual_test");
            swordMainPlayer.setItemInHand(InteractionHand.MAIN_HAND, swordStack);
            swordMainPlayer.setItemInHand(InteractionHand.OFF_HAND, gauntletStack.copy());

            helper.assertFalse(net.bettercombat.logic.PlayerAttackHelper.isDualWielding(swordMainPlayer),
                    "Offhand-only Scrollcaster Gauntlet should not make a normal mainhand weapon dual wield");
            var secondSwordAttack = net.bettercombat.logic.PlayerAttackHelper.getCurrentAttack(swordMainPlayer, 1);
            helper.assertTrue(secondSwordAttack != null && !secondSwordAttack.isOffHand(),
                    "Offhand-only Scrollcaster Gauntlet should keep Better Combat attacks on mainhand but got "
                            + secondSwordAttack);

            var dualGauntletPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "better_combat_dual_gauntlet_stays_dual_test");
            dualGauntletPlayer.setItemInHand(InteractionHand.MAIN_HAND, gauntletStack.copy());
            dualGauntletPlayer.setItemInHand(InteractionHand.OFF_HAND, gauntletStack.copy());

            helper.assertTrue(net.bettercombat.logic.PlayerAttackHelper.isDualWielding(dualGauntletPlayer),
                    "Two Scrollcaster Gauntlets should keep the previous Better Combat dual wield behavior");
            var secondGauntletAttack = net.bettercombat.logic.PlayerAttackHelper.getCurrentAttack(dualGauntletPlayer, 1);
            helper.assertTrue(secondGauntletAttack != null && secondGauntletAttack.isOffHand(),
                    "Dual Scrollcaster Gauntlets should still select offhand on the second attack but got "
                            + secondGauntletAttack);
        });
    }
    static void spellSideEdgeBetterCombatTooltipFollowsLoadedMod(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var edgeStack = new ItemStack(ItemRegistry.SPELL_SIDE_EDGE.get());
            var mirrorStack = new ItemStack(ItemRegistry.SPELL_SIDE_EDGE_MIRROR.get());
            if (!ModList.get().isLoaded("bettercombat")) {
                assertTooltipKeyAbsent(
                        helper,
                        edgeStack,
                        "item.apprenticecodex.spell_side_edge.desc.better_combat",
                        "Spell Side Edge should hide Better Combat tooltip without Better Combat"
                );
                assertTooltipKeyAbsent(
                        helper,
                        mirrorStack,
                        "item.apprenticecodex.spell_side_edge_mirror.desc.better_combat",
                        "Spell Side Edge Mirror should hide Better Combat tooltip without Better Combat"
                );
                return;
            }

            assertTooltipKeyUsesColor(
                    helper,
                    edgeStack,
                    "item.apprenticecodex.spell_side_edge.desc.better_combat",
                    ChatFormatting.GRAY,
                    "Spell Side Edge should show Better Combat tooltip when Better Combat is loaded"
            );
            assertTooltipKeyUsesColor(
                    helper,
                    mirrorStack,
                    "item.apprenticecodex.spell_side_edge_mirror.desc.better_combat",
                    ChatFormatting.GRAY,
                    "Spell Side Edge Mirror should show Better Combat tooltip when Better Combat is loaded"
            );
        });
    }
    static void betterCombatSpellSideEdgeSuppressesNonMirrorOffhand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var edgeStack = new ItemStack(ItemRegistry.SPELL_SIDE_EDGE.get());
            var mirrorStack = new ItemStack(ItemRegistry.SPELL_SIDE_EDGE_MIRROR.get());
            var swordStack = new ItemStack(Items.DIAMOND_SWORD);
            helper.assertTrue(net.bettercombat.logic.WeaponRegistry.getAttributes(edgeStack) != null,
                    "Better Combat Spell Side Edge attributes should be present for dual wield policy test");
            helper.assertTrue(net.bettercombat.logic.WeaponRegistry.getAttributes(mirrorStack) != null,
                    "Better Combat Spell Side Edge Mirror attributes should be present for dual wield policy test");
            helper.assertTrue(net.bettercombat.logic.WeaponRegistry.getAttributes(swordStack) != null,
                    "Better Combat diamond sword attributes should be present for Spell Side Edge dual wield policy test");

            var swordOffhandPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "better_combat_spell_side_edge_sword_offhand_test");
            swordOffhandPlayer.setItemInHand(InteractionHand.MAIN_HAND, edgeStack.copy());
            swordOffhandPlayer.setItemInHand(InteractionHand.OFF_HAND, swordStack.copy());

            helper.assertFalse(net.bettercombat.logic.PlayerAttackHelper.isDualWielding(swordOffhandPlayer),
                    "Spell Side Edge should suppress non-Mirror offhand Better Combat dual wielding");
            var secondSwordOffhandAttack = net.bettercombat.logic.PlayerAttackHelper.getCurrentAttack(swordOffhandPlayer, 1);
            helper.assertTrue(secondSwordOffhandAttack != null && !secondSwordOffhandAttack.isOffHand(),
                    "Spell Side Edge with a non-Mirror offhand should keep attacks on mainhand but got "
                            + secondSwordOffhandAttack);

            var mirrorOffhandPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "better_combat_spell_side_edge_mirror_offhand_test");
            mirrorOffhandPlayer.setItemInHand(InteractionHand.MAIN_HAND, edgeStack.copy());
            mirrorOffhandPlayer.setItemInHand(InteractionHand.OFF_HAND, mirrorStack.copy());

            helper.assertTrue(net.bettercombat.logic.PlayerAttackHelper.isDualWielding(mirrorOffhandPlayer),
                    "Spell Side Edge Mirror should remain allowed for Better Combat dual wielding");
            var secondMirrorOffhandAttack = net.bettercombat.logic.PlayerAttackHelper.getCurrentAttack(mirrorOffhandPlayer, 1);
            helper.assertTrue(secondMirrorOffhandAttack != null && secondMirrorOffhandAttack.isOffHand(),
                    "Spell Side Edge Mirror should stay in the Better Combat combo but got "
                            + secondMirrorOffhandAttack);
        });
    }
    static void betterCombatOffhandRescueIncludesEnchantAndImbueDerivedModifiers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var ironAmplifier = new ItemStack(ItemRegistry.IRON_SPELL_AMPLIFIER.get());
            ironAmplifier.enchant(EnchantmentRegistry.SURGE.get(), 1);
            var rescuedIronModifiers =
                    jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat
                            .buildRescueModifiers(ironAmplifier);

            var rescuedSpellPowerBonus = sumModifierAmount(
                    rescuedIronModifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(rescuedSpellPowerBonus - 0.07D) < 1.0e-9D,
                    "Better Combat rescue should keep Iron Spell Amplifier + Surge at +0.07 spell power but got "
                            + rescuedSpellPowerBonus + " modifiers=" + describeModifiers(rescuedIronModifiers));

            var copperAmplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var copperAmplifier = new ItemStack(copperAmplifierItem);
            copperAmplifierItem.initializeSpellContainer(copperAmplifier);
            copperAmplifier.enchant(EnchantmentRegistry.ATTUNEMENT.get(), 1);
            var rescuedCopperModifiers =
                    jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat
                            .buildRescueModifiers(copperAmplifier);

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(copperAmplifier);
            helper.assertTrue(imbuedSchool != null,
                    "Copper Spell Amplifier rescue test could not resolve imbued school");
            var imbuedSpellPowerAttribute =
                    jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(imbuedSpellPowerAttribute != null,
                    "Copper Spell Amplifier rescue test could not resolve school spell power attribute");

            var rescuedAttunementBonus = sumModifierAmount(
                    rescuedCopperModifiers.get(imbuedSpellPowerAttribute),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            var expectedAttunementBonus = 0.10D + AttributeEnchantmentType.ATTUNEMENT.amountPerLevel();
            helper.assertTrue(Math.abs(rescuedAttunementBonus - expectedAttunementBonus) < 1.0e-9D,
                    "Better Combat rescue should keep Copper Spell Amplifier base + Attunement at "
                            + expectedAttunementBonus + " but got "
                            + rescuedAttunementBonus + " modifiers=" + describeModifiers(rescuedCopperModifiers));
        });
    }
    static void betterCombatRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var spellbreaker = ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spellbreaker")
            );
            helper.assertTrue(spellbreaker != null, "Missing irons_spellbooks:spellbreaker for Better Combat rescue test");

            var player = createBetterCombatHiddenOffhandPlayer(
                    helper,
                    new ItemStack(spellbreaker),
                    new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                    "better_combat_hidden_offhand_attribute_test"
            );
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Better Combat should hide getOffhandItem() for spellbreaker but returned " + player.getOffhandItem());

            var physicalOffhand =
                    jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat
                            .getPhysicalOffhandStack(player);
            helper.assertTrue(
                    physicalOffhand.is(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()),
                    "Physical offhand resolver should keep Silver Spell Amplifier but got " + physicalOffhand
            );
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat
                            .isRescueActive(player),
                    "Better Combat rescue should stay active while physical offhand stack exists"
            );

            var maxManaAttribute = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get();
            var expectedMaxManaBonus = sumModifierAmount(
                    jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat
                            .buildRescueModifiers(physicalOffhand)
                            .get(maxManaAttribute),
                    AttributeModifier.Operation.ADDITION
            );
            helper.assertTrue(expectedMaxManaBonus > 0.0D,
                    "Silver Spell Amplifier Better Combat rescue should provide positive max mana but got "
                            + expectedMaxManaBonus);

            var baseMaxMana = player.getAttributeValue(maxManaAttribute);
            jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat.sync(player);
            var rescuedMaxMana = player.getAttributeValue(maxManaAttribute);
            helper.assertTrue(Math.abs((rescuedMaxMana - baseMaxMana) - expectedMaxManaBonus) < 1.0e-9D,
                    "Better Combat rescue should restore Silver Spell Amplifier max mana by "
                            + expectedMaxManaBonus + " but changed from " + baseMaxMana + " to " + rescuedMaxMana);
        });
    }
    static void betterCombatHiddenNonOffhandMagicItemDoesNotApplyTranscendence(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var manaForceBlade = TranscendenceGameTestScenarios.createStack(
                    ItemRegistry.MANA_FORCE_BLADE.get(),
                    2,
                    spell
            );
            var player = createBetterCombatHiddenOffhandPlayer(
                    helper,
                    ItemStack.EMPTY,
                    manaForceBlade,
                    "better_combat_hidden_non_offhand_magic_transcendence_test"
            );

            var physicalOffhand = player.getInventory().offhand.get(0);
            helper.assertTrue(physicalOffhand.is(ItemRegistry.MANA_FORCE_BLADE.get()),
                    "Physical offhand should retain Mana Force Blade but got " + physicalOffhand);
            helper.assertFalse(physicalOffhand.getItem() instanceof AbstractOffhandMagicItem,
                    "Transcendence negative case must use an item outside AbstractOffhandMagicItem rescue scope");
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Better Combat should hide a two-handed physical offhand item from logical equipment access");

            // 物理スロットの無条件直読みを防ぎ、明示的なoffhand補助具だけを救済対象に保つ。
            TranscendenceGameTestScenarios.assertEventLevel(
                    helper,
                    player,
                    spell,
                    1,
                    "Hidden non-offhand magic item should not apply Transcendence"
            );
        });
    }
    static void betterCombatSpellSelectionRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var spellbreaker = ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spellbreaker")
            );
            helper.assertTrue(spellbreaker != null, "Missing irons_spellbooks:spellbreaker for Better Combat spell rescue test");

            var copperAmplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var copperAmplifier = new ItemStack(copperAmplifierItem);
            copperAmplifierItem.initializeSpellContainer(copperAmplifier);
            var expectedSpell = ISpellContainer.get(copperAmplifier).getSpellAtIndex(0);
            helper.assertTrue(expectedSpell != SpellData.EMPTY,
                    "Copper Spell Amplifier should expose a fixed offhand spell for Better Combat spell rescue test");

            var player = createBetterCombatHiddenOffhandPlayer(
                    helper,
                    new ItemStack(spellbreaker),
                    copperAmplifier,
                    "better_combat_hidden_offhand_spell_test"
            );
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Better Combat should hide getOffhandItem() for spellbreaker spell rescue but returned "
                            + player.getOffhandItem());

            var selectionManager = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player);
            var offhandSelections = selectionManager.getSpellsForSlot(io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND);
            helper.assertTrue(offhandSelections.size() == 1,
                    "Better Combat spell rescue should add exactly one fixed offhand spell but got "
                            + offhandSelections.size() + " selections=" + offhandSelections);

            var rescuedSpell = selectionManager.getSpellForSlot(
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND,
                    0
            );
            helper.assertTrue(
                    rescuedSpell != SpellData.EMPTY
                            && rescuedSpell.getSpell().equals(expectedSpell.getSpell())
                            && rescuedSpell.getLevel() == expectedSpell.getLevel(),
                    "Better Combat spell rescue should restore Copper Spell Amplifier fixed spell "
                            + expectedSpell + " but got " + rescuedSpell
            );
        });
    }
    static void betterCombatScrollcasterGauntletRescueUsesPhysicalOffhandInventoryStack(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("bettercombat")) {
                return;
            }

            var spellbreaker = ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spellbreaker")
            );
            helper.assertTrue(spellbreaker != null, "Missing irons_spellbooks:spellbreaker for Better Combat Scrollcaster test");

            var expectedSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(expectedSpell));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);
            SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(gauntlet, 0, new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get()));

            var player = createBetterCombatHiddenOffhandPlayer(
                    helper,
                    new ItemStack(spellbreaker),
                    gauntlet,
                    "better_combat_hidden_scrollcaster_spell_test"
            );
            helper.assertTrue(player.getOffhandItem().isEmpty(),
                    "Better Combat should hide getOffhandItem() for spellbreaker Scrollcaster test but returned "
                            + player.getOffhandItem());
            helper.assertFalse(((ScrollcasterGauntlet) gauntlet.getItem()).tryTriggerSpellOnSwing(player, InteractionHand.OFF_HAND, true),
                    "Scrollcaster Gauntlet freecast swing should not resolve Better Combat hidden physical offhand stacks");
            helper.assertFalse(
                    jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat
                            .isRescueActive(player),
                    "Scrollcaster Gauntlet should not join the Better Combat attribute rescue path"
            );
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatScrollcasterGauntletCompat
                            .isRescueActive(player),
                    "Scrollcaster Gauntlet should join only the Better Combat magic-holder rescue path"
            );

            var resolvedStack =
                    jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatScrollcasterGauntletCompat
                            .getResolvedHeldStack(player, InteractionHand.OFF_HAND);
            helper.assertTrue(resolvedStack.is(ItemRegistry.SCROLLCASTER_GAUNTLET.get()),
                    "Scrollcaster resolver should return the physical offhand gauntlet but got " + resolvedStack);

            var selectionManager = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player);
            var offhandSelections = selectionManager.getSpellsForSlot(
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND
            );
            helper.assertTrue(offhandSelections.size() == 1,
                    "Better Combat Scrollcaster rescue should add exactly one selected offhand spell but got "
                            + offhandSelections.size() + " selections=" + offhandSelections);

            var rescuedSpell = selectionManager.getSpellForSlot(
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND,
                    0
            );
            helper.assertTrue(
                    rescuedSpell != SpellData.EMPTY
                            && rescuedSpell.getSpell().equals(expectedSpell)
                            && rescuedSpell.getLevel() == 1,
                    "Better Combat Scrollcaster rescue should restore selected spell "
                            + expectedSpell.getSpellResource() + " but got " + rescuedSpell
            );
        });
    }
    static void enchantedCircletCurioBonusesMirrorOffhandMagicEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = createInitializedPresetStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            var item = (top.theillusivec4.curios.api.type.capability.ICurioItem) stack.getItem();
            var slotContext = new top.theillusivec4.curios.api.SlotContext(
                    CuriosSlotConstants.HEAD,
                    helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(0, 2, 0)),
                    0,
                    false,
                    true
            );

            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    -0.10D,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Enchanted Circlet attack damage penalty regression"
            );

            ISpellContainer.createImbuedContainer(io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(), 1, stack);
            stack.enchant(EnchantmentRegistry.ALACRITY.get(), 1);
            stack.enchant(EnchantmentRegistry.REFLUX.get(), 1);
            stack.enchant(EnchantmentRegistry.RESERVOIR.get(), 1);
            stack.enchant(EnchantmentRegistry.SURGE.get(), 1);
            stack.enchant(EnchantmentRegistry.ATTUNEMENT.get(), 1);
            stack.enchant(EnchantmentRegistry.TENSE.get(), 1);

            var imbuedSchool = jp.aquafactory.apprenticecodex.utility.MagicTools.getImbuedSpellSchool(stack);
            helper.assertTrue(imbuedSchool != null, "Enchanted Circlet imbued school could not be resolved");

            var resolvedSpellPower = jp.aquafactory.apprenticecodex.utility.MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(resolvedSpellPower != null,
                    "Enchanted Circlet could not resolve spell power attribute for Attunement: " + imbuedSchool.getId());

            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION.get(),
                    0.02D,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Enchanted Circlet Alacrity regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN.get(),
                    0.05D,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Enchanted Circlet Reflux regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get(),
                    20.0D,
                    AttributeModifier.Operation.ADDITION,
                    "Enchanted Circlet Reservoir regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get(),
                    0.02D,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Enchanted Circlet Surge regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    resolvedSpellPower,
                    AttributeEnchantmentType.ATTUNEMENT.amountPerLevel(),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Enchanted Circlet Attunement regression"
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    stack,
                    io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION.get(),
                    AttributeEnchantmentType.TENSE.amountPerLevel(),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    "Enchanted Circlet Tense regression"
            );
        });
    }
    static void enchantedCircletCurioModifiersStayIndependentAcrossSlots(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (top.theillusivec4.curios.api.type.capability.ICurioItem) ItemRegistry.ENCHANTED_CIRCLET.get();
            var wearer = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(0, 2, 0));
            var headZero = new top.theillusivec4.curios.api.SlotContext(
                    CuriosSlotConstants.HEAD, wearer, 0, false, true
            );
            var headOne = new top.theillusivec4.curios.api.SlotContext(
                    CuriosSlotConstants.HEAD, wearer, 1, false, true
            );
            var genericCurio = new top.theillusivec4.curios.api.SlotContext(
                    "curio", wearer, 0, false, true
            );
            var spellPower = io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get();

            var headZeroStack = createInitializedPresetStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            headZeroStack.enchant(EnchantmentRegistry.SURGE.get(), 1);
            var headOneStack = headZeroStack.copy();
            var genericCurioStack = headZeroStack.copy();

            var headZeroModifiers = item.getAttributeModifiers(
                            headZero,
                            top.theillusivec4.curios.api.CuriosApi.getSlotUuid(headZero),
                            headZeroStack
                    ).get(spellPower).stream()
                    .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE)
                    .toList();
            var headOneModifiers = item.getAttributeModifiers(
                            headOne,
                            top.theillusivec4.curios.api.CuriosApi.getSlotUuid(headOne),
                            headOneStack
                    ).get(spellPower).stream()
                    .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE)
                    .toList();
            var genericCurioModifierMap = item.getAttributeModifiers(
                    genericCurio,
                    top.theillusivec4.curios.api.CuriosApi.getSlotUuid(genericCurio),
                    genericCurioStack
            );
            var genericCurioModifiers = genericCurioModifierMap.get(spellPower).stream()
                    .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE)
                    .toList();

            helper.assertTrue(headZeroModifiers.size() == 1
                            && headOneModifiers.size() == 1
                            && genericCurioModifiers.size() == 1,
                    "Every Curios slot should expose one Enchanted Circlet Surge modifier");
            helper.assertFalse(headZeroModifiers.get(0).getId().equals(headOneModifiers.get(0).getId()),
                    "Expanded head slots must use different Enchanted Circlet modifier UUIDs");
            helper.assertFalse(headZeroModifiers.get(0).getId().equals(genericCurioModifiers.get(0).getId()),
                    "Generic Curios slots must use different Enchanted Circlet modifier UUIDs");
            helper.assertTrue(Math.abs(sumModifierAmount(
                            genericCurioModifierMap.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                            AttributeModifier.Operation.MULTIPLY_BASE) + 0.10D) < 1.0e-9D,
                    "Generic Curios slot should retain the Enchanted Circlet attack damage penalty");

            var attributeInstance = new AttributeInstance(spellPower, unused -> {
            });
            headZeroModifiers.forEach(attributeInstance::addTransientModifier);
            headOneModifiers.forEach(attributeInstance::addTransientModifier);
            genericCurioModifiers.forEach(attributeInstance::addTransientModifier);
            helper.assertTrue(Math.abs(sumModifierAmount(
                            attributeInstance.getModifiers(), AttributeModifier.Operation.MULTIPLY_BASE) - 0.06D) < 1.0e-9D,
                    "Three Enchanted Circlet Surge modifiers should stack to 0.06");

            headZeroModifiers.forEach(modifier -> attributeInstance.removeModifier(modifier.getId()));
            helper.assertTrue(Math.abs(sumModifierAmount(
                            attributeInstance.getModifiers(), AttributeModifier.Operation.MULTIPLY_BASE) - 0.04D) < 1.0e-9D,
                    "Removing one Enchanted Circlet should leave the other slot modifiers active");
        });
    }
    static void enchantedCircletWorkbenchExtractionTagDoesNotAffectAshenCirclet(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertTrue(new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get()).is(TagRegistry.Items.SPELLCASTER_WORKBENCH_EXTRACTABLE),
                    "Enchanted Circlet should be extractable in Spellcaster Workbench");
            helper.assertFalse(new ItemStack(ItemRegistry.ASHEN_CIRCLET.get()).is(TagRegistry.Items.SPELLCASTER_WORKBENCH_EXTRACTABLE),
                    "Ashen Circlet should remain non-extractable in Spellcaster Workbench");
        });
    }
    static void enchantedCircletWisdomMatchesArmorRate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "enchanted_circlet_wisdom_test"));
            var baseExperience = 20;

            var withoutCirclet = new LivingExperienceDropEvent(helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0)), player, baseExperience);
            MinecraftForge.EVENT_BUS.post(withoutCirclet);
            helper.assertTrue(withoutCirclet.getDroppedExperience() == baseExperience,
                    "Wisdom baseline should stay unchanged without enchanted circlet");

            var circletStack = createInitializedPresetStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            circletStack.enchant(EnchantmentRegistry.WISDOM.get(), 1);
            var backCircletStack = circletStack.copy();

            var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                    .orElseThrow(() -> new IllegalStateException("Missing curios inventory for wisdom test"));
            curiosInventory.setEquippedCurio(CuriosSlotConstants.HEAD, 0, circletStack);
            curiosInventory.setEquippedCurio(CuriosSlotConstants.BACK, 0, backCircletStack);

            var withCirclet = new LivingExperienceDropEvent(helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0)), player, baseExperience);
            MinecraftForge.EVENT_BUS.post(withCirclet);
            helper.assertTrue(withCirclet.getDroppedExperience() == 22,
                    "Wisdom from head and non-head Curios slots should stack to +10% but got " + withCirclet.getDroppedExperience());

            var roundedUp = new LivingExperienceDropEvent(helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0)), player, 1);
            MinecraftForge.EVENT_BUS.post(roundedUp);
            helper.assertTrue(roundedUp.getDroppedExperience() == 2,
                    "Wisdom should round enemy experience up from 1 to 2 at +5% but got " + roundedUp.getDroppedExperience());
        });
    }
    static void wisdomAppliesToBlockBreakExperienceAndRoundsUp(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var state = Blocks.DIAMOND_ORE.defaultBlockState();

            var baselinePlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "wisdom_block_break_baseline_test"));
            var baselineExperience = new BlockEvent.BreakEvent(level, new BlockPos(0, 2, 0), state, baselinePlayer);
            baselineExperience.setExpToDrop(3);
            WisdomExperienceDropEvent.onBlockBreak(baselineExperience);
            helper.assertTrue(baselineExperience.getExpToDrop() == 3,
                    "Block experience should stay unchanged without Wisdom but got " + baselineExperience.getExpToDrop());

            var curioPlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "wisdom_block_break_curio_test"));
            var circletStack = createInitializedPresetStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            circletStack.enchant(EnchantmentRegistry.WISDOM.get(), 1);

            var curiosInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(curioPlayer)
                    .orElseThrow(() -> new IllegalStateException("Missing curios inventory for block wisdom test"));
            curiosInventory.setEquippedCurio(CuriosSlotConstants.HEAD, 0, circletStack);

            var roundedCurioExperience = new BlockEvent.BreakEvent(level, new BlockPos(1, 2, 0), state, curioPlayer);
            roundedCurioExperience.setExpToDrop(1);
            WisdomExperienceDropEvent.onBlockBreak(roundedCurioExperience);
            helper.assertTrue(roundedCurioExperience.getExpToDrop() == 2,
                    "Curio Wisdom should round block experience up from 1 to 2 at +5% but got " + roundedCurioExperience.getExpToDrop());

            var heldPlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "wisdom_block_break_held_test"));
            var spellGunStack = new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get());
            spellGunStack.enchant(EnchantmentRegistry.WISDOM.get(), 1);
            heldPlayer.setItemInHand(InteractionHand.MAIN_HAND, spellGunStack);

            var heldExperience = new BlockEvent.BreakEvent(level, new BlockPos(2, 2, 0), state, heldPlayer);
            heldExperience.setExpToDrop(3);
            WisdomExperienceDropEvent.onBlockBreak(heldExperience);
            helper.assertTrue(heldExperience.getExpToDrop() == 4,
                    "Held Wisdom should increase block experience from 3 to 4 at +20% but got " + heldExperience.getExpToDrop());

            assertHeldWisdomBlockExperience(helper, level, state, ItemRegistry.PASTEL_STAFF.get(), 3, 4,
                    "Pastel Staff");
            assertHeldWisdomBlockExperience(helper, level, state, ItemRegistry.MULTICAST_ECHO_STAFF.get(), 3, 4,
                    "Multicast Echo Staff");
        });
    }
}
