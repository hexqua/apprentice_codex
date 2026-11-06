package jp.aquafactory.apprenticecodex.gametest.spell.assistwings;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import jp.aquafactory.apprenticecodex.network.packet.HoverrideBroomAssistWingsJumpPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncAssistWingsJumpPacket;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.spell.assistwings.AssistWingsWingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

public final class AssistWingsScenarios {
    private AssistWingsScenarios() {}

    static void assistWingsOnlyJumpItemsTagIncludesSmashcastScepter(GameTestHelper helper) {
        helper.succeedIf(() -> helper.assertTrue(
                new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get()).is(TagRegistry.Items.ASSIST_WINGS_ONLY_JUMP_ITEMS),
                "Smashcast Scepter should be tagged as an Assist Wings only-jump item"
        ));
    }

    static void assistWingsRegularAirCastPreservesHorizontalMovementWithoutSelfMotionSync(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createAssistWingsPlayer(helper, new BlockPos(0, 4, 0), "assist_wings_regular_air_test");
            player.setOnGround(false);
            player.setDeltaMovement(0.12D, -0.2D, -0.08D);
            player.fallDistance = 6.0F;

            var spell = SpellRegistry.ASSIST_WINGS.get();
            var magicData = MagicData.getPlayerMagicData(player);
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            var movement = player.getDeltaMovement();
            helper.assertTrue(Math.abs(movement.x - 0.12D) < 0.0001D
                            && movement.y > 0.59D
                            && Math.abs(movement.z + 0.08D) < 0.0001D,
                    "Assist Wings should preserve horizontal movement while applying the regular air jump");
            helper.assertTrue(player.hasImpulse,
                    "Assist Wings should mark the regular air jump as an impulse");
            helper.assertFalse(player.hurtMarked,
                    "Assist Wings should not force a full velocity sync back to the casting ServerPlayer");
            helper.assertTrue(player.fallDistance == 0.0F,
                    "Assist Wings should reset fall distance on a regular air jump");
            helper.assertTrue(getAssistWingsDoneJump(player) == 1,
                    "Regular air casts should consume one Assist Wings air jump");
            helper.assertTrue(countActiveAssistWingsWings(helper, player) == 1,
                    "Regular Assist Wings casts should keep one wing entity");
            helper.assertTrue(magicData.getAdditionalCastData() == null,
                    "Assist Wings should not depend on resettable additional cast data for client movement");
            assertAssistWingsJumpPacketRoundTrips(helper, player);
        });
    }

    static void assistWingsTaggedGroundCastKeepsWingAndBlocksFallProtection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createAssistWingsPlayer(helper, new BlockPos(0, 2, 0), "assist_wings_smashcast_ground_test");
            player.setOnGround(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get()));
            player.setDeltaMovement(0.12D, -0.2D, -0.08D);
            player.fallDistance = 5.0F;

            var spell = SpellRegistry.ASSIST_WINGS.get();
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Assist Wings should allow a ground cast with Smashcast Scepter");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

            var movement = player.getDeltaMovement();
            helper.assertTrue(Math.abs(movement.x - 0.12D) < 0.0001D
                            && movement.y > 0.59D
                            && Math.abs(movement.z + 0.08D) < 0.0001D,
                    "Assist Wings should preserve horizontal movement during a Smashcast Scepter ground jump");
            helper.assertFalse(player.hurtMarked,
                    "Smashcast Scepter ground jumps should not force a full velocity sync back to the casting ServerPlayer");
            helper.assertTrue(player.fallDistance == 0.0F,
                    "Assist Wings should reset fall distance at takeoff");
            helper.assertTrue(getAssistWingsDoneJump(player) == 0,
                    "Ground casts with a tagged item should not consume an air jump");
            helper.assertTrue(countActiveAssistWingsWings(helper, player) == 1,
                    "Assist Wings should keep its wing entity while a tagged item is held");

            var state = Capabilities.getSpellDataOrNull(player).get(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE);
            if (!(helper.getLevel().getEntity(state.localEntityId) instanceof AssistWingsWingEntity wing)) {
                helper.fail("Assist Wings should register its wing entity in spell state");
                return;
            }
            wing.tickOnServer(helper.getLevel());
            helper.assertTrue(wing.isFallProtectionBlocked(),
                    "A tagged main-hand item should block Assist Wings fall protection");
            helper.assertFalse(player.hasEffect(MobEffects.SLOW_FALLING),
                    "Assist Wings should not use the vanilla Slow Falling effect");
            helper.assertTrue(magicData.getAdditionalCastData() == null,
                    "Assist Wings should not depend on resettable additional cast data for client movement");
        });
    }

    static void assistWingsTaggedOffhandBlocksFallProtectionWithoutDiscardingWing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createAssistWingsPlayer(helper, new BlockPos(0, 4, 0), "assist_wings_offhand_protection_test");
            player.setOnGround(false);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get()));
            player.setDeltaMovement(0.12D, -0.2D, -0.08D);
            player.fallDistance = 7.0F;

            var wing = new AssistWingsWingEntity(EntityRegistry.ASSIST_WINGS_WING.get(), helper.getLevel(), player);
            helper.getLevel().addFreshEntity(wing);
            setAssistWingsState(player, 1, wing.getId());
            wing.tickOnServer(helper.getLevel());

            var movement = player.getDeltaMovement();
            helper.assertTrue(Math.abs(movement.x - 0.12D) < 0.0001D
                            && Math.abs(movement.y + 0.2D) < 0.0001D
                            && Math.abs(movement.z + 0.08D) < 0.0001D,
                    "A tagged offhand item should leave falling movement unchanged");
            helper.assertTrue(player.fallDistance == 7.0F,
                    "A tagged offhand item should preserve accumulated fall distance");
            helper.assertTrue(wing.isFallProtectionBlocked(),
                    "A tagged offhand item should block Assist Wings fall protection");
            helper.assertFalse(wing.isRemoved(),
                    "Blocking fall protection from the offhand should not discard the wing");
        });
    }

    static void assistWingsRemovingTaggedItemRestoresFallProtectionWithoutSlowingFall(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createAssistWingsPlayer(helper, new BlockPos(0, 4, 0), "assist_wings_restore_protection_test");
            player.setOnGround(false);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get()));

            var wing = new AssistWingsWingEntity(EntityRegistry.ASSIST_WINGS_WING.get(), helper.getLevel(), player);
            helper.getLevel().addFreshEntity(wing);
            setAssistWingsState(player, 1, wing.getId());
            wing.tickOnServer(helper.getLevel());
            helper.assertTrue(wing.isFallProtectionBlocked(),
                    "The tagged main-hand item should initially block fall protection");

            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setDeltaMovement(0.12D, -0.3D, -0.08D);
            player.fallDistance = 8.0F;
            player.setShiftKeyDown(true);
            wing.tickOnServer(helper.getLevel());

            var movement = player.getDeltaMovement();
            helper.assertTrue(Math.abs(movement.x - 0.12D) < 0.0001D
                            && Math.abs(movement.y + 0.3D) < 0.0001D
                            && Math.abs(movement.z + 0.08D) < 0.0001D,
                    "Assist Wings fall protection should not change movement while sneaking");
            helper.assertTrue(player.fallDistance == 0.0F,
                    "Restored fall protection should reset accumulated fall distance while sneaking");
            helper.assertFalse(wing.isFallProtectionBlocked(),
                    "Removing the tagged item should restore the normal wing state");
            helper.assertFalse(player.hasEffect(MobEffects.SLOW_FALLING),
                    "Assist Wings fall protection should not add the vanilla Slow Falling effect");

            player.setShiftKeyDown(false);
            player.setDeltaMovement(-0.07D, -0.45D, 0.09D);
            player.fallDistance = 12.0F;
            wing.tickOnServer(helper.getLevel());
            movement = player.getDeltaMovement();
            helper.assertTrue(Math.abs(movement.x + 0.07D) < 0.0001D
                            && Math.abs(movement.y + 0.45D) < 0.0001D
                            && Math.abs(movement.z - 0.09D) < 0.0001D,
                    "Assist Wings fall protection should not change regular falling movement");
            helper.assertTrue(player.fallDistance == 0.0F,
                    "Assist Wings should reset regular accumulated fall distance");
        });
    }

    static void assistWingsTaggedLandingResetsAirJumpsAndAllowsNextAirCast(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createAssistWingsPlayer(helper, new BlockPos(0, 2, 0), "assist_wings_tagged_landing_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get()));

            var wing = new AssistWingsWingEntity(EntityRegistry.ASSIST_WINGS_WING.get(), helper.getLevel(), player);
            helper.getLevel().addFreshEntity(wing);
            setAssistWingsState(player, 2, wing.getId());

            var spell = SpellRegistry.ASSIST_WINGS.get();
            var magicData = MagicData.getPlayerMagicData(player);
            player.setOnGround(false);
            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Tagged items should not bypass the normal Assist Wings air-jump limit");

            player.setOnGround(true);
            for (var i = 0; i < 10; ++i) {
                wing.tickOnServer(helper.getLevel());
            }

            helper.assertFalse(wing.isRemoved(),
                    "The wing should ignore landing during its first 10 ticks");
            helper.assertTrue(getAssistWingsDoneJump(player) == 2,
                    "Landing during the removal grace should preserve Assist Wings air jumps");

            wing.tickOnServer(helper.getLevel());
            helper.assertTrue(wing.isRemoved(),
                    "The wing should detect landing after its 10 tick removal grace");
            helper.assertTrue(getAssistWingsDoneJump(player) == 0,
                    "Landing with a tagged item should reset Assist Wings air jumps");

            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setOnGround(false);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "A normal midair Assist Wings cast should be available after the tagged landing reset");
        });
    }

    static void assistWingsWaterRemovalUsesGraceAndResetsAirJumps(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var waterPos = new BlockPos(0, 2, 0);
            helper.setBlock(waterPos, Blocks.WATER);

            var submergedPlayer = createAssistWingsPlayer(helper, waterPos, "assist_wings_water_removal_test");
            submergedPlayer.setOnGround(false);
            var submergedWing = new AssistWingsWingEntity(
                    EntityRegistry.ASSIST_WINGS_WING.get(), helper.getLevel(), submergedPlayer);
            helper.getLevel().addFreshEntity(submergedWing);
            setAssistWingsState(submergedPlayer, 2, submergedWing.getId());

            for (var i = 0; i < 10; ++i) {
                submergedWing.tickOnServer(helper.getLevel());
            }

            helper.assertFalse(submergedWing.isRemoved(),
                    "Water contact should not remove Assist Wings during its first 10 ticks");
            helper.assertTrue(getAssistWingsDoneJump(submergedPlayer) == 2,
                    "Water contact during the removal grace should preserve Assist Wings air jumps");

            submergedWing.tickOnServer(helper.getLevel());
            helper.assertTrue(submergedWing.isRemoved(),
                    "Water contact should remove Assist Wings after its 10 tick removal grace");
            var submergedState = Capabilities.getSpellDataOrNull(submergedPlayer)
                    .get(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE);
            helper.assertTrue(submergedState.localEntityId == -1 && submergedState.doneJump == 0,
                    "Water removal should clear the managed wing and reset Assist Wings air jumps");

            var leavingWaterPos = new BlockPos(3, 2, 0);
            helper.setBlock(leavingWaterPos, Blocks.WATER);
            var leavingPlayer = createAssistWingsPlayer(helper, leavingWaterPos, "assist_wings_leave_water_test");
            leavingPlayer.setOnGround(false);
            var leavingWing = new AssistWingsWingEntity(
                    EntityRegistry.ASSIST_WINGS_WING.get(), helper.getLevel(), leavingPlayer);
            helper.getLevel().addFreshEntity(leavingWing);
            setAssistWingsState(leavingPlayer, 1, leavingWing.getId());

            for (var i = 0; i < 5; ++i) {
                leavingWing.tickOnServer(helper.getLevel());
            }
            var dryPos = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(6, 4, 0)));
            leavingPlayer.setPos(dryPos.x, dryPos.y, dryPos.z);
            for (var i = 0; i < 6; ++i) {
                leavingWing.tickOnServer(helper.getLevel());
            }

            helper.assertFalse(leavingWing.isRemoved(),
                    "Leaving water during the removal grace should keep Assist Wings active");
            helper.assertTrue(getAssistWingsDoneJump(leavingPlayer) == 1,
                    "Leaving water during the removal grace should preserve Assist Wings air jumps");
        });
    }

    static void assistWingsLavaContactDoesNotRemoveWing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var lavaPos = new BlockPos(0, 2, 0);
            helper.setBlock(lavaPos, Blocks.LAVA);
            var player = createAssistWingsPlayer(helper, lavaPos, "assist_wings_lava_contact_test");
            player.setOnGround(false);
            var wing = new AssistWingsWingEntity(EntityRegistry.ASSIST_WINGS_WING.get(), helper.getLevel(), player);
            helper.getLevel().addFreshEntity(wing);
            setAssistWingsState(player, 2, wing.getId());

            for (var i = 0; i < 11; ++i) {
                wing.tickOnServer(helper.getLevel());
            }

            helper.assertFalse(wing.isRemoved(),
                    "Lava contact should not remove Assist Wings");
            helper.assertTrue(getAssistWingsDoneJump(player) == 2,
                    "Lava contact should not reset Assist Wings air jumps");
        });
    }

    static void assistWingsSuccessfulCastRestartsRemovalGrace(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createAssistWingsPlayer(helper, new BlockPos(0, 2, 0), "assist_wings_grace_restart_test");
            player.setOnGround(true);
            var wing = new AssistWingsWingEntity(EntityRegistry.ASSIST_WINGS_WING.get(), helper.getLevel(), player);
            helper.getLevel().addFreshEntity(wing);
            setAssistWingsState(player, 1, wing.getId());

            for (var i = 0; i < 8; ++i) {
                wing.tickOnServer(helper.getLevel());
            }
            wing.restartRemovalGrace();
            for (var i = 0; i < 10; ++i) {
                wing.tickOnServer(helper.getLevel());
            }

            helper.assertFalse(wing.isRemoved(),
                    "A successful Assist Wings recast should restart the full 10 tick removal grace");
            wing.tickOnServer(helper.getLevel());
            helper.assertTrue(wing.isRemoved(),
                    "Landing should remove Assist Wings after the restarted grace expires");
            helper.assertTrue(getAssistWingsDoneJump(player) == 0,
                    "Landing after a restarted grace should reset Assist Wings air jumps");
        });
    }

    static void assistWingsRejectsOtherBroomsWithoutChangingState(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createAssistWingsRider(helper, new BlockPos(0, 4, 0), "assist_wings_other_mount_test");
            player.setOnGround(false);
            setAssistWingsState(player, 1, -1);
            var broom = EntityRegistry.FLOATMOUNT_BROOM.get().create(helper.getLevel());
            if (broom == null) {
                helper.fail("Floatmount broom should be available for the Assist Wings mount rejection test");
                return;
            }
            broom.setPos(player.position());
            helper.getLevel().addFreshEntity(broom);
            helper.assertTrue(player.startRiding(broom, true),
                    "Assist Wings mount rejection test rider should mount the Floatmount broom");

            var spell = SpellRegistry.ASSIST_WINGS.get();
            helper.assertFalse(spell.checkPreCastConditions(
                            helper.getLevel(), 1, player, MagicData.getPlayerMagicData(player)),
                    "Assist Wings should reject vehicles other than Hoverride broom");
            helper.assertTrue(getAssistWingsDoneJump(player) == 1,
                    "Rejecting another vehicle should preserve Assist Wings air jumps");
            helper.assertTrue(countActiveAssistWingsWings(helper, player) == 0,
                    "Rejecting another vehicle should not summon Assist Wings");

            var wing = new AssistWingsWingEntity(EntityRegistry.ASSIST_WINGS_WING.get(), helper.getLevel(), player);
            helper.getLevel().addFreshEntity(wing);
            setAssistWingsState(player, 1, wing.getId());
            broom.setOnGround(true);
            for (var i = 0; i < 11; ++i) {
                wing.tickOnServer(helper.getLevel());
            }
            helper.assertTrue(wing.isRemoved() && getAssistWingsDoneJump(player) == 0,
                    "A grounded vehicle should remove an existing wing and reset its air jumps");
        });
    }

    static void assistWingsHoverrideUsesSurfaceAndAirJumpCounts(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
            var broom = new HoverrideBroomEntity(EntityRegistry.HOVERRIDE_BROOM.get(), helper.getLevel());
            var surfacePosition = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 3, 0)));
            broom.setPos(surfacePosition.x, surfacePosition.y, surfacePosition.z);
            helper.getLevel().addFreshEntity(broom);

            var player = createAssistWingsRider(helper, new BlockPos(0, 3, 0), "assist_wings_hoverride_test");
            player.setOnGround(false);
            helper.assertTrue(player.startRiding(broom, true),
                    "Assist Wings Hoverride test rider should mount");
            helper.assertTrue(broom.isWithinAssistWingsLandingDistance(),
                    "Hoverride broom should detect the nearby ride surface for Assist Wings");

            var spell = SpellRegistry.ASSIST_WINGS.get();
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.setMana(100.0F);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "A healthy Hoverride broom should accept Assist Wings near the surface");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
            helper.assertTrue(getAssistWingsDoneJump(player) == 0,
                    "A Hoverride Assist Wings cast within 1.5 blocks of a surface should be free");
            helper.assertTrue(player.getDeltaMovement().y == 0.0D,
                    "Hoverride Assist Wings should not apply the jump directly to the rider");

            var state = Capabilities.getSpellDataOrNull(player)
                    .get(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE);
            if (!(helper.getLevel().getEntity(state.localEntityId) instanceof AssistWingsWingEntity wing)) {
                helper.fail("Hoverride Assist Wings should summon a managed wing entity");
                return;
            }
            for (var i = 0; i < 10; ++i) {
                wing.tickOnServer(helper.getLevel());
            }
            helper.assertFalse(wing.isRemoved(),
                    "The Hoverride surface should not remove Assist Wings during the cast grace");
            wing.tickOnServer(helper.getLevel());
            helper.assertTrue(wing.isRemoved(),
                    "The Hoverride surface should remove Assist Wings after the cast grace");

            var airbornePosition = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 20, 0)));
            broom.setPos(airbornePosition.x, airbornePosition.y, airbornePosition.z);
            player.setOnGround(false);
            helper.assertFalse(broom.isWithinAssistWingsLandingDistance(),
                    "Hoverride broom should not treat a distant surface as an Assist Wings landing");
            for (var i = 0; i < 40; ++i) {
                broom.tick();
            }
            helper.assertTrue(broom.isAirborneAccelerationLocked(),
                    "Hoverride broom should lock airborne acceleration after losing the ride surface");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
            helper.assertFalse(broom.isAirborneAccelerationLocked(),
                    "A successful Hoverride Assist Wings jump should reset the airborne acceleration lock");
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
            helper.assertTrue(getAssistWingsDoneJump(player) == 2,
                    "Two airborne Hoverride casts should consume both level 1 air jumps");
            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "A fourth level 1 Hoverride jump should be rejected after the free jump and two air jumps");

            assertHoverrideAssistWingsJumpPacketRoundTrips(helper, broom);

            magicData.setMana(0.0F);
            broom.tick();
            helper.assertTrue(broom.isManaDepleted(),
                    "Hoverride broom should enter mana-depleted mode before testing Assist Wings rejection");
            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Assist Wings should reject a mana-depleted Hoverride broom without changing jump state");
            helper.assertTrue(getAssistWingsDoneJump(player) == 2,
                    "Rejecting a mana-depleted Hoverride broom should preserve Assist Wings air jumps");
        });
    }

    static FakePlayer createAssistWingsPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), profileName));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    public static ServerPlayer createAssistWingsRider(GameTestHelper helper, BlockPos pos, String profileName) {
        var level = helper.getLevel();
        var server = level.getServer();
        var profile = new GameProfile(UUID.randomUUID(), profileName);
        var cookie = CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(server, level, profile, cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        new ServerGamePacketListenerImpl(server, connection, player, cookie);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        return player;
    }

    static int getAssistWingsDoneJump(Player player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return -1;
        }
        return spellData.get(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE).doneJump;
    }

    public static void setAssistWingsState(Player player, int doneJump, int localEntityId) {
        Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE, state -> {
            state.doneJump = doneJump;
            state.localEntityId = localEntityId;
        }));
    }

    static void assertAssistWingsJumpPacketRoundTrips(GameTestHelper helper, Player player) {
        var source = new SyncAssistWingsJumpPacket(0.75F);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        SyncAssistWingsJumpPacket.encode(source, buffer);
        var restored = SyncAssistWingsJumpPacket.decode(buffer);
        helper.assertTrue(restored.jumpHeight() == source.jumpHeight(),
                "Assist Wings jump packet should preserve jump height through network serialization");

        player.setDeltaMovement(0.18D, -0.4D, -0.11D);
        player.fallDistance = 8.0F;
        SyncAssistWingsJumpPacket.applyTo(player, restored.jumpHeight());
        var movement = player.getDeltaMovement();
        helper.assertTrue(Math.abs(movement.x - 0.18D) < 0.0001D
                        && Math.abs(movement.y - 0.75D) < 0.0001D
                        && Math.abs(movement.z + 0.11D) < 0.0001D,
                "Assist Wings jump packet should replace only vertical movement");
        helper.assertTrue(player.hasImpulse,
                "Assist Wings jump packet should mark the client movement as an impulse");
        helper.assertTrue(player.fallDistance == 0.0F,
                "Assist Wings jump packet should reset client fall distance");
    }

    static void assertHoverrideAssistWingsJumpPacketRoundTrips(
            GameTestHelper helper,
            HoverrideBroomEntity broom
    ) {
        var source = new HoverrideBroomAssistWingsJumpPacket(broom.getId(), 0.75F);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        HoverrideBroomAssistWingsJumpPacket.encode(buffer, source);
        var restored = HoverrideBroomAssistWingsJumpPacket.decode(buffer);
        helper.assertTrue(restored.equals(source),
                "Hoverride Assist Wings packet should preserve entity id and jump height");

        broom.setDeltaMovement(0.18D, -0.4D, -0.11D);
        broom.acceptLocalAssistWingsJump(restored.jumpHeight());
        var movement = broom.getDeltaMovement();
        helper.assertTrue(broom.isLocalAssistWingsJumpActive(),
                "Hoverride Assist Wings packet data should activate the local jump");
        helper.assertTrue(Math.abs(movement.x - 0.18D) < 0.0001D
                        && Math.abs(movement.y - 0.75D) < 0.0001D
                        && Math.abs(movement.z + 0.11D) < 0.0001D,
                "Hoverride Assist Wings should replace only local vertical movement");

        var sanitized = new HoverrideBroomAssistWingsJumpPacket(broom.getId(), Float.NaN);
        helper.assertTrue(sanitized.jumpHeight() == 0.0F,
                "Hoverride Assist Wings packet should reject non-finite jump heights");
    }

    static int countActiveAssistWingsWings(GameTestHelper helper, Player player) {
        return helper.getLevel().getEntitiesOfClass(
                AssistWingsWingEntity.class,
                new AABB(player.position(), player.position()).inflate(16.0D),
                wing -> !wing.isRemoved()
                        && wing.getOwner() != null
                        && wing.getOwner().getUUID().equals(player.getUUID())
        ).size();
    }
}
