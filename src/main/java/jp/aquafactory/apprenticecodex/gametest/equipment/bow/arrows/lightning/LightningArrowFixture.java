package jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.lightning;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.support.ArrowTestPositions;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.support.BowTestScope;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.lightningarrow.LightningArrowEntity;
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

final class LightningArrowFixture implements AutoCloseable {
    final BowTestScope scope;
    boolean closed;
    final GameTestHelper helper;
    final List<Entity> entities = new ArrayList<>();
    final FakePlayer owner;

    LightningArrowFixture(GameTestHelper helper) {
        this.helper = helper;
        scope = BowTestScope.of(helper);
        scope.onClose(this::close);
        owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "lightning_test"));
        owner.setPos(ArrowTestPositions.absolute(helper, new Vec3(1.5, 2, 1.5)));
        add(owner);
    }

    void add(Entity entity) {
        entities.add(entity);
        scope.track(entity);
        scope.assertInside(entity);
        helper.assertTrue(helper.getLevel().addFreshEntity(entity), "Fixture entity must join the level: " + entity.getType());
        helper.assertTrue(helper.getLevel().getEntity(entity.getUUID()) == entity, "Fixture entity must be available by UUID: " + entity.getType());
    }

    Zombie zombie(Vec3 localPosition) {
        var entity = EntityType.ZOMBIE.create(helper.getLevel());
        entity.setNoAi(true);
        entity.setNoGravity(true);
        entity.getAttribute(Attributes.ARMOR).setBaseValue(0);
        entity.setPos(ArrowTestPositions.absolute(helper, localPosition));
        add(entity);
        return entity;
    }

    LightningArrowEntity arrow(Vec3 origin, Vec3 direction, double range) {
        var arrow = new LightningArrowEntity(EntityRegistry.LIGHTNING_ARROW.get(), helper.getLevel());
        arrow.launch(owner, origin, direction, range, 2);
        add(arrow);
        return arrow;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try {
            entities.forEach(Entity::discard);
        } finally {
            scope.close();
        }
    }

}
