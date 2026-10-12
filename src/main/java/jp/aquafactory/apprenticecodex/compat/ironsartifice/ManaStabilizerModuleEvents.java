package jp.aquafactory.apprenticecodex.compat.ironsartifice;

import io.redspace.irons_artifice.api.ComposeShotEvent;
import io.redspace.irons_artifice.data.RecoilState;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.ValueModifier;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.ReloadState;
import io.redspace.irons_artifice.item.PendingShot;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.registry.DataAttachmentRegistry;
import io.redspace.irons_artifice.modifier.ModifierItem;
import io.redspace.irons_artifice.modifier.modifiers.BayonetAttachmentModifier;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.modifier.manastabilizermodule.ManaStabilizerModuleModifier;
import jp.aquafactory.apprenticecodex.item.modifier.manastabilizermodule.ManaStabilizerModuleItem;
import jp.aquafactory.apprenticecodex.item.modifier.manastabilizermodule.ManaStabilizerRecovery;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncManaStabilizerRecoilPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

public final class ManaStabilizerModuleEvents {
    private static final ResourceLocation MOVEMENT = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mana_stabilizer_ads_movement");
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();
    private static final class State {
        ItemStack stack;
        long lastInput, acknowledgedShot, revision;
        boolean aiming, hook;
    }
    private ManaStabilizerModuleEvents() {}

    public static int count(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem)) return 0;
        int result = 0;
        for (ItemStack modifier : new GunContainer(stack).getItems()) {
            if (modifier.getItem() instanceof ManaStabilizerModuleItem) result++;
        }
        return result;
    }

    public static boolean eligible(LivingEntity player) {
        ItemStack stack = player.getMainHandItem();
        return player.isAlive() && !player.isSpectator() && count(stack) > 0
                && !GunItem.hasGunSpyglass(stack) && !hasBayonet(stack) && !ReloadState.has(stack);
    }

    private static boolean hasBayonet(ItemStack stack) {
        for (ItemStack modifier : new GunContainer(stack).getItems()) {
            if (modifier.getItem() instanceof ModifierItem item && item.getModifier() instanceof BayonetAttachmentModifier) return true;
        }
        return false;
    }

    public static void input(ServerPlayer player, boolean aiming, boolean hook, long shot) {
        State state = STATES.computeIfAbsent(player, ignored -> new State());
        state.stack = player.getMainHandItem();
        state.lastInput = player.level().getGameTime();
        state.aiming = aiming && eligible(player);
        state.hook = hook;
        // 番号は同期の対応付け専用。射撃可否や反動量はclientの申告を使わない。
        state.acknowledgedShot = Math.max(state.acknowledgedShot, shot);
        movement(player, active(player));
    }

    public static boolean active(ServerPlayer player) {
        State state = STATES.get(player);
        return state != null && state.aiming && state.stack == player.getMainHandItem()
                && player.level().getGameTime() - state.lastInput < 10 && eligible(player);
    }

    public static void movement(ServerPlayer player, boolean aiming) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        double amount = ApprenticeCodexServerConfig.manaStabilizerModuleConfig().adsMovementSpeedMultiplier() - 1;
        if (!aiming) { speed.removeModifier(MOVEMENT); return; }
        player.setSprinting(false);
        var previous = speed.getModifier(MOVEMENT);
        if (previous == null || previous.amount() != amount) {
            speed.removeModifier(MOVEMENT);
            speed.addTransientModifier(new AttributeModifier(MOVEMENT, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            State state = STATES.get(player);
            if (state != null) { state.aiming = false; state.hook = false; }
            movement(player, false);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATES.remove(player);
            movement(player, false);
        }
    }

    @SubscribeEvent
    public static void onCompose(ComposeShotEvent event) {
        LivingEntity shooter = event.getEntity();
        boolean aiming = shooter instanceof ServerPlayer player ? active(player)
                : shooter.level().isClientSide && ManaStabilizerClientBridge.aiming(shooter);
        if (!aiming) return;
        int count = event.getShotProfile().peek(ManaStabilizerModuleModifier.MODULE_COUNT);
        var values = shooter.level().isClientSide ? ManaStabilizerClientBridge.values() : ApprenticeCodexServerConfig.manaStabilizerModuleConfig();
        event.getShotProfile().modifyValue(ShotComponents.CAMERA_RECOIL_MULTIPLIER,
                new ValueModifier(Math.pow(values.adsRecoilMultiplier(), count) - 1, ValueModifier.Operation.MULTIPLY_TOTAL, ValueModifier.Type.HARMFUL));
        event.getShotProfile().modifyValue(ShotComponents.SPREAD,
                new ValueModifier(values.adsSpreadBonus() * count, ValueModifier.Operation.ADD, ValueModifier.Type.HARMFUL));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        boolean aiming = active(player);
        movement(player, aiming);
        State state = STATES.get(player);
        if (state == null) return;
        if (!aiming) state.aiming = false;
        long now = player.level().getGameTime();
        RecoilState recoil = RecoilState.current(player, now);
        boolean paid = false;
        var values = ApprenticeCodexServerConfig.manaStabilizerModuleConfig();
        double step = values.recoveryDegreesPerTick() * count(player.getMainHandItem());
        if (aiming && state.hook && step > 0) {
            double cost = values.manaCostPerTick() * count(player.getMainHandItem());
            var magic = MagicData.getPlayerMagicData(player);
            boolean needsPayment = ManaStabilizerRecovery.needsPayment(recoil.pitch(), recoil.yaw(), step);
            paid = !needsPayment || player.hasInfiniteMaterials() || cost == 0 || magic.getMana() >= cost;
            if (needsPayment && paid && !player.hasInfiniteMaterials() && cost > 0) {
                magic.setMana((float) Math.max(0, magic.getMana() - cost));
                if (!(player instanceof FakePlayer)) PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
            }
            var next = ManaStabilizerRecovery.recover(recoil.pitch(), recoil.yaw(), step, paid);
            recoil = new RecoilState(next.x, next.y, now);
            player.setData(DataAttachmentRegistry.RECOIL, recoil);
        }
        // 上流の1tick保留が解消するまで、その射撃より古い反動を確定結果として返さない。
        if (state.hook && PendingShot.get(player).isEmpty() && !(player instanceof FakePlayer)) Networks.sendToPlayer(player, new SyncManaStabilizerRecoilPacket(
                now, ++state.revision, state.acknowledgedShot, recoil.pitch(), recoil.yaw(), paid));
        if (!aiming) state.hook = false;
        if (!player.isAlive() || player.isRemoved()) STATES.remove(player);
    }
}
