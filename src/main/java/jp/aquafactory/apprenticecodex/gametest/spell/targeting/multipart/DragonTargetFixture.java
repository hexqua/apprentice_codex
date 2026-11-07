package jp.aquafactory.apprenticecodex.gametest.spell.targeting.multipart;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.Objects;
import java.util.UUID;

final class DragonTargetFixture implements AutoCloseable {
    final GameTestHelper helper;
    final Vec3 origin;
    final FakePlayer owner;
    final TargetingTestFixture scope;

    DragonTargetFixture(GameTestHelper helper) {
        this.helper = helper;
        scope = TargetingTestFixture.of(helper);
        origin = helper.absoluteVec(new Vec3(24, 8, 24));
        owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "part_hit_test"));
        owner.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        owner.setPos(origin);
        owner.setYRot(-90);
        owner.setYHeadRot(-90);
        owner.setXRot(0);
        owner.setNoGravity(true);
        add(owner);
    }

    void add(Entity entity) {
        scope.add(entity);
    }

    EnderDragon dragon() {
        var dragon = Objects.requireNonNull(EntityType.ENDER_DRAGON.create(helper.getLevel()));
        // 本体と未露出部位は、専用領域内で攻撃経路より上へ待避する。
        var parkedPos = origin.add(0, 20, 0);
        dragon.setPos(parkedPos);
        dragon.setNoAi(true);
        dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
        Objects.requireNonNull(dragon.getAttribute(AttributeRegistry.SPELL_RESIST)).setBaseValue(1);
        add(dragon);
        for (var part : dragon.getSubEntities()) {
            part.setPos(parkedPos);
            scope.assertInside(part);
        }
        return dragon;
    }

    void exposePart(Entity part, Vec3 offset) {
        part.setPos(origin.add(offset));
        scope.assertInside(part);
    }

    @Override
    public void close() {
        scope.close();
    }

    static final String TEMPLATE = "gametest/dragon_targeting";

    static void assertDragonDamage(GameTestHelper helper, DragonTargetFixture scene, boolean spell) {
        // 比較条件ごとに別個体を使い、部位間で共有する無敵時間やフェーズ遷移を混ぜない。
        var dragon = scene.dragon();
        for (int index = 0; index <= dragon.getSubEntities().length; index++) {
            if (index > 0) dragon = scene.dragon();
            Entity target = index < dragon.getSubEntities().length ? dragon.getSubEntities()[index] : dragon;
            var source = helper.getLevel().damageSources().playerAttack(scene.owner);
            boolean applied = spell
                    ? CombatTools.applyDamage(target, 20, source, null, CombatTools.KnockbackTypes.NO_KNOCKBACK)
                    : CombatTools.applyUnscaledDamage(target, 20, source, CombatTools.KnockbackTypes.NO_KNOCKBACK);
            helper.assertTrue(applied, "Dragon damage should be accepted for target " + index);
            assertDamage(helper, dragon, 20);
        }
    }

    static boolean skipDragonTest(GameTestHelper helper) {
        // Epic Fight系はドラゴンの部位・ダメージ処理を変更するため、バニラ比較の対象外にする。
        if (ModList.get().isLoaded("epicfight") || ModList.get().isLoaded("efn")) {
            helper.succeed();
            return true;
        }
        return false;
    }

    static void assertDamage(GameTestHelper helper, EnderDragon dragon, float expected) {
        float actual = dragon.getMaxHealth() - dragon.getHealth();
        helper.assertTrue(Math.abs(actual - expected) < 0.01F,
                "Unexpected dragon damage: expected=" + expected + ", actual=" + actual);
    }
}