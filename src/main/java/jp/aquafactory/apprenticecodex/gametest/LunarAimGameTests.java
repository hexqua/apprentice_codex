package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.lunaraim.*;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class LunarAimGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String BATCH = "apprenticecodex.lunar_aim";
    private static int sceneSequence;

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void selectionSupportsCrystalsRangeAssistAndWalls(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var crystal = new EndCrystal(EntityType.END_CRYSTAL, h.getLevel());
            crystal.setPos(s.origin.add(8, 0, 0)); s.add(crystal);
            h.assertTrue(s.selected() == crystal, "End crystals must be selectable");
            crystal.discard();
            var target = s.zombie(8, 1.1);
            h.assertTrue(s.selected() == target, "One block aim assist must accept a near miss");
            target.setPos(s.origin.add(8, 0, 1.5));
            h.assertTrue(s.selected() == null, "Targets outside the assisted ray must be excluded");
            target.setPos(s.origin.add(8, 0, 0));
            var block = BlockPos.containing(s.owner.getEyePosition().add(4, 0, 0));
            var previous = h.getLevel().getBlockState(block);
            try {
                h.getLevel().setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
                h.assertTrue(s.selected() == null, "A wall must stop lock-on");
                target.setPos(block.getX() + 1.4, s.origin.y, s.origin.z);
                h.assertTrue(s.selected() == null, "Aim assist must not reach through the front of a wall");
            } finally { h.getLevel().setBlockAndUpdate(block, previous); }
            // 水平方向の遠方は未追跡chunkになるため、同一chunk内の上方向で射程境界を検証する。
            s.owner.setXRot(-90);
            target.setPos(s.owner.getEyePosition().add(0, 127, 0));
            h.assertTrue(CombatTools.findLookCombatTarget(s.owner, 128, 0) == target, "Targets within 128 blocks must lock");
            target.setPos(s.owner.getEyePosition().add(0, 129, 0));
            h.assertTrue(CombatTools.findLookCombatTarget(s.owner, 128, 0) == null, "Targets beyond the ray endpoint must not lock");
            target.setPos(s.owner.getEyePosition().add(0, 128, 0));
            h.assertTrue(CombatTools.findLookCombatTarget(s.owner, 128, 0) == target, "An intersection at the range endpoint must lock");
            s.owner.setPos(s.origin.add(0, 160, 0)); s.owner.setXRot(90);
            target.setPos(s.owner.getEyePosition().add(0, -128 - target.getBbHeight(), 0));
            h.assertTrue(CombatTools.findLookCombatTarget(s.owner, 128, 0) == target, "The inclusive endpoint must also accept the upper face of a target");
            target.setPos(target.position().add(0, -0.001, 0));
            h.assertTrue(CombatTools.findLookCombatTarget(s.owner, 128, 0) == null, "A target just beyond the endpoint must be excluded");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void selectionUsesCombatPolicyAndNearestIntersection(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var near = s.zombie(4, 0); var far = s.zombie(8, 0);
            h.assertTrue(s.selected() == near, "The nearest eligible intersection must win");
            var board = h.getLevel().getScoreboard();
            var team = board.addPlayerTeam("lunar_" + UUID.randomUUID().toString().substring(0, 8));
            try {
                board.addPlayerToTeam(s.owner.getScoreboardName(), team);
                board.addPlayerToTeam(near.getScoreboardName(), team);
                team.setAllowFriendlyFire(false);
                h.assertTrue(s.selected() == far, "Protected allies must be skipped");
                team.setAllowFriendlyFire(true);
                h.assertTrue(s.selected() == near, "Friendly fire must follow the common combat policy");
                near.discard(); far.discard();
                h.assertTrue(s.selected() == null, "The caster must never select itself");
            } finally { board.removePlayerTeam(team); }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void castFreezesSelectionAndFiresFourEvenDirections(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(8, 0);
            var data = s.preCast();
            h.assertTrue(data.getAdditionalCastData() instanceof LunarAimCastData captured && target.getUUID().equals(captured.targetId()),
                    "Precast must snapshot the target: expected=" + target.getUUID() + ", captured=" + data.getAdditionalCastData());
            s.owner.setYRot(35); s.owner.setXRot(-25);
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SPELLBOOK, data);
            var arrows = s.castArrows();
            h.assertTrue(arrows.size() == 4, "Exactly four arrows must spawn");
            for (int i = 0; i < 4; i++) {
                var direction = Vec3.directionFromRotation(-25, 35 - 60 + 40 * i).scale(2);
                h.assertTrue(arrows.stream().anyMatch(a -> a.getDeltaMovement().distanceTo(direction) < 1.0e-6), "Launch yaw spacing must be 40 degrees and preserve pitch");
            }
            h.assertTrue(arrows.stream().allMatch(LunarAimArrowEntity::hasTarget), "Turning during casting must preserve the selected target");
            h.assertTrue(!(data.getAdditionalCastData() instanceof LunarAimCastData), "Casting must clear the snapshot");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void emptyCancelledAndDirectCasts(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var data = s.preCast();
            var target = s.zombie(8, 0);
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SPELLBOOK, data);
            h.assertTrue(s.castArrows().stream().noneMatch(LunarAimArrowEntity::hasTarget), "An empty snapshot must not reselect");
            s.castArrows().forEach(Entity::discard);
            data = s.preCast();
            s.spell.onServerCastComplete(h.getLevel(), 1, s.owner, data, true);
            h.assertTrue(data.getAdditionalCastData() == null, "Cancellation must clear target state");
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SWORD, data);
            h.assertTrue(s.castArrows().stream().allMatch(LunarAimArrowEntity::hasTarget), "Direct casts must perform the missing selection");
            s.castArrows().forEach(Entity::discard);
            data = s.preCast(); target.discard();
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SPELLBOOK, data);
            h.assertTrue(s.castArrows().stream().noneMatch(LunarAimArrowEntity::hasTarget), "A target removed during casting must be lost");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void straightDelayHomingAndLifetime(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(12, 0);
            var arrow = s.arrow(target, new Vec3(-1, 0, 0));
            var start = arrow.position();
            for (int i = 0; i < 5; i++) step(arrow);
            h.assertTrue(arrow.position().distanceTo(start.add(-10, 0, 0)) < 1.0e-6, "First five ticks must fly straight at speed two");
            step(arrow);
            double firstTurnDegrees = Math.toDegrees(Math.acos(new Vec3(-1, 0, 0).dot(arrow.getDeltaMovement().normalize())));
            h.assertTrue(Math.abs(firstTurnDegrees - 7.2) < 1.0e-5,
                    "The first homing tick must turn by 7.2 degrees, excluding the straight delay: " + firstTurnDegrees);
            for (int i = 0; i < 100 && !arrow.isBursting(); i++) step(arrow);
            h.assertTrue(arrow.isBursting() && target.getHealth() < 20, "Backwards launch must converge and explode");
            var normal = s.arrow(null, new Vec3(0, 1, 0));
            for (int i = 0; i < 199; i++) { normal.setPos(start); step(normal); }
            h.assertTrue(!normal.isRemoved() && normal.getDeltaMovement().equals(new Vec3(0, 2, 0)), "Unlocked arrows must keep speed and survive until tick 199");
            step(normal);
            h.assertTrue(normal.isRemoved() && !normal.isBursting(), "Tick 200 must silently expire");
            var locked = s.arrow(target, new Vec3(0, 1, 0)); locked.tickCount = 199; step(locked);
            h.assertTrue(locked.isRemoved() && !locked.isBursting(), "Locked arrows must use the same deadline");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void targetLossIsPermanentAndKeepsDeadline(GameTestHelper h) {
        try (var s = new Scene(h)) {
            for (var reason : List.of(Entity.RemovalReason.KILLED, Entity.RemovalReason.UNLOADED_TO_CHUNK,
                    Entity.RemovalReason.CHANGED_DIMENSION, Entity.RemovalReason.DISCARDED)) {
                var target = s.zombie(12, 0);
                var arrow = s.arrow(target, new Vec3(0, 0, 1));
                target.remove(reason); step(arrow);
                h.assertTrue(!arrow.hasTarget(), "Target removal must clear homing during the straight delay: " + reason);
                var replacement = s.zombie(12, 0, target.getUUID());
                var velocity = arrow.getDeltaMovement();
                for (int i = 0; i < 198; i++) { arrow.setPos(s.origin.add(0, 8, 0)); step(arrow); }
                h.assertTrue(!arrow.isRemoved() && !arrow.hasTarget() && arrow.getDeltaMovement().equals(velocity), "Target loss must preserve straight flight and the original deadline");
                step(arrow);
                h.assertTrue(arrow.isRemoved() && !arrow.isBursting(), "Lost-target arrows must silently expire at 200 ticks");
                replacement.discard();
            }
            var target = s.zombie(15, 0);
            var arrow = s.arrow(target, new Vec3(0, 1, 0));
            for (int i = 0; i < 11; i++) step(arrow);
            target.setHealth(0); step(arrow);
            var velocity = arrow.getDeltaMovement(); step(arrow);
            h.assertTrue(!arrow.hasTarget() && velocity.equals(arrow.getDeltaMovement()), "Death after homing begins must freeze the last velocity");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void impactDealsOnlyBurstDamageAndEachArrowCounts(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var direct = s.zombie(1.5, 0);
            var corner = s.zombie(3, 2);
            var outside = s.zombie(1.5, 4);
            var arrow = s.arrow(null, new Vec3(1, 0, 0)); step(arrow);
            h.assertTrue(arrow.isBursting() && direct.getHealth() == 16, "Direct hit must deal exactly one burst worth of damage");
            h.assertTrue(corner.getHealth() == 16 && outside.getHealth() == 20, "A cube corner must be hit, while outside targets are excluded");
            h.assertTrue(s.owner.getHealth() == s.owner.getMaxHealth(), "The blast must protect its caster");
            for (int i = 0; i < 3; i++) step(s.arrow(null, new Vec3(1, 0, 0)));
            h.assertTrue(direct.getHealth() == 4, "Four separate arrows must bypass hurt immunity and each deal damage");
            for (int i = 0; i < 20; i++) step(arrow);
            h.assertTrue(arrow.isRemoved() && direct.getHealth() == 4, "The visual burst must expire without repeated damage");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void homingHitsBystandersBlocksAndCounterspellIsSilent(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(12, 0); var bystander = s.zombie(1.5, 0);
            var arrow = s.arrow(target, new Vec3(1, 0, 0)); arrow.tickCount = 10; step(arrow);
            h.assertTrue(arrow.isBursting() && bystander.getHealth() == 16 && target.getHealth() == 20, "Homing must explode at the first bystander without piercing");
            bystander.discard();
            var block = BlockPos.containing(s.origin.add(1, 0, 0));
            var previous = h.getLevel().getBlockState(block);
            try {
                h.getLevel().setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
                arrow = s.arrow(target, new Vec3(1, 0, 0)); arrow.tickCount = 10; step(arrow);
                h.assertTrue(arrow.isBursting(), "Homing must explode on a block");
                h.assertTrue(h.getLevel().getBlockState(block).is(Blocks.STONE), "The blast must not destroy terrain");
                arrow = s.arrow(null, new Vec3(1, 0, 0)); arrow.setPos(Vec3.atCenterOf(block)); step(arrow);
                h.assertTrue(arrow.isBursting(), "Spawning inside solid terrain must impact immediately");
                for (var fluid : List.of(Blocks.WATER, Blocks.LAVA)) {
                    h.getLevel().setBlockAndUpdate(block, fluid.defaultBlockState());
                    arrow = s.arrow(null, new Vec3(1, 0, 0)); var start = arrow.position(); step(arrow);
                    h.assertTrue(!arrow.isBursting() && arrow.position().distanceTo(start.add(2, 0, 0)) < 1.0e-6, "Fluids must not stop or slow flight");
                }
            } finally { h.getLevel().setBlockAndUpdate(block, previous); }
            arrow = s.arrow(target, new Vec3(1, 0, 0));
            arrow.onAntiMagic(MagicData.getPlayerMagicData(s.owner)); step(arrow);
            h.assertTrue(arrow.isRemoved() && !arrow.isBursting() && target.getHealth() == 20, "Counterspell must discard without a burst or damage");
            h.assertTrue(EntityRegistry.LUNAR_AIM_ARROW.get().is(io.redspace.ironsspellbooks.util.ModTags.GUIDING_BOLT_IMMUNE), "External Guided steering must not override flight");
        }
        h.succeed();
    }

    private static void step(Entity entity) { entity.tickCount++; entity.tick(); }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void precastSyncsTargetMarkerForLivingTargetsAndCrystals(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var packets = new ArrayList<net.minecraft.network.protocol.Packet<?>>();
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
            net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
            var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(s.owner.getGameProfile(), false);
            var previous = s.owner.connection;
            new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(), connection, s.owner, cookie) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { packets.add(packet); }
            };
            try {
                var living = s.zombie(8, 0);
                var crystal = new EndCrystal(EntityType.END_CRYSTAL, h.getLevel());
                crystal.setPos(s.origin.add(12, 0, 0)); s.add(crystal);
                for (var target : List.of(living, crystal)) {
                    packets.clear(); s.preCast();
                    var markers = packets.stream()
                            .filter(p -> p instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket payload
                                    && payload.payload() instanceof io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket)
                            .map(p -> (io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket)
                                    ((net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket) p).payload()).toList();
                    h.assertTrue(markers.size() == 1, "Successful precast must send one native targeting marker");
                    var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                    try {
                        markers.getFirst().write(buffer);
                        h.assertTrue(buffer.readUtf().equals(s.spell.getSpellId()) && buffer.readInt() == 1
                                        && buffer.readUUID().equals(target.getUUID()),
                                "The marker must carry LunarAim and the selected UUID, including non-living targets");
                    } finally { buffer.release(); }
                    target.discard();
                }
                packets.clear(); s.preCast();
                h.assertTrue(packets.isEmpty(), "No target must produce neither marker nor success notification");
            } finally {
                s.owner.connection = previous;
                channel.finishAndReleaseAll();
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void multipartSelectionAndBurstResolveParentOnlyOnce(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var dragon = EntityType.ENDER_DRAGON.create(h.getLevel());
            Objects.requireNonNull(dragon).setPos(s.origin.add(8, 0, 8));
            dragon.setNoAi(true);
            dragon.getPhaseManager().setPhase(net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOLDING_PATTERN);
            s.add(dragon);
            // 親の箱を視線・爆発範囲から外し、複数部位だけが同じ爆発に入る配置にする。
            for (var part : dragon.getParts()) part.setPos(s.origin.add(1.5, 0, 0));
            h.assertTrue(s.selected() == dragon, "A visible multipart hitbox must select its parent");
            var arrow = s.arrow(dragon, new Vec3(1, 0, 0)); step(arrow);
            // Dragonのbody経由ダメージは4/4+1=2。複数部位への重複適用を検出する。
            h.assertTrue(arrow.isBursting() && Math.abs(dragon.getHealth() - (dragon.getMaxHealth() - 2)) < 0.01,
                    "The burst must damage a multipart parent exactly once: health=" + dragon.getHealth());
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void cancelledEntityAndPhysicalBlockImpactsContinue(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(1.5, 0);
            var arrow = s.arrow(null, new Vec3(1, 0, 0));
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.ProjectileImpactEvent> listener = event -> {
                if (event.getProjectile() instanceof LunarAimArrowEntity lunar && lunar.getOwner() == s.owner) event.setCanceled(true);
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
            var block = BlockPos.containing(s.origin.add(1, 0, 0));
            var previous = h.getLevel().getBlockState(block);
            try {
                var start = arrow.position(); step(arrow);
                h.assertTrue(!arrow.isBursting() && target.getHealth() == 20 && arrow.position().distanceTo(start.add(2, 0, 0)) < 1.0e-6,
                        "Cancelled entity impacts must preserve movement and deal no damage");
                target.discard();
                h.getLevel().setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
                arrow = s.arrow(null, new Vec3(1, 0, 0)); start = arrow.position(); step(arrow);
                h.assertTrue(!arrow.isBursting() && arrow.position().distanceTo(start.add(2, 0, 0)) < 1.0e-6,
                        "Cancelled block impacts must undo physical clipping");
            } finally {
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
                h.getLevel().setBlockAndUpdate(block, previous);
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 80)
    public static void realServerTicksPreserveDelayAndLoseTargets(GameTestHelper h) {
        var s = new Scene(h);
        var target = s.zombie(12, 0);
        // 実tickの移動と手動stepの双方で、tickCountの数え方が一致することを確認する。
        var arrow = s.arrow(target, new Vec3(0, 1, 0));
        h.runAfterDelay(3, () -> {
            try {
                h.assertTrue(arrow.tickCount > 0 && arrow.tickCount <= 5 && arrow.getDeltaMovement().equals(new Vec3(0, 2, 0)),
                        "Server ticks must retain straight movement during the initial delay");
            } catch (Throwable failure) { s.close(); throw failure; }
        });
        h.runAfterDelay(8, () -> {
            try {
                h.assertTrue(arrow.hasTarget() && arrow.getDeltaMovement().x > 0, "Server ticks must begin steering after the delay");
                target.discard();
            } catch (Throwable failure) { s.close(); throw failure; }
        });
        h.runAfterDelay(11, () -> {
            try (s) {
                h.assertTrue(!arrow.isRemoved() && !arrow.isBursting() && !arrow.hasTarget(), "Target removal must leave a live straight arrow");
            }
            h.succeed();
        });
    }

    private static final class Scene implements AutoCloseable {
        final GameTestHelper helper;
        final Vec3 origin;
        final FakePlayer owner;
        final LunarAim spell = (LunarAim) SpellRegistry.LUNAR_AIM.get();
        final List<Entity> entities = new ArrayList<>();
        Scene(GameTestHelper h) {
            // 128ブロックの視線が隣のテストのmobや大型部位を拾わないよう、各sceneの高度を分離する。
            helper = h; origin = h.absoluteVec(new Vec3(2, 80 + 12 * sceneSequence++, 2));
            owner = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "lunar_test"));
            owner.setPos(origin); owner.setYRot(-90); owner.setXRot(0); owner.setNoGravity(true); add(owner);
        }
        void add(Entity entity) { entities.add(entity); helper.getLevel().addFreshEntity(entity); }
        Zombie zombie(double x, double z) {
            return zombie(x, z, UUID.randomUUID());
        }
        Zombie zombie(double x, double z, UUID uuid) {
            var entity = EntityType.ZOMBIE.create(helper.getLevel());
            Objects.requireNonNull(entity).setUUID(uuid);
            entity.setPos(origin.add(x, 0, z)); entity.setNoAi(true); entity.setNoGravity(true);
            entity.getAttribute(Attributes.ARMOR).setBaseValue(0); add(entity); return entity;
        }
        LunarAimArrowEntity arrow(Entity target, Vec3 direction) {
            var entity = new LunarAimArrowEntity(EntityRegistry.LUNAR_AIM_ARROW.get(), helper.getLevel());
            entity.launch(owner, origin.add(0, 0.9, 0), direction, 4, 2.5f, target); add(entity); return entity;
        }
        Entity selected() { return CombatTools.findLookCombatTarget(owner, 128, 1); }
        MagicData preCast() {
            var data = MagicData.getPlayerMagicData(owner);
            spell.onServerPreCast(helper.getLevel(), 1, owner, data); return data;
        }
        List<LunarAimArrowEntity> castArrows() {
            return helper.getLevel().getEntitiesOfClass(LunarAimArrowEntity.class, owner.getBoundingBox().inflate(3)).stream()
                    .filter(entity -> entity.getOwner() == owner && !entity.isRemoved()).toList();
        }
        @Override public void close() {
            castArrows().forEach(Entity::discard);
            entities.forEach(Entity::discard);
        }
    }
}
