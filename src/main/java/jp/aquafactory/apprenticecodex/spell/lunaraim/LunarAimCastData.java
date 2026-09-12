package jp.aquafactory.apprenticecodex.spell.lunaraim;

import io.redspace.ironsspellbooks.api.spells.ICastData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

// nullも選出済みとして保持し、発射時に別の対象を選び直さない。
public record LunarAimCastData(@Nullable UUID targetId, ResourceKey<Level> dimension) implements ICastData {
    @Override public void reset() {}
}
