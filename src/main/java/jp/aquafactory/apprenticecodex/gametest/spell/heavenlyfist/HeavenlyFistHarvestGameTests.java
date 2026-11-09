package jp.aquafactory.apprenticecodex.gametest.spell.heavenlyfist;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.heavenlyfist.HeavenlyFistFistEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.MINING_SPELL_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class HeavenlyFistHarvestGameTests {
    private HeavenlyFistHarvestGameTests() {}

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void heavenlyFistWithCraftsmansDelightHarvestsSilkTouchedBuddingCrystal(GameTestHelper helper) {
        HeavenlyFistHarvestScenarios.heavenlyFistWithCraftsmansDelightHarvestsSilkTouchedBuddingCrystal(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void heavenlyFistWithUnresolvedOwnerLeavesBuddingCrystal(GameTestHelper helper) {
        HeavenlyFistHarvestScenarios.heavenlyFistWithUnresolvedOwnerLeavesBuddingCrystal(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void heavenlyFistInAdventureModeLeavesBuddingCrystal(GameTestHelper helper) {
        HeavenlyFistHarvestScenarios.heavenlyFistInAdventureModeLeavesBuddingCrystal(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void heavenlyFistCanceledBreakEventLeavesBuddingCrystal(GameTestHelper helper) {
        HeavenlyFistHarvestScenarios.heavenlyFistCanceledBreakEventLeavesBuddingCrystal(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void heavenlyFistWithoutCraftsmansDelightHarvestsWithoutEnchantments(GameTestHelper helper) {
        HeavenlyFistHarvestScenarios.heavenlyFistWithoutCraftsmansDelightHarvestsWithoutEnchantments(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void heavenlyFistSkipsCrystalNotGrowingFromHarvestSource(GameTestHelper helper) {
        HeavenlyFistHarvestScenarios.heavenlyFistSkipsCrystalNotGrowingFromHarvestSource(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void heavenlyFistSkipsImmatureAmethystBuds(GameTestHelper helper) {
        HeavenlyFistHarvestScenarios.heavenlyFistSkipsImmatureAmethystBuds(helper);
    }

    static BlockState matureAmethystCluster(Direction facing) {
        return Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, facing);
    }

    static void spawnHeavenlyFist(ServerLevel level, LivingEntity owner,
                                          Vec3 center, float radius) {
        var fist = new HeavenlyFistFistEntity(EntityRegistry.HEAVENLY_FIST_FIST.get(), level, owner, center, 0.0F, radius, 0);
        level.addFreshEntity(fist);
    }

    static ServerPlayer createServerEquipmentTestPlayer(GameTestHelper helper, BlockPos pos) {
        var level = helper.getLevel();
        var server = level.getServer();
        var profile = new GameProfile(UUID.randomUUID(), "heavenly_fist_server_owner_test");
        var player = new ServerPlayer(server, level, profile);
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        // placeNewPlayerはlevelのempty tick状態を変えて後続GameTestのentity tick条件へ干渉するため、
        // ServerPlayerとして必要な接続だけを構成する。
        new ServerGamePacketListenerImpl(server, connection, player);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    public static void equipRingCurio(ServerPlayer player, ItemStack ringStack) {
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for curio equip test"));
        curiosInventory.setEquippedCurio(Curios.RING_SLOT, 0, ringStack);
    }
}
