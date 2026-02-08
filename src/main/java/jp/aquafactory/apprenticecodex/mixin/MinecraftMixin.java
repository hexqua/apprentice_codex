package jp.aquafactory.apprenticecodex.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static jp.aquafactory.apprenticecodex.client.ClientCastTargetHighlightHandler.getHighlightEntityId;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    // 発光エフェクト判定を外部から注入する.
    @Inject(method = "shouldEntityAppearGlowing", at = @At(value = "RETURN"), cancellable = true)
    private void shouldEntityAppearGlowing(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        // 元の値を汚染しないようにしておく.
        if (Minecraft.getInstance().player == null || entity == null || cir.getReturnValue()) {
            return;
        }

        if (entity.getId() == getHighlightEntityId()){
            cir.setReturnValue(true);
        }
    }
}
