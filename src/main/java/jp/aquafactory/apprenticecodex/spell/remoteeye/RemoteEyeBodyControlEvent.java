package jp.aquafactory.apprenticecodex.spell.remoteeye;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.RemoteEyeState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class RemoteEyeBodyControlEvent {
    private static final double POSITION_EPSILON_SQ = 1.0e-6;

    private RemoteEyeBodyControlEvent() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        var level = player.level();
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.REMOTE_EYE_STATE);
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

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            var spellData = Capabilities.getSpellDataOrNull(serverPlayer);
            if (spellData == null) {
                return;
            }
            spellData.edit(CodexSpellStateTypeRegister.REMOTE_EYE_STATE, RemoteEyeState::reset);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        syncToClient(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncToClient(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncToClient(event.getEntity());
    }

    private static void syncToClient(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var spellData = Capabilities.getSpellDataOrNull(serverPlayer);
        if (spellData == null) {
            return;
        }

        RemoteEyeSync.syncToClient(serverPlayer, spellData.get(CodexSpellStateTypeRegister.REMOTE_EYE_STATE));
    }
}
