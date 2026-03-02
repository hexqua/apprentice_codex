package jp.aquafactory.apprenticecodex.compat.jei;

import org.jetbrains.annotations.Nullable;

/**
 * JEI の説明ページを明示的に登録したいアイテムに付与するマーカー.
 */
public interface IJeiInfoItem {
    /**
     * 説明文の翻訳キー接頭辞.
     * 例: "item.apprenticecodex.example_item.jei.desc_"
     */
    String getJeiInfoTranslationKeyPrefix();

    /**
     * 同じ説明を共有したいアイテム群の識別子.
     * null または空文字の場合は単体表示として扱う.
     */
    @Nullable
    default String getJeiInfoGroupId() {
        return null;
    }
}
