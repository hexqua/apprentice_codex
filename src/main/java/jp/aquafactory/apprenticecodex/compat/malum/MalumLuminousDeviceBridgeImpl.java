package jp.aquafactory.apprenticecodex.compat.malum;

import com.sammy.malum.common.item.ether.AbstractEtherItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

final class MalumLuminousDeviceBridgeImpl {
    private static final String DISPLAY_TAG = "display";
    private static final int RGB_MASK = 0x00FFFFFF;

    private MalumLuminousDeviceBridgeImpl() {
    }

    static boolean isSupportedEther(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && stack.getItem() instanceof AbstractEtherItem;
    }

    static void normalizeForStorage(ItemStack stack) {
        if (!isSupportedEther(stack)) {
            return;
        }

        var etherItem = (AbstractEtherItem) stack.getItem();
        var displayTag = stack.getTagElement(DISPLAY_TAG);
        if (displayTag != null) {
            normalizeColor(
                    displayTag,
                    AbstractEtherItem.FIRST_COLOR,
                    AbstractEtherItem.DEFAULT_FIRST_COLOR
            );
            if (etherItem.iridescent) {
                normalizeColor(
                        displayTag,
                        AbstractEtherItem.SECOND_COLOR,
                        AbstractEtherItem.DEFAULT_SECOND_COLOR
                );
            } else {
                // 非IridescentではsecondColorを参照しないため、同じ見た目のスタックが分裂しないよう除去する。
                displayTag.remove(AbstractEtherItem.SECOND_COLOR);
            }
        }
        removeEmptyTags(stack);

        // 1.21.1版Malumは色データのコンポーネント構造などが異なる。
        // forward-port時はこのNBTパスや既定値を流用せず、対象バージョンの設置・破壊経路から再調査すること。
    }

    static boolean isSameEtherIgnoringColor(ItemStack first, ItemStack second) {
        if (!isSupportedEther(first)
                || !isSupportedEther(second)
                || first.getItem() != second.getItem()) {
            return false;
        }

        var normalizedFirst = first.copyWithCount(1);
        var normalizedSecond = second.copyWithCount(1);
        removeColors(normalizedFirst);
        removeColors(normalizedSecond);
        return ItemStack.isSameItemSameTags(normalizedFirst, normalizedSecond);
    }

    private static void normalizeColor(CompoundTag displayTag, String key, int defaultColor) {
        if (!displayTag.contains(key, Tag.TAG_ANY_NUMERIC)) {
            // Malumも数値以外は未指定として扱うため、意味を持たない表現揺れを残さない。
            displayTag.remove(key);
            return;
        }

        var rgb = displayTag.getInt(key) & RGB_MASK;
        if (rgb == (defaultColor & RGB_MASK)) {
            displayTag.remove(key);
        } else {
            // java.awt.Color(int)は上位8bitを参照しないため、ARGB/RGBの表現差を24bitへ統一する。
            displayTag.putInt(key, rgb);
        }
    }

    private static void removeColors(ItemStack stack) {
        var displayTag = stack.getTagElement(DISPLAY_TAG);
        if (displayTag != null) {
            displayTag.remove(AbstractEtherItem.FIRST_COLOR);
            displayTag.remove(AbstractEtherItem.SECOND_COLOR);
        }
        removeEmptyTags(stack);
    }

    private static void removeEmptyTags(ItemStack stack) {
        var displayTag = stack.getTagElement(DISPLAY_TAG);
        if (displayTag != null && displayTag.isEmpty()) {
            stack.removeTagKey(DISPLAY_TAG);
        }
        if (stack.getTag() != null && stack.getTag().isEmpty()) {
            stack.setTag(null);
        }
    }
}
