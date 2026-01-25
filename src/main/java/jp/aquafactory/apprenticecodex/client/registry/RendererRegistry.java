package jp.aquafactory.apprenticecodex.client.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.common.spells.archermultiple.ArcherMultipleBowRenderer;
import jp.aquafactory.apprenticecodex.common.spells.skyedge.SkyEdgeProjectileRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RendererRegistry {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(EntityRegistry.SKY_EDGE_PROJECTILE.get(), SkyEdgeProjectileRenderer::new);
        e.registerEntityRenderer(EntityRegistry.ARCHER_MULTIPLE_BOW.get(), ArcherMultipleBowRenderer::new);
    }
}
