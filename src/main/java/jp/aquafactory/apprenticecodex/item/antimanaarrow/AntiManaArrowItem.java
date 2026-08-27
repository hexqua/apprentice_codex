package jp.aquafactory.apprenticecodex.item.antimanaarrow;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class AntiManaArrowItem extends ArrowItem {
    public AntiManaArrowItem() {
        super(new Item.Properties());
    }

    @Override
    public @NotNull AbstractArrow createArrow(@NotNull Level level, ItemStack ammo, @NotNull LivingEntity shooter) {
        return new AntiManaArrowEntity(level, shooter);
    }

    @Override
    public boolean isInfinite(@NotNull ItemStack ammo, @NotNull ItemStack bow, @NotNull Player player) {
        return false;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable("item.apprenticecodex.anti_mana_arrow.desc").withStyle(ChatFormatting.GRAY));
    }
}
