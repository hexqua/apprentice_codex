package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.arcanebeam.ArcaneBeamRenderer;
import jp.aquafactory.apprenticecodex.spell.archermultiple.ArcherMultipleBowRenderer;
import jp.aquafactory.apprenticecodex.spell.assistwings.AssistWingsWingRenderer;
import jp.aquafactory.apprenticecodex.spell.breachingenemy.BreachingEnemyShotgunRenderer;
import jp.aquafactory.apprenticecodex.spell.bulletstream.BulletStreamMinigunRenderer;
import jp.aquafactory.apprenticecodex.spell.commencefire.CommenceFireRifleRenderer;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhialProjectileRenderer;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterLauncherRenderer;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterProjectileRenderer;
import jp.aquafactory.apprenticecodex.spell.gracedrain.GracedRainCloudRenderer;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLightTorchBlockEntityRenderer;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfChestBlockRenderer;
import jp.aquafactory.apprenticecodex.spell.quickarms.QuickArmsHandgunRenderer;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdgeProjectileRenderer;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjackSawRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RendererRegisterEvent {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityRegistry.MAGE_LIGHT_TORCH.get(), MageLightTorchBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.PERSONAL_SHELF_CHEST.get(), PersonalShelfChestBlockRenderer::new);

        event.registerEntityRenderer(EntityRegistry.SKY_EDGE_PROJECTILE.get(), SkyEdgeProjectileRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ARCHER_MULTIPLE_BOW.get(), ArcherMultipleBowRenderer::new);
        event.registerEntityRenderer(EntityRegistry.COMMENCE_FIRE_RIFLE.get(), CommenceFireRifleRenderer::new);
        event.registerEntityRenderer(EntityRegistry.COMPOUND_PHIAL_PROJECTILE.get(), CompoundPhialProjectileRenderer::new);
        event.registerEntityRenderer(EntityRegistry.QUICK_ARMS_HANDGUN.get(), QuickArmsHandgunRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BREACHING_ENEMY_SHOTGUN.get(), BreachingEnemyShotgunRenderer::new);
        event.registerEntityRenderer(EntityRegistry.BULLET_STREAM_MINIGUN.get(), BulletStreamMinigunRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GRACED_RAIN_CLOUD.get(), GracedRainCloudRenderer::new);
        event.registerEntityRenderer(EntityRegistry.TINY_LUMBERJACK_SAW.get(), TinyLumberjackSawRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ARCANE_BEAM.get(), ArcaneBeamRenderer::new);
        event.registerEntityRenderer(EntityRegistry.FLY_SWATTER_LAUNCHER.get(), FlySwatterLauncherRenderer::new);
        event.registerEntityRenderer(EntityRegistry.FLY_SWATTER_PROJECTILE.get(), FlySwatterProjectileRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ASSIST_WINGS_WING.get(), AssistWingsWingRenderer::new);
    }
}
