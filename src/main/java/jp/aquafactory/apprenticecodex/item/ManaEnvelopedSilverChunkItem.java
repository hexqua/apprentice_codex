package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import net.minecraft.world.item.Item;

public class ManaEnvelopedSilverChunkItem extends Item implements IJeiInfoItem {
    public ManaEnvelopedSilverChunkItem() {
        super(new Item.Properties());
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return "jei.apprenticecodex.mana_enveloped_silver_chunk.desc_";
    }
}
