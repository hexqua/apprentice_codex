package jp.aquafactory.apprenticecodex.item.curios.isekaitravelguidebook;

import io.redspace.ironsspellbooks.api.registry.SpellDataRegistryHolder;
import io.redspace.ironsspellbooks.item.UniqueSpellBook;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        if (!IsekaiTravelGuidebookTooltipState.shouldShowTooltip()) {
            return;
        }

        lines.add(Component.empty());
        lines.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.YELLOW));
    }
}
