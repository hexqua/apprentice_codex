package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

public final class ItemTransformPreservingCastAnimations {
    private static final AnimationHolder CONTINUOUS_THRUST_ONE_HANDED_PRESERVE_ITEM =
            new AnimationHolder(
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "continuous_thrust_one_handed_preserve_item"),
                    false
            );
    // PlayerAnimator の rightItem 変形だけを避けたい詠唱モーションを、同等の腕モーションへ差し替える。
    private static final Map<ResourceLocation, AnimationHolder> REPLACEMENTS = Map.of(
            requiredPlayerAnimation(SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED),
            CONTINUOUS_THRUST_ONE_HANDED_PRESERVE_ITEM
    );

    private ItemTransformPreservingCastAnimations() {
    }

    public static Optional<AnimationHolder> resolve(AnimationHolder animation) {
        return animation.getForPlayer()
                .map(REPLACEMENTS::get);
    }

    private static ResourceLocation requiredPlayerAnimation(AnimationHolder animation) {
        return animation.getForPlayer().orElseThrow();
    }
}
