package jp.aquafactory.apprenticecodex.gametest.equipment.broom.control;

import io.netty.buffer.Unpooled;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.HoverrideBroomServerConfig;
import jp.aquafactory.apprenticecodex.entity.broom.BroomCoreWarningState;
import jp.aquafactory.apprenticecodex.entity.broom.BroomInputTransition;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomPresentation;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.item.FloatmountBroomConfigState;
import jp.aquafactory.apprenticecodex.item.broom.AbstractBroomItem;
import jp.aquafactory.apprenticecodex.network.packet.ClientBroomInputPacket;
import jp.aquafactory.apprenticecodex.network.packet.HoverrideBroomImpulseEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.HoverrideBroomReleaseResultPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncFloatmountBroomConfigPacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.environment.BroomEnvironmentGameTests.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.environment.BroomEnvironmentGameTests.magicData;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.serverRider;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.spawnHoverrideBroom;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BroomPresentationGameTests {
    private BroomPresentationGameTests() {}

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
    public static void configSyncPacketPreservesManaThreshold(GameTestHelper helper) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        SyncFloatmountBroomConfigPacket.encode(new SyncFloatmountBroomConfigPacket(4321), buffer);
        var decoded = SyncFloatmountBroomConfigPacket.decode(buffer);
        helper.assertTrue(decoded.normalFlightManaThreshold() == 4321,
                "Floatmount Broom config sync should preserve the mana threshold");
        helper.succeed();
    }

    public static void assertTooltipLine(GameTestHelper helper, List<Component> lines, int index,
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

    static void assertComponentKey(GameTestHelper helper, Object argument,
                                           String expectedKey, String message) {
        helper.assertTrue(argument instanceof Component, message + " (argument is not a component)");
        if (argument instanceof Component component) {
            helper.assertTrue(component.getContents() instanceof KeybindContents contents
                            && expectedKey.equals(contents.getName()), message);
        }
    }
}
