package jp.aquafactory.apprenticecodex.compat.epicfight;

import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

public final class EpicFightSmashcastScepterCapability extends WeaponCapability {
    public EpicFightSmashcastScepterCapability(WeaponCapability.Builder builder) {
        super(builder);
    }

    @Override
    public Style getStyle(LivingEntityPatch<?> entityPatch) {
        return CapabilityItem.Styles.ONE_HAND;
    }

    @Override
    public boolean checkOffhandValid(LivingEntityPatch<?> entityPatch) {
        // Epic Fight 21.17 の default_1h_wield_style は offhand 表示を許可するため、
        // Smashcast Scepter では旧実装どおりメインハンド時の併用も明示的に拒否する。
        return false;
    }

    @Override
    public boolean canHoldInOffhandAlone() {
        return false;
    }
}
