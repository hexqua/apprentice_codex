package jp.aquafactory.apprenticecodex.gametest.spell.targeting.aim;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.Arrays;
import java.util.UUID;

final class AimTestSupport {
    private AimTestSupport() {
    }

    static final String TEMPLATE = TargetingTestFixture.AIM_TEMPLATE;

    static FakePlayer player(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "aim_test"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(12.5, 4.5, 12.5)));
        player.setYRot(0);
        player.setXRot(0);
        player.setNoGravity(true);
        return TargetingTestFixture.of(helper).add(player);
    }

    static Zombie target(GameTestHelper helper, FakePlayer source, Vec3 offset) {
        return target(helper, source, offset, true);
    }

    static Zombie target(GameTestHelper helper, FakePlayer source, Vec3 offset, boolean pickable) {
        var target = new Zombie(EntityType.ZOMBIE, helper.getLevel()) {
            @Override
            public boolean isPickable() {
                return pickable;
            }
        };
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setPos(source.position().add(offset));
        return TargetingTestFixture.of(helper).add(target);
    }

    static void move(GameTestHelper helper, Entity target, Vec3 position) {
        target.setPos(position);
        TargetingTestFixture.of(helper).assertVisible(target);
    }

    static String diagnostics(GameTestHelper helper, Entity... entities) {
        return Arrays.stream(entities).map(TargetingTestFixture.of(helper)::diagnostics).toList().toString();
    }

    static void assertHit(GameTestHelper helper, RaycastTools.TargetResult result, Entity expected, String message, Entity... candidates) {
        helper.assertTrue(result.hitEntity() == expected, message + ": result=" + result + ", candidates=" + diagnostics(helper, candidates));
    }
}
