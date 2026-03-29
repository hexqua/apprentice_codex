package jp.aquafactory.apprenticecodex.spell;

import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface IClientPlacementPreviewSpell extends IClientBlockTargetingSpell {
    Optional<ClientPlacementPreviewData> getClientPlacementPreview(Level level, LivingEntity entity, int spellLevel, @Nullable BlockTargetData targetData);
}
