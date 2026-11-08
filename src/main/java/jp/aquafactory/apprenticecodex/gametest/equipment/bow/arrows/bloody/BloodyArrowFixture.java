package jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.bloody;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.support.ArrowTestPositions;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.support.BowTestScope;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.bloodyarrow.BloodyArrowEntity;
import jp.aquafactory.apprenticecodex.spell.bloodyarrow.BloodyArrowOrbEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class BloodyArrowFixture implements AutoCloseable {
    final BowTestScope scope;
    boolean closed;
    final GameTestHelper helper;
    final List<Entity> entities = new ArrayList<>();
    final FakePlayer owner;

    BloodyArrowFixture(GameTestHelper helper) {
        this.helper = helper;
        scope = BowTestScope.of(helper);
        scope.onClose(this::close);
        owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "blood_test"));
        owner.setPos(ArrowTestPositions.absolute(helper, new Vec3(1, 20, 1)));
        owner.setNoGravity(true);
        add(owner);
    }

    void add(Entity entity) {
        entities.add(entity);
        scope.track(entity);
        scope.assertInside(entity);
        helper.assertTrue(helper.getLevel().addFreshEntity(entity), "Fixture entity must join the level: " + entity.getType());
        helper.assertTrue(helper.getLevel().getEntity(entity.getUUID()) == entity, "Fixture entity must be available by UUID: " + entity.getType());
    }

    Zombie zombie(Vec3 local) {
        var target = EntityType.ZOMBIE.create(helper.getLevel());
        target.setNoAi(true);
        target.setNoGravity(true);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.setPos(ArrowTestPositions.absolute(helper, local));
        add(target);
        return target;
    }

    BloodyArrowEntity arrow(Vec3 origin, Vec3 direction, float damage, int count) {
        var arrow = new BloodyArrowEntity(EntityRegistry.BLOODY_ARROW.get(), helper.getLevel());
        arrow.launch(owner, origin, direction, damage, count, 2);
        add(arrow);
        return arrow;
    }

    BloodyArrowOrbEntity orb(Vec3 position, float healing) {
        var orb = new BloodyArrowOrbEntity(EntityRegistry.BLOODY_ARROW_ORB.get(), helper.getLevel());
        orb.spawn(owner, position, healing);
        orb.setDeltaMovement(Vec3.ZERO);
        orb.setNoGravity(true);
        add(orb);
        return orb;
    }

    List<BloodyArrowOrbEntity> orbs() {
        return scope.entities(BloodyArrowOrbEntity.class);
    }

    void clearOrbs() {
        orbs().forEach(Entity::discard);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try {
            clearOrbs();
            entities.forEach(Entity::discard);
        } finally {
            scope.close();
        }
    }

    static void step(Entity entity) {
        // 通常はServerLevelがtick()の前に加算する。手動tickでも同じ順序を再現する。
        BowTestScope.tickOwned(entity, true);
    }
}
