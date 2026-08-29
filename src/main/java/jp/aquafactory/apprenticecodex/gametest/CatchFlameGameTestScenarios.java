package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastContext;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.CatchFlameServerConfig;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastAnchorEntity;
import jp.aquafactory.apprenticecodex.spell.catchflame.CatchFlame;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.UUID;

final class CatchFlameGameTestScenarios {
    private static final float VALUE_EPSILON = 1.0E-4F;

    private CatchFlameGameTestScenarios() {
    }

    static void catchFlameDamagesAndPenetratesBurningTargetIFrames(GameTestHelper helper) {
        var caster = createPlayer(helper, "catch_flame_damage", new Vec3(1.5D, 2.0D, 1.5D));
        var target = createLiving(helper, EntityType.ZOMBIE, new Vec3(1.5D, 2.0D, 3.0D));
        aimAt(caster, target.getBoundingBox().getCenter());
        var spell = catchFlame();

        cast(helper, spell, 2, caster);
        var healthAfterFirstHit = target.getHealth();
        helper.assertTrue(healthAfterFirstHit < target.getMaxHealth(), "Catch Flame should damage its target");
        helper.assertTrue(target.getRemainingFireTicks() >= 20,
                "Catch Flame should ignite a target after successful damage");

        target.invulnerableTime = 20;
        cast(helper, spell, 2, caster);
        helper.assertTrue(target.getHealth() < healthAfterFirstHit,
                "Catch Flame should bypass invulnerability frames on a burning target");

        discardAll(target, caster);
        helper.succeed();
    }

    static void catchFlameRejectsFireImmuneTargetsIncludingEndCrystal(GameTestHelper helper) {
        var caster = createPlayer(helper, "catch_flame_fire_immune", new Vec3(1.5D, 2.0D, 1.5D));
        var blaze = createLiving(helper, EntityType.BLAZE, new Vec3(1.5D, 2.0D, 3.0D));
        var crystalPosition = helper.absoluteVec(new Vec3(1.5D, 2.0D, 3.0D));
        var crystal = new EndCrystal(helper.getLevel(), crystalPosition.x, crystalPosition.y, crystalPosition.z);
        helper.getLevel().addFreshEntity(crystal);
        aimAt(caster, blaze.getBoundingBox().getCenter());
        var blazeHealth = blaze.getHealth();

        cast(helper, catchFlame(), 2, caster);

        helper.assertTrue(Math.abs(blaze.getHealth() - blazeHealth) < VALUE_EPSILON,
                "Catch Flame should not damage a fire-immune living target");
        helper.assertTrue(blaze.getRemainingFireTicks() <= 0,
                "Catch Flame should not ignite a fire-immune living target");
        helper.assertTrue(crystal.isAlive(),
                "Catch Flame should include non-living combat targets without bypassing fire immunity");

        discardAll(crystal, blaze, caster);
        helper.succeed();
    }

    static void catchFlameAreaRejectsTargetsBehindThinCover(GameTestHelper helper) {
        var caster = createPlayer(helper, "catch_flame_cover", new Vec3(1.5D, 2.0D, 1.5D));
        var target = createLiving(helper, EntityType.ZOMBIE, new Vec3(1.5D, 2.0D, 3.2D));
        var coverPosition = helper.absolutePos(new BlockPos(1, 3, 2));
        helper.getLevel().setBlockAndUpdate(coverPosition, Blocks.GLASS_PANE.defaultBlockState());
        aimAt(caster, target.getBoundingBox().getCenter());
        var health = target.getHealth();

        cast(helper, catchFlame(), 2, caster);

        helper.assertTrue(Math.abs(target.getHealth() - health) < VALUE_EPSILON,
                "Catch Flame should not affect a target behind thin cover");
        helper.assertTrue(target.getRemainingFireTicks() <= 0,
                "Catch Flame should not ignite a target behind thin cover");

        helper.getLevel().setBlockAndUpdate(coverPosition, Blocks.AIR.defaultBlockState());
        discardAll(target, caster);
        helper.succeed();
    }

