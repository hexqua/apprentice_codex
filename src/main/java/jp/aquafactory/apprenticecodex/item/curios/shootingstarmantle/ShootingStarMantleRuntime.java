package jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncMantlePacket;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.quickblink.QuickBlinkRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ShootingStarMantleRuntime {
    public static final String SPELL_SLOT = "apprenticecodex:shooting_star_mantle";
    public static final String SCROLL_SPELL_SLOT = "apprenticecodex:shooting_star_mantle_scrolls";
    // 統合サーバーでもclient予測がserver状態を書き換えないよう分離する。
    private static final Map<UUID, State> SERVER = new HashMap<>();
    private static final Map<UUID, State> CLIENT = new HashMap<>();

    private ShootingStarMantleRuntime() { }

    public static final class State {
        public boolean hovering;
        public boolean flying;
        public int energy = 100;
        public int maxEnergy = MantleEnergy.MAX;
        public boolean recovering;
        public int movingTicks;
        public Vec3 lastPosition;
        public int dashTicks;
        public final MantleBlink blink = new MantleBlink();
        public final MantleElementalDash elemental = new MantleElementalDash();
        public final MantleFireworkBoost firework = new MantleFireworkBoost();
        public Vec3 dashDirection = Vec3.ZERO;
        public long lastSequence = -1;
        public int blinkTicks;
        public boolean equipped;
        private ItemStack stack = ItemStack.EMPTY;
        private int recoveryTicks;
        private SyncMantlePacket lastSent;
    }

    public static State state(Player player) {
        return (player.level().isClientSide ? CLIENT : SERVER).computeIfAbsent(player.getUUID(), id -> new State());
    }

    public static ItemStack findEquipped(Player player) {
        return CuriosApi.getCuriosInventory(player).map(inventory -> {
            var back = inventory.getCurios().get("back");
            if (back != null) {
                for (int i = 0; i < back.getStacks().getSlots(); i++) {
                    var stack = back.getStacks().getStackInSlot(i);
                    if (back.getActiveStates().size() > i && back.getActiveStates().get(i)
                            && stack.getItem() instanceof ShootingStarMantle) return stack;
                }
            }
            return ItemStack.EMPTY;
        }).orElse(ItemStack.EMPTY);
    }

    public static boolean conflict(Player player) {
        var data = Capabilities.getSpellDataOrNull(player);
        return player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
                || player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffectRegistry.ANGEL_WINGS.get()))
                || data != null && data.get(CodexSpellStateTypeRegister.SPECTRAL_WING_STATE).active;
    }

    public static boolean invalidHoverContext(Player player) {
        return !player.isAlive() || player.isSpectator() || player.isPassenger() || player.onClimbable()
                || player.getAbilities().flying || player.isInWaterOrBubble() || player.isInLava();
    }

    public static boolean canFly(Player player) {
        var stack = findEquipped(player);
        return !stack.isEmpty() && (MantleEnergy.read(stack).usable() || state(player).elemental.paidFlight()) && !conflict(player)
                && !state(player).hovering && !invalidHoverContext(player) && !player.hasEffect(MobEffects.LEVITATION);
    }

    public static boolean isHovering(Player player) {
        // 枯渇しても支払い済みの高速移動だけは完走する。装備・競合の制約は緩めない。
        return state(player).hovering && !findEquipped(player).isEmpty() && !conflict(player)
                && !invalidHoverContext(player) && (state(player).elemental.paidHover() || state(player).dashTicks > 0
                || state(player).blink.active(player.level().getGameTime()) || (player.level().isClientSide
                ? !state(player).recovering && state(player).energy > 0 : MantleEnergy.read(findEquipped(player)).usable()));
    }

    public static String castError(Player player) {
        var stack = findEquipped(player);
        if (stack.isEmpty()) return "not_found_mantle";
        if (conflict(player) || invalidHoverContext(player)) return "conflict";
        if (!MantleEnergy.read(stack).usable()) return "error_recovery_mode";
        return null;
    }

    public static void notify(Player player, String key, ChatFormatting color) {
        if (player instanceof ServerPlayer) player.displayClientMessage(Component.translatable(key).withStyle(color), true);
    }

    static void notifyDepleted(ServerPlayer player) {
        notify(player, "ui.apprenticecodex.shooting_star_mantle.depleted_energy", ChatFormatting.YELLOW);
        player.level().playSound(null, player.blockPosition(), SoundRegistry.VANILLA_MANTLE_DEPLETE.get(), SoundSource.PLAYERS, 1, 1);
    }

    public static boolean toggle(ServerPlayer player) {
        if (castError(player) != null) return false;
        var state = state(player);
        bind(player, state, findEquipped(player));
        state.elemental.cancel(player);
        state.firework.reset();
        if (player.isFallFlying()) player.stopFallFlying();
        state.flying = false;
        state.hovering = !state.hovering;
        if (state.hovering) player.fallDistance = 0;
        state.dashTicks = 0;
        state.blink.cancel();
        state.lastPosition = null;
        sync(player, false);
        return true;
    }

    private static void bind(ServerPlayer player, State state, ItemStack stack) {
        if (state.stack == stack) return;
        stop(player, state);
        state.stack = stack;
        state.recoveryTicks = 0;
    }

    public static void refreshEquipment(ServerPlayer player) {
        bind(player, state(player), findEquipped(player));
        sync(player, false);
    }

    private static void stop(Player player, State state) {
        stop(player, state, false);
    }

    private static void stop(Player player, State state, boolean completed) {
        state.elemental.cancel(player);
        state.firework.reset();
        // 競合時のfallFlyingフラグは優先側へ引き継ぎ、毎tick解除しない。
        if (state.flying && !conflict(player) && player.isFallFlying()) player.stopFallFlying();
        state.flying = false;
        state.hovering = false;
        state.dashTicks = 0;
        // 完走した描画時刻は、他playerの位置補間が追いつくまで残す。
        if (!completed) state.blink.cancel();
        state.movingTicks = 0;
        state.lastPosition = null;
    }

    public static void tick(ServerPlayer player) {
        var state = state(player);
        var stack = findEquipped(player);
        bind(player, state, stack);
        if ((state.hovering || state.flying) && conflict(player)) {
            notify(player, "ui.apprenticecodex.wavering_star.conflict", ChatFormatting.RED);
            stop(player, state);
        }
        if (invalidHoverContext(player)) stop(player, state);
        if (stack.isEmpty() || !player.isAlive() || player.isSpectator()) {
            stop(player, state);
            sync(player, false);
            return;
        }
        state.flying = player.isFallFlying() && canFly(player);
        state.blink.update(player, state);
        state.elemental.tick(player);
        state.firework.tick(player);
        var before = MantleEnergy.read(stack);
        var after = before;
        boolean full = false;
        if (state.hovering || state.flying) {
            state.recoveryTicks = 0;
            after = before.tickUse();
            if (state.hovering) {
                player.fallDistance = 0;
                if (state.dashTicks > 0 && --state.dashTicks == 0) MantleMovement.finishImpulse(player);
            }
            if (!after.usable()) {
                if (state.dashTicks == 0 && !state.blink.active(player.level().getGameTime())
                        && !state.elemental.active()) stop(player, state, true);
                if (before.usable()) {
                    if (state.elemental.active()) state.elemental.deferDepletion();
                    else notifyDepleted(player);
                }
            }
        } else if (!player.isFallFlying() && before.energy() < before.maxEnergy()) {
            if (++state.recoveryTicks >= 10) {
                state.recoveryTicks = 0;
                float cost = ApprenticeCodexServerConfig.shootingStarMantleRecoveryCost(
                        before.recovering() || MantleCalibration.fastRecovery(stack));
                if (recharge(player, stack, cost)) {
                    after = MantleEnergy.read(stack);
                    full = before.recovering() && !after.recovering();
                }
            }
        } else state.recoveryTicks = 0;
        if (!before.equals(after)) after.save(stack);
        if (full) player.level().playSound(null, player.blockPosition(), SoundRegistry.VANILLA_COLLECT_MANA.get(), SoundSource.PLAYERS, 1, 1);
        sync(player, full);
    }

    public static boolean recharge(ServerPlayer player, ItemStack stack, float cost) {
        var energy = MantleEnergy.read(stack);
        var magic = MagicData.getPlayerMagicData(player);
        if (energy.energy() == energy.maxEnergy() || magic.getMana() < cost) return false;
        magic.setMana(magic.getMana() - cost);
        energy.recharge(MantleCalibration.fastRecovery(stack)).save(stack);
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
        return true;
    }

    public static boolean impulse(ServerPlayer player, long sequence, float forward, float strafe) {
        var state = state(player);
        var direction = MantleMovement.direction(forward, strafe, player.getYRot());
        var stack = findEquipped(player);
        boolean accepted = sequence > state.lastSequence && sequence >= 0 && state.stack == stack
                && MantleCalibration.elementalKind(stack) == 0
                && isHovering(player) && state.dashTicks == 0 && !state.blink.active(player.level().getGameTime())
                && !QuickBlinkRuntime.active(player)
                && MantleEnergy.read(stack).canImpulse()
                && direction.lengthSqr() > 0;
        state.lastSequence = Math.max(state.lastSequence, sequence);
        if (accepted) {
            MantleEnergy.read(stack).impulse().save(stack);
            if (MantleEnergy.read(stack).recovering()) {
                notifyDepleted(player);
            }
            if (MantleCalibration.usesBlink(stack)) {
                state.blink.begin(player, sequence, direction);
            } else {
                state.blink.cancel();
                state.dashTicks = 5;
                state.dashDirection = direction;
                player.setDeltaMovement(direction.x, player.getDeltaMovement().y, direction.z);
                player.level().playSound(null, player.blockPosition(), SoundRegistry.VANILLA_MANTLE_IMPULSE.get(), SoundSource.PLAYERS, 1, 1);
            }
        }
        PacketDistributor.sendToPlayer(player, packet(player, false, sequence, accepted));
        if (accepted) sync(player, false);
        if (!accepted && !(player instanceof FakePlayer)) {
            // 拒否済みの予測移動は現在のserver位置へ戻す。通常の移動検証は維持する。
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        }
        return accepted;
    }

    public static SyncMantlePacket packet(ServerPlayer player, boolean blink, long sequence, boolean accepted) {
        var stack = findEquipped(player);
        var energy = MantleEnergy.read(stack);
        var state = state(player);
        return new SyncMantlePacket(player.getId(), !stack.isEmpty(), energy.energy(), energy.maxEnergy(), energy.recovering() && !state.elemental.defersDepletion(), state.hovering, blink, sequence, accepted,
                state.blink.start(), state.blink.sequence(), state.blink.direction());
    }

    public static void sync(ServerPlayer player, boolean blink) {
        var state = state(player);
        var packet = packet(player, blink, -1, false);
        if (!packet.equals(state.lastSent)) {
            Networks.sendToTrackingEntityAndSelf(player, packet);
            state.lastSent = packet;
        }
    }

    public static void clear(ServerPlayer player) {
        stop(player, state(player));
        sync(player, false);
        SERVER.remove(player.getUUID());
    }

    public static void clearClient() { CLIENT.clear(); }
    public static void clearServer() { SERVER.clear(); }
}
