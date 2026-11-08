package jp.aquafactory.apprenticecodex.gametest.spell.higanbana;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.spell.higanbana.HiganbanaKatanaEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

import java.util.UUID;

public final class KatanaAreaHitGameTestScenarios {
    private KatanaAreaHitGameTestScenarios() {}

    public static final double POSITION_EPSILON = 1.0E-6D;

    public static FakePlayer createPlayer(GameTestHelper helper, String name, Vec3 localPosition) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        var position = helper.absoluteVec(localPosition);
        player.setPos(position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    public static Zombie createZombie(ServerLevel level, Vec3 position) {
        var zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            throw new IllegalStateException("Failed to create katana area test zombie");
        }
        zombie.setPos(position.x, position.y, position.z);
        zombie.setNoAi(true);
        level.addFreshEntity(zombie);
        return zombie;
    }

    public static void tickWeapon(ServerLevel level, HiganbanaKatanaEntity weapon, int ticks) {
        for (var tick = 0; tick < ticks && !weapon.isRemoved(); ++tick) {
            weapon.tickOnServer(level);
        }
    }
}
