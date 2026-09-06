package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.sammy.malum.common.item.curiosities.curios.runes.miracle.RuneCullingItem", remap = false)
public abstract class MalumScytheCullingMixin {
    @WrapOperation(method = "outgoingDamageEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean apprenticecodex$normalThrow(DamageSource source, TagKey<DamageType> tag, Operation<Boolean> original) {
        // 通常投擲を大鎌タグへ登録するとGeasも発動するため、Cullingだけの対象拡張に留める。
        return original.call(source, tag) || source.is(DamageTypes.SPELL_REAPER_SCYTHE_THROW)
                || source.is(DamageTypes.SPELL_REAPER_SCYTHE_THROW_CONTINUOUS);
    }
}
