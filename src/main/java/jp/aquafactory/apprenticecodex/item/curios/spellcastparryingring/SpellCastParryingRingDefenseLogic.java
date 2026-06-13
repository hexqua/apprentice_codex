package jp.aquafactory.apprenticecodex.item.curios.spellcastparryingring;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class SpellCastParryingRingDefenseLogic {
    private SpellCastParryingRingDefenseLogic() {
    }

    static boolean canParry(LivingEntity defender, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_SHIELD)) {
            return false;
        }
        if (!isFromFront(defender, source)) {
            return false;
        }

        var windowTicks = ApprenticeCodexServerConfig.spellCastParryingRingParryWindowTicks();
        return isWithinNormalLongCastWindow(defender, windowTicks)
                || isWithinFocusStaffbowPendingWindow(defender, windowTicks);
    }

    private static boolean isWithinNormalLongCastWindow(LivingEntity defender, int windowTicks) {
        var magicData = MagicData.getPlayerMagicData(defender);
        if (magicData == null || !magicData.isCasting()) {
            return false;
        }
        if (magicData.getCastType() != CastType.LONG) {
            return false;
        }

        var elapsedTicks = magicData.getCastDuration() - magicData.getCastDurationRemaining();
        return elapsedTicks >= 0 && elapsedTicks <= windowTicks;
    }

    private static boolean isWithinFocusStaffbowPendingWindow(LivingEntity defender, int windowTicks) {
        var spellData = Capabilities.getSpellDataOrNull(defender);
        if (spellData == null) {
            return false;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE);
        if (!state.isPending()) {
            return false;
        }

        var spell = SpellRegistry.getSpell(state.spellId);
        if (spell == null || spell == SpellRegistry.none()) {
            return false;
        }
        if (spell.getCastType() != CastType.LONG && spell.getCastType() != CastType.INSTANT) {
            return false;
        }

        // FocusStaffbow の pending は通常の Iron's casting state を使わないため、専用 state の開始 tick を参照する。
        var elapsedTicks = state.getElapsedTicks(defender.level().getGameTime());
        return elapsedTicks <= windowTicks;
    }

    private static boolean isFromFront(LivingEntity defender, DamageSource source) {
        var defenderForward = resolveHorizontalFacing(defender);
        var incomingOrigin = resolveIncomingOrigin(defender, source);
        var horizontalIncomingOrigin = horizontal(incomingOrigin);
        if (!isUsableDirection(horizontalIncomingOrigin)) {
            return false;
        }

        return defenderForward.dot(horizontalIncomingOrigin.normalize()) >= 0.0D;
    }

    private static Vec3 resolveHorizontalFacing(LivingEntity defender) {
        var look = horizontal(defender.getLookAngle());
        if (isUsableDirection(look)) {
            return look.normalize();
        }
        return horizontal(Vec3.directionFromRotation(0.0F, defender.getYRot())).normalize();
    }

    private static Vec3 resolveIncomingOrigin(LivingEntity defender, DamageSource source) {
        var directEntity = source.getDirectEntity();
        if (directEntity != null && directEntity != defender) {
            return directEntity.getBoundingBox().getCenter().subtract(defender.getBoundingBox().getCenter());
        }

        var attacker = source.getEntity();
        if (attacker != null && attacker != defender) {
            return attacker.getBoundingBox().getCenter().subtract(defender.getBoundingBox().getCenter());
        }

        var sourcePosition = source.getSourcePosition();
        if (sourcePosition != null) {
            return sourcePosition.subtract(defender.getBoundingBox().getCenter());
        }

        if (directEntity != null) {
            return directEntity.getDeltaMovement().reverse();
        }
        return Vec3.ZERO;
    }

    private static Vec3 horizontal(@Nullable Vec3 vector) {
        if (vector == null) {
            return Vec3.ZERO;
        }
        return new Vec3(vector.x, 0.0D, vector.z);
    }

    private static boolean isUsableDirection(Vec3 vector) {
        return vector.lengthSqr() > 1.0e-6D;
    }
}
