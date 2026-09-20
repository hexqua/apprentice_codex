package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.entity.spells.ice_tomb.IceTombEntity;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.IceTombShatter;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.SupportedIceTomb;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    // tickはMinecraft継承メソッドなので本番名へ変換し、Iron's独自のdestroyTombは変換しない。
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/entity/spells/ice_tomb/IceTombEntity;destroyTomb()V", remap = false), remap = true)
    private void allowNaturalRelease(IceTombEntity tomb) {
        apprenticecodex$withShatterRelease(tomb::destroyTomb);
    }

    @Redirect(method = "die", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/entity/spells/ice_tomb/IceTombEntity;destroyTomb()V"))
    private void allowDamageRelease(IceTombEntity tomb) {
        apprenticecodex$withShatterRelease(tomb::destroyTomb);
    }

    @Unique private int apprenticecodex$releaseDepth;
    @Unique private Runnable apprenticecodex$pendingBurst;

    @Inject(method = "destroyTomb", at = @At("HEAD"))
    private void prepareShatter(CallbackInfo ci) {
        var tomb = (IceTombEntity) (Object) this;
        apprenticecodex$releaseDepth++;
        if (!apprenticecodex$releaseConsumed) {
            // 降車による再入の前に確定し、最外周の解除が終わるまで炸裂を遅延する。
            apprenticecodex$releaseConsumed = true;
            if (apprenticecodex$shatterRelease && !tomb.isRemoved()) {
                apprenticecodex$pendingBurst = IceTombShatter.prepare(tomb, apprenticecodex$spellLevel);
            }
        }
    }

    @Inject(method = "destroyTomb", at = @At("RETURN"))
    private void shatterAfterRelease(CallbackInfo ci) {
        if (--apprenticecodex$releaseDepth == 0 && apprenticecodex$pendingBurst != null) {
            var burst = apprenticecodex$pendingBurst;
            apprenticecodex$pendingBurst = null;
            burst.run();
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"), remap = true)
    private void saveSupport(CompoundTag tag, CallbackInfo ci) {
        if (apprenticecodex$spellLevel > 0) {
            tag.putInt("apprenticecodex:ice_tomb_level", apprenticecodex$spellLevel);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"), remap = true)
    private void loadSupport(CompoundTag tag, CallbackInfo ci) {
        apprenticecodex$spellLevel = tag.getInt("apprenticecodex:ice_tomb_level");
    }
}
