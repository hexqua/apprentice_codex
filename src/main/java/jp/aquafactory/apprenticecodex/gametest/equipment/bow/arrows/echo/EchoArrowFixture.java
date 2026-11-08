package jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.echo;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.support.BowTestScope;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.echoarrow.EchoArrowCoreEntity;
import jp.aquafactory.apprenticecodex.spell.echoarrow.EchoArrowEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

final class EchoArrowFixture implements AutoCloseable {
    final BowTestScope scope;
    boolean closed;
    final GameTestHelper helper;
    final FakePlayer owner;
    final List<Entity> entities = new ArrayList<>();
    final List<Shot> shots = new ArrayList<>();
    final Consumer<EntityJoinLevelEvent> listener;

    EchoArrowFixture(GameTestHelper helper) {
        this.helper = helper;
        scope = BowTestScope.of(helper);
        scope.onClose(this::close);
        owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "echo_test"));
        owner.setPos(pos(new Vec3(1, 30, 1)));
        owner.setNoGravity(true);
        add(owner);
        listener = event -> {
            if (event.getLevel() == helper.getLevel() && event.getEntity() instanceof EchoArrowEntity arrow && arrow.getOwner() == owner) {
                shots.add(new Shot(arrow, helper.getLevel().getGameTime(), arrow.position(), arrow.getDeltaMovement()));
                if (!entities.contains(arrow)) entities.add(arrow);
            }
        };
        NeoForge.EVENT_BUS.addListener(listener);
    }

    Vec3 pos(Vec3 local) {
        return helper.absoluteVec(local.add(64, 0, 64));
    }

    void setBlock(BlockPos local, Block block) {
        helper.setBlock(local.offset(64, 0, 64), block);
    }

    void add(Entity entity) {
        if (!entities.contains(entity)) entities.add(entity);
        scope.track(entity);
        scope.assertInside(entity);
        helper.assertTrue(helper.getLevel().addFreshEntity(entity), "Fixture entity must join the level: " + entity.getType());
        helper.assertTrue(helper.getLevel().getEntity(entity.getUUID()) == entity, "Fixture entity must be available by UUID: " + entity.getType());
    }

    Zombie zombie(Vec3 local) {
        var zombie = EntityType.ZOMBIE.create(helper.getLevel());
        zombie.setPos(pos(local));
        zombie.setNoAi(true);
        zombie.setNoGravity(true);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        zombie.setHealth(100);
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(0);
        add(zombie);
        return zombie;
    }

    EchoArrowEntity arrow(Vec3 origin, Vec3 direction, boolean initial, EchoArrowCoreEntity core) {
        var arrow = new EchoArrowEntity(EntityRegistry.ECHO_ARROW.get(), helper.getLevel());
        arrow.launch(owner, origin, direction, 3, initial, 2.5, core);
        add(arrow);
        return arrow;
    }

    Pair pair(Vec3 origin, Vec3 direction, int count) {
        var arrow = new EchoArrowEntity(EntityRegistry.ECHO_ARROW.get(), helper.getLevel());
        var core = new EchoArrowCoreEntity(EntityRegistry.ECHO_ARROW_CORE.get(), helper.getLevel());
        core.configure(owner, pos(origin), 3, count, arrow);
        arrow.launch(owner, pos(origin), direction, 3, true, 2.5, core);
        add(core);
        add(arrow);
        return new Pair(arrow, core);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try {
            if (listener != null) NeoForge.EVENT_BUS.unregister(listener);
            List.copyOf(entities).forEach(Entity::discard);
        } finally {
            scope.close();
        }
    }

    static void step(Entity entity) {
        BowTestScope.tickOwned(entity, true);
    }

    record Pair(EchoArrowEntity arrow, EchoArrowCoreEntity core) {
    }

    record Shot(EchoArrowEntity arrow, long time, Vec3 origin, Vec3 velocity) {
    }
}
