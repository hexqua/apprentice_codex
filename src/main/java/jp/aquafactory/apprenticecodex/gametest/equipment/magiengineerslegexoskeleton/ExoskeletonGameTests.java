package jp.aquafactory.apprenticecodex.gametest.equipment.magiengineerslegexoskeleton;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonAction;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonInputState;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonMovement;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.MagiEngineersLegExoskeleton;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ExoskeletonGameTests {
    private ExoskeletonGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void everyActivationReservesMana(GameTestHelper h) {
        var state = new ExoskeletonInputState();
        var first = ExoskeletonAction.select(false, false, true, false);
        state.reserve(first.manaCost(15, 20));
        h.assertTrue(state.availableMana(30) == 15, "First airborne lifter must reserve mana");
        var next = ExoskeletonAction.select(false, true, true, false);
        state.reserve(next.manaCost(15, 20));
        h.assertTrue(state.availableMana(30) == 0, "Repeated lifter must reserve the same cost without a free activation");
        var ground = ExoskeletonAction.select(true, true, false, true);
        h.assertTrue(ground.manaCost(15, 20) > state.availableMana(49),
                "Ground strider must also require mana rather than bypassing reservations");
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void simultaneousInputSelectsOnlyStrider(GameTestHelper h) {
        var state = new ExoskeletonInputState();
        h.assertTrue(ExoskeletonAction.select(true, false, true, true) == null, "Walking ground input must not activate either function");
        h.assertTrue(ExoskeletonAction.select(true, true, true, false) == null, "Ground lifter-only input must not activate strider");
        var action = ExoskeletonAction.select(false, true, true, true);
        h.assertTrue(action == ExoskeletonAction.AIR_STRIDER, "Strider must win simultaneous airborne sprint input");
        h.assertTrue(action.manaCost(15, 20) > state.availableMana(19),
                "Insufficient strider mana must reject the selected action without a lifter fallback");
        h.assertTrue(ExoskeletonAction.select(false, false, false, true) == null, "Non-sprinting strider input must not activate lifter");
        h.assertTrue(ExoskeletonAction.select(true, true, false, true) == ExoskeletonAction.GROUND_STRIDER,
                "Ground sprint input must select strider");
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void pendingManaCannotBeSpentTwice(GameTestHelper h) {
        var state = new ExoskeletonInputState();
        long first = state.reserve(15);
        long second = state.reserve(20);
        h.assertTrue(state.availableMana(40) == 5, "Pending actions must reserve mana before server replies");
        h.assertTrue(state.acknowledge(first) && state.availableMana(25) == 5,
                "Confirming one action must retain later reservations");
        h.assertFalse(state.acknowledge(first), "Duplicate replies must not restore spent mana");
        h.assertTrue(state.acknowledge(second) && state.availableMana(25) == 25,
                "Rejected server action must also release its reservation");
        state.reserve(15);
        state.reset();
        h.assertFalse(state.acknowledge(second), "Old world replies must be ignored after reset");
        h.assertTrue(state.availableMana(25) == 25, "World reset must clear reservations");
        long free = state.reserve(0);
        h.assertTrue(state.availableMana(0) == 0 && state.acknowledge(free), "Zero-cost configuration must work at zero mana");
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void feetAttributesStackAndRemoveIndependently(GameTestHelper h) {
        var player = createEquipmentTestPlayer(h, new BlockPos(1, 2, 1), "exo_attributes");
        var stack = new ItemStack(ItemRegistry.MAGI_ENGINEERS_LEG_EXOSKELETON.get());
        h.assertTrue(stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "feet"))),
                "Exoskeleton must be accepted by the feet slot tag");
        equipCurio(player, "feet", stack);
        h.assertTrue(ExoskeletonMovement.equipped(player), "Movement lookup must find the equipped exoskeleton");
        var attribute = player.getAttribute(Attributes.SAFE_FALL_DISTANCE);
        double base = attribute.getValue();
        var other = new AttributeModifier(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "exo_test_other"),
                3, AttributeModifier.Operation.ADD_VALUE);
        attribute.addTransientModifier(other);
        var item = (MagiEngineersLegExoskeleton) stack.getItem();
        var modifiers = item.getAttributeModifiers(new SlotContext("feet", player, 0, false, true),
                ResourceLocation.fromNamespaceAndPath("curios", "feet/0"), stack);
        player.getAttributes().addTransientAttributeModifiers(modifiers);
        h.assertTrue(attribute.getValue() == base + 8, "Exoskeleton must add safe fall distance alongside another source");
        player.getAttributes().removeAttributeModifiers(modifiers);
        equipCurio(player, "feet", ItemStack.EMPTY);
        h.assertTrue(attribute.getValue() == base + 3 && !ExoskeletonMovement.equipped(player),
                "Unequipping must preserve unrelated safe fall modifiers");
        var tooltip = item.getSlotsTooltip(List.of(Component.literal("slot")), null, stack);
        h.assertTrue(tooltip.get(0).getString().equals("slot") && tooltip.get(1).getStyle().getColor().getValue() == 0x808080
                        && tooltip.get(2).getString().isEmpty(), "Lore must immediately follow the slot line before a blank line");
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void striderCorrectionUsesPitchAndReplacesVerticalMomentum(GameTestHelper h) {
        var player = createEquipmentTestPlayer(h, new BlockPos(1, 2, 1), "exo_correction");
        player.setXRot(0);
        player.setDeltaMovement(0.4, -1, 0.6);
        ExoskeletonMovement.applyStriderCorrection(player, ExoskeletonAction.AIR_STRIDER);
        h.assertTrue(player.getDeltaMovement().equals(new Vec3(0.4, 0.4, 0.6)) && player.fallDistance == -5,
                "Level-view air strider must replace downward momentum while preserving horizontal movement");
        player.setDeltaMovement(0.4, 1.2, 0.6);
        ExoskeletonMovement.applyStriderCorrection(player, ExoskeletonAction.AIR_STRIDER);
        h.assertTrue(player.getDeltaMovement().y == 0.4, "Level-view air strider must also discard existing upward momentum");
        player.setXRot(-90);
        ExoskeletonMovement.applyStriderCorrection(player, ExoskeletonAction.AIR_STRIDER);
        h.assertTrue(Math.abs(player.getDeltaMovement().y - 0.6) < 1.0e-6, "Looking up must increase strider lift");
        player.setXRot(90);
        ExoskeletonMovement.applyStriderCorrection(player, ExoskeletonAction.AIR_STRIDER);
        h.assertTrue(Math.abs(player.getDeltaMovement().y - 0.2) < 1.0e-6, "Looking down must decrease strider lift");
        player.setDeltaMovement(0, -0.2, 0);
        ExoskeletonMovement.applyStriderCorrection(player, ExoskeletonAction.GROUND_STRIDER);
        h.assertTrue(player.getDeltaMovement().y == -0.2 && player.fallDistance == -5,
                "Ground strider must correct fall distance without replacing vertical momentum");
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magitechMovementAndManaIntegration(GameTestHelper h) {
        if (MagiEngineersLegExoskeleton.available()) ExoskeletonMagitechScenarios.movement(h);
        else {
            var player = createEquipmentTestPlayer(h, new BlockPos(1, 2, 1), "exo_absent");
            equipCurio(player, "feet", new ItemStack(ItemRegistry.MAGI_ENGINEERS_LEG_EXOSKELETON.get()));
            h.assertFalse(ExoskeletonMovement.accept(player, ExoskeletonAction.LIFTER),
                    "Missing Magitech must disable movement while retaining the item");
            h.succeed();
        }
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magitechCooldownAndConfigIntegration(GameTestHelper h) {
        if (MagiEngineersLegExoskeleton.available()) ExoskeletonMagitechScenarios.cooldownAndConfig(h);
        else h.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magitechTooltipUsesSyncedCostsAndColors(GameTestHelper h) {
        if (MagiEngineersLegExoskeleton.available()) ExoskeletonMagitechScenarios.tooltip(h);
        else h.succeed();
    }
}
