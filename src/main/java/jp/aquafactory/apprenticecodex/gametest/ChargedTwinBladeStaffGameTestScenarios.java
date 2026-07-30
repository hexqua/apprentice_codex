package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.entity.spells.fire_breath.FireBreathProjectile;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastAnchorEntity;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileManager;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.item.spellthrowablecard.AbstractSpellThrowableCardItem;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhialProjectileEntity;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                    stack.getItem().getDefaultAttributeModifiers(stack)
            );
            NeoForge.EVENT_BUS.post(event);
            var modifiers = toModifierMultimap(event.build());

            assertSingleModifierAmount(
                    helper,
                    modifiers.get(Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                    0.05D,
                    "Charged Twin Blade Staff melee damage upgrade should be a single display modifier"
                            + " upgradeData=" + upgradeData
                            + " modifiers=" + describeModifiers(modifiers)
            );
        });
    }
    static void chargedTwinBladeStaffKeepsExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedChargedTwinBladeStaffEnchantments(helper.getLevel().registryAccess(), stack),
                    "Charged Twin Blade Staff"
            );
        });
    }
    static void chargedTwinBladeStaffExposesExpectedMainhandAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var modifiers = toModifierMultimap(stack.getAttributeModifiers());
            var componentModifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
            var componentModifierMap = toModifierMultimap(componentModifiers);

            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.ADD_VALUE
            ) - 10.0D) < 1.0e-9D, "Charged Twin Blade Staff attack damage regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED),
                    AttributeModifier.Operation.ADD_VALUE
            ) - (-3.0D)) < 1.0e-9D, "Charged Twin Blade Staff attack speed regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    modifiers.get((Holder<Attribute>) io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ) - 0.10D) < 1.0e-9D, "Charged Twin Blade Staff spell power regression: " + describeModifiers(modifiers));
            helper.assertTrue(Math.abs(sumModifierAmount(
                    componentModifierMap.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED),
                    AttributeModifier.Operation.ADD_VALUE
            ) - (-3.0D)) < 1.0e-9D, "Charged Twin Blade Staff attack speed component regression: "
                    + describeModifiers(componentModifierMap));
        });
    }
    static void chargedTwinBladeStaffResolveThrownDamageIncludesApplicableEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var baseStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var level = helper.getLevel();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_damage_test");
            var genericTarget = new ArmorStand(level, 0.0D, 2.0D, 0.0D);
            var undeadTarget = EntityType.ZOMBIE.create(level);
            var arthropodTarget = EntityType.SPIDER.create(level);
            var aquaticTarget = EntityType.DROWNED.create(level);
            helper.assertTrue(undeadTarget != null, "Charged Twin Blade Staff damage test could not create undead target");
            helper.assertTrue(arthropodTarget != null, "Charged Twin Blade Staff damage test could not create arthropod target");
            helper.assertTrue(aquaticTarget != null, "Charged Twin Blade Staff damage test could not create aquatic target");

            var damageSource = level.damageSources().playerAttack(player);
            var baseDamage = ChargedTwinBladeStaff.resolveThrownDamage(baseStack);
            helper.assertTrue(Math.abs(baseDamage - 11.0D) < 1.0e-9D,
                    "Charged Twin Blade Staff base thrown damage regression: " + baseDamage);

            var sharpnessStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            sharpnessStack.enchant(enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), 3);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    sharpnessStack,
                    genericTarget,
                    damageSource,
                    "Charged Twin Blade Staff sharpness thrown damage regression"
            );

            var smiteStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            smiteStack.enchant(enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SMITE), 2);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    smiteStack,
                    undeadTarget,
                    damageSource,
                    "Charged Twin Blade Staff smite thrown damage regression"
            );
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    smiteStack,
                    genericTarget,
                    damageSource,
                    "Charged Twin Blade Staff smite fallback damage regression"
            );

            var baneStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            baneStack.enchant(enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.BANE_OF_ARTHROPODS), 2);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    baneStack,
                    arthropodTarget,
                    damageSource,
                    "Charged Twin Blade Staff bane thrown damage regression"
            );
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    baneStack,
                    genericTarget,
                    damageSource,
                    "Charged Twin Blade Staff bane fallback damage regression"
            );

            var impalingStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            impalingStack.enchant(enchantmentLookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.IMPALING), 2);
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    impalingStack,
                    aquaticTarget,
                    damageSource,
                    "Charged Twin Blade Staff impaling thrown damage regression"
            );
            assertChargedTwinBladeStaffThrownDamage(
                    helper,
                    impalingStack,
                    genericTarget,
                    damageSource,
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
                stack.getUseDuration(player) - jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS
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
        var loyalty = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOYALTY);
        stack.enchant(loyalty, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Charged Twin Blade Staff loyalty mana test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.runAtTickTime(1, () -> stack.getItem().releaseUsing(
                stack,
                helper.getLevel(),
                player,
                stack.getUseDuration(player) - jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS
        ));
        helper.succeedWhen(() -> helper.assertTrue(Math.abs(magicData.getMana() - (100.0F - 100.0F / 3.0F)) < 1.0e-3F,
                "Charged Twin Blade Staff loyalty mana discount regressed: " + magicData.getMana()));
    }
    static void chargedTwinBladeStaffRiptideWorksOnDryGroundWithoutProjectile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_riptide_test");
            var stack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var riptide = helper.getLevel().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.RIPTIDE);
            stack.enchant(riptide, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff riptide test could not resolve player mana data");
            magicData.setMana(50.0F);

            stack.getItem().releaseUsing(
                    stack,
                    helper.getLevel(),
                    player,
                    stack.getUseDuration(player) - jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff.THROW_THRESHOLD_TICKS
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
            var level = (ServerLevel) helper.getLevel();
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
                    CastSource.SWORD.name(),
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND
            );
            NeoForge.EVENT_BUS.addListener(projectileListener);
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
                        CastSource.SWORD.name(),
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
                NeoForge.EVENT_BUS.unregister(projectileListener);
            }
        });
    }

    static void chargedTwinBladeStaffImpactCastManagerCastsInstantWhileOwnerBusy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
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
            var level = (ServerLevel) helper.getLevel();
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
            var level = (ServerLevel) helper.getLevel();
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
            var level = (ServerLevel) helper.getLevel();
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
                    CastSource.SWORD.name(),
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
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_self_profile_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff self profile test could not resolve player mana data");
            magicData.setMana(200.0F);
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "oakskin"),
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
                            helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3))),
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff self profile failed to cast Oakskin"
            );
            helper.assertTrue(player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(io.redspace.ironsspellbooks.registries.MobEffectRegistry.OAKSKIN.get())),
                    "Charged Twin Blade Staff self profile should apply Oakskin to the real player");
        });
    }
    static void chargedTwinBladeStaffCreativeImpactCastUsesRemoteOwnerProfileWithZeroMana(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_creative_remote_owner_profile_test");
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Charged Twin Blade Staff creative RemoteOwner profile test could not resolve player mana data");
        var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
        var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "magic_missile"),
                1,
                CastSource.SWORD.name(),
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

        helper.runAtTickTime(1, () -> {
            NeoForge.EVENT_BUS.addListener(projectileListener);
            magicData.setMana(0.0F);
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
                helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                        "Charged Twin Blade Staff creative RemoteOwner profile should leave mana at zero but got " + magicData.getMana());
            } finally {
                NeoForge.EVENT_BUS.unregister(projectileListener);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(!spawnedProjectiles.isEmpty(),
                    "Charged Twin Blade Staff creative RemoteOwner profile should spawn Magic Missile projectiles");
        });
    }
    static void chargedTwinBladeStaffCreativeImpactCastUsesStaffProfileWithZeroMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_creative_staff_profile_test");
            player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff creative staff profile test could not resolve player mana data");
            magicData.setMana(0.0F);
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "oakskin"),
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
                            helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3))),
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff creative impact cast should use staff profile with zero mana"
            );
            helper.assertTrue(player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(io.redspace.ironsspellbooks.registries.MobEffectRegistry.OAKSKIN.get())),
                    "Charged Twin Blade Staff creative staff profile should apply Oakskin to the real player");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Charged Twin Blade Staff creative staff profile should leave mana at zero but got " + magicData.getMana());
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerCastsInitialRaiseDeadProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_raise_dead_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff Raise Dead test could not resolve player mana data");
            magicData.setMana(500.0F);
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "raise_dead"),
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
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_raise_dead_recast_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff Raise Dead recast test could not resolve player mana data");
            magicData.setMana(500.0F);
            var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "raise_dead"),
                    1,
                    CastSource.SWORD.name(),
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
            var level = (ServerLevel) helper.getLevel();
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
                    CastSource.SWORD.name(),
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
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_unprofiled_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff unprofiled test could not resolve player mana data");
            magicData.setMana(500.0F);
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "ray_of_siphoning"),
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
                            helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3))),
                            new Vec3(0.0D, 0.0D, 1.0D)
                    ),
                    "Charged Twin Blade Staff should reject spells without a RemoteOwner impact profile"
            );
        });
    }
    static void chargedTwinBladeStaffImpactCastManagerStartsContinuousSpells(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_impact_continuous_test");
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Charged Twin Blade Staff continuous impact test could not resolve player mana data");
        magicData.setMana(200.0F);
        var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
        var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
        var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "fire_breath"),
                1,
                CastSource.SWORD.name(),
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
        var level = (ServerLevel) helper.getLevel();
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
        var level = (ServerLevel) helper.getLevel();
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
            var level = (ServerLevel) helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "charged_twin_blade_staff_impact_fail_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Charged Twin Blade Staff impact fail test could not resolve player mana data");
            magicData.setMana(0.0F);
            var sourceStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
            var impactPos = helper.absoluteVec(Vec3.atCenterOf(new BlockPos(0, 2, 3)));
            var payload = new jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "magic_missile"),
                    1,
                    CastSource.SWORD.name(),
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
