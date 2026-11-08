package jp.aquafactory.apprenticecodex.gametest.spell.targeting.support;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerUuidSource;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TraceableEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 照準・戦闘の自分の生成物を追跡し、成功・失敗・timeoutの全経路で解放する。
 */
public final class TargetingTestFixture implements AutoCloseable {
    public static final String AIM_TEMPLATE = "gametest/aim_queries";
    private static final Map<GameTestHelper, TargetingTestFixture> ACTIVE = new IdentityHashMap<>();
    private final GameTestHelper helper;
    private final Set<Entity> owned = new LinkedHashSet<>();
    private final List<Runnable> cleanup = new ArrayList<>();
    private final Consumer<EntityJoinLevelEvent> listener;
    private int captureDepth;
    private boolean closed;

    public static TargetingTestFixture of(GameTestHelper helper) {
        return ACTIVE.computeIfAbsent(helper, TargetingTestFixture::new);
    }

    private TargetingTestFixture(GameTestHelper helper) {
        this.helper = helper;
        listener = event -> {
            if (event.getLevel() != helper.getLevel()) return;
            var entity = event.getEntity();
            boolean traced = entity instanceof TraceableEntity traceable && owned.contains(traceable.getOwner());
            boolean matchedOwner = entity instanceof CombatOwnerUuidSource source && owned.stream().anyMatch(e -> e.getUUID().equals(source.getCombatOwnerUuid()));
            if (captureDepth > 0 || traced || matchedOwner) owned.add(entity);
        };
        NeoForge.EVENT_BUS.addListener(listener);
        helper.testInfo.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo test) {
            }

            @Override
            public void testPassed(GameTestInfo test, GameTestRunner runner) {
                close();
            }

            @Override
            public void testFailed(GameTestInfo test, GameTestRunner runner) {
                close();
            }

            @Override
            public void testAddedForRerun(GameTestInfo previous, GameTestInfo next, GameTestRunner runner) {
                close();
            }
        });
    }

    public void onClose(Runnable action) {
        cleanup.add(action);
    }

    public <T> T capture(Supplier<T> action) {
        captureDepth++;
        try {
            return action.get();
        } finally {
            captureDepth--;
        }
    }

    public void run(Runnable action) {
        capture(() -> {
            action.run();
            return null;
        });
    }

    public <T extends Entity> T track(T entity) {
        owned.add(entity);
        return entity;
    }

    public void assertInside(Entity entity) {
        var bounds = helper.getBounds();
        var box = entity.getBoundingBox();
        helper.assertTrue(bounds.minX <= box.minX && bounds.minY <= box.minY && bounds.minZ <= box.minZ
                        && bounds.maxX >= box.maxX && bounds.maxY >= box.maxY && bounds.maxZ >= box.maxZ,
                "Targeting entity bounds must stay inside the fixture: " + diagnostics(entity));
    }

    public void assertVisible(Entity entity) {
        assertInside(entity);
        helper.assertTrue(!entity.isRemoved() && helper.getLevel().hasChunkAt(entity.blockPosition())
                        && helper.getLevel().getEntity(entity.getUUID()) == entity
                        && helper.getLevel().getEntitiesOfClass(Entity.class, entity.getBoundingBox(), e -> e == entity).contains(entity),
                "Targeting entity must be registered and spatially queryable: " + diagnostics(entity));
    }

    public String diagnostics(Entity entity) {
        var level = helper.getLevel();
        return entity.getType() + "{pos=" + entity.position() + ", box=" + entity.getBoundingBox()
                + ", alive=" + entity.isAlive() + ", removed=" + entity.isRemoved() + ", spectator=" + entity.isSpectator()
                + ", pickable=" + entity.isPickable() + ", inside=" + helper.getBounds().contains(entity.position())
                + ", chunkLoaded=" + level.hasChunkAt(entity.blockPosition())
                + ", entityTicking=" + level.isPositionEntityTicking(entity.blockPosition())
                + ", uuidTracked=" + (level.getEntity(entity.getUUID()) == entity)
                + ", spatiallyQueryable=" + level.getEntitiesOfClass(Entity.class, entity.getBoundingBox(), e -> e == entity).contains(entity)
                + ", testTick=" + helper.getTick() + ", gameTime=" + level.getGameTime() + "}";
    }

    public static void stepOwned(Entity entity, boolean advanceAge) {
        var scope = ACTIVE.values().stream().filter(candidate -> candidate.owned.contains(entity)).findFirst().orElseThrow();
        scope.step(entity, advanceAge);
    }

    public void step(Entity entity, boolean advanceAge) {
        run(() -> {
            if (advanceAge) entity.tickCount++;
            entity.tick();
        });
        if (!entity.isRemoved()) assertInside(entity);
    }


    public <T extends Entity> T add(T entity) {
        track(entity);
        if (helper.getLevel().getEntity(entity.getUUID()) != entity)
            helper.assertTrue(helper.getLevel().addFreshEntity(entity), "Fixture entity must join the level: " + entity.getType());
        assertVisible(entity);
        return entity;
    }

    public <T extends Entity> List<T> entities(Class<T> type) {
        return owned.stream().filter(type::isInstance).map(type::cast).filter(e -> !e.isRemoved()).toList();
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        Throwable failure = null;
        try {
            for (int i = cleanup.size() - 1; i >= 0; i--) {
                try {
                    cleanup.get(i).run();
                } catch (RuntimeException | Error error) {
                    if (failure == null) failure = error;
                    else failure.addSuppressed(error);
                }
            }
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            for (var entity : List.copyOf(owned)) {
                if (entity instanceof FakePlayer player) {
                    player.stopUsingItem();
                    var magic = MagicData.getPlayerMagicData(player);
                    magic.resetCastingState();
                    magic.resetAdditionalCastData();
                }
                if (!entity.isRemoved()) entity.discard();
            }
            ACTIVE.remove(helper);
            owned.clear();
            cleanup.clear();
        }
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }
}
