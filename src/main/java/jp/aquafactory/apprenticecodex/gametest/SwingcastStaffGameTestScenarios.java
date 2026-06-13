package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;

import java.util.Set;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import jp.aquafactory.apprenticecodex.item.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.item.SpellGunCastType;
import jp.aquafactory.apprenticecodex.item.SwingTriggeredMagicItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastCooldownMode;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastStaffCastContext;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.manaslash.ManaSlash;
import jp.aquafactory.apprenticecodex.spell.manaslash.ManaSlashProjectileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

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
                    SwingcastCooldownMode.IMBUED_ONLY,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Silver Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.GOLD_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG),
                    SwingcastCooldownMode.IMBUED_PLUS_LONG_CAST_TIME,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Gold Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.DIAMOND_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG),
                    SwingcastCooldownMode.IMBUED_PLUS_LONG_CAST_TIME,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Diamond Swingcast Staff"
            );
            assertSwingcastStaffTier(
                    helper,
                    (AbstractSwingcastStaffItem) ItemRegistry.NETHERITE_SWINGCAST_STAFF.get(),
                    Set.of(SpellGunCastType.INSTANT, SpellGunCastType.LONG),
                    SwingcastCooldownMode.IMBUED_PLUS_LONG_CAST_TIME,
                    instantSpell,
                    longSpell,
                    continuousSpell,
                    "Netherite Swingcast Staff"
            );
        });
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
