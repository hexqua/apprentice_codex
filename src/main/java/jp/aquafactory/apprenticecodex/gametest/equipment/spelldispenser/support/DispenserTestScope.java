package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastHelper;
import jp.aquafactory.apprenticecodex.entity.spelldispenser.SpellDispenserAnchorEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** 自分の操作が生成したentityと詠唱資源だけを、成功・失敗・timeout時に解放する。 */
public final class DispenserTestScope implements AutoCloseable {
    private final ServerLevel level;
    private final GameTestHelper helper;
    private final Set<Entity> owned = new LinkedHashSet<>();
    private final Set<SpellDispenserCastHelper.ContinuousCastSession> sessions = new LinkedHashSet<>();
    private final List<Runnable> cleanup = new ArrayList<>();
    private final Consumer<EntityJoinLevelEvent> listener;
    private int captureDepth;
    private boolean closed;

    public DispenserTestScope(GameTestHelper helper) {
        this.helper = helper;
        level = helper.getLevel();
        listener = event -> {
            if (event.getLevel() != level) {
                return;
            }
            var entity = event.getEntity();
            if (captureDepth > 0 || entity instanceof TraceableEntity traceable && owned.contains(traceable.getOwner())) {
                owned.add(entity);
                if (entity instanceof TraceableEntity traceable && traceable.getOwner() != null
                        && level.getEntity(traceable.getOwner().getId()) != traceable.getOwner()) {
                    // world未登録のproxyも追跡し、通常tickで後から生まれる子entityを同じ発生源へ結び付ける。
                    owned.add(traceable.getOwner());
                }
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

    public <T> T capture(Supplier<T> operation) {
        ++captureDepth;
        try {
            return operation.get();
        } finally {
            --captureDepth;
        }
    }

    public void run(Runnable operation) {
        capture(() -> { operation.run(); return null; });
    }

    public SpellDispenserCastHelper.ContinuousCastStartResult start(
            Supplier<SpellDispenserCastHelper.ContinuousCastStartResult> operation
    ) {
        var result = capture(operation);
        var session = result.session();
        if (session != null && sessions.add(session)) {
            owned.add(session.proxy());
            if (session.trackedAnchor() != null) {
                owned.add(session.trackedAnchor());
            }
            onClose(() -> SpellDispenserCastHelper.finishContinuousCast(level, session, true));
        }
        return result;
    }

    public SpellDispenserBlockEntity block(SpellDispenserBlockEntity blockEntity) {
        onClose(() -> blockEntity.stopContinuousCast(true));
        return blockEntity;
    }

    public void onClose(Runnable operation) {
        cleanup.add(operation);
    }

    public <T extends Entity> List<T> entities(Class<T> type) {
        var result = owned.stream().filter(type::isInstance).map(type::cast)
                .filter(entity -> !entity.isRemoved()).toList();
        for (var entity : result) {
            helper.assertTrue(helper.getBounds().contains(entity.position()),
                    "Owned entity should remain inside its fixture: type=" + type.getSimpleName()
                            + " position=" + entity.position() + " bounds=" + helper.getBounds());
        }
        return result;
    }

    public void assertReleased(GameTestHelper helper, String context) {
        helper.assertTrue(sessions.stream().allMatch(session -> session.isFinished() && !session.magicData().isCasting()),
                context + " (unfinished owned sessions)");
        helper.assertTrue(entities(SpellDispenserAnchorEntity.class).isEmpty(), context + " (owned tracked anchors)");
        for (var entity : owned) {
            if (entity instanceof ArmorStand && !(entity instanceof SpellDispenserAnchorEntity) || entity instanceof FakePlayer) {
                helper.assertTrue(level.getEntity(entity.getId()) != entity, context + " (registered owned proxy)");
            }
            if (entity instanceof FakePlayer player) {
                var data = MagicData.getPlayerMagicData(player);
                helper.assertTrue(data != null && !data.isCasting(), context + " (owned proxy casting state)");
            }
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        Throwable failure = null;
        try {
            for (var index = cleanup.size() - 1; index >= 0; --index) {
                try {
                    run(cleanup.get(index));
                } catch (RuntimeException | Error thrown) {
                    if (failure == null) failure = thrown;
                    else failure.addSuppressed(thrown);
                }
            }
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            for (var entity : owned) {
                if (!entity.isRemoved()) entity.discard();
            }
            cleanup.clear();
        }
        if (failure instanceof RuntimeException exception) throw exception;
        if (failure instanceof Error error) throw error;
    }
}
