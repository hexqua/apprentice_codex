package jp.aquafactory.apprenticecodex.gametest.spell.lunaraim;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.lunaraim.LunarAim;
import jp.aquafactory.apprenticecodex.spell.lunaraim.LunarAimArrowEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

final class LunarAimFixture implements AutoCloseable {
    final TargetingTestFixture scope;
    final GameTestHelper helper;
    final Vec3 origin;
    final FakePlayer owner;
    final LunarAim spell = (LunarAim) SpellRegistry.LUNAR_AIM.get();

    LunarAimFixture(GameTestHelper h) {
        this(h, new Vec3(32, 8, 20));
    }

    LunarAimFixture(GameTestHelper h, Vec3 relativeOrigin) {
        helper = h;
        scope = TargetingTestFixture.of(h);
        origin = h.absoluteVec(relativeOrigin);
        owner = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "lunar_test"));
        owner.setPos(origin);
        owner.setYRot(-90);
        owner.setXRot(0);
        owner.setNoGravity(true);
        add(owner);
    }

    void add(Entity entity) {
        scope.add(entity);
    }

    Zombie zombie(double x, double z) {
        return zombie(x, z, UUID.randomUUID());
    }

    Zombie zombie(double x, double z, UUID uuid) {
        var entity = EntityType.ZOMBIE.create(helper.getLevel());
        Objects.requireNonNull(entity).setUUID(uuid);
        entity.setPos(origin.add(x, 0, z));
        entity.setNoAi(true);
        entity.setNoGravity(true);
        entity.getAttribute(Attributes.ARMOR).setBaseValue(0);
        add(entity);
        return entity;
    }

    LunarAimArrowEntity arrow(Entity target, Vec3 direction) {
        var entity = new LunarAimArrowEntity(EntityRegistry.LUNAR_AIM_ARROW.get(), helper.getLevel());
        entity.launch(owner, origin.add(0, 0.9, 0), direction, 4, 2.5f, target);
        add(entity);
        return entity;
    }

    Entity selected() {
        return CombatTools.findLookCombatTarget(owner, 128, 1);
    }

    MagicData preCast() {
        var data = MagicData.getPlayerMagicData(owner);
        spell.onServerPreCast(helper.getLevel(), 1, owner, data);
        return data;
    }

    List<LunarAimArrowEntity> castArrows() {
        return scope.entities(LunarAimArrowEntity.class).stream().filter(entity -> entity.getOwner() == owner).toList();
    }

    public void close() {
        scope.close();
    }

    static final String TEMPLATE = "gametest/lunar_aim";
    static final String BATCH = "apprenticecodex.lunar_aim";

    static void step(Entity entity) {
        TargetingTestFixture.stepOwned(entity, true);
    }

    static void move(GameTestHelper helper, Entity entity, Vec3 position) {
        entity.setPos(position);
        TargetingTestFixture.of(helper).assertVisible(entity);
    }
}
