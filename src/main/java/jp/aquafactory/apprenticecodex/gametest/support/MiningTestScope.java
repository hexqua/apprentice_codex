package jp.aquafactory.apprenticecodex.gametest.support;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** 採掘操作で生成した実体と、限定した破壊地点の後続dropを追跡する。 */
public final class MiningTestScope implements AutoCloseable {
    private final GameTestHelper helper;
    private final Set<Entity> owned = new LinkedHashSet<>();
    private final Set<BlockPos> dropSources = new LinkedHashSet<>();
    private final List<Runnable> cleanup = new ArrayList<>();
    private final Consumer<EntityJoinLevelEvent> listener;
    private final Consumer<BlockDropsEvent> dropListener;
    private int captureDepth;
    private boolean closed;

    public MiningTestScope(GameTestHelper helper) {
        this.helper = helper;
        listener = event -> {
            if (event.getLevel() != helper.getLevel()) return;
            var entity = event.getEntity();
            if (captureDepth > 0 || entity instanceof TraceableEntity traceable && owned.contains(traceable.getOwner())) {
                owned.add(entity);
            }
        };
        NeoForge.EVENT_BUS.addListener(listener);
        dropListener = event -> {
            if (event.getLevel() == helper.getLevel() && owned.contains(event.getBreaker()) && dropSources.contains(event.getPos())) {
                owned.addAll(event.getDrops());
            }
        };
        // Malumが運搬用に一時保持するdropもUUIDで追跡するが、worldへの排出前は成功条件へ数えない。
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, BlockDropsEvent.class, dropListener);
        helper.testInfo.addListener(new GameTestListener() {
            @Override public void testStructureLoaded(GameTestInfo test) {}
            @Override public void testPassed(GameTestInfo test, GameTestRunner runner) { close(); }
            @Override public void testFailed(GameTestInfo test, GameTestRunner runner) { close(); }
            @Override public void testAddedForRerun(GameTestInfo previous, GameTestInfo next, GameTestRunner runner) { close(); }
        });
    }

    public <T extends Entity> T track(T entity) {
        owned.add(entity);
        return entity;
    }

    public <T> T capture(Supplier<T> operation) {
        ++captureDepth;
        try { return operation.get(); }
        finally { --captureDepth; }
    }

    public void run(Runnable operation) {
        capture(() -> { operation.run(); return null; });
    }

    public void onClose(Runnable operation) { cleanup.add(operation); }

    public void watchDrops(BlockPos source) {
        var box = new AABB(source).inflate(0.25D);
        helper.assertTrue(helper.getBounds().contains(box.minX, box.minY, box.minZ)
                        && helper.getBounds().contains(box.maxX, box.maxY, box.maxZ),
                "Drop source must fit inside the mining fixture: " + source);
        dropSources.add(source.immutable());
    }

    public <T extends Entity> List<T> entities(Class<T> type) {
        var entities = owned.stream().filter(type::isInstance).map(type::cast)
                .filter(entity -> !entity.isRemoved()).toList();
        for (var entity : entities) {
            helper.assertTrue(helper.getBounds().contains(entity.position()),
                    "Owned entity escaped the mining fixture: " + entity.getType() + " at " + entity.position());
        }
        return entities;
    }

    public List<ItemEntity> drops(Item item) {
        return entities(ItemEntity.class).stream().filter(entity -> entity.getItem().is(item)
                && helper.getLevel().getEntity(entity.getUUID()) == entity).toList();
    }

    public int count(Item item) {
        return drops(item).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    public String diagnostics() {
        return owned.stream().map(entity -> entity.getUUID() + ":" + entity.position()
                + ":removed=" + entity.isRemoved() + ":ticks=" + entity.tickCount).toList().toString();
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        Throwable failure = null;
        try {
            for (var i = cleanup.size() - 1; i >= 0; --i) {
                try { cleanup.get(i).run(); }
                catch (RuntimeException | Error thrown) {
                    if (failure == null) failure = thrown;
                    else failure.addSuppressed(thrown);
                }
            }
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            NeoForge.EVENT_BUS.unregister(dropListener);
            for (var entity : owned) if (!entity.isRemoved()) entity.discard();
            cleanup.clear();
            dropSources.clear();
        }
        if (failure instanceof RuntimeException exception) throw exception;
        if (failure instanceof Error error) throw error;
    }
}
