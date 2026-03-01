package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GrimoireManifest extends Item implements IPresetSpellContainer {
    public GrimoireManifest() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, Item.TooltipContext context, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(itemStack, context, lines, flag);
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        // Datagen時はSpellRegistry未バインドのため、初期呪文の注入をスキップする.
        if (!SpellRegistry.MANIFESTATION_GRIMOIRE.isBound()) {
            return;
        }

        ISpellContainer.createImbuedContainer(SpellRegistry.MANIFESTATION_GRIMOIRE.get(), 1, itemStack);
    }
}
