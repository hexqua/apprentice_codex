package jp.aquafactory.apprenticecodex.spell.mistform;

import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public final class MistFormMovementRestrictionHelper {
    private MistFormMovementRestrictionHelper() {
    }

    public static boolean ignoresMovementRestriction(Entity entity, BlockState state) {
        return entity instanceof Player player
                && player.hasEffect(EffectRegistry.MIST_FORM.get())
                && state.is(TagRegistry.Blocks.MIST_FORM_IGNORES_MOVEMENT_RESTRICTION);
    }
}
