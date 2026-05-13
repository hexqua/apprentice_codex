package jp.aquafactory.apprenticecodex.compat.epicfight;

import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

public final class EpicFightSmashcastScepterCapability extends WeaponCapability {
    public EpicFightSmashcastScepterCapability(CapabilityItem.Builder builder) {
        super(builder);
    }

    @Override
    public Style getStyle(LivingEntityPatch<?> entityPatch) {
        return CapabilityItem.Styles.ONE_HAND;
    }

    @Override
    public boolean canHoldInOffhandAlone() {
        return false;
    }
}
