package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class CombatOwnerResolver {
    private CombatOwnerResolver() {
    }

    public static @Nullable UUID captureCombatOwnerUuid(@Nullable Entity owner) {
        if (owner instanceof CombatOwnerUuidSource source && source.getCombatOwnerUuid() != null) {
            return source.getCombatOwnerUuid();
        }
        if (owner instanceof Player player) {
            return player.getUUID();
        }
        return null;
    }

    public static @Nullable Entity resolveCombatOwner(
            Level level,
            @Nullable Entity currentOwner,
            @Nullable UUID combatOwnerUuid
    ) {
        if (isDirectPlayerOwner(currentOwner)) {
            return currentOwner;
        }
        if (combatOwnerUuid == null || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getPlayerByUUID(combatOwnerUuid);
    }

    public static DamageSource createDamageSource(
            Level level,
            Entity directEntity,
            @Nullable Entity currentOwner,
            @Nullable UUID combatOwnerUuid,
            ResourceKey<DamageType> damageType
    ) {
        var combatOwner = resolveCombatOwner(level, currentOwner, combatOwnerUuid);
        if (combatOwner != null) {
            return CombatTools.getDamageSource(level, directEntity, combatOwner, damageType);
        }
        return CombatTools.getDamageSource(level, directEntity, damageType);
    }

    private static boolean isDirectPlayerOwner(@Nullable Entity owner) {
        return owner instanceof Player && !(owner instanceof FakePlayer);
    }
}
