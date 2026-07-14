package jp.aquafactory.apprenticecodex.item.blockitem;

import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class SpellCalibrationBenchItem extends BlockItem implements IJeiInfoItem {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.spell_calibration_bench.desc_";

    public SpellCalibrationBenchItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }
}
