package jp.aquafactory.apprenticecodex.spell.mistform;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;

public final class MistFormCollisionHelper {
    private MistFormCollisionHelper() {
    }

    public static boolean canPassThrough(BlockState state, CollisionContext context) {
        if (!(context instanceof EntityCollisionContext entityContext)
                || !(entityContext.getEntity() instanceof Player player)) {
            return false;
        }

        return player.hasEffect(EffectRegistry.MIST_FORM)
                && state.is(TagRegistry.Blocks.MIST_FORM_PASSABLE)
                && !ApprenticeCodexServerConfig.isMistFormPassableBlockDenied(state);
    }
}
