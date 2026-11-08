package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.bound;

import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdgeMirror;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.boundbow.BoundBowManager;
import jp.aquafactory.apprenticecodex.spell.edgedancer.EdgeDancerManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.bound.BoundWeaponTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BoundBowStorageGameTests {
    private BoundBowStorageGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void boundBowActivationStoresAndRestoresMainhand(GameTestHelper helper) {
        var player = createPlayer(helper, "bound_bow_restore_test");
        var original = new ItemStack(Items.DIAMOND);
        player.setItemInHand(InteractionHand.MAIN_HAND, original.copy());

        BoundBowManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), boundBow(), 3);

        var bow = player.getMainHandItem();
        helper.assertTrue(bow.is(ItemRegistry.BOUND_BOW.get()), "Bound Bow should replace the mainhand item");
        helper.assertTrue(getPowerLevel(helper, bow) == 3,
                "Bound Bow should keep the resolved Power level");

        var state = Capabilities.getSpellDataOrNull(player).get(CodexSpellStateTypeRegister.BOUND_BOW_STATE);
        helper.assertTrue(state.hasStoredMainhandStack() && state.getStoredMainhandStack().is(Items.DIAMOND),
                "Bound Bow state should store the replaced mainhand item");

        BoundBowManager.deactivate(player, true);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND),
                "Bound Bow should restore the stored item to the main hand");
        helper.assertFalse(Capabilities.getSpellDataOrNull(player)
                        .get(CodexSpellStateTypeRegister.BOUND_BOW_STATE).active,
                "Bound Bow state should be inactive after deactivation");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundBowActivationDeactivatesEdgeDancerFirst(GameTestHelper helper) {
        var player = createPlayer(helper, "bound_bow_deactivates_edge_dancer_test");
        var magicData = resolveMagicData(helper, player);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemRegistry.SPELL_SIDE_EDGE.get().getDefaultInstance());
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));

        EdgeDancerManager.activate(player, 1, CastSource.SPELLBOOK, magicData, edgeDancer());
        helper.assertTrue(SpellSideEdgeMirror.isGeneratedMirror(player.getOffhandItem()),
                "Edge Dancer should generate a Mirror before Bound Bow activation");

        BoundBowManager.activate(player, 1, CastSource.SPELLBOOK, magicData, boundBow(), 1);

        helper.assertFalse(Capabilities.getSpellDataOrNull(player)
                        .get(CodexSpellStateTypeRegister.EDGE_DANCER_STATE).active,
                "Bound Bow activation should deactivate Edge Dancer first");
        helper.assertTrue(player.getMainHandItem().is(ItemRegistry.BOUND_BOW.get()),
                "Bound Bow should replace the mainhand item after Edge Dancer is deactivated");
        helper.assertTrue(player.getOffhandItem().is(Items.SHIELD),
                "Bound Bow activation should restore the Edge Dancer offhand item");

        BoundBowManager.deactivate(player, true);
        helper.assertTrue(player.getMainHandItem().is(ItemRegistry.SPELL_SIDE_EDGE.get()),
                "Bound Bow should restore the pre-cast Spell Side Edge to the main hand");
        helper.assertTrue(player.getOffhandItem().is(Items.SHIELD),
                "Bound Bow should keep the restored offhand item after deactivation");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundBowCanMoveWithinInventoryAndCursor(GameTestHelper helper) {
        var player = createPlayer(helper, "bound_bow_move_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD));

        BoundBowManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), boundBow(), 1);
        var bow = player.getMainHandItem().copy();
        player.getInventory().items.set(player.getInventory().selected, ItemStack.EMPTY);
        player.getInventory().offhand.set(0, bow);

        BoundBowManager.validateActiveBowLocation(player);
        helper.assertTrue(player.getOffhandItem().is(ItemRegistry.BOUND_BOW.get()),
                "Bound Bow should stay active after moving to the offhand");

        var movedBow = player.getOffhandItem().copy();
        player.getInventory().offhand.set(0, ItemStack.EMPTY);
        player.getInventory().items.set(10, movedBow);
        BoundBowManager.validateActiveBowLocation(player);

        helper.assertTrue(player.getInventory().items.get(10).is(ItemRegistry.BOUND_BOW.get()),
                "Moving Bound Bow inside the player inventory should keep it active");
        helper.assertTrue(Capabilities.getSpellDataOrNull(player)
                        .get(CodexSpellStateTypeRegister.BOUND_BOW_STATE).active,
                "Bound Bow state should stay active while the generated bow remains in player inventory");

        var carriedBow = player.getInventory().items.get(10).copy();
        player.getInventory().items.set(10, ItemStack.EMPTY);
        player.containerMenu.setCarried(carriedBow);
        BoundBowManager.validateActiveBowLocation(player);

        helper.assertTrue(player.containerMenu.getCarried().is(ItemRegistry.BOUND_BOW.get()),
                "Bound Bow should stay active while held by the cursor");
        BoundBowManager.deactivate(player, true);
        helper.assertTrue(player.containerMenu.getCarried().isEmpty(),
                "Deactivation should remove a cursor-held Bound Bow");
        helper.assertTrue(player.getMainHandItem().is(Items.EMERALD),
                "Deactivation should restore the stored item");
        helper.succeed();
    }
}
