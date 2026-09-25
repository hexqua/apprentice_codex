package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.network.casting.OnCastFinishedPacket;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.util.ModTags;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.lockonray.LockOnRay;
import jp.aquafactory.apprenticecodex.spell.lockonray.LockOnRayCastData;
import jp.aquafactory.apprenticecodex.spell.lockonray.LockOnRayCurve;
import jp.aquafactory.apprenticecodex.spell.lockonray.LockOnRayEvents;
import jp.aquafactory.apprenticecodex.spell.lockonray.LockOnRayLaserEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class LockOnRayGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String BATCH = "apprenticecodex.lock_on_ray";

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void acquisitionAndCadence(GameTestHelper h) {
        try (var s = new Scene(h)) {
            s.owner.setXRot(-90);
            h.assertTrue(!s.spell.checkPreCastConditions(h.getLevel(), 1, s.owner, s.data), "Empty aim must reject casting");
            s.owner.setXRot(0);
            var target = s.zombie(6, 0);
            s.begin();
            s.owner.setYRot(90);
            for (int i = 0; i < 19; i++) s.castTick();
            h.assertTrue(s.lasers().isEmpty(), "Warmup must not emit a laser");
            s.castTick();
            h.assertTrue(s.lasers().size() == 1, "First laser must fire at elapsed tick 20");
            for (int i = 0; i < 4; i++) s.castTick();
            h.assertTrue(s.lasers().size() == 2, "Subsequent lasers must fire every four ticks");
            h.assertTrue(((LockOnRayCastData) s.data.getAdditionalCastData()).resolve(h.getLevel()) == target,
                    "Turning must not change the locked target");
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SPELLBOOK, s.data);
            h.assertTrue(s.lasers().size() == 2, "Standard mana callbacks must not emit extra lasers");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void rangeOcclusionAndCrystalSelection(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(64, 0);
            h.assertTrue(s.spell.checkPreCastConditions(h.getLevel(), 1, s.owner, s.data), "Range boundary must be selectable");
            target.setPos(s.origin.add(67, 0, 0));
            h.assertTrue(!s.spell.checkPreCastConditions(h.getLevel(), 1, s.owner, s.data), "Targets beyond range must be rejected");
            target.setPos(s.origin.add(6, 0, 0));
            var wall = BlockPos.containing(s.origin.add(3, 1, 0));
            var previous = h.getLevel().getBlockState(wall);
            try {
                h.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
                h.assertTrue(!s.spell.checkPreCastConditions(h.getLevel(), 1, s.owner, s.data), "Initial lock must not see through a wall");
            } finally { h.getLevel().setBlockAndUpdate(wall, previous); }
            target.discard();
            var crystal = EntityType.END_CRYSTAL.create(h.getLevel());
            crystal.setPos(s.origin.add(6, 0, 0)); s.add(crystal);
            s.begin();
            h.assertTrue(((LockOnRayCastData) s.data.getAdditionalCastData()).resolve(h.getLevel()) == crystal,
                    "CombatTools crystal targets must be supported");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void deathAndRemovalCancelCasting(GameTestHelper h) {
        for (var reason : List.of(Entity.RemovalReason.KILLED, Entity.RemovalReason.UNLOADED_TO_CHUNK,
                Entity.RemovalReason.CHANGED_DIMENSION, Entity.RemovalReason.DISCARDED)) {
            try (var s = new Scene(h)) {
                var target = s.zombie(6, 0); s.begin();
                var cast = (LockOnRayCastData) s.data.getAdditionalCastData();
                target.setRemoved(reason);
                s.castTick();
                h.assertTrue(!s.data.isCasting() && s.data.getAdditionalCastData() == null && cast.resolve(h.getLevel()) == null,
                        "Target removal must release the lock and cancel casting: " + reason);
                h.assertTrue(s.lasers().isEmpty(), "Invalid targets must not emit a laser");
            }
        }
        try (var s = new Scene(h)) {
            var target = s.zombie(6, 0); s.begin(); target.setHealth(0); s.castTick();
            h.assertTrue(!s.data.isCasting(), "Dying targets must cancel before entity removal");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void casterLifecycleReleasesOnlyCastLock(GameTestHelper h) {
        for (int mode = 0; mode < 3; mode++) {
            try (var s = new Scene(h)) {
                var target = s.zombie(8, 0); s.begin();
                var cast = (LockOnRayCastData) s.data.getAdditionalCastData();
                var laser = s.laser(target);
                if (mode == 0) s.spell.onServerCastComplete(h.getLevel(), 1, s.owner, s.data, true);
                else if (mode == 1) LockOnRayEvents.onLogout(new PlayerEvent.PlayerLoggedOutEvent(s.owner));
                else LockOnRayEvents.onDimensionChanged(new PlayerEvent.PlayerChangedDimensionEvent(s.owner, Level.OVERWORLD, Level.NETHER));
                h.assertTrue(!s.data.isCasting() && s.data.getAdditionalCastData() == null && cast.resolve(h.getLevel()) == null,
                        "Caster lifecycle must release cast data");
                fly(laser, 30);
                h.assertTrue(laser.isRemoved() && target.getHealth() < 20, "Already launched lasers must continue homing after cancellation");
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void hermiteArrivalAndMovingTarget(GameTestHelper h) {
        for (double distance : List.of(4.0, 16.0, 64.0)) {
            for (boolean moving : List.of(false, true)) {
                try (var s = new Scene(h)) {
                    var target = s.zombie(distance, 0);
                    var laser = s.laser(target);
                    int arrival = 5 + (int) Math.round(laser.position().distanceTo(target.getBoundingBox().getCenter()) / 16);
                    var start = laser.position(); step(laser);
                    h.assertTrue(laser.getX() < start.x - 2, "Laser must arc at least two blocks behind the caster on its first tick");
                    for (int i = 1; i < arrival && !laser.isRemoved(); i++) {
                        if (moving) target.setPos(target.position().add(0, 0, 0.2));
                        step(laser);
                    }
                    h.assertTrue(laser.isRemoved() && target.getHealth() < 20, "Laser must reach a live target by its distance-derived arrival time");
                    h.assertTrue(laser.tickCount >= arrival - 1, "Unobstructed laser must not arrive substantially early");
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void nativeTargetNotificationsAndMarkerLifecycle(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var packets = new ArrayList<Packet<?>>();
            var connection = new Connection(PacketFlow.SERVERBOUND);
            var channel = new EmbeddedChannel(connection);
            NetworkRegistry.configureMockConnection(connection);
            var previous = s.owner.connection;
            new ServerGamePacketListenerImpl(h.getLevel().getServer(), connection, s.owner,
                    CommonListenerCookie.createInitial(s.owner.getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) { packets.add(packet); }
            };
            try {
                s.owner.setXRot(-90);
                h.assertTrue(!s.spell.checkPreCastConditions(h.getLevel(), 1, s.owner, s.data), "Empty aim must fail");
                h.assertTrue(packets.size() == 1 && packets.getFirst() instanceof ClientboundSetActionBarTextPacket,
                        "Failed targeting must send the native action-bar error without a marker");
                packets.clear(); s.owner.setXRot(0);
                var target = s.zombie(8, 0); s.begin();
                h.assertTrue(packets.stream().filter(p -> p instanceof ClientboundSetActionBarTextPacket).count() == 1,
                        "Successful targeting must send exactly one action-bar notification");
                var markers = packets.stream().filter(p -> p instanceof ClientboundCustomPayloadPacket payload
                                && payload.payload() instanceof SyncTargetingDataPacket)
                        .map(p -> (SyncTargetingDataPacket) ((ClientboundCustomPayloadPacket) p).payload()).toList();
                h.assertTrue(markers.size() == 1, "Living targets must receive the native targeting marker");
                var buffer = new FriendlyByteBuf(Unpooled.buffer());
                try {
                    markers.getFirst().write(buffer);
                    h.assertTrue(buffer.readUtf().equals(s.spell.getSpellId()) && buffer.readInt() == 1
                                    && buffer.readUUID().equals(target.getUUID()), "Marker must identify the spell and locked entity");
                } finally { buffer.release(); }
                packets.clear();
                s.spell.onServerCastComplete(h.getLevel(), 1, s.owner, s.data, true);
                h.assertTrue(packets.stream().anyMatch(p -> p instanceof ClientboundCustomPayloadPacket payload
                                && payload.payload() instanceof OnCastFinishedPacket),
                        "Cancellation must send the native completion packet that clears targeting visuals");
            } finally {
                s.owner.connection = previous;
                channel.finishAndReleaseAll();
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void lostTargetFliesStraightUntilFortyTicks(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(12, 0);
            var laser = s.laser(target); fly(laser, 4);
            var delta = laser.getDeltaMovement();
            var previous = laser.position(); target.discard(); step(laser);
            h.assertTrue(laser.position().subtract(previous).distanceTo(delta) < 1.0e-8,
                    "Lost targets must preserve the last actual tick displacement");
            fly(laser, 34);
            h.assertTrue(!laser.isRemoved() && laser.tickCount == 39, "Missed laser must survive through tick 39");
            step(laser);
            h.assertTrue(laser.isRemoved(), "Missed laser must expire at tick 40");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void terrainBypassAndNoKnockbackOrIframes(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(8, 0);
            var wall = BlockPos.containing(s.origin.add(7, 1, 0));
            var previous = h.getLevel().getBlockState(wall);
            try {
                h.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
                var motion = new Vec3(0.1, 0.2, 0.3); target.setDeltaMovement(motion);
                for (int i = 0; i < 2; i++) {
                    target.invulnerableTime = 20;
                    var laser = s.laser(target); fly(laser, 30);
                    h.assertTrue(laser.isRemoved(), "Terrain must not prevent a hit");
                }
                h.assertTrue(Math.abs(target.getHealth() - 4) < 0.01, "Each laser must bypass damage immunity frames");
                h.assertTrue(target.getDeltaMovement().distanceTo(motion) < 1.0e-8, "Laser damage must preserve target motion");
            } finally { h.getLevel().setBlockAndUpdate(wall, previous); }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void otherTargetsInterceptAndAlliesAreProtected(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var primary = s.zombie(12, 0);
            var laser = s.laser(primary);
            var bystander = s.zombie(-0.1, 0); bystander.setPos(laser.position().add(0, -0.9, 0));
            step(laser);
            h.assertTrue(laser.isRemoved() && bystander.getHealth() < 20 && primary.getHealth() == 20,
                    "First valid combat target must consume the laser even when it is not the locked target");
            bystander.discard();
            var board = h.getLevel().getScoreboard();
            var team = board.addPlayerTeam("lor_" + UUID.randomUUID().toString().substring(0, 8));
            try {
                team.setAllowFriendlyFire(false);
                board.addPlayerToTeam(s.owner.getScoreboardName(), team);
                board.addPlayerToTeam(primary.getScoreboardName(), team);
                h.assertTrue(!s.spell.checkPreCastConditions(h.getLevel(), 1, s.owner, s.data), "Allies must not be locked");
                laser = s.laser(primary); fly(laser, 40);
                h.assertTrue(primary.getHealth() == 20, "Allies must not take damage from an in-flight laser");
                h.assertTrue(s.owner.getHealth() == s.owner.getMaxHealth(), "Backward flight must not hurt the caster");
            } finally { board.removePlayerTeam(team); }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void curvePrefixPreservesGeometryAndGuidedCannotSteer(GameTestHelper h) {
        var curve = new LockOnRayCurve(new Vec3(1, 2, 3), new Vec3(12, 5, 7), new Vec3(-4, 3, 2), new Vec3(8, 0, 0));
        var prefix = curve.prefix(0.25);
        for (int i = 0; i <= 10; i++) {
            double t = i / 10.0;
            h.assertTrue(prefix.position(t).distanceTo(curve.position(t * 0.25)) < 1.0e-8,
                    "Packet curve clipping must preserve the server collision path");
        }
        h.assertTrue(EntityRegistry.LOCK_ON_RAY_LASER.get().is(ModTags.GUIDING_BOLT_IMMUNE),
                "Guided must not override the Hermite trajectory");
        h.succeed();
    }

    private static void step(Entity entity) { entity.tickCount++; entity.tick(); }
    private static void fly(Entity entity, int ticks) {
        for (int i = 0; i < ticks && !entity.isRemoved(); i++) step(entity);
    }

    private static final class Scene implements AutoCloseable {
        final GameTestHelper helper;
        final Vec3 origin;
        final FakePlayer owner;
        final LockOnRay spell = (LockOnRay) SpellRegistry.LOCK_ON_RAY.get();
        final MagicData data;
        final List<Entity> entities = new ArrayList<>();

        Scene(GameTestHelper helper) {
            this.helper = helper;
            origin = helper.absoluteVec(new Vec3(2, 30, 2));
            owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "lock_ray_test"));
            owner.setPos(origin); owner.setYRot(-90); owner.setXRot(0); owner.setNoGravity(true); add(owner);
            data = MagicData.getPlayerMagicData(owner);
            // FakePlayerは通常ログインを通らないため、詠唱状態の同期データを明示初期化する。
            data.setSyncedData(new SyncedSpellData(owner));
        }

        void add(Entity entity) { entities.add(entity); helper.getLevel().addFreshEntity(entity); }
        Zombie zombie(double x, double z) {
            var zombie = EntityType.ZOMBIE.create(helper.getLevel());
            zombie.setPos(origin.add(x, 0, z)); zombie.setNoAi(true); zombie.setNoGravity(true);
            zombie.getAttribute(Attributes.ARMOR).setBaseValue(0); add(zombie); return zombie;
        }
        void begin() {
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, owner, data), "Scene target must be selectable");
            data.initiateCast(spell, 1, 100, CastSource.SPELLBOOK, "mainhand");
            spell.onServerPreCast(helper.getLevel(), 1, owner, data);
        }
        void castTick() { spell.onServerCastTick(helper.getLevel(), 1, owner, data); }
        LockOnRayLaserEntity laser(Entity target) {
            var laser = new LockOnRayLaserEntity(EntityRegistry.LOCK_ON_RAY_LASER.get(), helper.getLevel());
            laser.launch(owner, origin.add(0, 0.9, 0), target, 8); add(laser); return laser;
        }
        List<LockOnRayLaserEntity> lasers() {
            return helper.getLevel().getEntitiesOfClass(LockOnRayLaserEntity.class, owner.getBoundingBox().inflate(150))
                    .stream().filter(e -> e.getOwner() == owner && !e.isRemoved()).toList();
        }
        @Override public void close() {
            lasers().forEach(Entity::discard);
            data.resetCastingState(); data.resetAdditionalCastData();
            entities.forEach(Entity::discard);
        }
    }
}
