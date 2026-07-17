package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;

import java.util.ArrayList;

import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.ItemManaBypassCastEvent;
import jp.aquafactory.apprenticecodex.item.ManaBypassSpellItem;
import jp.aquafactory.apprenticecodex.item.OffhandUsePriorityHelper;
import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepterAttackEvent;
import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepterFallProtectionEvent;
import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepterReadyStateSyncEvent;
import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepter;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

final class RightClickMagicWeaponGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private RightClickMagicWeaponGameTestScenarios() {
    }

    static void smashcastScepterKeepsExpectedStatsAndImbueRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get());
            var item = (SmashcastScepter) stack.getItem();
            var modifiers = item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack);

            assertSingleModifierAmount(
                    helper,
                    modifiers.get(Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.ADDITION,
                    SmashcastScepter.ATTACK_DAMAGE_MODIFIER,
                    "Smashcast Scepter attack damage modifier changed"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(Attributes.ATTACK_SPEED),
                    AttributeModifier.Operation.ADDITION,
                    SmashcastScepter.ATTACK_SPEED_MODIFIER,
                    "Smashcast Scepter attack speed modifier changed"
            );
            helper.assertTrue(stack.getMaxDamage() == 0,
                    "Smashcast Scepter should stay non-damageable");
            helper.assertFalse(stack.is(SPELLCASTER_WORKBENCH_EXTRACTABLE),
                    "Smashcast Scepter should not need the legacy Spellcaster Workbench extraction tag");
            helper.assertTrue(stack.is(MALUM_SOUL_HUNTER_WEAPON),
                    "Smashcast Scepter should join Malum soul hunter weapon tag");
            helper.assertTrue(stack.is(TOMAGIC_REVERSAL_WEAPON),
                    "Smashcast Scepter should join Travel Optics reversal tag");
            helper.assertFalse(stack.is(IRONS_STAFF),
                    "Smashcast Scepter should not join Iron's staff tag");
            helper.assertFalse(stack.getItem() instanceof io.redspace.ironsspellbooks.item.UniqueItem,
                    "Smashcast Scepter should not block external imbue as a UniqueItem");
            helper.assertFalse(stack.getItem() instanceof ManaBypassSpellItem,
                    "Smashcast Scepter should consume normal spell mana");

            item.initializeSpellContainer(stack);
            helper.assertTrue(Utils.canImbue(stack),
                    "Smashcast Scepter should be accepted by Iron's imbuement checks after initialization");
            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Smashcast Scepter spell container is null");
            helper.assertTrue(spellContainer != null && spellContainer.getMaxSpellCount() == 1,
                    "Smashcast Scepter should expose exactly one imbue slot");
            helper.assertTrue(spellContainer != null && spellContainer.isSpellWheel(),
                    "Smashcast Scepter imbue slot should be visible to the spell wheel");
            var removableMutable = ISpellContainer.create(1, true, false).mutableCopy();
            helper.assertTrue(removableMutable.addSpellAtIndex(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1, 0, false),
                    "Smashcast Scepter removable imbue test setup failed");
            ISpellContainer.set(stack, removableMutable.toImmutable());
            var removableContainer = ISpellContainer.get(stack);
            var removableSpellData = removableContainer == null ? SpellData.EMPTY : removableContainer.getSpellAtIndex(0);
            helper.assertTrue(removableContainer != null && item.canRemoveWorkbenchSpell(stack, removableContainer, 0, removableSpellData),
                    "Smashcast Scepter imbue slot should be removable at the Spellcaster Workbench");
            helper.assertTrue(item.canImbueSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1),
                    "Smashcast Scepter should accept non-continuous imbues");
            var arcaneBlastResult = item.createArcaneAnvilImbueResult(stack, new SpellData(SpellRegistry.ARCANE_BLAST.get(), 1));
            var arcaneBlastContainer = ISpellContainer.get(arcaneBlastResult);
            helper.assertTrue(arcaneBlastContainer != null,
                    "Smashcast Scepter Arcane Blast imbue result should keep a spell container");
            assertSpellData(helper, arcaneBlastContainer, 0, SpellRegistry.ARCANE_BLAST.get(), 1, false,
                    "Smashcast Scepter should accept Arcane Blast as a removable imbue");
            helper.assertTrue(arcaneBlastContainer != null
                            && item.canRemoveWorkbenchSpell(arcaneBlastResult, arcaneBlastContainer, 0, arcaneBlastContainer.getSpellAtIndex(0)),
                    "Smashcast Scepter Arcane Blast imbue should remain extractable");
            helper.assertFalse(item.canImbueSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(), 1),
                    "Smashcast Scepter should reject CONTINUOUS imbues");

            var mutable = ISpellContainer.create(1, true, false).mutableCopy();
            helper.assertTrue(mutable.addSpellAtIndex(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get(), 1, 0, false),
                    "Smashcast Scepter continuous imbue test setup failed");
            ISpellContainer.set(stack, mutable.toImmutable());
            item.normalizeImbuedSpellContainer(stack);
            var normalizedContainer = ISpellContainer.get(stack);
            helper.assertTrue(normalizedContainer != null && normalizedContainer.getSpellAtIndex(0) == SpellData.EMPTY,
                    "Smashcast Scepter should clear rejected CONTINUOUS imbues during normalization");
            assertClose(helper, SmashcastScepter.calculateReleaseBounceImpulse(0),
                    0.0D, 1.0E-6D, "Smashcast Scepter Release level 0 should not launch");
            assertClose(helper, SmashcastScepter.calculateReleaseBounceImpulse(1),
                    1.2D, 1.0E-6D, "Smashcast Scepter Release level 1 should match Wind Burst");
            assertClose(helper, SmashcastScepter.calculateReleaseBounceImpulse(2),
                    1.75D, 1.0E-6D, "Smashcast Scepter Release level 2 should match Wind Burst");
            assertClose(helper, SmashcastScepter.calculateReleaseBounceImpulse(3),
                    2.2D, 1.0E-6D, "Smashcast Scepter Release level 3 should match Wind Burst");
            assertClose(helper, SmashcastScepter.calculateReleaseBounceImpulse(4),
                    2.55D, 1.0E-6D, "Smashcast Scepter Release should use Wind Burst fallback above level 3");
            assertClose(helper, SmashcastScepter.calculateReleaseBounceImpulse(5),
                    2.9D, 1.0E-6D, "Smashcast Scepter Release should not clamp above level 3");

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 4, 0), "smashcast_scepter_pending_test");
            player.setOnGround(false);
            player.fallDistance = SmashcastScepter.SMASH_ATTACK_FALL_DISTANCE_THRESHOLD + 1.0F;
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            helper.assertTrue(SmashcastScepterAttackEvent.canRegisterSmashcast(player, stack),
                    "Smashcast Scepter should register a smash without an imbued spell");
            helper.assertTrue(SmashcastScepterReadyStateSyncEvent.resolveReadyState(player),
                    "Smashcast Scepter server ready state should match a valid smash attack");
            helper.assertFalse(item.tryCastSmashSpell(player, stack, player.fallDistance),
                    "Smashcast Scepter should skip spell casting without an imbued spell");
            player.fallDistance = 0.0F;
            helper.assertFalse(SmashcastScepterReadyStateSyncEvent.resolveReadyState(player),
                    "Smashcast Scepter server ready state should reject reset fall distance");
            player.fallDistance = SmashcastScepter.SMASH_ATTACK_FALL_DISTANCE_THRESHOLD + 1.0F;
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0));
            helper.assertFalse(SmashcastScepterReadyStateSyncEvent.resolveReadyState(player),
                    "Smashcast Scepter server ready state should reject Slow Falling");
            player.removeEffect(MobEffects.SLOW_FALLING);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            helper.assertFalse(SmashcastScepterReadyStateSyncEvent.resolveReadyState(player),
                    "Smashcast Scepter server ready state should require mainhand Smashcast Scepter");
            player.setItemInHand(InteractionHand.MAIN_HAND, arcaneBlastResult);
            helper.assertTrue(SmashcastScepterAttackEvent.canRegisterSmashcast(player, arcaneBlastResult),
                    "Smashcast Scepter should register a smashcast when a valid spell is imbued");
            var arcaneBlastSpell = SpellRegistry.ARCANE_BLAST.get();
            var arcaneBlastManaCost = arcaneBlastSpell.getManaCost(1);
            MagicData.getPlayerMagicData(player).setPlayerCastingItem(arcaneBlastResult);
            var arcaneBlastManaEvent = new SpellOnCastEvent(
                    player,
                    arcaneBlastSpell.getSpellId(),
                    1,
                    arcaneBlastManaCost,
                    arcaneBlastSpell.getSchoolType(),
                    CastSource.SWORD
            );
            ItemManaBypassCastEvent.onSpellCast(arcaneBlastManaEvent);
            helper.assertTrue(arcaneBlastManaEvent.getManaCost() == arcaneBlastManaCost,
                    "Smashcast Scepter should keep normal spell mana cost: " + arcaneBlastManaEvent.getManaCost());

            var compressedStack = new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get());
            compressedStack.enchant(EnchantmentRegistry.COMPRESS.get(), 2);
            assertClose(helper, SmashcastScepter.calculateSmashBonusDamage(new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get()), 5.0F),
                    16.0D, 1.0E-6D, "Smashcast Scepter mace-like smash damage changed");
            assertClose(helper, SmashcastScepter.calculateSmashBonusDamage(compressedStack, 5.0F),
                    21.0D, 1.0E-6D, "Smashcast Scepter Compress damage bonus changed");
            assertClose(helper, SmashcastScepter.calculateSmashSpellPowerMultiplier(new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get()), 15.0F),
                    2.25D, 1.0E-6D, "Smashcast Scepter spell power falloff changed");
            assertClose(helper, SmashcastScepter.calculateSmashSpellPowerMultiplier(compressedStack, 15.0F),
                    2.85D, 1.0E-6D, "Smashcast Scepter Compress spell power bonus changed");
            assertClose(helper, SmashcastScepter.calculateSmashSpellPowerMultiplier(compressedStack, 200.0F),
                    11.0D, 1.0E-6D, "Smashcast Scepter spell power cap changed");

            var epicFightStack = new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, epicFightStack);
            var target = EntityType.ARMOR_STAND.create(helper.getLevel());
            helper.assertTrue(target != null, "Armor Stand target could not be created for Epic Fight Smashcast test");
            target.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 1, 0))));
            helper.getLevel().addFreshEntity(target);
            var secondTarget = EntityType.ARMOR_STAND.create(helper.getLevel());
            helper.assertTrue(secondTarget != null, "Second Armor Stand target could not be created for Epic Fight Smashcast test");
            secondTarget.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(1, 1, 0))));
            helper.getLevel().addFreshEntity(secondTarget);

            var epicFightFallDistance = 4.0F;
            var expectedEpicFightBonus = SmashcastScepter.calculateSmashBonusDamage(epicFightStack, epicFightFallDistance);
            var damageSource = player.damageSources().playerAttack(player);
            var firstEpicFightDamage = new net.minecraftforge.event.entity.living.LivingDamageEvent(target, damageSource, 1.0F);
            SmashcastScepterAttackEvent.registerEpicFightSmashcastImpact(
                    player,
                    target,
                    firstEpicFightDamage,
                    epicFightFallDistance
            );
            assertClose(helper, firstEpicFightDamage.getAmount(), 1.0F + expectedEpicFightBonus, 1.0E-6D,
                    "Epic Fight Smashcast should apply bonus damage to the first target");

            var duplicateEpicFightDamage = new net.minecraftforge.event.entity.living.LivingDamageEvent(target, damageSource, 1.0F);
            SmashcastScepterAttackEvent.registerEpicFightSmashcastImpact(
                    player,
                    target,
                    duplicateEpicFightDamage,
                    epicFightFallDistance
            );
            assertClose(helper, duplicateEpicFightDamage.getAmount(), 1.0F, 1.0E-6D,
                    "Epic Fight Smashcast should not apply duplicate bonus damage to the same target in one tick");

            var secondEpicFightDamage = new net.minecraftforge.event.entity.living.LivingDamageEvent(secondTarget, damageSource, 1.0F);
            SmashcastScepterAttackEvent.registerEpicFightSmashcastImpact(
                    player,
                    secondTarget,
                    secondEpicFightDamage,
                    epicFightFallDistance
            );
            assertClose(helper, secondEpicFightDamage.getAmount(), 1.0F + expectedEpicFightBonus, 1.0E-6D,
                    "Epic Fight Smashcast should keep bonus damage for a different target in the same tick");

        });
    }
    static void smashcastScepterFallProtectionKeepsFallDistanceAndCancelsNextFall(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 4, 0),
                    "smashcast_scepter_fall_protection_test");
            player.fallDistance = 6.0F;
            player.setDeltaMovement(0.12D, -0.8D, -0.08D);

            SmashcastScepterFallProtectionEvent.register(player);

            helper.assertTrue(player.fallDistance == 6.0F,
                    "Smashcast Scepter fall protection should not reset fall distance");
            helper.assertTrue(Math.abs(player.getDeltaMovement().y - SmashcastScepter.WIND_BURST_MOTION_EPSILON) < 1.0E-6D,
                    "Smashcast Scepter fall protection should mimic vanilla Mace landing motion");

            var protectedFall = postLivingFallEventForGameTest(player, 6.0F, 1.0F);
            helper.assertTrue(protectedFall.isCanceled(),
                    "Smashcast Scepter fall protection should cancel the next fall damage event");

            var repeatedFall = postLivingFallEventForGameTest(player, 6.0F, 1.0F);
            helper.assertFalse(repeatedFall.isCanceled(),
                    "Smashcast Scepter fall protection should be consumed after one fall event");

            var unprotectedPlayer = createTrackedEquipmentTestPlayer(helper, new BlockPos(2, 4, 0),
                    "smashcast_scepter_unprotected_fall_test");
            var unprotectedFall = postLivingFallEventForGameTest(unprotectedPlayer, 6.0F, 1.0F);
            helper.assertFalse(unprotectedFall.isCanceled(),
                    "Smashcast Scepter fall protection should not cancel unrelated player falls");
        });
    }
    static void smashcastScepterFallProtectionExpiresAfterGracePeriod(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 4, 0),
                "smashcast_scepter_fall_protection_expire_test");
        SmashcastScepterFallProtectionEvent.register(player);

        helper.runAfterDelay(45, () -> {
            var expiredFall = postLivingFallEventForGameTest(player, 6.0F, 1.0F);
            helper.assertFalse(expiredFall.isCanceled(),
                    "Smashcast Scepter fall protection should expire before late fall damage");
            helper.succeed();
        });
    }
    static void rightClickMagicWeaponTooltipsStartWithOffhandPriorityHint(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var rightClickMagicWeapons = ForgeRegistries.ITEMS.getValues().stream()
                    .filter(item -> item instanceof AbstractRightClickMagicWeaponItem && !(item instanceof SmashcastScepter))
                    .toList();
            helper.assertTrue(!rightClickMagicWeapons.isEmpty(),
                    "Right Click Magic Weapon tooltip test found no target items");

            for (var item : rightClickMagicWeapons) {
                var stack = new ItemStack(item);
                var tooltipLines = new ArrayList<Component>();
                item.appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(!tooltipLines.isEmpty(),
                        item + " should expose right click magic weapon tooltip");
                helper.assertTrue(tooltipLines.size() > 1,
                        item + " should expose right click magic weapon item type tooltip");
                assertTranslatableKey(
                        helper,
                        tooltipLines.get(0),
                        "item.apprenticecodex.right_click_magic_weapon.desc",
                        item + " should show offhand priority tooltip first"
                );
                assertTranslatableKey(
                        helper,
                        tooltipLines.get(1),
                        "item.apprenticecodex.right_click_magic_weapon.item_type",
                        item + " should show offhand priority item type tooltip second"
                );
            }

            assertTooltipKeyAt(
                    helper,
                    new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get()),
                    2,
                    "item.apprenticecodex.crystal_bladed_staff.swing_miss.desc",
                    "Crystal Bladed Staff should show miss-swing tooltip after offhand priority tooltips"
            );
            assertTooltipKeyAt(
                    helper,
                    new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get()),
                    3,
                    "item.apprenticecodex.crystal_bladed_staff.desc",
                    "Crystal Bladed Staff should keep its mana orb tooltip after swingcast tooltip"
            );
            assertTooltipKeyAt(
                    helper,
                    new ItemStack(ItemRegistry.COPPER_SWINGCAST_STAFF.get()),
                    2,
                    "item.apprenticecodex.swingcast.common.desc",
                    "Swingcast Staff should show swingcast tooltip after offhand priority tooltips"
            );
            assertTooltipKeyAt(
                    helper,
                    new ItemStack(ItemRegistry.REVOLVERCAST_STAFF.get()),
                    2,
                    "item.apprenticecodex.swingcast.common.desc",
                    "Revolvercast Staff should show swingcast tooltip after offhand priority tooltips"
            );
            assertTooltipKeyUsesColor(
                    helper,
                    new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()),
                    "item.apprenticecodex.spellgun.tooltip.hint",
                    ChatFormatting.YELLOW,
                    "Spell Gun shift hint should stand out"
            );
            assertTooltipKeyUsesColor(
                    helper,
                    new ItemStack(ItemRegistry.COPPER_SWINGCAST_STAFF.get()),
                    "item.apprenticecodex.spellgun.tooltip.hint",
                    ChatFormatting.YELLOW,
                    "Swingcast Staff shift hint should stand out"
            );
            assertTooltipKeyUsesColor(
                    helper,
                    new ItemStack(ItemRegistry.REFLECTCAST_SHIELD.get()),
                    "item.apprenticecodex.spellgun.tooltip.hint",
                    ChatFormatting.YELLOW,
                    "Reflectcast Shield shift hint should stand out"
            );
        });
    }

    static void rightClickMagicWeaponPrioritizesSupportedOffhandUseItems(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertRightClickMagicWeaponPrioritizesOffhandUse(
                    helper,
                    new ItemStack(Items.SHIELD),
                    "right_click_magic_weapon_offhand_shield_test"
            );
            assertRightClickMagicWeaponPrioritizesOffhandUse(
                    helper,
                    new ItemStack(ItemRegistry.ELEMENTAL_BOW.get()),
                    "right_click_magic_weapon_offhand_elemental_bow_test"
            );
            assertRightClickMagicWeaponPrioritizesOffhandUse(
                    helper,
                    createIronAutoloaderCrossbowStack(helper),
                    "right_click_magic_weapon_offhand_autoloader_crossbow_test"
            );
            assertRightClickMagicWeaponPrioritizesOffhandUse(
                    helper,
                    new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()),
                    "right_click_magic_weapon_offhand_spellgun_test"
            );
        });
    }

    private static void assertRightClickMagicWeaponPrioritizesOffhandUse(
            GameTestHelper helper,
            ItemStack offhandStack,
            String profileName
    ) {
        helper.assertTrue(OffhandUsePriorityHelper.isPriorityOffhandUseItem(offhandStack),
                "Expected a supported priority offhand use item but got " + offhandStack);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        var mainhandStack = new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, mainhandStack);
        player.setItemInHand(InteractionHand.OFF_HAND, offhandStack.copy());
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null,
                "Right click magic weapon offhand priority test could not resolve player mana data");
        magicData.setMana(100.0F);

        var result = mainhandStack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.PASS,
                "Right click magic weapon should pass to supported offhand use item " + offhandStack
                        + " but got " + result.getResult());
        helper.assertFalse(magicData.isCasting(),
                "Right click magic weapon should not cast before supported offhand use item " + offhandStack);
    }

    private static ItemStack createIronAutoloaderCrossbowStack(GameTestHelper helper) {
        var autoloaderCrossbow = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "autoloader_crossbow")
        );
        helper.assertTrue(autoloaderCrossbow != null,
                "Missing irons_spellbooks:autoloader_crossbow for offhand priority test");
        return new ItemStack(autoloaderCrossbow);
    }
}
