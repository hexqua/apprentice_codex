package jp.aquafactory.apprenticecodex.block.essencesmoker;

import io.redspace.ironsspellbooks.entity.spells.firebolt.FireboltProjectile;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class EssenceSmokerIgniteEvent {
    private EssenceSmokerIgniteEvent() {}

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof FireboltProjectile)
                || !(event.getRayTraceResult() instanceof BlockHitResult blockHitResult)
                || !(event.getProjectile().level() instanceof ServerLevel serverLevel)
                || !(serverLevel.getBlockEntity(blockHitResult.getBlockPos()) instanceof EssenceSmokerBlockEntity blockEntity)) {
            return;
        }

        if (blockEntity.canIgnite()) {
            blockEntity.startProcessing(serverLevel.getGameTime());
        }
    }
}
