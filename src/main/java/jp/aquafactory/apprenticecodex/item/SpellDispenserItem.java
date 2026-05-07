package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class SpellDispenserItem extends BlockItem implements IJeiInfoItem {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.spell_dispenser.desc_";

    public SpellDispenserItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }
}
