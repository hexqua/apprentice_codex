package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import java.util.ArrayList;
import java.util.List;
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
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 採掘条件と回収を腕の戦闘処理から分け、loader固有のドロップ捕捉をここに閉じ込める。 */
@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class PsychicDriveGauntletsMining {
    public static final int MINING_TICKS = 12;
    private static final ThreadLocal<Capture> CAPTURE = new ThreadLocal<>();
    private record Capture(ServerPlayer owner, BlockTarget target, List<ItemEntity> items) {}
    private record Carried(ItemEntity item, boolean noGravity, boolean noPhysics, int pickupDelay, boolean canUpdate) {
        void restore() {
            item.canUpdate(canUpdate);
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
            for (var item : capture.items()) {
                // spawn拒否や後続listenerの変更を確認し、実際に生成されたドロップだけを追跡する。
                if (item.isRemoved() || level.getEntity(item.getUUID()) != item
                        || carried.stream().anyMatch(entry -> entry.item() == item)) continue;
                carried.add(new Carried(item, item.isNoGravity(), item.noPhysics,
                        ((ItemEntityAccessor) item).apprenticecodex$getPickupDelay(), item.canUpdate()));
                // ServerLevelはcanUpdateの判定前にtickCountを進める。物理・寿命・合流だけ止め、元の更新可否も復元する。
                item.canUpdate(false);
                item.setNoGravity(true);
                item.noPhysics = true;
                item.setNeverPickUp();
                item.setDeltaMovement(Vec3.ZERO);
                item.teleportTo(contact.x, contact.y, contact.z);
                item.hasImpulse = true;
            }
            return true;
        } finally {
            if (previous == null) CAPTURE.remove();
            else CAPTURE.set(previous);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureDrops(EntityJoinLevelEvent event) {
        var capture = CAPTURE.get();
        // ForgeにはBlockDropsEventがないため、この採掘呼出し中に対象位置からspawnするitemに限定する。
        if (capture != null && event.getLevel() == capture.owner().level()
                && event.getEntity() instanceof ItemEntity item && item.blockPosition().equals(capture.target().pos())) {
            capture.items().add(item);
        }
    }

    public void follow(Player owner, Vec3 position, boolean arrived) {
        if (owner.level() != level || !owner.isAlive() || owner.isSpectator()) {
            release();
            return;
        }
        for (var entry : carried) {
            var item = entry.item();
            if (item.isRemoved()) {
                entry.restore();
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
