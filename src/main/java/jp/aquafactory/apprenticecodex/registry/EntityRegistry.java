package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.arcanebeam.ArcaneBeamEntity;
import jp.aquafactory.apprenticecodex.spell.archermultiple.ArcherMultipleBowEntity;
import jp.aquafactory.apprenticecodex.spell.assistwings.AssistWingsWingEntity;
import jp.aquafactory.apprenticecodex.spell.breachingenemy.BreachingEnemyShotgunEntity;
import jp.aquafactory.apprenticecodex.spell.bulletstream.BulletStreamMinigunEntity;
import jp.aquafactory.apprenticecodex.spell.commencefire.CommenceFireRifleEntity;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhialProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterLauncherEntity;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.gracedrain.GracedRainCloudEntity;
import jp.aquafactory.apprenticecodex.spell.moonunite.MoonUniteKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.quickarms.QuickArmsHandgunEntity;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdgeProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjackSawEntity;
import jp.aquafactory.apprenticecodex.spell.worldflatter.WorldFlatterDrillEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ApprenticeCodex.MODID);

    public static final RegistryObject<EntityType<SkyEdgeProjectileEntity>> SKY_EDGE_PROJECTILE =
            ENTITIES.register("sky_edge_projectile",
                    () -> EntityType.Builder
                            .<SkyEdgeProjectileEntity>of(SkyEdgeProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .setShouldReceiveVelocityUpdates(true)
                            .build("sky_edge_projectile"));

    public static final RegistryObject<EntityType<ArcherMultipleBowEntity>> ARCHER_MULTIPLE_BOW =
            ENTITIES.register("archer_multiple_bow",
                    () -> EntityType.Builder
                            .<ArcherMultipleBowEntity>of(ArcherMultipleBowEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(64)
                            .updateInterval(4)
                            .build("archer_multiple_bow"));

    public static final RegistryObject<EntityType<CommenceFireRifleEntity>> COMMENCE_FIRE_RIFLE =
            ENTITIES.register("commence_fire_rifle",
                    () -> EntityType.Builder
                            .<CommenceFireRifleEntity>of(CommenceFireRifleEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(64)
                            .updateInterval(2)
                            .build("commence_fire_rifle"));

    public static final RegistryObject<EntityType<CompoundPhialProjectileEntity>> COMPOUND_PHIAL_PROJECTILE =
            ENTITIES.register("compound_phial_projectile",
                    () -> EntityType.Builder
                            .<CompoundPhialProjectileEntity>of(CompoundPhialProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(48)
                            .updateInterval(1)
                            .build("compound_phial_projectile"));

    public static final RegistryObject<EntityType<QuickArmsHandgunEntity>> QUICK_ARMS_HANDGUN =
            ENTITIES.register("quick_arms_handgun",
                    () -> EntityType.Builder
                            .<QuickArmsHandgunEntity>of(QuickArmsHandgunEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(48)
                            .updateInterval(1)
                            .build("quick_arms_handgun"));

    public static final RegistryObject<EntityType<BreachingEnemyShotgunEntity>> BREACHING_ENEMY_SHOTGUN =
            ENTITIES.register("breaching_enemy_shotgun",
                    () -> EntityType.Builder
                            .<BreachingEnemyShotgunEntity>of(BreachingEnemyShotgunEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(32)
                            .updateInterval(1)
                            .build("breaching_enemy_shotgun"));

    public static final RegistryObject<EntityType<BulletStreamMinigunEntity>> BULLET_STREAM_MINIGUN =
            ENTITIES.register("bullet_stream_minigun",
                    () -> EntityType.Builder
                            .<BulletStreamMinigunEntity>of(BulletStreamMinigunEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(64)
                            .updateInterval(2)
                            .build("bullet_stream_minigun"));

    public static final RegistryObject<EntityType<GracedRainCloudEntity>> GRACED_RAIN_CLOUD =
            ENTITIES.register("graced_rain_cloud",
                    () -> EntityType.Builder
                            .<GracedRainCloudEntity>of(GracedRainCloudEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("graced_rain_cloud"));

    public static final RegistryObject<EntityType<TinyLumberjackSawEntity>> TINY_LUMBERJACK_SAW =
            ENTITIES.register("tiny_lumberjack_saw",
                    () -> EntityType.Builder
                            .<TinyLumberjackSawEntity>of(TinyLumberjackSawEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(48)
                            .updateInterval(1)
                            .build("tiny_lumberjack_saw"));

    public static final RegistryObject<EntityType<ArcaneBeamEntity>> ARCANE_BEAM =
            ENTITIES.register("arcane_beam",
                    () -> EntityType.Builder
                            .<ArcaneBeamEntity>of(ArcaneBeamEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("arcane_beam"));

    public static final RegistryObject<EntityType<FlySwatterLauncherEntity>> FLY_SWATTER_LAUNCHER =
            ENTITIES.register("fly_swatter_launcher",
                    () -> EntityType.Builder
                            .<FlySwatterLauncherEntity>of(FlySwatterLauncherEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .build("fly_swatter_launcher"));

    public static final RegistryObject<EntityType<FlySwatterProjectileEntity>> FLY_SWATTER_PROJECTILE =
            ENTITIES.register("fly_swatter_projectile",
                    () -> EntityType.Builder
                            .<FlySwatterProjectileEntity>of(FlySwatterProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .build("fly_swatter_projectile"));

    public static final RegistryObject<EntityType<AssistWingsWingEntity>> ASSIST_WINGS_WING =
            ENTITIES.register("assist_wings_wing",
                    () -> EntityType.Builder
                            .<AssistWingsWingEntity>of(AssistWingsWingEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("assist_wings_wing"));

    public static final RegistryObject<EntityType<WorldFlatterDrillEntity>> WORLD_FLATTER_DRILL =
            ENTITIES.register("world_flatter_drill",
                    () -> EntityType.Builder
                            .<WorldFlatterDrillEntity>of(WorldFlatterDrillEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(32)
                            .updateInterval(1)
                            .build("world_flatter_drill"));

    public static final RegistryObject<EntityType<MoonUniteKatanaEntity>> MOON_UNITE_KATANA =
            ENTITIES.register("moon_unite_katana",
                    () -> EntityType.Builder
                            .<MoonUniteKatanaEntity>of(MoonUniteKatanaEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(32)
                            .updateInterval(1)
                            .build("moon_unite_katana"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
