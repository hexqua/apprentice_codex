package jp.aquafactory.apprenticecodex.gametest;

import io.netty.buffer.Unpooled;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntlets;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsHudState;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsRaycast;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsVisualState;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.network.packet.SyncPsychicDriveGauntletsPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class PsychicDriveGauntletsMiningGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private record Fixture(FakePlayer player, BlockPos pos, ItemStack weapon, ItemStack tool) {}

    private PsychicDriveGauntletsMiningGameTests() {}

    private static Fixture fixture(GameTestHelper helper, Block block, ItemStack tool) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(1, 18, 1), "psychic_mining");
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        player.setYRot(0);
        player.setXRot(0);
        var weapon = new ItemStack(ItemRegistry.PSYCHIC_DRIVE_GAUNTLETS.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        // runtime側の適性検査も確認するため、不正な候補を含む保存内容を直接構築する。
        CalibrationAdjustmentStorage.set(weapon, 2, 3, tool, helper.getLevel().registryAccess());
        var pos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ() + 4);
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        return new Fixture(player, pos, weapon, tool.copy());
    }

    private static PsychicDriveGauntletsArm launch(Fixture fixture, float charge) {
        var arm = new PsychicDriveGauntletsArm(true);
        arm.launch(fixture.player(), PsychicDriveGauntletsRaycast.aim(fixture.player(), 12),
                1, 0, InteractionHand.MAIN_HAND, charge);
        for (int tick = 1; tick <= 3 && arm.phase() == Phase.OUTBOUND; tick++) {
            arm.tick(fixture.player(), 1, tick);
        }
        return arm;
    }

    private static long contact(PsychicDriveGauntletsArm arm) { return arm.snapshot().impactTick(); }

    private static void ticks(PsychicDriveGauntletsArm arm, Fixture fixture, long from, long to) {
        for (long tick = from; tick <= to; tick++) arm.tick(fixture.player(), 1, tick);
    }

    private static List<ItemEntity> drops(Fixture fixture) {
        return fixture.player().level().getEntitiesOfClass(ItemEntity.class, new AABB(fixture.pos()).inflate(1));
    }

    @GameTest(template = TEMPLATE)
    public static void nonRequiredToolBlocksCanBeMinedIncludingGlassWithoutDrops(GameTestHelper helper) {
        // 素材適性がなくても通常採掘できる対象は許可し、ドロップなしのガラスも破壊する。
        for (var block : List.of(Blocks.OAK_LOG, Blocks.DIRT, Blocks.GLASS)) {
            var fixture = fixture(helper, block, new ItemStack(Items.DIAMOND_PICKAXE));
            var arm = launch(fixture, 1);
            long contact = contact(arm);
            helper.assertTrue(arm.phase() == Phase.MINING, "Blocks without a required tool must start mining");
            ticks(arm, fixture, contact + 1, contact + 11);
            helper.assertTrue(helper.getLevel().getBlockState(fixture.pos()).is(block),
                    "Non-required-tool blocks must still take twelve ticks");
            arm.tick(fixture.player(), 1, contact + 12);
            helper.assertTrue(helper.getLevel().isEmptyBlock(fixture.pos()),
                    "Logs, dirt, and glass must break after twelve ticks");
            if (block == Blocks.GLASS) {
                helper.assertTrue(drops(fixture).isEmpty(), "Glass without silk touch must break without drops");
            }
            ticks(arm, fixture, contact + 13, contact + 17);
            helper.assertTrue(fixture.player().getInventory().countItem(block.asItem()) == (block == Blocks.GLASS ? 0 : 1),
                    "Logs and dirt must be collected while glass must not be recreated");
        }
        var missingTool = fixture(helper, Blocks.DIRT, ItemStack.EMPTY);
        helper.assertTrue(launch(missingTool, 1).phase() != Phase.MINING,
                "Mining must still require an inserted tool even for dirt");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void calibrationAcceptsOneToolAcrossThreeSlotsAndPreservesEnchantments(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.PSYCHIC_DRIVE_GAUNTLETS.get());
        var item = (PsychicDriveGauntlets) stack.getItem();
        var lookup = helper.getLevel().registryAccess();
        var tool = new ItemStack(Items.DIAMOND_PICKAXE);
        tool.setDamageValue(125);
        tool.enchant(lookup.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        tool.enchant(lookup.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 3);
        helper.assertTrue(item.getCalibrationAdjustmentSlotCount(stack) == 3, "Gauntlets must have three slots");
        for (int slot = 0; slot < 3; slot++) {
            helper.assertTrue(item.trySetCalibrationAdjustment(stack, slot, tool, lookup), "An empty slot must accept a mining tool");
            helper.assertTrue(!item.trySetCalibrationAdjustment(stack, (slot + 1) % 3,
                    new ItemStack(Items.IRON_PICKAXE), lookup), "A second mining tool must be rejected across tool types");
            var restored = ItemStack.parse(lookup, stack.save(lookup)).orElseThrow();
            helper.assertTrue(ItemStack.isSameItemSameComponents(tool,
                    item.getCalibrationAdjustment(restored, slot, lookup)), "Stored tools must preserve all enchantments and damage");
            helper.assertTrue(item.trySetCalibrationAdjustment(stack, slot, ItemStack.EMPTY, lookup), "Tools must remain removable");
        }
        helper.assertTrue(!item.trySetCalibrationAdjustment(stack, 0, new ItemStack(Items.DIAMOND_AXE), lookup),
                "Items outside the mining tool tag must be rejected");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void miningBreaksExactlyTwelveTicksAfterContactAndCollectsOnlyItsDrops(GameTestHelper helper) {
        var fixture = fixture(helper, Blocks.OBSIDIAN, new ItemStack(Items.DIAMOND_PICKAXE));
        var unrelated = new ItemEntity(helper.getLevel(), fixture.pos().getX() + 0.5,
                fixture.pos().getY(), fixture.pos().getZ(), new ItemStack(Items.DIAMOND));
        helper.getLevel().addFreshEntity(unrelated);
        var arm = launch(fixture, 1);
        long contact = contact(arm);
        helper.assertTrue(arm.phase() == Phase.MINING && PsychicDriveGauntletsVisualState.drilling(arm.snapshot()),
                "A full charge must begin mining and its drilling visuals");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var packet = new SyncPsychicDriveGauntletsPacket(fixture.player().getUUID(), false, arm.snapshot(), arm.snapshot());
            SyncPsychicDriveGauntletsPacket.STREAM_CODEC.encode(buffer, packet);
            helper.assertTrue(packet.equals(SyncPsychicDriveGauntletsPacket.STREAM_CODEC.decode(buffer)),
                    "Mining phase and timing must survive synchronization");
        } finally { buffer.release(); }
        ticks(arm, fixture, contact + 1, contact + 11);
        helper.assertTrue(helper.getLevel().getBlockState(fixture.pos()).is(Blocks.OBSIDIAN), "Mining must not finish before twelve ticks");
        helper.assertTrue(PsychicDriveGauntletsHudState.sample(arm.snapshot(), 0).recovery() < 1,
                "Mining must not display a fully recovered arm");
        arm.tick(fixture.player(), 1, contact + 12);
        helper.assertTrue(helper.getLevel().isEmptyBlock(fixture.pos()) && arm.phase() == Phase.RETURNING,
                "Even obsidian must break after exactly twelve ticks");
        var mined = drops(fixture).stream().filter(entity -> entity.getItem().is(Items.OBSIDIAN)).toList();
        helper.assertTrue(mined.size() == 1 && mined.getFirst().hasPickUpDelay(), "Only one mined drop must be carried");
        arm.tick(fixture.player(), 1, contact + 13);
        helper.assertTrue(mined.getFirst().position().distanceToSqr(arm.position()) < 1.0e-6,
                "Mined drops must follow the returning arm");
        ticks(arm, fixture, contact + 14, contact + 17);
        helper.assertTrue(fixture.player().getInventory().countItem(Items.OBSIDIAN) == 1 && mined.getFirst().isRemoved(),
                "Returning drops must be picked up exactly once");
        helper.assertTrue(!unrelated.isRemoved() && fixture.player().getInventory().countItem(Items.DIAMOND) == 0,
                "Nearby pre-existing items must not be collected");
        helper.assertTrue(ItemStack.isSameItemSameComponents(fixture.tool(),
                PsychicDriveGauntlets.miningTool(fixture.weapon(), helper.getLevel().registryAccess())),
                "Mining must not alter the stored tool");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void silkTouchAndAllToolEnchantmentsReachTheNormalMiningPipeline(GameTestHelper helper) {
        var lookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var tool = new ItemStack(Items.DIAMOND_PICKAXE);
        tool.setDamageValue(125);
        tool.enchant(lookup.getOrThrow(Enchantments.SILK_TOUCH), 1);
        tool.enchant(lookup.getOrThrow(Enchantments.EFFICIENCY), 5);
        tool.enchant(lookup.getOrThrow(Enchantments.SHARPNESS), 3);
        var fixture = fixture(helper, Blocks.DIAMOND_ORE, tool);
        boolean[] observed = {false};
        Consumer<BlockDropsEvent> listener = event -> {
            if (event.getBreaker() != fixture.player()) return;
            observed[0] = true;
            helper.assertTrue(ItemStack.isSameItemSameComponents(event.getTool(), tool),
                    "All tool enchantments must reach the drop pipeline without filtering");
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            var arm = launch(fixture, 1);
            long contact = contact(arm);
            ticks(arm, fixture, contact + 1, contact + 11);
            helper.assertTrue(helper.getLevel().getBlockState(fixture.pos()).is(Blocks.DIAMOND_ORE),
                    "Efficiency must not shorten fixed mining time");
            arm.tick(fixture.player(), 1, contact + 12);
            helper.assertTrue(observed[0] && drops(fixture).stream().anyMatch(item -> item.getItem().is(Items.DIAMOND_ORE)),
                    "Silk touch must produce the ore block through normal mining");
            helper.assertTrue(fixture.player().getMainHandItem() == fixture.weapon(), "The original hand must be restored");
            arm.reset();
        } finally { NeoForge.EVENT_BUS.unregister(listener); }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fortuneUsesTheInsertedToolWithoutConsumingItsDurability(GameTestHelper helper) {
        var tool = new ItemStack(Items.DIAMOND_PICKAXE);
        tool.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.FORTUNE), 3);
        var fixture = fixture(helper, Blocks.DIAMOND_ORE, tool);
        boolean[] observed = {false};
        Consumer<BlockDropsEvent> listener = event -> {
            if (event.getBreaker() == fixture.player()) {
                observed[0] = ItemStack.isSameItemSameComponents(event.getTool(), tool);
            }
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            var arm = launch(fixture, 1);
            ticks(arm, fixture, contact(arm) + 1, contact(arm) + 12);
            helper.assertTrue(observed[0] && drops(fixture).stream().anyMatch(item -> item.getItem().is(Items.DIAMOND)),
                    "Fortune must be supplied to normal ore drops");
            helper.assertTrue(ItemStack.isSameItemSameComponents(tool,
                    PsychicDriveGauntlets.miningTool(fixture.weapon(), helper.getLevel().registryAccess())),
                    "Stored durability and enchantments must not change");
            arm.reset();
        } finally { NeoForge.EVENT_BUS.unregister(listener); }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void missingToolsWrongTiersAndWrongToolTypesNeverStartMining(GameTestHelper helper) {
        for (var tool : List.of(ItemStack.EMPTY, new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.DIAMOND_AXE))) {
            var fixture = fixture(helper, Blocks.OBSIDIAN, tool);
            var arm = launch(fixture, 1);
            helper.assertTrue(arm.phase() != Phase.MINING, "An unsuitable tool must never enter mining");
            ticks(arm, fixture, 4, 20);
            helper.assertTrue(helper.getLevel().getBlockState(fixture.pos()).is(Blocks.OBSIDIAN),
                    "Unsuitable tools must leave the block intact");
        }
        var bedrock = fixture(helper, Blocks.BEDROCK, new ItemStack(Items.NETHERITE_PICKAXE));
        helper.assertTrue(launch(bedrock, 1).phase() != Phase.MINING, "Unbreakable blocks must not start mining");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void normalAndPartialChargeDoNotMineAndRaycastSkipsTorches(GameTestHelper helper) {
        var fixture = fixture(helper, Blocks.STONE, new ItemStack(Items.DIAMOND_PICKAXE));
        for (float charge : new float[]{0, 0.5F}) {
            var arm = launch(fixture, charge);
            helper.assertTrue(arm.phase() != Phase.MINING, "Only maximum charge may mine");
            ticks(arm, fixture, 4, 20);
        }
        helper.assertTrue(helper.getLevel().getBlockState(fixture.pos()).is(Blocks.STONE), "Partial attacks must leave blocks intact");
        helper.getLevel().setBlockAndUpdate(fixture.pos().below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(fixture.pos(), Blocks.TORCH.defaultBlockState());
        var aim = PsychicDriveGauntletsRaycast.aim(fixture.player(), 12);
        helper.assertTrue(aim.target() == null || !aim.target().pos().equals(fixture.pos()),
                "Blocks without collision must not become mining targets");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void changedBlocksHeldItemsAndRangeCancelMining(GameTestHelper helper) {
        for (int condition = 0; condition < 3; condition++) {
            var fixture = fixture(helper, Blocks.STONE, new ItemStack(Items.DIAMOND_PICKAXE));
            var arm = launch(fixture, 1);
            helper.assertTrue(arm.phase() == Phase.MINING, "Valid fixture must start mining");
            if (condition == 0) helper.getLevel().setBlockAndUpdate(fixture.pos(), Blocks.DIAMOND_ORE.defaultBlockState());
            if (condition == 1) fixture.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (condition == 2) fixture.player().setPos(fixture.player().position().add(20, 0, 0));
            arm.tick(fixture.player(), 1, contact(arm) + 1);
            helper.assertTrue(arm.phase() == Phase.RETURNING && !helper.getLevel().isEmptyBlock(fixture.pos()),
                    "Changed targets, held items, and out-of-range movement must cancel without breaking");
            arm.reset();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void canceledBreakProducesNoDropsAndRestoresMainHand(GameTestHelper helper) {
        var fixture = fixture(helper, Blocks.DIAMOND_ORE, new ItemStack(Items.DIAMOND_PICKAXE));
        Consumer<BlockEvent.BreakEvent> listener = event -> {
            if (event.getPlayer() == fixture.player()) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            var arm = launch(fixture, 1);
            ticks(arm, fixture, contact(arm) + 1, contact(arm) + 12);
            helper.assertTrue(helper.getLevel().getBlockState(fixture.pos()).is(Blocks.DIAMOND_ORE) && drops(fixture).isEmpty(),
                    "Canceled breaks must keep the block and produce no phantom drops");
            helper.assertTrue(fixture.player().getMainHandItem() == fixture.weapon(), "Rejected mining must restore the main hand");
            arm.reset();
        } finally { NeoForge.EVENT_BUS.unregister(listener); }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bothArmsCannotDuplicateTheSameBlockDrops(GameTestHelper helper) {
        var fixture = fixture(helper, Blocks.OBSIDIAN, new ItemStack(Items.DIAMOND_PICKAXE));
        var first = launch(fixture, 1);
        var second = launch(fixture, 1);
        long contact = contact(first);
        ticks(first, fixture, contact + 1, contact + 12);
        ticks(second, fixture, contact + 1, contact + 12);
        helper.assertTrue(drops(fixture).stream().filter(item -> item.getItem().is(Items.OBSIDIAN)).count() == 1,
                "Two arms targeting one block must produce only one drop");
        first.reset();
        second.reset();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void offhandMiningRestoresTheMainHandAndStoredTool(GameTestHelper helper) {
        var fixture = fixture(helper, Blocks.OBSIDIAN, new ItemStack(Items.DIAMOND_PICKAXE));
        var mainHand = new ItemStack(Items.DIAMOND_SWORD);
        fixture.player().setItemInHand(InteractionHand.MAIN_HAND, mainHand);
        fixture.player().setItemInHand(InteractionHand.OFF_HAND, fixture.weapon());
        var arm = new PsychicDriveGauntletsArm(false);
        arm.launch(fixture.player(), PsychicDriveGauntletsRaycast.aim(fixture.player(), 12),
                1, 0, InteractionHand.OFF_HAND, 1);
        for (int tick = 1; tick <= 3 && arm.phase() == Phase.OUTBOUND; tick++) arm.tick(fixture.player(), 1, tick);
        helper.assertTrue(arm.phase() == Phase.MINING, "Offhand gauntlets must start mining");
        ticks(arm, fixture, contact(arm) + 1, contact(arm) + 17);
        helper.assertTrue(fixture.player().getMainHandItem() == mainHand
                        && fixture.player().getOffhandItem() == fixture.weapon(),
                "Offhand mining must restore both held items");
        helper.assertTrue(fixture.player().getInventory().countItem(Items.OBSIDIAN) == 1,
                "Offhand mining must collect its drop");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void canceledDropsAreNotRecreatedOrCarried(GameTestHelper helper) {
        var fixture = fixture(helper, Blocks.OBSIDIAN, new ItemStack(Items.DIAMOND_PICKAXE));
        Consumer<BlockDropsEvent> listener = event -> {
            if (event.getBreaker() == fixture.player()) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            var arm = launch(fixture, 1);
            ticks(arm, fixture, contact(arm) + 1, contact(arm) + 17);
            helper.assertTrue(helper.getLevel().isEmptyBlock(fixture.pos()) && drops(fixture).isEmpty()
                            && fixture.player().getInventory().countItem(Items.OBSIDIAN) == 0,
                    "Canceled drops must not be recreated during return");
            arm.reset();
        } finally { NeoForge.EVENT_BUS.unregister(listener); }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void ownerDeathReleasesCarriedDropsWithoutPickup(GameTestHelper helper) {
        var fixture = fixture(helper, Blocks.OBSIDIAN, new ItemStack(Items.DIAMOND_PICKAXE));
        var arm = launch(fixture, 1);
        long contact = contact(arm);
        ticks(arm, fixture, contact + 1, contact + 12);
        var item = drops(fixture).stream().filter(entity -> entity.getItem().is(Items.OBSIDIAN)).findFirst().orElseThrow();
        fixture.player().setHealth(0);
        arm.tick(fixture.player(), 1, contact + 13);
        helper.assertTrue(!item.isRemoved() && !item.isNoGravity()
                        && fixture.player().getInventory().countItem(Items.OBSIDIAN) == 0,
                "Owner death must release drops instead of collecting them");
        arm.reset();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void carriedDropsIgnoreBlockCollisionUntilReleased(GameTestHelper helper) {
        var fixture = fixture(helper, Blocks.OBSIDIAN, new ItemStack(Items.DIAMOND_PICKAXE));
        var arm = launch(fixture, 1);
        long contact = contact(arm);
        ticks(arm, fixture, contact + 1, contact + 12);
        var item = drops(fixture).stream().filter(entity -> entity.getItem().is(Items.OBSIDIAN)).findFirst().orElseThrow();
        var carriedPosition = item.position();
        // 帰還中のアイテムが壁に押し出されず、解除後には通常tickへ戻ることを実tickで確認する。
        helper.getLevel().setBlockAndUpdate(BlockPos.containing(carriedPosition), Blocks.STONE.defaultBlockState());
        int originalAge = item.getAge();
        helper.runAfterDelay(2, () -> {
            try {
                helper.assertTrue(!item.isRemoved() && item.position().distanceToSqr(carriedPosition) < 1.0e-6
                                && item.getAge() == originalAge,
                        "Carried items must not run physics or merge while held inside a block");
            } finally { arm.reset(); }
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(!item.isRemoved() && item.getAge() > originalAge && !item.isNoGravity(),
                        "Released items must resume normal ticking");
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEMPLATE)
    public static void fullInventoryAndResetLeaveDropsInTheWorld(GameTestHelper helper) {
        for (boolean reset : new boolean[]{false, true}) {
            var fixture = fixture(helper, Blocks.OBSIDIAN, new ItemStack(Items.DIAMOND_PICKAXE));
            for (int slot = 1; slot < 36; slot++) fixture.player().getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            var arm = launch(fixture, 1);
            long contact = contact(arm);
            ticks(arm, fixture, contact + 1, contact + 12);
            var item = drops(fixture).stream().filter(entity -> entity.getItem().is(Items.OBSIDIAN))
                    .findFirst().orElseThrow();
            if (reset) arm.reset();
            else ticks(arm, fixture, contact + 13, contact + 17);
            helper.assertTrue(!item.isRemoved() && !item.isNoGravity() && item.getItem().getCount() == 1,
                    "Full inventories and resets must release the original item without loss");
            if (!reset) helper.assertTrue(item.position().distanceToSqr(fixture.player().position()) < 1.0e-6,
                    "Overflow must remain at the owner's feet");
            helper.assertTrue(!item.hasPickUpDelay() || reset, "Overflow must be available for normal pickup");
            item.discard();
        }
        helper.succeed();
    }
}
