package jp.aquafactory.apprenticecodex.gametest.spell.katana;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import jp.aquafactory.apprenticecodex.spell.higanbana.HiganbanaKatanaEntity;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class KatanaHitFixture implements AutoCloseable {
    static final String TEMPLATE = "gametest/katana_area";
    static final String BATCH = "apprenticecodex.katana_area_hit_isolated";
    static final double POSITION_EPSILON = 1.0E-6D;
    static final float HEALTH_EPSILON = 1.0E-4F;
    final GameTestHelper helper;
    final ServerLevel level;
    final TargetingTestFixture scope;
    private final Map<BlockPos, BlockState> previousBlocks = new HashMap<>();

    KatanaHitFixture(GameTestHelper helper) {
        this.helper = helper;
        level = helper.getLevel();
        scope = TargetingTestFixture.of(helper);
    }

    Vec3 position(Vec3 originalRelative) {
        // 遮蔽・命中の小数座標を維持し、本体の待避位置と各方向の攻撃範囲まで収める。
        return helper.absoluteVec(originalRelative.add(12, 4, 12));
    }

    FakePlayer player(String name, Vec3 originalRelative) {
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), name));
        player.setPos(position(originalRelative));
        player.setNoGravity(true);
        return add(player);
    }

    Zombie zombie(Vec3 absolutePosition) {
        var zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) throw new IllegalStateException("Failed to create katana area test zombie");
        zombie.setPos(absolutePosition);
        zombie.setNoAi(true);
        zombie.setNoGravity(true);
        return add(zombie);
    }

    <T extends Entity> T add(T entity) {
        return scope.add(entity);
    }

    void move(Entity entity, Vec3 absolutePosition) {
        entity.setPos(absolutePosition);
        scope.assertVisible(entity);
    }

    void movePart(Entity part, Vec3 absolutePosition) {
        // 部位の空間検索は登録済み本体を経由するため、部位へUUID検索を要求しない。
        part.setPos(absolutePosition);
        scope.assertInside(part);
    }

    void block(BlockPos originalRelative, Block block) {
        blockAbsolute(helper.absolutePos(originalRelative.offset(12, 4, 12)), block.defaultBlockState());
    }

    void blockAbsolute(BlockPos absolutePosition, BlockState state) {
        previousBlocks.computeIfAbsent(absolutePosition, pos -> {
            var previous = level.getBlockState(pos);
            scope.onClose(() -> level.setBlockAndUpdate(pos, previous));
            return previous;
        });
        helper.assertTrue(helper.getBounds().contains(Vec3.atCenterOf(absolutePosition)), "Katana wall must stay inside the fixture: " + absolutePosition);
        level.setBlockAndUpdate(absolutePosition, state);
    }

    RaycastTools.OrientedBoxHit singleHit(Entity source, RaycastTools.HorizontalOrientedBox attackBox) {
        var hits = RaycastTools.hitsHorizontalOrientedBox(level, source, attackBox, entity -> true);
        helper.assertTrue(hits.size() == 1, "Expected exactly one oriented-box hit: " + describe(hits));
        return hits.getFirst();
    }

    String describe(List<RaycastTools.OrientedBoxHit> hits) {
        return hits.stream().map(hit -> scope.diagnostics(hit.entity()) + ", occluded=" + hit.blockOccluded()).toList().toString();
    }


    void thinPaneWall(int centerX) {
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int y = 2; y <= 3; y++) block(new BlockPos(x, y, 2), Blocks.GLASS_PANE);
        }
    }

    void act(Runnable action) {
        scope.run(action);
        scope.entities(Entity.class).forEach(scope::assertInside);
    }

    void tick(HiganbanaKatanaEntity weapon, int ticks) {
        // 元の検証と同じserver処理を進め、world tickやtickCount更新とは区別する。
        for (int tick = 0; tick < ticks && !weapon.isRemoved(); tick++) act(() -> weapon.tickOnServer(level));
    }

    @Override
    public void close() {
        scope.close();
    }
}
