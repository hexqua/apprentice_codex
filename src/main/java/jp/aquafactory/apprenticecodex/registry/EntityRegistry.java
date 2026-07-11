package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.arcanebeam.ArcaneBeamEntity;
import jp.aquafactory.apprenticecodex.spell.anchorblink.AnchorBlinkDaggerEntity;
import jp.aquafactory.apprenticecodex.spell.archermultiple.ArcherMultipleBowEntity;
import jp.aquafactory.apprenticecodex.spell.artisansmash.ArtisanSmashLauncherEntity;
import jp.aquafactory.apprenticecodex.spell.artisansmash.ArtisanSmashShellEntity;
import jp.aquafactory.apprenticecodex.spell.assistwings.AssistWingsWingEntity;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetFamiliarEntity;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import jp.aquafactory.apprenticecodex.spell.breachingenemy.BreachingEnemyShotgunEntity;
import jp.aquafactory.apprenticecodex.entity.ChargedTwinBladeStaffThrownEntity;
import jp.aquafactory.apprenticecodex.entity.spellthrowablecard.SpellAutonomyCardEntity;
import jp.aquafactory.apprenticecodex.entity.spellthrowablecard.SpellInvokeCardEntity;
import jp.aquafactory.apprenticecodex.spell.bulletstream.BulletStreamMinigunEntity;
import jp.aquafactory.apprenticecodex.spell.commencefire.CommenceFireRifleEntity;
import jp.aquafactory.apprenticecodex.spell.companiontrunk.CompanionTrunkEntity;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhialProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.demicreatorwings.DemicreatorWingsCoreEntity;
import jp.aquafactory.apprenticecodex.spell.demicreatorwings.DemicreatorWingsWingEntity;
import jp.aquafactory.apprenticecodex.spell.dualacrobat.DualAcrobatSmgEntity;
import jp.aquafactory.apprenticecodex.spell.extract.ExtractPotionProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.featherrush.FeatherRushProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.featherrush.FeatherRushWingEntity;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterLauncherEntity;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerStaffEntity;
import jp.aquafactory.apprenticecodex.spell.gracedrain.GracedRainCloudEntity;
import jp.aquafactory.apprenticecodex.spell.grindrunner.GrindRunnerWheelEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomEntity;
import jp.aquafactory.apprenticecodex.spell.heavenlyfist.HeavenlyFistFistEntity;
import jp.aquafactory.apprenticecodex.spell.higanbana.HiganbanaKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.illuminatestellar.IlluminateStellarStarEntity;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBladeProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIceDaggerEntity;
import jp.aquafactory.apprenticecodex.spell.lethalassault.LethalAssaultRifleEntity;
import jp.aquafactory.apprenticecodex.spell.magicspear.MagicSpearMissileEntity;
import jp.aquafactory.apprenticecodex.spell.mantisleap.MantisLeapBladeEntity;
import jp.aquafactory.apprenticecodex.spell.manaslash.ManaSlashProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLightChargeCutEntity;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLightKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShieldProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShieldShieldEntity;
import jp.aquafactory.apprenticecodex.spell.phalanxcharge.PhalanxChargeBeamEntity;
import jp.aquafactory.apprenticecodex.spell.phalanxcharge.PhalanxWeaponryEntity;
import jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.precisionjack.PrecisionJackKnifeEntity;
import jp.aquafactory.apprenticecodex.spell.quickarms.QuickArmsHandgunEntity;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconEntity;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGazeProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGazeStaffEntity;
import jp.aquafactory.apprenticecodex.spell.shock.ShockBoltEntity;
import jp.aquafactory.apprenticecodex.spell.silentassassin.SilentAssassinRifleEntity;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdgeProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.thermalprocess.ThermalProcessThrowerEntity;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjackSawEntity;
import jp.aquafactory.apprenticecodex.spell.tirovolley.TiroVolleyMusketEntity;
import jp.aquafactory.apprenticecodex.spell.totemofpermafrost.TotemOfPermafrostTotemEntity;
import jp.aquafactory.apprenticecodex.spell.uniteluna.UniteLunaMoonEntity;
import jp.aquafactory.apprenticecodex.spell.worldflatter.WorldFlatterDrillEntity;
import jp.aquafactory.apprenticecodex.entity.spelldispenser.SpellDispenserAnchorEntity;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastAnchorEntity;
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

    // 既存ワールドの数値IDをずらさないため、新規エンティティは常に既存登録の末尾へ追加する.
    public static final RegistryObject<EntityType<SkyEdgeProjectileEntity>> SKY_EDGE_PROJECTILE =
            regProjectile("sky_edge_projectile", SkyEdgeProjectileEntity::new, 128, 1);

    public static final RegistryObject<EntityType<InscribeIceDaggerEntity>> INSCRIBE_ICE_DAGGER =
            regProjectile("inscribe_ice_dagger", InscribeIceDaggerEntity::new, 128, 1);

    public static final RegistryObject<EntityType<AnchorBlinkDaggerEntity>> ANCHOR_BLINK_DAGGER =
            regProjectile("anchor_blink_dagger", AnchorBlinkDaggerEntity::new, 128, 1);

    public static final RegistryObject<EntityType<ManaForceBladeProjectileEntity>> MANA_FORCE_BLADE_PROJECTILE =
            regProjectile("mana_force_blade_projectile", ManaForceBladeProjectileEntity::new, 128, 1);

    public static final RegistryObject<EntityType<ArcherMultipleBowEntity>> ARCHER_MULTIPLE_BOW =
            regWeapon("archer_multiple_bow", ArcherMultipleBowEntity::new, 1);

    public static final RegistryObject<EntityType<CommenceFireRifleEntity>> COMMENCE_FIRE_RIFLE =
            regWeapon("commence_fire_rifle", CommenceFireRifleEntity::new, 2);

    public static final RegistryObject<EntityType<CompoundPhialProjectileEntity>> COMPOUND_PHIAL_PROJECTILE =
            regProjectile("compound_phial_projectile", CompoundPhialProjectileEntity::new, 48, 1);

    public static final RegistryObject<EntityType<ExtractPotionProjectileEntity>> EXTRACT_POTION_PROJECTILE =
            regProjectile("extract_potion_projectile", ExtractPotionProjectileEntity::new, 48, 1);

    public static final RegistryObject<EntityType<QuickArmsHandgunEntity>> QUICK_ARMS_HANDGUN =
            regWeapon("quick_arms_handgun", QuickArmsHandgunEntity::new, 1);

    public static final RegistryObject<EntityType<BreachingEnemyShotgunEntity>> BREACHING_ENEMY_SHOTGUN =
            regWeapon("breaching_enemy_shotgun", BreachingEnemyShotgunEntity::new, 1);

    public static final RegistryObject<EntityType<SilentAssassinRifleEntity>> SILENT_ASSASSIN_RIFLE =
            regWeapon("silent_assassin_rifle", SilentAssassinRifleEntity::new, 1);

    public static final RegistryObject<EntityType<LethalAssaultRifleEntity>> LETHAL_ASSAULT_RIFLE =
            regWeapon("lethal_assault_rifle", LethalAssaultRifleEntity::new, 1);

    public static final RegistryObject<EntityType<BulletStreamMinigunEntity>> BULLET_STREAM_MINIGUN =
            regWeapon("bullet_stream_minigun", BulletStreamMinigunEntity::new, 1);

    public static final RegistryObject<EntityType<DualAcrobatSmgEntity>> DUAL_ACROBAT_SMG =
            regWeapon("dual_acrobat_smg", DualAcrobatSmgEntity::new, 1);

    public static final RegistryObject<EntityType<TiroVolleyMusketEntity>> TIRO_VOLLEY_MUSKET =
            regWeapon("tiro_volley_musket", TiroVolleyMusketEntity::new, 1);

    public static final RegistryObject<EntityType<ThermalProcessThrowerEntity>> THERMAL_PROCESS_THROWER =
            regWeapon("thermal_process_thrower", ThermalProcessThrowerEntity::new, 1);

    public static final RegistryObject<EntityType<ChargedTwinBladeStaffThrownEntity>> CHARGED_TWIN_BLADE_STAFF_THROWN =
            regProjectile("charged_twin_blade_staff_thrown", ChargedTwinBladeStaffThrownEntity::new, 96, 1);

    public static final RegistryObject<EntityType<SpellInvokeCardEntity>> SPELL_INVOKE_CARD =
            regProjectile("spell_invoke_card", SpellInvokeCardEntity::new, 96, 1);

    public static final RegistryObject<EntityType<SpellAutonomyCardEntity>> SPELL_AUTONOMY_CARD =
            regProjectile("spell_autonomy_card", SpellAutonomyCardEntity::new, 96, 1);

    public static final RegistryObject<EntityType<GracedRainCloudEntity>> GRACED_RAIN_CLOUD =
            regWeapon("graced_rain_cloud", GracedRainCloudEntity::new, 1);

    public static final RegistryObject<EntityType<TinyLumberjackSawEntity>> TINY_LUMBERJACK_SAW =
            regWeapon("tiny_lumberjack_saw", TinyLumberjackSawEntity::new, 1);

    public static final RegistryObject<EntityType<ArcaneBeamEntity>> ARCANE_BEAM =
            regProjectile("arcane_beam", ArcaneBeamEntity::new, 64, 1);

    public static final RegistryObject<EntityType<ShockBoltEntity>> SHOCK_BOLT =
            regProjectile("shock_bolt", ShockBoltEntity::new, 64, 1);

    public static final RegistryObject<EntityType<ManaSlashProjectileEntity>> MANA_SLASH_PROJECTILE =
            regProjectile("mana_slash_projectile", ManaSlashProjectileEntity::new, 64, 1);

    public static final RegistryObject<EntityType<FlySwatterLauncherEntity>> FLY_SWATTER_LAUNCHER =
            regWeapon("fly_swatter_launcher", FlySwatterLauncherEntity::new, 1);

    public static final RegistryObject<EntityType<FlySwatterProjectileEntity>> FLY_SWATTER_PROJECTILE =
            regProjectile("fly_swatter_projectile", FlySwatterProjectileEntity::new, 128, 1);

    public static final RegistryObject<EntityType<ArtisanSmashLauncherEntity>> ARTISAN_SMASH_LAUNCHER =
            regWeapon("artisan_smash_launcher", ArtisanSmashLauncherEntity::new, 1);

    public static final RegistryObject<EntityType<ArtisanSmashShellEntity>> ARTISAN_SMASH_SHELL =
            regProjectile("artisan_smash_shell", ArtisanSmashShellEntity::new, 128, 1);

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

    public static final RegistryObject<EntityType<TotemOfPermafrostTotemEntity>> TOTEM_OF_PERMAFROST_TOTEM =
            regLiving("totem_of_permafrost_totem", TotemOfPermafrostTotemEntity::new,
                    TotemOfPermafrostTotemEntity.WIDTH, TotemOfPermafrostTotemEntity.HEIGHT, 32);

    public static final RegistryObject<EntityType<CompanionTrunkEntity>> COMPANION_TRUNK =
            regLiving("companion_trunk", CompanionTrunkEntity::new, CompanionTrunkEntity.WIDTH, CompanionTrunkEntity.HEIGHT, 32);

    public static final RegistryObject<EntityType<HealingBloomEntity>> HEALING_BLOOM =
            regLiving("healing_bloom", HealingBloomEntity::new, HealingBloomEntity.WIDTH, HealingBloomEntity.HEIGHT, 32);

    public static final RegistryObject<EntityType<HeavenlyFistFistEntity>> HEAVENLY_FIST_FIST =
            reg("heavenly_fist_fist", HeavenlyFistFistEntity::new, MobCategory.MISC,
                    0.1f, 0.1f, 64, 1, false);

    public static final RegistryObject<EntityType<IlluminateStellarStarEntity>> ILLUMINATE_STELLAR_STAR =
            regProjectile("illuminate_stellar_star", IlluminateStellarStarEntity::new, 96, 1);

    public static final RegistryObject<EntityType<UniteLunaMoonEntity>> UNITE_LUNA_MOON =
            regProjectile("unite_luna_moon", UniteLunaMoonEntity::new, 96, 1);

    public static final RegistryObject<EntityType<MagicSpearMissileEntity>> MAGIC_SPEAR_MISSILE =
            regProjectile("magic_spear_missile", MagicSpearMissileEntity::new, 128, 1);

    public static final RegistryObject<EntityType<MysticShieldProjectileEntity>> MYSTIC_SHIELD_PROJECTILE =
            regProjectile("mystic_shield_projectile", MysticShieldProjectileEntity::new, 96, 1);

    public static final RegistryObject<EntityType<MysticShieldShieldEntity>> MYSTIC_SHIELD_SHIELD =
            reg("mystic_shield_shield", MysticShieldShieldEntity::new, MobCategory.MISC,
                    0.2f, 0.2f, 96, 1, false);

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

    public static final RegistryObject<EntityType<SearchBeaconEntity>> SEARCH_BEACON =
            regLiving("search_beacon", SearchBeaconEntity::new, SearchBeaconEntity.WIDTH, SearchBeaconEntity.HEIGHT, 32);

    public static final RegistryObject<EntityType<SpellDispenserAnchorEntity>> SPELL_DISPENSER_ANCHOR =
            reg("spell_dispenser_anchor", SpellDispenserAnchorEntity::new, MobCategory.MISC,
                    0.6f, 1.8f, 32, 1, false);

    public static final RegistryObject<EntityType<RemoteOwnerCastAnchorEntity>> REMOTE_OWNER_CAST_ANCHOR =
            reg("remote_owner_cast_anchor", RemoteOwnerCastAnchorEntity::new, MobCategory.MISC,
                    0.6f, 1.8f, 32, 1, false);

    public static final RegistryObject<EntityType<FieldOverseerStaffEntity>> FIELD_OVERSEER_STAFF =
            regLiving("field_overseer_staff", FieldOverseerStaffEntity::new,
                    FieldOverseerStaffEntity.WIDTH, FieldOverseerStaffEntity.HEIGHT, 32);

    public static final RegistryObject<EntityType<ServantGazeStaffEntity>> SERVANT_GAZE_STAFF =
            regWeapon("servant_gaze_staff", ServantGazeStaffEntity::new, 1);

    public static final RegistryObject<EntityType<ServantGazeProjectileEntity>> SERVANT_GAZE_PROJECTILE =
            regProjectile("servant_gaze_projectile", ServantGazeProjectileEntity::new, 128, 1);

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}

