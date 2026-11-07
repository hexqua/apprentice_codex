package jp.aquafactory.apprenticecodex.gametest.spell.lunaraim;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.util.ModTags;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.lunaraim.LunarAimArrowEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.spell.lunaraim.LunarAimFixture.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class LunarAimImpactGameTests {
    private LunarAimImpactGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void impactDealsOnlyBurstDamageAndEachArrowCounts(GameTestHelper h) {
        try (var s = new LunarAimFixture(h)) {
            var direct = s.zombie(1.5, 0);
            var corner = s.zombie(3, 2);
            var outside = s.zombie(1.5, 4);
            var arrow = s.arrow(null, new Vec3(1, 0, 0));
            step(arrow);
            h.assertTrue(arrow.isBursting() && direct.getHealth() == 16, "Direct hit must deal exactly one burst worth of damage");
            h.assertTrue(corner.getHealth() == 16 && outside.getHealth() == 20, "A cube corner must be hit, while outside targets are excluded");
            h.assertTrue(s.owner.getHealth() == s.owner.getMaxHealth(), "The blast must protect its caster");
            for (int i = 0; i < 3; i++) step(s.arrow(null, new Vec3(1, 0, 0)));
            h.assertTrue(direct.getHealth() == 4, "Four separate arrows must bypass hurt immunity and each deal damage");
            for (int i = 0; i < 20; i++) step(arrow);
            h.assertTrue(arrow.isRemoved() && direct.getHealth() == 4, "The visual burst must expire without repeated damage");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void upperBodyImpactKeepsIntersectionAndBurstCenter(GameTestHelper h) {
        try (var s = new LunarAimFixture(h)) {
            var direct = s.zombie(1.5, 0);
            // 背の高いmobへの上部命中と、足元・着弾付近の巻き込み範囲を分離する。
            direct.setBoundingBox(direct.getBoundingBox().expandTowards(0, 5, 0));
            var upper = s.zombie(1.5, 2);
            move(h, upper, upper.position().add(0, 5, 0));
            var lower = s.zombie(1.5, 2);
            var arrow = s.arrow(null, new Vec3(1, 0, 0));
            arrow.setPos(s.origin.add(0, 5, 0));
            var impacts = new ArrayList<Vec3>();
            Consumer<ProjectileImpactEvent> listener = event -> {
                if (event.getProjectile() == arrow) impacts.add(event.getRayTraceResult().getLocation());
            };
            NeoForge.EVENT_BUS.addListener(listener);
            try {
                step(arrow);
                var expected = new Vec3(direct.getBoundingBox().minX - (double) 0.3F, s.origin.y + 5, s.origin.z);
                h.assertTrue(arrow.isBursting() && arrow.position().distanceTo(expected) < 1.0e-6,
                        "The burst must stay at the upper-body entry point");
                h.assertTrue(impacts.size() == 1 && impacts.getFirst().distanceTo(expected) < 1.0e-6,
                        "The impact event must receive the same restored intersection");
                h.assertTrue(upper.getHealth() == 16 && lower.getHealth() == 20,
                        "Burst damage must affect neighbors of the impact, not neighbors of the feet");
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void homingHitsBystandersBlocksAndCounterspellIsSilent(GameTestHelper h) {
        try (var s = new LunarAimFixture(h)) {
            var target = s.zombie(12, 0);
            var bystander = s.zombie(1.5, 0);
            var arrow = s.arrow(target, new Vec3(1, 0, 0));
            arrow.tickCount = 10;
            step(arrow);
            h.assertTrue(arrow.isBursting() && bystander.getHealth() == 16 && target.getHealth() == 20, "Homing must explode at the first bystander without piercing");
            bystander.discard();
            var block = BlockPos.containing(s.origin.add(1, 0, 0));
            var previous = h.getLevel().getBlockState(block);
            try {
                h.getLevel().setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
                arrow = s.arrow(target, new Vec3(1, 0, 0));
                arrow.tickCount = 10;
                step(arrow);
                h.assertTrue(arrow.isBursting(), "Homing must explode on a block");
                h.assertTrue(h.getLevel().getBlockState(block).is(Blocks.STONE), "The blast must not destroy terrain");
                arrow = s.arrow(null, new Vec3(1, 0, 0));
                arrow.setPos(Vec3.atCenterOf(block));
                step(arrow);
                h.assertTrue(arrow.isBursting(), "Spawning inside solid terrain must impact immediately");
                for (var fluid : List.of(Blocks.WATER, Blocks.LAVA)) {
                    h.getLevel().setBlockAndUpdate(block, fluid.defaultBlockState());
                    arrow = s.arrow(null, new Vec3(1, 0, 0));
                    var start = arrow.position();
                    step(arrow);
                    h.assertTrue(!arrow.isBursting() && arrow.position().distanceTo(start.add(2, 0, 0)) < 1.0e-6, "Fluids must not stop or slow flight");
                }
            } finally {
                h.getLevel().setBlockAndUpdate(block, previous);
            }
            arrow = s.arrow(target, new Vec3(1, 0, 0));
            arrow.onAntiMagic(MagicData.getPlayerMagicData(s.owner));
            step(arrow);
            h.assertTrue(arrow.isRemoved() && !arrow.isBursting() && target.getHealth() == 20, "Counterspell must discard without a burst or damage");
            h.assertTrue(EntityRegistry.LUNAR_AIM_ARROW.get().is(ModTags.GUIDING_BOLT_IMMUNE), "External Guided steering must not override flight");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void multipartSelectionAndBurstResolveParentOnlyOnce(GameTestHelper h) {
        // Epic Fight系はドラゴンのダメージ処理を変更するため、バニラの固定値比較から除外する。
        if (ModList.get().isLoaded("epicfight") || ModList.get().isLoaded("efn")) {
            h.succeed();
            return;
        }
        try (var s = new LunarAimFixture(h)) {
            var dragon = EntityType.ENDER_DRAGON.create(h.getLevel());
            Objects.requireNonNull(dragon).setPos(s.origin.add(8, 0, 8));
            dragon.setNoAi(true);
            dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
            s.add(dragon);
            // 親の箱を視線・爆発範囲から外し、複数部位だけが同じ爆発に入る配置にする。
            for (var part : dragon.getParts()) part.setPos(s.origin.add(1.5, 0, 0));
            h.assertTrue(s.selected() == dragon, "A visible multipart hitbox must select its parent");
            var arrow = s.arrow(dragon, new Vec3(1, 0, 0));
            step(arrow);
            h.assertTrue(Math.abs(arrow.getY() - (s.origin.y + 0.9)) < 1.0e-6,
                    "Multipart impacts must preserve the flight height instead of using the part or parent feet");
            // 部位軽減は一律に無効化するが、複数部位への重複適用は引き続き防ぐ。
            h.assertTrue(arrow.isBursting() && Math.abs(dragon.getHealth() - (dragon.getMaxHealth() - 4)) < 0.01,
                    "The burst must damage a multipart parent exactly once: health=" + dragon.getHealth());
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void cancelledEntityAndPhysicalBlockImpactsContinue(GameTestHelper h) {
        try (var s = new LunarAimFixture(h)) {
            var target = s.zombie(1.5, 0);
            var arrow = s.arrow(null, new Vec3(1, 0, 0));
            Consumer<ProjectileImpactEvent> listener = event -> {
                if (event.getProjectile() instanceof LunarAimArrowEntity lunar && lunar.getOwner() == s.owner)
                    event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(listener);
            var block = BlockPos.containing(s.origin.add(1, 0, 0));
            var previous = h.getLevel().getBlockState(block);
            try {
                var start = arrow.position();
                step(arrow);
                h.assertTrue(!arrow.isBursting() && target.getHealth() == 20 && arrow.position().distanceTo(start.add(2, 0, 0)) < 1.0e-6,
                        "Cancelled entity impacts must preserve movement and deal no damage");
                target.discard();
                h.getLevel().setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
                arrow = s.arrow(null, new Vec3(1, 0, 0));
                start = arrow.position();
                step(arrow);
                h.assertTrue(!arrow.isBursting() && arrow.position().distanceTo(start.add(2, 0, 0)) < 1.0e-6,
                        "Cancelled block impacts must undo physical clipping");
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
                h.getLevel().setBlockAndUpdate(block, previous);
            }
        }
        h.succeed();
    }
}
