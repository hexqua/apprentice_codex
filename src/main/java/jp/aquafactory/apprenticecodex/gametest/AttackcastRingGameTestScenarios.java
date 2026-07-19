package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.item.curios.attackcastring.AttackcastRing;
import jp.aquafactory.apprenticecodex.item.curios.attackcastring.AttackcastRingAttackTrigger;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

    private static ItemStack createRingStack(GameTestHelper helper, AbstractSpell spell) {
        var stack = new ItemStack(ItemRegistry.ATTACKCAST_RING.get());
        var mutable = ISpellContainer.create(1, false, false).mutableCopy();
        helper.assertTrue(mutable.addSpellAtIndex(spell, 1, 0, false),
                "Attackcast Ring test could not imbue " + spell.getSpellId());
        ISpellContainer.set(stack, mutable.toImmutable());
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
