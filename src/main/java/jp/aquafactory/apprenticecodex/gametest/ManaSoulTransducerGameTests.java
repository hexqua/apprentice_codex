package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumStaffChargeBridge;
import jp.aquafactory.apprenticecodex.item.curios.manasoultransducer.ManaSoulTransducerLogic;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ManaSoulTransducerGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private ManaSoulTransducerGameTests() {}
    @GameTest(template = TEMPLATE)
    public static void transferUsesPositiveBonusesAndSoftCap(GameTestHelper h) {
        for (double rate : new double[]{0, 0.8, 1}) {
            close(h, ManaSoulTransducerLogic.durationModifier(0.5, rate), 0);
            close(h, ManaSoulTransducerLogic.durationModifier(1, rate), 0);
            close(h, ManaSoulTransducerLogic.durationModifier(1.5, rate), -0.5 * rate);
            close(h, ManaSoulTransducerLogic.durationModifier(2, rate), -0.75 * rate);
        }
        h.assertTrue(ManaSoulTransducerLogic.chargeTicks(21, 1.5, 0.8) == 13, "Fractional charge ticks must round up");
        h.assertTrue(ManaSoulTransducerLogic.chargeTicks(20, 1000000, 1) == 1, "Minimum charge must be one tick");
        h.succeed();
    }
    @GameTest(template = TEMPLATE)
    public static void soulConversionIsAlwaysDisabled(GameTestHelper h) {
        var spell = SpellRegistry.SOUL_CONVERSION.get();
        h.assertFalse(spell.getDefaultConfig().enabled, "Default config must disable Soul Conversion");
        try {
            spell.getDefaultConfig().enabled = true;
            h.assertFalse(spell.isEnabled(), "Config must not enable Soul Conversion");
            h.assertFalse(io.redspace.ironsspellbooks.api.registry.SpellRegistry.getEnabledSpells().contains(spell), "Research must exclude Soul Conversion");
        } finally { spell.getDefaultConfig().enabled = false; }
        h.succeed();
    }
    @GameTest(template = TEMPLATE, batch = "apprenticecodex.malum_compat")
    public static void legacyStaffPayments(GameTestHelper h) {
        if (MalumStaffChargeBridge.isAvailable()) ManaSoulTransducerMalumScenarios.payments(h);
        h.succeed();
    }
    @GameTest(template = TEMPLATE)
    public static void researchMarkersAndRecipe(GameTestHelper h) {
        h.assertTrue(SpellRegistry.ECHO_CAST.get() instanceof jp.aquafactory.apprenticecodex.spell.HiddenFromEldritchResearch,
                "Echo Cast must be hidden from research");
        h.assertTrue(SpellRegistry.PALETTE_SHIFT.get() instanceof jp.aquafactory.apprenticecodex.spell.HiddenFromEldritchResearch,
                "Palette Shift must be hidden from research");
        h.assertTrue(SpellRegistry.SOUL_CONVERSION.get() instanceof jp.aquafactory.apprenticecodex.spell.HiddenFromEldritchResearch,
                "Soul Conversion must be hidden from research");
        var recipeId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID,
                "malum/spirit_infusion/mana_soul_transducer");
        if (MalumStaffChargeBridge.isAvailable()) {
            jp.aquafactory.apprenticecodex.gametest.malum.MalumGameTestHooks.assertSpiritInfusionRecipe(
                    h.getLevel(), recipeId, malumItem("runic_brooch", 1),
                    java.util.List.of(malumItem("fused_consciousness", 1), malumItem("mnemonic_fragment", 8),
                            malumItem("malignant_pewter_plating", 4)),
                    new net.minecraft.world.item.ItemStack(jp.aquafactory.apprenticecodex.registry.ItemRegistry.MANA_SOUL_TRANSDUCER.get()),
                    java.util.Map.of("aerial_spirit", 16, "aqueous_spirit", 16, "arcane_spirit", 64, "eldritch_spirit", 64));
        } else {
            h.assertFalse(h.getLevel().getRecipeManager().byKey(recipeId).isPresent(), "Malum recipe must be conditional");
        }
        h.succeed();
    }
    private static net.minecraft.world.item.ItemStack malumItem(String path, int count) {
        return new net.minecraft.world.item.ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("malum", path)), count);
    }
    private static void close(GameTestHelper h, double actual, double expected) {
        h.assertTrue(Math.abs(actual - expected) < 1e-6, "Charge duration transfer must match the soft cap");
    }
}
