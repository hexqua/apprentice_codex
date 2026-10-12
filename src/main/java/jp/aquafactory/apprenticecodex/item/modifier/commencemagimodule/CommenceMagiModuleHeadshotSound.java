package jp.aquafactory.apprenticecodex.item.modifier.commencemagimodule;

import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.gun.HitEntityAccumulator;
import io.redspace.irons_artifice.modifier.OnHitEffect;
import jp.aquafactory.apprenticecodex.compat.ironsartifice.CommenceMagiModuleEvents;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class CommenceMagiModuleHeadshotSound implements OnHitEffect {
    @Override
    public void onHit(ServerLevel level, Bullet bullet, HitResult hit, HitEntityAccumulator accumulator) {
        var impact = bullet.getProfile().components().remove(CommenceMagiModuleEvents.IMPACT);
        if (impact != null && impact.headshot() && hit instanceof EntityHitResult entityHit && entityHit.getEntity() == impact.target()) {
            // CommenceFireと同じく、HP減少ではなく頭部命中判定を成功音の条件にする。
            AudioTools.playSoundFromPosition(level, impact.position(), SoundRegistry.VANILLA_CRITICAL_SHOT.get(), SoundSource.PLAYERS, 1, 2);
        }
    }
}
