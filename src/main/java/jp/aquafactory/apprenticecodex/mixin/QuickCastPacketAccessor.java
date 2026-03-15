package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.network.casting.QuickCastPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = QuickCastPacket.class, remap = false)
public interface QuickCastPacketAccessor {
    @Accessor("slot")
    int apprenticecodex$getSlot();
}
