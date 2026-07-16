package jp.aquafactory.apprenticecodex.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class SpellcasterRoundItem extends Item {
    private static final String COMMON_ROUND_DESCRIPTION_KEY =
            "item.apprenticecodex.common.round.desc";
    private static final String COMMON_EMPTY_CASING_DESCRIPTION_KEY =
            "item.apprenticecodex.common.empty_casing.desc";
    @Nullable
    private final Supplier<? extends Item> emptyCasingSupplier;
    private final String descriptionKey;

    public SpellcasterRoundItem() {
        this(null, COMMON_EMPTY_CASING_DESCRIPTION_KEY);
    }

    public SpellcasterRoundItem(@Nullable Supplier<? extends Item> emptyCasingSupplier) {
        this(emptyCasingSupplier, COMMON_ROUND_DESCRIPTION_KEY);
    }

    public SpellcasterRoundItem(@Nullable Supplier<? extends Item> emptyCasingSupplier, String descriptionKey) {
        super(new Item.Properties());
        this.emptyCasingSupplier = emptyCasingSupplier;
        this.descriptionKey = Objects.requireNonNull(descriptionKey);
    }

    @Nullable
    public Item getEmptyCasingItem() {
        return emptyCasingSupplier == null ? null : emptyCasingSupplier.get();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext context, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY));
    }
}
