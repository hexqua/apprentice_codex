package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsFollow;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection.Mode;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsTransition;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // GameTestはアノテーション走査で登録され、Java側の直接参照を持たない。
public final class PsychicDriveGauntletsGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private PsychicDriveGauntletsGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void registeredItemHasMinimalProperties(GameTestHelper helper) {
        var item = ItemRegistry.PSYCHIC_DRIVE_GAUNTLETS.get();
        var stack = new ItemStack(item);
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(item).equals(ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID, "psychic_drive_gauntlets")), "Gauntlets must use their intended content ID");
        helper.assertTrue(stack.getMaxStackSize() == 1, "Gauntlets must have a stack limit of one");
        var damage = new double[] {1.0};
        var speed = new double[] {4.0};
        stack.getAttributeModifiers().forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) damage[0] += modifier.amount();
            if (attribute.equals(Attributes.ATTACK_SPEED)) speed[0] += modifier.amount();
        });
        helper.assertTrue(Math.abs(damage[0] - 8.0) < 1.0e-6, "Gauntlets must display attack damage eight");
        helper.assertTrue(Math.abs(speed[0] - 20.0 / 15.0) < 1.0e-6, "Gauntlets must display a fifteen-tick attack speed");
        stack.getAttributeModifiers().forEach(EquipmentSlot.OFFHAND, (attribute, modifier) ->
                helper.assertTrue(!attribute.equals(Attributes.ATTACK_DAMAGE) && !attribute.equals(Attributes.ATTACK_SPEED),
                        "Offhand gauntlets must not add combat attributes"));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void selectionFollowsHandsThenLeftmostHotbar(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "psychic_gauntlets_selection");
        var inventory = player.getInventory();
        inventory.clearContent();
        inventory.selected = 0;
        var leftmost = new ItemStack(ItemRegistry.PSYCHIC_DRIVE_GAUNTLETS.get());
        var later = leftmost.copy();
        var offhand = leftmost.copy();

        // 通常インベントリだけでは表示せず、ホットバーの最終スロットも対象にする。
        inventory.setItem(9, leftmost);
        helper.assertTrue(PsychicDriveGauntletsSelection.find(player, true).mode() == Mode.HIDDEN,
                "Inventory-only gauntlets must stay hidden");
        inventory.setItem(9, ItemStack.EMPTY);
        inventory.setItem(8, later);
        var selection = PsychicDriveGauntletsSelection.find(player, true);
        helper.assertTrue(selection.mode() == Mode.SHEATHED && selection.stack() == later,
                "The last hotbar slot must allow sheathed gauntlets");
        inventory.setItem(2, leftmost);
        helper.assertTrue(PsychicDriveGauntletsSelection.find(player, true).stack() == leftmost,
                "The leftmost hotbar gauntlets must win");
        helper.assertTrue(PsychicDriveGauntletsSelection.find(player, false).mode() == Mode.HIDDEN,
                "Remote presentation must not inspect an unsynchronized hotbar");

        inventory.selected = 8;
        selection = PsychicDriveGauntletsSelection.find(player, true);
        helper.assertTrue(selection.mode() == Mode.DRAWN && selection.stack() == later,
                "Selected mainhand gauntlets must win over earlier hotbar slots");
        player.setItemInHand(InteractionHand.OFF_HAND, offhand);
        helper.assertTrue(PsychicDriveGauntletsSelection.find(player, true).stack() == offhand,
                "Offhand gauntlets must win over mainhand and hotbar gauntlets");
        helper.assertTrue(PsychicDriveGauntletsSelection.find(player, false).stack() == offhand,
                "Remote players must support synchronized offhand gauntlets");

        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        helper.assertTrue(PsychicDriveGauntletsSelection.find(player, false).mode() == Mode.DRAWN,
                "Remote players must support synchronized mainhand gauntlets");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        helper.assertTrue(PsychicDriveGauntletsSelection.find(player, true).mode() == Mode.SHEATHED,
                "Switching to another item must sheath remaining hotbar gauntlets");
        inventory.setItem(2, ItemStack.EMPTY);
        helper.assertTrue(PsychicDriveGauntletsSelection.find(player, true).mode() == Mode.HIDDEN,
                "Removing the final eligible stack must hide the gauntlets");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void transitionEasesOutAndReversesWithoutJumping(GameTestHelper helper) {
        var transition = new PsychicDriveGauntletsTransition();
        transition.update(Mode.SHEATHED, 100.0);
        assertAmount(helper, transition.amount(100.0), 0.0F, "Initial sheath must appear at its final position");
        transition.update(Mode.DRAWN, 100.0);
        assertAmount(helper, transition.amount(100.0), 0.0F, "Drawing must start from the sheath");
        helper.assertTrue(transition.amount(102.0) > 0.2F && transition.amount(102.0) < 1.0F,
                "Drawing must advance faster than linear initially without reaching the target early");
        assertAmount(helper, transition.amount(105.0), 0.875F, "Halfway drawing must follow cubic ease-out");
        assertAmount(helper, transition.amount(110.0), 1.0F, "Drawing must finish after ten ticks");

        transition.update(Mode.SHEATHED, 110.0);
        var reversalAmount = transition.amount(113.5);
        transition.update(Mode.DRAWN, 113.5);
        assertAmount(helper, transition.amount(113.5), reversalAmount, "Reversing must preserve current position");
        transition.update(Mode.DRAWN, 117.0);
        assertAmount(helper, transition.amount(123.5), 1.0F,
                "Same-mode updates must not restart the ten-tick transition");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hiddenStateResetsAndFirstDrawingSnaps(GameTestHelper helper) {
        var transition = new PsychicDriveGauntletsTransition();
        transition.update(Mode.DRAWN, 10.0);
        assertAmount(helper, transition.amount(10.0), 1.0F, "First drawing must appear at its final position");
        transition.update(Mode.SHEATHED, 11.0);
        transition.update(Mode.HIDDEN, 12.0);
        transition.update(Mode.SHEATHED, 13.0);
        assertAmount(helper, transition.amount(13.0), 0.0F, "Reappearing must not resume an obsolete transition");
        helper.succeed();
    }

    private static void assertAmount(GameTestHelper helper, float actual, float expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0e-6F, message + ": " + actual);
    }

    @GameTest(template = TEMPLATE)
    public static void followUsesElapsedTimeInsteadOfFrameCount(GameTestHelper helper) {
        var coarse = new PsychicDriveGauntletsFollow();
        var fine = new PsychicDriveGauntletsFollow();
        coarse.follow(Vec3.ZERO, Vec3.ZERO, 0.0, 1.0F);
        fine.follow(Vec3.ZERO, Vec3.ZERO, 0.0, 1.0F);
        var target = new Vec3(0.4, 0.0, 0.0);
        var coarsePosition = coarse.follow(target, Vec3.ZERO, 1.5, 1.0F);
        var finePosition = Vec3.ZERO;
        for (var frame = 1; frame <= 6; frame++) {
            finePosition = fine.follow(target, Vec3.ZERO, frame * 0.25, 1.0F);
        }
        helper.assertTrue(coarsePosition.distanceTo(finePosition) < 1.0e-9,
                "Follow response must agree across frame rates for a fixed target");
        helper.assertTrue(Math.abs(coarsePosition.x - 0.2) < 1.0e-9,
                "Drawn follow must close half the remaining distance in 1.5 ticks");
        helper.assertTrue(coarse.follow(target, Vec3.ZERO, 1.5, 1.0F).equals(coarsePosition),
                "Rendering again at the same time must not advance the follow");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void followBoundsLagAndResetsForSheathingAndTeleport(GameTestHelper helper) {
        var follow = new PsychicDriveGauntletsFollow();
        follow.follow(Vec3.ZERO, Vec3.ZERO, 0.0, 1.0F);
        var target = new Vec3(2.0, 0.0, 0.0);
        var position = follow.follow(target, Vec3.ZERO, 0.1, 1.0F);
        helper.assertTrue(position.distanceTo(target) <= 0.600000001,
                "Rapid turning must not leave gauntlets more than 0.6 blocks behind their target");
        helper.assertTrue(follow.follow(target, Vec3.ZERO, 0.1, 0.5F).distanceTo(target) <= 0.300000001,
                "Follow lag must fade with the draw amount during sheathing");
        helper.assertTrue(follow.follow(target, Vec3.ZERO, 0.2, 0.0F).equals(target),
                "Fully sheathed gauntlets must retain exact body attachment");
        var teleportAnchor = new Vec3(10.0, 0.0, 0.0);
        var teleportTarget = teleportAnchor.add(0.7, -0.25, 0.5);
        helper.assertTrue(follow.follow(teleportTarget, teleportAnchor, 0.3, 1.0F).equals(teleportTarget),
                "Teleporting must snap gauntlets to the new target");
        var resumedTarget = teleportTarget.add(0.3, 0.0, 0.0);
        helper.assertTrue(follow.follow(resumedTarget, teleportAnchor, 20.0, 1.0F).equals(resumedTarget),
                "Resuming after a long render gap must discard stale follow position");
        helper.succeed();
    }
}
