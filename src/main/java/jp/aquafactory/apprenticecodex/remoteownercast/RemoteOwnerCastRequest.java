package jp.aquafactory.apprenticecodex.remoteownercast;

import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record RemoteOwnerCastRequest(
        ServerLevel level,
        ServerPlayer owner,
        ItemStack sourceStack,
        SpellData spellData,
        RemoteOwnerCastOrigin origin,
        Vec3 providedOrigin,
        Vec3 providedForward,
        CastSource castSource,
        String castingSlot,
        boolean postSpellPreCastEvent,
        RemoteOwnerManaPolicy manaPolicy,
        int reservedOwnerMana,
        @Nullable Integer continuousDurationOverrideTicks
) {
    public RemoteOwnerCastRequest(
            ServerLevel level,
            ServerPlayer owner,
            ItemStack sourceStack,
            SpellData spellData,
            RemoteOwnerCastOrigin origin,
            Vec3 providedOrigin,
            Vec3 providedForward,
            CastSource castSource,
            String castingSlot,
            boolean postSpellPreCastEvent
    ) {
        this(
                level,
                owner,
                sourceStack,
                spellData,
                origin,
                providedOrigin,
                providedForward,
                castSource,
                castingSlot,
                postSpellPreCastEvent,
                RemoteOwnerManaPolicy.NORMAL,
                0,
                null
        );
    }

    public RemoteOwnerCastRequest(
            ServerLevel level,
            ServerPlayer owner,
            ItemStack sourceStack,
            SpellData spellData,
            RemoteOwnerCastOrigin origin,
            Vec3 providedOrigin,
            Vec3 providedForward,
            CastSource castSource,
            String castingSlot,
            boolean postSpellPreCastEvent,
            @Nullable Integer continuousDurationOverrideTicks
    ) {
        this(
                level,
                owner,
                sourceStack,
                spellData,
                origin,
                providedOrigin,
                providedForward,
                castSource,
                castingSlot,
                postSpellPreCastEvent,
                RemoteOwnerManaPolicy.NORMAL,
                0,
                continuousDurationOverrideTicks
        );
    }

    public RemoteOwnerCastRequest(
            ServerLevel level,
            ServerPlayer owner,
            ItemStack sourceStack,
            SpellData spellData,
            RemoteOwnerCastOrigin origin,
            Vec3 providedOrigin,
            Vec3 providedForward,
            CastSource castSource,
            String castingSlot,
            boolean postSpellPreCastEvent,
            RemoteOwnerManaPolicy manaPolicy,
            int reservedOwnerMana
    ) {
        this(
                level,
                owner,
                sourceStack,
                spellData,
                origin,
                providedOrigin,
                providedForward,
                castSource,
                castingSlot,
                postSpellPreCastEvent,
                manaPolicy,
                reservedOwnerMana,
                null
        );
    }

    public RemoteOwnerCastRequest {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(owner, "owner");
        sourceStack = Objects.requireNonNull(sourceStack, "sourceStack").copy();
        Objects.requireNonNull(spellData, "spellData");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(providedOrigin, "providedOrigin");
        Objects.requireNonNull(providedForward, "providedForward");
        Objects.requireNonNull(castSource, "castSource");
        Objects.requireNonNull(castingSlot, "castingSlot");
        Objects.requireNonNull(manaPolicy, "manaPolicy");
        reservedOwnerMana = Math.max(0, reservedOwnerMana);
        continuousDurationOverrideTicks = continuousDurationOverrideTicks != null
                ? Math.max(0, continuousDurationOverrideTicks)
                : null;
    }
}
