package jp.aquafactory.apprenticecodex.item.curios.isekaitravelguidebook;

import io.redspace.ironsspellbooks.api.registry.SpellDataRegistryHolder;
import io.redspace.ironsspellbooks.item.UniqueSpellBook;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class IsekaiTravelGuidebook extends UniqueSpellBook {
    public IsekaiTravelGuidebook() {
        super(new SpellDataRegistryHolder[] {
                new SpellDataRegistryHolder(SpellRegistry.HEALING_BLOOM, 1),
                new SpellDataRegistryHolder(SpellRegistry.COMPANION_TRUNK, 1)
        });
    }

    @Override
    public boolean isFireResistant() {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext context, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        if (!IsekaiTravelGuidebookTooltipState.shouldShowTooltip()) {
            return;
        }

        lines.add(Component.empty());
        lines.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.YELLOW));
    }
}
