package jp.aquafactory.apprenticecodex.block.apprenticedesk;

import jp.aquafactory.apprenticecodex.item.apprenticedesk.PartiallyUsedInkState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ApprenticeDeskInkTooltip {
    private ApprenticeDeskInkTooltip() {
    }

    public static @Nullable Component create(ItemStack stack) {
        var source = PartiallyUsedInkState.OfficialInk.fromOriginal(stack);
        if (source == null) {
            return null;
        }
        return Component.translatable(
                "container.apprenticecodex.apprentice_desk.ink_conversion_tooltip",
                ApprenticeDeskFeatureState.inkMaxUses(source.rarity())
        ).withStyle(ChatFormatting.GRAY);
    }
}
