package jp.aquafactory.apprenticecodex.gametest.enchantment.acquisition;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.assertApprenticeEnchantmentFlags;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.expectedRandomBookLootEnchantments;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EnchantmentAcquisitionGameTests {
    private EnchantmentAcquisitionGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void apprenticeEnchantmentAcquisitionFlagsKeepExpectedValues(GameTestHelper helper) {
        for (var enchantment : Set.of(
                Enchantments.ALACRITY, Enchantments.REFLUX, Enchantments.RESERVOIR,
                Enchantments.SURGE, Enchantments.ATTUNEMENT, Enchantments.TENSE,
                Enchantments.WISDOM, Enchantments.PLUNDER
        )) {
            assertApprenticeEnchantmentFlags(helper, enchantment, false, true, true, true);
        }
        assertApprenticeEnchantmentFlags(helper, Enchantments.TRANSCENDENCE, true, false, true, true);
        for (var enchantment : Set.of(
                Enchantments.GUZZLE, Enchantments.LARGE_MUG, Enchantments.RED_ENERGY,
                Enchantments.GLOW_ENERGY, Enchantments.SYNTHESIS, Enchantments.SHELL,
                Enchantments.SYNCHRONIZATION, Enchantments.NEUTRALIZATION
        )) {
            assertApprenticeEnchantmentFlags(helper, enchantment, false, true, false, false);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void randomApplicableBookEnchantmentsExcludeFlaskEnchantments(GameTestHelper helper) {
        var registryAccess = helper.getLevel().registryAccess();
        var candidates = registryAccess.lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(EnchantmentTags.ON_RANDOM_LOOT).stream().toList();
        var apprenticeCandidates = candidates.stream().map(holder -> holder.unwrapKey().orElseThrow().location())
                .filter(id -> ApprenticeCodex.MODID.equals(id.getNamespace())).collect(Collectors.toSet());
        helper.assertTrue(apprenticeCandidates.equals(expectedRandomBookLootEnchantments()),
                "Random book loot must expose the intended apprentice enchantments: " + apprenticeCandidates);
        helper.assertFalse(candidates.isEmpty(), "Random book loot must have candidates");

        var function = EnchantRandomlyFunction.randomApplicableEnchantment(registryAccess).build();
        var params = new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY);
        for (int index = 0; index < candidates.size(); index++) {
            int selectedIndex = index;
            // 有限回のseed試行で全種の出現を期待せず、実際の候補集合の各要素を一度ずつ選ぶ。
            var selectionRandom = new LegacyRandomSource(0L) {
                private boolean selected;

                @Override
                public int nextInt(int bound) {
                    if (!selected) {
                        helper.assertTrue(bound == candidates.size(),
                                "Random book loot must select from the on_random_loot candidates");
                        selected = true;
                        return selectedIndex;
                    }
                    return 0;
                }
            };
            var context = new LootContext.Builder(params).withOptionalRandomSource(selectionRandom)
                    .create(Optional.empty());
            var result = function.apply(new ItemStack(Items.BOOK), context);
            var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(result);
            var expected = candidates.get(index);
            helper.assertTrue(result.is(Items.ENCHANTED_BOOK) && enchantments.size() == 1
                            && enchantments.getLevel(expected) > 0,
                    "Random book loot must apply the selected candidate: " + expected);
        }
        helper.succeed();
    }
}
