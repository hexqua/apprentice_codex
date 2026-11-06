package jp.aquafactory.apprenticecodex.gametest.equipment.bow;

import java.util.Objects;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import static jp.aquafactory.apprenticecodex.gametest.support.InventoryTestSupport.getCustomDataTag;

final class ElementalBowSelectionTestSupport {
    private ElementalBowSelectionTestSupport() {}

    static void assertElementalBowMode(GameTestHelper helper, ItemStack stack, String expectedMode, String message) {
        var tag = getCustomDataTag(stack);
        var actualMode = tag != null && tag.contains("ElementalBowMode") ? tag.getString("ElementalBowMode") : null;
        helper.assertTrue(
                Objects.equals(actualMode, normalizeElementalBowModeId(expectedMode)),
                message + ": expected " + expectedMode + " but got " + actualMode
        );
    }

    static void setElementalBowMode(ItemStack stack, String mode) {
        BowGameTestSupport.setElementalBowMode(stack, mode);
    }

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
                Objects.equals(actualShotMode, expectedShotMode) && Objects.equals(actualSelectionId, expectedSelectionId),
                message + ": expected shotMode=" + expectedShotMode + ", selection=" + expectedSelectionId
                        + " but got shotMode=" + actualShotMode + ", selection=" + actualSelectionId
        );
    }

    static String describeElementalBowSelectionView(ElementalBow.ModeSelectionView view) {
        return view.selection().selectionId() == null
                ? view.selection().shotMode()
                : view.selection().shotMode() + ":" + view.selection().selectionId();
    }

    @Nullable
    public static ElementalBow.ModeSelectionView findElementalBowSelectionView(
            ServerPlayer player,
            ItemStack stack,
            String shotMode,
            @Nullable ResourceLocation selectionId
    ) {
        return ElementalBow.getAvailableSelectionViews(player, stack).stream()
                .filter(view -> shotMode.equals(view.selection().shotMode())
                        && Objects.equals(selectionId, view.selection().selectionId()))
                .findFirst()
                .orElse(null);
    }

    static void setElementalBowShotSelection(ItemStack stack, String shotMode, @Nullable ResourceLocation selectionId) {
        BowGameTestSupport.setElementalBowShotSelection(stack, shotMode, selectionId);
    }

    static @Nullable String normalizeElementalBowModeId(@Nullable String mode) {
        if (mode == null || mode.isBlank()) {
            return mode;
        }
        return mode.contains(":")
                ? mode
                : ResourceLocation.fromNamespaceAndPath("irons_spellbooks", mode).toString();
    }
}
