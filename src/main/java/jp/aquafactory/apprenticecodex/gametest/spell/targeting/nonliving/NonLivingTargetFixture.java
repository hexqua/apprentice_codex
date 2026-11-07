package jp.aquafactory.apprenticecodex.gametest.spell.targeting.nonliving;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.Objects;
import java.util.UUID;

final class NonLivingTargetFixture implements AutoCloseable {
    final GameTestHelper helper;
    final Vec3 origin;
    final FakePlayer owner;
    final TargetingTestFixture scope;

    NonLivingTargetFixture(GameTestHelper helper) {
        this.helper = helper;
        scope = TargetingTestFixture.of(helper);
        origin = helper.absoluteVec(new Vec3(24, 12, 24));
        owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "crystal_spell_test"));
        owner.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        owner.setPos(origin.add(-16, 0, 0));
        owner.setNoGravity(true);
        owner.setYRot(-90);
        add(owner);
    }

    void add(Entity entity) {
        scope.add(entity);
    }

    EndCrystal crystal() {
        var crystal = Objects.requireNonNull(EntityType.END_CRYSTAL.create(helper.getLevel()));
        crystal.setPos(origin.add(4, 0, 0));
        add(crystal);
        return crystal;
    }

    Sheep sheep(Vec3 position) {
        var sheep = Objects.requireNonNull(EntityType.SHEEP.create(helper.getLevel()));
        sheep.setNoAi(true);
        sheep.setNoGravity(true);
        sheep.setPos(position);
        add(sheep);
        return sheep;
    }

    void assertDestroyed(EndCrystal crystal) {
        helper.assertTrue(crystal.isRemoved(), "Spell damage must destroy an End Crystal");
    }

    @Override
    public void close() {
        scope.close();
    }
}