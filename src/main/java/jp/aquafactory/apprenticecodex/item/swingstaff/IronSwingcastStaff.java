package jp.aquafactory.apprenticecodex.item.swingstaff;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class IronSwingcastStaff extends AbstractSwingcastStaffItem {
    private static final SwingcastStaffTier TIER = createTier(
            Rarity.COMMON,
            14,
            5.0D,
            instantOnlyCastTypes(),
            SwingcastCooldownMode.IMBUED_ONLY,
            RecastTypes.RequireZeroRecast,
            bonus((Holder<Attribute>) AttributeRegistry.SPELL_POWER, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
    );

    public IronSwingcastStaff() {
        super("iron_swingcast_staff", TIER);
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            Item.@NotNull TooltipContext context,
            @NotNull List<Component> lines,
            @NotNull TooltipFlag flag
    ) {
        if (IronSwingcastStaffConfigState.crystallineArcaneShardDropChance() > 0.0D) {
            lines.add(Component.translatable(
                    "item.apprenticecodex.iron_swingcast_staff.crystallize_hint"
            ).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, lines, flag);
    }
}
