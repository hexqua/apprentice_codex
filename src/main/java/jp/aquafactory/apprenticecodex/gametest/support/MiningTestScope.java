package jp.aquafactory.apprenticecodex.gametest.support;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** 同期的な採掘操作で生成した実体を追跡する。mainのMiningTestScopeから使用範囲を移植。 */
public final class MiningTestScope implements AutoCloseable {
    private final GameTestHelper helper;
    private final AABB bounds;
    private final Set<Entity> owned = new LinkedHashSet<>();
    private final List<Runnable> cleanup = new ArrayList<>();
    private final Consumer<EntityJoinLevelEvent> listener;
    private int captureDepth;
    private boolean closed;

    public MiningTestScope(GameTestHelper helper) {
        this.helper = helper;
        bounds = testInfo(helper).getStructureBounds();
        listener = event -> {
            if (event.getLevel() != helper.getLevel()) return;
            var entity = event.getEntity();
            if (captureDepth > 0 || entity instanceof TraceableEntity traceable && owned.contains(traceable.getOwner())) {
                owned.add(entity);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(listener);
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

    public <T extends Entity> List<T> entities(Class<T> type) {
        var entities = owned.stream().filter(type::isInstance).map(type::cast)
                .filter(entity -> !entity.isRemoved()).toList();
        for (var entity : entities) {
            helper.assertTrue(bounds.contains(entity.position()),
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

    private static GameTestInfo testInfo(GameTestHelper helper) {
        // 1.20.1はtestInfoを公開しないため、難読化名に依存せず型で探す。
        for (var field : GameTestHelper.class.getDeclaredFields()) {
            if (field.getType() == GameTestInfo.class) {
                try {
                    field.setAccessible(true);
                    return (GameTestInfo) field.get(helper);
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access GameTestInfo", exception);
                }
            }
        }
        throw new IllegalStateException("GameTestInfo field not found");
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
            MinecraftForge.EVENT_BUS.unregister(listener);
            for (var entity : owned) if (!entity.isRemoved()) entity.discard();
            cleanup.clear();
        }
        if (failure instanceof RuntimeException exception) throw exception;
        if (failure instanceof Error error) throw error;
    }
}
