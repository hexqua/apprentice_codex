package jp.aquafactory.apprenticecodex.spell.callbroom;

import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.item.broom.BroomCurioSupport;
import jp.aquafactory.apprenticecodex.item.broom.BroomDeploymentState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CallBroomDeploymentManager {
    public static final double RECALL_DISTANCE = 8.0D;
    private static final double RECALL_DISTANCE_SQR = RECALL_DISTANCE * RECALL_DISTANCE;

    private CallBroomDeploymentManager() {
    }

    public static Validation validate(ServerPlayer player) {
        var equipped = BroomCurioSupport.findUniqueEquippedBroom(player).orElse(null);
        if (equipped == null) {
            return Validation.failure(Failure.NOT_FOUND);
        }
        if (player.isPassenger()) {
            return Validation.failure(Failure.RIDING);
        }

        var stack = equipped.stack();
        var deployed = BroomDeploymentState.getDeployedEntityId(stack)
                .map(id -> findBroom(player.serverLevel().getServer(), id))
                .filter(broom -> broom.isOwnedBy(player) && broom.matchesBroomItem(stack))
                .orElse(null);
        if (deployed != null) {
            return deployed.isVehicle()
                    ? Validation.failure(Failure.OCCUPIED)
                    : Validation.recall(equipped, deployed);
        }

        for (var broom : findOwnedBrooms(player.serverLevel().getServer(), player.getUUID())) {
            if (broom.isVehicle()) {
                return Validation.failure(Failure.OCCUPIED);
            }
        }
        return Validation.summon(equipped);
    }

    public static boolean execute(ServerPlayer player) {
        var validation = validate(player);
        if (!validation.canCast()) {
            return false;
        }
        if (validation.action == Action.RECALL) {
            validation.broom.recallWithoutItem();
            return true;
        }

        var stack = validation.equipped.stack();
        for (var stale : findOwnedBrooms(player.serverLevel().getServer(), player.getUUID())) {
            if (stale.isVehicle()) {
                return false;
            }
            stale.recallWithoutItem();
        }
        BroomDeploymentState.clear(stack);

        var broom = BroomCurioSupport.createBroom(stack, player.level());
        if (broom == null) {
            return false;
        }
        broom.setBroomItemStack(stack);
        broom.setCalledOwner(player);
        var customName = stack.get(DataComponents.CUSTOM_NAME);
        broom.setCustomName(customName);
        broom.setCustomNameVisible(customName != null);
        broom.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        if (!player.serverLevel().addFreshEntity(broom)) {
            return false;
        }
        if (!player.startRiding(broom, true)) {
            broom.recallWithoutItem();
            return false;
        }

        BroomDeploymentState.setDeployedEntity(stack, broom.getUUID());
        return true;
    }

    public static boolean shouldRecall(AbstractBroomEntity broom, ServerPlayer owner) {
        if (!owner.isAlive() || owner.level() != broom.level()) {
            return true;
        }
        if (broom.isVehicle()) {
            return broom.getControllingPassenger() != owner;
        }
        return owner.distanceToSqr(broom) > RECALL_DISTANCE_SQR
                || !matchesEquippedDeployment(owner, broom);
    }

    public static boolean matchesEquippedDeployment(ServerPlayer owner, AbstractBroomEntity broom) {
        return BroomCurioSupport.findUniqueEquippedBroom(owner)
                .filter(result -> broom.matchesBroomItem(result.stack()))
                .map(SlotResult::stack)
                .filter(stack -> BroomDeploymentState.matches(stack, broom.getUUID()))
                .isPresent();
    }

    public static void onUnequip(ServerPlayer player, ItemStack stack) {
        BroomDeploymentState.getDeployedEntityId(stack)
                .map(id -> findBroom(player.serverLevel().getServer(), id))
                .filter(broom -> broom.isOwnedBy(player))
                .ifPresent(AbstractBroomEntity::recallWithoutItem);
        BroomDeploymentState.clear(stack);
    }

    public static void reconcileEquippedStack(ServerPlayer player, ItemStack stack) {
        var deployedId = BroomDeploymentState.getDeployedEntityId(stack).orElse(null);
        if (deployedId == null) {
            return;
        }
        var broom = findBroom(player.serverLevel().getServer(), deployedId);
        if (broom == null || !broom.isOwnedBy(player) || !broom.matchesBroomItem(stack)) {
            BroomDeploymentState.clear(stack);
        }
    }

    public static void onBroomRemoved(AbstractBroomEntity broom) {
        var owner = broom.getCalledOwner();
        if (owner == null) {
            return;
        }
        CuriosApi.getCuriosInventory(owner).ifPresent(inventory -> {
            for (var result : inventory.findCurios(BroomCurioSupport::isBroom)) {
                if (BroomDeploymentState.matches(result.stack(), broom.getUUID())) {
                    BroomDeploymentState.clear(result.stack());
                }
            }
        });
    }

    public static void onLogout(ServerPlayer player) {
        if (player.getVehicle() instanceof AbstractBroomEntity broom
                && broom.isOwnedBy(player)
                && BroomCurioSupport.findUniqueEquippedBroom(player)
                .filter(result -> broom.matchesBroomItem(result.stack()))
                .map(SlotResult::stack)
                .map(stack -> {
                    BroomDeploymentState.setDeployedEntity(stack, broom.getUUID());
                    return true;
                }).orElse(false)) {
            removeOtherUnoccupiedBrooms(player, broom);
            return;
        }
        recallAllOwnedBrooms(player);
        clearAllEquippedStates(player);
    }

    public static void onLogin(ServerPlayer player) {
        if (player.getVehicle() instanceof AbstractBroomEntity broom && broom.isOwnedBy(player)) {
            var equipped = BroomCurioSupport.findUniqueEquippedBroom(player).orElse(null);
            if (equipped != null && broom.matchesBroomItem(equipped.stack())) {
                BroomDeploymentState.setDeployedEntity(equipped.stack(), broom.getUUID());
                removeOtherUnoccupiedBrooms(player, broom);
                return;
            }
            broom.recallWithoutItem();
        }

        var equipped = BroomCurioSupport.findUniqueEquippedBroom(player).orElse(null);
        if (equipped != null) {
            reconcileEquippedStack(player, equipped.stack());
        }
        removeOtherUnoccupiedBrooms(player, null);
    }

    private static void removeOtherUnoccupiedBrooms(ServerPlayer player, @Nullable AbstractBroomEntity keep) {
        for (var broom : findOwnedBrooms(player.serverLevel().getServer(), player.getUUID())) {
            if (broom != keep && !broom.isVehicle()) {
                broom.recallWithoutItem();
            }
        }
    }

    private static void recallAllOwnedBrooms(ServerPlayer player) {
        for (var broom : findOwnedBrooms(player.serverLevel().getServer(), player.getUUID())) {
            broom.recallWithoutItem();
        }
    }

    private static void clearAllEquippedStates(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            for (var result : inventory.findCurios(BroomCurioSupport::isBroom)) {
                BroomDeploymentState.clear(result.stack());
            }
        });
    }

    private static List<AbstractBroomEntity> findOwnedBrooms(MinecraftServer server, UUID ownerId) {
        var result = new ArrayList<AbstractBroomEntity>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof AbstractBroomEntity broom && broom.isOwnedBy(ownerId)) {
                    result.add(broom);
                }
            }
        }
        return result;
    }

    private static @Nullable AbstractBroomEntity findBroom(MinecraftServer server, UUID entityId) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(entityId) instanceof AbstractBroomEntity broom) {
                return broom;
            }
        }
        return null;
    }

    public enum Failure {
        NONE,
        NOT_FOUND,
        RIDING,
        OCCUPIED
    }

    private enum Action {
        SUMMON,
        RECALL
    }

    public static final class Validation {
        private final Failure failure;
        private final Action action;
        private final SlotResult equipped;
        private final AbstractBroomEntity broom;

        private Validation(Failure failure, Action action, SlotResult equipped, AbstractBroomEntity broom) {
            this.failure = failure;
            this.action = action;
            this.equipped = equipped;
            this.broom = broom;
        }

        public static Validation failure(Failure failure) {
            return new Validation(failure, null, null, null);
        }

        private static Validation summon(SlotResult equipped) {
            return new Validation(Failure.NONE, Action.SUMMON, equipped, null);
        }

        private static Validation recall(SlotResult equipped, AbstractBroomEntity broom) {
            return new Validation(Failure.NONE, Action.RECALL, equipped, broom);
        }

        public boolean canCast() {
            return failure == Failure.NONE;
        }

        public Failure failure() {
            return failure;
        }
    }
}
