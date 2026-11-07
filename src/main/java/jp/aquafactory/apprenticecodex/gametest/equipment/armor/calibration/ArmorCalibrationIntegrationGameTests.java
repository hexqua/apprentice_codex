package jp.aquafactory.apprenticecodex.gametest.equipment.armor.calibration;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.item.armor.EndgameArmorCalibration;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.integration.create.CreateDispenserTestSupport.invokeCreateGameTestHook;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ArmorCalibrationIntegrationGameTests {
    private ArmorCalibrationIntegrationGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void malumAdjustmentsFollowAvailabilityAndUpdateEquippedAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var coat = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get());
            var plate = new ItemStack(ItemRegistry.SOUL_COVERED_PLATE.get());
            var loaded = ModList.get().isLoaded("malum");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(coat, 0, plate) == loaded,
                    "Soul adjustment insertion should follow Malum availability");
            if (!loaded) {
                return;
            }
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(coat, 1,
                            new ItemStack(ItemRegistry.SOUL_AUGMENTED_WEAVE.get())), "Soul weave should coexist with plate");
            var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.ATTRIBUTE);
            var capacity = registry.getHolder(EndgameArmorCalibration.SOUL_WARD_CAPACITY_ATTRIBUTE).orElseThrow();
            var recovery = registry.getHolder(EndgameArmorCalibration.SOUL_WARD_RECOVERY_RATE_ATTRIBUTE).orElseThrow();
            var proficiency = registry.getHolder(EndgameArmorCalibration.MAGIC_PROFICIENCY_ATTRIBUTE).orElseThrow();
            try (var fixture = new ArmorEquipmentFixture(helper)) {
                fixture.player().getAttribute(capacity).setBaseValue(2);
                fixture.player().getAttribute(recovery).setBaseValue(1);
                fixture.player().getAttribute(proficiency).setBaseValue(0.2);
                fixture.equip(coat, EquipmentSlot.CHEST);
                fixture.assertAttribute(capacity, 5, "Soul plate equipped");
                fixture.assertAttribute(recovery, 1.15, "Soul plate recovery equipped");
                fixture.assertAttribute(proficiency, 0.23, "Soul weave equipped");
                fixture.equip(coat, EquipmentSlot.CHEST);
                fixture.assertAttribute(capacity, 5, "Soul plate re-equipped");
                fixture.equip(ItemStack.EMPTY, EquipmentSlot.CHEST);
                fixture.assertAttribute(capacity, 2, "Soul plate removed");
                fixture.assertAttribute(recovery, 1, "Soul plate recovery removed");
                fixture.assertAttribute(proficiency, 0.2, "Soul weave removed");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void createRecognizesGogglesOnlyWhileCalibratedHelmetIsWorn(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded("create")) {
                return;
            }
            var goggles = BuiltInRegistries.ITEM.getOptional(EndgameArmorCalibration.CREATE_GOGGLES).orElseThrow();
            var hood = new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get());
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(hood, 0,
                            new ItemStack(goggles)), "Create goggles should be accepted by helmet");
            try (var fixture = new ArmorEquipmentFixture(helper)) {
                helper.assertFalse(wearingGoggles(fixture.player()), "Unequipped player should not have goggles");
                fixture.equip(hood, EquipmentSlot.HEAD);
                helper.assertTrue(wearingGoggles(fixture.player()), "Create should recognize calibrated helmet");
                fixture.equip(ItemStack.EMPTY, EquipmentSlot.HEAD);
                helper.assertFalse(wearingGoggles(fixture.player()), "Removing calibrated helmet should remove goggles effect");
            }
        });
    }

    private static boolean wearingGoggles(Player player) {
        return (boolean) invokeCreateGameTestHook("isWearingGoggles", new Class<?>[]{Player.class}, player);
    }
}
