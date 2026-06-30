package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface RightClickSpellSourceItem {
    @NotNull SpellData getRightClickSpellData(@NotNull ItemStack stack, @NotNull Player player, @NotNull InteractionHand hand);
}
