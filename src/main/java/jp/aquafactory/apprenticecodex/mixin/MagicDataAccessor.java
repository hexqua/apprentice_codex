package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = MagicData.class, remap = false)
public interface MagicDataAccessor {
    @Accessor("serverPlayer")
    @Nullable ServerPlayer apprenticecodex$getServerPlayer();

    @Accessor("castingSpellLevel")
    void apprenticecodex$setCastingSpellLevel(int castingSpellLevel);

    @Accessor("castDuration")
    void apprenticecodex$setCastDuration(int castDuration);

    @Accessor("castDurationRemaining")
    void apprenticecodex$setCastDurationRemaining(int castDurationRemaining);

    @Accessor("castSource")
    void apprenticecodex$setCastSource(CastSource castSource);

    @Accessor("castType")
    void apprenticecodex$setCastType(CastType castType);
}
