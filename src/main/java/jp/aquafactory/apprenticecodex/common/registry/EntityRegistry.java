package jp.aquafactory.apprenticecodex.common.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.spells.archermultiple.ArcherMultipleBowEntity;
import jp.aquafactory.apprenticecodex.common.spells.skyedge.SkyEdgeProjectileEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityRegistry {
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

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
