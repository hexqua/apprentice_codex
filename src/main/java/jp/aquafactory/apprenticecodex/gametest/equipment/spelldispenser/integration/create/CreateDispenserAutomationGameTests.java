package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.SpellDispenserGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CreateDispenserAutomationGameTests {
    private CreateDispenserAutomationGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellDispenserFluidAcceptsCreateManaPotion(GameTestHelper helper) {
        CreateDispenserAutomationScenarios.spellDispenserFluidAcceptsCreateManaPotion(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateRefillsFromFuelStorageAndReturnsBottle(GameTestHelper helper) {
        CreateDispenserAutomationScenarios.spellDispenserCreateRefillsFromFuelStorageAndReturnsBottle(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateIgnoresProtectedFuelStorage(GameTestHelper helper) {
        CreateDispenserAutomationScenarios.spellDispenserCreateIgnoresProtectedFuelStorage(helper);
    }

    static FluidStack createCreateManaPotionFluid(Potion potion, int amountMb) {
        var createPotion = ForgeRegistries.FLUIDS.getValue(ResourceLocation.fromNamespaceAndPath("create", "potion"));
        if (createPotion == null) {
            return FluidStack.EMPTY;
        }

        var fluid = new FluidStack(createPotion, amountMb);
        var tag = fluid.getOrCreateTag();
        tag.putString("Potion", BuiltInRegistries.POTION.getKey(potion).toString());
        tag.putString("Bottle", "REGULAR");
        return fluid;
    }
}
