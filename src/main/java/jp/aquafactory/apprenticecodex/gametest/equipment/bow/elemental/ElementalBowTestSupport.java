package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.Objects;

public final class ElementalBowTestSupport {
    private ElementalBowTestSupport() {}

    public static void assertTranslatableKey(GameTestHelper helper, Component component, String expectedKey, String message) {
        var contents = component.getContents();
        helper.assertTrue(contents instanceof TranslatableContents,
                message + " (component was not translatable: " + component + ")");
        if (contents instanceof TranslatableContents translatableContents) {
            helper.assertTrue(expectedKey.equals(translatableContents.getKey()),
                    message + " (expected=" + expectedKey + ", actual=" + translatableContents.getKey() + ")");
        }
    }

    static void assertTooltipKeyAt(
            GameTestHelper helper,
            ItemStack stack,
            int index,
            String expectedKey,
            String message
    ) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
        helper.assertTrue(tooltipLines.size() > index,
                message + " (tooltip line count=" + tooltipLines.size() + ")");
        assertTranslatableKey(helper, tooltipLines.get(index), expectedKey, message);
    }

    static void assertTooltipKeyUsesColor(
            GameTestHelper helper,
            ItemStack stack,
            String expectedKey,
            ChatFormatting expectedColor,
            String message
    ) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
        var matchingLine = tooltipLines.stream()
                .filter(component -> component.getContents() instanceof TranslatableContents contents
                        && expectedKey.equals(contents.getKey()))
                .findFirst();
        helper.assertTrue(matchingLine.isPresent(),
                message + " (missing tooltip key=" + expectedKey + ")");
        if (matchingLine.isPresent()) {
            var expectedTextColor = TextColor.fromLegacyFormat(expectedColor);
            helper.assertTrue(Objects.equals(expectedTextColor, matchingLine.get().getStyle().getColor()),
                    message + " (expected=" + expectedTextColor + ", actual="
                            + matchingLine.get().getStyle().getColor() + ")");
        }
    }

    static void assertTooltipKeyAbsent(GameTestHelper helper, ItemStack stack, String key, String message) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
        var present = tooltipLines.stream()
                .anyMatch(component -> component.getContents() instanceof TranslatableContents contents
                        && key.equals(contents.getKey()));
        helper.assertFalse(present, message + " (unexpected tooltip key=" + key + ")");
    }
}
