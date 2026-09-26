package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumStaffChargeBridge;
import jp.aquafactory.apprenticecodex.gametest.malum.MalumGameTestHooks;
import jp.aquafactory.apprenticecodex.item.curios.manasoultransducer.ManaSoulTransducerLogic;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.HiddenFromEldritchResearch;
import jp.aquafactory.apprenticecodex.utility.AdvancementTools;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import net.minecraftforge.registries.ForgeRegistries;
import java.util.List;
import java.util.Map;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ManaSoulTransducerGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private ManaSoulTransducerGameTests() {}

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.mana_soul_transducer")
    public static void blackCrystalManualAdvancementLoadsWithoutDisplay(GameTestHelper h) {
        var advancement = h.getLevel().getServer().getAdvancements().getAdvancement(
                AdvancementTools.MALUM_BLACK_CRYSTAL_REVEALED);
        h.assertTrue(advancement != null, "Missing Black Crystal manual advancement");
        h.assertTrue(advancement.getDisplay() == null,
                "Black Crystal manual advancement must not appear in the advancement UI");
        h.assertTrue(advancement.getCriteria().containsKey(
                        AdvancementTools.MALUM_BLACK_CRYSTAL_REVEALED_CRITERION),
                "Black Crystal manual advancement must contain the criterion awarded by the packet");
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.mana_soul_transducer")
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
    @GameTest(template = TEMPLATE, batch = "apprenticecodex.mana_soul_transducer")
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
    @GameTest(template = TEMPLATE, batch = "apprenticecodex.mana_soul_transducer")
    public static void legacyStaffPayments(GameTestHelper h) {
        if (MalumStaffChargeBridge.isAvailable()) ManaSoulTransducerMalumScenarios.payments(h);
        h.succeed();
    }
    @GameTest(template = TEMPLATE, batch = "apprenticecodex.mana_soul_transducer")
    public static void researchMarkersAndRecipe(GameTestHelper h) {
        h.assertTrue(SpellRegistry.ECHO_CAST.get() instanceof HiddenFromEldritchResearch,
                "Echo Cast must be hidden from research");
        h.assertTrue(SpellRegistry.PALETTE_SHIFT.get() instanceof HiddenFromEldritchResearch,
                "Palette Shift must be hidden from research");
        h.assertTrue(SpellRegistry.SOUL_CONVERSION.get() instanceof HiddenFromEldritchResearch,
                "Soul Conversion must be hidden from research");
        var recipeId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID,
                "malum/spirit_infusion/mana_soul_transducer");
        if (MalumStaffChargeBridge.isAvailable()) {
            MalumGameTestHooks.assertSpiritInfusionRecipe(
                    h.getLevel(), recipeId, malumItem("runic_brooch", 1),
                    List.of(malumItem("fused_consciousness", 1), malumItem("mnemonic_fragment", 8),
                            malumItem("malignant_pewter_plating", 4)),
                    new ItemStack(ItemRegistry.MANA_SOUL_TRANSDUCER.get()),
                    Map.of("aerial_spirit", 16, "aqueous_spirit", 16, "arcane_spirit", 64, "eldritch_spirit", 64));
        } else {
            h.assertFalse(h.getLevel().getRecipeManager().byKey(recipeId).isPresent(), "Malum recipe must be conditional");
        }
        h.succeed();
    }
    private static ItemStack malumItem(String path, int count) {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(
                ResourceLocation.fromNamespaceAndPath("malum", path)), count);
    }
    private static void close(GameTestHelper h, double actual, double expected) {
        h.assertTrue(Math.abs(actual - expected) < 1e-6, "Charge duration transfer must match the soft cap");
    }
}
