package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.BoundBowItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.boundbow.BoundBow;
import jp.aquafactory.apprenticecodex.spell.boundbow.BoundBowManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexBoundBowGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private ApprenticeCodexBoundBowGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void boundBowActivationStoresAndRestoresMainhand(GameTestHelper helper) {
        var player = createBoundBowTestPlayer(helper, "bound_bow_restore_test");
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
    public static void boundBowCanMoveWithinInventoryAndCursor(GameTestHelper helper) {
        var player = createBoundBowTestPlayer(helper, "bound_bow_move_test");
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

    @GameTest(template = TEMPLATE)
    public static void boundBowPowerLevelHonorsServerCap(GameTestHelper helper) {
        try (var ignored = ApprenticeCodexServerConfig.useBoundBowConfigOverrideForGameTest(4, 25.0F)) {
            helper.assertTrue(BoundBow.getPowerLevelForSpellPower(400.0F) == 4,
                    "Bound Bow Power level should be capped by server config");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundBowConsumesManaToForgeArrowWithoutAmmo(GameTestHelper helper) {
        var player = createBoundBowTestPlayer(helper, "bound_bow_forge_arrow_test");
        var magicData = resolveMagicData(helper, player);
        try (var ignored = ApprenticeCodexServerConfig.useBoundBowConfigOverrideForGameTest(6, 25.0F)) {
            magicData.setMana(40.0F);
            var bow = BoundBowItem.create(UUID.randomUUID(), 0, helper.getLevel().registryAccess());
            player.setItemInHand(InteractionHand.MAIN_HAND, bow);

            var beforeArrows = countArrows(helper, player);
            bow.getItem().releaseUsing(bow, helper.getLevel(), player, bow.getUseDuration(player) - 20);

            helper.assertTrue(magicData.getMana() == 15.0F,
                    "Bound Bow should consume configured mana when forging an arrow");
            helper.assertTrue(countArrows(helper, player) > beforeArrows,
                    "Bound Bow should fire an arrow when mana can forge one");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundBowCannotStartWithoutAmmoOrMana(GameTestHelper helper) {
        var player = createBoundBowTestPlayer(helper, "bound_bow_no_mana_test");
        var magicData = resolveMagicData(helper, player);
        try (var ignored = ApprenticeCodexServerConfig.useBoundBowConfigOverrideForGameTest(6, 25.0F)) {
            magicData.setMana(10.0F);
            var bow = BoundBowItem.create(UUID.randomUUID(), 0, helper.getLevel().registryAccess());
            player.setItemInHand(InteractionHand.MAIN_HAND, bow);

            var result = bow.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(!result.getResult().consumesAction(),
                    "Bound Bow should not start drawing without ammo or enough mana");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void boundBowUsesVanillaArrowBeforeForging(GameTestHelper helper) {
        var player = createBoundBowTestPlayer(helper, "bound_bow_vanilla_arrow_test");
        var magicData = resolveMagicData(helper, player);
        try (var ignored = ApprenticeCodexServerConfig.useBoundBowConfigOverrideForGameTest(6, 25.0F)) {
            magicData.setMana(40.0F);
            var bow = BoundBowItem.create(UUID.randomUUID(), 0, helper.getLevel().registryAccess());
            var arrows = new ItemStack(Items.ARROW, 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, bow);
            player.getInventory().items.set(1, arrows);

            bow.getItem().releaseUsing(bow, helper.getLevel(), player, bow.getUseDuration(player) - 20);

            helper.assertTrue(magicData.getMana() == 40.0F,
                    "Bound Bow should not consume mana while a vanilla arrow is available");
            helper.assertTrue(player.getInventory().items.get(1).getCount() == 1,
                    "Bound Bow should consume a vanilla arrow first");
        }
        helper.succeed();
    }

    private static int countArrows(GameTestHelper helper, FakePlayer player) {
        return helper.getLevel()
                .getEntitiesOfClass(AbstractArrow.class, new AABB(player.position(), player.position()).inflate(16.0D))
                .size();
    }

    private static FakePlayer createBoundBowTestPlayer(GameTestHelper helper, String name) {
        var player = new FakePlayer((ServerLevel) helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 2, 0)));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static MagicData resolveMagicData(GameTestHelper helper, FakePlayer player) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Bound Bow test could not resolve player magic data");
        return magicData;
    }

    private static BoundBow boundBow() {
        return (BoundBow) SpellRegistry.BOUND_BOW.get();
    }

    private static int getPowerLevel(GameTestHelper helper, ItemStack stack) {
        var power = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.POWER);
        return EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(power);
    }
}
