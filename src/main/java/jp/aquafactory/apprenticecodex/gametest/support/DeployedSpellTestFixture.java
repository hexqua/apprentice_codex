package jp.aquafactory.apprenticecodex.gametest.support;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerUuidSource;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 設置型魔法と所有者を同じ領域で観測し、全終了経路で自分の資源だけを解放する。
 */
public final class DeployedSpellTestFixture implements AutoCloseable {
    public static final String TEMPLATE = "gametest/deployed_spell";
    private static final Map<GameTestHelper, DeployedSpellTestFixture> ACTIVE = new IdentityHashMap<>();
    private final GameTestHelper helper;
    private final Set<Entity> owned = new LinkedHashSet<>();
    private final List<Runnable> cleanup = new ArrayList<>();
    private final Consumer<EntityJoinLevelEvent> listener;
    private int captureDepth;
    private boolean closed;

    public static DeployedSpellTestFixture of(GameTestHelper helper) {
        return ACTIVE.computeIfAbsent(helper, DeployedSpellTestFixture::new);
    }

    private DeployedSpellTestFixture(GameTestHelper helper) {
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

    // 32/33blockの境界と24blockの標的探索を、隣接テストへはみ出さず収める。
    public BlockPos local(BlockPos relative) {
        return relative.offset(36, 0, 36);
    }

    public BlockPos absolute(BlockPos relative) {
        return helper.absolutePos(local(relative));
    }

    public Vec3 absolute(Vec3 relative) {
        return helper.absoluteVec(relative.add(36, 0, 36));
    }

    public void setBlock(BlockPos relative, Block block) {
        helper.setBlock(local(relative), block);
    }

    public void setBlock(BlockPos relative, BlockState state) {
        helper.setBlock(local(relative), state);
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

    public void assertVisible(Entity entity) {
        helper.assertTrue(helper.getBounds().contains(entity.position()), "Fixture entity must be inside the structure: " + entity.getType() + " at " + entity.position());
        helper.assertTrue(helper.getLevel().hasChunkAt(entity.blockPosition()), "Fixture entity chunk must be loaded: " + entity.blockPosition());
        helper.assertTrue(helper.getLevel().getEntity(entity.getUUID()) == entity, "Fixture entity must be visible by UUID: " + entity.getType()
                        + ", removed=" + entity.isRemoved() + ", lookup=" + helper.getLevel().getEntity(entity.getUUID())
                        + ", gameTime=" + helper.getLevel().getGameTime());
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

    public FakePlayer player(BlockPos relative, String name) {
        var player = PlayerTestSupport.createEquipmentTestPlayer(helper, local(relative), name);
        player.setNoGravity(true);
        return add(player);
    }

    public <T extends Mob> T mob(EntityType<T> type, BlockPos relative) {
        var mob = type.create(helper.getLevel());
        mob.setPos(absolute(Vec3.atBottomCenterOf(relative)));
        mob.setNoAi(true);
        mob.setNoGravity(true);
        return add(mob);
    }

    public AutoTurretEntity turret(FakePlayer owner, BlockPos relative, int bullets, int manaCost) {
        var turret = new AutoTurretEntity(EntityRegistry.AUTO_TURRET.get(), helper.getLevel());
        turret.setOwner(owner);
        turret.setAnchorPos(absolute(relative));
        turret.setDamage(4);
        turret.setRestockData(bullets, manaCost);
        turret.setTurretMaxHealth(20);
        turret.setPos(absolute(Vec3.atBottomCenterOf(relative)));
        return add(turret);
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
