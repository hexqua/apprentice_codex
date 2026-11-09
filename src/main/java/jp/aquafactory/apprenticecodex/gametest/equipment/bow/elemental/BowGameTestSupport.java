package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.elementalBowSlotId;

final class BowGameTestSupport {
    private BowGameTestSupport() {}

    static void assertElementalBowSelection(
            GameTestHelper helper,
            ItemStack stack,
            @Nullable String expectedShotMode,
            @Nullable ResourceLocation expectedSelectionId,
            String message
    ) {
        var tag = stack.getTag();
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
}
