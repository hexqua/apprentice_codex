package jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.sacred;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.support.ArrowTestPositions;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.support.BowTestScope;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.sacredarrow.SacredArrow;
import jp.aquafactory.apprenticecodex.spell.sacredarrow.SacredArrowEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class SacredArrowFixture implements AutoCloseable {
    final BowTestScope scope;
    boolean closed;
    final GameTestHelper helper;
    final Vec3 origin;
    final FakePlayer owner;
    final SacredArrow spell = (SacredArrow) SpellRegistry.SACRED_ARROW.get();
    final List<Entity> entities = new ArrayList<>();

    SacredArrowFixture(GameTestHelper h) {
        helper = h;
        scope = BowTestScope.of(h);
        scope.onClose(this::close);
        origin = ArrowTestPositions.absolute(h, new Vec3(2, 20, 2));
        owner = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "sacred_test"));
        owner.setPos(origin);
        owner.setYRot(-90);
        owner.setXRot(0);
        owner.setNoGravity(true);
        add(owner);
    }

    void add(Entity e) {
        entities.add(e);
        scope.track(e);
        scope.assertInside(e);
        helper.assertTrue(helper.getLevel().addFreshEntity(e), "Fixture entity must join the level: " + e.getType());
        helper.assertTrue(helper.getLevel().getEntity(e.getUUID()) == e, "Fixture entity must be available by UUID: " + e.getType());
    }

    Zombie zombie(double x, double z) {
        var e = EntityType.ZOMBIE.create(helper.getLevel());
        e.setPos(origin.add(x, 0, z));
        e.setNoAi(true);
        e.setNoGravity(true);
        e.getAttribute(Attributes.ARMOR).setBaseValue(0);
        add(e);
        return e;
    }

    SacredArrowEntity arrow(LivingEntity target, Vec3 direction) {
        var e = new SacredArrowEntity(EntityRegistry.SACRED_ARROW.get(), helper.getLevel());
        e.launch(owner, origin.add(0, 0.9, 0), direction, 8, 0.25f, 300, target);
        add(e);
        return e;
    }

    MagicData preCast() {
        var data = MagicData.getPlayerMagicData(owner);
        spell.onServerPreCast(helper.getLevel(), 1, owner, data);
        return data;
    }

    SacredArrowEntity castArrow() {
        return scope.entities(SacredArrowEntity.class).stream()
                .filter(e -> e.getOwner() == owner).findFirst().orElseThrow();
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

    static void step(Entity entity) {
        BowTestScope.tickOwned(entity, true);
    }
}
