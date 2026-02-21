package jp.aquafactory.apprenticecodex.spell;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ICastHighlightSpell {
    int getHighlightColor();
    @Nullable Entity getHighlightEntity(@NotNull Player player, int skillLevel);
}
