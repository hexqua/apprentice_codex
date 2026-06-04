package jp.aquafactory.apprenticecodex.remoteownercast;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class RemoteOwnerCastRules {
    private RemoteOwnerCastRules() {
    }

    public static RemoteOwnerCastCheckResult checkImbue(
            @Nullable AbstractSpell spell,
            int spellLevel,
            RemoteOwnerCastOrigin origin
    ) {
        return checkImbue(spell, spellLevel, null, origin);
    }

    public static RemoteOwnerCastCheckResult checkImbue(
            @Nullable AbstractSpell spell,
            int spellLevel,
            @Nullable Player player,
            RemoteOwnerCastOrigin origin
    ) {
        return checkProfile(spell, spellLevel, player, origin, false);
    }

    public static RemoteOwnerCastCheckResult checkExecution(
            SpellData spellData,
            ServerPlayer owner,
            RemoteOwnerCastOrigin origin
    ) {
        if (spellData == null || spellData == SpellData.EMPTY) {
            return RemoteOwnerCastCheckResult.denied(RemoteOwnerCastFailureReason.EMPTY_SPELL);
        }
        return checkProfile(spellData.getSpell(), spellData.getLevel(), owner, origin, true);
    }

    public static Optional<RemoteOwnerCastProfile> resolveProfileForImbue(
            @Nullable AbstractSpell spell,
            RemoteOwnerCastOrigin origin
    ) {
        return checkImbue(spell, 1, origin).profile();
    }

    public static Optional<RemoteOwnerCastProfile> resolveProfileForExecution(
            SpellData spellData,
            ServerPlayer owner,
            RemoteOwnerCastOrigin origin
    ) {
        return checkExecution(spellData, owner, origin).profile();
    }

    private static RemoteOwnerCastCheckResult checkProfile(
            @Nullable AbstractSpell spell,
            int spellLevel,
            @Nullable Player player,
            RemoteOwnerCastOrigin origin,
            boolean execution
    ) {
        if (isEmptySpell(spell)) {
            return RemoteOwnerCastCheckResult.denied(RemoteOwnerCastFailureReason.EMPTY_SPELL);
        }
        if (execution && ApprenticeCodexServerConfig.isRemoteOwnerCastSpellDenied(spell.getSpellResource())) {
            return RemoteOwnerCastCheckResult.denied(RemoteOwnerCastFailureReason.SERVER_DENIED);
        }

        var profile = RemoteOwnerCastProfileManager.getProfile(spell);
        if (profile.isEmpty()) {
            return RemoteOwnerCastCheckResult.denied(RemoteOwnerCastFailureReason.NO_PROFILE);
        }
        var resolvedProfile = profile.get();
        if (!resolvedProfile.allowsOrigin(origin)) {
            return RemoteOwnerCastCheckResult.denied(RemoteOwnerCastFailureReason.ORIGIN_NOT_ALLOWED);
        }
        if (!isSupportedCastType(spell.getCastType())) {
            return RemoteOwnerCastCheckResult.denied(RemoteOwnerCastFailureReason.UNSUPPORTED_CAST_TYPE);
        }
        if (execution
                && resolvedProfile.castMode() == RemoteOwnerCastMode.REMOTE_PLAYER_GEOMETRY
                && !ApprenticeCodexServerConfig.remoteOwnerCastEnableRemotePlayerGeometry()) {
            return RemoteOwnerCastCheckResult.denied(RemoteOwnerCastFailureReason.REMOTE_PLAYER_GEOMETRY_DISABLED);
        }

        var hasRecast = spell.getRecastCount(spellLevel, player) > 0;
        if (hasRecast && !resolvedProfile.allowInitialRecast()) {
            return RemoteOwnerCastCheckResult.denied(RemoteOwnerCastFailureReason.RECAST_NOT_ALLOWED);
        }
        if (hasRecast && player != null) {
            var magicData = MagicData.getPlayerMagicData(player);
            if (magicData == null || magicData.getPlayerRecasts().hasRecastForSpell(spell)) {
                return RemoteOwnerCastCheckResult.denied(RemoteOwnerCastFailureReason.ACTIVE_RECAST_EXISTS);
            }
        }

        return RemoteOwnerCastCheckResult.allowed(resolvedProfile);
    }

    private static boolean isEmptySpell(@Nullable AbstractSpell spell) {
        return spell == null
                || spell == io.redspace.ironsspellbooks.api.registry.SpellRegistry.none()
                || spell.getSpellResource() == null;
    }

    private static boolean isSupportedCastType(CastType castType) {
        return castType == CastType.INSTANT
                || castType == CastType.LONG
                || castType == CastType.CONTINUOUS;
    }
}
