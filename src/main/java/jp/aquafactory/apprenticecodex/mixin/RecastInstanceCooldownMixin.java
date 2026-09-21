package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellgunRecastCooldown;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RecastInstance.class, remap = false)
public abstract class RecastInstanceCooldownMixin implements SpellgunRecastCooldown.Holder {
    @Unique private static final String APPRENTICECODEX_COOLDOWN = "apprenticecodex:spellgun_cooldown";
    @Unique private @Nullable SpellgunRecastCooldown apprenticecodex$cooldown;

    @Override
    public @Nullable SpellgunRecastCooldown apprenticecodex$getCooldown() { return apprenticecodex$cooldown; }

    @Override
    public void apprenticecodex$setCooldown(@Nullable SpellgunRecastCooldown cooldown) { apprenticecodex$cooldown = cooldown; }

    // 表示は新規発射の予測値なので、このサーバー専用情報をRecastの通信形式には追加しない。
    @Inject(method = "serializeNBT(Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
    private void apprenticecodex$saveCooldown(HolderLookup.Provider provider, CallbackInfoReturnable<CompoundTag> cir) {
        if (apprenticecodex$cooldown != null) cir.getReturnValue().put(APPRENTICECODEX_COOLDOWN, apprenticecodex$cooldown.save());
    }

    @Inject(method = "deserializeNBT(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void apprenticecodex$loadCooldown(HolderLookup.Provider provider, CompoundTag compoundTag, CallbackInfo ci) {
        apprenticecodex$cooldown = compoundTag.contains(APPRENTICECODEX_COOLDOWN, Tag.TAG_COMPOUND)
                ? SpellgunRecastCooldown.load(compoundTag.getCompound(APPRENTICECODEX_COOLDOWN)) : null;
    }
}
