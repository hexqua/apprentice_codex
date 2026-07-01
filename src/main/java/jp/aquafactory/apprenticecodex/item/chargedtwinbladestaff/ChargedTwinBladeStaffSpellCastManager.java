package jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff;

import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastRequest;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastRunner;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastService;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCooldownManager;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCooldownPolicy;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerContinuousCastManager;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerContinuousRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class ChargedTwinBladeStaffSpellCastManager {
    private static final String CONTINUOUS_RUNTIME_KEY = "charged_twin_blade_staff_impact";
    public static final int CONTINUOUS_IMPACT_CAST_TICKS = 20 * 5;

    private ChargedTwinBladeStaffSpellCastManager() {
    }

    public static boolean tryCastAtImpact(
            ServerLevel level,
            ServerPlayer owner,
            ItemStack sourceStack,
            ChargedTwinBladeStaffSpellPayload payload,
            Vec3 impactPosition,
            Vec3 forward
    ) {
        return tryCastAtImpact(
                level,
                owner,
                sourceStack,
                payload,
                impactPosition,
                forward,
                RemoteOwnerCastOrigin.CHARGED_TWIN_BLADE_STAFF_IMPACT,
                false
        );
    }

    public static boolean tryCastAtImpact(
            ServerLevel level,
            ServerPlayer owner,
            ItemStack sourceStack,
            ChargedTwinBladeStaffSpellPayload payload,
            Vec3 impactPosition,
            Vec3 forward,
            RemoteOwnerCastOrigin castOrigin,
            boolean extendLongCastCooldown
    ) {
        if (!payload.isPresent()) {
            return false;
        }

        var spellData = payload.toSpellData();
        if (spellData == SpellData.EMPTY) {
            return false;
        }

        var spell = spellData.getSpell();
        var castSource = payload.castSource();

        if (spell.getCastType() == CastType.CONTINUOUS) {
            var result = RemoteOwnerCastService.startContinuous(new RemoteOwnerCastRequest(
                    level,
                    owner,
                    sourceStack,
                    spellData,
                    castOrigin,
                    impactPosition,
                    forward,
                    castSource,
                    payload.castingSlot(),
                    true,
                    CONTINUOUS_IMPACT_CAST_TICKS
            ));
            if (!result.handled()) {
                notifyUnsupportedCast(owner, spellData, sourceStack);
                return false;
            }
            if (!result.succeeded() || result.continuousSession() == null) {
                return false;
            }

            RemoteOwnerContinuousCastManager.register(level, new RemoteOwnerContinuousRuntime(
                    owner.getUUID(),
                    CONTINUOUS_RUNTIME_KEY,
                    sourceStack,
                    result.continuousSession(),
                    level.getGameTime() + CONTINUOUS_IMPACT_CAST_TICKS,
                    RemoteOwnerCooldownPolicy.WEAPON_IMBUE,
                    (tickLevel, tickOwner, session) -> {
                        RemoteOwnerCastRunner.syncContinuousCastTransform(session, impactPosition, forward);
                        return true;
                    },
                    (finishedLevel, finishedOwner, cancelled) -> {
                    }
            ));
            return true;
        }

        var result = RemoteOwnerCastService.cast(new RemoteOwnerCastRequest(
                level,
                owner,
                sourceStack,
                spellData,
                castOrigin,
                impactPosition,
                forward,
                castSource,
                payload.castingSlot(),
                true
        ));
        if (!result.handled()) {
            notifyUnsupportedCast(owner, spellData, sourceStack);
            return false;
        }
        if (!result.succeeded()) {
            return false;
        }

        RemoteOwnerCooldownManager.addCooldown(
                owner,
                spellData,
                castSource,
                extendLongCastCooldown
                        ? RemoteOwnerCooldownPolicy.WEAPON_IMBUE_WITH_LONG_CAST_EXTENSION
                        : RemoteOwnerCooldownPolicy.WEAPON_IMBUE
        );
        return true;
    }

    private static void notifyUnsupportedCast(ServerPlayer owner, SpellData spellData, ItemStack sourceStack) {
        owner.displayClientMessage(
                Component.translatable(
                        "ui.apprenticecodex.charged_twin_blade_staff.unsupported_cast",
                        spellData.getSpell().getDisplayName(owner),
                        sourceStack.getHoverName()
                ),
                true
        );
    }
}
