package jp.aquafactory.apprenticecodex.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class SpellcasterRoundItem extends Item {
    @Nullable
    private final Supplier<? extends Item> emptyCasingSupplier;

    public SpellcasterRoundItem() {
        this(null);
    }

    public SpellcasterRoundItem(@Nullable Supplier<? extends Item> emptyCasingSupplier) {
        super(new Item.Properties());
        this.emptyCasingSupplier = emptyCasingSupplier;
    }

    @Nullable
    public Item getEmptyCasingItem() {
        return emptyCasingSupplier == null ? null : emptyCasingSupplier.get();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext context, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
