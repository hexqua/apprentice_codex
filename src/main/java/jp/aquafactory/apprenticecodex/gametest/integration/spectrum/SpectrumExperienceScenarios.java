package jp.aquafactory.apprenticecodex.gametest.integration.spectrum;

import jp.aquafactory.apprenticecodex.gametest.support.ConnectedPlayerTestSupport;
import jp.aquafactory.apprenticecodex.gametest.support.MiningTestScope;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static jp.aquafactory.apprenticecodex.enchantment.Enchantments.WISDOM;

final class SpectrumExperienceScenarios {
    // 基礎XPは触媒・クリーパーとも5。Exuberanceで切捨ててからWisdomで切上げる境界を含む。
    private static final List<ExperienceCase> CASES = List.of(
            new ExperienceCase(0, 0, 5),
            new ExperienceCase(1, 0, 7),
            new ExperienceCase(0, 2, 6),
            new ExperienceCase(1, 2, 8),
            new ExperienceCase(2, 2, 11),
            new ExperienceCase(5, 2, 19)
    );

    private SpectrumExperienceScenarios() {}

    static void blockExperience(GameTestHelper helper) {
        for (var test : CASES) {
            try (var scope = new MiningTestScope(helper); var fixture = ConnectedPlayerTestSupport.createPlayer(helper)) {
                var player = prepare(helper, scope, fixture.player(), test, false);
                var pos = helper.absolutePos(new BlockPos(4, 3, 6));
                helper.getLevel().setBlockAndUpdate(pos, Blocks.SCULK_CATALYST.defaultBlockState());
                helper.assertTrue(scope.capture(() -> player.gameMode.destroyBlock(pos)),
                        "Player mining must destroy the catalyst: " + test);
                helper.assertTrue(helper.getLevel().isEmptyBlock(pos), "Catalyst must be removed after mining");
                assertExperience(helper, scope, test, "block");
            }
        }
    }

    static void mobExperience(GameTestHelper helper) {
        for (var test : CASES) {
            try (var scope = new MiningTestScope(helper); var fixture = ConnectedPlayerTestSupport.createPlayer(helper)) {
                var player = prepare(helper, scope, fixture.player(), test, true);
                var target = scope.capture(() -> helper.spawn(EntityType.CREEPER, new BlockPos(4, 3, 6)));
                target.setNoAi(true);
                scope.run(() -> target.hurt(player.damageSources().playerAttack(player), 1000));
                helper.assertTrue(target.isDeadOrDying(), "Player damage must kill the creeper: " + test);
                // 死亡時の実処理を進め、イベントだけでなく最終的なXP排出まで確認する。
                for (int tick = 0; tick < 25 && !target.isRemoved(); tick++) scope.run(target::tick);
                assertExperience(helper, scope, test, "mob");
            }
        }
    }

    private static ServerPlayer prepare(GameTestHelper helper, MiningTestScope scope, ServerPlayer player,
                                        ExperienceCase test, boolean combat) {
        scope.track(player);
        scope.onClose(player.getAdvancements()::stopListening);
        player.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(4, 2, 4))));
        SpectrumMiningScenarios.setAdvancement(helper, player, ResourceLocation.fromNamespaceAndPath("spectrum",
                "unlocks/enchantments/exuberance"), true);
        var lookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var tool = new ItemStack(combat ? Items.DIAMOND_SWORD : Items.DIAMOND_PICKAXE);
        if (test.exuberance() > 0) tool.enchant(lookup.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath("spectrum", "exuberance"))), test.exuberance());
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        if (test.wisdom() > 0) {
            // 正規の装備場所を使い、採掘ツールへ非対応のWisdomを強制付与しない。
            var robe = new ItemStack(ItemRegistry.SOULCOLLECTOR_ROBE.get());
            robe.enchant(lookup.getOrThrow(WISDOM), test.wisdom());
            player.setItemSlot(EquipmentSlot.CHEST, robe);
        }
        return player;
    }

    private static void assertExperience(GameTestHelper helper, MiningTestScope scope, ExperienceCase test, String source) {
        int actual = scope.entities(ExperienceOrb.class).stream()
                .filter(orb -> helper.getLevel().getEntity(orb.getUUID()) == orb)
                // award時点で同額orbが合流する場合もあるため、実体数ではなく保存された個数も数える。
                .mapToInt(orb -> orb.getValue() * orb.saveWithoutId(new CompoundTag()).getInt("Count")).sum();
        helper.assertTrue(actual == test.expected(), "Unexpected " + source + " XP: " + test + " actual=" + actual);
    }

    private record ExperienceCase(int exuberance, int wisdom, int expected) {}
}
