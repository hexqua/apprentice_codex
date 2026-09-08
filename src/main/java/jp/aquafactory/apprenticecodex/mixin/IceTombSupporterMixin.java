package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.ironsspellbooks.entity.spells.ice_tomb.IceTombEntity;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.IceTombShatter;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.SupportedIceTomb;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IceTombEntity.class, remap = false)
public abstract class IceTombSupporterMixin implements SupportedIceTomb {
    @Unique private int apprenticecodex$spellLevel;
    @Unique private boolean apprenticecodex$shatterRelease;
    @Unique private boolean apprenticecodex$releaseConsumed;

    @Override
    public void apprenticecodex$setSpellLevel(int level) {
        apprenticecodex$spellLevel = level;
    }

    @Override
    public void apprenticecodex$withShatterRelease(Runnable release) {
        boolean previous = apprenticecodex$shatterRelease;
        apprenticecodex$shatterRelease = true;
        try {
            release.run();
        } finally {
            apprenticecodex$shatterRelease = previous;
        }
    }

    @WrapOperation(method = {"tick", "die"}, at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/entity/spells/ice_tomb/IceTombEntity;destroyTomb()V"))
    private void allowNaturalOrDamageRelease(IceTombEntity tomb, Operation<Void> original) {
        apprenticecodex$withShatterRelease(() -> original.call(tomb));
    }

    @WrapMethod(method = "destroyTomb")
    private void shatterAfterRelease(Operation<Void> original) {
        var tomb = (IceTombEntity) (Object) this;
        Runnable burst = () -> {};
        if (!apprenticecodex$releaseConsumed) {
            // ejectPassengersからdestroyTombへ再入するため、元処理より先に確定する。
            apprenticecodex$releaseConsumed = true;
            if (apprenticecodex$shatterRelease && !tomb.isRemoved()) {
                burst = IceTombShatter.prepare(tomb, apprenticecodex$spellLevel);
            }
        }
        original.call();
        burst.run();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveSupport(CompoundTag tag, CallbackInfo ci) {
        if (apprenticecodex$spellLevel > 0) {
            tag.putInt("apprenticecodex:ice_tomb_level", apprenticecodex$spellLevel);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadSupport(CompoundTag tag, CallbackInfo ci) {
        apprenticecodex$spellLevel = tag.getInt("apprenticecodex:ice_tomb_level");
    }
}
