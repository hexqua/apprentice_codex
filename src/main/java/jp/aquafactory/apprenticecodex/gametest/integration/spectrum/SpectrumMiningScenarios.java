package jp.aquafactory.apprenticecodex.gametest.integration.spectrum;

import de.dafuqs.spectrum.api.interaction.ResonanceProcessor;
import de.dafuqs.spectrum.helpers.SpectrumEnchantmentHelper;
import de.dafuqs.spectrum.registries.SpectrumAdvancements;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;
import jp.aquafactory.apprenticecodex.gametest.support.ConnectedPlayerTestSupport;
import jp.aquafactory.apprenticecodex.gametest.support.MiningTestScope;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntlets;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsRaycast;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjackJob;
import jp.aquafactory.apprenticecodex.spell.worldflatter.WorldFlatterDrillEntity;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.CuriosApi;

final class SpectrumMiningScenarios {
    private static final ResourceLocation RESONANCE_USAGE = id("unlocks/enchantments/resonance_usage");
    private static final BlockPos PLAYER_POS = new BlockPos(4, 2, 4);
    private static final BlockPos TARGET_POS = new BlockPos(4, 3, 6);

    private SpectrumMiningScenarios() {}

    static void touchDig(GameTestHelper helper, boolean unlocked) {
        for (boolean enchanted : new boolean[]{false, true}) {
            try (var scope = new MiningTestScope(helper); var fixture = ConnectedPlayerTestSupport.createPlayer(helper)) {
                var player = preparePlayer(helper, scope, fixture.player(), unlocked);
                player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe(helper, enchanted));
                var target = placeSpawner(helper);
                var spell = new TouchDigSpell();
                var magic = MagicData.getPlayerMagicData(player);
                helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magic),
                        "Touch Dig must accept the spawner target");
                scope.run(() -> spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magic));
                assertSpawner(helper, scope, player, target, unlocked && enchanted,
                        "Touch Dig unlocked=" + unlocked + " resonance=" + enchanted);
            }
        }
    }

    static void worldFlatter(GameTestHelper helper, boolean unlocked) {
        for (boolean equipped : new boolean[]{false, true}) {
            try (var scope = new MiningTestScope(helper); var fixture = ConnectedPlayerTestSupport.createPlayer(helper)) {
                var player = preparePlayer(helper, scope, fixture.player(), unlocked);
                player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe(helper, true));
                if (equipped) equipRing(player);
                var target = placeSpawner(helper);
                var drill = scope.track(new WorldFlatterDrillEntity(EntityRegistry.WORLD_FLATTER_DRILL.get(), helper.getLevel(), player));
                drill.setPos(player.position());
                drill.setToolSpeed(30.0F);
                drill.updateOwnerTarget(helper.getLevel(), new RaycastTools.TargetResult(
                        RaycastTools.TargetType.BLOCK, Vec3.atCenterOf(target).add(0, 0, -0.5), null, target));
                for (int tick = 0; tick < 100 && !helper.getLevel().isEmptyBlock(target); tick++) {
                    scope.run(() -> drill.tickOnServer(helper.getLevel()));
                }
                assertSpawner(helper, scope, player, target, unlocked && equipped,
                        "World Flatter unlocked=" + unlocked + " ring=" + equipped);
            }
        }
    }

    static void gauntlets(GameTestHelper helper, boolean unlocked) {
        for (boolean enchanted : new boolean[]{false, true}) {
            try (var scope = new MiningTestScope(helper); var fixture = ConnectedPlayerTestSupport.createPlayer(helper)) {
                var player = preparePlayer(helper, scope, fixture.player(), unlocked);
                var tool = pickaxe(helper, enchanted);
                tool.setDamageValue(37);
                var weapon = new ItemStack(ItemRegistry.PSYCHIC_DRIVE_GAUNTLETS.get());
                var item = (PsychicDriveGauntlets) weapon.getItem();
                helper.assertTrue(item.trySetCalibrationAdjustment(weapon, 0, tool, helper.getLevel().registryAccess()),
                        "Gauntlets must accept the Resonance mining tool");
                player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
                var target = placeSpawner(helper);
                var arm = new PsychicDriveGauntletsArm(true);
                try {
                    scope.run(() -> arm.launch(player, PsychicDriveGauntletsRaycast.aim(player, 12),
                            1, 0, InteractionHand.MAIN_HAND, 1));
                    for (int tick = 1; tick <= 3 && arm.phase() == Phase.OUTBOUND; tick++) {
                        int sampleTick = tick;
                        scope.run(() -> arm.tick(player, 1, sampleTick));
                    }
                    helper.assertTrue(arm.phase() == Phase.MINING, "Gauntlets must reach the spawner and start mining");
                    long contact = arm.snapshot().impactTick();
                    for (long tick = contact + 1; tick <= contact + 17; tick++) {
                        long sampleTick = tick;
                        scope.run(() -> arm.tick(player, 1, sampleTick));
                    }
                    assertSpawner(helper, scope, player, target, unlocked && enchanted,
                            "Gauntlets unlocked=" + unlocked + " resonance=" + enchanted);
                    helper.assertTrue(player.getInventory().countItem(Items.SPAWNER) == (unlocked && enchanted ? 1 : 0),
                            "Gauntlets must deliver the recovered spawner to the owner");
                    helper.assertTrue(player.getMainHandItem() == weapon && ItemStack.isSameItemSameComponents(tool,
                                    PsychicDriveGauntlets.miningTool(weapon, helper.getLevel().registryAccess())),
                            "Mining must restore the held gauntlets and preserve the stored tool");
                } finally {
                    arm.reset();
                }
            }
        }
    }

    static void coloredTrees(GameTestHelper helper) {
        var log = BuiltInRegistries.BLOCK.get(id("blue_log"));
        var leaves = BuiltInRegistries.BLOCK.get(id("blue_leaves"));
        helper.assertTrue(log != Blocks.AIR && leaves instanceof LeavesBlock,
                "Spectrum colored tree blocks must be registered");
        var advancement = SpectrumAdvancements.REVEAL_COLORED_TREES_CMY;
        for (boolean revealed : new boolean[]{false, true}) {
            for (boolean silk : new boolean[]{false, true}) {
                try (var scope = new MiningTestScope(helper); var fixture = ConnectedPlayerTestSupport.createPlayer(helper)) {
                    var player = preparePlayer(helper, scope, fixture.player(), false);
                    setAdvancement(helper, player, advancement, revealed);
                    equipRing(player);
                    var tool = new ItemStack(Items.DIAMOND_AXE);
                    if (silk) tool.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.SILK_TOUCH), 1);
                    player.setItemInHand(InteractionHand.MAIN_HAND, tool);
                    var origin = helper.absolutePos(new BlockPos(5, 3, 6));
                    var leafPos = origin.above();
                    helper.getLevel().setBlockAndUpdate(origin, log.defaultBlockState());
                    helper.getLevel().setBlockAndUpdate(leafPos, leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false));
                    var job = new TinyLumberjackJob(origin, 1, player);
                    for (int tick = 0; tick < 10 && !job.isComplete(); tick++) scope.run(() -> job.tick(helper.getLevel()));
                    var label = "revealed=" + revealed + " silk=" + silk;
                    helper.assertTrue(job.isComplete() && helper.getLevel().isEmptyBlock(origin)
                                    && helper.getLevel().isEmptyBlock(leafPos),
                            "Tiny Lumberjack must finish harvesting colored logs and leaves: " + label);
                    helper.assertTrue(scope.count(revealed ? log.asItem() : Items.OAK_LOG) == 1
                                    && scope.count(revealed ? Items.OAK_LOG : log.asItem()) == 0,
                            "Log drops must follow the owner's revelation: " + label);
                    helper.assertTrue(scope.count(revealed ? leaves.asItem() : Items.OAK_LEAVES) == (silk ? 1 : 0)
                                    && scope.count(revealed ? Items.OAK_LEAVES : leaves.asItem()) == 0,
                            "Silk Touch leaf drops must follow the owner's revelation: " + label);
                }
            }
        }
    }

    private static ServerPlayer preparePlayer(GameTestHelper helper, MiningTestScope scope, ServerPlayer player, boolean unlocked) {
        scope.track(player);
        scope.onClose(player.getAdvancements()::stopListening);
        player.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(PLAYER_POS)));
        player.setYRot(0);
        player.setXRot(0);
        // Resonanceは共有static flagも変更するため、任意テストの失敗時も次の採掘へ持ち越さない。
        boolean previous = ResonanceProcessor.preventNextXPDrop;
        scope.onClose(() -> ResonanceProcessor.preventNextXPDrop = previous);
        setAdvancement(helper, player, RESONANCE_USAGE, unlocked);
        helper.assertTrue(SpectrumEnchantmentHelper.canEntityUse(player, "spectrum:resonance") == unlocked,
                "Resonance usage advancement must match the fixture: " + unlocked);
        return player;
    }

    static void setAdvancement(GameTestHelper helper, ServerPlayer player, ResourceLocation id, boolean completed) {
        var holder = helper.getLevel().getServer().getAdvancements().get(id);
        helper.assertTrue(holder != null, "Missing Spectrum advancement: " + id);
        var progress = player.getAdvancements().getOrStartProgress(holder);
        if (completed) {
            // 実際の進捗を付与し、可視性・使用判定のhelper自体は差し替えない。
            for (var criterion : holder.value().criteria().keySet()) player.getAdvancements().award(holder, criterion);
        }
        helper.assertTrue(progress.isDone() == completed, "Unexpected Spectrum advancement state: " + id);
    }

    private static ItemStack pickaxe(GameTestHelper helper, boolean enchanted) {
        var tool = new ItemStack(Items.DIAMOND_PICKAXE);
        if (enchanted) tool.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, id("resonance"))), 1);
        return tool;
    }

    private static void equipRing(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio(Curios.RING_SLOT, 0,
                new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
    }

    private static BlockPos placeSpawner(GameTestHelper helper) {
        var pos = helper.absolutePos(TARGET_POS);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.SPAWNER.defaultBlockState());
        var spawner = (SpawnerBlockEntity) helper.getLevel().getBlockEntity(pos);
        // 既定値と異なる保存値で、単なるスポナーアイテムの生成と内容の回収を区別する。
        var data = spawner.saveWithoutMetadata(helper.getLevel().registryAccess());
        data.putShort("SpawnCount", (short) 7);
        data.putShort("RequiredPlayerRange", (short) 11);
        spawner.loadWithComponents(data, helper.getLevel().registryAccess());
        return pos;
    }

    private static void assertSpawner(GameTestHelper helper, MiningTestScope scope, ServerPlayer player,
                                      BlockPos target, boolean recovered, String label) {
        helper.assertTrue(helper.getLevel().isEmptyBlock(target), "Mining must break the spawner: " + label);
        int actual = scope.count(Items.SPAWNER) + player.getInventory().countItem(Items.SPAWNER);
        helper.assertTrue(actual == (recovered ? 1 : 0),
                "Unexpected spawner recovery: " + label + " expected=" + (recovered ? 1 : 0) + " actual=" + actual);
        if (recovered) {
            var stack = scope.drops(Items.SPAWNER).stream().map(drop -> drop.getItem()).findFirst()
                    .orElseGet(() -> player.getInventory().items.stream().filter(item -> item.is(Items.SPAWNER)).findFirst().orElseThrow());
            var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            helper.assertTrue(data != null && data.copyTag().getShort("SpawnCount") == 7
                            && data.copyTag().getShort("RequiredPlayerRange") == 11,
                    "Recovered spawner must retain its block entity data: " + label);
        }
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("spectrum", path); }
}
