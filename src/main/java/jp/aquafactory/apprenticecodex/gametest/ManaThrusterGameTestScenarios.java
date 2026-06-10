package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThruster;
import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThrusterFlightManager;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

final class ManaThrusterGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final TagKey<Item> CURIOS_FEET = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.FEET)
    );

    private ManaThrusterGameTestScenarios() {
    }

    static void manaThrusterUsesFeetSlotAndDedicatedImplementation(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MANA_THRUSTER.get());
            helper.assertTrue(stack.is(CURIOS_FEET),
                    "Mana Thruster should be tagged for the Curios feet slot");
            helper.assertTrue(stack.getItem() instanceof ManaThruster,
                    "Mana Thruster should resolve to the dedicated curio item implementation");
        });
    }

    static void manaThrusterAppliesFixedThrustAndUsesServerConfigManaCost(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createManaThrusterTestPlayer(helper, "mana_thruster_config_test");
            var magicData = magicData(helper, player, "config");
            magicData.setMana(50.0F);
            player.setOnGround(false);
            player.setDeltaMovement(new Vec3(0.0D, 0.20D, 0.0D));
            player.fallDistance = 5.0F;

            try (var ignored = ApprenticeCodexServerConfig.useManaThrusterConfigOverrideForGameTest(7.0D)) {
                ManaThrusterFlightManager.setJumpInput(player, true);
                ManaThrusterFlightManager.tickEquippedPlayer(player);
            }

            helper.assertTrue(player.getDeltaMovement().y > 0.20D,
                    "Mana Thruster should apply fixed upward thrust: " + player.getDeltaMovement().y);
            helper.assertTrue(Math.abs(magicData.getMana() - 43.0F) < 1.0e-4F,
                    "Mana Thruster should consume configured mana per tick: " + magicData.getMana());
            helper.assertTrue(Math.abs(player.fallDistance) < 1.0e-4F,
                    "Mana Thruster should reset fall distance on a successful thrust tick");
        });
    }

    static void manaThrusterInsufficientManaDoesNotAccelerate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createManaThrusterTestPlayer(helper, "mana_thruster_low_mana_test");
            var magicData = magicData(helper, player, "low mana");
            magicData.setMana(4.0F);
            player.setOnGround(false);
            player.setDeltaMovement(new Vec3(0.0D, -0.35D, 0.0D));
            player.fallDistance = 6.0F;

            try (var ignored = ApprenticeCodexServerConfig.useManaThrusterConfigOverrideForGameTest(5.0D)) {
                ManaThrusterFlightManager.setJumpInput(player, true);
                ManaThrusterFlightManager.tickEquippedPlayer(player);
            }

            helper.assertTrue(Math.abs(player.getDeltaMovement().y + 0.35D) < 1.0e-4D,
                    "Mana Thruster should not accelerate when mana is insufficient: " + player.getDeltaMovement().y);
            helper.assertTrue(Math.abs(magicData.getMana() - 4.0F) < 1.0e-4F,
                    "Mana Thruster should not spend mana when it cannot thrust: " + magicData.getMana());
            helper.assertTrue(Math.abs(player.fallDistance - 6.0F) < 1.0e-4F,
                    "Mana Thruster should not reset fall distance when it cannot thrust");
        });
    }

    static void manaThrusterSuppressesManaRecoveryUntilLanding(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createManaThrusterTestPlayer(helper, "mana_thruster_regen_test");
            var magicData = magicData(helper, player, "regen suppression");
            magicData.setMana(50.0F);
            player.setOnGround(false);

            try (var ignored = ApprenticeCodexServerConfig.useManaThrusterConfigOverrideForGameTest(5.0D)) {
                ManaThrusterFlightManager.setJumpInput(player, true);
                ManaThrusterFlightManager.tickEquippedPlayer(player);
            }

            helper.assertTrue(ManaThrusterFlightManager.isManaRecoverySuppressed(player),
                    "Mana Thruster should suppress mana recovery after the first successful thrust");
            magicData.setMana(magicData.getMana() + 10.0F);
            helper.assertTrue(Math.abs(magicData.getMana() - 45.0F) < 1.0e-4F,
                    "Mana Thruster should block mana increases before landing: " + magicData.getMana());

            player.setOnGround(true);
            ManaThrusterFlightManager.tickEquippedPlayer(player);
            helper.assertFalse(ManaThrusterFlightManager.isManaRecoverySuppressed(player),
                    "Mana Thruster should clear mana recovery suppression on landing");
            magicData.setMana(magicData.getMana() + 10.0F);
            helper.assertTrue(Math.abs(magicData.getMana() - 55.0F) < 1.0e-4F,
                    "Mana Thruster should allow mana recovery after landing: " + magicData.getMana());
        });
    }

    static void manaThrusterGroundHeldJumpDoesNotStartAfterTakeoff(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createManaThrusterTestPlayer(helper, "mana_thruster_ground_hold_test");
            var magicData = magicData(helper, player, "ground held jump");
            magicData.setMana(50.0F);
            player.setOnGround(true);
            player.setDeltaMovement(Vec3.ZERO);

            ManaThrusterFlightManager.setJumpInput(player, true);
            player.setOnGround(false);
            ManaThrusterFlightManager.tickEquippedPlayer(player);

            helper.assertTrue(Math.abs(player.getDeltaMovement().y) < 1.0e-4D,
                    "Mana Thruster should not activate from a jump key held before leaving the ground");
            helper.assertTrue(Math.abs(magicData.getMana() - 50.0F) < 1.0e-4F,
                    "Mana Thruster should not spend mana for a ground-held jump: " + magicData.getMana());
        });
    }

    static void manaThrusterCreativeMayflyAndFlyingDisableThrust(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertDisabledAbilityContextDoesNotThrust(helper, "mana_thruster_instabuild_disabled_test",
                    abilities -> abilities.instabuild = true);
            assertDisabledAbilityContextDoesNotThrust(helper, "mana_thruster_mayfly_disabled_test",
                    abilities -> abilities.mayfly = true);
            assertDisabledAbilityContextDoesNotThrust(helper, "mana_thruster_flying_disabled_test",
                    abilities -> abilities.flying = true);
        });
    }

    static void manaThrusterCreativeFlyingClearsManaRecoverySuppression(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createManaThrusterTestPlayer(helper, "mana_thruster_flying_clears_regen_test");
            var magicData = magicData(helper, player, "flying recovery clear");
            magicData.setMana(50.0F);
            player.setOnGround(false);

            try (var ignored = ApprenticeCodexServerConfig.useManaThrusterConfigOverrideForGameTest(5.0D)) {
                ManaThrusterFlightManager.setJumpInput(player, true);
                ManaThrusterFlightManager.tickEquippedPlayer(player);
                helper.assertTrue(ManaThrusterFlightManager.isManaRecoverySuppressed(player),
                        "Mana Thruster should suppress mana recovery before creative flight starts");

                player.getAbilities().flying = true;
                player.setDeltaMovement(Vec3.ZERO);
                ManaThrusterFlightManager.tickEquippedPlayer(player);
            }

            helper.assertFalse(ManaThrusterFlightManager.isManaRecoverySuppressed(player),
                    "Mana Thruster should clear mana recovery suppression when creative flight starts");
            helper.assertTrue(player.getDeltaMovement().lengthSqr() < 1.0e-8D,
                    "Mana Thruster should not keep accelerating during creative flight: " + player.getDeltaMovement());
        });
    }

    static void manaThrusterSwimmingAcceleratesForwardWithoutSuppressingRecovery(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createManaThrusterTestPlayer(helper, "mana_thruster_swim_accel_test");
            var magicData = magicData(helper, player, "swimming acceleration");
            magicData.setMana(50.0F);
            player.setOnGround(false);
            player.setSwimming(true);
            player.setXRot(0.0F);
            player.setYRot(0.0F);
            player.setDeltaMovement(Vec3.ZERO);

            try (var ignored = ApprenticeCodexServerConfig.useManaThrusterConfigOverrideForGameTest(5.0D)) {
                ManaThrusterFlightManager.setJumpInput(player, true);
                ManaThrusterFlightManager.tickEquippedPlayer(player);
            }

            helper.assertTrue(player.getDeltaMovement().z > 0.0D,
                    "Mana Thruster should accelerate swimming players toward their look direction: "
                            + player.getDeltaMovement());
            helper.assertTrue(Math.abs(player.getDeltaMovement().y) < 1.0e-4D,
                    "Mana Thruster should not apply vertical jetpack thrust while swimming: "
                            + player.getDeltaMovement());
            helper.assertFalse(ManaThrusterFlightManager.isManaRecoverySuppressed(player),
                    "Mana Thruster should not suppress mana recovery while swimming");
        });
    }

    static void manaThrusterWaterToAirResumesManaRecoverySuppression(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createManaThrusterTestPlayer(helper, "mana_thruster_water_to_air_regen_test");
            var magicData = magicData(helper, player, "water to air recovery");
            magicData.setMana(50.0F);
            player.setOnGround(false);
            player.setSwimming(true);

            try (var ignored = ApprenticeCodexServerConfig.useManaThrusterConfigOverrideForGameTest(5.0D)) {
                ManaThrusterFlightManager.setJumpInput(player, true);
                ManaThrusterFlightManager.tickEquippedPlayer(player);
                helper.assertFalse(ManaThrusterFlightManager.isManaRecoverySuppressed(player),
                        "Mana Thruster should keep mana recovery available while underwater/swimming");

                player.setSwimming(false);
                player.setDeltaMovement(Vec3.ZERO);
                ManaThrusterFlightManager.tickEquippedPlayer(player);
            }

            helper.assertTrue(ManaThrusterFlightManager.isManaRecoverySuppressed(player),
                    "Mana Thruster should resume mana recovery suppression after leaving water into air");
        });
    }

    static void manaThrusterElytraFlightAcceleratesForwardAndSuppressesRecovery(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createManaThrusterTestPlayer(helper, "mana_thruster_elytra_accel_test");
            var magicData = magicData(helper, player, "elytra acceleration");
            magicData.setMana(50.0F);
            player.setOnGround(false);
            player.startFallFlying();
            player.setXRot(0.0F);
            player.setYRot(0.0F);
            player.setDeltaMovement(Vec3.ZERO);

            try (var ignored = ApprenticeCodexServerConfig.useManaThrusterConfigOverrideForGameTest(5.0D)) {
                ManaThrusterFlightManager.setJumpInput(player, true);
                ManaThrusterFlightManager.tickEquippedPlayer(player);
            }

            helper.assertTrue(player.getDeltaMovement().z > 0.0D,
                    "Mana Thruster should accelerate fall flying players toward their look direction: "
                            + player.getDeltaMovement());
            helper.assertTrue(Math.abs(player.getDeltaMovement().y) < 1.0e-4D,
                    "Mana Thruster should not apply vertical jetpack thrust while fall flying: "
                            + player.getDeltaMovement());
            helper.assertTrue(ManaThrusterFlightManager.isManaRecoverySuppressed(player),
                    "Mana Thruster should suppress mana recovery during elytra-style air acceleration");
        });
    }

    static void manaThrusterVanillaJumpMovementContextsDoNotActivate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertDisabledPlayerContextDoesNotThrust(helper, "mana_thruster_ladder_disabled_test", player -> {
                var ladderPos = player.blockPosition();
                helper.getLevel().setBlock(ladderPos.relative(Direction.NORTH), Blocks.STONE.defaultBlockState(), 3);
                helper.getLevel().setBlock(ladderPos,
                        Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH), 3);
            });
            assertDisabledPlayerContextDoesNotThrust(helper, "mana_thruster_passenger_disabled_test", player -> {
                var pig = helper.spawn(EntityType.PIG, new BlockPos(0, 2, 0));
                player.startRiding(pig, true);
            });
        });
    }

    static void manaThrusterLavaAllowsUpwardEscapeWithoutSuppressingRecovery(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createManaThrusterTestPlayer(helper, "mana_thruster_lava_escape_test");
            var magicData = magicData(helper, player, "lava escape");
            magicData.setMana(50.0F);
            player.setOnGround(false);
            player.setDeltaMovement(new Vec3(0.0D, -0.2D, 0.0D));
            placeAbsoluteFluidTestBasin(helper.getLevel(), player.blockPosition(), Blocks.LAVA.defaultBlockState());

            try (var ignored = ApprenticeCodexServerConfig.useManaThrusterConfigOverrideForGameTest(5.0D)) {
                ManaThrusterFlightManager.setJumpInput(player, true);
                ManaThrusterFlightManager.tickEquippedPlayer(player);
            }

            helper.assertTrue(player.getDeltaMovement().y > -0.2D,
                    "Mana Thruster should apply upward thrust for escaping lava: " + player.getDeltaMovement());
            helper.assertTrue(Math.abs(magicData.getMana() - 45.0F) < 1.0e-4F,
                    "Mana Thruster should spend mana while thrusting in lava: " + magicData.getMana());
            helper.assertFalse(ManaThrusterFlightManager.isManaRecoverySuppressed(player),
                    "Mana Thruster should not suppress mana recovery while the player is touching lava");
        });
    }

    private static void assertDisabledAbilityContextDoesNotThrust(
            GameTestHelper helper,
            String profileName,
            Consumer<Abilities> configureAbilities
    ) {
        var player = createManaThrusterTestPlayer(helper, profileName);
        var magicData = magicData(helper, player, profileName);
        magicData.setMana(50.0F);
        player.setOnGround(false);
        player.setDeltaMovement(new Vec3(0.0D, -0.2D, 0.0D));
        configureAbilities.accept(player.getAbilities());

        try (var ignored = ApprenticeCodexServerConfig.useManaThrusterConfigOverrideForGameTest(5.0D)) {
            ManaThrusterFlightManager.setJumpInput(player, true);
            ManaThrusterFlightManager.tickEquippedPlayer(player);
        }

        helper.assertTrue(Math.abs(player.getDeltaMovement().y + 0.2D) < 1.0e-4D,
                "Mana Thruster should not accelerate in disabled ability context " + profileName + ": "
                        + player.getDeltaMovement());
        helper.assertTrue(Math.abs(magicData.getMana() - 50.0F) < 1.0e-4F,
                "Mana Thruster should not spend mana in disabled ability context " + profileName + ": "
                        + magicData.getMana());
        helper.assertFalse(ManaThrusterFlightManager.isManaRecoverySuppressed(player),
                "Mana Thruster should not suppress recovery in disabled ability context " + profileName);
    }

    private static void assertDisabledPlayerContextDoesNotThrust(
            GameTestHelper helper,
            String profileName,
            Consumer<net.minecraftforge.common.util.FakePlayer> configurePlayer
    ) {
        var player = createManaThrusterTestPlayer(helper, profileName);
        var magicData = magicData(helper, player, profileName);
        magicData.setMana(50.0F);
        player.setOnGround(false);
        player.setDeltaMovement(new Vec3(0.0D, -0.2D, 0.0D));
        configurePlayer.accept(player);

        try (var ignored = ApprenticeCodexServerConfig.useManaThrusterConfigOverrideForGameTest(5.0D)) {
            ManaThrusterFlightManager.setJumpInput(player, true);
            ManaThrusterFlightManager.tickEquippedPlayer(player);
        }

        helper.assertTrue(Math.abs(player.getDeltaMovement().y + 0.2D) < 1.0e-4D,
                "Mana Thruster should not accelerate in disabled movement context " + profileName + ": "
                        + player.getDeltaMovement());
        helper.assertTrue(Math.abs(magicData.getMana() - 50.0F) < 1.0e-4F,
                "Mana Thruster should not spend mana in disabled movement context " + profileName + ": "
                        + magicData.getMana());
        helper.assertFalse(ManaThrusterFlightManager.isManaRecoverySuppressed(player),
                "Mana Thruster should not suppress recovery in disabled movement context " + profileName);
    }

    private static net.minecraftforge.common.util.FakePlayer createManaThrusterTestPlayer(
            GameTestHelper helper,
            String profileName
    ) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        equipCurio(player, CuriosSlotConstants.FEET, new ItemStack(ItemRegistry.MANA_THRUSTER.get()));
        ManaThrusterFlightManager.clear(player);
        return player;
    }

    private static MagicData magicData(
            GameTestHelper helper,
            net.minecraftforge.common.util.FakePlayer player,
            String label
    ) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Mana Thruster " + label + " test could not resolve player mana data");
        return magicData;
    }
}
