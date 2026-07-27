package jp.aquafactory.apprenticecodex.item.apprenticedesk;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class PartiallyUsedInkItem extends Item {
    public PartiallyUsedInkItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        var source = PartiallyUsedInkState.readSourceOnly(stack).orElse(null);
        if (source == null) {
            return Component.translatable("item.apprenticecodex.partially_used_ink.unknown");
        }
        return Component.translatable(
                "item.apprenticecodex.partially_used_ink.named",
                source.item().getDescription()
        );
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            @Nullable Level level,
            @NotNull List<Component> lines,
            @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable("item.apprenticecodex.partially_used_ink.desc")
                .withStyle(ChatFormatting.GRAY));

        var state = PartiallyUsedInkState.readValid(stack).orElse(null);
        if (state == null) {
            lines.add(Component.translatable("item.apprenticecodex.partially_used_ink.remaining_unknown")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        lines.add(Component.translatable(
                "item.apprenticecodex.partially_used_ink.remaining",
                state.remainingUses(),
                state.capacity()
        ).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return PartiallyUsedInkState.readValid(stack).isPresent();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        var state = PartiallyUsedInkState.readValid(stack).orElse(null);
        if (state == null) {
            return 0;
        }
        return Mth.clamp(Math.round(13.0F * state.remainingUses() / state.capacity()), 1, 13);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        var state = PartiallyUsedInkState.readValid(stack).orElse(null);
        if (state == null) {
            return 0;
        }
        var ratio = (float) state.remainingUses() / state.capacity();
        return Mth.hsvToRgb(Math.max(0.0F, ratio) / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public int getEnchantmentValue(@NotNull ItemStack stack) {
        return 0;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack stack, @NotNull ItemStack repairCandidate) {
        return false;
    }
}
