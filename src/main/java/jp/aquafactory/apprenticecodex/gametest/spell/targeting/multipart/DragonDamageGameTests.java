package jp.aquafactory.apprenticecodex.gametest.spell.targeting.multipart;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.spell.targeting.multipart.DragonTargetFixture.assertDragonDamage;
import static jp.aquafactory.apprenticecodex.gametest.spell.targeting.multipart.DragonTargetFixture.skipDragonTest;

@SuppressWarnings("unused")
@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class DragonDamageGameTests {
    private DragonDamageGameTests() {}

    static final String TEMPLATE = "gametest/basic_floor";

    static int sceneSequence;

    @GameTest(template = TEMPLATE)
    public static void spellDamageUsesHeadDamageForEveryPartAndParent(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        try (var scene = new Scene(helper)) {
            assertDragonDamage(helper, scene, true);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unscaledDamageUsesHeadDamageForEveryPartAndParent(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        try (var scene = new Scene(helper)) {
            assertDragonDamage(helper, scene, false);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void partDamageUsesParentResistanceAndProtection(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        try (var scene = new Scene(helper)) {
            var dragon = scene.dragon();
            Objects.requireNonNull(dragon.getAttribute(AttributeRegistry.SPELL_RESIST.get())).setBaseValue(2);
            var source = helper.getLevel().damageSources().playerAttack(scene.owner);
            CombatTools.applyDamage(dragon.getSubEntities()[2], 20, source, null, CombatTools.KnockbackTypes.DEFAULT);
            assertDamage(helper, dragon, 20 * (2 - (float) Utils.softCapFormula(2)));

            var unscaled = scene.dragon();
            Objects.requireNonNull(unscaled.getAttribute(AttributeRegistry.SPELL_RESIST.get())).setBaseValue(2);
            CombatTools.applyUnscaledDamage(unscaled, 20, source, CombatTools.KnockbackTypes.DEFAULT);
            assertDamage(helper, unscaled, 20);

            var ally = scene.dragon();
            var board = helper.getLevel().getScoreboard();
            var team = board.addPlayerTeam("part_" + UUID.randomUUID().toString().substring(0, 8));
            team.setAllowFriendlyFire(false);
            try {
                board.addPlayerToTeam(scene.owner.getScoreboardName(), team);
                board.addPlayerToTeam(ally.getScoreboardName(), team);
                helper.assertFalse(CombatTools.applyDamage(ally.getSubEntities()[6], 20, source, null,
                        CombatTools.KnockbackTypes.DEFAULT), "Spell damage must protect the parent team");
                helper.assertFalse(CombatTools.applyUnscaledDamage(ally, 20, source,
                        CombatTools.KnockbackTypes.DEFAULT), "Unscaled damage must protect the parent team");
                assertDamage(helper, ally, 0);
            } finally {
                board.removePlayerTeam(team);
            }
            var selfSource = helper.getLevel().damageSources().mobAttack(ally);
            helper.assertFalse(CombatTools.applyDamage(ally.head, 20, selfSource, null,
                    CombatTools.KnockbackTypes.DEFAULT), "A parent must be protected from its own part hit");
            helper.assertFalse(CombatTools.applyUnscaledDamage(ally.head, 20, selfSource,
                    CombatTools.KnockbackTypes.DEFAULT), "Unscaled self damage must resolve the parent");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void ordinaryLivingTargetsAndCrystalsStillTakeDamage(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var source = helper.getLevel().damageSources().playerAttack(scene.owner);
            for (boolean spell : new boolean[]{true, false}) {
                var target = Objects.requireNonNull(EntityType.HUSK.create(helper.getLevel()));
                target.setPos(scene.origin.add(3, 0, 3));
                target.setNoAi(true);
                Objects.requireNonNull(target.getAttribute(Attributes.ARMOR)).setBaseValue(0);
                scene.add(target);
                float before = target.getHealth();
                boolean applied = spell
                        ? CombatTools.applyDamage(target, 4, source, null, CombatTools.KnockbackTypes.NO_KNOCKBACK)
                        : CombatTools.applyUnscaledDamage(target, 4, source, CombatTools.KnockbackTypes.NO_KNOCKBACK);
                helper.assertTrue(applied && Math.abs(before - target.getHealth() - 4) < 0.01F,
                        "Ordinary living targets must retain damage behavior");
                var crystal = Objects.requireNonNull(EntityType.END_CRYSTAL.create(helper.getLevel()));
                crystal.setPos(scene.origin.add(0, 30, 0));
                scene.add(crystal);
                applied = spell
                        ? CombatTools.applyDamage(crystal, 4, source, null, CombatTools.KnockbackTypes.DEFAULT)
                        : CombatTools.applyUnscaledDamage(crystal, 4, source, CombatTools.KnockbackTypes.DEFAULT);
                helper.assertTrue(applied && crystal.isRemoved(), "End crystals must remain damageable");
            }
        }
        helper.succeed();
    }

    static void assertDamage(GameTestHelper helper, EnderDragon dragon, float expected) {
        float actual = dragon.getMaxHealth() - dragon.getHealth();
        helper.assertTrue(Math.abs(actual - expected) < 0.01F,
                "Unexpected dragon damage: expected=" + expected + ", actual=" + actual);
    }

    static final class Scene implements AutoCloseable {
        final GameTestHelper helper;
        final Vec3 origin;
        final FakePlayer owner;
        final List<Entity> entities = new ArrayList<>();

        Scene(GameTestHelper helper) {
            this.helper = helper;
            // 同時実行される隣のテストの大型部位をレイや接触判定に拾わないよう、高度を分離する。
            origin = helper.absoluteVec(new Vec3(2.5, 260 + 16 * sceneSequence++, 2.5));
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
            entities.add(entity);
            helper.getLevel().addFreshEntity(entity);
        }

        EnderDragon dragon() {
            var dragon = Objects.requireNonNull(EntityType.ENDER_DRAGON.create(helper.getLevel()));
            // 本体が管理範囲外の chunk に入ると部位が検索へ登録されないため、水平位置は template 内に保つ。
            // 本体と未露出部位の大きなAABBは、高さだけで攻撃経路から離す。
            var parkedPos = origin.add(0, 20, 0);
            dragon.setPos(parkedPos);
            dragon.setNoAi(true);
            dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
            Objects.requireNonNull(dragon.getAttribute(AttributeRegistry.SPELL_RESIST.get())).setBaseValue(1);
            add(dragon);
            for (var part : dragon.getSubEntities()) part.setPos(parkedPos);
            return dragon;
        }

        void exposePart(Entity part, Vec3 offset) {
            part.setPos(origin.add(offset));
        }

        @Override
        public void close() {
            entities.forEach(Entity::discard);
        }
    }
}
