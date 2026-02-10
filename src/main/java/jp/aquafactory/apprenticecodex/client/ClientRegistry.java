package jp.aquafactory.apprenticecodex.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.client.particles.MuzzleFlashParticle;
import jp.aquafactory.apprenticecodex.client.particles.ReticleDotParticle;
import jp.aquafactory.apprenticecodex.common.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.common.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.common.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.common.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.common.spells.arcanebeam.ArcaneBeamRenderer;
import jp.aquafactory.apprenticecodex.common.spells.archermultiple.ArcherMultipleBowRenderer;
import jp.aquafactory.apprenticecodex.common.spells.breachingenemy.BreachingEnemyShotgunRenderer;
import jp.aquafactory.apprenticecodex.common.spells.bulletstream.BulletStreamMinigunRenderer;
import jp.aquafactory.apprenticecodex.common.spells.commencefire.CommenceFireRifleRenderer;
import jp.aquafactory.apprenticecodex.common.spells.compoundphial.CompoundPhialProjectileRenderer;
import jp.aquafactory.apprenticecodex.common.spells.gracedrain.GracedRainCloudRenderer;
import jp.aquafactory.apprenticecodex.common.spells.magelight.MageLightTorchBlockEntityRenderer;
import jp.aquafactory.apprenticecodex.common.spells.quickarms.QuickArmsHandgunRenderer;
import jp.aquafactory.apprenticecodex.common.spells.skyedge.SkyEdgeProjectileRenderer;
import jp.aquafactory.apprenticecodex.common.spells.tinylumberjack.TinyLumberjackAxeRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientRegistry {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ItemRegistry::register);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityRegistry.MAGE_LIGHT_TORCH.get(), MageLightTorchBlockEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.SKY_EDGE_PROJECTILE.get(), SkyEdgeProjectileRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ARCHER_MULTIPLE_BOW.get(), ArcherMultipleBowRenderer::new);
        event.registerEntityRenderer(EntityRegistry.COMMENCE_FIRE_RIFLE.get(), CommenceFireRifleRenderer::new);
        event.registerEntityRenderer(EntityRegistry.COMPOUND_PHIAL_PROJECTILE.get(), CompoundPhialProjectileRenderer::new);
        event.registerEntityRenderer(EntityRegistry.QUICK_ARMS_HANDGUN.get(), QuickArmsHandgunRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BREACHING_ENEMY_SHOTGUN.get(), BreachingEnemyShotgunRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BULLET_STREAM_MINIGUN.get(), BulletStreamMinigunRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GRACED_RAIN_CLOUD.get(), GracedRainCloudRenderer::new);
        event.registerEntityRenderer(EntityRegistry.TINY_LUMBERJACK_AXE.get(), TinyLumberjackAxeRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ARCANE_BEAM.get(), ArcaneBeamRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegistry.RETICLE_DOT.get(), ReticleDotParticle.Provider::new);
        event.registerSpriteSet(ParticleRegistry.MUZZLE_FLASH.get(), MuzzleFlashParticle.Provider::new);
    }
}
