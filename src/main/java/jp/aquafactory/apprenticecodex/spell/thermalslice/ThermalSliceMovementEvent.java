package jp.aquafactory.apprenticecodex.spell.thermalslice;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.ThermalSliceState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ThermalSliceMovementEvent {
    public static final int DASH_DURATION_TICKS = 4;
    public static final double DASH_DISTANCE = 3.5D;

    private ThermalSliceMovementEvent() {
    }

    public static boolean startDash(LivingEntity caster, int bladeEntityId) {
        if (!(caster instanceof Player player) || !player.isAlive() || player.isPassenger()) {
            return false;
        }

        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return false;
        }

        var yawRad = player.getYRot() * Mth.DEG_TO_RAD;
        var direction = new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad));
        var start = player.position();
        var target = start.add(direction.scale(DASH_DISTANCE));
        spellData.edit(CodexSpellStateTypeRegister.THERMAL_SLICE_STATE, state -> {
            state.totalTicks = DASH_DURATION_TICKS;
            state.elapsedTicks = 0;
            state.startX = start.x;
            state.startZ = start.z;
            state.targetX = target.x;
            state.targetZ = target.z;
            state.bladeEntityId = bladeEntityId;
        });
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        applyDashMovement(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        finishDashIfNeeded(event.getEntity());
    }

    static void applyDashMovement(Player player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.THERMAL_SLICE_STATE);
        if (!state.isActive()) {
            return;
        }

        if (!player.isAlive() || player.isPassenger()) {
            finishDash(spellData, player, state);
            return;
        }
        if (!player.level().isClientSide && !hasValidBlade(player.level(), player, state)) {
            deactivate(spellData, player);
            return;
        }

        var nextTick = Math.min(state.totalTicks, state.elapsedTicks + 1);
        var progress = nextTick / (double) state.totalTicks;
        var nextX = Mth.lerp(progress, state.startX, state.targetX);
        var nextZ = Mth.lerp(progress, state.startZ, state.targetZ);
        var currentMovement = player.getDeltaMovement();

        // 高さは通常物理へ任せ、発動時に固定した水平方向だけを短時間補正する。
        player.setDeltaMovement(nextX - player.getX(), currentMovement.y, nextZ - player.getZ());
        markMovementChanged(player);
        spellData.edit(CodexSpellStateTypeRegister.THERMAL_SLICE_STATE, s -> s.elapsedTicks = nextTick);
    }

    static void finishDashIfNeeded(Player player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.THERMAL_SLICE_STATE);
        if (state.totalTicks <= 0) {
            return;
        }
        if (state.elapsedTicks >= state.totalTicks || player.horizontalCollision) {
            finishDash(spellData, player, state);
        }
    }

    private static boolean hasValidBlade(Level level, Player player, ThermalSliceState state) {
        if (state.bladeEntityId < 0) {
            return false;
        }

        var entity = level.getEntity(state.bladeEntityId);
        return entity instanceof ThermalSliceKatanaEntity blade
                && blade.getOwner() == player
                && !blade.isSlashed();
    }

    private static void finishDash(CodexSpellData spellData, Player player, ThermalSliceState state) {
        var movement = player.getDeltaMovement();
        player.setDeltaMovement(0.0D, movement.y, 0.0D);
        markMovementChanged(player);

        if (!player.level().isClientSide && state.bladeEntityId >= 0) {
            var entity = player.level().getEntity(state.bladeEntityId);
            if (entity instanceof ThermalSliceKatanaEntity blade
                    && blade.getOwner() == player
                    && !blade.isSlashed()) {
                blade.slash(player.level());
            }
        }
        spellData.edit(CodexSpellStateTypeRegister.THERMAL_SLICE_STATE, ThermalSliceState::reset);
    }

    private static void deactivate(CodexSpellData spellData, Player player) {
        var movement = player.getDeltaMovement();
        player.setDeltaMovement(0.0D, movement.y, 0.0D);
        markMovementChanged(player);
        spellData.edit(CodexSpellStateTypeRegister.THERMAL_SLICE_STATE, ThermalSliceState::reset);
    }

    private static void markMovementChanged(Player player) {
        player.hasImpulse = true;
        if (!player.level().isClientSide) {
            player.hurtMarked = true;
        }
    }
}
