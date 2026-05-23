package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpellDispenserItem extends BlockItem implements IJeiInfoItem {
    private static final String JEI_INFO_KEY_PREFIX_NORMAL = "jei.apprenticecodex.spell_dispenser.desc_";
    private static final String JEI_INFO_KEY_PREFIX_CREATIVE = "jei.apprenticecodex.creative_spell_dispenser.desc_";
    private static final String CREATIVE_TOOLTIP_KEY = "item.apprenticecodex.spell_dispenser.creative_tooltip";

    private final boolean showCreativeTooltip;

    public SpellDispenserItem(Block block, Properties properties) {
        this(block, properties, false);
    }

    public SpellDispenserItem(Block block, Properties properties, boolean showCreativeTooltip) {
        super(block, properties);
        this.showCreativeTooltip = showCreativeTooltip;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        if (showCreativeTooltip) {
            lines.add(Component.translatable(CREATIVE_TOOLTIP_KEY).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, level, lines, flag);
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return showCreativeTooltip ? JEI_INFO_KEY_PREFIX_CREATIVE : JEI_INFO_KEY_PREFIX_NORMAL;
    }
}
