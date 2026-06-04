package jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff;

import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileManager;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastRequest;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastRunner;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastService;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCooldownManager;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCooldownPolicy;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ChargedTwinBladeStaffSpellCastManager {
    public static final int CONTINUOUS_IMPACT_CAST_TICKS = 20 * 5;
    private static final Map<ServerLevel, List<ContinuousImpactCastRuntime>> ACTIVE_CONTINUOUS_CASTS = new WeakHashMap<>();

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
            var remoteProfile = RemoteOwnerCastProfileManager.getUsableProfile(spell, castOrigin);
            if (remoteProfile.isEmpty()) {
                notifyUnsupportedCast(owner, spellData, sourceStack);
                return false;
            }

            var remoteStartResult = RemoteOwnerCastRunner.tryStartContinuousCast(
                    level,
                    owner,
                    sourceStack,
                    spellData,
                    remoteProfile.get(),
                    castOrigin,
                    impactPosition,
                    forward,
                    castSource,
                    payload.castingSlot(),
                    CONTINUOUS_IMPACT_CAST_TICKS,
                    true
            );
            if (!remoteStartResult.handled()) {
                notifyUnsupportedCast(owner, spellData, sourceStack);
                return false;
            }
            if (!remoteStartResult.succeeded() || remoteStartResult.session() == null) {
                return false;
            }

            ACTIVE_CONTINUOUS_CASTS.computeIfAbsent(level, key -> new ArrayList<>()).add(
                    new ContinuousImpactCastRuntime(
                            owner.getUUID(),
                            impactPosition,
                            forward,
                            remoteStartResult.session(),
                            level.getGameTime() + CONTINUOUS_IMPACT_CAST_TICKS,
                            sourceStack.copy()
                    )
            );
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
                sourceStack,
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

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }

        var runtimes = ACTIVE_CONTINUOUS_CASTS.get(level);
        if (runtimes == null || runtimes.isEmpty()) {
            return;
        }

        var iterator = runtimes.iterator();
        while (iterator.hasNext()) {
            var runtime = iterator.next();
            var owner = level.getPlayerByUUID(runtime.ownerId());
            if (!(owner instanceof ServerPlayer serverPlayer) || serverPlayer.isDeadOrDying() || serverPlayer.isSpectator()) {
                RemoteOwnerCastRunner.cancelContinuousCastWithoutOwner(runtime.session());
                iterator.remove();
                continue;
            }

            RemoteOwnerCastRunner.syncContinuousCastTransform(runtime.session(), runtime.position(), runtime.forward());
            if (level.getGameTime() >= runtime.finishAtGameTime()) {
                RemoteOwnerCastRunner.finishContinuousCast(level, serverPlayer, runtime.session(), false);
            } else if (RemoteOwnerCastRunner.tickContinuousCast(level, serverPlayer, runtime.session())) {
                continue;
            }

            applyCooldownIfNeeded(serverPlayer, runtime.session(), runtime.castingStack());
            iterator.remove();
        }

        if (runtimes.isEmpty()) {
            ACTIVE_CONTINUOUS_CASTS.remove(level);
        }
    }

    private static void applyCooldownIfNeeded(ServerPlayer owner, RemoteOwnerCastRunner.ContinuousCastSession session, ItemStack castingStack) {
        if (session.consumeFinishedCooldownTicks() <= 0) {
            return;
        }

        var spellData = session.spellData();
        var castSource = session.castSource();
        if (spellData == null || castSource == null) {
            return;
        }

        RemoteOwnerCooldownManager.addCooldown(
                owner,
                spellData,
                castSource,
                castingStack,
                RemoteOwnerCooldownPolicy.WEAPON_IMBUE
        );
    }

    private record ContinuousImpactCastRuntime(
            UUID ownerId,
            Vec3 position,
            Vec3 forward,
            RemoteOwnerCastRunner.ContinuousCastSession session,
            long finishAtGameTime,
            ItemStack castingStack
    ) {
    }
}
