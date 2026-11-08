package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.bound;

import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.item.boundweapon.BoundSwordItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.boundsword.BoundSwordManager;
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
public final class BoundSwordDualWieldGameTests {
    private BoundSwordDualWieldGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordWithoutCombatCompatDoesNotGenerateOffhand(GameTestHelper helper) {
        if (BoundSwordManager.hasDualWieldCompat()) {
            helper.succeed();
            return;
        }

        var player = createPlayer(helper, "bound_sword_no_compat_dual_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), boundSword(), 7.0F);

        helper.assertTrue(player.getMainHandItem().is(ItemRegistry.BOUND_SWORD.get()),
                "Bound Sword should replace the mainhand item without combat compat");
        helper.assertTrue(player.getOffhandItem().isEmpty(),
                "Bound Sword should not generate an offhand sword without Better Combat or Epic Fight");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordCombatCompatGeneratesOffhandWhenEmpty(GameTestHelper helper) {
        if (!BoundSwordManager.hasDualWieldCompat()) {
            helper.succeed();
            return;
        }

        var player = createPlayer(helper, "bound_sword_compat_dual_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), boundSword(), 8.0F);

        var mainSword = player.getMainHandItem();
        var offhandSword = player.getOffhandItem();
        helper.assertTrue(mainSword.is(ItemRegistry.BOUND_SWORD.get()),
                "Bound Sword should replace the mainhand item with combat compat");
        helper.assertTrue(offhandSword.is(ItemRegistry.BOUND_SWORD.get()),
                "Bound Sword should generate an offhand sword when the physical offhand slot is empty");
        helper.assertTrue(BoundSwordItem.getInstanceId(mainSword).equals(BoundSwordItem.getInstanceId(offhandSword)),
                "Mainhand and offhand Bound Swords should share the same instance id");

        var state = Capabilities.getSpellDataOrNull(player).get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE);
        helper.assertTrue(state.isOffhandSwordGenerated(),
                "Bound Sword state should record that an offhand sword was generated");

        BoundSwordManager.deactivate(player, true);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND),
                "Bound Sword should restore the original mainhand after dual wielding");
        helper.assertTrue(player.getOffhandItem().isEmpty(),
                "Bound Sword should leave an originally empty offhand empty after deactivation");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordCombatCompatKeepsOccupiedOffhandWithoutForce(GameTestHelper helper) {
        if (!BoundSwordManager.hasDualWieldCompat()) {
            helper.succeed();
            return;
        }

        var player = createPlayer(helper, "bound_sword_occupied_offhand_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
        player.getInventory().offhand.set(0, new ItemStack(Items.SHIELD));

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), boundSword(), 8.0F);

        helper.assertTrue(player.getMainHandItem().is(ItemRegistry.BOUND_SWORD.get()),
                "Bound Sword should replace the mainhand item when offhand is occupied");
        helper.assertTrue(player.getOffhandItem().is(Items.SHIELD),
                "Bound Sword should not replace an occupied offhand without force dual wield");

        var state = Capabilities.getSpellDataOrNull(player).get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE);
        helper.assertFalse(state.isOffhandSwordGenerated(),
                "Bound Sword state should not mark offhand generation when offhand was occupied");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordForceDualWieldReplacesAndRestoresOffhand(GameTestHelper helper) {
        if (!BoundSwordManager.hasDualWieldCompat()) {
            helper.succeed();
            return;
        }

        var player = createPlayer(helper, "bound_sword_force_dual_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
        player.getInventory().offhand.set(0, new ItemStack(Items.SHIELD));

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), boundSword(), 8.0F,
                true);

        helper.assertTrue(player.getMainHandItem().is(ItemRegistry.BOUND_SWORD.get()),
                "Force dual wield should replace the mainhand item");
        helper.assertTrue(player.getOffhandItem().is(ItemRegistry.BOUND_SWORD.get()),
                "Force dual wield should replace the occupied offhand item");

        var state = Capabilities.getSpellDataOrNull(player).get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE);
        helper.assertTrue(state.isOffhandSwordGenerated() && state.getStoredOffhandStack().is(Items.SHIELD),
                "Force dual wield should store the replaced offhand item in Bound Sword state");

        BoundSwordManager.deactivate(player, true);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND),
                "Force dual wield should restore the original mainhand item");
        helper.assertTrue(player.getOffhandItem().is(Items.SHIELD),
                "Force dual wield should restore the replaced offhand item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundSwordDualWieldMovingOneSwordWithinInventoryKeepsEffect(GameTestHelper helper) {
        if (!BoundSwordManager.hasDualWieldCompat()) {
            helper.succeed();
            return;
        }

        var player = createPlayer(helper, "bound_sword_dual_loss_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), boundSword(), 8.0F);
        var movedOffhandSword = player.getOffhandItem().copy();
        player.getInventory().offhand.set(0, ItemStack.EMPTY);
        player.getInventory().items.set(10, movedOffhandSword);

        BoundSwordManager.validateActiveSwordLocation(player);

        helper.assertTrue(player.getMainHandItem().is(ItemRegistry.BOUND_SWORD.get()),
                "Moving one dual-wield Bound Sword inside the player inventory should keep the mainhand sword active");
        helper.assertTrue(player.getOffhandItem().isEmpty(),
                "Moving the generated offhand sword should leave the offhand slot empty");
        helper.assertTrue(player.getInventory().items.get(10).is(ItemRegistry.BOUND_SWORD.get()),
                "Moving one dual-wield Bound Sword inside the player inventory should keep the moved sword");
        helper.assertTrue(Capabilities.getSpellDataOrNull(player)
                        .get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE).active,
                "Moving one dual-wield Bound Sword inside the player inventory should keep the effect active");

        BoundSwordManager.deactivate(player, true);
        helper.assertTrue(player.getMainHandItem().is(Items.DIAMOND),
                "Deactivation should restore the stored mainhand item");
        helper.assertTrue(!player.getInventory().items.get(10).is(ItemRegistry.BOUND_SWORD.get()),
                "Deactivation should remove moved generated swords");
        helper.succeed();
    }
}
