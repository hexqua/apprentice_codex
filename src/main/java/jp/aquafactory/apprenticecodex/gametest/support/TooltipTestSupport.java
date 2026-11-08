package jp.aquafactory.apprenticecodex.gametest.support;

import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

public final class TooltipTestSupport {
    private TooltipTestSupport() {}

    public static void assertTranslatableKey(GameTestHelper helper, Component component, String expectedKey, String message) {
        var contents = component.getContents();
        helper.assertTrue(contents instanceof TranslatableContents,
                message + " (component was not translatable: " + component + ")");
        if (contents instanceof TranslatableContents translatableContents) {
            helper.assertTrue(expectedKey.equals(translatableContents.getKey()),
                    message + " (expected=" + expectedKey + ", actual=" + translatableContents.getKey() + ")");
        }
    }

    public static void assertTooltipKeyAt(
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

    public static void assertTooltipKeyUsesColor(
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

    public static void assertTooltipKeyArgumentUsesColor(
            GameTestHelper helper,
            ItemStack stack,
            String expectedKey,
            int argumentIndex,
            @Nullable TextColor expectedColor,
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
        if (matchingLine.isEmpty()) {
            return;
        }

        var contents = (TranslatableContents) matchingLine.get().getContents();
        var args = contents.getArgs();
        helper.assertTrue(args.length > argumentIndex,
                message + " (argument count=" + args.length + ")");
        if (args.length <= argumentIndex) {
            return;
        }

        helper.assertTrue(args[argumentIndex] instanceof Component,
                message + " (argument was not a component: " + args[argumentIndex] + ")");
        if (args[argumentIndex] instanceof Component component) {
            helper.assertTrue(Objects.equals(expectedColor, component.getStyle().getColor()),
                    message + " (expected=" + expectedColor + ", actual="
                            + component.getStyle().getColor() + ")");
        }
    }

    public static void assertTooltipKeyArgumentUsesTranslationKey(
            GameTestHelper helper,
            ItemStack stack,
            String expectedKey,
            int argumentIndex,
            String expectedArgumentKey,
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
        if (matchingLine.isEmpty()) {
            return;
        }

        var contents = (TranslatableContents) matchingLine.get().getContents();
        var args = contents.getArgs();
        helper.assertTrue(args.length > argumentIndex,
                message + " (argument count=" + args.length + ")");
        if (args.length <= argumentIndex) {
            return;
        }

        helper.assertTrue(args[argumentIndex] instanceof Component component
                        && component.getContents() instanceof TranslatableContents argumentContents
                        && expectedArgumentKey.equals(argumentContents.getKey()),
                message + " (unexpected argument=" + args[argumentIndex] + ")");
    }

    public static void assertTooltipKeyAbsent(GameTestHelper helper, ItemStack stack, String key, String message) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
        var present = tooltipLines.stream()
                .anyMatch(component -> component.getContents() instanceof TranslatableContents contents
                        && key.equals(contents.getKey()));
        helper.assertFalse(present, message + " (unexpected tooltip key=" + key + ")");
    }

    public static void assertTooltipTranslationKey(
            GameTestHelper helper,
            ItemStack stack,
            String translationKey,
            boolean expected,
            String message
    ) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);
        var hasKey = tooltipLines.stream()
                .anyMatch(component -> component.getContents() instanceof TranslatableContents translatableContents
                        && translationKey.equals(translatableContents.getKey()));
        helper.assertTrue(hasKey == expected, message);
    }
}
