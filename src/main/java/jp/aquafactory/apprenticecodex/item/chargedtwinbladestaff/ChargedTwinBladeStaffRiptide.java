package jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.network.packet.SyncChargedStaffRiptidePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ChargedTwinBladeStaffRiptide {
    private static final double SUSTAIN_RESPONSE = 0.2D;
    private ChargedTwinBladeStaffRiptide() {
    }

    // Entityへ保持し、保存・cloneしない。Item singletonや両side共用のMapには操作状態を置かない。
    public static final class State {
        private final ItemStack stack;
        private long nextPayment;
        private boolean active;
        private boolean maintenanceInput;
        private Vec3 sustainVelocity;
        private long lastPropulsionTick = Long.MIN_VALUE;

        private State(ItemStack stack, long nextPayment, boolean active, boolean maintenanceInput) {
            this.stack = stack;
            this.nextPayment = nextPayment;
            this.active = active;
            this.maintenanceInput = maintenanceInput;
        }
    }

    private static ChargedTwinBladeStaffRiptideAccess access(LivingEntity entity) {
        return (ChargedTwinBladeStaffRiptideAccess) entity;
    }

    public static void started(Player player, ItemStack stack) {
        access(player).apprenticecodex$setStaffRiptide(new State(stack, player.level().getGameTime() + 20, true, false));
        // vanillaはserverでだけフラグを立てるため、ローカルの再使用予測にも開始を反映する。
        access(player).apprenticecodex$updateStaffSpin(20);
        sync(player);
    }

    public static void beginUse(Player player, ItemStack stack) {
        var state = access(player).apprenticecodex$getStaffRiptide();
        // 同じ押下中のuse再送でも、中断済みの維持入力を溜めへ読み替えない。
        if (state != null && state.maintenanceInput && player.isUsingItem()) {
            return;
        }
        if (state != null && state.active && player.isAutoSpinAttack() && matches(player, state, stack)
                && ChargedTwinBladeStaff.getRiptideLevel(stack) > 0) {
            state.maintenanceInput = true;
            sync(player);
        } else {
            // 終了後の新しいuseだけを溜め操作へ戻す。維持中の解除処理からはここへ入らない。
            clear(player);
        }
    }

    public static boolean isMaintenanceInput(LivingEntity entity) {
        var state = access(entity).apprenticecodex$getStaffRiptide();
        return state != null && state.maintenanceInput;
    }

    public static boolean release(Player player) {
        if (!isMaintenanceInput(player)) {
            return false;
        }
        interrupt(player);

        var state = access(player).apprenticecodex$getStaffRiptide();
        if(state != null){
            state.maintenanceInput = false;
        }

        sync(player);
        return true;
    }

    public static void tick(Player player) {
        if (player.level().isClientSide && !player.isLocalPlayer()) {
            return;
        }
        var state = access(player).apprenticecodex$getStaffRiptide();
        if (state == null) {
            return;
        }
        var stack = player.getMainHandItem();
        if (!player.isAlive() || !matches(player, state, stack) || ChargedTwinBladeStaff.getRiptideLevel(stack) <= 0) {
            clear(player);
            return;
        }
        if (state.maintenanceInput && (!player.isUsingItem() || player.getUsedItemHand() != InteractionHand.MAIN_HAND)) {
            release(player);
            return;
        }
        if (!state.active || !state.maintenanceInput) {
            return;
        }
        if (!player.isAutoSpinAttack()) {
            interrupt(player);
            return;
        }
        var now = player.level().getGameTime();
        if (!player.level().isClientSide && now >= state.nextPayment) {
            var cost = ApprenticeCodexServerConfig.chargedTwinBladeStaffConfig().riptideSustainManaCostPer10Ticks();
            if (!spendMana(player, cost)) {
                interrupt(player);
                return;
            }
            // 使用開始ではなく初動時刻が基準。同じtickの追加呼び出しでも二重に徴収しない。
            state.nextPayment += 10;
            sync(player);
        }
        if (state.lastPropulsionTick != now) {
            // 初動の勢いから滑らかに移行する。毎tickの重力・減速を補間元へ累積させず、
            // 水平・上昇とも指定した巡航速度へ収束させるため、継続用の移動ベクトルを保持する。
            if (state.sustainVelocity == null) {
                state.sustainVelocity = player.getDeltaMovement();
            }
            var target = player.getLookAngle().scale(speed(ChargedTwinBladeStaff.getRiptideLevel(stack)));
            state.sustainVelocity = state.sustainVelocity.lerp(target, SUSTAIN_RESPONSE);
            player.setDeltaMovement(state.sustainVelocity);
            state.lastPropulsionTick = now;
        }
        // 毎tickの初動再実行は音・肩のEntity解放などの副作用を伴うため、残り時間だけを更新する。
        access(player).apprenticecodex$updateStaffSpin(2);
    }

    public static double speed(int level) {
        // ブロック/秒からブロック/tickへ変換。上限突破エンチャントも同じ式で扱う。
        return (12.0D + level * 4.0D) / 20.0D;
    }

    public static boolean spendMana(Player player, int cost) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (player.level().isClientSide) {
            // マナ同期先はEntityのattachmentではない。clientは同期済み残量で初動だけを予測する。
            return ChargedTwinBladeStaffClientMana.hasMana(cost);
        }
        var data = MagicData.getPlayerMagicData(player);
        if (data.getMana() < cost) {
            return false;
        }
        data.setMana(Math.max(0, data.getMana() - cost));
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(data));
        }
        return true;
    }

    public static void interrupt(Player player) {
        var state = access(player).apprenticecodex$getStaffRiptide();
        if (state != null && state.active) {
            state.active = false;
            access(player).apprenticecodex$updateStaffSpin(0);
            sync(player);
        }
    }

    public static void clear(Player player) {
        var state = access(player).apprenticecodex$getStaffRiptide();
        if (state != null) {
            if (state.active) {
                access(player).apprenticecodex$updateStaffSpin(0);
            }
            access(player).apprenticecodex$setStaffRiptide(null);
            sync(player);
        }
    }

    private static boolean matches(Player player, State state, ItemStack stack) {
        // clientのinventory同期は同じ装備を別のItemStackへ置換することがある。
        return player.level().isClientSide ? ItemStack.isSameItemSameComponents(state.stack, stack) : state.stack == stack;
    }

    public static SyncChargedStaffRiptidePacket snapshot(Player player) {
        var state = access(player).apprenticecodex$getStaffRiptide();
        return new SyncChargedStaffRiptidePacket(player.getId(), state != null && state.active,
                state != null && state.maintenanceInput);
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, snapshot(player));
        }
    }

    public static void acceptSync(Player player, boolean active, boolean maintenanceInput) {
        var state = access(player).apprenticecodex$getStaffRiptide();
        if (player.isLocalPlayer() && state != null) {
            // 初動の応答が再使用・解除より遅れても、ローカル入力を過去の状態へ戻さない。
            if (!active) {
                state.active = false;
                access(player).apprenticecodex$updateStaffSpin(0);
            } else if (maintenanceInput && state.maintenanceInput && player.isUsingItem()) {
                // clientだけが衝突を予測した場合はserverの維持判定へ戻す。解除済み入力は復活させない。
                state.active = true;
                access(player).apprenticecodex$updateStaffSpin(2);
            }
            return;
        }
        access(player).apprenticecodex$setStaffRiptide(new State(player.getMainHandItem(), 0, active, maintenanceInput));
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof Player target) {
            PacketDistributor.sendToPlayer(viewer, snapshot(target));
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity());
    }
}
