package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.item.BoundSwordItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.boundsword.BoundSword;
import jp.aquafactory.apprenticecodex.spell.boundsword.BoundSwordManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexBoundSwordGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private ApprenticeCodexBoundSwordGameTests() {
    }

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
    public static void boundSwordInvalidInventoryMoveEndsEffect(GameTestHelper helper) {
        var player = createBoundSwordTestPlayer(helper, "bound_sword_invalid_move_test");
        var original = new ItemStack(Items.EMERALD);
        player.setItemInHand(InteractionHand.MAIN_HAND, original.copy());

        BoundSwordManager.activate(player, 1, CastSource.SPELLBOOK, resolveMagicData(helper, player), boundSword(), 6.0F);
        var sword = player.getMainHandItem().copy();
        player.getInventory().items.set(player.getInventory().selected, ItemStack.EMPTY);
        player.getInventory().items.set(10, sword);

        BoundSwordManager.validateActiveSwordLocation(player);

        helper.assertTrue(!player.getInventory().items.get(10).is(ItemRegistry.BOUND_SWORD.get()),
                "Moving Bound Sword outside hotbar/offhand should remove it");
        helper.assertTrue(player.getMainHandItem().is(Items.EMERALD),
                "Invalid Bound Sword movement should restore the stored item");
        helper.assertTrue(!Capabilities.getSpellDataOrNull(player)
                        .get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE).active,
                "Invalid Bound Sword movement should end the effect");
        helper.succeed();
    }

    private static FakePlayer createBoundSwordTestPlayer(GameTestHelper helper, String name) {
        return new FakePlayer((ServerLevel) helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
    }

    private static MagicData resolveMagicData(GameTestHelper helper, FakePlayer player) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Bound Sword test could not resolve player magic data");
        return magicData;
    }

    private static BoundSword boundSword() {
        return (BoundSword) SpellRegistry.BOUND_SWORD.get();
    }
}
