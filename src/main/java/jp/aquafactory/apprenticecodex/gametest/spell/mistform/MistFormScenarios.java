package jp.aquafactory.apprenticecodex.gametest.spell.mistform;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.effect.MistFormEffect;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.mistform.MistForm;
import jp.aquafactory.apprenticecodex.spell.mistform.MistFormEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.support.EntityTestSupport.createTargetingZombie;
import static jp.aquafactory.apprenticecodex.gametest.support.EntityTestSupport.placeAbsoluteFluidTestBasin;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createTrackedEquipmentTestPlayer;

final class MistFormScenarios {
    private MistFormScenarios() {}

    static void mistFormAppliesEffectAndFixedAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mist_form_attribute_test");
            var spell = (MistForm) SpellRegistry.MIST_FORM.get();
            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));

            var instance = player.getEffect(EffectRegistry.MIST_FORM.get());
            helper.assertTrue(instance != null, "Mist Form cast should apply the Mist Form effect");
            var expectedDuration = Math.round(10 * 20 * spell.getSpellPower(1, player) / 100.0F);
            helper.assertTrue(instance != null && instance.getDuration() == expectedDuration,
                    "Mist Form effect duration should use existing spell power duration: "
                            + (instance == null ? "null" : instance.getDuration()) + " / " + expectedDuration);

            var effect = EffectRegistry.MIST_FORM.get();
            assertMistFormModifierAmount(helper, effect, Attributes.MOVEMENT_SPEED,
                    AttributeModifier.Operation.MULTIPLY_TOTAL,
                    MistFormEffect.MOVEMENT_SPEED_BONUS,
                    "Mist Form should provide fixed movement speed");
            assertMistFormModifierAmount(helper, effect, ForgeMod.STEP_HEIGHT_ADDITION.get(),
                    AttributeModifier.Operation.ADDITION,
                    MistFormEffect.STEP_HEIGHT_ADDITION,
                    "Mist Form should provide fixed step assist");
            assertMistFormModifierAmount(helper, effect,
                    AttributeRegistry.FIRE_MAGIC_RESIST.get(),
                    AttributeModifier.Operation.ADDITION,
                    MistFormEffect.SCHOOL_RESIST_WEAKNESS,
                    "Mist Form should provide fixed fire weakness");
            assertMistFormModifierAmount(helper, effect,
                    AttributeRegistry.HOLY_MAGIC_RESIST.get(),
                    AttributeModifier.Operation.ADDITION,
                    MistFormEffect.SCHOOL_RESIST_WEAKNESS,
                    "Mist Form should provide fixed holy weakness");
        });
    }

    static void mistFormSuppressesAwarenessWithinThirtyTwoBlocks(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mist_form_awareness_test");
            var nearZombie = createTargetingZombie(helper, new BlockPos(4, 2, 0), player, "near");
            var farZombie = createTargetingZombie(helper, new BlockPos(36, 2, 0), player, "far");

            SpellRegistry.MIST_FORM.get().onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));

            helper.assertTrue(nearZombie.getTarget() == null,
                    "Mist Form should clear nearby mob target within 32 blocks");
            helper.assertTrue(nearZombie.getLastHurtByMob() == null,
                    "Mist Form should clear nearby mob last hurt by mob within 32 blocks");
            helper.assertTrue(farZombie.getTarget() == player,
                    "Mist Form should not clear mob target outside 32 blocks");
            helper.assertTrue(farZombie.getLastHurtByMob() == player,
                    "Mist Form should not clear mob last hurt by mob outside 32 blocks");
        });
    }

    static void mistFormDamageToLivingTargetRemovesEffect(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mist_form_damage_test");
            player.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            var target = createTargetingZombie(helper, new BlockPos(2, 2, 0), player, "damage_target");

            target.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);

            helper.assertFalse(player.hasEffect(EffectRegistry.MIST_FORM.get()),
                    "Mist Form should be removed when the caster damages a living target");
        });
    }

    static void mistFormSlowsFallingWithoutAmplifierScaling(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 8, 0), "mist_form_fall_test");
            player.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 9, false, false, true));
            player.fallDistance = 12.0F;
            player.hurtMarked = false;
            player.setDeltaMovement(0.12D, -0.7D, -0.08D);

            MistFormEvents.onPlayerTick(
                    new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player)
            );

            helper.assertTrue(Math.abs(player.getDeltaMovement().y + 0.08D) < 1.0E-9D,
                    "Mist Form should clamp falling speed without amplifier scaling: " + player.getDeltaMovement().y);
            helper.assertTrue(Math.abs(player.getDeltaMovement().x - 0.12D) < 1.0E-9D
                            && Math.abs(player.getDeltaMovement().z + 0.08D) < 1.0E-9D,
                    "Mist Form slow falling should preserve horizontal movement: " + player.getDeltaMovement());
            helper.assertFalse(player.hurtMarked,
                    "Mist Form slow falling should not force velocity sync that overwrites client horizontal input");
            helper.assertTrue(player.fallDistance == 0.0F,
                    "Mist Form should reset fall distance while slowing descent");
        });
    }

    static void mistFormStandsOnLiquidAndSneakSinks(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            // 各検査は同期的に完了するため、隣接 test の領域へ出ず中央の basin を再利用する.
            var basinPlayerPos = new BlockPos(2, 3, 2);
            var waterWalker = createEquipmentTestPlayer(helper, basinPlayerPos, "mist_form_water_walk_test");
            var waterSupportPos = waterWalker.blockPosition().below();
            placeAbsoluteFluidTestBasin(helper.getLevel(), waterSupportPos, Blocks.WATER.defaultBlockState());
            waterWalker.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            waterWalker.setDeltaMovement(0.1D, -0.2D, 0.0D);
            MistFormEvents.onPlayerTick(
                    new TickEvent.PlayerTickEvent(TickEvent.Phase.START, waterWalker)
            );
            helper.assertTrue(waterWalker.onGround() && waterWalker.getDeltaMovement().y == 0.0D,
                    "Mist Form should hold the player on liquid surface without downward motion: onGround="
                            + waterWalker.onGround()
                            + ", y=" + waterWalker.getY()
                            + ", dy=" + waterWalker.getDeltaMovement().y);
            helper.assertTrue(Math.abs(waterWalker.getDeltaMovement().x - 0.1D) < 1.0E-9D,
                    "Mist Form liquid standing should preserve horizontal movement on water");

            var sneakingWalker = createEquipmentTestPlayer(helper, basinPlayerPos, "mist_form_sneak_sink_test");
            sneakingWalker.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            sneakingWalker.setShiftKeyDown(true);
            sneakingWalker.setOnGround(false);
            sneakingWalker.setDeltaMovement(0.0D, -0.2D, 0.0D);
            MistFormEvents.onPlayerTick(
                    new TickEvent.PlayerTickEvent(TickEvent.Phase.START, sneakingWalker)
            );
            helper.assertFalse(sneakingWalker.onGround(),
                    "Mist Form should not hold the player on liquid while sneaking");

            var lavaWalker = createEquipmentTestPlayer(helper, basinPlayerPos, "mist_form_lava_walk_test");
            placeAbsoluteFluidTestBasin(helper.getLevel(), lavaWalker.blockPosition().below(), Blocks.LAVA.defaultBlockState());
            lavaWalker.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            lavaWalker.setDeltaMovement(0.0D, -0.2D, 0.0D);
            MistFormEvents.onPlayerTick(
                    new TickEvent.PlayerTickEvent(TickEvent.Phase.START, lavaWalker)
            );
            helper.assertTrue(lavaWalker.onGround() && !lavaWalker.isInLava(),
                    "Mist Form should stand on lava by avoiding liquid contact, not by granting fire resistance");

            var flowingWaterWalker = createEquipmentTestPlayer(helper, basinPlayerPos, "mist_form_flowing_water_walk_test");
            placeAbsoluteFluidTestBasin(helper.getLevel(), flowingWaterWalker.blockPosition().below(),
                    Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
            flowingWaterWalker.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            flowingWaterWalker.setDeltaMovement(0.1D, -0.2D, 0.0D);
            MistFormEvents.onPlayerTick(
                    new TickEvent.PlayerTickEvent(TickEvent.Phase.START, flowingWaterWalker)
            );
            helper.assertTrue(flowingWaterWalker.onGround()
                            && flowingWaterWalker.getDeltaMovement().y == 0.0D
                            && Math.abs(flowingWaterWalker.getDeltaMovement().x - 0.1D) < 1.0E-9D,
                    "Mist Form should stand on flowing liquid without crushing horizontal movement: "
                            + flowingWaterWalker.getDeltaMovement());

            var swimmer = createEquipmentTestPlayer(helper, basinPlayerPos, "mist_form_swimming_test");
            placeAbsoluteFluidTestBasin(helper.getLevel(), swimmer.blockPosition(), Blocks.WATER.defaultBlockState());
            swimmer.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            swimmer.setDeltaMovement(0.0D, 0.2D, 0.0D);
            MistFormEvents.onPlayerTick(
                    new TickEvent.PlayerTickEvent(TickEvent.Phase.START, swimmer)
            );
            helper.assertFalse(swimmer.onGround(),
                    "Mist Form should not force liquid standing while the player is touching liquid");
            helper.assertTrue(Math.abs(swimmer.getDeltaMovement().y - 0.2D) < 1.0E-9D,
                    "Mist Form should preserve upward swimming movement while touching liquid: "
                            + swimmer.getDeltaMovement());

            var cooldownWalker = createEquipmentTestPlayer(helper, basinPlayerPos, "mist_form_fluid_cooldown_test");
            placeAbsoluteFluidTestBasin(helper.getLevel(), cooldownWalker.blockPosition(), Blocks.WATER.defaultBlockState());
            cooldownWalker.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            cooldownWalker.tickCount = 100;
            helper.assertFalse(MistFormEvents.canStandOnFluid(cooldownWalker),
                    "Mist Form should disable liquid standing immediately after touching liquid");

            helper.getLevel().setBlock(cooldownWalker.blockPosition(), Blocks.AIR.defaultBlockState(), 3);
            placeAbsoluteFluidTestBasin(helper.getLevel(), cooldownWalker.blockPosition().below(), Blocks.WATER.defaultBlockState());
            // ここでは再接触ではなく、20 tick 経過後に液体立ちが再有効化されることだけを検証する。
            var cooldownSafePos = Vec3.atBottomCenterOf(cooldownWalker.blockPosition().above(2));
            cooldownWalker.setPos(cooldownSafePos.x, cooldownSafePos.y, cooldownSafePos.z);
            cooldownWalker.tickCount = 120;
            helper.assertFalse(MistFormEvents.canStandOnFluid(cooldownWalker),
                    "Mist Form should keep liquid standing disabled for 20 ticks after leaving liquid");
            cooldownWalker.tickCount = 121;
            helper.assertTrue(MistFormEvents.canStandOnFluid(cooldownWalker),
                    "Mist Form should re-enable liquid standing after the 20 tick liquid-contact delay");
            helper.succeed();
        });
    }

    static void mistFormPassesTaggedBlocksAndRejectsGlass(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var normalPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mist_form_collision_normal_test");
            var mistPlayer = createEquipmentTestPlayer(helper, new BlockPos(1, 2, 0), "mist_form_collision_pass_test");
            mistPlayer.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));

            for (var sample : MIST_FORM_PASSABLE_COLLISION_SAMPLES) {
                helper.assertFalse(isCollisionShapeEmptyForPlayer(helper, sample.state(), normalPlayer),
                        "Mist Form passable sample should still collide without Mist Form: " + sample.name());
                helper.assertTrue(isCollisionShapeEmptyForPlayer(helper, sample.state(), mistPlayer),
                        "Mist Form should remove collision from passable sample: " + sample.name());
            }

            helper.assertFalse(isCollisionShapeEmptyForPlayer(helper, Blocks.GLASS.defaultBlockState(), mistPlayer),
                    "Mist Form should not remove glass collision");
        });
    }

    static void mistFormPassableBlockDenylistBlocksIdsAndTags(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mist_form_collision_deny_test");
            player.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));

            try (var ignored = ApprenticeCodexServerConfig.useMistFormPassableBlockDenylistOverrideForGameTest(
                    List.of("minecraft:iron_bars")
            )) {
                helper.assertFalse(isCollisionShapeEmptyForPlayer(helper, Blocks.IRON_BARS.defaultBlockState(), player),
                        "Mist Form server denylist should block a configured block ID");
                helper.assertTrue(isCollisionShapeEmptyForPlayer(helper, Blocks.OAK_LEAVES.defaultBlockState(), player),
                        "Mist Form server denylist should not block unrelated passable blocks");
            }

            try (var ignored = ApprenticeCodexServerConfig.useMistFormPassableBlockDenylistOverrideForGameTest(
                    List.of("#minecraft:leaves")
            )) {
                helper.assertFalse(isCollisionShapeEmptyForPlayer(helper, Blocks.OAK_LEAVES.defaultBlockState(), player),
                        "Mist Form server denylist should block a configured block tag");
                helper.assertTrue(isCollisionShapeEmptyForPlayer(helper, Blocks.IRON_BARS.defaultBlockState(), player),
                        "Mist Form server denylist should not block unrelated passable IDs");
            }
        });
    }

    static void mistFormWaterloggedPassableBlockDoesNotSnapUp(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 3, 0), "mist_form_waterlogged_passable_test");
            var waterloggedTrapdoorPos = player.blockPosition();
            helper.getLevel().setBlock(
                    waterloggedTrapdoorPos,
                    Blocks.OAK_TRAPDOOR.defaultBlockState()
                            .setValue(TrapDoorBlock.WATERLOGGED, true),
                    3
            );
            player.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            player.setDeltaMovement(0.0D, -0.2D, 0.0D);

            var yBeforeTick = player.getY();
            MistFormEvents.onPlayerTick(
                    new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player)
            );

            helper.assertTrue(Math.abs(player.getY() - yBeforeTick) < 1.0E-9D,
                    "Mist Form should not snap upward while touching fluid inside a passable waterlogged block");
        });
    }

    static void mistFormIgnoresTaggedMovementRestrictions(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var normalPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mist_form_stuck_normal_test");
            var mistPlayer = createEquipmentTestPlayer(helper, new BlockPos(3, 2, 0),
                    "mist_form_stuck_ignore_test");
            mistPlayer.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));

            for (var sample : MIST_FORM_MOVEMENT_RESTRICTION_SAMPLES) {
                resetPlayerPosition(helper, normalPlayer, new BlockPos(0, 2, 0));
                resetPlayerPosition(helper, mistPlayer, new BlockPos(3, 2, 0));

                var normalMove = moveAfterStuckInBlock(normalPlayer, sample.state(), sample.motionMultiplier());
                var mistMove = moveAfterStuckInBlock(mistPlayer, sample.state(), sample.motionMultiplier());

                helper.assertTrue(normalMove <= sample.motionMultiplier().x + 0.01D,
                        "Movement restriction sample should slow normal player: " + sample.name()
                                + " move=" + normalMove);
                helper.assertTrue(mistMove > 0.99D,
                        "Mist Form should ignore movement restriction sample: " + sample.name()
                                + " move=" + mistMove);
            }
        });
    }

    static void mistFormMovementRestrictionIgnoreKeepsBlockEffects(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var powderPlayer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "mist_form_powder_effect_test");
            powderPlayer.addEffect(new MobEffectInstance(EffectRegistry.MIST_FORM.get(), 200, 0, false, false, true));
            var powderPos = powderPlayer.blockPosition();
            helper.getLevel().setBlock(powderPos, Blocks.POWDER_SNOW.defaultBlockState(), 3);
            Blocks.POWDER_SNOW.defaultBlockState().entityInside(helper.getLevel(), powderPos, powderPlayer);
            helper.assertTrue(powderPlayer.isInPowderSnow,
                    "Mist Form should ignore powder snow movement restriction without removing powder snow state");

            // makeStuckInBlock だけを止めるため、entityInside の後続処理が消えないことを粉雪で代表確認する。
        });
    }

    static void assertMistFormModifierAmount(
            GameTestHelper helper,
            MobEffect effect,
            Attribute attribute,
            AttributeModifier.Operation operation,
            double expectedAmount,
            String message
    ) {
        var modifier = effect.getAttributeModifiers().get(attribute);
        helper.assertTrue(modifier != null, message + ": missing modifier for " + attribute.getDescriptionId());
        if (modifier == null) {
            return;
        }
        helper.assertTrue(modifier.getOperation() == operation,
                message + ": expected operation " + operation + " but got " + modifier.getOperation());
        helper.assertTrue(Math.abs(effect.getAttributeModifierValue(0, modifier) - expectedAmount) < 1.0E-9D,
                message + ": expected level 0 amount " + expectedAmount);
        helper.assertTrue(Math.abs(effect.getAttributeModifierValue(9, modifier) - expectedAmount) < 1.0E-9D,
                message + ": expected level 9 amount to remain " + expectedAmount);
    }

    static boolean isCollisionShapeEmptyForPlayer(GameTestHelper helper, BlockState state, Player player) {
        return state.getCollisionShape(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), CollisionContext.of(player)).isEmpty();
    }

    static final List<MistFormCollisionSample> MIST_FORM_PASSABLE_COLLISION_SAMPLES = List.of(
            new MistFormCollisionSample("fence", Blocks.OAK_FENCE.defaultBlockState()),
            new MistFormCollisionSample("fence_gate", Blocks.OAK_FENCE_GATE.defaultBlockState()),
            new MistFormCollisionSample("door", Blocks.OAK_DOOR.defaultBlockState()),
            new MistFormCollisionSample("iron_bars", Blocks.IRON_BARS.defaultBlockState()),
            new MistFormCollisionSample("trapdoor", Blocks.OAK_TRAPDOOR.defaultBlockState()),
            new MistFormCollisionSample("leaves", Blocks.OAK_LEAVES.defaultBlockState())
    );

    record MistFormCollisionSample(String name, BlockState state) {
    }

    static double moveAfterStuckInBlock(Player player, BlockState state, Vec3 motionMultiplier) {
        player.setDeltaMovement(Vec3.ZERO);
        var beforeX = player.getX();
        player.makeStuckInBlock(state, motionMultiplier);
        player.move(MoverType.SELF, new Vec3(1.0D, 0.0D, 0.0D));
        return player.getX() - beforeX;
    }

    static void resetPlayerPosition(GameTestHelper helper, Player player, BlockPos pos) {
        var absolutePos = helper.absoluteVec(Vec3.atBottomCenterOf(pos));
        player.setPos(absolutePos.x, absolutePos.y, absolutePos.z);
        player.setDeltaMovement(Vec3.ZERO);
    }

    static final List<MistFormMovementRestrictionSample> MIST_FORM_MOVEMENT_RESTRICTION_SAMPLES = List.of(
            new MistFormMovementRestrictionSample(
                    "cobweb",
                    Blocks.COBWEB.defaultBlockState(),
                    new Vec3(0.25D, 0.05D, 0.25D)
            ),
            new MistFormMovementRestrictionSample(
                    "powder_snow",
                    Blocks.POWDER_SNOW.defaultBlockState(),
                    new Vec3(0.9D, 1.5D, 0.9D)
            ),
            new MistFormMovementRestrictionSample(
                    "sweet_berry_bush",
                    Blocks.SWEET_BERRY_BUSH.defaultBlockState(),
                    new Vec3(0.8D, 0.75D, 0.8D)
            )
    );

    record MistFormMovementRestrictionSample(String name, BlockState state, Vec3 motionMultiplier) {
    }
}
