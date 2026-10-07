package jp.aquafactory.apprenticecodex.item.blockitem;

import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import net.minecraft.world.level.block.Block;

public final class CreativeSpellDispenserItem extends SpellDispenserItem implements IJeiInfoItem {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.creative_spell_dispenser.desc_";

    public CreativeSpellDispenserItem(Block block, Properties properties) {
        super(block, properties, true);
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }
}
