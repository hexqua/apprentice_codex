package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.FloatmountBroomServerConfig;
import jp.aquafactory.apprenticecodex.config.item.HoverrideBroomServerConfig;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.BroomInputTransition;
import jp.aquafactory.apprenticecodex.entity.broom.BroomDismountEvents;
import jp.aquafactory.apprenticecodex.entity.broom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.FloatmountBroomMovement;
import jp.aquafactory.apprenticecodex.entity.broom.BroomSurfaceScanner;
import jp.aquafactory.apprenticecodex.entity.broom.BroomCoreWarningState;
import jp.aquafactory.apprenticecodex.entity.broom.BroomSpellSelectionEvents;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomMovement;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomPresentation;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomRushAttack;
import jp.aquafactory.apprenticecodex.item.broom.AbstractBroomItem;
import jp.aquafactory.apprenticecodex.item.broom.BroomCurioSupport;
import jp.aquafactory.apprenticecodex.item.broom.BroomDeploymentState;
import jp.aquafactory.apprenticecodex.item.broom.FloatmountBroomItem;
import jp.aquafactory.apprenticecodex.item.broom.HoverrideBroomItem;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.FloatmountBroomConfigState;
import jp.aquafactory.apprenticecodex.network.packet.SyncFloatmountBroomConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientBroomInputPacket;
import jp.aquafactory.apprenticecodex.network.packet.HoverrideBroomReleaseResultPacket;
import jp.aquafactory.apprenticecodex.network.packet.HoverrideBroomImpulseEffectPacket;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.callbroom.CallBroomDeploymentEvents;
import jp.aquafactory.apprenticecodex.spell.callbroom.CallBroomDeploymentManager;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.GameType;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FloatmountBroomGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final BlockPos TEST_POS = new BlockPos(1, 1, 1);
    private static final BlockPos VIBRATION_SENSOR_POS = new BlockPos(4, 1, 1);

    private FloatmountBroomGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void registrationsResolveDedicatedTypes(GameTestHelper helper) {
        helper.assertTrue(ItemRegistry.FLOATMOUNT_BROOM.get() instanceof FloatmountBroomItem,
                "Floatmount Broom item should use its dedicated implementation");
        var broom = EntityRegistry.FLOATMOUNT_BROOM.get().create(helper.getLevel());
        helper.assertTrue(broom instanceof FloatmountBroomEntity,
                "Floatmount Broom entity type should create its dedicated entity");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideRegistrationsResolveDedicatedTypes(GameTestHelper helper) {
        helper.assertTrue(ItemRegistry.HOVERRIDE_BROOM.get() instanceof HoverrideBroomItem,
                "Hoverride Broom item should use its dedicated implementation");
        var broom = EntityRegistry.HOVERRIDE_BROOM.get().create(helper.getLevel());
        helper.assertTrue(broom instanceof HoverrideBroomEntity,
                "Hoverride Broom entity type should create its dedicated entity");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 90)
    public static void mountedFloatmountRepeatedlyEmitsFlightVibrationWhileStationary(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var player = mountForVibrationTest(helper, broom, "floatmount_vibration_rider");
        placeVibrationSensor(helper);

        helper.runAfterDelay(20, () -> {
            assertVibrationSensorActive(helper,
                    "Stationary low-altitude Floatmount riding should emit an Elytra flight vibration");
            helper.runAfterDelay(50, () -> {
                assertVibrationSensorActive(helper,
                        "Continuous Floatmount riding should emit another vibration after the sensor resets");
                player.stopRiding();
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void mountedHoverrideEmitsFlightVibrationWhileStationary(GameTestHelper helper) {
        var broom = spawnHoverrideBroom(helper, 1.5D);
        var player = mountForVibrationTest(helper, broom, "hoverride_vibration_rider");
        placeVibrationSensor(helper);

        helper.runAfterDelay(20, () -> {
            assertVibrationSensorActive(helper,
                    "Stationary low-altitude Hoverride riding should emit an Elytra flight vibration");
            player.stopRiding();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void unoccupiedBroomDoesNotEmitFlightVibration(GameTestHelper helper) {
        spawnBroom(helper, 1.5D);
        placeVibrationSensor(helper);

        helper.runAfterDelay(25, () -> {
            helper.assertFalse(isVibrationSensorActive(helper),
                    "An unoccupied broom must not emit an Elytra flight vibration");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 50)
    public static void deepSensorSilencesMountedBroomFlightVibration(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var player = serverRider(helper, "floatmount_deep_sensor_rider");
        var senseSensor = EffectRegistry.SENSE_SENSOR.get();
        player.addEffect(new MobEffectInstance(senseSensor, 60, 0));
        mountForVibrationTest(helper, broom, player);
        placeVibrationSensor(helper);

        helper.runAfterDelay(35, () -> {
            helper.assertFalse(isVibrationSensorActive(helper),
                    "Deep Sensor should silence player-sourced broom flight vibrations");
            player.stopRiding();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void calibratedBroomsUseBackSlotWithoutQuickEquip(GameTestHelper helper) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "broom_curio_contract_test"
        );
        var backContext = new SlotContext(CuriosSlotConstants.BACK, player, 0, false, true);
        var beltContext = new SlotContext(CuriosSlotConstants.BELT, player, 0, false, true);

        for (var broom : List.of(
                (AbstractBroomItem) ItemRegistry.FLOATMOUNT_BROOM.get(),
                (AbstractBroomItem) ItemRegistry.HOVERRIDE_BROOM.get()
        )) {
            var stack = new ItemStack(broom);
            // 1.20.1ではcapability付与がstack生成時だけなので、装備可否だけをNBTで切り替える。
            helper.assertTrue(CuriosApi.getCurio(stack).isPresent(),
                    "Broom should retain its Curio capability before saddle calibration");
            helper.assertFalse(stack.is(BowGameTestSupport.CURIOS_BACK),
                    "Broom should not use the unconditional Curios back tag");
            helper.assertFalse(CuriosApi.getItemStackSlots(stack, player).containsKey(CuriosSlotConstants.BACK),
                    "Uncalibrated broom should not expose the Curios back slot");
            helper.assertFalse(broom.canEquip(backContext, stack),
                    "Uncalibrated broom should not be equippable in the Curios back slot");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 2, new ItemStack(Items.SADDLE)),
                    "Broom should accept a saddle as its Curios calibration");
            helper.assertTrue(AbstractBroomItem.isBackCurioEnabled(stack),
                    "Saddle-calibrated broom should enable Curios support");
            helper.assertTrue(CuriosApi.getItemStackSlots(stack, player).containsKey(CuriosSlotConstants.BACK),
                    "Saddle-calibrated broom should expose the Curios back slot");
            helper.assertTrue(broom.canEquip(backContext, stack),
                    "Saddle-calibrated broom should be equippable in the Curios back slot");
            helper.assertFalse(broom.canEquip(beltContext, stack),
                    "Broom should reject non-back Curios slots");
            helper.assertFalse(broom.canEquipFromUse(backContext, stack),
                    "Broom should preserve its normal right-click placement instead of quick-equipping");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 1, new ItemStack(Items.SADDLE)),
                    "Broom should reject a duplicate Curios calibration");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 2, ItemStack.EMPTY),
                    "Broom should allow removing its Curios calibration");
            helper.assertFalse(AbstractBroomItem.isBackCurioEnabled(stack),
                    "Removing the saddle should disable Curios support");
            helper.assertTrue(CuriosApi.getCurio(stack).isPresent(),
                    "Removing the saddle should not detach the existing Curio capability");
            helper.assertFalse(CuriosApi.getItemStackSlots(stack, player).containsKey(CuriosSlotConstants.BACK),
                    "Removing the saddle should hide the Curios back slot on the same stack");
            helper.assertFalse(broom.canEquip(backContext, stack),
                    "Removing the saddle should make the same stack unequippable");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void broomsAcceptOneFirewardRingCalibration(GameTestHelper helper) {
        for (var broomItem : List.of(
                (AbstractBroomItem) ItemRegistry.FLOATMOUNT_BROOM.get(),
                (AbstractBroomItem) ItemRegistry.HOVERRIDE_BROOM.get()
        )) {
            var stack = new ItemStack(broomItem);
            var firewardRing = new ItemStack(
                    io.redspace.ironsspellbooks.registries.ItemRegistry.FIREWARD_RING.get()
            );
            var firewardRule = broomItem.getCalibrationAdjustmentProfile(stack).rules().stream()
                    .filter(rule -> rule.displayId().equals("gain_fireward"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing broom Fireward calibration rule"));
            var effectLines = firewardRule.effectLines();

            helper.assertTrue(effectLines.size() == 2,
                    "Broom Fireward calibration should expose two JEI effect lines");
            helper.assertTrue(effectLines.get(0).getContents() instanceof TranslatableContents firstLine
                            && firstLine.getKey().equals(
                            "jei.apprenticecodex.spell_calibration_bench.effect.gain_fireward_1"),
                    "Broom Fireward calibration should use its first dedicated JEI effect key");
            helper.assertTrue(effectLines.get(1).getContents() instanceof TranslatableContents secondLine
                            && secondLine.getKey().equals(
                            "jei.apprenticecodex.spell_calibration_bench.effect.gain_fireward_2"),
                    "Broom Fireward calibration should use its broom-only JEI effect key");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 0, firewardRing),
                    "Broom should accept a Fireward Ring calibration");
            helper.assertTrue(AbstractBroomItem.isFirewardEnabled(stack),
                    "Fireward-calibrated broom stack should enable fire immunity");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 1, firewardRing),
                    "Broom should reject a duplicate Fireward Ring calibration");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 0, ItemStack.EMPTY),
                    "Broom should allow removing its Fireward Ring calibration");
            helper.assertFalse(AbstractBroomItem.isFirewardEnabled(stack),
                    "Removing the Fireward Ring should disable fire immunity");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void broomsAcceptOneOverdriveEngineCalibration(GameTestHelper helper) {
        for (var broomItem : List.of(
                (AbstractBroomItem) ItemRegistry.FLOATMOUNT_BROOM.get(),
                (AbstractBroomItem) ItemRegistry.HOVERRIDE_BROOM.get()
        )) {
            var stack = new ItemStack(broomItem);
            var engine = new ItemStack(ItemRegistry.OVERDRIVE_BROOM_ENGINE.get());
            var displayId = broomItem instanceof FloatmountBroomItem
                    ? "overdrive_engine_floatmount"
                    : "overdrive_engine_hoverride";
            var keyVariant = broomItem instanceof FloatmountBroomItem ? "float" : "hover";
            var rule = broomItem.getCalibrationAdjustmentProfile(stack).rules().stream()
                    .filter(candidate -> candidate.displayId().equals(displayId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing broom Overdrive calibration rule"));

            helper.assertTrue(rule.effectLines().size() == 2,
                    "Broom Overdrive calibration should expose two JEI effect lines");
            for (var line = 0; line < 2; ++line) {
                var expectedKey = "jei.apprenticecodex.spell_calibration_bench.effect.overdrive_engine."
                        + keyVariant + "_" + (line + 1);
                helper.assertTrue(rule.effectLines().get(line).getContents() instanceof TranslatableContents contents
                                && contents.getKey().equals(expectedKey),
                        "Broom Overdrive calibration should use its broom-specific JEI effect key");
            }
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 0, engine),
                    "Broom should accept an Overdrive Broom Engine calibration");
            helper.assertTrue(AbstractBroomItem.isOverdriveEnabled(stack),
                    "Engine-calibrated broom stack should enable Overdrive");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 1, engine),
                    "Broom should reject a duplicate Overdrive Broom Engine calibration");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 0, ItemStack.EMPTY),
                    "Broom should allow removing its Overdrive Broom Engine calibration");
            helper.assertFalse(AbstractBroomItem.isOverdriveEnabled(stack),
                    "Removing the Overdrive Broom Engine should disable Overdrive");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void onlyFloatmountAcceptsOneAquaticCalibration(GameTestHelper helper) {
        var floatmountItem = (FloatmountBroomItem) ItemRegistry.FLOATMOUNT_BROOM.get();
        var floatmount = new ItemStack(floatmountItem);
        var heartOfTheSea = new ItemStack(Items.HEART_OF_THE_SEA);
        var aquaticRule = floatmountItem.getCalibrationAdjustmentProfile(floatmount).rules().stream()
                .filter(rule -> rule.displayId().equals("adapt_underwater_mobility"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing Floatmount aquatic calibration rule"));

        helper.assertTrue(aquaticRule.effectLines().size() == 2,
                "Floatmount aquatic calibration should expose two JEI effect lines");
        helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        floatmount, 0, heartOfTheSea),
                "Floatmount Broom should accept Heart of the Sea as a calibration");
        helper.assertTrue(FloatmountBroomItem.isAquaticCalibrationEnabled(floatmount),
                "Heart of the Sea should enable Floatmount aquatic calibration");
        helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        floatmount, 1, heartOfTheSea),
                "Floatmount Broom should reject duplicate aquatic calibrations");

        var hoverride = new ItemStack(ItemRegistry.HOVERRIDE_BROOM.get());
        helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        hoverride, 0, heartOfTheSea),
                "Hoverride Broom should reject Heart of the Sea calibration");
        CalibrationAdjustmentStorage.set(
                hoverride, 0, AbstractBroomItem.CALIBRATION_ADJUSTMENT_SLOT_COUNT, heartOfTheSea
        );
        helper.assertFalse(FloatmountBroomItem.isAquaticCalibrationEnabled(hoverride),
                "Stored Heart of the Sea data must not enable aquatic calibration for Hoverride Broom");

        var broom = spawnBroom(helper, 1.5D);
        broom.setBroomItemStack(floatmount);
        helper.assertTrue(broom.isAquaticCalibrationEnabled(),
                "Deployed Floatmount Broom should derive aquatic calibration from its stored stack");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void floatmountWaterMovementLimitsUseSpecifiedFactors(GameTestHelper helper) {
        helper.assertTrue(Math.abs(FloatmountBroomMovement.horizontalAcceleration(true) - 0.02D) < 1.0e-9D,
                "Water should halve Floatmount horizontal acceleration");
        helper.assertTrue(Math.abs(FloatmountBroomMovement.maximumHorizontalSpeed(false, true) - 0.21D) < 1.0e-9D,
                "Water should reduce Floatmount maximum horizontal speed to sixty percent");
        helper.assertTrue(Math.abs(FloatmountBroomMovement.maximumHorizontalSpeed(true, true) - 0.06D) < 1.0e-9D,
                "Water should reduce emergency maximum horizontal speed to sixty percent");
        helper.assertTrue(Math.abs(FloatmountBroomMovement.maximumVerticalSpeed(true) - 0.09D) < 1.0e-9D,
                "Water should reduce Floatmount vertical speed to sixty percent");
        helper.assertTrue(Math.abs(FloatmountBroomMovement.maximumUnoccupiedVerticalSpeed(true) - 0.06D) < 1.0e-9D,
                "Water should reduce unoccupied Floatmount rise and fall speed to sixty percent");
        helper.assertTrue(Math.abs(
                        FloatmountBroomMovement.horizontalAcceleration(false, true) - 0.056D
                ) < 1.0e-9D,
                "Overdrive should increase Floatmount horizontal acceleration by forty percent");
        helper.assertTrue(Math.abs(
                        FloatmountBroomMovement.maximumHorizontalSpeed(false, false, true) - 0.49D
                ) < 1.0e-9D,
                "Overdrive should increase Floatmount maximum horizontal speed by forty percent");
        helper.assertTrue(Math.abs(
                        FloatmountBroomMovement.maximumHorizontalSpeed(false, true, true) - 0.294D
                ) < 1.0e-9D,
                "Water penalty should apply after the Floatmount Overdrive speed increase");
        helper.assertTrue(Math.abs(
                        FloatmountBroomMovement.maximumHorizontalSpeed(true, false, true) - 0.1D
                ) < 1.0e-9D,
                "Overdrive must not increase Floatmount emergency landing speed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void broomsAllowOnlyOneEquippedAcrossExpandedSlots(GameTestHelper helper) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "broom_expanded_back_limit_test"
        );
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for broom test"));
        curiosInventory.addTransientSlotModifier(
                CuriosSlotConstants.BACK,
                UUID.fromString("fd917056-c011-432e-a5d7-8f574c08d5cd"),
                "apprenticecodex:gametest/broom_expanded_back",
                1.0D,
                AttributeModifier.Operation.ADDITION
        );

        var floatmount = (AbstractBroomItem) ItemRegistry.FLOATMOUNT_BROOM.get();
        var hoverride = (AbstractBroomItem) ItemRegistry.HOVERRIDE_BROOM.get();
        var equippedStack = backCurioBroomStack(floatmount);
        curiosInventory.setEquippedCurio(CuriosSlotConstants.BACK, 0, equippedStack);
        var currentContext = new SlotContext(CuriosSlotConstants.BACK, player, 0, false, true);
        var secondContext = new SlotContext(CuriosSlotConstants.BACK, player, 1, false, true);

        helper.assertTrue(floatmount.canEquip(currentContext, equippedStack),
                "Equipped broom should remain valid in its current slot");
        helper.assertFalse(floatmount.canEquip(secondContext, backCurioBroomStack(floatmount)),
                "Broom should reject a second copy in an expanded back slot");
        helper.assertFalse(hoverride.canEquip(secondContext, backCurioBroomStack(hoverride)),
                "Floatmount and Hoverride Brooms should be mutually exclusive");
        var quiverPlayer = BowGameTestSupport.createEquipmentTestPlayer(
                helper,
                new BlockPos(1, 2, 0),
                "broom_quiver_coexistence_test"
        );
        var quiverInventory = CuriosApi.getCuriosInventory(quiverPlayer)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for quiver test"));
        quiverInventory.addTransientSlotModifier(
                CuriosSlotConstants.BACK,
                UUID.fromString("f3ba4970-b8e9-4ad8-a228-688198f2af19"),
                "apprenticecodex:gametest/broom_quiver_back",
                1.0D,
                AttributeModifier.Operation.ADDITION
        );
        quiverInventory.setEquippedCurio(
                CuriosSlotConstants.BACK,
                0,
                new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get())
        );
        helper.assertTrue(floatmount.canEquip(
                        new SlotContext(CuriosSlotConstants.BACK, quiverPlayer, 1, false, true),
                        backCurioBroomStack(floatmount)
                ),
                "Spellcaster Quiver should not block a broom when another back slot exists");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void equippedBroomAddsCallBroomSpellSelection(GameTestHelper helper) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "call_broom_spell_selection_test"
        );
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for spell selection test"));

        helper.assertTrue(new SpellSelectionManager(player)
                        .getSpellsForSlot(BroomCurioSupport.SPELL_SELECTION_SLOT).isEmpty(),
                "Call Broom should not be selected without an equipped broom");
        var floatmount = backCurioCalibratedBroomStack(ItemRegistry.FLOATMOUNT_BROOM.get(), 1);
        curiosInventory.setEquippedCurio(
                CuriosSlotConstants.BACK,
                0,
                floatmount
        );
        assertSingleCallBroomSelection(helper, player, "Floatmount Broom");
        helper.assertTrue(new SpellSelectionManager(player)
                        .getSpellsForSlot(BroomSpellSelectionEvents.SPELL_SELECTION_SLOT).isEmpty(),
                "A calibrated Floatmount Broom should not expose scrolls while only equipped as a Curio");
        helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        floatmount, 2, ItemStack.EMPTY),
                "Equipped broom should allow removing its Curios calibration");
        helper.assertTrue(BroomCurioSupport.findUniqueEquippedBroom(player).isEmpty(),
                "Broom should stop being a valid casting source after Curios calibration is removed");
        helper.assertTrue(new SpellSelectionManager(player)
                        .getSpellsForSlot(BroomCurioSupport.SPELL_SELECTION_SLOT).isEmpty(),
                "Call Broom should disappear on the next spell selection update");
        helper.assertFalse(SpellRegistry.CALL_BROOM.get().checkPreCastConditions(
                        helper.getLevel(), 1, player, magicData(helper, player)),
                "Call Broom should reject an equipped broom whose Curios calibration was removed");

        var hoverride = backCurioCalibratedBroomStack(ItemRegistry.HOVERRIDE_BROOM.get(), 1);
        curiosInventory.setEquippedCurio(
                CuriosSlotConstants.BACK,
                0,
                hoverride
        );
        assertSingleCallBroomSelection(helper, player, "Hoverride Broom");
        helper.assertTrue(new SpellSelectionManager(player)
                        .getSpellsForSlot(BroomSpellSelectionEvents.SPELL_SELECTION_SLOT).isEmpty(),
                "A calibrated Hoverride Broom should not expose scrolls while only equipped as a Curio");
        curiosInventory.setEquippedCurio(CuriosSlotConstants.BACK, 0, ItemStack.EMPTY);
        helper.assertTrue(new SpellSelectionManager(player)
                        .getSpellsForSlot(BroomCurioSupport.SPELL_SELECTION_SLOT).isEmpty(),
                "Call Broom should be removed after unequipping the broom");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void broomCalibrationSlotsOnlySelectWhileControllingAndDisabledScrollCanBeRemoved(
            GameTestHelper helper
    ) {
        assertBroomCalibrationSelection(
                helper,
                ItemRegistry.FLOATMOUNT_BROOM.get(),
                EntityRegistry.FLOATMOUNT_BROOM.get().create(helper.getLevel()),
                0,
                "Floatmount"
        );
        assertBroomCalibrationSelection(
                helper,
                ItemRegistry.HOVERRIDE_BROOM.get(),
                EntityRegistry.HOVERRIDE_BROOM.get().create(helper.getLevel()),
                3,
                "Hoverride"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void invalidBroomEquipmentDoesNotAddCallBroomSelection(GameTestHelper helper) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "invalid_broom_spell_selection_test"
        );
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for invalid broom test"));
        curiosInventory.addTransientSlotModifier(
                CuriosSlotConstants.BACK,
                UUID.fromString("ffb43636-5457-47d2-a94c-194d6216c33a"),
                "apprenticecodex:gametest/invalid_broom_back",
                1.0D,
                AttributeModifier.Operation.ADDITION
        );
        var backHandler = curiosInventory.getCurios().get(CuriosSlotConstants.BACK);
        helper.assertTrue(backHandler != null && backHandler.getStacks().getSlots() >= 2,
                "Invalid broom test should provide two back slots");
        backHandler.getStacks().setStackInSlot(0, backCurioBroomStack(ItemRegistry.FLOATMOUNT_BROOM.get()));
        backHandler.getStacks().setStackInSlot(1, backCurioBroomStack(ItemRegistry.HOVERRIDE_BROOM.get()));

        helper.assertTrue(BroomCurioSupport.findUniqueEquippedBroom(player).isEmpty(),
                "Multiple equipped brooms should not resolve as a valid casting source");
        helper.assertTrue(new SpellSelectionManager(player)
                        .getSpellsForSlot(BroomCurioSupport.SPELL_SELECTION_SLOT).isEmpty(),
                "Invalid duplicate broom equipment should not add Call Broom");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void callBroomRequiresEquippedBroomAndIgnoresFloatmountMountThreshold(GameTestHelper helper) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "call_broom_precondition_test"
        );
        var magicData = magicData(helper, player);
        magicData.setMana(20.0F);
        helper.assertFalse(SpellRegistry.CALL_BROOM.get().checkPreCastConditions(
                        helper.getLevel(), 1, player, magicData),
                "Call Broom obtained directly must fail without an equipped broom");

        var stack = equipBroom(player, ItemRegistry.FLOATMOUNT_BROOM.get());
        helper.assertTrue(SpellRegistry.CALL_BROOM.get().checkPreCastConditions(
                        helper.getLevel(), 1, player, magicData),
                "Call Broom should ignore the higher Floatmount mount threshold");
        helper.assertFalse(BroomDeploymentState.isDeployed(stack),
                "Precondition checks must not mutate deployment state");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void callBroomDeploysAndRecallsBothTypesWithoutItemizing(GameTestHelper helper) {
        assertCallBroomDeployAndRecall(helper, ItemRegistry.FLOATMOUNT_BROOM.get(), 0, "Floatmount");
        assertCallBroomDeployAndRecall(helper, ItemRegistry.HOVERRIDE_BROOM.get(), 2, "Hoverride");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void calledBroomRejectsThirdPartyAndRecallsBeyondEightBlocks(GameTestHelper helper) {
        var owner = calledBroomPlayer(helper, new BlockPos(0, 2, 0), "called_broom_owner_test");
        var thirdParty = calledBroomPlayer(helper, new BlockPos(1, 2, 0), "called_broom_third_party_test");
        var stack = equipBroom(owner, ItemRegistry.FLOATMOUNT_BROOM.get());
        helper.assertTrue(CallBroomDeploymentManager.execute(owner), "Call Broom should deploy for its owner");
        var broom = (AbstractBroomEntity) owner.getVehicle();
        owner.stopRiding();

        thirdParty.setShiftKeyDown(true);
        helper.assertTrue(broom.interact(thirdParty, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME,
                "A third party should not recover a called broom");
        thirdParty.setShiftKeyDown(false);
        helper.assertTrue(broom.interact(thirdParty, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME,
                "A third party should not mount a called broom");
        helper.assertFalse(thirdParty.isPassenger(), "Rejected third party must remain dismounted");
        helper.assertFalse(broom.isRemoved(), "Rejected interaction must leave the called broom deployed");

        broom.setPos(owner.getX() + 8.01D, owner.getY(), owner.getZ());
        broom.tick();
        helper.assertTrue(broom.isRemoved(), "An unoccupied called broom should recall beyond eight blocks");
        helper.assertFalse(BroomDeploymentState.isDeployed(stack),
                "Distance recall should restore the equipped broom rendering state");
        helper.assertTrue(countBroomItems(thirdParty) == 0,
                "Distance recall and rejected recovery must not create a broom item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void calledBroomUnequipAndLogoutPreserveOnlyMountedRootVehicle(GameTestHelper helper) {
        var player = calledBroomPlayer(helper, new BlockPos(0, 2, 0), "called_broom_logout_test");
        var stack = equipBroom(player, ItemRegistry.FLOATMOUNT_BROOM.get());
        helper.assertTrue(CallBroomDeploymentManager.execute(player), "Call Broom should deploy before unequip");
        var first = (AbstractBroomEntity) player.getVehicle();
        ((AbstractBroomItem) stack.getItem()).onUnequip(
                new SlotContext(CuriosSlotConstants.BACK, player, 0, false, true),
                ItemStack.EMPTY,
                stack
        );
        helper.assertTrue(first.isRemoved(), "Unequipping should recall the called broom even while mounted");
        helper.assertFalse(BroomDeploymentState.isDeployed(stack), "Unequipping should clear deployment state");

        helper.assertTrue(CallBroomDeploymentManager.execute(player), "Call Broom should redeploy after cleanup");
        var mounted = (AbstractBroomEntity) player.getVehicle();
        CallBroomDeploymentEvents.onLogout(new PlayerEvent.PlayerLoggedOutEvent(player));
        helper.assertFalse(mounted.isRemoved(), "Mounted logout should leave the broom for vanilla RootVehicle saving");
        helper.assertTrue(BroomDeploymentState.matches(stack, mounted.getUUID()),
                "Mounted logout should preserve the RootVehicle deployment UUID");

        player.stopRiding();
        CallBroomDeploymentEvents.onLogout(new PlayerEvent.PlayerLoggedOutEvent(player));
        helper.assertTrue(mounted.isRemoved(), "Unmounted logout should recall the called broom");
        helper.assertFalse(BroomDeploymentState.isDeployed(stack),
                "Unmounted logout should clear deployment state before player saving");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void calledBroomDamageAndExternalNameNeverCreateReplacementItem(GameTestHelper helper) {
        var player = calledBroomPlayer(helper, new BlockPos(0, 2, 0), "called_broom_damage_test");
        var originalName = Component.literal("Original Broom");
        var stack = equipBroom(player, ItemRegistry.FLOATMOUNT_BROOM.get());
        stack.setHoverName(originalName);
        helper.assertTrue(CallBroomDeploymentManager.execute(player), "Named called broom should deploy");
        var broom = (AbstractBroomEntity) player.getVehicle();
        broom.setCustomName(Component.literal("External Entity Name"));
        broom.hurt(helper.getLevel().damageSources().generic(), 1000.0F);
        player.stopRiding();

        helper.assertTrue(broom.isRemoved(), "A damaged called broom should recall after dismount");
        helper.assertTrue(originalName.equals(stack.getHoverName()),
                "Entity-side renaming must not overwrite the original equipped item name");
        helper.assertTrue(countBroomItems(player) == 0,
                "Damaged called broom recall must not create a replacement item");
        helper.succeed();
    }

    private static void assertSingleCallBroomSelection(GameTestHelper helper, Player player, String broomName) {
        var selections = new SpellSelectionManager(player)
                .getSpellsForSlot(BroomCurioSupport.SPELL_SELECTION_SLOT);
        helper.assertTrue(selections.size() == 1,
                broomName + " should add exactly one Call Broom selection");
        helper.assertTrue(selections.get(0).spellData.getSpell() == SpellRegistry.CALL_BROOM.get(),
                broomName + " should add the Call Broom spell");
    }

    private static ItemStack equipBroom(ServerPlayer player, Item item) {
        var stack = backCurioBroomStack(item);
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for called broom test"));
        curiosInventory.setEquippedCurio(CuriosSlotConstants.BACK, 0, stack);
        return stack;
    }

    private static void assertCallBroomDeployAndRecall(
            GameTestHelper helper,
            Item item,
            int xOffset,
            String broomName
    ) {
        var player = calledBroomPlayer(
                helper,
                new BlockPos(xOffset, 2, 0),
                "called_" + broomName.toLowerCase() + "_test"
        );
        var originalName = Component.literal(broomName + " Original");
        var stack = equipBroom(player, item);
        stack.setHoverName(originalName);
        installBroomCalibration(stack, 2);

        helper.assertTrue(CallBroomDeploymentManager.execute(player), broomName + " should deploy");
        helper.assertTrue(player.getVehicle() instanceof AbstractBroomEntity,
                broomName + " should mount its dedicated broom immediately");
        var broom = (AbstractBroomEntity) player.getVehicle();
        helper.assertTrue(broom.isOwnedBy(player), broomName + " should retain its owner UUID");
        helper.assertTrue(broom.matchesBroomItem(stack), broomName + " should retain its entity type");
        helper.assertTrue(BroomDeploymentState.matches(stack, broom.getUUID()),
                broomName + " stack should store the deployed entity UUID");
        helper.assertTrue(originalName.equals(broom.getCustomName()),
                broomName + " entity should initially copy the equipped item name");
        assertCalibrationContents(helper, broom.getBroomItemStack(), 2,
                broomName + " called broom entity");
        helper.assertFalse(BroomDeploymentState.isDeployed(broom.getBroomItemStack()),
                broomName + " entity copy must not retain the Curio deployment UUID");

        player.stopRiding();
        broom.setCustomName(Component.literal(broomName + " External"));
        helper.assertTrue(CallBroomDeploymentManager.execute(player), broomName + " should recall while unoccupied");
        helper.assertTrue(broom.isRemoved(), broomName + " recall should remove its entity");
        helper.assertFalse(BroomDeploymentState.isDeployed(stack),
                broomName + " recall should clear deployment state");
        helper.assertTrue(originalName.equals(stack.getHoverName()),
                broomName + " recall must preserve the original item name");
        assertCalibrationContents(helper, stack, 2, broomName + " recalled Curio stack");
        helper.assertTrue(countBroomItems(player) == 0,
                broomName + " recall must not grant a replacement item");
    }

    private static int countBroomItems(Player player) {
        var count = 0;
        for (var slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (BroomCurioSupport.isBroom(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @GameTest(template = TEMPLATE)
    public static void hoverridePlacementAndRecoveryKeepDedicatedItemAndCustomName(GameTestHelper helper) {
        var player = player(helper, "hoverride_broom_placement");
        var expectedName = Component.literal("Sidewinder").withStyle(ChatFormatting.AQUA);
        var stack = new ItemStack(ItemRegistry.HOVERRIDE_BROOM.get());
        stack.setHoverName(expectedName);
        installBroomCalibration(stack, 2);
        var broom = placeHoverrideBroomFromItem(helper, player, stack);

        helper.assertTrue(stack.isEmpty(), "Hoverride Broom placement should consume its item");
        helper.assertTrue(expectedName.equals(broom.getCustomName()),
                "Placed Hoverride Broom should copy the item name");

        player.setShiftKeyDown(true);
        broom.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(broom.isRemoved(), "Hoverride Broom should be recovered while unoccupied");
        var recovered = findBroomInInventory(helper, player, ItemRegistry.HOVERRIDE_BROOM.get());
        helper.assertTrue(recovered.is(ItemRegistry.HOVERRIDE_BROOM.get()),
                "Recovered Hoverride Broom must not turn into a Floatmount Broom");
        helper.assertTrue(expectedName.equals(recovered.getHoverName()),
                "Recovered Hoverride Broom should preserve its custom name");
        assertCalibrationContents(helper, recovered, 2, "Recovered Hoverride Broom");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideMountingDoesNotRequireMinimumMana(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50,
                1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var pos = helper.absolutePos(TEST_POS);
            var broom = new HoverrideBroomEntity(EntityRegistry.HOVERRIDE_BROOM.get(), helper.getLevel());
            broom.setPos(pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D);
            helper.getLevel().addFreshEntity(broom);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(0.0F);

            broom.interact(player, InteractionHand.MAIN_HAND);

            helper.assertTrue(player.getVehicle() == broom,
                    "Hoverride Broom should allow mounting with zero mana");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Mounting the Hoverride Broom must not consume mana");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideMovementMathMatchesPrototypeContract(GameTestHelper helper) {
        var movement = new Vec3(HoverrideBroomMovement.MAX_HORIZONTAL_SPEED, 0.0D, 0.0D);
        for (var tick = 0; tick < 41; tick++) {
            movement = HoverrideBroomMovement.normalHorizontal(movement, new Vec3(0.0D, 0.0D, 1.0D),
                    0.0F, true);
        }
        helper.assertTrue(movement.lengthSqr() == 0.0D,
                "Hoverride Broom should reach its practical stop speed in about two seconds");

        movement = new Vec3(HoverrideBroomMovement.MAX_HORIZONTAL_SPEED, 0.0D, 0.0D);
        for (var tick = 0; tick < 10; tick++) {
            movement = HoverrideBroomMovement.normalHorizontal(movement, Vec3.ZERO, -1.0F, true);
        }
        helper.assertTrue(movement.lengthSqr() < 1.0e-8D,
                "Hoverride Broom braking must stop without reversing");

        var direction = new Vec3(1.0D, 0.0D, 0.0D);
        var target = new Vec3(0.0D, 0.0D, 1.0D);
        for (var tick = 0; tick < 6; tick++) {
            direction = HoverrideBroomMovement.rotateToward(direction, target, 0.4D);
        }
        helper.assertTrue(direction.dot(target) > 0.995D,
                "Hoverride Broom velocity should substantially follow its yaw within six ticks");

        var released = HoverrideBroomMovement.releaseHorizontal(
                Vec3.ZERO,
                target,
                HoverrideBroomMovement.MAX_HORIZONTAL_SPEED * 0.5D
        );
        helper.assertTrue(Math.abs(released.length() - 0.275D) < 1.0e-6D,
                "Hoverride Broom release should restore the configured minimum speed");

        var overdriveMaximumSpeed = HoverrideBroomMovement.maximumHorizontalSpeed(true);
        helper.assertTrue(Math.abs(overdriveMaximumSpeed - 0.825D) < 1.0e-9D,
                "Overdrive should increase Hoverride maximum horizontal speed by fifty percent");
        var overdriveAcceleration = HoverrideBroomMovement.normalHorizontal(
                Vec3.ZERO, target, 1.0F, true, true
        );
        helper.assertTrue(Math.abs(overdriveAcceleration.length() - 0.06D) < 1.0e-9D,
                "Overdrive should increase Hoverride horizontal acceleration by fifty percent");
        var overdriveBraking = HoverrideBroomMovement.normalHorizontal(
                new Vec3(overdriveMaximumSpeed, 0.0D, 0.0D), Vec3.ZERO, -1.0F, true, true
        );
        helper.assertTrue(Math.abs(overdriveBraking.length() - 0.7425D) < 1.0e-9D,
                "Overdrive should increase Hoverride braking by fifty percent");
        var overdriveReleased = HoverrideBroomMovement.releaseHorizontal(
                Vec3.ZERO, target, overdriveMaximumSpeed * 0.8D, overdriveMaximumSpeed
        );
        helper.assertTrue(Math.abs(overdriveReleased.length() - 0.66D) < 1.0e-9D,
                "Overdrive release should restore eighty percent of its maximum speed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideTwilightGaleCalibrationAndRushMathMatchContract(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.HOVERRIDE_BROOM.get());
        var twilightGale = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.TWILIGHT_GALE.get());
        helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        stack, 0, twilightGale),
                "Hoverride Broom should accept Twilight Gale calibration");
        helper.assertTrue(HoverrideBroomItem.isRushStyleEnabled(stack),
                "Twilight Gale calibration should enable the rush style");
        helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        stack, 1, twilightGale),
                "Hoverride Broom should reject duplicate Twilight Gale calibration");
        helper.assertTrue(Math.abs(HoverrideBroomRushAttack.baseDamage(0.5D, 4.0D, 8.0D) - 4.0F) < 1.0e-6F,
                "Rush minimum speed should use minimum damage");
        helper.assertTrue(Math.abs(HoverrideBroomRushAttack.baseDamage(0.7D, 4.0D, 8.0D) - 8.0F) < 1.0e-6F,
                "Rush damage should cap at maximum speed");
        helper.assertTrue(Math.abs(HoverrideBroomRushAttack.knockbackStrength(0.5D) - 0.5D) < 1.0e-9D
                        && Math.abs(HoverrideBroomRushAttack.knockbackStrength(0.7D) - 1.0D) < 1.0e-9D,
                "Rush knockback should scale from 0.5 to 1.0");

        var broomBox = new AABB(0.1D, 0.0D, -0.4D, 0.9D, 0.5D, 0.4D);
        var movement = new Vec3(0.5D, 0.0D, 0.0D);
        var grazingTarget = new AABB(0.0D, 0.0D, 0.8D, 0.6D, 1.8D, 1.4D);
        var outsideTarget = new AABB(0.0D, 0.0D, 1.0D, 0.6D, 1.8D, 1.6D);
        helper.assertTrue(HoverrideBroomRushAttack.intersectsPath(broomBox, movement, grazingTarget),
                "Rush contact padding should accept a target 1.1 blocks from the travel centerline");
        helper.assertFalse(HoverrideBroomRushAttack.intersectsPath(broomBox, movement, outsideTarget),
                "Rush contact padding should reject a target 1.3 blocks from the travel centerline");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideRushDamageRespectsIframeAndUsesExplicitKnockback(GameTestHelper helper) {
        var config = rushTestConfig(0.0D);
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 1.5D);
            installRushStyle(broom);
            var player = serverRider(helper, "hoverride_rush_iframe");
            var magicData = magicData(helper, player);
            magicData.setMana(10.0F);
            helper.assertTrue(player.startRiding(broom, true), "Rush test rider should mount");
            broom.tick();

            var target = spawnRushTarget(helper, broom.position().add(0.4D, 0.0D, 0.0D));
            var initialHealth = target.getHealth();
            broom.setPos(broom.getX() + 0.5D, broom.getY(), broom.getZ());
            broom.tick();

            helper.assertTrue(target.getHealth() < initialHealth,
                    "A validated 0.5 block rush should damage a contact target");
            helper.assertTrue(target.getDeltaMovement().x > 0.0D,
                    "Successful rush damage should knock the target along travel direction");
            helper.assertTrue(broom.isRushAttackActive() && !broom.isPushable(),
                    "An active rush should disable ordinary entity pushing");
            helper.assertTrue(Math.abs(magicData.getMana() - 9.5F) < 1.0e-4F,
                    "An active rush should consume its configured mana cost");

            var healthAfterFirstHit = target.getHealth();
            target.setDeltaMovement(Vec3.ZERO);
            broom.setPos(broom.getX() + 0.5D, broom.getY(), broom.getZ());
            broom.tick();
            helper.assertTrue(Math.abs(target.getHealth() - healthAfterFirstHit) < 1.0e-4F,
                    "Rush damage should respect the target's normal invulnerability frames");
            helper.assertTrue(target.getDeltaMovement().lengthSqr() < 1.0e-8D,
                    "A rejected iframe hit must not apply explicit rush knockback");

            var blocker = spawnRushTarget(helper, broom.position().add(0.8D, 0.0D, 0.0D));
            helper.assertFalse(broom.canCollideWith(blocker),
                    "An active rush should ignore entity collision shapes");
            var beforeCollisionMove = broom.getX();
            broom.move(MoverType.SELF, new Vec3(0.3D, 0.0D, 0.0D));
            helper.assertTrue(broom.getX() - beforeCollisionMove > 0.299D,
                    "An active rush should not lose travel distance when crossing a mob");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideRushRequiresFullCombinedManaAndRejectsAnomalousTravel(GameTestHelper helper) {
        var config = rushTestConfig(1.0D);
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 1.5D);
            installRushStyle(broom);
            var player = serverRider(helper, "hoverride_rush_mana");
            var magicData = magicData(helper, player);
            magicData.setMana(10.0F);
            helper.assertTrue(player.startRiding(broom, true), "Rush mana test rider should mount");
            broom.tick();

            var target = spawnRushTarget(helper, broom.position().add(0.4D, 0.0D, 0.0D));
            var initialHealth = target.getHealth();
            magicData.setMana(1.4F);
            broom.acceptServerInput(player, 0.0F, 1.0F, false, false,
                    BroomInputTransition.NONE, 0L);
            broom.setPos(broom.getX() + 0.5D, broom.getY(), broom.getZ());
            broom.tick();
            helper.assertTrue(Math.abs(target.getHealth() - initialHealth) < 1.0e-4F,
                    "Rush attack should require the full movement and rush mana cost");
            helper.assertTrue(broom.isManaDepleted() && !broom.isRushAttackActive(),
                    "Insufficient combined mana should deplete propulsion without activating rush");

            magicData.setMana(50.0F);
            broom.tick();
            // 回復確認の静止tickを含む2tick観測なので、平均0.5 block/tickとなる距離を使う。
            var exactCostTarget = spawnRushTarget(helper, broom.position().add(0.8D, 0.0D, 0.0D));
            magicData.setMana(1.5F);
            broom.setPos(broom.getX() + 1.0D, broom.getY(), broom.getZ());
            broom.tick();
            helper.assertTrue(exactCostTarget.getHealth() < exactCostTarget.getMaxHealth(),
                    "An exact combined mana payment should still apply rush damage");
            helper.assertTrue(broom.isManaDepleted() && broom.isRushAttackActive(),
                    "An exact combined mana payment should attack before entering depleted mode");

            magicData.setMana(50.0F);
            broom.tick();
            var manaBeforeAnomaly = magicData.getMana();
            broom.setPos(broom.getX() + 5.0D, broom.getY(), broom.getZ());
            broom.tick();
            helper.assertFalse(broom.isRushAttackActive(),
                    "An anomalously long server-observed displacement must not activate rush");
            helper.assertTrue(Math.abs(magicData.getMana() - (manaBeforeAnomaly - 1.0F)) < 1.0e-4F,
                    "Rejected rush travel should only retain the ordinary movement cost");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideRushCapsTargetsPerTick(GameTestHelper helper) {
        var config = rushTestConfig(0.0D);
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 1.5D);
            installRushStyle(broom);
            var player = serverRider(helper, "hoverride_rush_target_cap");
            magicData(helper, player).setMana(10.0F);
            helper.assertTrue(player.startRiding(broom, true), "Rush target cap rider should mount");
            broom.tick();

            var targets = new ArrayList<Zombie>();
            for (var index = 0; index < 5; ++index) {
                targets.add(spawnRushTarget(helper, broom.position().add(0.4D, 0.0D, index * 0.05D)));
            }
            broom.setPos(broom.getX() + 0.5D, broom.getY(), broom.getZ());
            broom.tick();
            var damagedTargets = targets.stream().filter(target -> target.getHealth() < target.getMaxHealth()).count();
            helper.assertTrue(damagedTargets == HoverrideBroomRushAttack.MAX_TARGETS_PER_TICK,
                    "Rush should damage at most four targets per tick");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideReleaseRequiresOneSuccessfulServerTick(GameTestHelper helper) {
        var config = new HoverrideBroomServerConfig.Values(1.0D, 0.5D, 50.0D, 0.5D, 20.0D);
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 1.5D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(100.0F);
            helper.assertTrue(player.startRiding(broom, true), "Hoverride release test rider should mount");

            broom.acceptServerInput(player, 0.0F, 0.0F, true, false,
                    BroomInputTransition.NONE, 0L);
            broom.acceptServerInput(player, 0.0F, 0.0F, false, false,
                    BroomInputTransition.RELEASE, 1L);
            helper.assertTrue(Math.abs(magicData.getMana() - 100.0F) < 1.0e-4F,
                    "A same-tick Hoverride release must be rejected without consuming mana");

            broom.acceptServerInput(player, 0.0F, 0.0F, true, false,
                    BroomInputTransition.NONE, 0L);
            broom.tick();
            helper.assertTrue(broom.isServerInertiaGlideActive()
                            && broom.getServerSuccessfulGlideTicks() == 1,
                    "Hoverride inertia glide should become releasable after one paid server tick");
            helper.assertTrue(Math.abs(magicData.getMana() - 99.5F) < 1.0e-4F,
                    "One Hoverride inertia glide tick should consume its configured mana");

            broom.acceptServerInput(player, 0.0F, 0.0F, false, false,
                    BroomInputTransition.RELEASE, 2L);
            helper.assertTrue(Math.abs(magicData.getMana() - 49.5F) < 1.0e-4F,
                    "An accepted Hoverride release should consume its configured mana once");
            helper.assertFalse(broom.isServerInertiaGlideActive(),
                    "An accepted Hoverride release should end server glide state");

            broom.acceptServerInput(player, 0.0F, 0.0F, false, false,
                    BroomInputTransition.RELEASE, 2L);
            helper.assertTrue(Math.abs(magicData.getMana() - 49.5F) < 1.0e-4F,
                    "A repeated Hoverride release must not consume mana again");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideLocalReleaseWaitsForItsMatchingResult(GameTestHelper helper) {
        var broom = spawnHoverrideBroom(helper, 1.5D);
        var player = serverRider(helper);
        var magicData = magicData(helper, player);
        magicData.setMana(1000.0F);
        helper.assertTrue(player.startRiding(broom, true), "Hoverride delayed release test rider should mount");

        broom.setYRot(0.0F);
        broom.setDeltaMovement(0.1D, 0.0D, 0.0D);
        broom.setLocalInput(0.0F, 0.0F, true, false);
        broom.handleLocalInputTransition(BroomInputTransition.RELEASE, 42L);

        // 旧実装の20 client tick相当を超えても、serverで確定した解除結果との相関を維持する。
        for (var i = 0; i < 25; ++i) {
            broom.tick();
        }
        broom.acceptLocalReleaseResult(41L, true, 0.4D);
        helper.assertTrue(broom.getDeltaMovement().equals(Vec3.ZERO),
                "A mismatched Hoverride release result must not apply an impulse");

        broom.acceptLocalReleaseResult(42L, true, 0.4D);
        helper.assertTrue(Math.abs(broom.getDeltaMovement().horizontalDistance() - 0.4D) < 1.0e-6D,
                "A delayed matching Hoverride release result should still apply its impulse");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideCancelDoesNotTriggerReleaseCost(GameTestHelper helper) {
        var config = new HoverrideBroomServerConfig.Values(1.0D, 0.5D, 50.0D, 0.5D, 20.0D);
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 1.5D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(100.0F);
            helper.assertTrue(player.startRiding(broom, true), "Hoverride cancel test rider should mount");
            broom.acceptServerInput(player, 0.0F, 0.0F, true, false,
                    BroomInputTransition.NONE, 0L);
            broom.tick();

            broom.acceptServerInput(player, 0.0F, 0.0F, false, false,
                    BroomInputTransition.CANCEL, 1L);

            helper.assertTrue(Math.abs(magicData.getMana() - 99.5F) < 1.0e-4F,
                    "Hoverride cancellation should only retain the already paid glide tick");
            helper.assertFalse(broom.isServerInertiaGlideActive(),
                    "Hoverride cancellation should clear glide state without a release");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideActionSequenceResetsForTheNextRider(GameTestHelper helper) {
        var config = new HoverrideBroomServerConfig.Values(1.0D, 0.0D, 10.0D, 0.5D, 20.0D);
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 1.5D);
            var firstRider = serverRider(helper);
            var firstMana = magicData(helper, firstRider);
            firstMana.setMana(100.0F);
            helper.assertTrue(firstRider.startRiding(broom, true), "First Hoverride rider should mount");
            broom.acceptServerInput(firstRider, 0.0F, 0.0F, false, false,
                    BroomInputTransition.CANCEL, 100L);
            firstRider.stopRiding();

            var nextRider = serverRider(helper);
            var nextMana = magicData(helper, nextRider);
            nextMana.setMana(100.0F);
            helper.assertTrue(nextRider.startRiding(broom, true), "Next Hoverride rider should mount");
            broom.acceptServerInput(nextRider, 0.0F, 0.0F, true, false,
                    BroomInputTransition.NONE, 0L);
            broom.tick();
            broom.acceptServerInput(nextRider, 0.0F, 0.0F, false, false,
                    BroomInputTransition.RELEASE, 1L);

            helper.assertTrue(Math.abs(nextMana.getMana() - 90.0F) < 1.0e-4F,
                    "A new Hoverride rider should be able to release from a fresh action sequence");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideReleaseDepletionAndRecoveryUseConfiguredCost(GameTestHelper helper) {
        var config = new HoverrideBroomServerConfig.Values(1.0D, 0.0D, 50.0D, 0.5D, 20.0D);
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 1.5D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(50.0F);
            helper.assertTrue(player.startRiding(broom, true), "Hoverride depletion test rider should mount");
            broom.acceptServerInput(player, 0.0F, 0.0F, true, false,
                    BroomInputTransition.NONE, 0L);
            broom.tick();
            broom.acceptServerInput(player, 0.0F, 0.0F, false, false,
                    BroomInputTransition.RELEASE, 1L);

            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "An exact-cost Hoverride release should consume all remaining mana");
            helper.assertTrue(broom.isManaDepleted(),
                    "An exact-cost Hoverride release should enter depleted mode");
            helper.assertTrue(broom.getCoreWarningState() == BroomCoreWarningState.CRITICAL,
                    "Hoverride depleted mode should activate the core warning flash");

            magicData.setMana(49.999F);
            broom.tick();
            helper.assertTrue(broom.isManaDepleted(),
                    "Hoverride depleted mode should remain below the configured recovery threshold");
            magicData.setMana(50.0F);
            broom.tick();
            helper.assertFalse(broom.isManaDepleted(),
                    "Hoverride depleted mode should recover at the configured release cost");
            helper.assertTrue(broom.getCoreWarningState() == BroomCoreWarningState.NONE,
                    "Recovering Hoverride propulsion should clear the core warning flash");

            magicData.setMana(0.0F);
            broom.acceptServerInput(player, 0.0F, -1.0F, false, false,
                    BroomInputTransition.NONE, 0L);
            broom.tick();
            helper.assertTrue(broom.isManaDepleted(), "Zero mana should enter Hoverride depleted mode");
            helper.assertTrue(broom.getPresentationState() == HoverrideBroomPresentation.BRAKING,
                    "Hoverride braking feedback should remain active while mana is depleted");
            player.stopRiding();
            helper.assertFalse(broom.isManaDepleted(), "Dismounting should clear Hoverride depleted mode");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideReleaseManaCostMustBePositive(GameTestHelper helper) {
        var rejected = false;
        try {
            new HoverrideBroomServerConfig.Values(0.0D, 0.0D, 0.0D, 0.5D, 20.0D);
        } catch (IllegalArgumentException ignored) {
            rejected = true;
        }
        helper.assertTrue(rejected,
                "Hoverride release mana cost must reject zero even when per-tick costs are disabled");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideInputAndReleaseResultPacketsRoundTrip(GameTestHelper helper) {
        var inputBuffer = new FriendlyByteBuf(Unpooled.buffer());
        var input = new ClientBroomInputPacket(
                0.25F, 1.0F, false, false, BroomInputTransition.RELEASE, 42L
        );
        ClientBroomInputPacket.encode(input, inputBuffer);
        helper.assertTrue(input.equals(ClientBroomInputPacket.decode(inputBuffer)),
                "Hoverride input transition packet should round-trip");

        var resultBuffer = new FriendlyByteBuf(Unpooled.buffer());
        var result = new HoverrideBroomReleaseResultPacket(12, 42L, true, 0.275D);
        HoverrideBroomReleaseResultPacket.encode(result, resultBuffer);
        helper.assertTrue(result.equals(HoverrideBroomReleaseResultPacket.decode(resultBuffer)),
                "Hoverride release result packet should round-trip");

        var effectBuffer = new FriendlyByteBuf(Unpooled.buffer());
        var effect = new HoverrideBroomImpulseEffectPacket(
                new Vec3(1.25D, 2.5D, -3.75D),
                new Vec3(4.0D, 0.0D, 3.0D)
        );
        HoverrideBroomImpulseEffectPacket.encode(effect, effectBuffer);
        helper.assertTrue(effect.equals(HoverrideBroomImpulseEffectPacket.decode(effectBuffer)),
                "Hoverride impulse effect packet should round-trip");
        var sanitizedEffect = new HoverrideBroomImpulseEffectPacket(
                new Vec3(Double.NaN, Double.POSITIVE_INFINITY, 2.0D),
                Vec3.ZERO
        );
        helper.assertTrue(sanitizedEffect.center().equals(new Vec3(0.0D, 0.0D, 2.0D))
                        && sanitizedEffect.direction().equals(new Vec3(0.0D, 0.0D, 1.0D)),
                "Hoverride impulse effect packet should sanitize invalid presentation data");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideLowManaWarningRearmsAtReleaseCost(GameTestHelper helper) {
        var config = new HoverrideBroomServerConfig.Values(1.0D, 0.0D, 50.0D, 0.5D, 20.0D);
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 1.5D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(70.0F);
            helper.assertTrue(player.startRiding(broom, true), "Hoverride low mana warning rider should mount");

            broom.acceptServerInput(player, 0.0F, 0.0F, true, false,
                    BroomInputTransition.NONE, 0L);
            broom.tick();
            broom.acceptServerInput(player, 0.0F, 0.0F, false, false,
                    BroomInputTransition.RELEASE, 1L);
            helper.assertTrue(broom.isLowManaWarningShown(),
                    "Hoverride should warn when release consumption reaches the configured low mana threshold");

            magicData.setMana(49.0F);
            broom.tick();
            helper.assertTrue(broom.isLowManaWarningShown(),
                    "Hoverride low mana warning should remain latched below the recovery threshold");
            magicData.setMana(50.0F);
            broom.tick();
            helper.assertFalse(broom.isLowManaWarningShown(),
                    "Hoverride low mana warning should rearm at the configured release cost");
            magicData.setMana(20.0F);
            broom.tick();
            helper.assertTrue(broom.isLowManaWarningShown(),
                    "Hoverride should warn again after the low mana warning has rearmed");

            player.stopRiding();
            helper.assertFalse(broom.isLowManaWarningShown(),
                    "Dismounting should rearm the Hoverride low mana warning");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverridePresentationPrioritizesAcceptedControlStates(GameTestHelper helper) {
        helper.assertTrue(HoverrideBroomPresentation.resolve(1.0F, true, true)
                        == HoverrideBroomPresentation.GLIDING,
                "Hoverride glide presentation should suppress acceleration feedback");
        helper.assertTrue(HoverrideBroomPresentation.resolve(-1.0F, false, false)
                        == HoverrideBroomPresentation.BRAKING,
                "Hoverride braking feedback should remain available without acceleration");
        helper.assertTrue(HoverrideBroomPresentation.resolve(1.0F, false, true)
                        == HoverrideBroomPresentation.ACCELERATING,
                "Hoverride valid forward input should show acceleration feedback");
        helper.assertTrue(HoverrideBroomPresentation.resolve(1.0F, false, false)
                        == HoverrideBroomPresentation.NORMAL,
                "Hoverride invalid forward input must not show acceleration feedback");
        helper.assertTrue(HoverrideBroomPresentation.fromId(Integer.MAX_VALUE)
                        == HoverrideBroomPresentation.NORMAL,
                "Unknown Hoverride presentation ids should safely fall back to normal");
        helper.assertTrue(HoverrideBroomPresentation.speedEffectIntensity(0.15D) == 0.0F,
                "Low Hoverride travel speed should not add camera or lean effects");
        helper.assertTrue(HoverrideBroomPresentation.speedEffectIntensity(1.0D) == 1.0F,
                "Maximum Hoverride travel speed should apply full camera and lean effects");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideAirborneAccelerationLockUsesSyncedCautionState(GameTestHelper helper) {
        var config = new HoverrideBroomServerConfig.Values(1.0D, 0.0D, 50.0D, 0.5D, 20.0D);
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 20.0D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(100.0F);
            helper.assertTrue(player.startRiding(broom, true), "Hoverride airborne warning rider should mount");

            for (var tick = 0; tick < 39; tick++) {
                broom.tick();
            }
            helper.assertFalse(broom.isAirborneAccelerationLocked(),
                    "Hoverride airborne grace should remain active before two seconds elapse");
            helper.assertTrue(broom.getCoreWarningState() == BroomCoreWarningState.NONE,
                    "Hoverride core should remain normal during the airborne grace period");

            broom.tick();
            helper.assertTrue(broom.isAirborneAccelerationLocked(),
                    "Hoverride should synchronize acceleration lock after two airborne seconds");
            helper.assertTrue(broom.getCoreWarningState() == BroomCoreWarningState.CAUTION,
                    "Hoverride airborne acceleration lock should use the caution core state");

            broom.acceptServerInput(player, 0.0F, 0.0F, true, false,
                    BroomInputTransition.NONE, 0L);
            broom.tick();
            broom.acceptServerInput(player, 0.0F, 0.0F, false, false,
                    BroomInputTransition.RELEASE, 1L);
            helper.assertFalse(broom.isAirborneAccelerationLocked(),
                    "Accepted Hoverride inertia release should reset the synchronized airborne lock");
            helper.assertTrue(broom.getCoreWarningState() == BroomCoreWarningState.NONE,
                    "Accepted Hoverride inertia release should restore the normal core state");

            magicData.setMana(0.0F);
            broom.tick();
            helper.assertTrue(broom.getCoreWarningState() == BroomCoreWarningState.CRITICAL,
                    "Hoverride mana depletion should take priority over airborne caution");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideUnoccupiedBroomHoversOverLava(GameTestHelper helper) {
        var lava = helper.absolutePos(TEST_POS);
        helper.getLevel().setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());
        var broom = spawnHoverrideBroom(helper, 1.6D);
        var startY = broom.getY();
        for (var tick = 0; tick < 6; ++tick) {
            broom.tick();
        }
        helper.assertTrue(broom.getY() > startY,
                "An unoccupied Hoverride Broom should rise toward its hover height above lava");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideLavaSurfacePreventsAirborneLock(GameTestHelper helper) {
        var lava = helper.absolutePos(TEST_POS);
        helper.getLevel().setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());
        var broom = spawnHoverrideBroom(helper, 2.2D);
        var player = serverRider(helper);
        magicData(helper, player).setMana(100.0F);
        helper.assertTrue(player.startRiding(broom, true), "Hoverride lava test rider should mount");
        for (var tick = 0; tick < 40; ++tick) {
            broom.tick();
        }
        helper.assertFalse(broom.isAirborneAccelerationLocked(),
                "Lava below an Hoverride Broom should prevent the airborne acceleration lock");
        helper.assertTrue(broom.getCoreWarningState() == BroomCoreWarningState.NONE,
                "Lava-supported Hoverride flight should keep the normal core warning state");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void dismountPreservesServerObservedBroomMovement(GameTestHelper helper) {
        assertDismountPreservesMovement(helper, spawnBroom(helper, 1.5D), "Floatmount");
        assertDismountPreservesMovement(helper, spawnHoverrideBroom(helper, 1.5D), "Hoverride");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverridePassengerStandsAndSmoothlyFollowsBroomYaw(GameTestHelper helper) {
        var floatmount = spawnBroom(helper, 1.5D);
        var hoverride = spawnHoverrideBroom(helper, 1.5D);
        helper.assertTrue(floatmount.shouldRiderSit(), "Floatmount Broom should retain its seated rider pose");
        helper.assertFalse(hoverride.shouldRiderSit(), "Hoverride Broom should render its rider standing");

        var floatmountPlayer = serverRider(helper);
        helper.assertTrue(floatmountPlayer.startRiding(floatmount, true), "Floatmount pose reference rider should mount");
        floatmount.positionRider(floatmountPlayer);
        var player = serverRider(helper);
        player.setYRot(0.0F);
        helper.assertTrue(player.startRiding(hoverride, true), "Hoverride yaw-follow rider should mount");
        hoverride.setYRot(0.0F);
        hoverride.positionRider(player);
        helper.assertTrue(Math.abs(player.getY() - floatmountPlayer.getY() - 0.625D) < 1.0e-6D,
                "Hoverride standing rider should receive its dedicated vertical position correction");

        hoverride.setYRot(60.0F);
        hoverride.positionRider(player);
        helper.assertTrue(player.getYRot() > 20.0F && player.getYRot() < 30.0F,
                "Hoverride yaw follow should begin smoothly instead of snapping");
        for (var tick = 1; tick < 6; tick++) {
            hoverride.positionRider(player);
        }
        helper.assertTrue(Math.abs(Mth.wrapDegrees(player.getYRot() - 60.0F)) < 4.0F,
                "Hoverride rider view should substantially follow a turn within six ticks");
        helper.assertTrue(Math.abs(Mth.wrapDegrees(player.getYHeadRot() - player.getYRot())) < 1.0e-4F,
                "Hoverride rider head should follow the adjusted camera yaw");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void defaultItemPlacementDoesNotCreateAVisibleCustomName(GameTestHelper helper) {
        var player = player(helper, "floatmount_broom_default_placement");
        var broom = placeBroomFromItem(helper, player, new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get()));
        helper.assertFalse(broom.hasCustomName(), "Default item name must not become a custom name");
        helper.assertFalse(broom.isCustomNameVisible(), "Default broom must not show a nameplate");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cancelledEntityJoinDoesNotConsumeItem(GameTestHelper helper) {
        var player = player(helper, "floatmount_broom_cancelled_placement");
        var target = helper.absolutePos(TEST_POS);
        player.setPos(target.getX() + 0.5D, target.getY() + 2.5D, target.getZ() + 0.5D);
        player.setXRot(90.0F);
        player.setYRot(0.0F);
        var stack = new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Consumer<EntityJoinLevelEvent> cancelBroomJoin = event -> {
            if (event.getLevel() == helper.getLevel() && event.getEntity() instanceof FloatmountBroomEntity) {
                event.setCanceled(true);
            }
        };

        InteractionResult result;
        MinecraftForge.EVENT_BUS.addListener(cancelBroomJoin);
        try {
            result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(cancelBroomJoin);
        }

        helper.assertTrue(result == InteractionResult.FAIL,
                "Cancelled broom placement should report failure");
        helper.assertTrue(stack.getCount() == 1,
                "Cancelled broom placement must not consume the item");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                FloatmountBroomEntity.class,
                new AABB(target).inflate(2.0D, 4.0D, 2.0D)
        ).isEmpty(), "Cancelled broom placement must not add an entity");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void namedItemPlacementCopiesVisibleNameAndEntitySaveRetainsIt(GameTestHelper helper) {
        var player = player(helper, "floatmount_broom_named_placement");
        var expectedName = Component.literal("Zephyr").withStyle(ChatFormatting.AQUA);
        var stack = new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get());
        stack.setHoverName(expectedName);
        var broom = placeBroomFromItem(helper, player, stack);
        helper.assertTrue(expectedName.equals(broom.getCustomName()), "Placed broom should copy the item name");
        helper.assertTrue(broom.isCustomNameVisible(), "Named broom should show its nameplate");

        var saved = new CompoundTag();
        broom.saveWithoutId(saved);
        var loaded = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
        loaded.load(saved);
        helper.assertTrue(expectedName.equals(loaded.getCustomName()), "Saved broom should retain its name");
        helper.assertTrue(loaded.isCustomNameVisible(), "Saved broom should retain nameplate visibility");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void onlyOnePlayerMayRideAndOccupiedBroomCannotBeRecovered(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var first = serverRider(helper);
        var second = serverRider(helper);

        helper.assertTrue(first.startRiding(broom, true), "First player should be able to ride the broom");
        broom.positionRider(first);
        helper.assertTrue(Math.abs(first.getY()
                        - (broom.getY() + FloatmountBroomEntity.RIDER_ATTACHMENT_Y)) < 1.0e-6D,
                "Rider vehicle attachment should match the configured broom model height");
        helper.assertTrue(broom.getControllingPassenger() == first,
                "The sole player passenger should control the broom");
        helper.assertFalse(second.startRiding(broom), "Second player should not be able to ride the occupied broom");

        second.setShiftKeyDown(true);
        broom.interact(second, InteractionHand.MAIN_HAND);
        helper.assertFalse(broom.isRemoved(), "A third party must not recover an occupied broom");
        helper.assertTrue(first.getVehicle() == broom,
                "Rejected third-party recovery must leave the rider mounted");
        helper.assertFalse(second.getInventory().contains(new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get())),
                "Rejected third-party recovery must not grant a broom item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void thirdPartyCanRecoverUnoccupiedBroom(GameTestHelper helper) {
        var placer = player(helper, "floatmount_broom_placer");
        var broom = placeBroomFromItem(helper, placer, new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get()));
        var collector = player(helper, "floatmount_broom_third_party_collector");
        collector.setShiftKeyDown(true);

        broom.interact(collector, InteractionHand.MAIN_HAND);

        helper.assertTrue(broom.isRemoved(), "A third party should be able to recover an unoccupied broom");
        helper.assertTrue(collector.getInventory().contains(new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get())),
                "The third party who recovered the broom should receive its item");
        helper.assertFalse(placer.getInventory().contains(new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get())),
                "Recovering the broom must not return its item to the original placer");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sneakingRecoveryReturnsFreshItemEvenInCreative(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var player = player(helper, "floatmount_broom_recovery");
        player.getAbilities().instabuild = true;
        player.setShiftKeyDown(true);

        broom.interact(player, InteractionHand.MAIN_HAND);

        helper.assertTrue(broom.isRemoved(), "Recovered broom should be removed");
        helper.assertTrue(player.getInventory().contains(new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get())),
                "Creative recovery should grant a fresh broom item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sneakingRecoveryPreservesCalibrationAndRedeploysWithResetState(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var expectedName = Component.literal("Restored Broom").withStyle(ChatFormatting.GOLD);
            var sourceStack = backCurioCalibratedBroomStack(ItemRegistry.FLOATMOUNT_BROOM.get(), 2);
            sourceStack.setHoverName(expectedName);
            var placer = player(helper, "floatmount_broom_calibrated_placer");
            var broom = placeBroomFromItem(helper, placer, sourceStack);
            broom.setCustomNameVisible(true);
            broom.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 20.0F);
            var damagedState = new CompoundTag();
            broom.saveWithoutId(damagedState);
            damagedState.putBoolean("EmergencyLanding", true);
            broom.load(damagedState);
            helper.assertTrue(broom.isDamaged(), "Recovery setup should use a damaged broom");
            helper.assertTrue(broom.isManaEmergencyLanding(), "Recovery setup should use an emergency broom");

            var player = player(helper, "floatmount_broom_named_recovery");
            player.setShiftKeyDown(true);
            broom.interact(player, InteractionHand.MAIN_HAND);
            var recovered = findBroomInInventory(helper, player);
            helper.assertTrue(expectedName.equals(recovered.getHoverName()),
                    "Sneaking recovery should copy the entity custom name");
            assertCalibrationContents(helper, recovered, 2, "Recovered Floatmount Broom");
            helper.assertTrue(AbstractBroomItem.isBackCurioEnabled(recovered),
                    "Normal placement and recovery should preserve Curios calibration");

            var redeployStack = recovered.copy();
            player.getInventory().clearContent();
            player.setShiftKeyDown(false);
            var redeployed = placeBroomFromItem(helper, player, redeployStack);
            helper.assertTrue(expectedName.equals(redeployed.getCustomName()), "Redeployed broom should retain its name");
            helper.assertTrue(redeployed.getDamage() == 0, "Redeployed broom should reset damage");
            helper.assertFalse(redeployed.isDamaged(), "Redeployed broom should reset damaged state");
            helper.assertFalse(redeployed.isManaEmergencyLanding(), "Redeployed broom should reset emergency landing");
            assertCalibrationContents(helper, redeployed.getBroomItemStack(), 2, "Redeployed Floatmount Broom");
            helper.assertTrue(AbstractBroomItem.isBackCurioEnabled(redeployed.getBroomItemStack()),
                    "Redeployed broom should preserve Curios calibration");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void nameTagDoesNotRenameBroom(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var player = player(helper, "floatmount_broom_name_tag");
        player.getAbilities().instabuild = true;
        var nameTag = new ItemStack(Items.NAME_TAG);
        nameTag.setHoverName(Component.literal("Rejected Name"));
        player.setItemInHand(InteractionHand.MAIN_HAND, nameTag);

        broom.interact(player, InteractionHand.MAIN_HAND);

        helper.assertFalse(broom.hasCustomName(), "Name tags must not rename Floatmount Broom entities");
        helper.assertTrue(nameTag.getCount() == 1, "Unsupported name tag use must not consume the item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void damageScalesAndRecoversAtTenTickIntervals(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = player(helper, "floatmount_broom_damage_recovery");
            var source = helper.getLevel().damageSources().playerAttack(player);

            broom.hurt(source, 1.0F);
            helper.assertTrue(broom.getDamage() == 50,
                    "One point of incoming damage should add fifty broom damage");
            // Entity.tickの手動呼び出しではLevel側のtickCount更新が入らないため、境界値を明示する。
            broom.tickCount = 9;
            broom.tick();
            helper.assertTrue(broom.getDamage() == 50,
                    "Damage must not recover before the ten tick interval");
            broom.tickCount = 10;
            broom.tick();
            helper.assertTrue(broom.getDamage() == 0,
                    "The configured amount should recover at the ten tick interval");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void tooltipUsesSyncedManaThresholdAndExpectedControls(GameTestHelper helper) {
        FloatmountBroomConfigState.setNormalFlightManaThreshold(321);
        try {
            var stack = new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get());
            var broomItem = (AbstractBroomItem) stack.getItem();
            var lines = new ArrayList<Component>();
            stack.getItem().appendHoverText(stack, helper.getLevel(), lines, TooltipFlag.Default.NORMAL);

            helper.assertTrue(lines.size() == 5, "Floatmount Broom should have five tooltip lines");
            assertTooltipLine(helper, lines, 0, "item.apprenticecodex.floatmount_broom.desc_1", 1);
            assertTooltipLine(helper, lines, 1, "item.apprenticecodex.floatmount_broom.desc_2", 2);
            assertTooltipLine(helper, lines, 2, "item.apprenticecodex.broom.desc_calibration", 0);
            assertTooltipLine(helper, lines, 3, "item.apprenticecodex.floatmount_broom.desc_3", 1);
            assertTooltipLine(helper, lines, 4, "item.apprenticecodex.floatmount_broom.desc_4", 0);

            var firstArgs = ((TranslatableContents) lines.get(0).getContents()).getArgs();
            var secondArgs = ((TranslatableContents) lines.get(1).getContents()).getArgs();
            assertComponentKey(helper, firstArgs[0], "key.use", "Placement control should use the use key");
            assertComponentKey(helper, secondArgs[0], "key.sneak", "Retrieval control should start with sneak");
            assertComponentKey(helper, secondArgs[1], "key.use", "Retrieval control should end with use");

            var manaArg = ((TranslatableContents) lines.get(3).getContents()).getArgs()[0];
            helper.assertTrue(manaArg instanceof Component, "Mana threshold should be supplied as a styled component");
            if (manaArg instanceof Component manaComponent) {
                helper.assertTrue("321".equals(manaComponent.getString()),
                        "Tooltip should use the synchronized mana threshold");
                helper.assertTrue(manaComponent.getStyle().getColor() != null
                                && manaComponent.getStyle().getColor().getValue() == ChatFormatting.AQUA.getColor(),
                        "Mana threshold should be aqua");
            }

            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 2, new ItemStack(Items.SADDLE)),
                    "Tooltip test broom should accept its Curios calibration");
            lines.clear();
            stack.getItem().appendHoverText(
                    stack, helper.getLevel(), lines, TooltipFlag.Default.NORMAL
            );
            helper.assertTrue(lines.size() == 5,
                    "Curios calibration should not mix slot details into the normal item description");

            var defaultSlotLine = Component.translatable("curios.tooltip.slot");
            var slotsTooltip = broomItem.getSlotsTooltip(List.of(defaultSlotLine), stack);
            helper.assertTrue(slotsTooltip.size() == 7,
                    "Saddle-calibrated broom should add the standard Curios section and separator");
            helper.assertTrue(slotsTooltip.get(1).getString().isEmpty(),
                    "Curios section should start after a blank line");
            helper.assertTrue(slotsTooltip.get(2).getContents() instanceof TranslatableContents contents
                            && contents.getKey().equals("curios.modifiers.back")
                            && contents.getArgs().length == 0,
                    "Curios section should use the back slot modifier heading");
            helper.assertTrue(slotsTooltip.get(2).getStyle().getColor() != null
                            && slotsTooltip.get(2).getStyle().getColor().getValue() == ChatFormatting.GOLD.getColor(),
                    "Curios section header should use the standard gold style");
            for (var line = 0; line < 3; ++line) {
                var detail = slotsTooltip.get(line + 3);
                helper.assertTrue(detail.getSiblings().size() == 1
                                && detail.getSiblings().get(0).getContents() instanceof TranslatableContents contents
                                && contents.getKey().equals(
                                "item.apprenticecodex.brooms.curios.desc_" + (line + 1)
                        ),
                        "Curios section should contain its expected detail line");
                helper.assertTrue(detail.getStyle().getColor() != null
                                && detail.getStyle().getColor().getValue() == ChatFormatting.YELLOW.getColor(),
                        "Curios detail lines should use the standard yellow style");
            }
            helper.assertTrue(slotsTooltip.get(6).getString().isEmpty(),
                    "Curios details should be separated from the normal control description by a blank line");
            helper.succeed();
        } finally {
            FloatmountBroomConfigState.reset();
        }
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideTooltipIncludesAssistWingsAndRecoveryDescriptions(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.HOVERRIDE_BROOM.get());
        var lines = new ArrayList<Component>();
        stack.getItem().appendHoverText(
                stack, helper.getLevel(), lines, TooltipFlag.Default.NORMAL
        );

        helper.assertTrue(lines.size() == 5, "Hoverride Broom should have five tooltip lines");
        assertTooltipLine(helper, lines, 0, "item.apprenticecodex.hoverride_broom.desc_1", 1);
        assertTooltipLine(helper, lines, 1, "item.apprenticecodex.hoverride_broom.desc_2", 2);
        assertTooltipLine(helper, lines, 2, "item.apprenticecodex.broom.desc_calibration", 0);
        assertTooltipLine(helper, lines, 3, "item.apprenticecodex.hoverride_broom.desc_3", 0);
        assertTooltipLine(helper, lines, 4, "item.apprenticecodex.hoverride_broom.desc_4", 0);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void configSyncPacketPreservesManaThreshold(GameTestHelper helper) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        SyncFloatmountBroomConfigPacket.encode(new SyncFloatmountBroomConfigPacket(4321), buffer);
        var decoded = SyncFloatmountBroomConfigPacket.decode(buffer);
        helper.assertTrue(decoded.normalFlightManaThreshold() == 4321,
                "Floatmount Broom config sync should preserve the mana threshold");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void damageIFrameIncludesAcceptedTickAndReopensAtTickTen(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var source = helper.getLevel().damageSources().generic();

        helper.assertTrue(broom.hurt(source, 1.0F), "The first damage hit should be accepted");
        helper.assertFalse(broom.hurt(source, 1.0F),
                "The accepted tick itself should be inside the broom damage i-frame");

        helper.runAfterDelay(9, () -> helper.assertFalse(broom.hurt(source, 1.0F),
                "A normal hit at T+9 should still be rejected"));
        helper.runAfterDelay(10, () -> {
            var damageBeforeHit = broom.getDamage();
            helper.assertTrue(broom.hurt(source, 1.0F), "A normal hit at T+10 should be accepted");
            helper.assertTrue(broom.getDamage() == damageBeforeHit + 50,
                    "The T+10 hit should apply the configured damage conversion");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void damageIFrameZeroSettingDisablesRejection(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 0, 0, Set.of(), 100, 50,
                1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var source = helper.getLevel().damageSources().generic();

            helper.assertTrue(broom.hurt(source, 1.0F), "The first hit should be accepted");
            helper.assertTrue(broom.hurt(source, 1.0F), "A zero-tick i-frame should accept a same-tick hit");
            helper.assertTrue(broom.getDamage() == 100,
                    "Disabling the i-frame should apply both same-tick hits");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void ignoredAndZeroDamageDoNotInteractWithDamageIFrame(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(
                1000, 0, 10, Set.of(DamageTypes.GENERIC.location()), 100, 50,
                1.0D, 1.0D, 1.5D
        );
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = player(helper, "floatmount_broom_iframe_owner");
            var ignoredSource = helper.getLevel().damageSources().generic();
            var normalSource = helper.getLevel().damageSources().playerAttack(player);

            helper.assertTrue(broom.hurt(normalSource, 0.01F),
                    "A hit converted to zero broom damage should retain the existing hurt result");
            helper.assertTrue(broom.hurt(ignoredSource, 1.0F),
                    "A configured ignored damage type should pass without starting an i-frame");
            helper.assertTrue(broom.hurt(normalSource, 1.0F),
                    "Normal damage should still be accepted after zero and ignored damage");
            helper.assertTrue(broom.hurt(ignoredSource, 1.0F),
                    "Ignored damage should pass through an active i-frame");
            helper.assertFalse(broom.hurt(normalSource, 1.0F),
                    "Ignored damage must not clear the active normal-damage i-frame");
            helper.assertTrue(broom.getDamage() == 150,
                    "Only the three positive accepted hits should change broom damage");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void damageIFrameIsNotPersisted(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 0, 10, Set.of(), 100, 50,
                1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var source = helper.getLevel().damageSources().generic();
            var broom = spawnBroom(helper, 1.5D);
            broom.hurt(source, 1.0F);
            var saved = new CompoundTag();
            broom.saveWithoutId(saved);

            var loaded = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
            loaded.load(saved);
            helper.assertTrue(loaded.hurt(source, 1.0F),
                    "Loading should not restore the volatile damage i-frame");
            helper.assertTrue(loaded.getDamage() == 100,
                    "The loaded broom should retain damage while accepting a new hit immediately");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void lavaDamagesBroomWithoutPersistentFire(GameTestHelper helper) {
        var lava = helper.absolutePos(TEST_POS);
        helper.getLevel().setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());
        var broom = spawnBroom(helper, 0.2D);

        helper.runAfterDelay(5, () -> {
            helper.assertTrue(broom.getDamage() > 0, "Lava contact should still damage the broom");
            helper.assertFalse(broom.isOnFire(), "The broom should clear persistent fire after contact damage");
            helper.assertTrue(broom.getRemainingFireTicks() <= 0,
                    "Persistent fire ticks should not remain on the broom");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void firewardCalibratedBroomsIgnoreFireDamageAndLavaContact(GameTestHelper helper) {
        var level = helper.getLevel();
        var firstLava = helper.absolutePos(TEST_POS);
        var secondLava = helper.absolutePos(TEST_POS.offset(2, 0, 0));
        var floatmount = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        var hoverride = new HoverrideBroomEntity(EntityRegistry.HOVERRIDE_BROOM.get(), level);
        var brooms = List.of((AbstractBroomEntity) floatmount, hoverride);
        var broomItems = List.of(ItemRegistry.FLOATMOUNT_BROOM.get(), ItemRegistry.HOVERRIDE_BROOM.get());
        var lavaPositions = List.of(firstLava, secondLava);

        for (var index = 0; index < brooms.size(); ++index) {
            var broom = brooms.get(index);
            var broomStack = new ItemStack(broomItems.get(index));
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            broomStack,
                            0,
                            new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIREWARD_RING.get())
                    ),
                    "Broom fire immunity test should accept its Fireward Ring calibration");
            broom.setBroomItemStack(broomStack);

            var lava = lavaPositions.get(index);
            level.setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());
            broom.setPos(lava.getX() + 0.5D, lava.getY() + 0.2D, lava.getZ() + 0.5D);
            level.addFreshEntity(broom);
            broom.setRemainingFireTicks(100);
            helper.assertFalse(broom.hurt(level.damageSources().lava(), 1.0F),
                    "Fireward-calibrated broom should reject direct lava damage");
            helper.assertTrue(broom.getRemainingFireTicks() <= 0,
                    "Rejecting fire damage should extinguish a Fireward-calibrated broom immediately");
        }

        helper.runAfterDelay(5, () -> {
            for (var broom : brooms) {
                helper.assertTrue(broom.getDamage() == 0,
                        "Fireward-calibrated broom should take no damage from lava contact");
                helper.assertFalse(broom.isOnFire(),
                        "Fireward-calibrated broom should not remain on fire in lava");
                helper.assertTrue(broom.hurt(level.damageSources().generic(), 1.0F),
                        "Fireward calibration should not reject non-fire damage");
                helper.assertTrue(broom.getDamage() > 0,
                        "Non-fire damage should still damage a Fireward-calibrated broom");
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void combatToolsIgnoresVehiclesAndTargetsOnlyPvpHarmableRiders(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        helper.assertTrue(server != null, "Floatmount Broom PvP test requires a server");
        var attacker = new ServerPlayer(server, level,
                new GameProfile(UUID.randomUUID(), "broom_pvp_attacker"));
        // 降車時のteleport処理も通るため、騎乗者には接続を備えたGameTestのmock playerを使う。
        var rider = serverRider(helper);
        var broom = spawnBroom(helper, 1.5D);
        var boat = new Boat(level, broom.getX() + 2.0D, broom.getY(), broom.getZ());
        level.addFreshEntity(boat);
        var scoreboard = level.getScoreboard();
        var team = scoreboard.addPlayerTeam("broom_pvp_policy");
        var previousPvp = server.isPvpAllowed();

        try {
            server.setPvpAllowed(true);
            helper.assertTrue(rider.startRiding(broom, true), "PvP target rider should mount the broom");
            helper.assertFalse(CombatTools.isValidCombatTarget(broom, attacker),
                    "A ridden broom should remain excluded as a non-living vehicle");
            helper.assertFalse(CombatTools.isValidCombatTarget(boat, attacker),
                    "A vanilla boat should remain excluded as a non-living vehicle");
            helper.assertTrue(CombatTools.isValidCombatTarget(rider, attacker),
                    "An enemy rider should be a combat target while PvP is enabled");
            helper.assertFalse(CombatTools.isValidCombatTarget(broom, rider),
                    "A rider must not target their own root vehicle");
            helper.assertFalse(CombatTools.isValidCombatTarget(rider, rider),
                    "A rider must not target themselves");

            scoreboard.addPlayerToTeam(attacker.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(rider.getScoreboardName(), team);
            team.setAllowFriendlyFire(false);
            helper.assertFalse(CombatTools.isValidCombatTarget(rider, attacker),
                    "Friendly-fire protection should protect the rider");
            helper.assertFalse(CombatTools.isValidCombatTarget(broom, attacker),
                    "Friendly-fire settings must not make the non-living broom targetable");
            team.setAllowFriendlyFire(true);
            helper.assertTrue(CombatTools.isValidCombatTarget(rider, attacker),
                    "Friendly-fire-enabled teammates should expose the rider");
            helper.assertFalse(CombatTools.isValidCombatTarget(broom, attacker),
                    "A ridden broom should remain excluded when friendly fire is enabled");

            scoreboard.removePlayerFromTeam(attacker.getScoreboardName(), team);
            scoreboard.removePlayerFromTeam(rider.getScoreboardName(), team);
            server.setPvpAllowed(false);
            helper.assertFalse(CombatTools.isValidCombatTarget(rider, attacker),
                    "Server PvP disablement should protect the rider");
            helper.assertFalse(CombatTools.isValidCombatTarget(broom, attacker),
                    "Server PvP settings must not make the non-living broom targetable");

            server.setPvpAllowed(true);
            rider.stopRiding();
            helper.assertFalse(CombatTools.isValidCombatTarget(broom, attacker),
                    "An unoccupied broom should remain excluded as a non-living vehicle");
        } finally {
            rider.stopRiding();
            server.setPvpAllowed(previousPvp);
            scoreboard.removePlayerTeam(team);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void maximumDamageIsPersistentAndPreventsMounting(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = serverRider(helper);
            helper.assertTrue(player.startRiding(broom, true), "Damage test player should mount directly");
            broom.hurt(helper.getLevel().damageSources().playerAttack(player), 20.0F);

            helper.assertTrue(broom.isDamaged(), "Maximum damage should enter the damaged state");
            helper.assertFalse(broom.isRemoved(), "Normal damage must not itemize the broom");
            helper.assertTrue(player.getVehicle() == broom, "Damage must not eject the current rider immediately");
            helper.assertTrue(broom.isForcedLanding(), "Damaged broom must enter forced landing");
            helper.assertTrue(broom.isDangerousDismount(),
                    "Damaged broom must always require dismount confirmation");
            helper.assertTrue(broom.getDamage() == broom.getMaxDamage(),
                    "Damaged broom should remain at maximum damage");
            for (var tick = 0; tick < 20; tick++) {
                broom.tick();
            }
            helper.assertTrue(broom.getDamage() == broom.getMaxDamage(),
                    "Damaged state must disable natural recovery");
            player.stopRiding();
            helper.assertFalse(player.startRiding(broom), "Damaged broom must reject riders");

            var saved = new CompoundTag();
            broom.saveWithoutId(saved);
            var loaded = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
            loaded.load(saved);
            helper.assertTrue(loaded.isDamaged(), "Damaged state must persist in entity NBT");
            helper.assertTrue(loaded.getDamage() == loaded.getMaxDamage(),
                    "Loaded damaged broom should retain maximum damage");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void damagedBroomItemizesOnlyBelowWorldBottomRegardlessOfDamageSource(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var expectedName = Component.literal("Void Survivor").withStyle(ChatFormatting.LIGHT_PURPLE);
            var broom = spawnBroom(helper, 1.5D);
            broom.setCustomName(expectedName);
            broom.setCustomNameVisible(true);
            broom.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 20.0F);
            helper.assertTrue(broom.isDamaged(), "Void damage may damage the broom like any other source");
            helper.assertFalse(broom.isRemoved(),
                    "Void damage source alone must never be used as the itemization condition");

            broom.setPos(broom.getX(), helper.getLevel().getMinBuildHeight() - 1.0D, broom.getZ());
            var dropPos = broom.position();
            broom.tick();
            helper.assertTrue(broom.isRemoved(), "Damaged broom below world bottom should itemize");
            helper.assertTrue(countDroppedBrooms(helper, dropPos) == 1,
                    "World-bottom itemization should drop exactly one fresh broom");
            var dropped = findDroppedBroom(helper, dropPos).getItem();
            helper.assertTrue(expectedName.equals(dropped.getHoverName()),
                    "World-bottom itemization should preserve the broom custom name");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void mountingRequiresConfiguredManaButCreativeIsExempt(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(99.0F);

            broom.interact(player, InteractionHand.MAIN_HAND);
            helper.assertFalse(player.isPassenger(), "Player below the normal flight threshold must not mount");

            magicData.setMana(100.0F);
            broom.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getVehicle() == broom, "Player at the normal flight threshold should mount");
            helper.assertTrue(Math.abs(magicData.getMana() - 100.0F) < 1.0e-4F,
                    "Mounting must not consume mana");

            player.stopRiding();
            var creativeBroom = spawnBroom(helper, 1.5D);
            player.getAbilities().instabuild = true;
            magicData.setMana(0.0F);
            creativeBroom.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getVehicle() == creativeBroom, "Creative players must mount with zero mana");
            creativeBroom.acceptServerInput(player, 0.0F, 1.0F, true, false);
            creativeBroom.tick();
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Creative broom movement must not consume mana");
            helper.assertFalse(creativeBroom.isManaEmergencyLanding(),
                    "Creative broom movement must not enter emergency landing");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void movementInputsUseConfiguredManaCosts(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 2.0D, 3.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            assertMovementManaCost(helper, "floatmount_broom_horizontal_cost", 1.0F,
                    0.0F, 1.0F, false, false);
            assertMovementManaCost(helper, "floatmount_broom_ascending_cost", 2.0F,
                    0.0F, 0.0F, true, false);
            assertMovementManaCost(helper, "floatmount_broom_combined_cost", 3.5F,
                    0.0F, -1.0F, true, false);
            assertMovementManaCost(helper, "floatmount_broom_turning_free", 0.0F,
                    1.0F, 0.0F, false, false);
            assertMovementManaCost(helper, "floatmount_broom_descending_free", 0.0F,
                    0.0F, 0.0F, false, true);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void inactiveInputStopsMovementManaCost(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 2.0D, 3.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(100.0F);
            helper.assertTrue(player.startRiding(broom, true), "Inactive input test player should mount directly");

            broom.acceptServerInput(player, 0.0F, 1.0F, true, false);
            broom.tick();
            helper.assertTrue(Math.abs(magicData.getMana() - 96.5F) < 1.0e-4F,
                    "Active combined movement should consume configured mana");

            // clientの画面操作はGameTestで再現せず、inactive受理後のserver課金停止だけを固定する。
            broom.acceptServerInput(player, 0.0F, 0.0F, false, false);
            broom.tick();
            helper.assertTrue(Math.abs(magicData.getMana() - 96.5F) < 1.0e-4F,
                    "Inactive movement input must stop additional mana consumption");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void serverTeleportWithoutReportedPoweredInputRemainsFree(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 2.0D, 3.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(0.5F);
            helper.assertTrue(player.startRiding(broom, true), "Server teleport test player should mount directly");
            broom.acceptServerInput(player, 0.0F, 0.0F, false, false);

            // client権威のvehicle位置差だけでは、惰性・server再配置・他MODの外力と偽装移動を区別できない。
            // 正規playerへの誤課金を避けるため、申告された動力入力がない再配置は課金対象にしない。
            // その結果、改造clientによる入力の過少申告も防げないことを設計上受容する。
            var beforeTeleport = broom.position();
            broom.teleportRelative(1.0D, 1.0D, 0.0D);
            helper.assertTrue(broom.position().distanceToSqr(beforeTeleport) > 1.0D,
                    "Server teleport should move the mounted broom horizontally and upward");

            broom.tick();

            helper.assertTrue(Math.abs(magicData.getMana() - 0.5F) < 1.0e-4F,
                    "Server teleport without reported powered input must not consume mana");
            helper.assertFalse(broom.isManaEmergencyLanding(),
                    "Server teleport without reported powered input must not trigger emergency landing");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void nonControllingPlayerCannotSubmitPaidMovementInput(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 2.0D, 3.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var rider = serverRider(helper);
            var nonController = player(helper, "floatmount_broom_non_controller");
            var riderMagicData = magicData(helper, rider);
            riderMagicData.setMana(100.0F);
            helper.assertTrue(rider.startRiding(broom, true), "Controller test rider should mount directly");

            broom.acceptServerInput(nonController, 0.0F, 1.0F, true, false);
            broom.tick();

            helper.assertTrue(Math.abs(riderMagicData.getMana() - 100.0F) < 1.0e-4F,
                    "A non-controlling player must not consume the rider's mana");
            helper.assertFalse(broom.isManaEmergencyLanding(),
                    "A non-controlling player must not trigger the rider's emergency landing");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void depletionEmergencyRecoveryAndSavedStateFollowServerRules(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(0.5F);
            helper.assertTrue(player.startRiding(broom, true), "Emergency test player should mount directly");
            broom.acceptServerInput(player, 0.0F, 1.0F, false, false);

            broom.tick();

            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "A movement cost above the remaining mana should consume the remainder");
            helper.assertTrue(broom.isManaEmergencyLanding(), "Movement depletion should enter emergency landing");

            magicData.setMana(10.0F);
            broom.acceptServerInput(player, 0.0F, 1.0F, true, false);
            broom.tick();
            helper.assertTrue(Math.abs(magicData.getMana() - 10.0F) < 1.0e-4F,
                    "Emergency movement must not consume mana");

            var saved = new CompoundTag();
            broom.saveWithoutId(saved);
            var loaded = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
            loaded.load(saved);
            helper.assertTrue(loaded.isManaEmergencyLanding(), "Emergency landing must persist in entity NBT");

            magicData.setMana(100.0F);
            broom.tick();
            helper.assertFalse(broom.isManaEmergencyLanding(),
                    "Reaching the normal flight threshold should recover normal flight");
            helper.assertTrue(Math.abs(magicData.getMana() - 100.0F) < 1.0e-4F,
                    "Recovery tick must not immediately consume mana");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void externalDepletionWaitsForPaidInputAndEmergencyForcesDismountWarning(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(0.0F);
            helper.assertTrue(player.startRiding(broom, true), "External depletion test player should mount directly");

            broom.tick();
            helper.assertFalse(broom.isManaEmergencyLanding(),
                    "External zero mana must not trigger emergency landing while idle");
            var warningState = new CompoundTag();
            broom.saveWithoutId(warningState);
            helper.assertTrue(warningState.getBoolean("LowManaWarningShown"),
                    "Low mana warning latch should be stored on the broom entity");

            broom.acceptServerInput(player, 0.0F, 1.0F, false, false);
            broom.tick();
            helper.assertTrue(broom.isManaEmergencyLanding(),
                    "Paid movement attempted at zero mana should trigger emergency landing");
            helper.assertTrue(broom.isDangerousDismount(),
                    "Emergency landing must force dismount confirmation even near the ground");

            BroomDismountEvents.handleSneakInput(player, broom, true);
            var first = new EntityMountEvent(player, broom, helper.getLevel(), false);
            BroomDismountEvents.onDismount(first);
            helper.assertTrue(first.isCanceled(), "First emergency dismount should be canceled");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unoccupiedBroomUsesCoastDamping(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        broom.setDeltaMovement(0.2D, 0.0D, 0.0D);

        broom.tick();

        helper.assertTrue(Math.abs(broom.getDeltaMovement().x - 0.17D) < 1.0e-6D,
                "Unoccupied broom should retain eighty-five percent of horizontal speed per tick");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void floatmountOverdriveUsesConfiguredManaCosts(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(
                1000, 50, 10, Set.of(), 100, 50,
                1.0D, 2.0D, 3.5D,
                1.75D, 1.0D, 2.5D
        );
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            assertMovementManaCost(helper, "floatmount_overdrive_horizontal_cost", 1.75F,
                    0.0F, 1.0F, false, false, true);
            assertMovementManaCost(helper, "floatmount_overdrive_ascending_cost", 1.0F,
                    0.0F, 0.0F, true, false, true);
            assertMovementManaCost(helper, "floatmount_overdrive_combined_cost", 2.5F,
                    0.0F, -1.0F, true, false, true);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideOverdriveUsesConfiguredManaCostsAndRecoveryThreshold(GameTestHelper helper) {
        var config = new HoverrideBroomServerConfig.Values(
                1.0D, 0.5D, 50.0D,
                2.25D, 1.25D, 77.0D,
                0.5D, 20.0D
        );
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 1.5D);
            var stack = new ItemStack(ItemRegistry.HOVERRIDE_BROOM.get());
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 0, new ItemStack(ItemRegistry.OVERDRIVE_BROOM_ENGINE.get())),
                    "Hoverride Overdrive mana test should accept its engine");
            broom.setBroomItemStack(stack);
            var player = serverRider(helper, "hoverride_overdrive_mana_rider");
            var magicData = magicData(helper, player);
            magicData.setMana(78.25F);
            helper.assertTrue(player.startRiding(broom, true),
                    "Hoverride Overdrive mana test rider should mount");

            broom.acceptServerInput(player, 0.0F, 0.0F, true, false,
                    BroomInputTransition.NONE, 0L);
            broom.tick();
            helper.assertTrue(Math.abs(magicData.getMana() - 77.0F) < 1.0e-4F,
                    "Overdrive inertia glide should use its configured mana cost");

            broom.acceptServerInput(player, 0.0F, 0.0F, false, false,
                    BroomInputTransition.RELEASE, 1L);
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Overdrive inertia release should use its configured mana cost");
            helper.assertTrue(broom.isManaDepleted(),
                    "Exact Overdrive release cost should enter depleted mode");

            magicData.setMana(76.0F);
            broom.tick();
            helper.assertTrue(broom.isManaDepleted(),
                    "Overdrive depleted mode should remain below its configured release cost");
            magicData.setMana(77.0F);
            broom.tick();
            helper.assertFalse(broom.isManaDepleted(),
                    "Overdrive depleted mode should recover at its configured release cost");

            broom.acceptServerInput(player, 0.0F, 1.0F, false, false,
                    BroomInputTransition.NONE, 0L);
            broom.tick();
            helper.assertTrue(Math.abs(magicData.getMana() - 74.75F) < 1.0e-4F,
                    "Overdrive forward acceleration should use its configured mana cost");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void broomMovementChecksBubbleColumnsOncePerTick(GameTestHelper helper) {
        var level = helper.getLevel();
        var bubble = helper.absolutePos(new BlockPos(1, 1, 1));
        var water = helper.absolutePos(new BlockPos(3, 1, 1));
        installBubbleColumn(level, bubble, 3);
        installWaterColumn(level, water, 3);

        var floatmountBubble = spawnBroomAt(helper, bubble);
        var floatmountWater = spawnBroomAt(helper, water);
        floatmountBubble.tick();
        floatmountWater.tick();
        helper.assertTrue(Math.abs(
                        floatmountBubble.getDeltaMovement().y - floatmountWater.getDeltaMovement().y - 0.06D
                ) < 1.0e-6D,
                "Floatmount Broom should receive one inside-bubble-column impulse per movement tick");
        floatmountBubble.discard();
        floatmountWater.discard();

        var hoverrideBubble = spawnHoverrideBroomAt(helper, bubble);
        var hoverrideWater = spawnHoverrideBroomAt(helper, water);
        hoverrideBubble.tick();
        hoverrideWater.tick();
        helper.assertTrue(Math.abs(
                        hoverrideBubble.getDeltaMovement().y - hoverrideWater.getDeltaMovement().y - 0.06D
                ) < 1.0e-6D,
                "Hoverride Broom should receive one inside-bubble-column impulse per movement tick");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void aquaticCalibrationIgnoresOnlyBroomBubbleMovement(GameTestHelper helper) {
        var level = helper.getLevel();
        var bubble = helper.absolutePos(new BlockPos(1, 1, 1));
        var water = helper.absolutePos(new BlockPos(3, 1, 1));
        // 高さのある同一条件の列で、気泡callbackの移動成分だけが除外されることを検証する。
        installBubbleColumn(level, bubble, 8);
        installWaterColumn(level, water, 8);

        var calibratedStack = new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get());
        helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        calibratedStack, 0, new ItemStack(Items.HEART_OF_THE_SEA)),
                "Aquatic calibration setup should accept Heart of the Sea");
        var bubbleBroom = spawnBroomAt(helper, bubble);
        var waterBroom = spawnBroomAt(helper, water);
        bubbleBroom.setBroomItemStack(calibratedStack);
        waterBroom.setBroomItemStack(calibratedStack);
        bubbleBroom.tick();
        waterBroom.tick();
        helper.assertTrue(Math.abs(
                        bubbleBroom.getDeltaMovement().y - waterBroom.getDeltaMovement().y
                ) < 1.0e-6D,
                "Aquatic calibration should ignore bubble-column movement on the broom");
        bubbleBroom.fallDistance = 5.0F;
        var beforeCallback = bubbleBroom.getDeltaMovement();
        bubbleBroom.onInsideBubbleColumn(false);
        helper.assertTrue(bubbleBroom.getDeltaMovement().equals(beforeCallback),
                "Aquatic calibration should restore only the broom velocity changed by the callback");
        helper.assertTrue(bubbleBroom.fallDistance == 0.0F,
                "Aquatic calibration should preserve non-movement effects from the bubble callback");
        helper.assertTrue(level.getBlockState(bubble).is(Blocks.BUBBLE_COLUMN),
                "Aquatic calibration must leave the bubble column available to the rider");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void waterPenaltyClampsUnoccupiedAndDismountMovement(GameTestHelper helper) {
        var level = helper.getLevel();
        var water = helper.absolutePos(new BlockPos(1, 1, 1));
        installWaterColumn(level, water, 3);

        var unoccupied = spawnBroomAt(helper, water);
        unoccupied.setDeltaMovement(0.4D, 0.0D, 0.0D);
        var unoccupiedStartX = unoccupied.getX();
        unoccupied.tick();
        var unoccupiedDisplacement = unoccupied.getX() - unoccupiedStartX;
        helper.assertTrue(unoccupiedDisplacement > 0.0D && unoccupiedDisplacement <= 0.21D + 1.0e-6D,
                "Unoccupied Floatmount Broom should move at most 0.21 blocks per water tick, actual: "
                        + unoccupiedDisplacement);
        unoccupied.discard();

        var falling = spawnBroomAt(helper, water.above());
        falling.setDeltaMovement(0.0D, -0.2D, 0.0D);
        var fallingStartY = falling.getY();
        falling.tick();
        var fallingDisplacement = falling.getY() - fallingStartY;
        helper.assertTrue(Math.abs(fallingDisplacement + 0.06D) < 1.0e-6D,
                "Unoccupied Floatmount Broom should clamp water fall speed to 0.06, actual: "
                        + fallingDisplacement);
        falling.discard();

        var ridden = spawnBroomAt(helper, water);
        var player = serverRider(helper, "water_dismount_clamp_test");
        player.getAbilities().instabuild = true;
        helper.assertTrue(player.startRiding(ridden, true), "Water dismount test rider should mount");
        ridden.tick();
        ridden.setPos(ridden.getX() + 0.4D, ridden.getY() + 0.2D, ridden.getZ());
        ridden.tick();
        player.stopRiding();
        helper.assertTrue(Math.abs(ridden.getDeltaMovement().horizontalDistance() - 0.21D) < 1.0e-6D,
                "Dismounted Floatmount Broom should clamp inherited horizontal water speed to 0.21");
        helper.assertTrue(Math.abs(ridden.getDeltaMovement().y - 0.09D) < 1.0e-6D,
                "Dismounted Floatmount Broom should clamp inherited vertical water speed to 0.09");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void damagedUnoccupiedBroomRisesOutOfLava(GameTestHelper helper) {
        var level = helper.getLevel();
        var lava = helper.absolutePos(TEST_POS);
        level.setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());
        level.setBlockAndUpdate(lava.above(), Blocks.LAVA.defaultBlockState());
        level.setBlockAndUpdate(lava.above(2), Blocks.LAVA.defaultBlockState());

        var broom = spawnBroom(helper, 0.2D);
        broom.hurt(level.damageSources().fellOutOfWorld(), 20.0F);
        broom.setDeltaMovement(0.0D, -0.15D, 0.0D);
        var startY = broom.getY();

        helper.runAfterDelay(12, () -> {
            helper.assertTrue(broom.isDamaged(), "Lava recovery must not clear the damaged state");
            helper.assertFalse(broom.isVehicle(), "Lava recovery test broom must remain unoccupied");
            helper.assertTrue(broom.getDeltaMovement().y > 0.0D,
                    "Damaged unoccupied broom should reverse its downward motion while in lava");
            helper.assertTrue(broom.getY() > startY,
                    "Damaged unoccupied broom should rise toward the lava surface");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void surfaceScannerTreatsSolidWaterLavaAndForcedBlocksAsHoverSurfaces(GameTestHelper helper) {
        var level = helper.getLevel();
        var solid = helper.absolutePos(new BlockPos(1, 1, 1));
        var water = helper.absolutePos(new BlockPos(2, 1, 1));
        var powderSnow = helper.absolutePos(new BlockPos(3, 1, 1));
        var lava = helper.absolutePos(new BlockPos(4, 1, 1));
        level.setBlockAndUpdate(solid, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(powderSnow.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(powderSnow, Blocks.POWDER_SNOW.defaultBlockState());
        level.setBlockAndUpdate(lava.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());

        helper.assertTrue(BroomSurfaceScanner.findSurfaceBelow(
                level, solid.getX() + 0.5D, solid.getY() + 2.0D, solid.getZ() + 0.5D, 3, true).isPresent(),
                "Solid collision shape should be a hover surface");
        helper.assertTrue(BroomSurfaceScanner.findSurfaceBelow(
                level, water.getX() + 0.5D, water.getY() + 2.0D, water.getZ() + 0.5D, 3, true).isPresent(),
                "Water should be a hover surface");
        var powderSnowSurface = BroomSurfaceScanner.findSurfaceBelow(
                level,
                powderSnow.getX() + 0.5D,
                powderSnow.getY() + 2.0D,
                powderSnow.getZ() + 0.5D,
                3,
                true
        ).orElseThrow();
        helper.assertTrue(powderSnowSurface.blockPos().equals(powderSnow),
                "Forced hover surface should preserve the tagged block position");
        helper.assertTrue(Math.abs(powderSnowSurface.y() - (powderSnow.getY() + 1.0D)) < 1.0E-5D,
                "Powder snow should use its own upper face instead of the solid block below");
        helper.assertTrue(BroomSurfaceScanner.findSurfaceBelow(
                level, lava.getX() + 0.5D, lava.getY() + 2.0D, lava.getZ() + 0.5D, 3, true).isPresent(),
                "Lava should be a hover surface");
        helper.assertTrue(BroomSurfaceScanner.findSurfaceBelow(
                        level, lava.getX() + 0.5D, lava.getY() + 2.0D, lava.getZ() + 0.5D, 3, false).isEmpty(),
                "Lava must make the safe dismount scan fail even with solid ground beneath it");

        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        broom.setPos(lava.getX() + 0.5D, lava.getY() + 1.5D, lava.getZ() + 0.5D);
        helper.assertTrue(broom.isDangerousDismount(),
                "Lava below the broom must always be classified as a dangerous dismount surface");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverridePowderSnowSurfacePreventsAirborneLock(GameTestHelper helper) {
        var powderSnowBase = helper.absolutePos(TEST_POS);
        for (var height = 0; height < 5; ++height) {
            helper.getLevel().setBlockAndUpdate(
                    powderSnowBase.above(height),
                    Blocks.POWDER_SNOW.defaultBlockState()
            );
        }
        var powderSnowTop = powderSnowBase.above(4);
        var broom = new HoverrideBroomEntity(EntityRegistry.HOVERRIDE_BROOM.get(), helper.getLevel());
        broom.setPos(
                powderSnowTop.getX() + 0.5D,
                powderSnowTop.getY() + 2.2D,
                powderSnowTop.getZ() + 0.5D
        );
        helper.getLevel().addFreshEntity(broom);
        var player = serverRider(helper, "hoverride_powder_snow_surface");
        magicData(helper, player).setMana(100.0F);
        helper.assertTrue(player.startRiding(broom, true), "Hoverride powder snow test rider should mount");

        for (var tick = 0; tick < 40; ++tick) {
            broom.tick();
        }

        helper.assertFalse(broom.isAirborneAccelerationLocked(),
                "Tagged powder snow below an Hoverride Broom should prevent the airborne acceleration lock");
        helper.assertTrue(broom.getCoreWarningState() == BroomCoreWarningState.NONE,
                "Powder snow-supported Hoverride flight should keep the normal core warning state");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unoccupiedBroomsRiseTowardPowderSnowSurface(GameTestHelper helper) {
        var level = helper.getLevel();
        var floatmountSurface = helper.absolutePos(new BlockPos(1, 1, 2));
        var hoverrideSurface = helper.absolutePos(new BlockPos(3, 1, 2));
        level.setBlockAndUpdate(floatmountSurface, Blocks.POWDER_SNOW.defaultBlockState());
        level.setBlockAndUpdate(hoverrideSurface, Blocks.POWDER_SNOW.defaultBlockState());

        var floatmount = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        floatmount.setPos(
                floatmountSurface.getX() + 0.5D,
                floatmountSurface.getY() + 1.2D,
                floatmountSurface.getZ() + 0.5D
        );
        level.addFreshEntity(floatmount);
        var hoverride = new HoverrideBroomEntity(EntityRegistry.HOVERRIDE_BROOM.get(), level);
        hoverride.setPos(
                hoverrideSurface.getX() + 0.5D,
                hoverrideSurface.getY() + 1.2D,
                hoverrideSurface.getZ() + 0.5D
        );
        level.addFreshEntity(hoverride);
        var floatmountStartY = floatmount.getY();
        var hoverrideStartY = hoverride.getY();

        for (var tick = 0; tick < 6; ++tick) {
            floatmount.tick();
            hoverride.tick();
        }

        helper.assertTrue(floatmount.getY() > floatmountStartY,
                "Unoccupied Floatmount Broom should rise toward the powder snow hover height");
        helper.assertTrue(hoverride.getY() > hoverrideStartY,
                "Unoccupied Hoverride Broom should rise toward the powder snow hover height");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void safeDismountUsesBroomLeftSurfaceInsteadOfPassengerView(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(2, 1, 2));
        var leftGround = center.east();
        level.setBlockAndUpdate(center, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(2), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(3), Blocks.AIR.defaultBlockState());

        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        broom.setPos(center.getX() + 0.5D, center.getY() + 2.5D, center.getZ() + 0.5D);
        broom.setYRot(0.0F);
        level.addFreshEntity(broom);
        var player = serverRider(helper);
        helper.assertTrue(player.startRiding(broom, true), "Safe dismount test player should mount the broom");
        player.setYRot(180.0F);

        var target = broom.getDismountLocationForPassenger(player);
        var minimumSeparation = (broom.getBbWidth() + player.getBbWidth()) / 2.0D;
        helper.assertTrue(target.x - broom.getX() > minimumSeparation,
                "Dismount target should clear both bounding boxes on the broom's left side");
        helper.assertTrue(Math.abs(target.z - broom.getZ()) < 1.0E-5D,
                "Passenger view must not rotate the broom-relative dismount side");
        helper.assertTrue(Math.abs(target.y - (leftGround.getY() + 1.0D)) < 1.0E-5D,
                "Safe dismount should resolve directly to the preferred-side surface");
        helper.assertFalse(broom.isDangerousDismount(),
                "A valid preferred-side surface below two blocks should be safe");

        player.stopRiding();
        helper.assertTrue(Math.abs(player.getY() - (leftGround.getY() + 1.0D)) < 1.0E-5D,
                "Server dismount should place the rider directly on the resolved surface");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void powderSnowRequiresDangerousDismountConfirmation(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(2, 1, 2));
        var leftSurface = center.east();
        level.setBlockAndUpdate(center, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftSurface, Blocks.POWDER_SNOW.defaultBlockState());
        level.setBlockAndUpdate(leftSurface.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftSurface.above(2), Blocks.AIR.defaultBlockState());

        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        broom.setPos(center.getX() + 0.5D, leftSurface.getY() + 2.5D, center.getZ() + 0.5D);
        broom.setYRot(0.0F);
        level.addFreshEntity(broom);
        var player = serverRider(helper, "powder_snow_dismount_test");
        helper.assertTrue(player.startRiding(broom, true), "Powder snow dismount test player should mount");

        helper.assertTrue(broom.isDangerousDismount(),
                "Powder snow should require dangerous dismount confirmation");
        var target = broom.getDismountLocationForPassenger(player);
        helper.assertTrue(Math.abs(target.y - (leftSurface.getY() + 1.0D)) > 1.0E-5D,
                "Dangerous powder snow must not become an automatic surface dismount target");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void dangerousDismountUsesPreferredSideInsteadOfBroomCenter(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(2, 1, 2));
        var leftGround = center.east();
        level.setBlockAndUpdate(center, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(leftGround, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(2), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(3), Blocks.AIR.defaultBlockState());

        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        broom.setPos(center.getX() + 0.5D, center.getY() + 2.5D, center.getZ() + 0.5D);
        broom.setYRot(0.0F);
        level.addFreshEntity(broom);
        var player = serverRider(helper);
        helper.assertTrue(player.startRiding(broom, true), "Cliff dismount test player should mount the broom");
        helper.assertTrue(broom.isDangerousDismount(),
                "Ground below the broom must not make a preferred-side cliff safe");

        level.setBlockAndUpdate(center, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround, Blocks.STONE.defaultBlockState());
        helper.assertFalse(broom.isDangerousDismount(),
                "Preferred-side ground should allow a safe dismount even when the broom center is over a cliff");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void safeDismountRequiresSurfaceBelowTwoBlocks(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(2, 1, 2));
        var leftGround = center.east();
        level.setBlockAndUpdate(center, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(2), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(3), Blocks.AIR.defaultBlockState());

        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        broom.setPos(center.getX() + 0.5D, leftGround.getY() + 3.0D, center.getZ() + 0.5D);
        broom.setYRot(0.0F);
        level.addFreshEntity(broom);
        var player = serverRider(helper);
        helper.assertTrue(player.startRiding(broom, true), "Height boundary test player should mount the broom");

        helper.assertTrue(broom.isDangerousDismount(),
                "A surface exactly two blocks below should require dangerous dismount confirmation");
        var dangerousTarget = broom.getDismountLocationForPassenger(player);
        helper.assertTrue(Math.abs(dangerousTarget.y - broom.getY()) < 1.0E-5D,
                "Dangerous dismount must not warp the rider down to the surface");

        broom.setPos(broom.getX(), leftGround.getY() + 2.99D, broom.getZ());
        helper.assertFalse(broom.isDangerousDismount(),
                "A surface just under two blocks below should allow a direct safe dismount");
        var safeTarget = broom.getDismountLocationForPassenger(player);
        helper.assertTrue(Math.abs(safeTarget.y - (leftGround.getY() + 1.0D)) < 1.0E-5D,
                "Safe dismount below the threshold should resolve to the server surface");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void dangerousDismountRequiresReleaseAndSecondPress(GameTestHelper helper) {
        var broom = spawnBroom(helper, 5.0D);
        var player = serverRider(helper);
        helper.assertTrue(player.startRiding(broom, true), "Dismount test player should mount the broom");
        broom.setPos(broom.getX(), broom.getY() + 10.0D, broom.getZ());
        helper.assertTrue(broom.isDangerousDismount(), "High broom should be classified as dangerous");

        BroomDismountEvents.handleSneakInput(player, broom, true);
        var first = new EntityMountEvent(player, broom, helper.getLevel(), false);
        BroomDismountEvents.onDismount(first);
        helper.assertTrue(first.isCanceled(), "First dangerous dismount should be canceled");

        BroomDismountEvents.handleSneakInput(player, broom, true);
        var held = new EntityMountEvent(player, broom, helper.getLevel(), false);
        BroomDismountEvents.onDismount(held);
        helper.assertTrue(held.isCanceled(), "Holding sneak must not confirm dismount");

        BroomDismountEvents.handleSneakInput(player, broom, false);
        helper.assertTrue(player.getVehicle() == broom, "Releasing sneak should not dismount by itself");
        BroomDismountEvents.handleSneakInput(player, broom, true);
        helper.assertFalse(player.isPassenger(), "Second press within thirty ticks should dismount");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void dangerousDismountNeverTreatsHeldSneakAsSecondPress(GameTestHelper helper) {
        var broom = spawnBroom(helper, 5.0D);
        var player = serverRider(helper);
        helper.assertTrue(player.startRiding(broom, true), "Held dismount test player should mount the broom");
        broom.setPos(broom.getX(), broom.getY() + 10.0D, broom.getZ());

        BroomDismountEvents.handleSneakInput(player, broom, true);
        var first = new EntityMountEvent(player, broom, helper.getLevel(), false);
        BroomDismountEvents.onDismount(first);
        helper.assertTrue(first.isCanceled(), "First dangerous dismount should be canceled");

        helper.runAfterDelay(FloatmountBroomEntity.DISMOUNT_CONFIRM_TICKS + 2, () -> {
            BroomDismountEvents.handleSneakInput(player, broom, true);
            var held = new EntityMountEvent(player, broom, helper.getLevel(), false);
            BroomDismountEvents.onDismount(held);
            helper.assertTrue(held.isCanceled(), "Held sneak must remain canceled after the confirmation window");
            helper.assertTrue(player.getVehicle() == broom, "Held sneak must never dismount the rider");
            helper.succeed();
        });
    }

    private static FloatmountBroomEntity spawnBroom(GameTestHelper helper, double relativeY) {
        var pos = helper.absolutePos(TEST_POS);
        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
        broom.setPos(pos.getX() + 0.5D, pos.getY() + relativeY, pos.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(broom);
        return broom;
    }

    private static FloatmountBroomEntity spawnBroomAt(GameTestHelper helper, BlockPos position) {
        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
        broom.setPos(position.getX() + 0.5D, position.getY() + 0.1D, position.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(broom);
        return broom;
    }

    private static HoverrideBroomEntity spawnHoverrideBroom(GameTestHelper helper, double relativeY) {
        var pos = helper.absolutePos(TEST_POS);
        var broom = new HoverrideBroomEntity(EntityRegistry.HOVERRIDE_BROOM.get(), helper.getLevel());
        broom.setPos(pos.getX() + 0.5D, pos.getY() + relativeY, pos.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(broom);
        return broom;
    }

    private static HoverrideBroomEntity spawnHoverrideBroomAt(GameTestHelper helper, BlockPos position) {
        var broom = new HoverrideBroomEntity(EntityRegistry.HOVERRIDE_BROOM.get(), helper.getLevel());
        broom.setPos(position.getX() + 0.5D, position.getY() + 0.1D, position.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(broom);
        return broom;
    }

    private static ServerPlayer mountForVibrationTest(
            GameTestHelper helper,
            AbstractBroomEntity broom,
            String profileName
    ) {
        return mountForVibrationTest(helper, broom, serverRider(helper, profileName));
    }

    private static ServerPlayer mountForVibrationTest(
            GameTestHelper helper,
            AbstractBroomEntity broom,
            ServerPlayer player
    ) {
        player.setPos(broom.getX(), broom.getY(), broom.getZ());
        helper.assertTrue(player.startRiding(broom, true), "Broom vibration test rider should mount directly");
        return player;
    }

    private static void placeVibrationSensor(GameTestHelper helper) {
        // 騎乗後に配置し、ENTITY_MOUNTではなく継続する飛行振動だけを検証する。
        helper.setBlock(VIBRATION_SENSOR_POS, Blocks.SCULK_SENSOR);
    }

    private static boolean isVibrationSensorActive(GameTestHelper helper) {
        var state = helper.getBlockState(VIBRATION_SENSOR_POS);
        return state.getValue(SculkSensorBlock.PHASE) == SculkSensorPhase.ACTIVE
                && state.getValue(SculkSensorBlock.POWER) > 0;
    }

    private static void assertVibrationSensorActive(GameTestHelper helper, String message) {
        helper.assertTrue(isVibrationSensorActive(helper), message);
    }

    private static HoverrideBroomServerConfig.Values rushTestConfig(double forwardManaCost) {
        return new HoverrideBroomServerConfig.Values(
                forwardManaCost,
                0.0D,
                50.0D,
                forwardManaCost,
                0.0D,
                100.0D,
                0.5D,
                20.0D,
                4.0D,
                8.0D,
                0.5D
        );
    }

    private static void installRushStyle(HoverrideBroomEntity broom) {
        var stack = new ItemStack(ItemRegistry.HOVERRIDE_BROOM.get());
        if (!SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                stack,
                0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.TWILIGHT_GALE.get())
        )) {
            throw new IllegalStateException("Hoverride rush test could not install Twilight Gale");
        }
        broom.setBroomItemStack(stack);
    }

    private static Zombie spawnRushTarget(GameTestHelper helper, Vec3 position) {
        var target = EntityType.ZOMBIE.create(helper.getLevel());
        if (target == null) {
            throw new IllegalStateException("Hoverride rush test could not create a zombie target");
        }
        target.setNoAi(true);
        target.setPos(position);
        helper.getLevel().addFreshEntity(target);
        return target;
    }

    private static void installBubbleColumn(net.minecraft.server.level.ServerLevel level, BlockPos bottom, int height) {
        level.setBlockAndUpdate(bottom.below(), Blocks.SOUL_SAND.defaultBlockState());
        for (var offset = 0; offset < height; ++offset) {
            level.setBlockAndUpdate(
                    bottom.above(offset),
                    Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(BubbleColumnBlock.DRAG_DOWN, false)
            );
        }
    }

    private static void installWaterColumn(net.minecraft.server.level.ServerLevel level, BlockPos bottom, int height) {
        for (var offset = 0; offset < height; ++offset) {
            level.setBlockAndUpdate(bottom.above(offset), Blocks.WATER.defaultBlockState());
        }
    }

    private static void assertDismountPreservesMovement(
            GameTestHelper helper,
            AbstractBroomEntity broom,
            String broomName
    ) {
        var player = serverRider(helper);
        player.getAbilities().instabuild = true;
        helper.assertTrue(player.startRiding(broom, true), broomName + " rider should mount");

        broom.tick();
        broom.setPos(broom.getX() + 0.2D, broom.getY(), broom.getZ());
        broom.tick();
        var dismountX = broom.getX();
        player.stopRiding();

        helper.assertTrue(broom.getDeltaMovement().x > 0.19D,
                broomName + " should inherit its server-observed movement on dismount");
        broom.tick();
        helper.assertTrue(broom.getX() > dismountX + 0.15D,
                broomName + " should continue coasting after dismount");
        broom.discard();
    }

    private static void assertTooltipLine(GameTestHelper helper, List<Component> lines, int index,
                                          String expectedKey, int expectedArgumentCount) {
        helper.assertTrue(lines.get(index).getStyle().getColor() != null
                        && lines.get(index).getStyle().getColor().getValue() == ChatFormatting.GRAY.getColor(),
                "Tooltip line " + index + " should be gray");
        helper.assertTrue(lines.get(index).getContents() instanceof TranslatableContents,
                "Tooltip line " + index + " should be translatable");
        if (lines.get(index).getContents() instanceof TranslatableContents contents) {
            helper.assertTrue(expectedKey.equals(contents.getKey()),
                    "Tooltip line " + index + " has an unexpected translation key");
            helper.assertTrue(contents.getArgs().length == expectedArgumentCount,
                    "Tooltip line " + index + " has an unexpected argument count");
        }
    }

    private static void assertBroomCalibrationSelection(
            GameTestHelper helper,
            Item broomItem,
            AbstractBroomEntity broom,
            int xOffset,
            String broomName
    ) {
        helper.assertTrue(broom != null, broomName + " Broom entity should be creatable");
        if (broom == null) {
            return;
        }
        var stack = new ItemStack(broomItem);
        helper.assertTrue(((AbstractBroomItem) broomItem).getCalibrationAdjustmentSlotCount(stack) == 3,
                broomName + " Broom should expose three adjustment slots");
        installBroomScrolls(stack);
        helper.assertTrue(AbstractBroomItem.getEnabledCalibrationScrollSlotCount(stack) == 0,
                broomName + " Broom should start with zero enabled scroll slots");
        helper.assertFalse(AbstractBroomItem.getCalibrationScroll(stack, 2).isEmpty(),
                broomName + " Broom should retain scrolls stored in disabled slots");

        installBroomUpgrades(stack, 3);
        helper.assertTrue(AbstractBroomItem.getEnabledCalibrationScrollSlotCount(stack) == 3,
                broomName + " Broom should enable one scroll slot per upgrade");
        broom.setBroomItemStack(stack);
        var pos = helper.absolutePos(TEST_POS.offset(xOffset, 0, 0));
        broom.setPos(pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(broom);
        var rider = serverRider(helper, broomName.toLowerCase() + "_calibration_rider");
        helper.assertTrue(rider.startRiding(broom, true), broomName + " rider should mount");

        var selections = new SpellSelectionManager(rider)
                .getSpellsForSlot(BroomSpellSelectionEvents.SPELL_SELECTION_SLOT);
        helper.assertTrue(selections.size() == 3,
                broomName + " rider should receive all three enabled broom spells");
        for (var index = 0; index < selections.size(); ++index) {
            helper.assertTrue(selections.get(index).slotIndex == index,
                    broomName + " broom selections should use compact wheel indices");
        }

        var entityStack = broom.getBroomItemStack();
        helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        entityStack, 2, ItemStack.EMPTY),
                broomName + " Broom should allow removing its third slot upgrade");
        broom.setBroomItemStack(entityStack);
        helper.assertTrue(AbstractBroomItem.getEnabledCalibrationScrollSlotCount(entityStack) == 2,
                broomName + " Broom should disable the third scroll slot after upgrade removal");
        helper.assertFalse(AbstractBroomItem.getCalibrationScroll(entityStack, 2).isEmpty(),
                broomName + " Broom should retain the disabled third scroll");
        helper.assertTrue(new SpellSelectionManager(rider)
                        .getSpellsForSlot(BroomSpellSelectionEvents.SPELL_SELECTION_SLOT).size() == 2,
                broomName + " rider should only receive enabled broom spells");

        rider.stopRiding();
        helper.assertTrue(new SpellSelectionManager(rider)
                        .getSpellsForSlot(BroomSpellSelectionEvents.SPELL_SELECTION_SLOT).isEmpty(),
                broomName + " broom spells should disappear after dismounting");

        var menu = new SpellCalibrationBenchMenu(0, rider.getInventory());
        menu.getSlot(SpellCalibrationBenchMenu.TARGET_MENU_SLOT).set(entityStack);
        var disabledSlot = menu.getSlot(SpellCalibrationBenchMenu.SCROLL_MENU_SLOT_START + 2);
        helper.assertFalse(menu.isScrollSlotEnabled(2),
                broomName + " third scroll slot should be disabled in the Calibration Bench");
        helper.assertTrue(disabledSlot.hasItem() && disabledSlot.mayPickup(rider),
                broomName + " disabled scroll should remain removable from the Calibration Bench");
        var extracted = disabledSlot.remove(1);
        disabledSlot.onTake(rider, extracted);
        helper.assertFalse(extracted.isEmpty(), broomName + " disabled scroll extraction should return the scroll");
        helper.assertTrue(AbstractBroomItem.getCalibrationScroll(entityStack, 2).isEmpty(),
                broomName + " extracted disabled scroll should be removed from broom storage");
        broom.discard();
    }

    private static ItemStack calibratedBroomStack(Item broomItem, int enabledSlots) {
        var stack = new ItemStack(broomItem);
        installBroomCalibration(stack, enabledSlots);
        return stack;
    }

    private static ItemStack backCurioBroomStack(Item broomItem) {
        var stack = new ItemStack(broomItem);
        if (!SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                stack,
                2,
                new ItemStack(Items.SADDLE)
        )) {
            throw new IllegalStateException("Broom Curios calibration was rejected");
        }
        return stack;
    }

    private static ItemStack backCurioCalibratedBroomStack(Item broomItem, int enabledSlots) {
        if (enabledSlots < 0 || enabledSlots > 2) {
            throw new IllegalArgumentException("Back Curio broom supports zero to two scroll slot upgrades");
        }
        var stack = calibratedBroomStack(broomItem, enabledSlots);
        if (!SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                stack,
                2,
                new ItemStack(Items.SADDLE)
        )) {
            throw new IllegalStateException("Broom Curios calibration was rejected");
        }
        return stack;
    }

    private static void installBroomCalibration(ItemStack stack, int enabledSlots) {
        installBroomScrolls(stack);
        installBroomUpgrades(stack, enabledSlots);
    }

    private static void installBroomScrolls(ItemStack stack) {
        var namedScroll = createBroomScroll(SpellRegistry.MANTIS_LEAP.get());
        namedScroll.setHoverName(Component.literal("Calibrated Mantis Leap"));
        AbstractBroomItem.setCalibrationScroll(stack, 0, namedScroll);
        AbstractBroomItem.setCalibrationScroll(stack, 1, createBroomScroll(SpellRegistry.WIZARDLAMP.get()));
        AbstractBroomItem.setCalibrationScroll(stack, 2, createBroomScroll(SpellRegistry.MANA_CHARGE.get()));
    }

    private static void installBroomUpgrades(ItemStack stack, int count) {
        for (var slot = 0; slot < count; ++slot) {
            var accepted = SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    stack,
                    slot,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
            );
            if (!accepted) {
                throw new IllegalStateException("Broom slot upgrade was rejected at slot " + slot);
            }
        }
    }

    private static ItemStack createBroomScroll(io.redspace.ironsspellbooks.api.spells.AbstractSpell spell) {
        var scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, 1, scroll);
        return scroll;
    }

    private static void assertCalibrationContents(
            GameTestHelper helper,
            ItemStack stack,
            int enabledSlots,
            String subject
    ) {
        helper.assertTrue(AbstractBroomItem.getEnabledCalibrationScrollSlotCount(stack) == enabledSlots,
                subject + " should preserve enabled scroll slot count");
        var expectedSpells = List.of(
                SpellRegistry.MANTIS_LEAP.get(),
                SpellRegistry.WIZARDLAMP.get(),
                SpellRegistry.MANA_CHARGE.get()
        );
        for (var slot = 0; slot < expectedSpells.size(); ++slot) {
            helper.assertTrue(AbstractBroomItem.getCalibrationSpellData(stack, slot).getSpell() == expectedSpells.get(slot),
                    subject + " should preserve calibration scroll " + slot);
        }
        helper.assertTrue(Component.literal("Calibrated Mantis Leap").equals(
                        AbstractBroomItem.getCalibrationScroll(stack, 0).getHoverName()),
                subject + " should preserve the full stored scroll ItemStack");
    }

    private static void assertComponentKey(GameTestHelper helper, Object argument,
                                           String expectedKey, String message) {
        helper.assertTrue(argument instanceof Component, message + " (argument is not a component)");
        if (argument instanceof Component component) {
            helper.assertTrue(component.getContents() instanceof KeybindContents contents
                            && expectedKey.equals(contents.getName()), message);
        }
    }

    private static FloatmountBroomEntity placeBroomFromItem(GameTestHelper helper, Player player, ItemStack stack) {
        var target = helper.absolutePos(TEST_POS);
        player.setPos(target.getX() + 0.5D, target.getY() + 2.5D, target.getZ() + 0.5D);
        player.setXRot(90.0F);
        player.setYRot(0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        var brooms = helper.getLevel().getEntitiesOfClass(
                FloatmountBroomEntity.class,
                new AABB(target).inflate(2.0D, 4.0D, 2.0D)
        );
        helper.assertTrue(brooms.size() == 1, "Broom item use should place exactly one broom entity");
        return brooms.get(0);
    }

    private static HoverrideBroomEntity placeHoverrideBroomFromItem(
            GameTestHelper helper,
            Player player,
            ItemStack stack
    ) {
        var target = helper.absolutePos(TEST_POS);
        player.setPos(target.getX() + 0.5D, target.getY() + 2.5D, target.getZ() + 0.5D);
        player.setXRot(90.0F);
        player.setYRot(0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        var brooms = helper.getLevel().getEntitiesOfClass(
                HoverrideBroomEntity.class,
                new AABB(target).inflate(2.0D, 4.0D, 2.0D)
        );
        helper.assertTrue(brooms.size() == 1,
                "Hoverride Broom item use should place exactly one dedicated entity");
        return brooms.get(0);
    }

    private static ItemStack findBroomInInventory(GameTestHelper helper, Player player) {
        return findBroomInInventory(helper, player, ItemRegistry.FLOATMOUNT_BROOM.get());
    }

    private static ItemStack findBroomInInventory(GameTestHelper helper, Player player, Item broomItem) {
        for (var slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.is(broomItem)) {
                return stack;
            }
        }
        helper.fail("Recovered broom item was not found in the player inventory");
        return ItemStack.EMPTY;
    }

    private static Player player(GameTestHelper helper, String name) {
        return serverRider(helper, "broom_" + Integer.toUnsignedString(name.hashCode(), 36));
    }

    private static ServerPlayer serverRider(GameTestHelper helper) {
        return serverRider(helper, "broom_rider_" + UUID.randomUUID().toString().substring(0, 4));
    }

    private static ServerPlayer serverRider(GameTestHelper helper, String profileName) {
        // Epic Fightはplayer tickでclient型を参照するため、world tickされる騎乗者には
        // 軽量mockではなく接続を備えたserver playerを使う。
        var level = helper.getLevel();
        var profile = new GameProfile(UUID.randomUUID(), profileName);
        var player = new ServerPlayer(level.getServer(), level, profile);
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, player);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        return player;
    }

    private static ServerPlayer calledBroomPlayer(GameTestHelper helper, BlockPos relativePos, String profileName) {
        var player = serverRider(helper, profileName);
        var position = helper.absoluteVec(Vec3.atBottomCenterOf(relativePos));
        player.setPos(position.x, position.y, position.z);
        return player;
    }

    private static MagicData magicData(GameTestHelper helper, Player player) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Floatmount Broom test could not resolve player mana data");
        return magicData;
    }

    private static void assertMovementManaCost(
            GameTestHelper helper,
            String playerName,
            float expectedCost,
            float strafe,
            float forward,
            boolean ascending,
            boolean descending
    ) {
        assertMovementManaCost(
                helper, playerName, expectedCost, strafe, forward, ascending, descending, false
        );
    }

    private static void assertMovementManaCost(
            GameTestHelper helper,
            String playerName,
            float expectedCost,
            float strafe,
            float forward,
            boolean ascending,
            boolean descending,
            boolean overdriveEnabled
    ) {
        var broom = spawnBroom(helper, 1.5D);
        if (overdriveEnabled) {
            var stack = new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get());
            if (!SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    stack, 0, new ItemStack(ItemRegistry.OVERDRIVE_BROOM_ENGINE.get())
            )) {
                throw new IllegalStateException("Floatmount Overdrive mana test could not install its engine");
            }
            broom.setBroomItemStack(stack);
        }
        var player = serverRider(helper);
        var magicData = magicData(helper, player);
        magicData.setMana(100.0F);
        helper.assertTrue(player.startRiding(broom, true), playerName + " should mount directly");
        broom.acceptServerInput(player, strafe, forward, ascending, descending);

        broom.tick();

        helper.assertTrue(Math.abs(magicData.getMana() - (100.0F - expectedCost)) < 1.0e-4F,
                playerName + " used an unexpected mana cost: " + magicData.getMana());
        player.stopRiding();
    }

    private static int countDroppedBrooms(GameTestHelper helper, net.minecraft.world.phys.Vec3 center) {
        return droppedBrooms(helper, center)
                .stream().mapToInt(item -> item.getItem().getCount()).sum();
    }

    private static ItemEntity findDroppedBroom(GameTestHelper helper, net.minecraft.world.phys.Vec3 center) {
        var drops = droppedBrooms(helper, center);
        helper.assertTrue(drops.size() == 1, "Expected exactly one dropped broom item entity");
        return drops.get(0);
    }

    private static java.util.List<ItemEntity> droppedBrooms(GameTestHelper helper,
                                                             net.minecraft.world.phys.Vec3 center) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, AABB.ofSize(center, 4.0D, 4.0D, 4.0D),
                item -> item.getItem().is(ItemRegistry.FLOATMOUNT_BROOM.get()));
    }
}
