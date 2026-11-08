package jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.lightning;

import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.support.ArrowTestPositions;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.support.BowTestScope;
import jp.aquafactory.apprenticecodex.network.packet.LightningArrowImpactPacket;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.lightningarrow.LightningArrowEntity;
import jp.aquafactory.apprenticecodex.spell.lightningarrow.LightningArrowFlight;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class LightningArrowFlightGameTests {
    private LightningArrowFlightGameTests() {
    }

    private static final String TEMPLATE = "gametest/arrow_combat";
    private static final String BATCH = "apprenticecodex.lightning_arrow";

    @GameTest(template = "gametest/lightning_arrow_range", batch = BATCH)
    public static void flightMaintainsFiveBlocksInEveryDirectionAndStopsAtRange(GameTestHelper helper) {
        try (var scene = new LightningArrowFixture(helper)) {
            var origin = ArrowTestPositions.absolute(helper, new Vec3(64, 72, 64));
            for (var direction : List.of(new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                    new Vec3(0, 1, 0), new Vec3(0, -1, 0), new Vec3(1, 1, 1).normalize())) {
                var arrow = scene.arrow(origin, direction, 64);
                for (int tick = 1; tick <= 13; tick++) {
                    BowTestScope.tickOwned(arrow, false);
                    var expected = Math.min(64, tick * 5);
                    helper.assertTrue(Math.abs(arrow.traveledDistance() - expected) < 1.0e-4,
                            "Arrow must travel five blocks per tick and trim its final step");
                    helper.assertTrue(arrow.position().distanceTo(origin.add(direction.scale(expected))) < 1.0e-4,
                            "Arrow must remain straight without gravity or direction-dependent speed");
                }
                helper.assertTrue(arrow.stopped(), "Arrow must stop at its range");
                var end = arrow.position();
                for (int tick = 0; tick < LightningArrowEntity.TRAIL_TICKS; tick++)
                    BowTestScope.tickOwned(arrow, false);
                helper.assertTrue(arrow.isRemoved() && arrow.position().equals(end),
                        "Stopped arrow must remain stationary and expire after its trail");
                helper.assertFalse(arrow.shouldBeSaved(), "Lightning arrows must not persist in world saves");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void flightAndImpactDataRoundTripWithoutVelocityClamping(GameTestHelper helper) {
        for (var direction : List.of(new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(1, -1, 1))) {
            var flight = new LightningArrowFlight(new Vec3(12345678.125, 65.625, -12345678.375), direction, 5, 64, 123);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeNbt(flight.encode());
                var restored = LightningArrowFlight.decode(buffer.readNbt());
                helper.assertTrue(restored.origin().equals(flight.origin()) && restored.seed() == flight.seed(),
                        "Flight synchronization must preserve position precision and trail seed");
                helper.assertTrue(Math.abs(restored.point(5).distanceTo(restored.origin()) - 5) < 1.0e-7,
                        "Five-block motion must survive synchronization in every direction");
                helper.assertTrue(restored.point(100).distanceTo(flight.point(64)) < 1.0e-7,
                        "Client flight reconstruction must clamp to the range");
            } finally {
                buffer.release();
            }
        }
        var packet = new LightningArrowImpactPacket(new Vec3(1, 2, 3), new Vec3(0, 0, 1), true);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            LightningArrowImpactPacket.STREAM_CODEC.encode(buffer, packet);
            helper.assertTrue(packet.equals(LightningArrowImpactPacket.STREAM_CODEC.decode(buffer)),
                    "Impact synchronization must preserve its position, direction and terrain flag");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void castingUsesArrowOffsetAndServerTicksAdvanceTheArrow(GameTestHelper helper) {
        var scene = new LightningArrowFixture(helper);
        scene.owner.setPos(ArrowTestPositions.absolute(helper, new Vec3(2.5, 20, 2.5)));
        scene.owner.setYRot(0);
        scene.owner.setXRot(0);
        var eye = scene.owner.getEyePosition();
        SpellRegistry.LIGHTNING_ARROW.get().onCast(helper.getLevel(), 1, scene.owner,
                CastSource.SPELLBOOK, MagicData.getPlayerMagicData(scene.owner));
        var arrows = scene.scope.entities(LightningArrowEntity.class);
        helper.assertTrue(arrows.size() == 1, "A completed cast must spawn exactly one arrow");
        var arrow = arrows.getFirst();
        scene.entities.add(arrow);
        helper.assertTrue(arrow.position().distanceTo(eye.add(0, -0.4, 1)) < 1.0e-7,
                "The cast must start one block forward and 0.4 blocks below the eye");
        helper.runAfterDelay(2, () -> {
            try (scene) {
                helper.assertTrue(arrow.traveledDistance() >= 5 && arrow.traveledDistance() <= 15,
                        "The registered projectile must advance during normal server ticks");
                helper.assertTrue(arrow.flight().range() == 64, "Casting must use the spell range");
            }
            helper.succeed();
        });
    }
}
