package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.util.Utils;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastType;
import jp.aquafactory.apprenticecodex.item.SwingTriggeredMagicItem;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaffAttackContextManager;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.HighTierSwingcastStaffConfigState;
import jp.aquafactory.apprenticecodex.item.swingstaff.IronSwingcastStaffConfigState;
import jp.aquafactory.apprenticecodex.item.swingstaff.IronSwingcastStaffCrystallizeEvent;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastCooldownMode;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastStaffCastContext;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.manaslash.ManaSlash;
import jp.aquafactory.apprenticecodex.spell.manaslash.ManaSlashProjectileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import java.util.ArrayList;

final class SwingcastStaffGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private SwingcastStaffGameTestScenarios() {
    }

    static void copperSwingcastStaffStartsWithBallLightningLevelOne(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingcastStaffItem) ItemRegistry.COPPER_SWINGCAST_STAFF.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);

            helper.assertTrue(ISpellContainer.isSpellContainer(stack), "Copper Swingcast Staff did not initialize a spell container");

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Copper Swingcast Staff spell container is null");

            var spellData = spellContainer.getSpellAtIndex(0);
            helper.assertTrue(spellData != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY,
                    "Copper Swingcast Staff has no preset spell");
            helper.assertTrue(spellData.getSpell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(),
                    "Copper Swingcast Staff preset spell mismatch: " + spellData.getSpell().getSpellResource());
            helper.assertTrue(spellData.getLevel() == 1,
                    "Copper Swingcast Staff preset spell level mismatch: " + spellData.getLevel());
        });
    }
    static void crystalBladedStaffStartsWithHiddenManaSlash(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
            var stack = new ItemStack(item);
            item.initializeSpellContainer(stack);

            helper.assertTrue(item instanceof SwingTriggeredMagicItem,
                    "Crystal Bladed Staff should trigger imbued spells on swing");
            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Crystal Bladed Staff spell container is null");
            helper.assertTrue(!spellContainer.isSpellWheel(),
                    "Crystal Bladed Staff preset spell should stay hidden from the spell wheel");
            assertSpellData(helper, spellContainer, 0, SpellRegistry.MANA_SLASH.get(), 1, true,
                    "Crystal Bladed Staff should start with locked Mana Slash");
        });
    }
    static void crystalBladedStaffMissSwingCastsManaSlash(GameTestHelper helper) {
        var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
        var stack = new ItemStack(item);
        item.initializeSpellContainer(stack);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 4, 0), "crystal_bladed_staff_miss_cast");
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.setYHeadRot(0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Crystal Bladed Staff miss cast test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(player, InteractionHand.MAIN_HAND, true),
                "Crystal Bladed Staff miss trigger should be accepted"
        );

        helper.succeedWhen(() -> {
            helper.assertTrue(SpellRegistry.MANA_SLASH.get().getSpellId().equals(magicData.getCastingSpellId()),
                    "Crystal Bladed Staff miss swing should cast Mana Slash but got " + magicData.getCastingSpellId());
            helper.assertTrue(ItemStack.isSameItemSameTags(magicData.getPlayerCastingItem(), stack),
                    "Crystal Bladed Staff miss swing should cast with the staff stack");
            helper.assertTrue(io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND.equals(magicData.getCastingEquipmentSlot()),
                    "Crystal Bladed Staff miss swing should mark the mainhand casting slot");
            helper.succeed();
        });
    }
    static void crystalBladedStaffHitSwingDoesNotCastManaSlash(GameTestHelper helper) {
        var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
        var stack = new ItemStack(item);
        item.initializeSpellContainer(stack);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "crystal_bladed_staff_hit_no_cast");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Crystal Bladed Staff hit suppression test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(player, InteractionHand.MAIN_HAND, true),
                "Crystal Bladed Staff hit suppression trigger should be accepted"
        );
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        helper.assertTrue(target.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F),
                "Crystal Bladed Staff hit suppression test should deal direct player attack damage");

        helper.runAfterDelay(3, () -> {
            helper.assertTrue(!SpellRegistry.MANA_SLASH.get().getSpellId().equals(magicData.getCastingSpellId()),
                    "Crystal Bladed Staff hit swing should not cast Mana Slash");
            helper.succeed();
        });
    }
    static void crystalBladedStaffVanillaAttackEntityHitDoesNotCastManaSlash(GameTestHelper helper) {
        var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
        var stack = new ItemStack(item);
        item.initializeSpellContainer(stack);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "crystal_bladed_staff_vanilla_hit_no_cast");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Crystal Bladed Staff vanilla hit test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(player, InteractionHand.MAIN_HAND, true, 2),
                "Crystal Bladed Staff vanilla hit trigger should be accepted"
        );
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player, target));

        helper.runAfterDelay(4, () -> {
            helper.assertTrue(!SpellRegistry.MANA_SLASH.get().getSpellId().equals(magicData.getCastingSpellId()),
                    "Crystal Bladed Staff vanilla hit should not cast Mana Slash");
            helper.succeed();
        });
    }
    static void crystalBladedStaffDelayedHitDoesNotCastManaSlash(GameTestHelper helper) {
        var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
        var stack = new ItemStack(item);
        item.initializeSpellContainer(stack);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "crystal_bladed_staff_delayed_hit_no_cast");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Crystal Bladed Staff delayed hit test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(player, InteractionHand.MAIN_HAND, true, 2),
                "Crystal Bladed Staff delayed hit trigger should be accepted"
        );
        helper.runAfterDelay(1, () -> CrystalBladedStaffAttackContextManager.recordRecentCrystalBladedStaffHit(
                player,
                InteractionHand.MAIN_HAND
        ));

        helper.runAfterDelay(4, () -> {
            helper.assertTrue(!SpellRegistry.MANA_SLASH.get().getSpellId().equals(magicData.getCastingSpellId()),
                    "Crystal Bladed Staff delayed hit should not cast Mana Slash");
            helper.succeed();
        });
    }

    static void crystalBladedStaffMissTriggerDoesNotUseSwappedStack(GameTestHelper helper) {
        var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
        var originalStack = new ItemStack(item);
        item.initializeSpellContainer(originalStack);
        var swappedSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var swappedStack = createLegacyCrystalBladedStaffContainer(swappedSpell, 1, false);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 4, 0), "crystal_bladed_staff_swap_no_cast");
        player.setItemInHand(InteractionHand.MAIN_HAND, originalStack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Crystal Bladed Staff swapped stack test could not resolve player mana data");
        magicData.setMana(1000.0F);

        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(player, InteractionHand.MAIN_HAND, true, 2),
                "Crystal Bladed Staff swapped stack miss trigger should be accepted"
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, swappedStack);

        helper.runAfterDelay(4, () -> {
            helper.assertTrue(!SpellRegistry.MANA_SLASH.get().getSpellId().equals(magicData.getCastingSpellId()),
                    "Crystal Bladed Staff swapped stack should not cast the original Mana Slash");
            helper.assertTrue(!swappedSpell.getSpellId().equals(magicData.getCastingSpellId()),
                    "Crystal Bladed Staff swapped stack should not cast the replacement staff spell");
            helper.succeed();
        });
    }

    static void crystalBladedStaffPendingMissTriggerKeepsEarlierHand(GameTestHelper helper) {
        var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
        var mainSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var mainStack = createLegacyCrystalBladedStaffContainer(mainSpell, 1, false);
        var offhandStack = new ItemStack(item);
        item.initializeSpellContainer(offhandStack);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 4, 0), "crystal_bladed_staff_dual_pending");
        player.setItemInHand(InteractionHand.MAIN_HAND, mainStack);
        player.setItemInHand(InteractionHand.OFF_HAND, offhandStack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Crystal Bladed Staff dual pending test could not resolve player mana data");
        magicData.setMana(1000.0F);

        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(player, InteractionHand.MAIN_HAND, true, 1),
                "Crystal Bladed Staff mainhand miss trigger should be accepted"
        );
        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(player, InteractionHand.OFF_HAND, true, 10),
                "Crystal Bladed Staff offhand miss trigger should be accepted without overwriting mainhand"
        );

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(mainSpell.getSpellId().equals(magicData.getCastingSpellId()),
                    "Crystal Bladed Staff mainhand pending trigger should cast first but got "
                            + magicData.getCastingSpellId());
            helper.assertTrue(io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND.equals(
                            magicData.getCastingEquipmentSlot()),
                    "Crystal Bladed Staff mainhand pending trigger should mark the mainhand casting slot");
            helper.succeed();
        });
    }

    static void crystalBladedStaffMainHandHitDoesNotSuppressOffhandMiss(GameTestHelper helper) {
        var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
        var offhandStack = new ItemStack(item);
        item.initializeSpellContainer(offhandStack);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "crystal_bladed_staff_offhand_miss");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        player.setItemInHand(InteractionHand.OFF_HAND, offhandStack);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Crystal Bladed Staff offhand miss test could not resolve player mana data");
        magicData.setMana(100.0F);

        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(player, InteractionHand.OFF_HAND, true, 2),
                "Crystal Bladed Staff offhand miss trigger should be accepted"
        );
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        helper.assertTrue(target.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F),
                "Crystal Bladed Staff offhand miss test should deal mainhand player attack damage");

        helper.succeedWhen(() -> {
            helper.assertTrue(SpellRegistry.MANA_SLASH.get().getSpellId().equals(magicData.getCastingSpellId()),
                    "Mainhand hit should not suppress offhand Crystal Bladed Staff miss cast but got "
                            + magicData.getCastingSpellId());
            helper.assertTrue(io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND.equals(
                            magicData.getCastingEquipmentSlot()),
                    "Offhand Crystal Bladed Staff miss cast should mark the offhand casting slot");
            helper.succeed();
        });
    }
    static void crystalBladedStaffLegacyWheelPresetIsHiddenWhenHeld(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
            var stack = createLegacyCrystalBladedStaffContainer(SpellRegistry.MANA_SLASH.get(), 1, true);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "crystal_bladed_staff_legacy_preset");

            item.inventoryTick(stack, helper.getLevel(), player, 0, true);

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Crystal Bladed Staff rescued preset container is null");
            helper.assertTrue(!spellContainer.isSpellWheel(),
                    "Crystal Bladed Staff legacy preset should be removed from the spell wheel");
            assertSpellData(helper, spellContainer, 0, SpellRegistry.MANA_SLASH.get(), 1, true,
                    "Crystal Bladed Staff legacy preset should stay locked after rescue");
        });
    }
    static void crystalBladedStaffLegacyWheelPresetIsHiddenWhenHeldInOffhand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
            var stack = createLegacyCrystalBladedStaffContainer(SpellRegistry.MANA_SLASH.get(), 1, true);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "crystal_bladed_staff_legacy_offhand");

            player.setItemInHand(InteractionHand.OFF_HAND, stack);
            item.inventoryTick(stack, helper.getLevel(), player, 40, false);

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Offhand Crystal Bladed Staff rescued preset container is null");
            helper.assertTrue(!spellContainer.isSpellWheel(),
                    "Offhand Crystal Bladed Staff legacy preset should be removed from the spell wheel");
            assertSpellData(helper, spellContainer, 0, SpellRegistry.MANA_SLASH.get(), 1, true,
                    "Offhand Crystal Bladed Staff legacy preset should stay locked after rescue");
        });
    }
    static void crystalBladedStaffLegacyWheelReplacementStaysRemovableWhenHeld(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var stack = createLegacyCrystalBladedStaffContainer(replacementSpell, 1, false);
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "crystal_bladed_staff_legacy_replacement");

            item.inventoryTick(stack, helper.getLevel(), player, 0, true);

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Crystal Bladed Staff rescued replacement container is null");
            helper.assertTrue(!spellContainer.isSpellWheel(),
                    "Crystal Bladed Staff legacy replacement should be removed from the spell wheel");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Crystal Bladed Staff legacy replacement should stay removable after rescue");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Crystal Bladed Staff legacy replacement should remain extractable after rescue");
        });
    }
    static void manaSlashOffhandSwingUsesOffhandCatalystAttackDamage(GameTestHelper helper) {
        var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
        var stack = new ItemStack(item);
        item.initializeSpellContainer(stack);
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_slash_offhand_damage");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        player.setItemInHand(InteractionHand.OFF_HAND, stack);

        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Mana Slash offhand damage test could not resolve player mana data");
        magicData.setMana(100.0F);

        try (var ignored = SwingcastStaffCastContext.open(player.getUUID(), stack, SpellRegistry.MANA_SLASH.get())) {
            SpellRegistry.MANA_SLASH.get().onCast(
                    helper.getLevel(),
                    1,
                    player,
                    CastSource.SWORD,
                    magicData
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to close Mana Slash swingcast context.", e);
        }

        helper.succeedWhen(() -> {
            var projectiles = helper.getLevel().getEntitiesOfClass(
                    ManaSlashProjectileEntity.class,
                    new AABB(player.blockPosition()).inflate(8.0D)
            );
            helper.assertTrue(projectiles.size() == 1,
                    "Mana Slash offhand damage test should spawn exactly one projectile but got " + projectiles.size());

            var damageTag = new CompoundTag();
            projectiles.get(0).saveWithoutId(damageTag);
            var actualDamage = damageTag.getFloat("Damage");
            var expectedDamage = Math.max(
                    1.0F,
                    ManaSlash.resolveCatalystWeaponDamage(player, stack, MobType.UNDEFINED)
                            * SpellRegistry.MANA_SLASH.get().getSpellPower(1, player) / 100.0F
            ) * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.MANA_SLASH);
            helper.assertTrue(Math.abs(actualDamage - expectedDamage) < 1.0e-4F,
                    "Mana Slash offhand damage should use offhand catalyst attack damage: expected "
                            + expectedDamage + " but got " + actualDamage);
        });
    }
    static void manaSlashCatalystDamageUsesStackAttributeModifiers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_slash_stack_attribute");
            var catalystStack = new ItemStack(Items.STICK);
            catalystStack.addAttributeModifier(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            UUID.fromString("1af17cb4-75be-44b8-bd30-9be5760c66d9"),
                            "Mana Slash GameTest attack damage",
                            20.0D,
                            AttributeModifier.Operation.ADDITION
                    ),
                    EquipmentSlot.MAINHAND
            );

            var resolvedDamage = ManaSlash.resolveCatalystWeaponDamage(player, catalystStack, MobType.UNDEFINED);
            helper.assertTrue(Math.abs(resolvedDamage - 21.0F) < 1.0e-4F,
                    "Mana Slash catalyst damage should include stack AttributeModifiers NBT: " + resolvedDamage);
        });
    }

    static void manaSlashCatalystDamageAppliesAttributeEventOnce(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_slash_attribute_event");
            var catalystStack = new ItemStack(Items.STICK);
            catalystStack.addAttributeModifier(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            UUID.fromString("9ecb8ab9-6cf7-46e1-befa-851e53146876"),
                            "Mana Slash GameTest NBT attack damage",
                            20.0D,
                            AttributeModifier.Operation.ADDITION
                    ),
                    EquipmentSlot.MAINHAND
            );

            var eventCalls = new AtomicInteger();
            Consumer<ItemAttributeModifierEvent> listener = event -> {
                if (event.getItemStack() == catalystStack && event.getSlotType() == EquipmentSlot.MAINHAND) {
                    eventCalls.incrementAndGet();
                    event.addModifier(
                            Attributes.ATTACK_DAMAGE,
                            new AttributeModifier(
                                    UUID.fromString("98624507-b4c7-4188-9b1c-b058c8168a6a"),
                                    "Mana Slash GameTest event attack damage",
                                    5.0D,
                                    AttributeModifier.Operation.ADDITION
                            )
                    );
                }
            };

            MinecraftForge.EVENT_BUS.addListener(listener);
            try {
                var resolvedDamage = ManaSlash.resolveCatalystWeaponDamage(player, catalystStack, MobType.UNDEFINED);
                helper.assertTrue(eventCalls.get() == 1,
                        "Mana Slash catalyst damage should fire ItemAttributeModifierEvent once but got " + eventCalls.get());
                helper.assertTrue(Math.abs(resolvedDamage - 26.0F) < 1.0e-4F,
                        "Mana Slash catalyst damage should include NBT and one event modifier: " + resolvedDamage);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(listener);
            }
        });
    }

    static void manaSlashDamageMultiplierAppliesAfterMinimumDamage(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_slash_low_multiplier");
        var catalystStack = new ItemStack(Items.STICK);
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Mana Slash low multiplier test could not resolve player mana data");
        magicData.setMana(100.0F);

        try (var configOverride = ApprenticeCodexServerConfig.useDamageMultiplierOverrideForGameTest(
                DamageMultiplierKey.MANA_SLASH,
                0.5D
        );
             var ignored = SwingcastStaffCastContext.open(player.getUUID(), catalystStack, SpellRegistry.MANA_SLASH.get())) {
            SpellRegistry.MANA_SLASH.get().onCast(
                    helper.getLevel(),
                    1,
                    player,
                    CastSource.SWORD,
                    magicData
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to close Mana Slash low multiplier test context.", e);
        }

        helper.succeedWhen(() -> {
            var projectiles = helper.getLevel().getEntitiesOfClass(
                    ManaSlashProjectileEntity.class,
                    new AABB(player.blockPosition()).inflate(8.0D)
            );
            helper.assertTrue(projectiles.size() == 1,
                    "Mana Slash low multiplier test should spawn exactly one projectile but got " + projectiles.size());

            var damageTag = new CompoundTag();
            projectiles.get(0).saveWithoutId(damageTag);
            var actualDamage = damageTag.getFloat("Damage");
            helper.assertTrue(Math.abs(actualDamage - 0.5F) < 1.0e-4F,
                    "Mana Slash low damage multiplier should apply after minimum damage: expected 0.5 but got "
                            + actualDamage);
        });
    }
    static void manaSlashAllowsNonSwingcastPrecondition(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_slash_non_swingcast");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Mana Slash non-swingcast test could not resolve player mana data");
            magicData.setMana(100.0F);

            var castResult = SpellRegistry.MANA_SLASH.get().canBeCastedBy(1, CastSource.SWORD, magicData, player);
            helper.assertTrue(castResult.isSuccess(),
                    "Mana Slash should keep non-swingcast cast paths available");
        });
    }
    static void manaSlashRequiresSwingcastCatalystWhenContextIsActive(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_slash_missing_catalyst");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Mana Slash missing catalyst test could not resolve player mana data");
            magicData.setMana(100.0F);

            CastResult castResult;
            try (var ignored = SwingcastStaffCastContext.open(player.getUUID(), ItemStack.EMPTY, SpellRegistry.MANA_SLASH.get())) {
                castResult = SpellRegistry.MANA_SLASH.get().canBeCastedBy(1, CastSource.SWORD, magicData, player);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to close Mana Slash swingcast context.", e);
            }

            helper.assertTrue(castResult.type == CastResult.Type.FAILURE,
                    "Mana Slash should fail before swingcast when swingcast catalyst is missing");
            helper.assertTrue(castResult.message != null,
                    "Mana Slash missing catalyst failure should provide a message");
            var contents = castResult.message.getContents();
            helper.assertTrue(contents instanceof TranslatableContents,
                    "Mana Slash missing catalyst message should be translatable");
            var translatableContents = (TranslatableContents) contents;
            helper.assertTrue("ui.apprenticecodex.mana_slash.missing_catalyst".equals(translatableContents.getKey()),
                    "Mana Slash missing catalyst message should use the dedicated translation key");
        });
    }
    static void copperSwingcastStaffReplacementSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingcastStaffItem) ItemRegistry.COPPER_SWINGCAST_STAFF.get();
            var stack = createInitializedPresetStack(item);
            var initialContainer = ISpellContainer.get(stack);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();

            helper.assertTrue(initialContainer != null, "Copper Swingcast Staff spell container is null");
            assertSpellData(helper, initialContainer, 0, io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get(), 1, true,
                    "Copper Swingcast Staff preset spell should remain locked");

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var normalizedContainer = ISpellContainer.get(stack);
            helper.assertTrue(normalizedContainer != null, "Copper Swingcast Staff normalized spell container is null");
            assertSpellData(helper, normalizedContainer, 0, replacementSpell, 1, false,
                    "Copper Swingcast Staff replacement spell should be removable");
            helper.assertTrue(normalizedContainer.getSpellAtIndex(0).canRemove(),
                    "Copper Swingcast Staff replacement spell should remain extractable in Spellcaster Workbench");
        });
    }
    static void ironSwingcastStaffImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingMagicItem) ItemRegistry.IRON_SWINGCAST_STAFF.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var restored = roundTripItemStack(stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Iron Swingcast Staff save/load spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Iron Swingcast Staff imbued spell should remain removable after save/load");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Iron Swingcast Staff imbued spell should remain extractable after save/load");
        });
    }
    static void copperSwingcastStaffPresetEquivalentSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingMagicItem) ItemRegistry.COPPER_SWINGCAST_STAFF.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var restored = roundTripItemStack(stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Copper Swingcast Staff preset-equivalent save/load spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Copper Swingcast Staff preset-equivalent imbued spell should remain removable after save/load");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Copper Swingcast Staff preset-equivalent imbued spell should remain extractable after save/load");
        });
    }
    static void ironSwingcastStaffExtractedSpellStaysClearedAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingMagicItem) ItemRegistry.IRON_SWINGCAST_STAFF.get();
            var stack = createInitializedPresetStack(item);

            applyPresetSpellExtraction(helper, stack);

            var restored = roundTripItemStack(stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            assertClearedSpellContainer(helper, restored, "Iron Swingcast Staff should stay cleared after save/load");
        });
    }
    static void ironSwingcastStaffLegacyLockedReplacementIsRecoveredAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSwingMagicItem) ItemRegistry.IRON_SWINGCAST_STAFF.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyLegacyLockedReplacement(helper, stack, replacementSpell, 1);

            var restored = roundTripItemStack(stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Iron Swingcast Staff recovered spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Iron Swingcast Staff legacy locked replacement should be recovered after save/load");
        });
    }
    static void ironSwingcastStaffCrystallizesOnlyForEnabledMainhandMobKillCredit(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(
                    helper,
                    new BlockPos(0, 2, 0),
                    "iron_swingcast_staff_crystallize_test"
            );
            var targetPos = new BlockPos(2, 2, 0);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.IRON_SWINGCAST_STAFF.get()));

            try (var ignored = ApprenticeCodexServerConfig.useIronSwingcastStaffConfigOverrideForGameTest(1.0D)) {
                var zombie = helper.spawn(EntityType.ZOMBIE, targetPos);
                zombie.setLastHurtByPlayer(player);
                IronSwingcastStaffCrystallizeEvent.onLivingDeath(new LivingDeathEvent(
                        zombie,
                        helper.getLevel().damageSources().generic()
                ));
                helper.assertTrue(
                        countFreshItemDrops(
                                helper.getLevel(),
                                ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get(),
                                helper.absolutePos(targetPos),
                                2.0D
                        ) == 1,
                        "Iron Swingcast Staff should add exactly one Crystalline Arcane Shard"
                );
            }

            var disabledPos = new BlockPos(5, 2, 0);
            try (var ignored = ApprenticeCodexServerConfig.useIronSwingcastStaffConfigOverrideForGameTest(0.0D)) {
                var zombie = helper.spawn(EntityType.ZOMBIE, disabledPos);
                zombie.setLastHurtByPlayer(player);
                IronSwingcastStaffCrystallizeEvent.onLivingDeath(new LivingDeathEvent(
                        zombie,
                        helper.getLevel().damageSources().generic()
                ));
                helper.assertTrue(
                        countFreshItemDrops(
                                helper.getLevel(),
                                ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get(),
                                helper.absolutePos(disabledPos),
                                2.0D
                        ) == 0,
                        "Zero drop chance should disable Crystalline Arcane Shard drops"
                );
            }

            var offhandPos = new BlockPos(8, 2, 0);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ItemRegistry.IRON_SWINGCAST_STAFF.get()));
            try (var ignored = ApprenticeCodexServerConfig.useIronSwingcastStaffConfigOverrideForGameTest(1.0D)) {
                var zombie = helper.spawn(EntityType.ZOMBIE, offhandPos);
                zombie.setLastHurtByPlayer(player);
                IronSwingcastStaffCrystallizeEvent.onLivingDeath(new LivingDeathEvent(
                        zombie,
                        helper.getLevel().damageSources().generic()
                ));
                helper.assertTrue(
                        countFreshItemDrops(
                                helper.getLevel(),
                                ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get(),
                                helper.absolutePos(offhandPos),
                                2.0D
                        ) == 0,
                        "Offhand Iron Swingcast Staff should not add Crystalline Arcane Shards"
                );
            }
        });
    }

    static void ironSwingcastStaffCrystallizeHintFollowsCommonDescription(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var originalChance = IronSwingcastStaffConfigState.crystallineArcaneShardDropChance();
            var stack = new ItemStack(ItemRegistry.IRON_SWINGCAST_STAFF.get());
            try {
                IronSwingcastStaffConfigState.setCrystallineArcaneShardDropChance(0.05D);
                var enabledLines = new ArrayList<Component>();
                stack.getItem().appendHoverText(stack, helper.getLevel(), enabledLines, TooltipFlag.Default.NORMAL);
                helper.assertTrue(enabledLines.size() > 3, "Iron Swingcast Staff tooltip should include its ability hint");
                helper.assertTrue(
                        enabledLines.get(2).getContents() instanceof TranslatableContents commonContents
                                && "item.apprenticecodex.swingcast.common.desc".equals(commonContents.getKey()),
                        "Swingcast description should remain before the Iron ability hint"
                );
                helper.assertTrue(
                        enabledLines.get(3).getContents() instanceof TranslatableContents contents
                                && "item.apprenticecodex.iron_swingcast_staff.crystallize_hint".equals(contents.getKey()),
                        "Crystallize hint should follow the common Swingcast description"
                );

                IronSwingcastStaffConfigState.setCrystallineArcaneShardDropChance(0.0D);
                var disabledLines = new ArrayList<Component>();
                stack.getItem().appendHoverText(stack, helper.getLevel(), disabledLines, TooltipFlag.Default.NORMAL);
                helper.assertFalse(
                        disabledLines.stream().anyMatch(line ->
                                line.getContents() instanceof TranslatableContents contents
                                        && "item.apprenticecodex.iron_swingcast_staff.crystallize_hint".equals(contents.getKey())
                        ),
                        "Zero synchronized drop chance should hide the crystallize hint"
                );
            } finally {
                IronSwingcastStaffConfigState.setCrystallineArcaneShardDropChance(originalChance);
            }
        });
    }

    static void highTierSwingcastStaffReducesImbuedCooldownAfterFullyChargedMeleeHit(GameTestHelper helper) {
        var diamondPlayer = createTrackedEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "swing_diamond");
        var netheritePlayer = createTrackedEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 3), "swing_netherite");
        var diamondTarget = helper.spawn(EntityType.HUSK, new BlockPos(2, 2, 0));
        var netheriteTarget = helper.spawn(EntityType.HUSK, new BlockPos(2, 2, 3));
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var diamondMagicData = MagicData.getPlayerMagicData(diamondPlayer);
        var netheriteMagicData = MagicData.getPlayerMagicData(netheritePlayer);
        helper.assertTrue(diamondMagicData != null, "Diamond Swingcast cooldown test requires MagicData");
        helper.assertTrue(netheriteMagicData != null, "Netherite Swingcast cooldown test requires MagicData");

        diamondPlayer.setItemInHand(InteractionHand.MAIN_HAND, createImbuedSwingcastStack(
                (AbstractSwingcastStaffItem) ItemRegistry.DIAMOND_SWINGCAST_STAFF.get(), spell));
        netheritePlayer.setItemInHand(InteractionHand.MAIN_HAND, createImbuedSwingcastStack(
                (AbstractSwingcastStaffItem) ItemRegistry.NETHERITE_SWINGCAST_STAFF.get(), spell));

        helper.runAtTickTime(20, () -> {
            tickUntilAttackIsFullyCharged(diamondPlayer);
            tickUntilAttackIsFullyCharged(netheritePlayer);
            helper.assertTrue(
                    AbstractRightClickMagicWeaponItem.isFullyChargedAttack(diamondPlayer),
                    "Diamond Swingcast Staff test attack should be fully charged"
            );
            helper.assertTrue(
                    AbstractRightClickMagicWeaponItem.isFullyChargedAttack(netheritePlayer),
                    "Netherite Swingcast Staff test attack should be fully charged"
            );
            diamondMagicData.getPlayerCooldowns().addCooldown(spell, 100, 100);
            netheriteMagicData.getPlayerCooldowns().addCooldown(spell, 100, 100);
            var diamondTargetHealth = diamondTarget.getHealth();
            var netheriteTargetHealth = netheriteTarget.getHealth();
            diamondPlayer.attack(diamondTarget);
            netheritePlayer.attack(netheriteTarget);
            helper.assertTrue(
                    diamondTarget.getHealth() < diamondTargetHealth,
                    "Diamond Swingcast Staff test attack should damage its target"
            );
            helper.assertTrue(
                    netheriteTarget.getHealth() < netheriteTargetHealth,
                    "Netherite Swingcast Staff test attack should damage its target"
            );
        });
        helper.runAtTickTime(21, () -> {
            var diamondCooldown = diamondMagicData.getPlayerCooldowns()
                    .getSpellCooldowns().get(spell.getSpellId());
            var netheriteCooldown = netheriteMagicData.getPlayerCooldowns()
                    .getSpellCooldowns().get(spell.getSpellId());
            helper.assertTrue(
                    diamondCooldown != null
                            && diamondCooldown.getCooldownRemaining() >= 79
                            && diamondCooldown.getCooldownRemaining() <= 80,
                    "A fully charged Diamond Swingcast Staff melee hit should reduce 20 cooldown ticks: "
                            + (diamondCooldown == null ? "missing" : diamondCooldown.getCooldownRemaining())
            );
            helper.assertTrue(
                    netheriteCooldown != null
                            && netheriteCooldown.getCooldownRemaining() >= 89
                            && netheriteCooldown.getCooldownRemaining() <= 90,
                    "A fully charged Netherite Swingcast Staff melee hit should reduce 10 cooldown ticks: "
                            + (netheriteCooldown == null ? "missing" : netheriteCooldown.getCooldownRemaining())
            );
            helper.succeed();
        });
    }

    private static void tickUntilAttackIsFullyCharged(net.minecraftforge.common.util.FakePlayer player) {
        for (var tick = 0;
             tick < 40 && !AbstractRightClickMagicWeaponItem.isFullyChargedAttack(player);
             tick++) {
            player.doTick();
        }
    }

    static void highTierSwingcastStaffCooldownRespectsDisabledAndInvalidHits(GameTestHelper helper) {
        var player = createTrackedEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "swing_guard"
        );
        var offhandPlayer = createTrackedEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 2),
                "swing_offhand"
        );
        var target = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 2, 0));
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var stack = createImbuedSwingcastStack(
                (AbstractSwingcastStaffItem) ItemRegistry.DIAMOND_SWINGCAST_STAFF.get(),
                spell
        );
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "High-tier Swingcast guard test requires MagicData");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        offhandPlayer.setItemInHand(InteractionHand.OFF_HAND, stack.copy());

        helper.runAtTickTime(20, () -> {
            magicData.getPlayerCooldowns().addCooldown(spell, 100, 100);

            try (var ignored = ApprenticeCodexServerConfig
                    .useHighTierSwingcastStaffConfigOverrideForGameTest(0, 0)) {
                player.attack(target);
                helper.assertTrue(
                        magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId())
                                .getCooldownRemaining() == 100,
                        "Zero high-tier Swingcast config should disable cooldown reduction"
                );
            }

            try (var ignored = ApprenticeCodexServerConfig
                    .useHighTierSwingcastStaffConfigOverrideForGameTest(20, 10)) {
                player.attack(target);
                helper.assertTrue(
                        magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId())
                                .getCooldownRemaining() == 100,
                        "Uncharged attacks should not reduce high-tier Swingcast cooldowns"
                );

                var offhandMagicData = MagicData.getPlayerMagicData(offhandPlayer);
                helper.assertTrue(offhandMagicData != null, "Offhand Swingcast guard test requires MagicData");
                offhandMagicData.getPlayerCooldowns().addCooldown(spell, 100, 100);
                offhandPlayer.attack(target);
                helper.assertTrue(
                        offhandMagicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId())
                                .getCooldownRemaining() == 100,
                        "Offhand high-tier Swingcast Staff should not reduce cooldowns"
                );
            }
            helper.succeed();
        });
    }

    static void netheriteSwingcastStaffPreventsLongCastInterruptionInMainhand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(
                    helper,
                    new BlockPos(0, 2, 0),
                    "netherite_swingcast_interrupt_test"
            );
            var longSpell = SpellRegistry.ARCANE_BLAST.get();

            helper.assertTrue(longSpell.canBeInterrupted(player),
                    "Control long spell should be interruptible without Netherite Swingcast Staff");

            var netheriteStack = new ItemStack(ItemRegistry.NETHERITE_SWINGCAST_STAFF.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, netheriteStack);
            helper.assertFalse(longSpell.canBeInterrupted(player),
                    "Netherite Swingcast Staff should protect long casts in the main hand");

            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, netheriteStack);
            helper.assertTrue(longSpell.canBeInterrupted(player),
                    "Offhand Netherite Swingcast Staff should not protect long casts");
        });
    }

    static void swingcastStaffAbilityTooltipsUseSyncedConfigAndStableOrder(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var originalDiamondTicks = HighTierSwingcastStaffConfigState.diamondCooldownReductionTicks();
            var originalNetheriteTicks = HighTierSwingcastStaffConfigState.netheriteCooldownReductionTicks();
            try {
                HighTierSwingcastStaffConfigState.setCooldownReductionTicks(20, 10);
                var diamondLines = collectTooltipLines(
                        helper,
                        new ItemStack(ItemRegistry.DIAMOND_SWINGCAST_STAFF.get())
                );
                assertTooltipKeyAt(helper, diamondLines, 2,
                        "item.apprenticecodex.swingcast.common.desc",
                        "Diamond Swingcast common description order changed");
                assertTooltipKeyAt(helper, diamondLines, 3,
                        "item.apprenticecodex.high_tier_swingcast_staff.cooldown_hint",
                        "Diamond Swingcast cooldown hint should follow the common description");
                assertTooltipStringArgument(
                        helper,
                        diamondLines.get(3),
                        Utils.timeFromTicks(20, 1),
                        "Diamond Swingcast cooldown hint should use Iron's tick formatter"
                );

                var netheriteLines = collectTooltipLines(
                        helper,
                        new ItemStack(ItemRegistry.NETHERITE_SWINGCAST_STAFF.get())
                );
                assertTooltipKeyAt(helper, netheriteLines, 3,
                        "item.apprenticecodex.high_tier_swingcast_staff.cooldown_hint",
                        "Netherite Swingcast cooldown hint should follow the common description");
                assertTooltipStringArgument(
                        helper,
                        netheriteLines.get(3),
                        Utils.timeFromTicks(10, 1),
                        "Netherite Swingcast cooldown hint should use Iron's tick formatter"
                );
                assertTooltipKeyAt(helper, netheriteLines, 4,
                        "item.apprenticecodex.netherite_swingcast_staff.protect_hint",
                        "Netherite Swingcast protection hint should follow the cooldown hint");

                HighTierSwingcastStaffConfigState.setCooldownReductionTicks(0, 0);
                var disabledDiamondLines = collectTooltipLines(
                        helper,
                        new ItemStack(ItemRegistry.DIAMOND_SWINGCAST_STAFF.get())
                );
                helper.assertFalse(containsTooltipKey(
                                disabledDiamondLines,
                                "item.apprenticecodex.high_tier_swingcast_staff.cooldown_hint"),
                        "Zero Diamond Swingcast config should hide the cooldown hint");

                var disabledNetheriteLines = collectTooltipLines(
                        helper,
                        new ItemStack(ItemRegistry.NETHERITE_SWINGCAST_STAFF.get())
                );
                assertTooltipKeyAt(helper, disabledNetheriteLines, 3,
                        "item.apprenticecodex.netherite_swingcast_staff.protect_hint",
                        "Netherite protection hint should keep its relative position when cooldown is disabled");
            } finally {
                HighTierSwingcastStaffConfigState.setCooldownReductionTicks(
                        originalDiamondTicks,
                        originalNetheriteTicks
                );
            }
        });
    }
    static void crystallineArcaneShardUsesBlastingOnlyRecipeContract(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipeManager = helper.getLevel().getRecipeManager();
            var recipeId = ItemRegistry.CRYSTALLINE_ARCANE_SHARD.getId();
            var recipe = recipeManager.byKey(recipeId).orElseThrow(
                    () -> new IllegalStateException("Missing Crystalline Arcane Shard blasting recipe")
            );
            helper.assertTrue(
                    recipe instanceof BlastingRecipe,
                    "Crystalline Arcane Shard recipe should use minecraft:blasting"
            );
            var blastingRecipe = (BlastingRecipe) recipe;
            helper.assertTrue(
                    Math.abs(blastingRecipe.getExperience() - 1.0F) < 1.0e-6F,
                    "Crystalline Arcane Shard blasting experience should be 1.0"
            );
            helper.assertTrue(
                    blastingRecipe.getCookingTime() == 100,
                    "Crystalline Arcane Shard blasting time should be 100 ticks"
            );
            helper.assertTrue(
                    blastingRecipe.getResultItem(helper.getLevel().registryAccess())
                            .is(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get()),
                    "Crystalline Arcane Shard blasting result should be Arcane Essence"
            );

            var input = new SimpleContainer(new ItemStack(ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get()));
            helper.assertTrue(
                    recipeManager.getRecipeFor(RecipeType.SMELTING, input, helper.getLevel()).isEmpty(),
                    "Crystalline Arcane Shard should not have a furnace smelting recipe"
            );
        });
    }
    static void swingcastStaffTiersExposeRequestedImbueRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var instantSpell = SpellRegistry.AUTO_MAGNET.get();
            var longSpell = SpellRegistry.ARCANE_BLAST.get();
            var continuousSpell = SpellRegistry.BULLET_STREAM.get();

            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.IRON_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT),
                    SwingcastCooldownMode.IMBUED_ONLY,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Iron Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.COPPER_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT),
                    SwingcastCooldownMode.IMBUED_ONLY,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Copper Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.SILVER_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG),
                    SwingcastCooldownMode.IMBUED_PLUS_LONG_CAST_TIME,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Silver Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.GOLD_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT),
                    SwingcastCooldownMode.IMBUED_ONLY,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Gold Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.DIAMOND_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT),
                    SwingcastCooldownMode.IMBUED_ONLY,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Diamond Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.NETHERITE_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT),
                    SwingcastCooldownMode.IMBUED_ONLY,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Netherite Swingcast Staff"
            );
        });
    }

    private static ItemStack createImbuedSwingcastStack(
            AbstractSwingcastStaffItem item,
            io.redspace.ironsspellbooks.api.spells.AbstractSpell spell
    ) {
        var stack = new ItemStack(item);
        var mutable = ISpellContainer.create(1, true, false).mutableCopy();
        mutable.addSpellAtIndex(spell, 1, 0, false);
        ISpellContainer.set(stack, mutable.toImmutable());
        return stack;
    }

    private static ArrayList<Component> collectTooltipLines(GameTestHelper helper, ItemStack stack) {
        var lines = new ArrayList<Component>();
        stack.getItem().appendHoverText(
                stack,
                helper.getLevel(),
                lines,
                TooltipFlag.Default.NORMAL
        );
        return lines;
    }

    private static void assertTooltipKeyAt(
            GameTestHelper helper,
            java.util.List<Component> lines,
            int index,
            String expectedKey,
            String message
    ) {
        helper.assertTrue(lines.size() > index, message + " (tooltip line count=" + lines.size() + ")");
        if (lines.size() > index) {
            assertTranslatableKey(helper, lines.get(index), expectedKey, message);
        }
    }

    private static void assertTooltipStringArgument(
            GameTestHelper helper,
            Component line,
            String expectedArgument,
            String message
    ) {
        helper.assertTrue(
                line.getContents() instanceof TranslatableContents,
                message + " (tooltip is not translatable)"
        );
        if (!(line.getContents() instanceof TranslatableContents contents)) {
            return;
        }
        var args = contents.getArgs();
        helper.assertTrue(
                args.length == 1 && expectedArgument.equals(args[0]),
                message + " (expected=" + expectedArgument + ", actual=" + java.util.Arrays.toString(args) + ")"
        );
    }

    private static boolean containsTooltipKey(java.util.List<Component> lines, String expectedKey) {
        return lines.stream().anyMatch(line ->
                line.getContents() instanceof TranslatableContents contents
                        && expectedKey.equals(contents.getKey())
        );
    }
    private static ItemStack createLegacyCrystalBladedStaffContainer(
            io.redspace.ironsspellbooks.api.spells.AbstractSpell spell,
            int spellLevel,
            boolean locked
    ) {
        var stack = new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get());
        var mutable = ISpellContainer.create(1, true, false).mutableCopy();
        mutable.addSpellAtIndex(spell, spellLevel, 0, locked);
        ISpellContainer.set(stack, mutable.toImmutable());
        return stack;
    }

}
