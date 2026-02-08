package jp.aquafactory.apprenticecodex.common.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.spells.arcanebeam.ArcaneBeamEntity;
import jp.aquafactory.apprenticecodex.common.spells.archermultiple.ArcherMultipleBowEntity;
import jp.aquafactory.apprenticecodex.common.spells.breachingenemy.BreachingEnemyShotgunEntity;
import jp.aquafactory.apprenticecodex.common.spells.bulletstream.BulletStreamMinigunEntity;
import jp.aquafactory.apprenticecodex.common.spells.commencefire.CommenceFireRifleEntity;
import jp.aquafactory.apprenticecodex.common.spells.compoundphial.CompoundPhialProjectileEntity;
import jp.aquafactory.apprenticecodex.common.spells.quickarms.QuickArmsHandgunEntity;
import jp.aquafactory.apprenticecodex.common.spells.skyedge.SkyEdgeProjectileEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ApprenticeCodex.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SkyEdgeProjectileEntity>> SKY_EDGE_PROJECTILE =
            ENTITIES.register("sky_edge_projectile",
                    () -> EntityType.Builder
                            .<SkyEdgeProjectileEntity>of(SkyEdgeProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .setShouldReceiveVelocityUpdates(true)
                            .build("sky_edge_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<ArcherMultipleBowEntity>> ARCHER_MULTIPLE_BOW =
            ENTITIES.register("archer_multiple_bow",
                    () -> EntityType.Builder
                            .<ArcherMultipleBowEntity>of(ArcherMultipleBowEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(64)
                            .updateInterval(4)
                            .build("archer_multiple_bow"));

    public static final DeferredHolder<EntityType<?>, EntityType<CommenceFireRifleEntity>> COMMENCE_FIRE_RIFLE =
            ENTITIES.register("commence_fire_rifle",
                    () -> EntityType.Builder
                            .<CommenceFireRifleEntity>of(CommenceFireRifleEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(64)
                            .updateInterval(2)
                            .build("commence_fire_rifle"));

    public static final DeferredHolder<EntityType<?>, EntityType<CompoundPhialProjectileEntity>> COMPOUND_PHIAL_PROJECTILE =
            ENTITIES.register("compound_phial_projectile",
                    () -> EntityType.Builder
                            .<CompoundPhialProjectileEntity>of(CompoundPhialProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(48)
                            .updateInterval(1)
                            .build("compound_phial_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<QuickArmsHandgunEntity>> QUICK_ARMS_HANDGUN =
            ENTITIES.register("quick_arms_handgun",
                    () -> EntityType.Builder
                            .<QuickArmsHandgunEntity>of(QuickArmsHandgunEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(48)
                            .updateInterval(1)
                            .build("quick_arms_handgun"));

    public static final DeferredHolder<EntityType<?>, EntityType<BreachingEnemyShotgunEntity>> BREACHING_ENEMY_SHOTGUN =
            ENTITIES.register("breaching_enemy_shotgun",
                    () -> EntityType.Builder
                            .<BreachingEnemyShotgunEntity>of(BreachingEnemyShotgunEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(32)
                            .updateInterval(1)
                            .build("breaching_enemy_shotgun"));

    public static final DeferredHolder<EntityType<?>, EntityType<BulletStreamMinigunEntity>> BULLET_STREAM_MINIGUN =
            ENTITIES.register("bullet_stream_minigun",
                    () -> EntityType.Builder
                            .<BulletStreamMinigunEntity>of(BulletStreamMinigunEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(64)
                            .updateInterval(2)
                            .build("bullet_stream_minigun"));

    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneBeamEntity>> ARCANE_BEAM =
            ENTITIES.register("arcane_beam",
                    () -> EntityType.Builder
                            .<ArcaneBeamEntity>of(ArcaneBeamEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("arcane_beam"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
