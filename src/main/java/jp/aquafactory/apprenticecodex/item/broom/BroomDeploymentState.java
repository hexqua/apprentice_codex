package jp.aquafactory.apprenticecodex.item.broom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public final class BroomDeploymentState {
    private static final String DEPLOYED_ENTITY_TAG = "apprenticecodex:called_broom_entity";

    private BroomDeploymentState() {
    }

    public static Optional<UUID> getDeployedEntityId(ItemStack stack) {
        if (!BroomCurioSupport.isBroom(stack)) {
            return Optional.empty();
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(DEPLOYED_ENTITY_TAG)
                ? Optional.of(tag.getUUID(DEPLOYED_ENTITY_TAG))
                : Optional.empty();
    }

    public static boolean isDeployed(ItemStack stack) {
        return getDeployedEntityId(stack).isPresent();
    }

    public static boolean matches(ItemStack stack, UUID entityId) {
        return getDeployedEntityId(stack).map(entityId::equals).orElse(false);
    }

    public static void setDeployedEntity(ItemStack stack, UUID entityId) {
        if (BroomCurioSupport.isBroom(stack)) {
            stack.getOrCreateTag().putUUID(DEPLOYED_ENTITY_TAG, entityId);
        }
    }

    public static void clear(ItemStack stack) {
        if (BroomCurioSupport.isBroom(stack)) {
            stack.removeTagKey(DEPLOYED_ENTITY_TAG);
        }
    }
}
