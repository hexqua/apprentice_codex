package jp.aquafactory.apprenticecodex.gametest.enchantment.acquisition;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.ApprenticeEnchantmentAvailability;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashSet;

import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.describeEnchantmentDifference;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.expectedRandomBookLootEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.loot.LootTestSupport.createEmptyLootContext;

final class EnchantmentAcquisitionScenarios {
    private EnchantmentAcquisitionScenarios() {}

    static void randomApplicableBookEnchantmentsExcludeFlaskEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            // 1.21.1申し送り事項:
            // 本棚由来の抽選可否は enchanting table 可否だけでは追えず、book/anvil 側の定義差分でも崩れる。
            // Flask 系除外は「今も本から引けないか」を seed 探索込みで見直し、そのまま移植前提にしない。
            var function = EnchantRandomlyFunction.randomApplicableEnchantment().build();
            var seenApprenticeEnchantments = new LinkedHashSet<ResourceLocation>();

            for (long seed = 0L; seed < 4096L; ++seed) {
                var result = function.apply(new ItemStack(Items.BOOK), createEmptyLootContext(helper, seed));
                var enchantments = EnchantmentHelper.getEnchantments(result);
                helper.assertTrue(result.is(Items.ENCHANTED_BOOK),
                        "Random applicable enchantment loot should convert books into enchanted books");
                helper.assertTrue(enchantments.size() == 1,
                        "Random applicable enchantment loot should apply exactly one enchantment: " + enchantments);

                for (var enchantment : enchantments.keySet()) {
                    var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
                    if (enchantmentId == null || !ApprenticeCodex.MODID.equals(enchantmentId.getNamespace())) {
                        continue;
                    }

                    helper.assertFalse(ApprenticeEnchantmentAvailability.isExcludedFromRandomBookLoot(enchantment),
                            "Random applicable enchantment loot included excluded enchantment: " + enchantmentId + " at seed " + seed);
                    seenApprenticeEnchantments.add(enchantmentId);
                }
            }

            var expectedEnchantments = expectedRandomBookLootEnchantments();
            helper.assertTrue(seenApprenticeEnchantments.containsAll(expectedEnchantments),
                    "Random applicable enchantment loot lost apprentice enchantments: "
                            + describeEnchantmentDifference(expectedEnchantments, seenApprenticeEnchantments));
        });
    }
}
