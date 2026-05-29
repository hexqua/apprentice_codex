package jp.aquafactory.apprenticecodex.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SortedMap;

@Mixin(RenderBuffers.class)
public abstract class RenderBuffersBoundSwordGlintMixin {
    @Shadow
    @Final
    private SortedMap<RenderType, BufferBuilder> fixedBuffers;

    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void apprentice_codex$addBoundSwordGlintBuffers(CallbackInfo ci) {
        this.fixedBuffers.put(
                ApprenticeRenderTypes.boundSpellWeaponGlint(),
                new BufferBuilder(ApprenticeRenderTypes.boundSpellWeaponGlint().bufferSize())
        );
        this.fixedBuffers.put(
                ApprenticeRenderTypes.boundSpellWeaponGlintDirect(),
                new BufferBuilder(ApprenticeRenderTypes.boundSpellWeaponGlintDirect().bufferSize())
        );
        ApprenticeRenderTypes.markBoundSpellWeaponGlintBuffersRegistered();
    }
}
