package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.entity.spells.fire_breath.FireBreathProjectile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.item.spellthrowablecard.AbstractSpellThrowableCardItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastAnchorEntity;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileManager;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhialProjectileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

final class ChargedTwinBladeStaffGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private ChargedTwinBladeStaffGameTestScenarios() {
    }

    static void chargedTwinBladeStaffUpgradeMergesMainhandMeleeDamage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ChargedTwinBladeStaff) ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get();
            var stack = new ItemStack(item);
            var upgradeData = createUpgradeData(
                    helper.getLevel().registryAccess(),
                    stack,
                    io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry.ATTACK_DAMAGE,
                    EquipmentSlot.MAINHAND.getName()
            );

            var event = new ItemAttributeModifierEvent(
                    stack,
                    EquipmentSlot.MAINHAND,
                    item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack)
            );
            MinecraftForge.EVENT_BUS.post(event);

            assertSingleModifierAmount(
                    helper,
                    event.getModifiers().get(Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.05D,
                    "Charged Twin Blade Staff melee damage upgrade should be a single display modifier"
                            + " upgradeData=" + upgradeData
                            + " modifiers=" + describeModifiers(event.getModifiers())
            );
        });
    }
    static void chargedTwinBladeStaffExposesExpectedMainhandAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);

            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.ADDITION
            ) - 10.0D) < 1.0e-9D, "Charged Twin Blade Staff attack damage regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED),
                    AttributeModifier.Operation.ADDITION
            ) - (-3.0D)) < 1.0e-9D, "Charged Twin Blade Staff attack speed regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE
            ) - 0.10D) < 1.0e-9D, "Charged Twin Blade Staff spell power regression: " + describeModifiers(modifiers));
        });
    }
    static void chargedTwinBladeStaffResolveThrownDamageIncludesApplicableEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var baseStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var baseDamage = ChargedTwinBladeStaff.resolveThrownDamage(baseStack, MobType.UNDEFINED);
            helper.assertTrue(Math.abs(baseDamage - 11.0D) < 1.0e-9D,
                    "Charged Twin Blade Staff base thrown damage regression: " + baseDamage);

            var sharpnessStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            sharpnessStack.enchant(Enchantments.SHARPNESS, 3);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    sharpnessStack,
                    MobType.UNDEFINED,
                    baseDamage + EnchantmentHelper.getDamageBonus(sharpnessStack, MobType.UNDEFINED),
                    "Charged Twin Blade Staff sharpness thrown damage regression"
            );

            var smiteStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            smiteStack.enchant(Enchantments.SMITE, 2);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    smiteStack,
                    MobType.UNDEAD,
                    baseDamage + EnchantmentHelper.getDamageBonus(smiteStack, MobType.UNDEAD),
                    "Charged Twin Blade Staff smite thrown damage regression"
            );
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    smiteStack,
                    MobType.UNDEFINED,
                    baseDamage + EnchantmentHelper.getDamageBonus(smiteStack, MobType.UNDEFINED),
                    "Charged Twin Blade Staff smite fallback damage regression"
            );

            var baneStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            baneStack.enchant(Enchantments.BANE_OF_ARTHROPODS, 2);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    baneStack,
                    MobType.ARTHROPOD,
                    baseDamage + EnchantmentHelper.getDamageBonus(baneStack, MobType.ARTHROPOD),
                    "Charged Twin Blade Staff bane thrown damage regression"
            );
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    baneStack,
                    MobType.UNDEFINED,
                    baseDamage + EnchantmentHelper.getDamageBonus(baneStack, MobType.UNDEFINED),
                    "Charged Twin Blade Staff bane fallback damage regression"
            );

            var impalingStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            impalingStack.enchant(Enchantments.IMPALING, 2);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    impalingStack,
                    MobType.WATER,
                    baseDamage + EnchantmentHelper.getDamageBonus(impalingStack, MobType.WATER),
                    "Charged Twin Blade Staff impaling thrown damage regression"
            );
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    impalingStack,
                    MobType.UNDEFINED,
                    baseDamage + EnchantmentHelper.getDamageBonus(impalingStack, MobType.UNDEFINED),
                    "Charged Twin Blade Staff impaling fallback damage regression"
            );
        });
    }
    static void chargedTwinBladeStaffThrowConsumesMana(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_throw_mana_test");
        var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Charged Twin Blade Staff mana test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.runAtTickTime(1, () -> stack.getItem().releaseUsing(
                stack,
                helper.getLevel(),
                player,
                stack.getUseDuration() - ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS
        ));
        helper.succeedWhen(() -> {
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Charged Twin Blade Staff normal throw should consume 100 mana but left " + magicData.getMana());
            var projectiles = helper.getLevel().getEntitiesOfClass(
                    jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity.class,
                    new AABB(player.blockPosition()).inflate(8.0D)
            );
            helper.assertTrue(!projectiles.isEmpty(), "Charged Twin Blade Staff throw did not spawn its projectile");
        });
    }
    static void chargedTwinBladeStaffLoyaltyReducesThrowManaCost(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_loyalty_mana_test");
        var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        stack.enchant(net.minecraft.world.item.enchantment.Enchantments.LOYALTY, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Charged Twin Blade Staff loyalty mana test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.runAtTickTime(1, () -> stack.getItem().releaseUsing(
                stack,
                helper.getLevel(),
                player,
                stack.getUseDuration() - ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS
        ));
        helper.succeedWhen(() -> helper.assertTrue(Math.abs(magicData.getMana() - (100.0F - 100.0F / 3.0F)) < 1.0e-3F,
                "Charged Twin Blade Staff loyalty mana discount regressed: " + magicData.getMana()));
    }
    static void chargedTwinBladeStaffRiptideWorksOnDryGroundWithoutProjectile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_riptide_test");
            var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            stack.enchant(net.minecraft.world.item.enchantment.Enchantments.RIPTIDE, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff riptide test could not resolve player mana data");
            magicData.setMana(50.0F);

            stack.getItem().releaseUsing(
                    stack,
                    helper.getLevel(),
                    player,
                    stack.getUseDuration() - ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS
            );
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Charged Twin Blade Staff riptide should consume 50 mana on dry ground");
            helper.assertTrue(player.getDeltaMovement().lengthSqr() > 0.01D,
                    "Charged Twin Blade Staff riptide should propel the player even without rain or water");
            var projectiles = helper.getLevel().getEntitiesOfClass(
                    jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity.class,
                    new AABB(player.blockPosition()).inflate(8.0D)
            );
            helper.assertTrue(projectiles.isEmpty(),
                    "Charged Twin Blade Staff riptide should not spawn a projectile");
        });
    }
    static void chargedTwinBladeStaffImpactForwardUsesHistoryAndFallback(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var historyResolved = jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity.resolveImpactForwardForTesting(
                    new Vec3(4.0D, 0.0D, 0.0D),
                    Vec3.ZERO,
                    new Vec3(1.0D, 0.0D, 0.0D)
            );
            helper.assertTrue(historyResolved.distanceTo(new Vec3(1.0D, 0.0D, 0.0D)) < 1.0E-6D,
                    "Charged Twin Blade Staff impact forward should prefer recent flight history: " + historyResolved);

            var shortHistoryFallback = jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity.resolveImpactForwardForTesting(
                    new Vec3(0.001D, 0.0D, 0.0D),
                    Vec3.ZERO,
                    new Vec3(0.0D, 0.0D, 1.0D)
            );
            helper.assertTrue(shortHistoryFallback.distanceTo(new Vec3(0.0D, 0.0D, 1.0D)) < 1.0E-6D,
                    "Charged Twin Blade Staff impact forward should fall back when history is too short: " + shortHistoryFallback);

            var reversedHistoryFallback = jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity.resolveImpactForwardForTesting(
                    new Vec3(-4.0D, 0.0D, 0.0D),
                    Vec3.ZERO,
                    new Vec3(1.0D, 0.0D, 0.0D)
            );
            helper.assertTrue(reversedHistoryFallback.distanceTo(new Vec3(1.0D, 0.0D, 0.0D)) < 1.0E-6D,
                    "Charged Twin Blade Staff impact forward should fall back when history reverses initial throw: " + reversedHistoryFallback);
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerCastsInstantAndLongSpells(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_impact_cast_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff impact cast test could not resolve player mana data");
            magicData.setMana(200.0F);
            var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var forward = new Vec3(0.0D, 0.0D, 1.0D);
            var spawnedInstantProjectiles = new ArrayList<io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile>();
            var spawnedLongProjectiles = new ArrayList<CompoundPhialProjectileEntity>();
            java.util.function.Consumer<EntityJoinLevelEvent> projectileListener = event -> {
                if (event.getLevel() != level || event.getEntity().position().distanceToSqr(impactPos) > 144.0D) {
                    return;
                }
                if (event.getEntity() instanceof io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile projectile) {
                    spawnedInstantProjectiles.add(projectile);
                } else if (event.getEntity() instanceof CompoundPhialProjectileEntity projectile) {
                    spawnedLongProjectiles.add(projectile);
                }
            };

            var instantPayload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "magic_missile"),
                    1,
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );
            MinecraftForge.EVENT_BUS.addListener(projectileListener);
            try {
                helper.assertTrue(
                        jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                                level, player, sourceStack, instantPayload, impactPos, forward
                        ),
                        "Charged Twin Blade Staff impact manager failed to cast an INSTANT payload"
                );
                helper.assertTrue(!spawnedInstantProjectiles.isEmpty(),
                        "Charged Twin Blade Staff INSTANT impact cast did not spawn Magic Missile projectiles");
                helper.assertTrue(spawnedInstantProjectiles.stream()
                                .anyMatch(projectile -> projectile.position().distanceTo(impactPos) < 2.0D),
                        "Charged Twin Blade Staff INSTANT impact cast spawned Magic Missile away from the impact point: "
                                + spawnedInstantProjectiles.stream().map(projectile -> projectile.position().toString()).toList());

                var longPayload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                        ResourceLocation.fromNamespaceAndPath("apprenticecodex", "compound_phial"),
                        1,
                        io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
                        io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
                );
                helper.assertTrue(
                        jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                                level, player, sourceStack, longPayload, impactPos, forward
                        ),
                        "Charged Twin Blade Staff impact manager failed to cast a LONG payload"
                );
                helper.assertTrue(!spawnedLongProjectiles.isEmpty(),
                        "Charged Twin Blade Staff LONG impact cast did not spawn Compound Phial projectiles");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(projectileListener);
            }
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerCastsInstantWhileOwnerBusy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_busy_impact_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff busy impact test could not resolve player mana data");
            magicData.setMana(500.0F);
            var triggerSpell = SpellRegistry.MYSTIC_SHIELD.get();
            magicData.getSyncedData().learnSpell(triggerSpell, false);
            magicData.initiateCast(
                    triggerSpell,
                    1,
                    triggerSpell.getEffectiveCastTime(1, player),
                    CastSource.SWORD,
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            var impactSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    impactSpell.getSpellResource(),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            impactPos,
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff impact manager should cast through the RemoteOwner busy fallback"
            );

            helper.assertTrue(magicData.isCasting(),
                    "Charged Twin Blade Staff busy fallback should not clear the original cast state");
            helper.assertTrue(magicData.getCastingSpellId().equals(triggerSpell.getSpellId()),
                    "Charged Twin Blade Staff busy fallback should preserve the original spell id");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(impactSpell),
                    "Charged Twin Blade Staff busy fallback should apply the impact spell cooldown");
            var projectiles = level.getEntitiesOfClass(
                    io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile.class,
                    new AABB(impactPos, impactPos).inflate(12.0D)
            );
            helper.assertTrue(!projectiles.isEmpty(),
                    "Charged Twin Blade Staff busy impact cast did not spawn Magic Missile projectiles");
        });
    }
    static void chargedTwinBladeStaffBusyFallbackDoesNotBypassCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_busy_cooldown_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff busy cooldown test could not resolve player mana data");
            magicData.setMana(500.0F);
            var triggerSpell = SpellRegistry.MYSTIC_SHIELD.get();
            magicData.getSyncedData().learnSpell(triggerSpell, false);
            magicData.initiateCast(
                    triggerSpell,
                    1,
                    triggerSpell.getEffectiveCastTime(1, player),
                    CastSource.SWORD,
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            var impactSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            io.redspace.ironsspellbooks.api.magic.MagicHelper.MAGIC_MANAGER.addCooldown(player, impactSpell, CastSource.SWORD);
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    impactSpell.getSpellResource(),
                    1,
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertFalse(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            impactPos,
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff busy fallback should not bypass owner cooldowns"
            );
            helper.assertTrue(magicData.isCasting(),
                    "Charged Twin Blade Staff cooldown rejection should not clear the original cast state");
            helper.assertTrue(magicData.getCastingSpellId().equals(triggerSpell.getSpellId()),
                    "Charged Twin Blade Staff cooldown rejection should preserve the original spell id");
            var projectiles = level.getEntitiesOfClass(
                    io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile.class,
                    new AABB(impactPos, impactPos).inflate(12.0D)
            );
            helper.assertTrue(projectiles.isEmpty(),
                    "Charged Twin Blade Staff cooldown rejection should not spawn Magic Missile projectiles");
        });
    }
    static void spellThrowableCardImpactCastManagerCastsInstantWhileOwnerBusy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_throwable_card_busy_impact_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Spell Throwable Card busy impact test could not resolve player mana data");
            magicData.setMana(500.0F);
            var triggerSpell = SpellRegistry.MYSTIC_SHIELD.get();
            magicData.getSyncedData().learnSpell(triggerSpell, false);
            magicData.initiateCast(
                    triggerSpell,
                    1,
                    triggerSpell.getEffectiveCastTime(1, player),
                    CastSource.SWORD,
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            var impactSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    impactSpell.getSpellResource(),
                    1,
                    CastSource.SWORD.name(),
                    AbstractSpellThrowableCardItem.CASTING_SLOT
            );
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.SPELL_INVOKE_CARD.get()),
                            payload,
                            impactPos,
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Spell Throwable Card impact manager should cast through the RemoteOwner busy fallback"
            );

            helper.assertTrue(magicData.isCasting(),
                    "Spell Throwable Card busy fallback should not clear the original cast state");
            helper.assertTrue(magicData.getCastingSpellId().equals(triggerSpell.getSpellId()),
                    "Spell Throwable Card busy fallback should preserve the original spell id");
            var projectiles = level.getEntitiesOfClass(
                    io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile.class,
                    new AABB(impactPos, impactPos).inflate(12.0D)
            );
            helper.assertTrue(!projectiles.isEmpty(),
                    "Spell Throwable Card busy impact cast did not spawn Magic Missile projectiles");
        });
    }
    static void chargedTwinBladeStaffRemoteOwnerDenylistBlocksRuntimeWithoutFallback(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_remote_denylist_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff remote denylist test could not resolve player mana data");
            magicData.setMana(200.0F);
            var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var forward = new Vec3(0.0D, 0.0D, 1.0D);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    spell.getSpellResource(),
                    1,
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            try (var ignoredConfig = ApprenticeCodexServerConfig.useRemoteOwnerCastConfigOverrideForGameTest(
                    true,
                    List.of(spell.getSpellResource().toString())
            )) {
                helper.assertFalse(
                        jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                                level, player, sourceStack, payload, impactPos, forward
                        ),
                        "Charged Twin Blade Staff should not fall back when Remote Owner Cast is denylisted"
                );
            }

            var projectiles = level.getEntitiesOfClass(
                    io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile.class,
                    new AABB(impactPos, impactPos).inflate(12.0D)
            );
            helper.assertTrue(projectiles.isEmpty(),
                    "Charged Twin Blade Staff Remote Owner denylist should prevent Magic Missile projectiles");
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerCastsPlayerSelfProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_self_profile_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff self profile test could not resolve player mana data");
            magicData.setMana(200.0F);
            var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "oakskin"),
                    1,
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level, player, sourceStack, payload, helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3))), new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff self profile failed to cast Oakskin"
            );
            helper.assertTrue(player.hasEffect(io.redspace.ironsspellbooks.registries.MobEffectRegistry.OAKSKIN.get()),
                    "Charged Twin Blade Staff self profile should apply Oakskin to the real player");
        });
    }
    static void chargedTwinBladeStaffCreativeImpactCastUsesRemoteOwnerProfileWithZeroMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_creative_remote_owner_profile_test");
            player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff creative RemoteOwner profile test could not resolve player mana data");
            magicData.setMana(0.0F);
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "magic_missile"),
                    1,
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );
            var spawnedProjectiles = new ArrayList<io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile>();
            java.util.function.Consumer<EntityJoinLevelEvent> projectileListener = event -> {
                if (event.getLevel() == level
                        && event.getEntity() instanceof io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile projectile
                        && projectile.position().distanceToSqr(impactPos) <= 144.0D) {
                    spawnedProjectiles.add(projectile);
                }
            };

            MinecraftForge.EVENT_BUS.addListener(projectileListener);
            try {
                helper.assertTrue(
                        jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                                level,
                                player,
                                new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                                payload,
                                impactPos,
                                new Vec3(0.0D, 0.0D, 1.0D)
                        ),
                        "Charged Twin Blade Staff creative impact cast should use RemoteOwner profile with zero mana"
                );
            } finally {
                MinecraftForge.EVENT_BUS.unregister(projectileListener);
            }

            helper.assertTrue(!spawnedProjectiles.isEmpty(),
                    "Charged Twin Blade Staff creative RemoteOwner profile should spawn Magic Missile projectiles");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Charged Twin Blade Staff creative RemoteOwner profile should leave mana at zero but got " + magicData.getMana());
        });
    }
    static void chargedTwinBladeStaffCreativeImpactCastUsesStaffProfileWithZeroMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_creative_staff_profile_test");
            player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff creative staff profile test could not resolve player mana data");
            magicData.setMana(0.0F);
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "oakskin"),
                    1,
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3))),
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff creative impact cast should use staff profile with zero mana"
            );
            helper.assertTrue(player.hasEffect(io.redspace.ironsspellbooks.registries.MobEffectRegistry.OAKSKIN.get()),
                    "Charged Twin Blade Staff creative staff profile should apply Oakskin to the real player");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Charged Twin Blade Staff creative staff profile should leave mana at zero but got " + magicData.getMana());
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerCastsInitialRaiseDeadProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_raise_dead_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff Raise Dead test could not resolve player mana data");
            magicData.setMana(500.0F);
            var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "raise_dead"),
                    1,
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level, player, sourceStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff Raise Dead profile failed its initial cast"
            );
            var summons = level.getEntitiesOfClass(
                    net.minecraft.world.entity.monster.Monster.class,
                    new AABB(impactPos, impactPos).inflate(12.0D),
                    monster -> monster instanceof io.redspace.ironsspellbooks.entity.mobs.IMagicSummon
            );
            helper.assertTrue(!summons.isEmpty(),
                    "Charged Twin Blade Staff Raise Dead profile should summon mobs near the impact");
            helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAISE_DEAD_SPELL.get()),
                    "Charged Twin Blade Staff Raise Dead profile should register recast on the real player");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAISE_DEAD_SPELL.get()),
                    "Charged Twin Blade Staff Raise Dead profile should not add a normal cooldown for a recast spell");
            summons.forEach(net.minecraft.world.entity.Entity::discard);
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerBlocksRaiseDeadWhenRecastExists(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_raise_dead_recast_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff Raise Dead recast test could not resolve player mana data");
            magicData.setMana(500.0F);
            var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "raise_dead"),
                    1,
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level, player, sourceStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff Raise Dead recast setup failed"
            );
            helper.assertFalse(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level, player, sourceStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff Raise Dead should not recast while an initial recast is active"
            );
            level.getEntitiesOfClass(
                    net.minecraft.world.entity.monster.Monster.class,
                    new AABB(impactPos, impactPos).inflate(12.0D),
                    monster -> monster instanceof io.redspace.ironsspellbooks.entity.mobs.IMagicSummon
            ).forEach(net.minecraft.world.entity.Entity::discard);
        });
    }
    static void chargedTwinBladeStaffRaiseDeadPreservesWheelSelectionAfterRecast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_raise_dead_selection_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff Raise Dead selection test could not resolve player mana data");
            magicData.setMana(500.0F);

            var amplifierStack = new ItemStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get());
            var mutable = ISpellContainer.create(2, true, false).mutableCopy();
            helper.assertTrue(mutable.addSpellAtIndex(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get(), 1, 0, false),
                    "Failed to prepare first wheel spell for Raise Dead selection regression");
            helper.assertTrue(mutable.addSpellAtIndex(io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAISE_DEAD_SPELL.get(), 1, 1, false),
                    "Failed to prepare Raise Dead wheel spell for selection regression");
            ISpellContainer.set(amplifierStack, mutable.toImmutable());
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            magicData.getSyncedData().setSpellSelection(new io.redspace.ironsspellbooks.gui.overlays.SpellSelection(
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND,
                    1
            ));

            var beforeSelection = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player).getSelection();
            helper.assertTrue(beforeSelection != null
                            && beforeSelection.spellData.getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAISE_DEAD_SPELL.get(),
                    "Raise Dead selection regression setup should select Raise Dead but got " + beforeSelection);

            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "raise_dead"),
                    1,
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND
            );
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            impactPos,
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff Raise Dead selection regression failed its initial cast"
            );

            var afterSelection = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player).getSelection();
            helper.assertTrue(afterSelection != null
                            && afterSelection.spellData.getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAISE_DEAD_SPELL.get(),
                    "Raise Dead impact cast should preserve the selected wheel spell but got " + afterSelection);
            var recastPayload = jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload.capture(afterSelection, player);
            helper.assertFalse(recastPayload.isPresent(),
                    "Raise Dead active recast should not fall back to a different wheel spell payload");

            level.getEntitiesOfClass(
                    net.minecraft.world.entity.monster.Monster.class,
                    new AABB(impactPos, impactPos).inflate(12.0D),
                    monster -> monster instanceof io.redspace.ironsspellbooks.entity.mobs.IMagicSummon
            ).forEach(net.minecraft.world.entity.Entity::discard);
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerRejectsUnprofiledSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_unprofiled_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff unprofiled test could not resolve player mana data");
            magicData.setMana(500.0F);
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "ray_of_siphoning"),
                    1,
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertFalse(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level,
                            player,
                            new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                            payload,
                            helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3))),
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff should reject spells without a RemoteOwner impact profile"
            );
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerStartsContinuousSpells(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_impact_continuous_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Charged Twin Blade Staff continuous impact test could not resolve player mana data");
        magicData.setMana(200.0F);
        var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
        var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "fire_breath"),
                1,
                io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
                io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
        );

        helper.runAtTickTime(1, () -> helper.assertTrue(
                jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                        level, player, sourceStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
                ),
                "Charged Twin Blade Staff impact manager failed to start a CONTINUOUS payload"
        ));
        helper.succeedWhen(() -> {
            var projectiles = level.getEntitiesOfClass(FireBreathProjectile.class, new AABB(impactPos, impactPos).inflate(16.0D));
            helper.assertTrue(!projectiles.isEmpty(),
                    "Charged Twin Blade Staff CONTINUOUS impact cast did not spawn Fire Breath projectiles");
            var anchorOwner = projectiles.stream()
                    .map(FireBreathProjectile::getOwner)
                    .filter(RemoteOwnerCastAnchorEntity.class::isInstance)
                    .map(RemoteOwnerCastAnchorEntity.class::cast)
                    .findFirst();
            helper.assertTrue(anchorOwner.isPresent(),
                    "Charged Twin Blade Staff CONTINUOUS impact cast should use a Remote Owner anchor for Fire Breath owner tracking");
            helper.assertTrue(anchorOwner.get().getDisplayName().getString().equals(player.getDisplayName().getString()),
                    "Remote Owner anchor should expose the player name for death messages");
        });
    }
    static void chargedTwinBladeStaffContinuousRemoteOwnerIgnoresMissingDispenserProfile(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_remote_continuous_profile_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Charged Twin Blade Staff RemoteOwner-only continuous test could not resolve player mana data");
        magicData.setMana(200.0F);
        var sourceStack = new ItemStack(ItemRegistry.SPELL_INVOKE_CARD.get());
        var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
        var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                spell.getSpellResource(),
                1,
                CastSource.SWORD.name(),
                io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
        );

        helper.runAtTickTime(1, () -> {
            try (var ignoredRemoteProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(
                    requireSpellId(spell),
                    remotePlayerGeometryProfile(false)
            ))) {
                helper.assertTrue(
                        jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                                level, player, sourceStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
                        ),
                        "Charged Twin Blade Staff should start RemoteOwner CONTINUOUS casts without a Spell Dispenser profile"
                );
            }
        });
        helper.succeedWhen(() -> {
            var projectiles = level.getEntitiesOfClass(FireBreathProjectile.class, new AABB(impactPos, impactPos).inflate(16.0D));
            helper.assertTrue(!projectiles.isEmpty(),
                    "RemoteOwner-only CONTINUOUS impact cast did not spawn Fire Breath projectiles");
            var anchorOwner = projectiles.stream()
                    .map(FireBreathProjectile::getOwner)
                    .filter(RemoteOwnerCastAnchorEntity.class::isInstance)
                    .map(RemoteOwnerCastAnchorEntity.class::cast)
                    .findFirst();
            helper.assertTrue(anchorOwner.isPresent(),
                    "RemoteOwner-only CONTINUOUS impact cast should keep Fire Breath owned by a Remote Owner anchor");
        });
    }
    static void chargedTwinBladeStaffContinuousThrowableCardUsesWeaponImbueCooldown(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_card_continuous_cooldown_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Charged Twin Blade Staff card continuous cooldown test could not resolve player mana data");
        magicData.setMana(500.0F);
        var cardStack = new ItemStack(ItemRegistry.SPELL_INVOKE_CARD.get());
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
        var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
        var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                spell.getSpellResource(),
                1,
                CastSource.SWORD.name(),
                AbstractSpellThrowableCardItem.CASTING_SLOT
        );

        var weaponImbueCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                spell,
                player,
                CastSource.SWORD
        );
        var spellbookCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                spell,
                player,
                CastSource.SPELLBOOK
        );
        helper.assertTrue(spellbookCooldown > weaponImbueCooldown,
                "Throwable Card cooldown regression needs a visible Weapon Imbue reduction: "
                        + spellbookCooldown + " / weapon " + weaponImbueCooldown);

        helper.runAtTickTime(1, () -> helper.assertTrue(
                jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                        level, player, cardStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
                ),
                "Charged Twin Blade Staff impact manager failed to start a Throwable Card CONTINUOUS payload"
        ));

        helper.succeedWhen(() -> {
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            helper.assertTrue(cooldown != null,
                    "Throwable Card CONTINUOUS impact cast has not finished its cooldown yet");
            var remainingCooldown = cooldown.getCooldownRemaining();
            helper.assertTrue(remainingCooldown <= weaponImbueCooldown,
                    "Throwable Card CONTINUOUS cooldown did not use the Weapon Imbue reduction: "
                            + remainingCooldown + " / weapon " + weaponImbueCooldown);
            helper.assertTrue(remainingCooldown < spellbookCooldown,
                    "Throwable Card CONTINUOUS cooldown matched the non-Weapon-Imbue spellbook cooldown: "
                            + remainingCooldown + " / spellbook " + spellbookCooldown);
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerSkipsWhenOwnerCannotCast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_impact_fail_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff impact fail test could not resolve player mana data");
            magicData.setMana(0.0F);
            var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "magic_missile"),
                    1,
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );

            helper.assertFalse(
                    jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                            level, player, sourceStack, payload, impactPos, new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff impact manager should skip casts when the owner cannot pay the spell mana"
            );
        });
    }
}
