package jp.aquafactory.apprenticecodex.gametest.equipment.scythe;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.collectAllowedEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.MALUM_MOD_ID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ScytheEquipmentContractGameTests {
    private ScytheEquipmentContractGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellReaperScytheKeepsExpectedStatsTagsEnchantmentsAndImbueContract(GameTestHelper helper) {
        ScytheEquipmentContractScenarios.spellReaperScytheKeepsExpectedStatsTagsEnchantmentsAndImbueContract(helper);
    }

    static Set<ResourceLocation> expectedSpellReaperScytheEnchantments(ItemStack stack) {
        var result = new LinkedHashSet<>(collectAllowedEnchantments(e -> e.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD))));
        result.add(ForgeRegistries.ENCHANTMENTS.getKey(EnchantmentRegistry.WISDOM.get()));
        result.add(ForgeRegistries.ENCHANTMENTS.getKey(EnchantmentRegistry.TRANSCENDENCE.get()));
        if (ModList.get().isLoaded(MALUM_MOD_ID)) {
            for (var name : List.of("spirit_plunder", "haunted", "animated", "ascension", "rebound")) result.add(ResourceLocation.fromNamespaceAndPath("malum", name));
        }
        return result;
    }
}
