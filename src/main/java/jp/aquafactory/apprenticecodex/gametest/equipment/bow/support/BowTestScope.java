package jp.aquafactory.apprenticecodex.gametest.equipment.bow.support;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TraceableEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** 自分の射手・弾・召喚物だけを追跡し、非同期テストの全終了経路で解放する。 */
public final class BowTestScope implements AutoCloseable {
    private static final Map<GameTestHelper, BowTestScope> ACTIVE = new IdentityHashMap<>();
    private final GameTestHelper helper;
    private final Set<Entity> owned = new LinkedHashSet<>();
    private final List<Runnable> cleanup = new ArrayList<>();
    private final Consumer<EntityJoinLevelEvent> listener;
    private int captureDepth;
    private boolean closed;

    public static BowTestScope of(GameTestHelper helper) {
        return ACTIVE.computeIfAbsent(helper, BowTestScope::new);
    }

    private BowTestScope(GameTestHelper helper) {
        this.helper = helper;
        listener = event -> {
            if (event.getLevel() != helper.getLevel()) return;
            var entity = event.getEntity();
            if (captureDepth > 0 || entity instanceof TraceableEntity traceable && owned.contains(traceable.getOwner())) {
                owned.add(entity);
            }
        };
        NeoForge.EVENT_BUS.addListener(listener);
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

    public void run(Runnable operation) { capture(() -> { operation.run(); return null; }); }

    public void onClose(Runnable operation) { cleanup.add(operation); }

    public <T extends Entity> List<T> entities(Class<T> type) {
        return owned.stream().filter(type::isInstance).map(type::cast).filter(entity -> !entity.isRemoved()).toList();
    }

    public void assertInside(Entity entity) {
        helper.assertTrue(helper.getBounds().contains(entity.position()),
                "Owned entity must stay inside its bow fixture: " + entity.getType() + " at " + entity.position());
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
            for (var entity : List.copyOf(owned)) if (!entity.isRemoved()) entity.discard();
            ACTIVE.remove(helper);
            cleanup.clear();
            owned.clear();
        }
        if (failure instanceof RuntimeException exception) throw exception;
        if (failure instanceof Error error) throw error;
    }
}
