package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.block.essencesmoker.EssenceSmokerBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastContext;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.CatchFlameServerConfig;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastAnchorEntity;
import jp.aquafactory.apprenticecodex.spell.catchflame.CatchFlame;
import jp.aquafactory.apprenticecodex.spell.catchflame.CatchFlameImpactEntity;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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

    static void catchFlameRejectsFireImmuneLivingTarget(GameTestHelper helper) {
        var caster = createPlayer(helper, "catch_flame_fire_immune", new Vec3(1.5D, 2.0D, 1.5D));
        var blaze = createLiving(helper, EntityType.BLAZE, new Vec3(1.5D, 2.0D, 3.0D));
        aimAt(caster, blaze.getBoundingBox().getCenter());
        var blazeHealth = blaze.getHealth();

        cast(helper, catchFlame(), 2, caster);

        helper.assertTrue(Math.abs(blaze.getHealth() - blazeHealth) < VALUE_EPSILON,
                "Catch Flame should not damage a fire-immune living target");
        helper.assertTrue(blaze.getRemainingFireTicks() <= 0,
                "Catch Flame should not ignite a fire-immune living target");

        discardAll(blaze, caster);
        helper.succeed();
    }

    static void catchFlameDamagesFireVulnerableEndCrystal(GameTestHelper helper) {
        var caster = createPlayer(helper, "catch_flame_end_crystal_target", new Vec3(1.5D, 2.0D, 1.5D));
        var crystalPosition = helper.absoluteVec(new Vec3(1.5D, 2.0D, 3.0D));
        var crystal = new EndCrystal(helper.getLevel(), crystalPosition.x, crystalPosition.y, crystalPosition.z);
        helper.getLevel().addFreshEntity(crystal);
        aimAt(caster, crystal.getBoundingBox().getCenter());

        cast(helper, catchFlame(), 2, caster);

        var impacts = helper.getLevel().getEntitiesOfClass(
                CatchFlameImpactEntity.class,
                crystal.getBoundingBox().inflate(0.15D)
        );
        helper.assertTrue(impacts.size() == 1,
                "Catch Flame should target a valid non-living End Crystal");
        helper.assertFalse(crystal.isAlive(),
                "Catch Flame should damage a fire-vulnerable End Crystal in Minecraft 1.20.1");

        discardAll(crystal, caster);
        impacts.forEach(Entity::discard);
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

    static void catchFlameIgnitesEssenceSmokerThroughPlayerInteraction(GameTestHelper helper) {
        var caster = createPlayer(helper, "catch_flame_smoker_ignite", new Vec3(1.5D, 2.0D, 1.5D));
        var smokerPosition = helper.absolutePos(new BlockPos(1, 2, 3));
        var essenceSmoker = prepareEssenceSmoker(helper, smokerPosition);
        aimAt(caster, Vec3.atCenterOf(smokerPosition));

        try (var ignored = ApprenticeCodexServerConfig.useCatchFlameConfigOverrideForGameTest(
                new CatchFlameServerConfig.Values(true, false))) {
            SpellDispenserCastContext.run(() -> cast(helper, catchFlame(), 1, caster));
        }

        helper.assertTrue(essenceSmoker.isProcessing(),
                "Catch Flame should ignite an Essence Smoker through its normal player interaction");
        helper.assertTrue(helper.getLevel().getBlockState(smokerPosition.above()).isAir(),
                "Catch Flame should not place surface fire after igniting an Essence Smoker");

        helper.getLevel().setBlockAndUpdate(smokerPosition, Blocks.AIR.defaultBlockState());
        caster.discard();
        helper.succeed();
    }

    static void catchFlameRespectsCancelledEssenceSmokerInteraction(GameTestHelper helper) {
        var caster = createPlayer(helper, "catch_flame_smoker_protection", new Vec3(1.5D, 2.0D, 1.5D));
        var smokerPosition = helper.absolutePos(new BlockPos(1, 2, 3));
        var essenceSmoker = prepareEssenceSmoker(helper, smokerPosition);
        aimAt(caster, Vec3.atCenterOf(smokerPosition));
        var interactionEvents = new AtomicInteger();

        java.util.function.Consumer<PlayerInteractEvent.RightClickBlock> cancelListener = event -> {
            if (event.getEntity() == caster && event.getPos().equals(smokerPosition)) {
                interactionEvents.incrementAndGet();
                event.setCanceled(true);
            }
        };

        MinecraftForge.EVENT_BUS.addListener(cancelListener);
        try {
            try (var ignored = ApprenticeCodexServerConfig.useCatchFlameConfigOverrideForGameTest(
                    new CatchFlameServerConfig.Values(true, false))) {
                SpellDispenserCastContext.run(() -> cast(helper, catchFlame(), 1, caster));
            }
        } finally {
            MinecraftForge.EVENT_BUS.unregister(cancelListener);
        }

        helper.assertTrue(interactionEvents.get() == 1,
                "Catch Flame should fire one Essence Smoker interaction event with the caster as the actor");
        helper.assertTrue(!essenceSmoker.isProcessing(),
                "Canceling the Essence Smoker interaction should prevent Catch Flame from starting processing");
        helper.assertTrue(helper.getLevel().getBlockState(smokerPosition.above()).isAir(),
                "Canceling the Essence Smoker interaction should not fall back to surface fire");

        helper.getLevel().setBlockAndUpdate(smokerPosition, Blocks.AIR.defaultBlockState());
        caster.discard();
        helper.succeed();
    }

    static void catchFlameUsesFlintAndSteelIgnitionBehavior(GameTestHelper helper) {
        var caster = createPlayer(helper, "catch_flame_flint_behavior", new Vec3(3.5D, 2.0D, 1.5D));
        var candlePosition = new BlockPos(3, 2, 3);
        helper.setBlock(candlePosition, Blocks.CANDLE);
        var absoluteCandlePosition = helper.absolutePos(candlePosition);
        aimAt(caster, Vec3.atLowerCornerOf(absoluteCandlePosition).add(0.5D, 0.2D, 0.5D));

        try (var ignored = ApprenticeCodexServerConfig.useCatchFlameConfigOverrideForGameTest(
                new CatchFlameServerConfig.Values(false, true))) {
            cast(helper, catchFlame(), 1, caster);
            helper.assertTrue(helper.getBlockState(candlePosition).getValue(BlockStateProperties.LIT),
                    "Catch Flame should light a candle like flint and steel");

            helper.setBlock(candlePosition, Blocks.AIR);
            buildNetherPortalFrame(helper);
            var portalBottom = new BlockPos(3, 2, 3);
            var portalInteriorCenter = Vec3.atCenterOf(helper.absolutePos(portalBottom));
            aimAt(caster, portalInteriorCenter.add(-0.5D, 0.0D, 0.0D));
            var rayStart = caster.getEyePosition(1.0F);
            var rayEnd = rayStart.add(caster.getViewVector(1.0F).scale(3.0D));
            var frameHit = helper.getLevel().clip(new ClipContext(
                    rayStart, rayEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
            helper.assertTrue(frameHit.getBlockPos().equals(helper.absolutePos(new BlockPos(2, 2, 3)))
                            && frameHit.getDirection() == Direction.EAST,
                    "Portal setup should expose the frame's inner face to Catch Flame: " + frameHit);
            helper.assertTrue(PortalShape.findEmptyPortalShape(
                            helper.getLevel(), helper.absolutePos(portalBottom), Direction.Axis.X).isPresent(),
                    "Portal setup should form a valid empty Nether portal frame");
            cast(helper, catchFlame(), 1, caster);
            helper.assertTrue(helper.getBlockState(portalBottom).is(Blocks.NETHER_PORTAL),
                    "Catch Flame should open a valid Nether portal like flint and steel: "
                            + helper.getBlockState(portalBottom));
        }

        caster.discard();
        helper.succeed();
    }

    static void catchFlameRespectsPlaceEventWhenIgnitingFire(GameTestHelper helper) {
        var caster = createPlayer(helper, "catch_flame_place_event", new Vec3(1.5D, 2.0D, 1.5D));
        var supportPosition = new BlockPos(1, 2, 3);
        var firePosition = supportPosition.above();
        helper.setBlock(supportPosition, Blocks.STONE);
        aimAt(caster, Vec3.atBottomCenterOf(helper.absolutePos(firePosition)));
        var placeEvents = new AtomicInteger();

        java.util.function.Consumer<BlockEvent.EntityPlaceEvent> cancelListener = event -> {
            if (event.getEntity() == caster && event.getPlacedBlock().is(Blocks.FIRE)) {
                placeEvents.incrementAndGet();
                event.setCanceled(true);
            }
        };

        MinecraftForge.EVENT_BUS.addListener(cancelListener);
        try {
            try (var ignored = ApprenticeCodexServerConfig.useCatchFlameConfigOverrideForGameTest(
                    new CatchFlameServerConfig.Values(false, true))) {
                cast(helper, catchFlame(), 1, caster);
            }
        } finally {
            MinecraftForge.EVENT_BUS.unregister(cancelListener);
        }

        helper.assertTrue(placeEvents.get() == 1,
                "Catch Flame should fire one place event with the caster as the actor");
        helper.assertTrue(helper.getBlockState(firePosition).isAir(),
                "Canceling the Catch Flame place event should restore the fire position");

        caster.discard();
        helper.succeed();
    }

    private static void buildNetherPortalFrame(GameTestHelper helper) {
        for (var x = 3; x <= 4; ++x) {
            for (var y = 2; y <= 4; ++y) {
                helper.setBlock(new BlockPos(x, y, 3), Blocks.AIR);
            }
        }
        for (var x = 2; x <= 5; ++x) {
            helper.setBlock(new BlockPos(x, 1, 3), Blocks.OBSIDIAN);
            helper.setBlock(new BlockPos(x, 5, 3), Blocks.OBSIDIAN);
        }
        for (var y = 2; y <= 4; ++y) {
            helper.setBlock(new BlockPos(2, y, 3), Blocks.OBSIDIAN);
            helper.setBlock(new BlockPos(5, y, 3), Blocks.OBSIDIAN);
        }
    }

    private static CatchFlame catchFlame() {
        return (CatchFlame) SpellRegistry.CATCH_FLAME.get();
    }

    private static EssenceSmokerBlockEntity prepareEssenceSmoker(GameTestHelper helper, BlockPos smokerPosition) {
        helper.getLevel().setBlockAndUpdate(smokerPosition, BlockRegistry.ESSENCE_SMOKER.get().defaultBlockState());
        var blockEntity = helper.getLevel().getBlockEntity(smokerPosition);
        if (!(blockEntity instanceof EssenceSmokerBlockEntity essenceSmoker)) {
            throw new IllegalStateException("Failed to create an Essence Smoker for Catch Flame GameTest");
        }
        if (!essenceSmoker.setCatalyst(new ItemStack(Items.BLAZE_POWDER))
                || !essenceSmoker.addMaterial(new ItemStack(Items.BONE_MEAL))) {
            throw new IllegalStateException("Failed to prepare an Essence Smoker recipe for Catch Flame GameTest");
        }
        return essenceSmoker;
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
