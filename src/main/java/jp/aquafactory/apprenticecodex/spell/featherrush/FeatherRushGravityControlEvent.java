package jp.aquafactory.apprenticecodex.spell.featherrush;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.FeatherRushState;
import jp.aquafactory.apprenticecodex.spell.featherrush.FeatherRushWingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class FeatherRushGravityControlEvent {
    private FeatherRushGravityControlEvent() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        var player = event.getEntity();
        var level = player.level();
        if (level.isClientSide) {
            return;
        }

        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.FEATHER_RUSH_STATE);
        if (!isActive(level, state)) {
            deactivate(spellData, player, state);
            return;
        }

        if (!isWingValid(level, player, state.wingEntityId)) {
            deactivate(spellData, player, state);
            return;
        }

        if (!isAirborneControlTarget(player)) {
            clearNoGravity(spellData, player, state);
            return;
        }

        player.fallDistance = 0;
        if (player.isShiftKeyDown()) {
            clearNoGravity(spellData, player, state);
            return;
        }

        applyNoGravity(spellData, player, state);
        var velocity = player.getDeltaMovement();
        if (velocity.y < 0.0) {
            player.setDeltaMovement(velocity.x, 0.0, velocity.z);
            player.hasImpulse = true;
            player.hurtMarked = true;
        }
    }

    private static boolean isActive(Level level, FeatherRushState state) {
        return state.activeUntilGameTime >= level.getGameTime();
    }

    private static boolean isWingValid(Level level, Player player, int wingEntityId) {
        if (wingEntityId < 0) {
            return false;
        }

        var entity = level.getEntity(wingEntityId);
        if (!(entity instanceof FeatherRushWingEntity wing) || wing.isRemoved()) {
            return false;
        }

        return wing.getOwner() == player;
    }

    private static boolean isAirborneControlTarget(Player player) {
        return !player.isInWaterOrBubble() && !player.isFallFlying() && !player.getAbilities().flying &&
                !player.onClimbable() && !player.isPassenger() && !player.isSwimming() && !player.onGround();
    }

    private static void applyNoGravity(CodexSpellData spellData, Player player, FeatherRushState state) {
        player.setNoGravity(true);
        if (!state.noGravityApplied) {
            spellData.edit(CodexSpellStateTypeRegister.FEATHER_RUSH_STATE, s -> s.noGravityApplied = true);
        }
    }

    private static void clearNoGravity(CodexSpellData spellData, Player player, FeatherRushState state) {
        if (!state.noGravityApplied) {
            return;
        }

        player.setNoGravity(false);
        spellData.edit(CodexSpellStateTypeRegister.FEATHER_RUSH_STATE, s -> s.noGravityApplied = false);
    }

    private static void deactivate(CodexSpellData spellData, Player player, FeatherRushState state) {
        if (state.activeUntilGameTime == 0 && state.wingEntityId == -1 && !state.noGravityApplied) {
            return;
        }

        if (state.noGravityApplied) {
            player.setNoGravity(false);
        }

        spellData.edit(CodexSpellStateTypeRegister.FEATHER_RUSH_STATE, s -> {
            s.activeUntilGameTime = 0;
            s.wingEntityId = -1;
            s.noGravityApplied = false;
        });
    }
}

