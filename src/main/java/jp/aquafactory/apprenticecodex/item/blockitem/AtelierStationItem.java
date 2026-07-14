package jp.aquafactory.apprenticecodex.item.blockitem;

import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public final class AtelierStationItem extends BlockItem implements IJeiInfoItem {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.atelier_station.desc_";

    public AtelierStationItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }
}
