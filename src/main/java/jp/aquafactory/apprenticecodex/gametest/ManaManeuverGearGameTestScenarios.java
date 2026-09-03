package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.manamaneuvergear.ManaManeuverGear;
import jp.aquafactory.apprenticecodex.item.curios.manamaneuvergear.ManaManeuverGearManager;
import jp.aquafactory.apprenticecodex.item.curios.manamaneuvergear.ManaManeuverGearMovement;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

final class ManaManeuverGearGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final float EPSILON = 1.0e-4F;
    private static final TagKey<Item> CURIOS_FEET = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.FEET)
    );

    private ManaManeuverGearGameTestScenarios() {
    }

    static void usesFeetSlotAndDedicatedImplementation(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MANA_MANEUVER_GEAR.get());
            helper.assertTrue(stack.is(CURIOS_FEET),
                    "Mana Maneuver Gear should be tagged for the Curios feet slot");
            helper.assertTrue(stack.getItem() instanceof ManaManeuverGear,
                    "Mana Maneuver Gear should resolve to the dedicated curio item implementation");
        });
    }

    static void wallJumpUsesConfiguredManaAndPreservesHorizontalVelocity(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createGearPlayerTouchingWall(helper, "mana_maneuver_wall_jump");
            var magicData = magicData(helper, player, "wall jump");
            magicData.setMana(20.0F);
            player.setYRot(-90.0F);
            player.setXRot(0.0F);
            player.setDeltaMovement(new Vec3(0.1D, -0.7D, 0.2D));
            player.fallDistance = 8.0F;

            boolean jumped;
            try (var ignored = ApprenticeCodexServerConfig.useManaManeuverGearConfigOverrideForGameTest(10, 5.0D)) {
                jumped = ManaManeuverGearManager.tryWallJump(player);
            }

            helper.assertTrue(jumped, "Mana Maneuver Gear should wall jump when all server conditions pass");
            helper.assertTrue(Math.abs(player.getDeltaMovement().x - 0.5D) < 1.0e-4D,
                    "Wall jump should preserve and add to horizontal velocity: " + player.getDeltaMovement());
            helper.assertTrue(Math.abs(player.getDeltaMovement().y
                            - ManaManeuverGearMovement.WALL_JUMP_BASE_Y_ACCELERATION) < 1.0e-4D,
                    "Horizontal wall jump should apply the base upward acceleration: " + player.getDeltaMovement());
            helper.assertTrue(Math.abs(player.getDeltaMovement().z - 0.2D) < 1.0e-4D,
                    "Wall jump should preserve unrelated horizontal velocity: " + player.getDeltaMovement());
            helper.assertTrue(Math.abs(magicData.getMana() - 10.0F) < EPSILON,
                    "Wall jump should spend the configured mana cost: " + magicData.getMana());
            helper.assertTrue(Math.abs(player.fallDistance) < EPSILON,
                    "Wall jump should reset fall distance");

            var upwardImpulse = ManaManeuverGearMovement.wallJumpImpulse(new Vec3(0.0D, 1.0D, 0.0D));
            helper.assertTrue(Math.abs(upwardImpulse.y - 0.6D) < 1.0e-4D,
                    "Looking upward should add a smaller upward bonus: " + upwardImpulse);
            var downwardImpulse = ManaManeuverGearMovement.wallJumpImpulse(new Vec3(0.0D, -1.0D, 0.0D));
            helper.assertTrue(Math.abs(downwardImpulse.y
                            - ManaManeuverGearMovement.WALL_JUMP_BASE_Y_ACCELERATION) < 1.0e-4D,
                    "Looking downward should not reduce the base upward acceleration: " + downwardImpulse);
        });
    }

    static void wallJumpRejectsGroundInsufficientManaAndRepeatedTick(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createGearPlayerTouchingWall(helper, "mana_maneuver_rejections");
            var magicData = magicData(helper, player, "wall jump rejection");

            try (var ignored = ApprenticeCodexServerConfig.useManaManeuverGearConfigOverrideForGameTest(10, 5.0D)) {
                player.setOnGround(true);
                magicData.setMana(20.0F);
                helper.assertFalse(ManaManeuverGearManager.tryWallJump(player),
                        "Mana Maneuver Gear should not wall jump from the ground");

                player.setOnGround(false);
                magicData.setMana(9.0F);
                helper.assertFalse(ManaManeuverGearManager.tryWallJump(player),
                        "Mana Maneuver Gear should require the full wall jump mana cost");

                magicData.setMana(20.0F);
                helper.assertTrue(ManaManeuverGearManager.tryWallJump(player),
                        "Mana Maneuver Gear should wall jump after rejection conditions are removed");
                helper.assertFalse(ManaManeuverGearManager.tryWallJump(player),
                        "Mana Maneuver Gear should reject a repeated request in the same tick");
                helper.assertTrue(Math.abs(magicData.getMana() - 10.0F) < EPSILON,
                        "A repeated same-tick request should not spend mana twice");
            }
        });
    }

    static void wallJumpRequiresWallButAllowsFreeCreativeFlightContext(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var noWallPlayer = createGearPlayer(helper, "mana_maneuver_no_wall");
            var noWallMana = magicData(helper, noWallPlayer, "no wall");
            noWallMana.setMana(20.0F);

            try (var ignored = ApprenticeCodexServerConfig.useManaManeuverGearConfigOverrideForGameTest(10, 5.0D)) {
                helper.assertFalse(ManaManeuverGearManager.tryWallJump(noWallPlayer),
                        "Mana Maneuver Gear should require contact with a wall");
            }

            var creativeFlightPlayer = createGearPlayerTouchingWall(helper, "mana_maneuver_mayfly");
            var creativeFlightMana = magicData(helper, creativeFlightPlayer, "free creative flight context");
            creativeFlightMana.setMana(0.0F);
            creativeFlightPlayer.getAbilities().mayfly = true;

            try (var ignored = ApprenticeCodexServerConfig.useManaManeuverGearConfigOverrideForGameTest(0, 5.0D)) {
                helper.assertTrue(ManaManeuverGearManager.tryWallJump(creativeFlightPlayer),
                        "Mayfly should not block a free Mana Maneuver Gear wall jump");
            }
            helper.assertTrue(Math.abs(creativeFlightMana.getMana()) < EPSILON,
                    "A zero-cost wall jump should not change mana");
        });
    }

    static void wallMovementRejectsClimbablesAndScaffolding(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var ladderPlayer = createGearPlayerTouchingWall(helper, "mana_maneuver_ladder");
            var ladderPos = ladderPlayer.blockPosition();
            helper.getLevel().setBlock(ladderPos.relative(Direction.NORTH), Blocks.STONE.defaultBlockState(), 3);
            helper.getLevel().setBlock(ladderPos,
                    Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH), 3);
            helper.assertTrue(ManaManeuverGearManager.isWallMovementBlocked(ladderPlayer),
                    "Climbable blocks should disable Mana Maneuver Gear wall movement");

            var scaffoldingPlayer = createGearPlayerTouchingWall(helper, "mana_maneuver_scaffolding");
            helper.getLevel().setBlock(scaffoldingPlayer.blockPosition(), Blocks.SCAFFOLDING.defaultBlockState(), 3);
            helper.assertTrue(ManaManeuverGearManager.isWallMovementBlocked(scaffoldingPlayer),
                    "Scaffolding should disable Mana Maneuver Gear wall movement");
        });
    }

    static void wallSlideClampsFallingSpeedAndResetsFallDistance(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createGearPlayerTouchingWall(helper, "mana_maneuver_wall_slide");
            player.setShiftKeyDown(false);
            player.setDeltaMovement(new Vec3(0.2D, -0.6D, -0.1D));
            player.fallDistance = 9.0F;

            helper.assertTrue(ManaManeuverGearManager.tickWallSlide(player),
                    "Falling against a wall should activate wall slide without sneaking");
            helper.assertTrue(Math.abs(player.getDeltaMovement().y
                            - ManaManeuverGearMovement.WALL_SLIDE_MINIMUM_Y_SPEED) < 1.0e-4D,
                    "Wall slide should clamp vertical speed: " + player.getDeltaMovement());
            helper.assertTrue(Math.abs(player.getDeltaMovement().x - 0.2D) < 1.0e-4D
                            && Math.abs(player.getDeltaMovement().z + 0.1D) < 1.0e-4D,
                    "Wall slide should preserve horizontal velocity: " + player.getDeltaMovement());
            helper.assertTrue(Math.abs(player.fallDistance) < EPSILON,
                    "Wall slide should reset fall distance");

            player.setDeltaMovement(new Vec3(0.0D, -0.05D, 0.0D));
            player.fallDistance = 3.0F;
            helper.assertTrue(ManaManeuverGearManager.tickWallSlide(player),
                    "A mild descent should still count as an active wall slide");
            helper.assertTrue(Math.abs(player.getDeltaMovement().y + 0.05D) < 1.0e-4D,
                    "Wall slide should not accelerate a mild descent");
            helper.assertTrue(Math.abs(player.fallDistance) < EPSILON,
                    "An active mild wall slide should still reset fall distance");

            player.setDeltaMovement(new Vec3(0.0D, 0.1D, 0.0D));
            helper.assertFalse(ManaManeuverGearManager.tickWallSlide(player),
                    "Wall slide should not activate while rising");
        });
    }

    static void fallDamageUsesFractionalManaPerDamageAndKeepsFallDistance(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createGearPlayer(helper, "mana_maneuver_fractional_fall");
            var magicData = magicData(helper, player, "fractional fall damage");
            magicData.setMana(7.5F);
            player.fallDistance = 14.0F;

            var event = postFallDamage(helper, player, 3.5F, 5.0D);

            helper.assertFalse(event.isCanceled(), "Partially absorbed fall damage should remain active");
            helper.assertTrue(Math.abs(event.getAmount() - 2.0F) < EPSILON,
                    "Fall damage should be reduced by the fractional affordable amount: " + event.getAmount());
            helper.assertTrue(Math.abs(magicData.getMana()) < EPSILON,
                    "Fractional fall absorption should spend all affordable mana");
            helper.assertTrue(Math.abs(player.fallDistance - 14.0F) < EPSILON,
                    "Passive fall absorption should preserve fall distance for mace behavior");
        });
    }

    static void fallDamageSupportsFullFreeAndZeroManaOutcomes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var fullPlayer = createGearPlayer(helper, "mana_maneuver_full_fall");
            equipCurio(fullPlayer, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
            var fullMana = magicData(helper, fullPlayer, "full fall damage");
            fullMana.setMana(20.0F);
            fullPlayer.invulnerableTime = 0;
            var full = postFallDamage(helper, fullPlayer, 3.0F, 5.0D);
            helper.assertTrue(full.isCanceled(), "Affordable fall damage should be fully canceled");
            helper.assertTrue(Math.abs(fullMana.getMana() - 5.0F) < EPSILON,
                    "Full fall absorption should spend exact fractional mana");
            helper.assertTrue(fullPlayer.invulnerableTime == 0,
                    "Full Gear absorption should not activate Mana Shield invulnerability");

            var freePlayer = createGearPlayer(helper, "mana_maneuver_free_fall");
            var freeMana = magicData(helper, freePlayer, "free fall damage");
            freeMana.setMana(0.0F);
            var free = postFallDamage(helper, freePlayer, 8.0F, 0.0D);
            helper.assertTrue(free.isCanceled(), "Zero mana-per-damage should make fall absorption free");
            helper.assertTrue(Math.abs(freeMana.getMana()) < EPSILON,
                    "Free fall absorption should not change mana");

            var emptyPlayer = createGearPlayer(helper, "mana_maneuver_empty_fall");
            var emptyMana = magicData(helper, emptyPlayer, "empty fall damage");
            emptyMana.setMana(0.0F);
            var empty = postFallDamage(helper, emptyPlayer, 4.0F, 5.0D);
            helper.assertFalse(empty.isCanceled(), "Positive cost with zero mana should not absorb fall damage");
            helper.assertTrue(Math.abs(empty.getAmount() - 4.0F) < EPSILON,
                    "Zero mana should preserve the original fall damage");
        });
    }

    static void gearRunsBeforeManaShieldAndIgnoresOtherDamage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createGearPlayer(helper, "mana_maneuver_ordering");
            equipCurio(player, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MANA_SHIELD_CHARM.get()));
            var magicData = magicData(helper, player, "Mana Shield ordering");
            magicData.setMana(7.5F);
            player.invulnerableTime = 0;

            LivingIncomingDamageEventHolder partial;
            try (var gearConfig = ApprenticeCodexServerConfig.useManaManeuverGearConfigOverrideForGameTest(10, 5.0D);
                 var shieldConfig = ApprenticeCodexServerConfig.useManaShieldCharmConfigOverrideForGameTest(
                         25.0D, 100, 50, 15.0D, 30.0D, 50, 100, 1, 20
                 )) {
                var event = postLivingAttackEventForGameTest(
                        player, helper.getLevel().damageSources().fall(), 3.5F);
                partial = new LivingIncomingDamageEventHolder(event.isCanceled(), event.getAmount());
            }
            helper.assertFalse(partial.canceled(), "Partial Gear absorption should leave damage for Mana Shield");
            helper.assertTrue(Math.abs(partial.amount() - 2.0F) < EPSILON,
                    "Mana Shield should receive only the damage left after Gear: " + partial.amount());
            helper.assertTrue(player.invulnerableTime == 0,
                    "A depleted Mana Shield should not create invulnerability after partial Gear absorption");

            var genericPlayer = createGearPlayer(helper, "mana_maneuver_generic_damage");
            var genericMana = magicData(helper, genericPlayer, "generic damage");
            genericMana.setMana(20.0F);
            try (var ignored = ApprenticeCodexServerConfig.useManaManeuverGearConfigOverrideForGameTest(10, 5.0D)) {
                var generic = postLivingAttackEventForGameTest(
                        genericPlayer, helper.getLevel().damageSources().generic(), 3.0F);
                helper.assertFalse(generic.isCanceled(), "Mana Maneuver Gear should ignore non-fall damage");
                helper.assertTrue(Math.abs(generic.getAmount() - 3.0F) < EPSILON,
                        "Mana Maneuver Gear should preserve non-fall damage");
                helper.assertTrue(Math.abs(genericMana.getMana() - 20.0F) < EPSILON,
                        "Mana Maneuver Gear should not spend mana on non-fall damage");
            }
        });
    }

    private static net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent postFallDamage(
            GameTestHelper helper,
            FakePlayer player,
            float damage,
            double manaPerDamage
    ) {
        try (var ignored = ApprenticeCodexServerConfig.useManaManeuverGearConfigOverrideForGameTest(10, manaPerDamage)) {
            return postLivingAttackEventForGameTest(player, helper.getLevel().damageSources().fall(), damage);
        }
    }

    private static FakePlayer createGearPlayerTouchingWall(GameTestHelper helper, String profileName) {
        var player = createGearPlayer(helper, profileName);
        player.setPos(player.getX() + 0.2D, player.getY(), player.getZ());
        helper.getLevel().setBlock(player.blockPosition().east(), Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(ManaManeuverGearMovement.isTouchingWall(player),
                "Mana Maneuver Gear test setup should place the player against a wall");
        return player;
    }

    private static FakePlayer createGearPlayer(GameTestHelper helper, String profileName) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        equipCurio(player, CuriosSlotConstants.FEET, new ItemStack(ItemRegistry.MANA_MANEUVER_GEAR.get()));
        player.setOnGround(false);
        ManaManeuverGearManager.clear(player);
        return player;
    }

    private static MagicData magicData(GameTestHelper helper, FakePlayer player, String label) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null,
                "Mana Maneuver Gear " + label + " test could not resolve player mana data");
        return magicData;
    }

    private record LivingIncomingDamageEventHolder(boolean canceled, float amount) {
    }
}
