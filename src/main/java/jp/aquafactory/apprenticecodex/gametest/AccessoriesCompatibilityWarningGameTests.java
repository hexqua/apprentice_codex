package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.accessories.AccessoriesCompatibility;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Objects;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class AccessoriesCompatibilityWarningGameTests {
    private AccessoriesCompatibilityWarningGameTests() {
    }

    @GameTest(template = "gametest/basic_floor")
    public static void caseWarningPrecedesDescriptionsOnlyWithCompatibilityLayer(GameTestHelper helper) {
        var lines = new ArrayList<Component>();
        var stack = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), lines, TooltipFlag.NORMAL);
        boolean installed = ModList.get().isLoaded("accessories_compat_layer");
        helper.assertTrue(lines.size() == (installed ? 3 : 2), "Case warning must follow Compatibility Layer availability");
        if (installed) {
            assertKey(helper, lines.getFirst(), AccessoriesCompatibility.CASE_WARNING_KEY);
            helper.assertTrue(Objects.equals(TextColor.fromLegacyFormat(ChatFormatting.YELLOW), lines.getFirst().getStyle().getColor()),
                    "Case warning must be yellow");
        }
        int offset = installed ? 1 : 0;
        assertKey(helper, lines.get(offset), "item.apprenticecodex.spellcaster_accessory_case.desc_1");
        assertKey(helper, lines.get(offset + 1), "item.apprenticecodex.spellcaster_accessory_case.desc_2");
        helper.succeed();
    }

    private static void assertKey(GameTestHelper helper, Component line, String expected) {
        helper.assertTrue(line.getContents() instanceof TranslatableContents contents && expected.equals(contents.getKey()),
                "Unexpected case tooltip translation key: " + expected);
    }
}
