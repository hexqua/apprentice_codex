package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.arcanebeam.ArcaneBeamEntity;
import jp.aquafactory.apprenticecodex.spell.archermultiple.ArcherMultipleBowEntity;
import jp.aquafactory.apprenticecodex.spell.assistwings.AssistWingsWingEntity;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetFamiliarEntity;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import jp.aquafactory.apprenticecodex.spell.breachingenemy.BreachingEnemyShotgunEntity;
import jp.aquafactory.apprenticecodex.spell.bulletstream.BulletStreamMinigunEntity;
import jp.aquafactory.apprenticecodex.spell.commencefire.CommenceFireRifleEntity;
import jp.aquafactory.apprenticecodex.spell.companiontrunk.CompanionTrunkEntity;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhialProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.demicreatorwings.DemicreatorWingsCoreEntity;
import jp.aquafactory.apprenticecodex.spell.demicreatorwings.DemicreatorWingsWingEntity;
import jp.aquafactory.apprenticecodex.spell.featherrush.FeatherRushProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.featherrush.FeatherRushWingEntity;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterLauncherEntity;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.gracedrain.GracedRainCloudEntity;
import jp.aquafactory.apprenticecodex.spell.grindrunner.GrindRunnerWheelEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomEntity;
import jp.aquafactory.apprenticecodex.spell.higanbana.HiganbanaKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.illuminatestellar.IlluminateStellarStarEntity;
import jp.aquafactory.apprenticecodex.spell.mantisleap.MantisLeapBladeEntity;
import jp.aquafactory.apprenticecodex.spell.manaslash.ManaSlashProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLightChargeCutEntity;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLightKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.phalanxcharge.PhalanxChargeBeamEntity;
import jp.aquafactory.apprenticecodex.spell.phalanxcharge.PhalanxWeaponryEntity;
import jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.precisionjack.PrecisionJackKnifeEntity;
import jp.aquafactory.apprenticecodex.spell.quickarms.QuickArmsHandgunEntity;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdgeProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.thermalprocess.ThermalProcessThrowerEntity;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjackSawEntity;
import jp.aquafactory.apprenticecodex.spell.uniteluna.UniteLunaMoonEntity;
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

    // ベース.
    private static <T extends net.minecraft.world.entity.Entity> RegistryObject<EntityType<T>> reg(
            String id,
            EntityType.EntityFactory<T> factory,
            MobCategory category,
            float w, float h,
            int trackingRange,
            int updateInterval,
            boolean velocityUpdates
    ) {
        return ENTITIES.register(id, () -> {
            var b = EntityType.Builder.of(factory, category)
                    .sized(w, h)
                    .clientTrackingRange(trackingRange)
                    .updateInterval(updateInterval);

            if (velocityUpdates) b.setShouldReceiveVelocityUpdates(true);
            return b.build(id);
        });
    }

    // テンプレ.
    private static <T extends net.minecraft.world.entity.Entity> RegistryObject<EntityType<T>> regProjectile(
            String id, EntityType.EntityFactory<T> factory,
            int trackingRange, int updateInterval
    ) {
        return reg(id, factory, MobCategory.MISC,
                0.25f, 0.25f,
                trackingRange, updateInterval,
                true);
    }

    private static <T extends net.minecraft.world.entity.Entity> RegistryObject<EntityType<T>> regWeapon(
            String id, EntityType.EntityFactory<T> factory,
            int updateInterval
    ) {
        return reg(id, factory, MobCategory.MISC,
                0.5f, 0.5f,
                32, updateInterval,
                false);
    }

    private static <T extends net.minecraft.world.entity.Entity> RegistryObject<EntityType<T>> regLiving(
            String id,
            EntityType.EntityFactory<T> factory,
            float width,
            float height,
            int trackingRange
    ) {
        return reg(id, factory, MobCategory.MISC, width, height, trackingRange, 1, false);
    }

    public static final RegistryObject<EntityType<SkyEdgeProjectileEntity>> SKY_EDGE_PROJECTILE =
            regProjectile("sky_edge_projectile", SkyEdgeProjectileEntity::new, 128, 1);

    public static final RegistryObject<EntityType<ArcherMultipleBowEntity>> ARCHER_MULTIPLE_BOW =
            regWeapon("archer_multiple_bow", ArcherMultipleBowEntity::new, 1);

    public static final RegistryObject<EntityType<CommenceFireRifleEntity>> COMMENCE_FIRE_RIFLE =
            regWeapon("commence_fire_rifle", CommenceFireRifleEntity::new, 2);

    public static final RegistryObject<EntityType<CompoundPhialProjectileEntity>> COMPOUND_PHIAL_PROJECTILE =
            regProjectile("compound_phial_projectile", CompoundPhialProjectileEntity::new, 48, 1);

    public static final RegistryObject<EntityType<QuickArmsHandgunEntity>> QUICK_ARMS_HANDGUN =
            regWeapon("quick_arms_handgun", QuickArmsHandgunEntity::new, 1);

    public static final RegistryObject<EntityType<BreachingEnemyShotgunEntity>> BREACHING_ENEMY_SHOTGUN =
            regWeapon("breaching_enemy_shotgun", BreachingEnemyShotgunEntity::new, 1);

    public static final RegistryObject<EntityType<BulletStreamMinigunEntity>> BULLET_STREAM_MINIGUN =
            regWeapon("bullet_stream_minigun", BulletStreamMinigunEntity::new, 1);

    public static final RegistryObject<EntityType<ThermalProcessThrowerEntity>> THERMAL_PROCESS_THROWER =
            regWeapon("thermal_process_thrower", ThermalProcessThrowerEntity::new, 1);

    public static final RegistryObject<EntityType<GracedRainCloudEntity>> GRACED_RAIN_CLOUD =
            regWeapon("graced_rain_cloud", GracedRainCloudEntity::new, 1);

    public static final RegistryObject<EntityType<TinyLumberjackSawEntity>> TINY_LUMBERJACK_SAW =
            regWeapon("tiny_lumberjack_saw", TinyLumberjackSawEntity::new, 1);

    public static final RegistryObject<EntityType<ArcaneBeamEntity>> ARCANE_BEAM =
            regProjectile("arcane_beam", ArcaneBeamEntity::new, 64, 1);

    public static final RegistryObject<EntityType<ManaSlashProjectileEntity>> MANA_SLASH_PROJECTILE =
            regProjectile("mana_slash_projectile", ManaSlashProjectileEntity::new, 64, 1);

    public static final RegistryObject<EntityType<FlySwatterLauncherEntity>> FLY_SWATTER_LAUNCHER =
            regWeapon("fly_swatter_launcher", FlySwatterLauncherEntity::new, 1);

    public static final RegistryObject<EntityType<FlySwatterProjectileEntity>> FLY_SWATTER_PROJECTILE =
            regProjectile("fly_swatter_projectile", FlySwatterProjectileEntity::new, 128, 1);

    public static final RegistryObject<EntityType<AssistWingsWingEntity>> ASSIST_WINGS_WING =
            regWeapon("assist_wings_wing", AssistWingsWingEntity::new, 1);

    public static final RegistryObject<EntityType<DemicreatorWingsCoreEntity>> DEMICREATOR_WINGS_CORE =
            reg("demicreator_wings_core", DemicreatorWingsCoreEntity::new, MobCategory.MISC,
                    0.2f, 0.2f, 64, 1, false);

    public static final RegistryObject<EntityType<DemicreatorWingsWingEntity>> DEMICREATOR_WINGS_WING =
            regWeapon("demicreator_wings_wing", DemicreatorWingsWingEntity::new, 1);

    public static final RegistryObject<EntityType<AutoMagnetFamiliarEntity>> AUTO_MAGNET_FAMILIAR =
            regWeapon("auto_magnet_familiar", AutoMagnetFamiliarEntity::new, 1);

    public static final RegistryObject<EntityType<AutoTurretEntity>> AUTO_TURRET =
            regLiving("auto_turret", AutoTurretEntity::new, AutoTurretEntity.WIDTH, AutoTurretEntity.HEIGHT, 32);

    public static final RegistryObject<EntityType<CompanionTrunkEntity>> COMPANION_TRUNK =
            regLiving("companion_trunk", CompanionTrunkEntity::new, CompanionTrunkEntity.WIDTH, CompanionTrunkEntity.HEIGHT, 32);

    public static final RegistryObject<EntityType<HealingBloomEntity>> HEALING_BLOOM =
            regLiving("healing_bloom", HealingBloomEntity::new, HealingBloomEntity.WIDTH, HealingBloomEntity.HEIGHT, 32);

    public static final RegistryObject<EntityType<IlluminateStellarStarEntity>> ILLUMINATE_STELLAR_STAR =
            regProjectile("illuminate_stellar_star", IlluminateStellarStarEntity::new, 96, 1);

    public static final RegistryObject<EntityType<UniteLunaMoonEntity>> UNITE_LUNA_MOON =
            regProjectile("unite_luna_moon", UniteLunaMoonEntity::new, 96, 1);

    public static final RegistryObject<EntityType<FeatherRushProjectileEntity>> FEATHER_RUSH_PROJECTILE =
            regProjectile("feather_rush_projectile", FeatherRushProjectileEntity::new, 96, 1);

    public static final RegistryObject<EntityType<FeatherRushWingEntity>> FEATHER_RUSH_WING =
            regWeapon("feather_rush_wing", FeatherRushWingEntity::new, 1);

    public static final RegistryObject<EntityType<WorldFlatterDrillEntity>> WORLD_FLATTER_DRILL =
            regWeapon("world_flatter_drill", WorldFlatterDrillEntity::new, 1);

    public static final RegistryObject<EntityType<GrindRunnerWheelEntity>> GRIND_RUNNER_WHEEL =
            regWeapon("grind_runner_wheel", GrindRunnerWheelEntity::new, 1);

    public static final RegistryObject<EntityType<SlashBladeKatanaEntity>> SLASH_BLADE_KATANA =
            regWeapon("slash_blade_katana", SlashBladeKatanaEntity::new, 1);

    public static final RegistryObject<EntityType<MoonLightKatanaEntity>> MOON_LIGHT_KATANA =
            regWeapon("moon_light_katana", MoonLightKatanaEntity::new, 1);

    public static final RegistryObject<EntityType<MoonLightChargeCutEntity>> MOON_LIGHT_CHARGE_CUT =
            regProjectile("moon_light_charge_cut", MoonLightChargeCutEntity::new, 64, 1);

    public static final RegistryObject<EntityType<PrecisionJackKnifeEntity>> PRECISION_JACK_KNIFE =
            regWeapon("precision_jack_knife", PrecisionJackKnifeEntity::new, 1);

    public static final RegistryObject<EntityType<HiganbanaKatanaEntity>> HIGANBANA_KATANA =
            regWeapon("higanbana_katana", HiganbanaKatanaEntity::new, 1);

    public static final RegistryObject<EntityType<MantisLeapBladeEntity>> MANTIS_LEAP_BLADE =
            regWeapon("mantis_leap_blade", MantisLeapBladeEntity::new, 1);

    public static final RegistryObject<EntityType<PhalanxWeaponryEntity>> PHALANX_WEAPONRY =
            regWeapon("phalanx_weaponry", PhalanxWeaponryEntity::new, 1);

    public static final RegistryObject<EntityType<PhalanxChargeBeamEntity>> PHALANX_CHARGE_BEAM =
            regProjectile("phalanx_charge_beam", PhalanxChargeBeamEntity::new, 64, 1);

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}

