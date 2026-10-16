package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsEffect;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsEffect.Kind;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsPose;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsRaycast;
import jp.aquafactory.apprenticecodex.network.packet.PsychicDriveGauntletsEffectPacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // GameTestはアノテーション走査で登録される。
public final class PsychicDriveGauntletsEffectGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private PsychicDriveGauntletsEffectGameTests() {}

    private static FakePlayer player(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(1, 18, 1), "psychic_effect");
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.PSYCHIC_DRIVE_GAUNTLETS.get()));
        Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(4);
        player.setYRot(0);
        player.setXRot(0);
        player.yBodyRot = 0;
        Objects.requireNonNull(player.getAttribute(AttributeRegistry.MAX_MANA)).setBaseValue(100000);
        MagicData.getPlayerMagicData(player).setMana(100000);
        return player;
    }

    private static Cow target(GameTestHelper helper, FakePlayer player) {
        var cow = Objects.requireNonNull(EntityType.COW.create(helper.getLevel()));
        cow.setPos(PsychicDriveGauntletsPose.position(player, 1, true).add(0, -0.5, 2));
        cow.setNoAi(true);
        cow.setNoGravity(true);
        Objects.requireNonNull(cow.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(100);
        cow.setHealth(100);
        helper.getLevel().addFreshEntity(cow);
        return cow;
    }

    private static PsychicDriveGauntletsArm launch(FakePlayer player, float charge) {
        var arm = new PsychicDriveGauntletsArm(true);
        arm.launch(player, PsychicDriveGauntletsPose.position(player, 1, true).add(0, 0, 12),
                1, 0, InteractionHand.MAIN_HAND, charge);
        return arm;
    }

    private static List<Kind> kinds(PsychicDriveGauntletsArm arm) {
        return arm.drainEffects().stream().map(PsychicDriveGauntletsEffect::kind).toList();
    }

    @GameTest(template = TEMPLATE)
    public static void normalLaunchWaitsForAccelerationAndContactOccursOnce(GameTestHelper helper) {
        var player = player(helper);
        var cow = target(helper, player);
        var arm = new PsychicDriveGauntletsArm(true);
        var aim = new PsychicDriveGauntletsRaycast.Aim(null,
                PsychicDriveGauntletsPose.position(player, 1, true).add(0, 0, 6), false);
        arm.prepare(player, aim, 1, 0);
        arm.tick(player, 1, 1);
        helper.assertTrue(kinds(arm).isEmpty(), "Preparation must not emit a launch sound");
        arm.tick(player, 1, 2);
        helper.assertTrue(kinds(arm).equals(List.of(Kind.LAUNCH)), "Acceleration must emit exactly one launch");
        var events = new ArrayList<Kind>();
        for (int tick = 3; tick <= 15; tick++) {
            arm.tick(player, 1, tick);
            events.addAll(kinds(arm));
        }
        helper.assertTrue(events.equals(List.of(Kind.IMPACT)), "A normal strike must emit only one successful impact");
        helper.assertTrue(cow.getHealth() == 96 && arm.available(), "Effects must preserve damage and recovery");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void chargeReadyIsOncePerHoldAndCancellationDoesNotLaunch(GameTestHelper helper) {
        var player = player(helper);
        var state = new PsychicDriveGauntletsAttackState();
        helper.assertTrue(state.beginUse(player, InteractionHand.MAIN_HAND, 0), "Charge fixture must start");
        helper.assertTrue(kinds(state.left()).equals(List.of(Kind.CHARGE_START)), "Accepted charge must emit its start once");
        for (int tick = 1; tick < 20; tick++) state.tick(player, tick);
        helper.assertTrue(kinds(state.left()).isEmpty(), "Partial charge must not notify readiness");
        state.tick(player, 20);
        helper.assertTrue(kinds(state.left()).equals(List.of(Kind.CHARGE_READY)), "Maximum charge must notify once");
        for (int tick = 21; tick <= 30; tick++) state.tick(player, tick);
        helper.assertTrue(kinds(state.left()).isEmpty(), "Holding maximum charge must not repeat the notification");
        helper.assertTrue(state.releaseUse(player, 30), "Full charge fixture must release");
        helper.assertTrue(kinds(state.left()).equals(List.of(Kind.LAUNCH)), "Release must launch without repeating readiness");
        state.left().reset();
        state.beginUse(player, InteractionHand.MAIN_HAND, 40);
        kinds(state.left());
        state.tick(player, 60);
        helper.assertTrue(kinds(state.left()).equals(List.of(Kind.CHARGE_READY)), "A new hold must be able to notify again");
        state.cancelUse();
        helper.assertTrue(kinds(state.left()).isEmpty(), "Cancellation must not emit a launch");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void earlyAndPartialReleaseHaveNoFullChargeEffects(GameTestHelper helper) {
        var player = player(helper);
        var state = new PsychicDriveGauntletsAttackState();
        state.beginUse(player, InteractionHand.MAIN_HAND, 0);
        kinds(state.left());
        helper.assertTrue(!state.releaseUse(player, 4) && kinds(state.left()).isEmpty(),
                "Release before five ticks must not launch");
        state.beginUse(player, InteractionHand.MAIN_HAND, 10);
        kinds(state.left());
        state.tick(player, 20);
        helper.assertTrue(state.releaseUse(player, 20), "Partial charge fixture must release");
        var effects = state.left().drainEffects();
        helper.assertTrue(effects.size() == 1 && effects.getFirst().kind() == Kind.LAUNCH
                        && effects.getFirst().charge() == 0.5F,
                "Partial release must use the normal launch event and preserve charge");
        var cow = target(helper, player);
        var partial = launch(player, 0.5F);
        kinds(partial);
        for (int tick = 1; tick <= 15; tick++) partial.tick(player, 1, tick);
        helper.assertTrue(kinds(partial).equals(List.of(Kind.IMPACT)) && cow.getHealth() == 96,
                "Partial charge must retain a single normal impact without drill feedback");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void drillEffectsFollowSuccessfulDamageAndFinalKnockback(GameTestHelper helper) {
        var player = player(helper);
        var cow = target(helper, player);
        var arm = launch(player, 1);
        helper.assertTrue(kinds(arm).equals(List.of(Kind.LAUNCH)), "Full release must launch once");
        var events = new ArrayList<Kind>();
        for (int tick = 1; tick <= 18; tick++) {
            arm.tick(player, 1, tick);
            events.addAll(kinds(arm));
        }
        helper.assertTrue(events.equals(List.of(Kind.DRILL_START, Kind.DRILL_TICK, Kind.DRILL_TICK, Kind.FINISH)),
                "Only four successful drill hits may produce the final effect");
        helper.assertTrue(cow.getHealth() == 84 && cow.getDeltaMovement().z >= 0.49 && arm.available(),
                "Presentation must preserve four hits, final knockback and return timing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void departureAndRejectedFinalHitNeverExplode(GameTestHelper helper) {
        var player = player(helper);
        var cow = target(helper, player);
        var arm = launch(player, 1);
        arm.tick(player, 1, 1);
        kinds(arm);
        cow.setPos(cow.position().add(3, 0, 0));
        for (int tick = 2; tick <= 18; tick++) arm.tick(player, 1, tick);
        helper.assertTrue(kinds(arm).isEmpty(), "Departure must not schedule intermediate or final effects");
        cow.discard();
        cow = target(helper, player);
        arm = launch(player, 1);
        for (int tick = 1; tick <= 9; tick++) arm.tick(player, 1, tick);
        kinds(arm);
        cow.setInvulnerable(true);
        for (int tick = 10; tick <= 18; tick++) arm.tick(player, 1, tick);
        helper.assertTrue(kinds(arm).isEmpty() && cow.getHealth() == 88,
                "Rejected final damage must not emit a successful finish");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rejectedInitialHitOnlyEmitsContactAndBlockNeverDrills(GameTestHelper helper) {
        var player = player(helper);
        var cow = target(helper, player);
        cow.setInvulnerable(true);
        var arm = launch(player, 1);
        kinds(arm);
        arm.tick(player, 1, 1);
        helper.assertTrue(kinds(arm).equals(List.of(Kind.REJECTED)) && cow.getHealth() == 100,
                "Rejected initial damage must only emit weak contact feedback");
        cow.discard();
        var block = new PsychicDriveGauntletsArm(true);
        block.launch(player, new PsychicDriveGauntletsRaycast.Aim(null,
                PsychicDriveGauntletsPose.position(player, 1, true).add(0, 0, 2), true),
                1, 0, InteractionHand.MAIN_HAND, 1);
        kinds(block);
        for (int tick = 1; tick <= 15; tick++) block.tick(player, 1, tick);
        helper.assertTrue(kinds(block).equals(List.of(Kind.BLOCK_IMPACT)) && block.available(),
                "Full block impact must remain one contact without drill or finish effects");
        var miss = launch(player, 1);
        kinds(miss);
        for (int tick = 1; tick <= 18; tick++) miss.tick(player, 1, tick);
        helper.assertTrue(kinds(miss).isEmpty(), "Misses must never emit contact or explosion effects");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void effectPacketsPreserveBothSidesAndAllStages(GameTestHelper helper) {
        var player = player(helper);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess(), ConnectionType.NEOFORGE);
        try {
            for (boolean right : new boolean[] {false, true}) {
                for (var kind : Kind.values()) {
                    var effect = new PsychicDriveGauntletsEffect(right, kind, player.position(),
                            new Vec3(0.25, -0.5, 0.75), 0.75F);
                    var packet = new PsychicDriveGauntletsEffectPacket(player.getUUID(), effect);
                    PsychicDriveGauntletsEffectPacket.STREAM_CODEC.encode(buffer, packet);
                    helper.assertTrue(packet.equals(PsychicDriveGauntletsEffectPacket.STREAM_CODEC.decode(buffer)),
                            "Effect payload must preserve player, side, stage, position, direction and charge");
                }
            }
            helper.assertTrue(buffer.readableBytes() == 0, "Effect decoding must consume all bytes");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void soundDefinitionsAreRegisteredAndHaveDistinctSubtitles(GameTestHelper helper) throws Exception {
        try (var stream = Objects.requireNonNull(PsychicDriveGauntletsEffectGameTests.class.getResourceAsStream(
                "/assets/apprenticecodex/sounds.json"));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var definitions = JsonParser.parseReader(reader).getAsJsonObject();
            var subtitles = new HashSet<String>();
            for (var suffix : List.of("launch", "impact", "charge_start", "charge_ready", "full_launch", "full_impact", "finish")) {
                var name = "psychic_drive_gauntlets_" + suffix;
                var id = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, name);
                helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(id), "Sound must be registered: " + id);
                var definition = definitions.getAsJsonObject(name);
                var subtitle = definition.get("subtitle").getAsString();
                helper.assertTrue(subtitle.equals("subtitle.apprenticecodex.sound." + name) && subtitles.add(subtitle),
                        "Gauntlet sounds must have dedicated, distinct subtitles");
                helper.assertTrue(definition.getAsJsonArray("sounds").size() == 1, "Each sound must start with one vanilla source");
            }
            var ready = definitions.getAsJsonObject("psychic_drive_gauntlets_charge_ready")
                    .getAsJsonArray("sounds").get(0).getAsJsonObject();
            helper.assertTrue(ready.get("name").getAsString().equals("minecraft:random/orb")
                            && ready.get("pitch").getAsFloat() == 0.7F && ready.get("volume").getAsFloat() == 0.8F,
                    "Readiness must retain the broom notification source, pitch and volume");
        }
        helper.succeed();
    }
}