    static void catchFlameUsesSeparateDispenserAndNonPlayerIgnitionRules(GameTestHelper helper) {
        var caster = createPlayer(helper, "catch_flame_ignition_rules", new Vec3(1.5D, 2.0D, 1.5D));
        var supportPosition = helper.absolutePos(new BlockPos(1, 2, 3));
        var firePosition = supportPosition.above();
        helper.getLevel().setBlockAndUpdate(supportPosition, Blocks.STONE.defaultBlockState());
        aimAt(caster, Vec3.atBottomCenterOf(supportPosition.above()));

        try (var ignored = ApprenticeCodexServerConfig.useCatchFlameConfigOverrideForGameTest(
                new CatchFlameServerConfig.Values(true, false))) {
            cast(helper, catchFlame(), 1, caster);
            helper.assertTrue(helper.getLevel().getBlockState(firePosition).isAir(),
                    "A non-player cast should not ignite blocks when non-player ignition is disabled");

            SpellDispenserCastContext.run(() -> cast(helper, catchFlame(), 1, caster));
            helper.assertTrue(helper.getLevel().getBlockState(firePosition).is(Blocks.FIRE),
                    "A Spell Dispenser cast should ignite blocks when its dedicated rule is enabled");
        }

        helper.getLevel().setBlockAndUpdate(firePosition, Blocks.AIR.defaultBlockState());
        try (var ignored = ApprenticeCodexServerConfig.useCatchFlameConfigOverrideForGameTest(
                new CatchFlameServerConfig.Values(false, true))) {
            cast(helper, catchFlame(), 1, caster);
            helper.assertTrue(helper.getLevel().getBlockState(firePosition).is(Blocks.FIRE),
                    "A non-player cast should ignite blocks when non-player ignition is enabled");
        }

        helper.getLevel().setBlockAndUpdate(firePosition, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(supportPosition, Blocks.AIR.defaultBlockState());
        caster.discard();
        helper.succeed();
    }

    static void catchFlameRejectsIgnitionFromUnresolvedRemoteOwner(GameTestHelper helper) {
        var level = helper.getLevel();
        var anchor = new RemoteOwnerCastAnchorEntity(EntityRegistry.REMOTE_OWNER_CAST_ANCHOR.get(), level);
        var eyePosition = helper.absoluteVec(new Vec3(1.5D, 3.62D, 1.5D));
        var supportPosition = helper.absolutePos(new BlockPos(1, 2, 3));
        var targetPosition = Vec3.atBottomCenterOf(supportPosition.above());
        anchor.syncFromRemoteGeometry(eyePosition, targetPosition.subtract(eyePosition).normalize());
        level.addFreshEntity(anchor);
        level.setBlockAndUpdate(supportPosition, Blocks.STONE.defaultBlockState());

        try (var ignored = ApprenticeCodexServerConfig.useCatchFlameConfigOverrideForGameTest(
                new CatchFlameServerConfig.Values(true, true))) {
            cast(helper, catchFlame(), 1, anchor);
        }

        helper.assertTrue(level.getBlockState(supportPosition.above()).isAir(),
                "Catch Flame should reject block ignition when a RemoteOwner cannot be resolved");

        level.setBlockAndUpdate(supportPosition, Blocks.AIR.defaultBlockState());
        anchor.discard();
        helper.succeed();
    }

    static void catchFlameEssenceSmokerBranchNeverFallsBackToSurfaceFire(GameTestHelper helper) {
        var caster = createPlayer(helper, "catch_flame_smoker", new Vec3(1.5D, 2.0D, 1.5D));
        var smokerPosition = helper.absolutePos(new BlockPos(1, 2, 3));
        helper.getLevel().setBlockAndUpdate(smokerPosition, BlockRegistry.ESSENCE_SMOKER.get().defaultBlockState());
        aimAt(caster, Vec3.atCenterOf(smokerPosition));

        try (var ignored = ApprenticeCodexServerConfig.useCatchFlameConfigOverrideForGameTest(
                new CatchFlameServerConfig.Values(true, false))) {
            SpellDispenserCastContext.run(() -> cast(helper, catchFlame(), 1, caster));
        }

        helper.assertTrue(helper.getLevel().getBlockState(smokerPosition.above()).isAir(),
                "Catch Flame should not place surface fire after handling an Essence Smoker");

        helper.getLevel().setBlockAndUpdate(smokerPosition, Blocks.AIR.defaultBlockState());
        caster.discard();
        helper.succeed();
    }

    private static CatchFlame catchFlame() {
        return (CatchFlame) SpellRegistry.CATCH_FLAME.get();
    }

    private static void cast(GameTestHelper helper, CatchFlame spell, int spellLevel, LivingEntity caster) {
        spell.onCast(helper.getLevel(), spellLevel, caster, CastSource.SPELLBOOK,
                caster instanceof FakePlayer player ? MagicData.getPlayerMagicData(player) : new MagicData());
    }

    private static FakePlayer createPlayer(GameTestHelper helper, String name, Vec3 localPosition) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        var position = helper.absoluteVec(localPosition);
        player.setPos(position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static <T extends LivingEntity> T createLiving(
            GameTestHelper helper, EntityType<T> type, Vec3 localPosition
    ) {
        var entity = type.create(helper.getLevel());
        if (entity == null) {
            throw new IllegalStateException("Failed to create Catch Flame test entity");
        }
        var position = helper.absoluteVec(localPosition);
        entity.setPos(position.x, position.y, position.z);
        if (entity instanceof Zombie zombie) {
            zombie.setNoAi(true);
        } else if (entity instanceof Blaze blaze) {
            blaze.setNoAi(true);
        }
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static void aimAt(LivingEntity caster, Vec3 targetPosition) {
        caster.lookAt(EntityAnchorArgument.Anchor.EYES, targetPosition);
    }

    private static void discardAll(Entity... entities) {
        for (var entity : entities) {
            entity.discard();
        }
    }
}
