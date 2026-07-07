package jp.aquafactory.apprenticecodex.spell.remoteeye;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.RemoteEyeState;
import jp.aquafactory.apprenticecodex.utility.PersistentGameTimeSanitizer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class RemoteEyeBodyControlEvent {
    private static final double POSITION_EPSILON_SQ = 1.0e-6;

    private RemoteEyeBodyControlEvent() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        var player = event.player;
        var level = player.level();
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.REMOTE_EYE_STATE);
        sanitizePersistentGameTimes(spellData, player, state);
        if (!isActive(level.getGameTime(), state)) {
            deactivate(spellData, player, state);
            return;
        }

        if (!player.isAlive() || player.isPassenger()) {
            deactivate(spellData, player, state);
            return;
        }

        // 視点移動中も本体は現場に残すため、クライアント/サーバーの両方で位置を固定する.
        anchorBody(player, state);
    }

    private static boolean isActive(long gameTime, RemoteEyeState state) {
        return state.activeUntilGameTime > gameTime;
    }

    private static void sanitizePersistentGameTimes(CodexSpellData spellData, Player player, RemoteEyeState state) {
        var gameTime = player.level().getGameTime();
        var repairMaxActiveTicks = state.activeDurationTicks > 0L
                ? state.activeDurationTicks
                : RemoteEye.PERSISTED_STATE_REPAIR_MAX_ACTIVE_TICKS;
        var sanitizedActiveUntilGameTime = PersistentGameTimeSanitizer.repairPersistedFutureUntil(
                gameTime,
                state.activeUntilGameTime,
                repairMaxActiveTicks
        );
        if (sanitizedActiveUntilGameTime == state.activeUntilGameTime) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.REMOTE_EYE_STATE, s -> s.activeUntilGameTime = sanitizedActiveUntilGameTime);
        state.activeUntilGameTime = sanitizedActiveUntilGameTime;
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            RemoteEyeSync.syncToClient(serverPlayer, spellData.get(CodexSpellStateTypeRegister.REMOTE_EYE_STATE));
        }
    }

    private static void anchorBody(Player player, RemoteEyeState state) {
        var anchor = new Vec3(state.anchorX, state.anchorY, state.anchorZ);
        if (player.position().distanceToSqr(anchor) > POSITION_EPSILON_SQ) {
            player.setPos(anchor.x, anchor.y, anchor.z);
        }

        player.setYRot(state.anchorYaw);
        player.setXRot(state.anchorPitch);
        player.setYHeadRot(state.anchorYaw);
        player.setYBodyRot(state.anchorYaw);
        player.yRotO = state.anchorYaw;
        player.xRotO = state.anchorPitch;
        player.fallDistance = 0.0f;
        player.setDeltaMovement(Vec3.ZERO);
        player.xOld = anchor.x;
        player.yOld = anchor.y;
        player.zOld = anchor.z;
        player.hasImpulse = false;
        player.hurtMarked = true;
    }

    private static void deactivate(CodexSpellData spellData, Player player, RemoteEyeState state) {
        if (state.activeUntilGameTime == 0L) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.REMOTE_EYE_STATE, RemoteEyeState::reset);
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            RemoteEyeSync.syncToClient(serverPlayer, spellData.get(CodexSpellStateTypeRegister.REMOTE_EYE_STATE));
        }
    }
}
