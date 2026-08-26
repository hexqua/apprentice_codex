package jp.aquafactory.apprenticecodex.item.antimanaarrow;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
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
    public @NotNull AbstractArrow createArrow(@NotNull Level level, ItemStack ammo, @NotNull LivingEntity shooter, @Nullable ItemStack weapon) {
        return new AntiManaArrowEntity(level, shooter, ammo.copyWithCount(1), weapon);
    }

    @Override
    public @NotNull Projectile asProjectile(@NotNull Level level, Position position, ItemStack stack, @NotNull Direction direction) {
        var arrow = new AntiManaArrowEntity(level, position.x(), position.y(), position.z(), stack.copyWithCount(1));
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        return arrow;
    }

    @Override
    public boolean isInfinite(@NotNull ItemStack ammo, @NotNull ItemStack bow, @NotNull LivingEntity livingEntity) {
        return false;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("item.apprenticecodex.anti_mana_arrow.desc").withStyle(ChatFormatting.GRAY));
    }
}
