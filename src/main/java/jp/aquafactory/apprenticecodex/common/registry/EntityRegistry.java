package jp.aquafactory.apprenticecodex.common.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.spells.TestBoltProjectileEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ApprenticeCodex.MODID);

    public static final RegistryObject<EntityType<TestBoltProjectileEntity>> TEST_BOLT =
            ENTITIES.register("test_bolt",
                    () -> EntityType.Builder
                            .<TestBoltProjectileEntity>of(TestBoltProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("test_bolt"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
