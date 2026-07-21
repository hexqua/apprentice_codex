package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class SpellExtractShard extends Item implements IJeiInfoItem {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.spell_extract_shard.desc_";
    public SpellExtractShard() {
        super(new Properties());
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            @Nullable Level level,
            @NotNull List<Component> lines,
            @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable(getDescriptionId() + ".desc_1").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(getDescriptionId() + ".desc_2").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }
}
