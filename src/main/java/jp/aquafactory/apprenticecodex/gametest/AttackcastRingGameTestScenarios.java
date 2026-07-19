package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaffAttackContextManager;
import jp.aquafactory.apprenticecodex.item.curios.attackcastring.AttackcastRing;
import jp.aquafactory.apprenticecodex.item.curios.attackcastring.AttackcastRingAttackTrigger;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class AttackcastRingGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private AttackcastRingGameTestScenarios() {
    }

    static void attackcastRingSupportsOnlyRemovableInstantSpells(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var ring = (AttackcastRing) ItemRegistry.ATTACKCAST_RING.get();
            var stack = ring.getDefaultInstance();
            var instantSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();
            var longSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBALL_SPELL.get();

            helper.assertTrue(ISpellContainer.isSpellContainer(stack),
                    "Attackcast Ring should initialize its spell container");
            helper.assertTrue(ring.canImbueSpell(instantSpell, 1),
                    "Attackcast Ring should accept instant spells");
            helper.assertTrue(!ring.canImbueSpell(longSpell, 1),
                    "Attackcast Ring should reject long spells");

            var imbued = createRingStack(helper, instantSpell);
            var container = ISpellContainer.get(imbued);
            helper.assertTrue(container != null && ring.canRemoveWorkbenchSpell(
                            imbued,
                            container,
                            0,
                            container.getSpellAtIndex(0)),
                    "Attackcast Ring imbued spells should remain removable");
        });
    }

    static void attackcastRingConsumesManaAndAddsCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "attackcast_ring_cast");
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();
        var ringStack = createRingStack(helper, spell);
        equipRing(player, 0, ringStack);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));

        var magicData = requireMagicData(helper, player);
        magicData.setMana(1000.0F);
        var manaBefore = magicData.getMana();

        helper.assertTrue(AttackcastRingAttackTrigger.tryTriggerAttack(player, InteractionHand.MAIN_HAND, true),
                "Attackcast Ring should cast from an ordinary weapon attack");
        helper.assertTrue(magicData.getMana() < manaBefore,
                "Attackcast Ring should consume normal sword-cast mana");
        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                "Attackcast Ring should add the normal sword-cast cooldown");
        helper.assertTrue(!magicData.isCasting(),
                "Attackcast Ring instant casts should complete before processing another ring");
        helper.assertTrue(ItemStack.isSameItemSameTags(magicData.getPlayerCastingItem(), ringStack),
                "Attackcast Ring should remain the casting item");
        helper.succeed();
    }

    static void attackcastRingSilentlySkipsCooldownAndFailsWithoutMana(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "attackcast_ring_failures");
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();
        equipRing(player, 0, createRingStack(helper, spell));
        var magicData = requireMagicData(helper, player);
        magicData.setMana(1000.0F);

        helper.assertTrue(AttackcastRingAttackTrigger.tryTriggerAttack(player, InteractionHand.MAIN_HAND, true),
                "Attackcast Ring failure test should establish a cooldown");
        var manaAfterFirstCast = magicData.getMana();
        helper.assertTrue(!AttackcastRingAttackTrigger.tryTriggerAttack(player, InteractionHand.MAIN_HAND, true),
                "Attackcast Ring should skip a spell that is on cooldown");
        helper.assertTrue(magicData.getMana() == manaAfterFirstCast,
                "Cooldown skips should not consume mana");

        magicData.getPlayerCooldowns().clearCooldowns();
        magicData.setMana(0.0F);
        helper.assertTrue(!AttackcastRingAttackTrigger.tryTriggerAttack(player, InteractionHand.MAIN_HAND, true),
                "Attackcast Ring should fail when mana is insufficient");
        helper.assertTrue(!magicData.getPlayerCooldowns().isOnCooldown(spell),
                "Failed Attackcast Ring casts should not add cooldowns");
        helper.succeed();
    }

    static void attackcastRingDoesNotInterruptActiveCast(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "attackcast_ring_active_cast");
        var activeSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBALL_SPELL.get();
        var ringSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();
        var heldItem = (AbstractSwingcastStaffItem) ItemRegistry.COPPER_SWINGCAST_STAFF.get();
        var heldStack = new ItemStack(heldItem);
        heldItem.initializeSpellContainer(heldStack);
        player.setItemInHand(InteractionHand.MAIN_HAND, heldStack);
        equipRing(player, 0, createRingStack(helper, ringSpell));
        var magicData = requireMagicData(helper, player);
        magicData.setMana(1000.0F);
        magicData.setSyncedData(new io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData(player));
        magicData.initiateCast(
                activeSpell,
                1,
                60,
                io.redspace.ironsspellbooks.api.spells.CastSource.SPELLBOOK,
                "gametest"
        );
        var manaBefore = magicData.getMana();

        helper.assertTrue(!AttackcastRingAttackTrigger.tryTriggerAttack(player, InteractionHand.MAIN_HAND, true),
                "Held Swingcast and Attackcast Ring should both skip while another spell is casting");
        helper.assertTrue(magicData.isCasting(),
                "Attackcast Ring should not cancel the active cast");
        helper.assertTrue(activeSpell.getSpellId().equals(magicData.getCastingSpellId()),
                "Attackcast Ring should preserve the active casting spell");
        helper.assertTrue(!magicData.getPlayerCooldowns().isOnCooldown(ringSpell),
                "Skipped Attackcast Ring should not add its cooldown");
        helper.assertTrue(magicData.getMana() == manaBefore,
                "Skipped Attackcast Ring should not consume mana");
        helper.succeed();
    }

    static void attackcastRingCastsAllEquippedRingsInSlotOrder(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "attackcast_ring_multiple");
        var firstSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();
        var secondSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.OAKSKIN_SPELL.get();
        var firstRing = createRingStack(helper, firstSpell);
        var secondRing = createRingStack(helper, secondSpell);
        equipRing(player, 0, firstRing);
        equipRing(player, 1, secondRing);
        var magicData = requireMagicData(helper, player);
        magicData.setMana(1000.0F);

        helper.assertTrue(AttackcastRingAttackTrigger.tryTriggerAttack(player, InteractionHand.MAIN_HAND, true),
                "Attackcast Ring should process all equipped rings");
        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(firstSpell),
                "The first equipped Attackcast Ring should cast");
        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(secondSpell),
                "The second equipped Attackcast Ring should cast");
        helper.assertTrue(ItemStack.isSameItemSameTags(magicData.getPlayerCastingItem(), secondRing),
                "Attackcast Rings should be processed in Curios slot order");
        helper.succeed();
    }

    static void attackcastRingDefersToSuccessfulHeldSwingcast(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "attackcast_ring_held_priority");
        var heldItem = (AbstractSwingcastStaffItem) ItemRegistry.COPPER_SWINGCAST_STAFF.get();
        var heldStack = new ItemStack(heldItem);
        heldItem.initializeSpellContainer(heldStack);
        player.setItemInHand(InteractionHand.MAIN_HAND, heldStack);
        var ringSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.OAKSKIN_SPELL.get();
        equipRing(player, 0, createRingStack(helper, ringSpell));
        var magicData = requireMagicData(helper, player);
        magicData.setMana(1000.0F);

        helper.assertTrue(AttackcastRingAttackTrigger.tryTriggerAttack(player, InteractionHand.MAIN_HAND, true),
                "Held Swingcast should start before Attackcast Ring fallback");
        helper.assertTrue(io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get()
                        .getSpellId().equals(magicData.getCastingSpellId()),
                "Held Swingcast should keep priority over Attackcast Ring");
        helper.assertTrue(!magicData.getPlayerCooldowns().isOnCooldown(ringSpell),
                "Attackcast Ring should not cast after a successful held Swingcast");
        helper.succeed();
    }

    static void attackcastRingRequiresFullChargeOutsideCompatTiming(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "attackcast_ring_charge");
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();
        equipRing(player, 0, createRingStack(helper, spell));
        requireMagicData(helper, player).setMana(1000.0F);
        player.resetAttackStrengthTicker();

        helper.assertTrue(AttackcastRingAttackTrigger.canTriggerAttack(player, InteractionHand.MAIN_HAND),
                "An equipped Attackcast Ring should make empty-hand attacks eligible");
        helper.assertTrue(!AttackcastRingAttackTrigger.tryTriggerAttack(player, InteractionHand.MAIN_HAND, false),
                "Attackcast Ring should reject an uncharged vanilla attack");
        helper.succeed();
    }

    static void attackcastRingEpicFightAttackPhaseUsesEquippedRing(GameTestHelper helper) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded(
                jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightCompat.MOD_ID)) {
            helper.succeed();
            return;
        }

        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "attackcast_ring_epic_fight");
        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();
        equipRing(player, 0, createRingStack(helper, spell));
        var magicData = requireMagicData(helper, player);
        magicData.setMana(1000.0F);

        helper.assertTrue(
                jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSwingMagicCompat
                        .triggerSwingMagicFromAttackPhase(player, InteractionHand.MAIN_HAND, -1, 0),
                "Epic Fight attack phase should trigger an equipped Attackcast Ring"
        );
        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                "Epic Fight Attackcast Ring should add the normal spell cooldown");
        helper.succeed();
    }

    static void attackcastRingEpicFightStaffrifleDoesNotFallback(GameTestHelper helper) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded(
                jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightCompat.MOD_ID)) {
            helper.succeed();
            return;
        }

        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "attackcast_ring_epic_fight_staffrifle");
        player.setItemInHand(
                InteractionHand.MAIN_HAND,
                new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get())
        );
        var ringSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();
        equipRing(player, 0, createRingStack(helper, ringSpell));
        var magicData = requireMagicData(helper, player);
        magicData.setMana(1000.0F);

        helper.assertTrue(
                !jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSwingMagicCompat
                        .triggerSwingMagicFromAttackPhase(player, InteractionHand.MAIN_HAND, -1, 1),
                "Epic Fight Staffrifle failure should not fall back to Attackcast Ring"
        );
        helper.assertTrue(!magicData.getPlayerCooldowns().isOnCooldown(ringSpell),
                "Epic Fight Staffrifle should leave Attackcast Ring untouched after failure");
        helper.succeed();
    }

    static void attackcastRingEpicFightUsesSyncedBlockTarget(GameTestHelper helper) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded(
                jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightCompat.MOD_ID)) {
            helper.succeed();
            return;
        }

        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "attackcast_ring_epic_fight_block_target");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        var spell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.RIFT_HOLE.get();
        equipRing(player, 0, createRingStack(helper, spell));
        var magicData = requireMagicData(helper, player);
        var maxMana = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get());
        helper.assertTrue(maxMana != null, "Epic Fight Attackcast Ring test should resolve max mana");
        maxMana.setBaseValue(1000.0D);
        magicData.setMana(1000.0F);
        magicData.getSyncedData().learnSpell(spell, false);

        var targetPos = new BlockPos(2, 2, 0);
        helper.setBlock(targetPos, Blocks.STONE);
        var absoluteTargetPos = helper.absolutePos(targetPos);
        var targetData = new BlockTargetData();
        targetData.setTarget(
                absoluteTargetPos,
                Direction.WEST,
                Vec3.atCenterOf(absoluteTargetPos),
                absoluteTargetPos.relative(Direction.WEST),
                Direction.EAST
        );

        helper.assertTrue(
                jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper
                        .validateTarget(helper.getLevel(), player, 16.0D, targetData)
                        .isPresent(),
                "Epic Fight Attackcast Ring test target should pass server validation"
        );
        helper.assertTrue(
                jp.aquafactory.apprenticecodex.spell.rifthole.RiftHoleBlockSafety
                        .canReplace(helper.getLevel(), absoluteTargetPos),
                "Epic Fight Attackcast Ring test target should be replaceable by Rift Hole"
        );
        helper.assertTrue(
                jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSwingMagicCompat
                        .queueAttackcastRingTargets(player, List.of(targetData)),
                "Epic Fight should queue the Attackcast Ring block target"
        );
        helper.assertTrue(
                jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSwingMagicCompat
                        .triggerSwingMagicFromAttackPhase(player, InteractionHand.MAIN_HAND, -1, 2),
                "Epic Fight Attackcast Ring should cast with its synced block target"
        );
        helper.assertBlockPresent(jp.aquafactory.apprenticecodex.registry.BlockRegistry.RIFT_HOLE.get(), targetPos);
        helper.succeed();
    }

    static void attackcastRingFallsBackAfterCrystalBladedStaffHit(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "attackcast_ring_crystal_hit");
        var staffStack = createCrystalBladedStaffStack();
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
        var ringSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();
        equipRing(player, 0, createRingStack(helper, ringSpell));
        var magicData = requireMagicData(helper, player);
        magicData.setMana(1000.0F);

        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(
                        player,
                        InteractionHand.MAIN_HAND,
                        true,
                        2
                ),
                "Crystal Bladed Staff hit should create a pending Attackcast Ring fallback"
        );
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        helper.assertTrue(target.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F),
                "Crystal Bladed Staff Attackcast Ring hit test should deal direct player attack damage");

        helper.runAfterDelay(4, () -> {
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(ringSpell),
                    "Crystal Bladed Staff hit should suppress Mana Slash and cast the Attackcast Ring");
            helper.succeed();
        });
    }

    static void attackcastRingFallsBackAfterCrystalBladedStaffMissFailure(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "attackcast_ring_crystal_miss_failure");
        var staffStack = createCrystalBladedStaffStack();
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
        var staffSpell = jp.aquafactory.apprenticecodex.registry.SpellRegistry.MANA_SLASH.get();
        var ringSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();
        equipRing(player, 0, createRingStack(helper, ringSpell));
        var magicData = requireMagicData(helper, player);
        magicData.setMana(1000.0F);
        io.redspace.ironsspellbooks.api.magic.MagicHelper.MAGIC_MANAGER.addCooldown(
                player,
                staffSpell,
                io.redspace.ironsspellbooks.api.spells.CastSource.SWORD
        );

        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(player, InteractionHand.MAIN_HAND, true),
                "Crystal Bladed Staff miss should create a pending Attackcast Ring fallback"
        );

        helper.succeedWhen(() -> helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(ringSpell),
                "Failed Crystal Bladed Staff miss cast should fall back to the Attackcast Ring"));
    }

    static void attackcastRingCrystalBladedStaffDoesNotInterruptActiveCast(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "attackcast_ring_crystal_active_cast");
        player.setItemInHand(InteractionHand.MAIN_HAND, createCrystalBladedStaffStack());
        var activeSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBALL_SPELL.get();
        var ringSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.BALL_LIGHTNING_SPELL.get();
        equipRing(player, 0, createRingStack(helper, ringSpell));
        var magicData = requireMagicData(helper, player);
        magicData.setMana(1000.0F);
        magicData.setSyncedData(new io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData(player));
        magicData.initiateCast(
                activeSpell,
                1,
                60,
                io.redspace.ironsspellbooks.api.spells.CastSource.SPELLBOOK,
                "gametest"
        );
        var manaBefore = magicData.getMana();

        helper.assertTrue(
                CrystalBladedStaffAttackContextManager.requestMissTrigger(
                        player,
                        InteractionHand.MAIN_HAND,
                        true,
                        1
                ),
                "Crystal Bladed Staff should queue its delayed Attackcast Ring fallback"
        );
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(magicData.isCasting(),
                    "Crystal Bladed Staff fallback should preserve the active cast");
            helper.assertTrue(activeSpell.getSpellId().equals(magicData.getCastingSpellId()),
                    "Crystal Bladed Staff fallback should not replace the active spell");
            helper.assertTrue(!magicData.getPlayerCooldowns().isOnCooldown(ringSpell),
                    "Crystal Bladed Staff fallback should leave Attackcast Ring untouched");
            helper.assertTrue(magicData.getMana() == manaBefore,
                    "Skipped Crystal Bladed Staff fallback should not consume mana");
            helper.succeed();
        });
    }

    private static ItemStack createRingStack(GameTestHelper helper, AbstractSpell spell) {
        var stack = new ItemStack(ItemRegistry.ATTACKCAST_RING.get());
        var mutable = ISpellContainer.create(1, false, false).mutableCopy();
        helper.assertTrue(mutable.addSpellAtIndex(spell, 1, 0, false),
                "Attackcast Ring test could not imbue " + spell.getSpellId());
        ISpellContainer.set(stack, mutable.toImmutable());
        return stack;
    }

    private static ItemStack createCrystalBladedStaffStack() {
        var item = (CrystalBladedStaff) ItemRegistry.CRYSTAL_BLADED_STAFF.get();
        var stack = new ItemStack(item);
        item.initializeSpellContainer(stack);
        return stack;
    }

    private static void equipRing(net.minecraftforge.common.util.FakePlayer player, int index, ItemStack stack) {
        var curios = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Attackcast Ring test could not resolve Curios inventory"));
        curios.setEquippedCurio(io.redspace.ironsspellbooks.compat.Curios.RING_SLOT, index, stack);
    }

    private static MagicData requireMagicData(GameTestHelper helper, net.minecraftforge.common.util.FakePlayer player) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Attackcast Ring test could not resolve player magic data");
        return magicData;
    }
}
