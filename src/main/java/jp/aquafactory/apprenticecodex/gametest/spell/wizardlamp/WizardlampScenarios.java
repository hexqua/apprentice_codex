package jp.aquafactory.apprenticecodex.gametest.spell.wizardlamp;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLight;
import jp.aquafactory.apprenticecodex.spell.wizardlamp.Wizardlamp;
import jp.aquafactory.apprenticecodex.spell.wizardlamp.WizardlampLanternBlock;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import static jp.aquafactory.apprenticecodex.gametest.spell.linearbuild.LinearBuildScenarios.castLinearBuildWithCurrentMana;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class WizardlampScenarios {
    private WizardlampScenarios() {}

    static void wizardlampUsesClientCellAndClampsItWithoutLineOfSight(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = createEquipmentTestPlayer(helper, new BlockPos(1, 2, 1), "wizardlamp_target_test");
            var spell = (Wizardlamp) SpellRegistry.WIZARDLAMP.get();
            var eyePosition = player.getEyePosition(1.0F);

            var inRangePos = BlockPos.containing(eyePosition.add(2.0, 0.0, 0.0));
            var inRangeTarget = new BlockTargetData();
            inRangeTarget.setTarget(inRangePos, Direction.UP, Vec3.atCenterOf(inRangePos), inRangePos, Direction.DOWN);
            helper.assertTrue(
                    Wizardlamp.resolveClientPlacePos(level, player, inRangeTarget, 6.0).orElseThrow().equals(inRangePos),
                    "Wizardlamp should keep an in-range client placement cell"
            );

            var farPos = BlockPos.containing(eyePosition.add(12.0, 0.0, 0.0));
            var farTarget = new BlockTargetData();
            farTarget.setTarget(farPos, Direction.UP, Vec3.atCenterOf(farPos), farPos, Direction.DOWN);
            var offset = Vec3.atCenterOf(farPos).subtract(eyePosition);
            var expectedPos = BlockPos.containing(eyePosition.add(offset.normalize().scale(6.0)));
            var wallPos = BlockPos.containing(eyePosition.add(offset.normalize().scale(2.0)));
            level.setBlockAndUpdate(wallPos, Blocks.STONE.defaultBlockState());

            BlockTargetingHelper.setPendingServerTarget(player, spell.getSpellResource(), farTarget);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Wizardlamp test could not resolve player magic data");
            helper.assertTrue(
                    spell.checkPreCastConditions(level, 1, player, magicData),
                    "Wizardlamp should accept a replaceable clamped cell without requiring line of sight"
            );
            spell.onCast(level, 1, player, CastSource.SPELLBOOK, magicData);

            helper.assertTrue(
                    level.getBlockState(expectedPos).is(BlockRegistry.WIZARDLAMP_LANTERN.get()),
                    "Wizardlamp should place its lantern at the maximum-range cell along the client vector"
            );
            helper.assertFalse(level.getBlockState(expectedPos).getValue(WizardlampLanternBlock.WATERLOGGED),
                    "Wizardlamp lantern placed in air should remain dry");
            helper.assertTrue(
                    level.getBlockState(expectedPos.below()).isAir(),
                    "Wizardlamp lantern should remain placed without a supporting block"
            );
            helper.assertTrue(
                    !level.getBlockState(farPos).is(BlockRegistry.WIZARDLAMP_LANTERN.get()),
                    "Wizardlamp should not trust an out-of-range placement cell directly"
            );
        });
    }

    static void blockPlacementSpellsRespectForgePlaceEvent(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var wizardlampPlayer = createEquipmentTestPlayer(
                    helper, new BlockPos(1, 2, 1), "wizardlamp_place_event_test");
            var mageLightPlayer = createEquipmentTestPlayer(
                    helper, new BlockPos(1, 2, 3), "mage_light_place_event_test");
            var linearBuildPlayer = createEquipmentTestPlayer(
                    helper, new BlockPos(2, 3, 2), "linear_build_place_event_test");
            var wizardlampPos = helper.absolutePos(new BlockPos(2, 3, 1));
            var mageLightPos = helper.absolutePos(new BlockPos(2, 3, 3));
            var linearBuildPos = new BlockPos(3, 3, 2);
            var wizardlampEvents = new AtomicInteger();
            var mageLightEvents = new AtomicInteger();
            var linearBuildEvents = new AtomicInteger();
            helper.setBlock(new BlockPos(2, 3, 1), Blocks.WATER);

            Consumer<BlockEvent.EntityPlaceEvent> cancelListener = event -> {
                if (event.getEntity() == wizardlampPlayer
                        && event.getPlacedBlock().is(BlockRegistry.WIZARDLAMP_LANTERN.get())) {
                    wizardlampEvents.incrementAndGet();
                    event.setCanceled(true);
                } else if (event.getEntity() == mageLightPlayer
                        && event.getPlacedBlock().is(BlockRegistry.MAGE_LIGHT_TORCH.get())) {
                    mageLightEvents.incrementAndGet();
                    event.setCanceled(true);
                } else if (event.getEntity() == linearBuildPlayer
                        && event.getPlacedBlock().is(Blocks.OAK_PLANKS)) {
                    linearBuildEvents.incrementAndGet();
                    event.setCanceled(true);
                }
            };

            NeoForge.EVENT_BUS.addListener(cancelListener);
            try {
                var wizardlamp = (Wizardlamp) SpellRegistry.WIZARDLAMP.get();
                var wizardlampTarget = new BlockTargetData();
                wizardlampTarget.setTarget(
                        wizardlampPos,
                        Direction.UP,
                        Vec3.atCenterOf(wizardlampPos),
                        wizardlampPos,
                        Direction.DOWN
                );
                BlockTargetingHelper.setPendingServerTarget(
                        wizardlampPlayer, wizardlamp.getSpellResource(), wizardlampTarget);
                var wizardlampMagicData = MagicData.getPlayerMagicData(wizardlampPlayer);
                helper.assertTrue(wizardlamp.checkPreCastConditions(
                                level, 1, wizardlampPlayer, wizardlampMagicData),
                        "Wizardlamp pre-cast check should accept the prepared target");
                wizardlamp.onCast(level, 1, wizardlampPlayer, CastSource.SPELLBOOK, wizardlampMagicData);

                var mageLight = (MageLight) SpellRegistry.MAGE_LIGHT.get();
                var mageLightTarget = new BlockTargetData();
                mageLightTarget.setTarget(
                        mageLightPos,
                        Direction.UP,
                        Vec3.atCenterOf(mageLightPos),
                        mageLightPos,
                        Direction.DOWN
                );
                BlockTargetingHelper.setPendingServerTarget(
                        mageLightPlayer, mageLight.getSpellResource(), mageLightTarget);
                var mageLightMagicData = MagicData.getPlayerMagicData(mageLightPlayer);
                helper.assertTrue(mageLight.checkPreCastConditions(
                                level, 1, mageLightPlayer, mageLightMagicData),
                        "Mage Light pre-cast check should accept the prepared target");
                mageLight.onCast(level, 1, mageLightPlayer, CastSource.SPELLBOOK, mageLightMagicData);

                linearBuildPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 2));
                helper.setBlock(new BlockPos(4, 3, 2), Blocks.STONE);
                MagicData.getPlayerMagicData(linearBuildPlayer).setMana(20.0F);
                castLinearBuildWithCurrentMana(
                        helper, linearBuildPlayer, new BlockPos(4, 3, 2), Direction.WEST);
            } finally {
                NeoForge.EVENT_BUS.unregister(cancelListener);
            }

            helper.assertTrue(wizardlampEvents.get() == 1,
                    "Wizardlamp should fire one Forge EntityPlaceEvent");
            helper.assertTrue(mageLightEvents.get() == 1,
                    "Mage Light should fire one Forge EntityPlaceEvent");
            helper.assertTrue(linearBuildEvents.get() == 1,
                    "Linear Build should keep using the Forge block placement event path");
            helper.assertBlockPresent(Blocks.WATER, new BlockPos(2, 3, 1));
            helper.assertBlockNotPresent(BlockRegistry.MAGE_LIGHT_TORCH.get(), new BlockPos(2, 3, 3));
            helper.assertBlockNotPresent(Blocks.OAK_PLANKS, linearBuildPos);
            helper.assertTrue(linearBuildPlayer.getMainHandItem().getCount() == 2,
                    "Canceled Linear Build placement should not consume its source block");
            helper.assertTrue(Math.abs(MagicData.getPlayerMagicData(linearBuildPlayer).getMana() - 20.0F) < 1e-4F,
                    "Canceled Linear Build placement should not consume additional mana");
        });
    }

    static void wizardlampLanternHasLanternCollisionAndNoDrops(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var absolutePos = helper.absolutePos(new BlockPos(2, 3, 2));
            var state = BlockRegistry.WIZARDLAMP_LANTERN.get().defaultBlockState();
            level.setBlockAndUpdate(absolutePos, state);

            helper.assertTrue(state.canSurvive(level, absolutePos),
                    "Wizardlamp lantern should survive without support");
            helper.assertTrue(!state.getCollisionShape(level, absolutePos).isEmpty(),
                    "Wizardlamp lantern should have a collision shape");
            var outlineShape = state.getShape(level, absolutePos);
            var collisionShape = state.getCollisionShape(level, absolutePos);
            helper.assertTrue(
                    Math.abs(outlineShape.min(Direction.Axis.Y) - WizardlampLanternBlock.FLOATING_OFFSET) < 0.0001D,
                    "Wizardlamp lantern outline should be raised with its rendered model"
            );
            helper.assertTrue(
                    Math.abs(collisionShape.min(Direction.Axis.Y) - WizardlampLanternBlock.FLOATING_OFFSET) < 0.0001D,
                    "Wizardlamp lantern collision should be raised with its rendered model"
            );
            helper.assertTrue(state.getLightEmission() == 15,
                    "Wizardlamp lantern should emit maximum block light");

            var player = createEquipmentTestPlayer(helper, new BlockPos(1, 2, 1), "wizardlamp_break_test");
            var bareHandProgress = state.getDestroyProgress(player, level, absolutePos);
            helper.assertTrue(Math.abs(state.getDestroySpeed(level, absolutePos) - 0.3F) < 0.0001F,
                    "Wizardlamp lantern hardness should produce a 30-tick bare-hand break time");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
            var pickaxeProgress = state.getDestroyProgress(player, level, absolutePos);
            helper.assertTrue(pickaxeProgress > bareHandProgress,
                    "Wizardlamp lantern should break faster with a pickaxe");

            var drops = Block.getDrops(
                    state,
                    level,
                    absolutePos,
                    level.getBlockEntity(absolutePos),
                    player,
                    player.getMainHandItem()
            );
            helper.assertTrue(drops.isEmpty(), "Wizardlamp lantern should not drop items");
        });
    }

    static void wizardlampSupportsWaterloggingWhileMageLightRemainsDry(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var wizardlampPlayer = createEquipmentTestPlayer(
                    helper, new BlockPos(1, 2, 1), "wizardlamp_waterlog_test");
            var mageLightPlayer = createEquipmentTestPlayer(
                    helper, new BlockPos(1, 2, 3), "mage_light_waterlog_test");
            var wizardlampPos = helper.absolutePos(new BlockPos(2, 3, 1));
            var mageLightPos = helper.absolutePos(new BlockPos(2, 3, 3));
            helper.setBlock(new BlockPos(2, 3, 1), Blocks.WATER);
            helper.setBlock(new BlockPos(2, 3, 3), Blocks.WATER);

            var wizardlamp = (Wizardlamp) SpellRegistry.WIZARDLAMP.get();
            var wizardlampTarget = new BlockTargetData();
            wizardlampTarget.setTarget(
                    wizardlampPos,
                    Direction.UP,
                    Vec3.atCenterOf(wizardlampPos),
                    wizardlampPos,
                    Direction.DOWN
            );
            BlockTargetingHelper.setPendingServerTarget(
                    wizardlampPlayer, wizardlamp.getSpellResource(), wizardlampTarget);
            var wizardlampMagicData = MagicData.getPlayerMagicData(wizardlampPlayer);
            helper.assertTrue(wizardlamp.checkPreCastConditions(
                            level, 1, wizardlampPlayer, wizardlampMagicData),
                    "Wizardlamp pre-cast check should accept a water source target");
            wizardlamp.onCast(level, 1, wizardlampPlayer, CastSource.SPELLBOOK, wizardlampMagicData);

            var wizardlampState = level.getBlockState(wizardlampPos);
            helper.assertTrue(wizardlampState.is(BlockRegistry.WIZARDLAMP_LANTERN.get()),
                    "Wizardlamp should place its lantern in a water source");
            helper.assertTrue(wizardlampState.getValue(WizardlampLanternBlock.WATERLOGGED),
                    "Wizardlamp lantern placed in a water source should be waterlogged");
            helper.assertTrue(wizardlampState.getFluidState().isSource(),
                    "Waterlogged Wizardlamp lantern should retain source water");

            var mageLight = (MageLight) SpellRegistry.MAGE_LIGHT.get();
            var mageLightTarget = new BlockTargetData();
            mageLightTarget.setTarget(
                    mageLightPos,
                    Direction.UP,
                    Vec3.atCenterOf(mageLightPos),
                    mageLightPos,
                    Direction.DOWN
            );
            BlockTargetingHelper.setPendingServerTarget(
                    mageLightPlayer, mageLight.getSpellResource(), mageLightTarget);
            var mageLightMagicData = MagicData.getPlayerMagicData(mageLightPlayer);
            helper.assertTrue(mageLight.checkPreCastConditions(
                            level, 1, mageLightPlayer, mageLightMagicData),
                    "Mage Light pre-cast check should accept a water source target");
            mageLight.onCast(level, 1, mageLightPlayer, CastSource.SPELLBOOK, mageLightMagicData);

            var mageLightState = level.getBlockState(mageLightPos);
            helper.assertTrue(mageLightState.is(BlockRegistry.MAGE_LIGHT_TORCH.get()),
                    "Mage Light should still replace a water source with its torch");
            helper.assertTrue(mageLightState.getFluidState().isEmpty(),
                    "Mage Light torch should remain non-waterlogged");

            var bucketTestPos = helper.absolutePos(new BlockPos(4, 3, 2));
            var lanternBlock = (WizardlampLanternBlock) BlockRegistry.WIZARDLAMP_LANTERN.get();
            level.setBlockAndUpdate(bucketTestPos, lanternBlock.defaultBlockState());
            var blockEntity = level.getBlockEntity(bucketTestPos);
            helper.assertTrue(lanternBlock.placeLiquid(
                            level,
                            bucketTestPos,
                            level.getBlockState(bucketTestPos),
                            Fluids.WATER.getSource(false)
                    ),
                    "Wizardlamp lantern should accept water from a bucket");
            helper.assertTrue(level.getBlockState(bucketTestPos).getValue(WizardlampLanternBlock.WATERLOGGED),
                    "Wizardlamp lantern should become waterlogged after accepting water");
            helper.assertTrue(level.getBlockEntity(bucketTestPos) == blockEntity,
                    "Waterlogging a Wizardlamp lantern should preserve its block entity");

            var pickedUp = lanternBlock.pickupBlock(
                    wizardlampPlayer,
                    level,
                    bucketTestPos,
                    level.getBlockState(bucketTestPos)
            );
            helper.assertTrue(pickedUp.is(Items.WATER_BUCKET),
                    "Wizardlamp lantern should return a water bucket when drained");
            helper.assertFalse(level.getBlockState(bucketTestPos).getValue(WizardlampLanternBlock.WATERLOGGED),
                    "Drained Wizardlamp lantern should no longer be waterlogged");
            helper.assertTrue(level.getBlockEntity(bucketTestPos) == blockEntity,
                    "Draining a Wizardlamp lantern should preserve its block entity");
        });
    }
}
