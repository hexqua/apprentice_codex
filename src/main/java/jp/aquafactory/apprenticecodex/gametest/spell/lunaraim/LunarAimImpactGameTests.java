package jp.aquafactory.apprenticecodex.gametest.spell.lunaraim;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.util.ModTags;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.lunaraim.LunarAim;
import jp.aquafactory.apprenticecodex.spell.lunaraim.LunarAimArrowEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class LunarAimImpactGameTests {
    private LunarAimImpactGameTests() {}

    static final String TEMPLATE = "gametest/basic_floor";

    static final String BATCH = "apprenticecodex.lunar_aim";

    static int sceneSequence;

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void impactDealsOnlyBurstDamageAndEachArrowCounts(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var direct = s.zombie(1.5, 0);
            var corner = s.zombie(3, 2);
            var outside = s.zombie(1.5, 4);
            var arrow = s.arrow(null, new Vec3(1, 0, 0)); step(arrow);
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
        try (var s = new Scene(h)) {
            var direct = s.zombie(1.5, 0);
            // 背の高いmobへの上部命中と、足元・着弾付近の巻き込み範囲を分離する。
            direct.setBoundingBox(direct.getBoundingBox().expandTowards(0, 5, 0));
            var upper = s.zombie(1.5, 2);
            upper.setPos(upper.position().add(0, 5, 0));
            var lower = s.zombie(1.5, 2);
            var arrow = s.arrow(null, new Vec3(1, 0, 0));
            arrow.setPos(s.origin.add(0, 5, 0));
            var impacts = new ArrayList<Vec3>();
            Consumer<ProjectileImpactEvent> listener = event -> {
                if (event.getProjectile() == arrow) impacts.add(event.getRayTraceResult().getLocation());
            };
            MinecraftForge.EVENT_BUS.addListener(listener);
            try {
                step(arrow);
                var expected = new Vec3(direct.getBoundingBox().minX - (double) 0.3F, s.origin.y + 5, s.origin.z);
                h.assertTrue(arrow.isBursting() && arrow.position().distanceTo(expected) < 1.0e-6,
                        "The burst must stay at the upper-body entry point");
                h.assertTrue(impacts.size() == 1 && impacts.get(0).distanceTo(expected) < 1.0e-6,
                        "The impact event must receive the same restored intersection");
                h.assertTrue(upper.getHealth() == 16 && lower.getHealth() == 20,
                        "Burst damage must affect neighbors of the impact, not neighbors of the feet");
            } finally { MinecraftForge.EVENT_BUS.unregister(listener); }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void homingHitsBystandersBlocksAndCounterspellIsSilent(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(12, 0); var bystander = s.zombie(1.5, 0);
            var arrow = s.arrow(target, new Vec3(1, 0, 0)); arrow.tickCount = 10; step(arrow);
            h.assertTrue(arrow.isBursting() && bystander.getHealth() == 16 && target.getHealth() == 20, "Homing must explode at the first bystander without piercing");
            bystander.discard();
            var block = BlockPos.containing(s.origin.add(1, 0, 0));
            var previous = h.getLevel().getBlockState(block);
            try {
                h.getLevel().setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
                arrow = s.arrow(target, new Vec3(1, 0, 0)); arrow.tickCount = 10; step(arrow);
                h.assertTrue(arrow.isBursting(), "Homing must explode on a block");
                h.assertTrue(h.getLevel().getBlockState(block).is(Blocks.STONE), "The blast must not destroy terrain");
                arrow = s.arrow(null, new Vec3(1, 0, 0)); arrow.setPos(Vec3.atCenterOf(block)); step(arrow);
                h.assertTrue(arrow.isBursting(), "Spawning inside solid terrain must impact immediately");
                for (var fluid : List.of(Blocks.WATER, Blocks.LAVA)) {
                    h.getLevel().setBlockAndUpdate(block, fluid.defaultBlockState());
                    arrow = s.arrow(null, new Vec3(1, 0, 0)); var start = arrow.position(); step(arrow);
                    h.assertTrue(!arrow.isBursting() && arrow.position().distanceTo(start.add(2, 0, 0)) < 1.0e-6, "Fluids must not stop or slow flight start=" + start + ", end=" + arrow.position() + ", burst=" + arrow.isBursting() + ", fluid=" + fluid);
                }
            } finally { h.getLevel().setBlockAndUpdate(block, previous); }
            arrow = s.arrow(target, new Vec3(1, 0, 0));
            arrow.onAntiMagic(MagicData.getPlayerMagicData(s.owner)); step(arrow);
            h.assertTrue(arrow.isRemoved() && !arrow.isBursting() && target.getHealth() == 20, "Counterspell must discard without a burst or damage");
            h.assertTrue(EntityRegistry.LUNAR_AIM_ARROW.get().is(ModTags.GUIDING_BOLT_IMMUNE), "External Guided steering must not override flight");
        }
        h.succeed();
    }

    static void step(Entity entity) { entity.tickCount++; entity.tick(); }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void multipartSelectionAndBurstResolveParentOnlyOnce(GameTestHelper h) {
        // Epic Fight系はドラゴンのダメージ処理を変更するため、バニラの固定値比較から除外する。
        if (ModList.get().isLoaded("epicfight") || ModList.get().isLoaded("efn")) {
            h.succeed();
            return;
        }
        try (var s = new Scene(h)) {
            var dragon = EntityType.ENDER_DRAGON.create(h.getLevel());
            Objects.requireNonNull(dragon).setPos(s.origin.add(8, 0, 8));
            dragon.setNoAi(true);
            dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
            s.add(dragon);
            // 親の箱を視線・爆発範囲から外し、複数部位だけが同じ爆発に入る配置にする。
            for (var part : dragon.getParts()) part.setPos(s.origin.add(1.5, 0, 0));
            h.assertTrue(s.selected() == dragon, "A visible multipart hitbox must select its parent");
            var arrow = s.arrow(dragon, new Vec3(1, 0, 0)); step(arrow);
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
        try (var s = new Scene(h)) {
            var target = s.zombie(1.5, 0);
            var arrow = s.arrow(null, new Vec3(1, 0, 0));
            Consumer<ProjectileImpactEvent> listener = event -> {
                if (event.getProjectile() instanceof LunarAimArrowEntity lunar && lunar.getOwner() == s.owner) event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
            };
            MinecraftForge.EVENT_BUS.addListener(listener);
            var block = BlockPos.containing(s.origin.add(1, 0, 0));
            var previous = h.getLevel().getBlockState(block);
            try {
                var start = arrow.position(); step(arrow);
                h.assertTrue(!arrow.isBursting() && target.getHealth() == 20 && arrow.position().distanceTo(start.add(2, 0, 0)) < 1.0e-6,
                        "Cancelled entity impacts must preserve movement and deal no damage");
                target.discard();
                h.getLevel().setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
                arrow = s.arrow(null, new Vec3(1, 0, 0)); start = arrow.position(); step(arrow);
                h.assertTrue(!arrow.isBursting() && arrow.position().distanceTo(start.add(2, 0, 0)) < 1.0e-6,
                        "Cancelled block impacts must undo physical clipping");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(listener);
                h.getLevel().setBlockAndUpdate(block, previous);
            }
        }
        h.succeed();
    }

    static final class Scene implements AutoCloseable {
        final GameTestHelper helper;
        final Vec3 origin;
        final FakePlayer owner;
        final LunarAim spell = (LunarAim) SpellRegistry.LUNAR_AIM.get();
        final List<Entity> entities = new ArrayList<>();
        Scene(GameTestHelper h) {
            // Forgeの通常地形を避け、128ブロックの視線が隣のsceneを拾わない高度へ分離する。
            helper = h; origin = h.absoluteVec(new Vec3(2, 280 + 12 * (sceneSequence++ % 4), 2));
            owner = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "lunar_test"));
            owner.setPos(origin); owner.setYRot(-90); owner.setXRot(0); owner.setNoGravity(true); add(owner);
        }
        void add(Entity entity) { entities.add(entity); helper.getLevel().addFreshEntity(entity); }
        Zombie zombie(double x, double z) {
            return zombie(x, z, UUID.randomUUID());
        }
        Zombie zombie(double x, double z, UUID uuid) {
            var entity = EntityType.ZOMBIE.create(helper.getLevel());
            Objects.requireNonNull(entity).setUUID(uuid);
            entity.setPos(origin.add(x, 0, z)); entity.setNoAi(true); entity.setNoGravity(true);
            entity.getAttribute(Attributes.ARMOR).setBaseValue(0); add(entity); return entity;
        }
        LunarAimArrowEntity arrow(Entity target, Vec3 direction) {
            var entity = new LunarAimArrowEntity(EntityRegistry.LUNAR_AIM_ARROW.get(), helper.getLevel());
            entity.launch(owner, origin.add(0, 0.9, 0), direction, 4, 2.5f, target); add(entity); return entity;
        }
        Entity selected() { return CombatTools.findLookCombatTarget(owner, 128, 1); }
        MagicData preCast() {
            var data = MagicData.getPlayerMagicData(owner);
            spell.onServerPreCast(helper.getLevel(), 1, owner, data); return data;
        }
        List<LunarAimArrowEntity> castArrows() {
            return helper.getLevel().getEntitiesOfClass(LunarAimArrowEntity.class, owner.getBoundingBox().inflate(3)).stream()
                    .filter(entity -> entity.getOwner() == owner && !entity.isRemoved()).toList();
        }
        @Override public void close() {
            castArrows().forEach(Entity::discard);
            entities.forEach(Entity::discard);
        }
    }
}
