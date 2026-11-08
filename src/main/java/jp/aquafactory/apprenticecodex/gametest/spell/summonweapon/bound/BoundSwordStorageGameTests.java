package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.bound;

import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.item.boundweapon.BoundSwordItem;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdgeMirror;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.boundsword.BoundSwordManager;
import jp.aquafactory.apprenticecodex.spell.edgedancer.EdgeDancerManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spellsideedge.ApprenticeCodexBoundSwordGameTests.resolveMagicData;
import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.bound.BoundSwordDualWieldGameTests.*;
import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.bound.BoundWeaponTestSupport.boundSword;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BoundSwordStorageGameTests {
    private BoundSwordStorageGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void boundSwordActivationStoresAndRestoresMainhand(GameTestHelper helper) {
        var player = createBoundSwordTestPlayer(helper, "bound_sword_restore_test");
        var original = new ItemStack(Items.DIAMOND);
        player.setItemInHand(InteractionHand.MAIN_HAND, original.copy());

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), boundSword(), 7.5F);

        var sword = player.getMainHandItem();
        helper.assertTrue(sword.is(ItemRegistry.BOUND_SWORD.get()), "Bound Sword should replace the mainhand item");
        helper.assertTrue(BoundSwordItem.getDisplayDamage(sword) == 7.5F,
                "Bound Sword should keep a snapshot of display damage");

        var state = Capabilities.getSpellDataOrNull(player).get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE);
        helper.assertTrue(state.hasStoredMainhandStack() && state.getStoredMainhandStack().is(Items.DIAMOND),
                "Bound Sword state should store the replaced mainhand item");

        BoundSwordManager.deactivate(player, true);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND),
                "Bound Sword should restore the stored item to the main hand");
        helper.assertTrue(!Capabilities.getSpellDataOrNull(player)
                        .get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE).active,
                "Bound Sword state should be inactive after deactivation");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordActivationDeactivatesEdgeDancerFirst(GameTestHelper helper) {
        var player = createBoundSwordTestPlayer(helper, "bound_sword_deactivates_edge_dancer_test");
        var magicData = resolveMagicData(helper, player);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemRegistry.SPELL_SIDE_EDGE.get().getDefaultInstance());
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));

        EdgeDancerManager.activate(player, 1, CastSource.SPELLBOOK, magicData, edgeDancer());
        helper.assertTrue(SpellSideEdgeMirror.isGeneratedMirror(player.getOffhandItem()),
                "Edge Dancer should generate a Mirror before Bound Sword activation");

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, magicData, boundSword(), 8.0F, true);

        helper.assertFalse(Capabilities.getSpellDataOrNull(player)
                        .get(CodexSpellStateTypeRegister.EDGE_DANCER_STATE).active,
                "Bound Sword activation should deactivate Edge Dancer first");
        helper.assertTrue(!SpellSideEdgeMirror.isGeneratedMirror(player.getOffhandItem()),
                "Bound Sword activation should remove the generated Edge Dancer Mirror");
        if (BoundSwordManager.hasDualWieldCompat()) {
            helper.assertTrue(player.getOffhandItem().is(ItemRegistry.BOUND_SWORD.get()),
                    "Bound Sword should replace the restored offhand after Edge Dancer is deactivated");
        } else {
            helper.assertTrue(player.getOffhandItem().is(Items.SHIELD),
                    "Bound Sword without dual-wield compat should leave the restored offhand item in place");
        }

        BoundSwordManager.deactivate(player, true);
        helper.assertTrue(player.getMainHandItem().is(ItemRegistry.SPELL_SIDE_EDGE.get()),
                "Bound Sword should restore the pre-cast Spell Side Edge to the main hand");
        helper.assertTrue(player.getOffhandItem().is(Items.SHIELD),
                "Bound Sword should restore the original offhand item after Edge Dancer handoff");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordCanMoveWithinInventoryAndCursor(GameTestHelper helper) {
        var player = createBoundSwordTestPlayer(helper, "bound_sword_invalid_move_test");
        var original = new ItemStack(Items.EMERALD);
        player.setItemInHand(InteractionHand.MAIN_HAND, original.copy());

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), boundSword(), 6.0F);
        var sword = player.getMainHandItem().copy();
        player.getInventory().items.set(player.getInventory().selected, ItemStack.EMPTY);
        player.getInventory().items.set(10, sword);

        BoundSwordManager.validateActiveSwordLocation(player);

        helper.assertTrue(player.getInventory().items.get(10).is(ItemRegistry.BOUND_SWORD.get()),
                "Moving Bound Sword inside the player inventory should keep it active");
        helper.assertTrue(Capabilities.getSpellDataOrNull(player)
                        .get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE).active,
                "Bound Sword state should stay active while the generated sword remains in player inventory");

        var carriedSword = player.getInventory().items.get(10).copy();
        player.getInventory().items.set(10, ItemStack.EMPTY);
        player.containerMenu.setCarried(carriedSword);
        BoundSwordManager.validateActiveSwordLocation(player);

        helper.assertTrue(player.containerMenu.getCarried().is(ItemRegistry.BOUND_SWORD.get()),
                "Bound Sword should stay active while held by the cursor");
        BoundSwordManager.deactivate(player, true);
        helper.assertTrue(player.containerMenu.getCarried().isEmpty(),
                "Deactivation should remove a cursor-held Bound Sword");
        helper.assertTrue(player.getMainHandItem().is(Items.EMERALD),
                "Deactivation should restore the stored item");
        helper.succeed();
    }
}
