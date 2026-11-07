package jp.aquafactory.apprenticecodex.gametest.spell.heavenlyfist;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.support.MiningTestScope;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.heavenlyfist.HeavenlyFistFistEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;
import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class HeavenlyFistHarvestGameTests {
    private HeavenlyFistHarvestGameTests() {}

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void heavenlyFistWithCraftsmansDelightHarvestsSilkTouchedBuddingCrystal(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var scope = new MiningTestScope(helper);
            var level = helper.getLevel();
            var enchantmentLookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var silkTouch = enchantmentLookup.getOrThrow(Enchantments.SILK_TOUCH);
            var playerPos = new BlockPos(4, 2, 4);

            var player = createServerEquipmentTestPlayer(helper, scope, playerPos);
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            var heldTool = new ItemStack(Items.DIAMOND_PICKAXE);
            heldTool.enchant(silkTouch, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldTool);

            var sourcePos = helper.absolutePos(new BlockPos(5, 2, 5));
            var clusterPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);

            scope.watchDrops(clusterPos);
            spawnHeavenlyFist(scope, level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(clusterPos).isAir(),
                        "Heavenly Fist with CraftsmansDelight should harvest mature crystals growing from budding amethyst");
                helper.assertTrue(level.getBlockState(sourcePos).is(Blocks.BUDDING_AMETHYST),
                        "Heavenly Fist with CraftsmansDelight should leave budding amethyst intact");
                helper.assertTrue(!scope.drops(Blocks.AMETHYST_CLUSTER.asItem()).isEmpty(),
                        "Heavenly Fist with main-hand Silk Touch should drop the crystal block itself");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void heavenlyFistWithUnresolvedOwnerLeavesBuddingCrystal(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var scope = new MiningTestScope(helper);
            var level = helper.getLevel();
            var playerPos = new BlockPos(4, 2, 4);

            var player = scope.track(createEquipmentTestPlayer(helper, playerPos, "heavenly_fist_unresolved_owner_test"));
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var sourcePos = helper.absolutePos(new BlockPos(5, 2, 5));
            var clusterPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);

            scope.watchDrops(clusterPos);
            spawnHeavenlyFist(scope, level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(clusterPos).is(Blocks.AMETHYST_CLUSTER),
                        "Heavenly Fist should not harvest crystals when its owner cannot resolve to an online player");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void heavenlyFistInAdventureModeLeavesBuddingCrystal(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var scope = new MiningTestScope(helper);
            var level = helper.getLevel();
            var playerPos = new BlockPos(4, 2, 4);

            var player = createServerEquipmentTestPlayer(helper, scope, playerPos);
            player.gameMode.changeGameModeForPlayer(GameType.ADVENTURE);

            var sourcePos = helper.absolutePos(new BlockPos(5, 2, 5));
            var clusterPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);

            scope.watchDrops(clusterPos);
            spawnHeavenlyFist(scope, level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(clusterPos).is(Blocks.AMETHYST_CLUSTER),
                        "Heavenly Fist should respect Adventure mode block restrictions");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void heavenlyFistCanceledBreakEventLeavesBuddingCrystal(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var scope = new MiningTestScope(helper);
            var level = helper.getLevel();
            var playerPos = new BlockPos(4, 2, 4);

            var player = createServerEquipmentTestPlayer(helper, scope, playerPos);

            var sourcePos = helper.absolutePos(new BlockPos(5, 2, 5));
            var clusterPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);
            Consumer<BlockEvent.BreakEvent> cancelListener = event -> {
                if (event.getPlayer() == player && event.getPos().equals(clusterPos)) {
                    event.setCanceled(true);
                }
            };
            NeoForge.EVENT_BUS.addListener(cancelListener);
            scope.onClose(() -> NeoForge.EVENT_BUS.unregister(cancelListener));

            scope.watchDrops(clusterPos);
            spawnHeavenlyFist(scope, level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                try {
                    helper.assertTrue(level.getBlockState(clusterPos).is(Blocks.AMETHYST_CLUSTER),
                            "Heavenly Fist should respect canceled block break events");
                    helper.succeed();
                } finally {
                    NeoForge.EVENT_BUS.unregister(cancelListener);
                }
            });
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void heavenlyFistWithoutCraftsmansDelightHarvestsWithoutEnchantments(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var scope = new MiningTestScope(helper);
            var level = helper.getLevel();
            var playerPos = new BlockPos(4, 2, 4);

            var player = createServerEquipmentTestPlayer(helper, scope, playerPos);
            var enchantmentLookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var silkTouch = enchantmentLookup.getOrThrow(Enchantments.SILK_TOUCH);
            var heldTool = new ItemStack(Items.DIAMOND_PICKAXE);
            heldTool.enchant(silkTouch, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldTool);

            var sourcePos = helper.absolutePos(new BlockPos(5, 2, 5));
            var clusterPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);

            scope.watchDrops(clusterPos);
            spawnHeavenlyFist(scope, level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(clusterPos).isAir(),
                        "Heavenly Fist without CraftsmansDelight should harvest mature crystals");
                helper.assertTrue(level.getBlockState(sourcePos).is(Blocks.BUDDING_AMETHYST),
                        "Heavenly Fist without CraftsmansDelight should leave budding amethyst intact");
                helper.assertTrue(!scope.drops(Items.AMETHYST_SHARD).isEmpty(),
                        "Heavenly Fist without CraftsmansDelight should drop amethyst shards despite main-hand Silk Touch");
                helper.assertTrue(scope.drops(Blocks.AMETHYST_CLUSTER.asItem()).isEmpty(),
                        "Heavenly Fist without CraftsmansDelight should not copy main-hand Silk Touch");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void heavenlyFistSkipsCrystalNotGrowingFromHarvestSource(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var scope = new MiningTestScope(helper);
            var level = helper.getLevel();
            var playerPos = new BlockPos(4, 2, 4);

            var player = createServerEquipmentTestPlayer(helper, scope, playerPos);

            var supportPos = helper.absolutePos(new BlockPos(5, 2, 5));
            var clusterPos = supportPos.east();
            level.setBlock(supportPos, Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
            level.setBlock(clusterPos, matureAmethystCluster(Direction.EAST), 3);

            scope.watchDrops(clusterPos);
            spawnHeavenlyFist(scope, level, player, Vec3.atCenterOf(supportPos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(clusterPos).is(Blocks.AMETHYST_CLUSTER),
                        "Heavenly Fist without CraftsmansDelight should skip crystals not attached to harvest sources");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void heavenlyFistSkipsImmatureAmethystBuds(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var scope = new MiningTestScope(helper);
            var level = helper.getLevel();
            var playerPos = new BlockPos(4, 2, 4);

            var player = createServerEquipmentTestPlayer(helper, scope, playerPos);

            var sourcePos = helper.absolutePos(new BlockPos(5, 2, 5));
            var budPos = sourcePos.east();
            level.setBlock(sourcePos, Blocks.BUDDING_AMETHYST.defaultBlockState(), 3);
            level.setBlock(budPos, Blocks.LARGE_AMETHYST_BUD.defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, Direction.EAST), 3);

            scope.watchDrops(budPos);
            spawnHeavenlyFist(scope, level, player, Vec3.atCenterOf(sourcePos), 2.0F);
            helper.runAtTickTime(28, () -> {
                helper.assertTrue(level.getBlockState(budPos).is(Blocks.LARGE_AMETHYST_BUD),
                        "Heavenly Fist without CraftsmansDelight should skip immature amethyst buds");
                helper.succeed();
            });
        });
    }

    private static BlockState matureAmethystCluster(Direction facing) {
        return Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, facing);
    }

    private static void spawnHeavenlyFist(MiningTestScope scope, ServerLevel level, LivingEntity owner, Vec3 center, float radius) {
        var fist = new HeavenlyFistFistEntity(EntityRegistry.HEAVENLY_FIST_FIST.get(), level, owner, center, 0.0F, radius, 0);
        scope.run(() -> level.addFreshEntity(fist));
    }

    private static ServerPlayer createServerEquipmentTestPlayer(GameTestHelper helper, MiningTestScope scope, BlockPos pos) {
        var level = helper.getLevel();
        var server = level.getServer();
        var profile = new GameProfile(UUID.randomUUID(), "heavenly_fist_server_owner_test");
        var cookie = CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(server, level, profile, cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        scope.onClose(channel::finishAndReleaseAll);
        NetworkRegistry.configureMockConnection(connection);
        // placeNewPlayerはlevelのempty tick状態を変えて後続GameTestのentity tick条件へ干渉するため、
        // ServerPlayerとして必要な接続だけを構成する。
        new ServerGamePacketListenerImpl(server, connection, player, cookie);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return scope.track(player);
    }

    private static void equipRingCurio(ServerPlayer player, ItemStack ringStack) {
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for curio equip test"));
        curiosInventory.setEquippedCurio(Curios.RING_SLOT, 0, ringStack);
    }
}
