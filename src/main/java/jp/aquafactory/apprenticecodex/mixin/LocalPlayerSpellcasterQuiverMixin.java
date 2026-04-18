package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiver;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerSpellcasterQuiverMixin {

    @ModifyConstant(method = "aiStep", constant = @Constant(floatValue = 0.2F))
    private float apprenticecodex$ignoreBowDrawSlowdown(float value) {
        if (!SpellcasterQuiver.shouldIgnoreBowSlowdown((LocalPlayer) (Object) this)) {
            return value;
        }

        // バニラ弓系の入力減衰だけを打ち消し、他の移動補正には触れない。
        return 1.0F;
    }
}
