package jp.aquafactory.apprenticecodex.item;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 術式調整台へ表示する候補ヘルプの意味情報。
 *
 * <p>Item 側へ client 専用の Component 生成を持ち込まず、表示方法は Screen 側へ委譲する。</p>
 */
public sealed interface CalibrationAdjustmentHint
        permits CalibrationAdjustmentHint.Translatable,
        CalibrationAdjustmentHint.SpecificItem,
        CalibrationAdjustmentHint.TaggedItems {

    static CalibrationAdjustmentHint translatable(String translationKey) {
        return new Translatable(translationKey);
    }

    static CalibrationAdjustmentHint specificItem(Supplier<? extends Item> item) {
        return new SpecificItem(item);
    }

    static CalibrationAdjustmentHint taggedItems(
            String headingTranslationKey,
            TagKey<Item> tag,
            Supplier<? extends Item> fallbackItem
    ) {
        return new TaggedItems(headingTranslationKey, tag, fallbackItem);
    }

    record Translatable(String translationKey) implements CalibrationAdjustmentHint {
        public Translatable {
            Objects.requireNonNull(translationKey);
        }
    }

    record SpecificItem(Supplier<? extends Item> item) implements CalibrationAdjustmentHint {
        public SpecificItem {
            Objects.requireNonNull(item);
        }
    }

    record TaggedItems(
            String headingTranslationKey,
            TagKey<Item> tag,
            Supplier<? extends Item> fallbackItem
    ) implements CalibrationAdjustmentHint {
        public TaggedItems {
            Objects.requireNonNull(headingTranslationKey);
            Objects.requireNonNull(tag);
            Objects.requireNonNull(fallbackItem);
        }
    }
}
