package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.compat.create.CreateCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RoundIncompleteMaterialItem extends Item {
    private static final String NO_CREATE_TRANSLATION_KEY =
            "item.apprenticecodex.round_incomplete_material.desc.no_create";

    public RoundIncompleteMaterialItem() {
        super(new Item.Properties());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        if (!ModList.get().isLoaded(CreateCompat.MOD_ID)) {
            lines.add(Component.translatable(NO_CREATE_TRANSLATION_KEY).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
