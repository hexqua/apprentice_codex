package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.GloveCapability;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

public final class EpicFightScrollcasterGauntletCapability extends GloveCapability {
    public EpicFightScrollcasterGauntletCapability(WeaponCapability.Builder builder) {
        super(builder);
    }

    @Override
    public boolean checkOffhandValid(LivingEntityPatch<?> entityPatch) {
        var livingEntity = (LivingEntity) entityPatch.getOriginal();
        // 空のオフハンドは Epic Fight 側で EMPTY 扱いになるため、ガントレットだけ例外的に両手有効化する.
        if (livingEntity.getMainHandItem().getItem() instanceof ScrollcasterGauntlet
                && livingEntity.getOffhandItem().isEmpty()) {
            return true;
        }

        return super.checkOffhandValid(entityPatch);
    }

    @Override
    public boolean canHoldInOffhandAlone() {
        // オフハンド単体では詠唱具として扱い、Epic Fight の武器扱いにはしない.
        return false;
    }
}
