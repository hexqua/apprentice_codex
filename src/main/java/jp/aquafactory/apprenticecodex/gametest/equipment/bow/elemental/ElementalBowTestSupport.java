package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.support.BowTestScope;
import jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowPendingCast;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class ElementalBowTestSupport {
    private ElementalBowTestSupport() {}
    static void assertElementalBowSelection(
            GameTestHelper helper,
            ItemStack stack,
            @Nullable String expectedShotMode,
            @Nullable ResourceLocation expectedSelectionId,
            String message
    ) {
        var tag = getCustomDataTag(stack);
        var actualShotMode = tag != null && tag.contains("ElementalBowShotMode")
                ? tag.getString("ElementalBowShotMode")
                : null;
        ResourceLocation actualSelectionId = null;
        if (tag != null) {
            if ("magic".equals(actualShotMode) && tag.contains("ElementalBowMode")) {
                actualSelectionId = ResourceLocation.tryParse(tag.getString("ElementalBowMode"));
            } else if (tag.contains("ElementalBowAmmoSelection")) {
                actualSelectionId = ResourceLocation.tryParse(tag.getString("ElementalBowAmmoSelection"));
            } else if (actualShotMode == null && tag.contains("ElementalBowMode")) {
                actualSelectionId = ResourceLocation.tryParse(tag.getString("ElementalBowMode"));
            }
        }
        helper.assertTrue(
                Objects.equals(actualShotMode, expectedShotMode) && Objects.equals(actualSelectionId, "magic".equals(expectedShotMode) ? elementalBowSlotId(expectedSelectionId) : expectedSelectionId),
                message + ": expected shotMode=" + expectedShotMode + ", selection=" + expectedSelectionId
                        + " but got shotMode=" + actualShotMode + ", selection=" + actualSelectionId
        );
    }

    static String describeElementalBowSelectionView(ElementalBow.ModeSelectionView view) {
        return view.selection().selectionId() == null
                ? view.selection().shotMode()
                : view.selection().shotMode() + ":" + view.selection().selectionId();
    }

    static void assertTranslatableKey(
            GameTestHelper helper,
            Component component,
            String expectedKey,
            String message
    ) {
        helper.assertTrue(
                component.getContents() instanceof TranslatableContents contents
                        && expectedKey.equals(contents.getKey()),
                message + ": expected " + expectedKey + " but got " + component
        );
    }

    static void assertTooltipKeyAt(
            GameTestHelper helper,
            ItemStack stack,
            int index,
            String expectedKey,
            String message
    ) {
        var tooltipLines = new ArrayList<Component>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltipLines, TooltipFlag.Default.NORMAL);
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
        stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltipLines, TooltipFlag.Default.NORMAL);
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
        stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), tooltipLines, TooltipFlag.Default.NORMAL);
        var present = tooltipLines.stream()
                .anyMatch(component -> component.getContents() instanceof TranslatableContents contents
                        && key.equals(contents.getKey()));
        helper.assertFalse(present, message + " (unexpected tooltip key=" + key + ")");
    }

    static ResourceLocation elementalBowSlotId(ResourceLocation school) {
        if (school == null) return null;
        return switch (school.toString()) {
            case "irons_spellbooks:fire" -> ElementalBow.selectionIdForSlot(0);
            case "irons_spellbooks:ender" -> ElementalBow.selectionIdForSlot(1);
            case "irons_spellbooks:nature" -> ElementalBow.selectionIdForSlot(2);
            default -> school;
        };
    }

    static void prepareElementalBowScrolls(ItemStack stack) {
        var lookup = ElementalBow.serializationLookup();
        var item = (ElementalBow) stack.getItem();
        var upgrade = new ItemStack(ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get());
        item.trySetCalibrationAdjustment(stack, 0, upgrade, lookup);
        item.trySetCalibrationAdjustment(stack, 1, upgrade, lookup);
        var spells = List.of(SpellRegistry.FIRE_ARROW_SPELL.get(),
                SpellRegistry.MAGIC_ARROW_SPELL.get(),
                SpellRegistry.POISON_ARROW_SPELL.get());
        for (int i = 0; i < spells.size(); i++) {
            if (ElementalBow.getCalibrationScroll(stack, i, lookup).isEmpty()) {
                var scroll = createSpellScroll(spells.get(i));
                ElementalBow.setCalibrationScroll(stack, i, scroll, lookup);
            }
        }
    }
    static FakePlayer createEquipmentTestPlayer(GameTestHelper helper, BlockPos pos, String profile) {
        return track(helper, PlayerTestSupport.createEquipmentTestPlayer(helper, pos, profile));
    }
    static FakePlayer track(GameTestHelper helper, FakePlayer player) {
        var scope = BowTestScope.of(helper);
        scope.track(player);
        scope.assertInside(player);
        scope.onClose(() -> {
            ElementalBowPendingCast.cancel(player);
            ElementalBowOverheatManager.clear(player);
            player.stopUsingItem();
            var magic = MagicData.getPlayerMagicData(player);
            if (magic != null) { magic.resetCastingState(); magic.resetAdditionalCastData(); }
        });
        return player;
    }
    static ItemStack createSpellScroll(AbstractSpell spell) { return BowGameTestSupport.createSpellScroll(spell); }
    static void setElementalBowShotSelection(ItemStack stack, String mode, ResourceLocation id) {
        BowGameTestSupport.setElementalBowShotSelection(stack, mode, id);
    }
    static void setElementalBowMode(ItemStack stack, String mode) { BowGameTestSupport.setElementalBowMode(stack, mode); }
    static ElementalBow.ModeSelectionView findElementalBowSelectionView(ServerPlayer player, ItemStack stack,
            String mode, ResourceLocation id) { return BowGameTestSupport.findElementalBowSelectionView(player, stack, mode, id); }
    static CompoundTag getCustomDataTag(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }
    static ApprenticeCodexServerConfig.GameTestConfigOverride useElementalBowSpellConfig(GameTestHelper helper) {
        return BowGameTestSupport.useElementalBowSpellConfig(helper);
    }
    static ApprenticeCodexServerConfig.GameTestConfigOverride useElementalBowConfigOverrideForGameTest(
            double draw, double linear, double quadratic, double duration, int minimum, int cap) {
        return BowGameTestSupport.useElementalBowConfigOverrideForGameTest(draw, linear, quadratic, duration, minimum, cap);
    }

    static void equipCurio(FakePlayer player, String slot, ItemStack stack) {
        BowGameTestSupport.equipCurio(player, slot, stack);
    }
}
