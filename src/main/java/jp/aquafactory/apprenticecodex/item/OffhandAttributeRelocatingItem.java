package jp.aquafactory.apprenticecodex.item;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** 調整状態に応じて mainhand 用 Attribute 一式を offhand へ移すアイテム。 */
public interface OffhandAttributeRelocatingItem {
    boolean usesOffhandAttributeModifiers(@NotNull ItemStack stack);
}
