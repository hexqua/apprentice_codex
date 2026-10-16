package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsRaycast.BlockTarget;
import jp.aquafactory.apprenticecodex.mixin.ItemEntityAccessor;
import jp.aquafactory.apprenticecodex.utility.BlockTools;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** 採掘条件と回収を腕の戦闘処理から分け、loader固有のドロップ捕捉をここに閉じ込める。 */
@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class PsychicDriveGauntletsMining {
    public static final int MINING_TICKS = 12;
    private static final ThreadLocal<Capture> CAPTURE = new ThreadLocal<>();
    private static final Set<ItemEntity> CARRIED_ITEMS = Collections.newSetFromMap(new IdentityHashMap<>());
    private record Capture(ServerPlayer owner, BlockTarget target, List<BlockDropsEvent> events) {}
    private record Carried(ItemEntity item, boolean noGravity, boolean noPhysics, int pickupDelay) {
        void restore() {
            CARRIED_ITEMS.remove(item);
            item.setNoGravity(noGravity);
            item.noPhysics = noPhysics;
            item.setPickUpDelay(pickupDelay);
        }
    }

    private final ServerLevel level;
    private final BlockTarget target;
    private final Vec3 contact;
    private final ItemStack tool;
    private final List<Carried> carried = new ArrayList<>();

    public PsychicDriveGauntletsMining(ServerLevel level, BlockTarget target, Vec3 contact, ItemStack tool) {
        this.level = level;
        this.target = target;
        this.contact = contact;
        // 全componentを保持し、採掘中に消費・変更されたコピーを収納ツールへ書き戻さない。
        this.tool = tool.copy();
    }

    public boolean canMine(Player owner) {
        if (!(owner instanceof ServerPlayer player) || owner.level() != level || target == null || tool.isEmpty()
                || !owner.isAlive() || owner.isSpectator() || !level.hasChunkAt(target.pos())) return false;
        var state = level.getBlockState(target.pos());
        double range = PsychicDriveGauntletsCharge.range(1);
        return owner.getEyePosition().distanceToSqr(contact) <= range * range + 1.0e-6
                && state == target.state() && !state.isAir()
                && !state.getCollisionShape(level, target.pos()).isEmpty()
                && state.getDestroySpeed(level, target.pos()) >= 0
                // 原木・土・ガラスなどは道具適性を要求せず、鉱石などの必須条件だけを守る。
                && (!state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state))
                && level.mayInteract(player, target.pos())
                && !player.blockActionRestricted(level, target.pos(), player.gameMode.getGameModeForPlayer())
                && player.mayUseItemAt(target.pos(), target.face(), tool);
    }

    public boolean finish(Player owner) {
        if (!canMine(owner)) return false;
        var player = (ServerPlayer) owner;
        var previous = CAPTURE.get();
        var capture = new Capture(player, target, new ArrayList<>());
        CAPTURE.set(capture);
        try {
            // 通常のplayer採掘経路を使い、保護イベント・幸運・シルクタッチ・外部エンチャントを尊重する。
            boolean broken = BlockTools.tryBreakBlockByPlayerHands(level, player, target.pos(), tool.copy());
            if (!broken || level.getBlockState(target.pos()) == target.state()) return false;
            for (var event : capture.events()) {
                if (event.isCanceled()) continue;
                for (var item : event.getDrops()) {
                    // event後の変更とspawn拒否を確認し、実際に生成されたドロップだけを追跡する。
                    if (item.isRemoved() || level.getEntity(item.getUUID()) != item
                            || carried.stream().anyMatch(entry -> entry.item() == item)) continue;
                    carried.add(new Carried(item, item.isNoGravity(), item.noPhysics,
                            ((ItemEntityAccessor) item).apprenticecodex$getPickupDelay()));
                    CARRIED_ITEMS.add(item);
                    item.setNoGravity(true);
                    item.noPhysics = true;
                    item.setNeverPickUp();
                    item.setDeltaMovement(Vec3.ZERO);
                    item.teleportTo(contact.x, contact.y, contact.z);
                    item.hasImpulse = true;
                }
            }
            return true;
        } finally {
            if (previous == null) CAPTURE.remove();
            else CAPTURE.set(previous);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureDrops(BlockDropsEvent event) {
        var capture = CAPTURE.get();
        if (capture != null && event.getBreaker() == capture.owner()
                && event.getLevel() == capture.owner().level() && event.getPos().equals(capture.target().pos())
                && event.getState() == capture.target().state()) capture.events().add(event);
    }

    @SubscribeEvent
    public static void holdCarriedItem(EntityTickEvent.Pre event) {
        // ItemEntity.tickはnoPhysicsを上書きするため、帰還中だけ通常の物理・合流処理を止める。
        if (event.getEntity() instanceof ItemEntity item && !item.level().isClientSide
                && CARRIED_ITEMS.contains(item)) event.setCanceled(true);
    }

    public void follow(Player owner, Vec3 position, boolean arrived) {
        if (owner.level() != level || !owner.isAlive() || owner.isSpectator()) {
            release();
            return;
        }
        for (var entry : carried) {
            var item = entry.item();
            if (item.isRemoved()) {
                CARRIED_ITEMS.remove(item);
                continue;
            }
            var destination = arrived ? owner.position() : position;
            item.setDeltaMovement(Vec3.ZERO);
            item.teleportTo(destination.x, destination.y, destination.z);
            // ItemEntityの標準同期周期より帰還が短いため、各tickの位置をtracking側へ送る。
            item.hasImpulse = true;
            if (arrived) {
                entry.restore();
                // 通常のspawn直後の待機時間だけを短縮し、永続的な拾得禁止は維持する。
                if (entry.pickupDelay() != 32767) item.setNoPickUpDelay();
                item.playerTouch(owner);
            }
        }
        if (arrived) carried.clear();
    }

    public void release() {
        for (var entry : carried) entry.restore();
        carried.clear();
    }
}
