package jp.aquafactory.apprenticecodex.item.swingstaff;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DiamondSwingcastStaff extends AbstractSwingcastStaffItem {
    private static final SwingcastStaffTier TIER = createTier(
            Rarity.UNCOMMON,
            10,
            6.0D,
            instantOnlyCastTypes(),
            SwingcastCooldownMode.IMBUED_ONLY,
            RecastTypes.NoRecastRestriction,
            bonus(AttributeRegistry.SPELL_POWER, 0.1, AttributeModifier.Operation.MULTIPLY_BASE));

    public DiamondSwingcastStaff() {
        super("diamond_swingcast_staff", TIER);
    }

    @Override
    protected void appendAdditionalSwingcastTooltip(
            ItemStack stack,
            @Nullable Level level,
            List<Component> lines,
            TooltipFlag flag
    ) {
        var cooldownReductionTicks = HighTierSwingcastStaffConfigState.diamondCooldownReductionTicks();
        if (cooldownReductionTicks > 0) {
            lines.add(Component.translatable(
                    "item.apprenticecodex.high_tier_swingcast_staff.cooldown_hint",
                    Utils.timeFromTicks(cooldownReductionTicks, 1)
            ).withStyle(ChatFormatting.GRAY));
        }
    }
}
