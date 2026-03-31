package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationBlockEntity;
import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJarBlockEntity;
import jp.aquafactory.apprenticecodex.block.essencesmoker.EssenceSmokerBlockEntity;
import jp.aquafactory.apprenticecodex.block.particletest.ParticleTestBlockEntity;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLightTorchBlockEntity;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfChestBlockEntity;
import jp.aquafactory.apprenticecodex.spell.rifthole.RiftHoleBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

@SuppressWarnings("DataFlowIssue")
public final class BlockEntityRegistry {
    // 1.20.1Forgeだと入れるものがないらしいのでnullに(合わせて警告握りつぶし)
    private static final com.mojang.datafixers.types.Type<?> NO_DFU = null;

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ApprenticeCodex.MODID);

    private static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> reg(
            String id, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block> block
    ) {
        return BLOCK_ENTITY_TYPES.register(id,
                () -> BlockEntityType.Builder.of(factory, block.get()).build(NO_DFU));
    }

    public static final RegistryObject<BlockEntityType<MageLightTorchBlockEntity>> MAGE_LIGHT_TORCH = reg(
            "mage_light_torch", MageLightTorchBlockEntity::new, BlockRegistry.MAGE_LIGHT_TORCH
    );

    public static final RegistryObject<BlockEntityType<ArcanumInAJarBlockEntity>> ARCANUM_IN_A_JAR = reg(
            "arcanum_in_a_jar", ArcanumInAJarBlockEntity::new, BlockRegistry.ARCANUM_IN_A_JAR
    );

    public static final RegistryObject<BlockEntityType<PersonalShelfChestBlockEntity>> PERSONAL_SHELF_CHEST = reg(
            "personal_shelf_chest", PersonalShelfChestBlockEntity::new, BlockRegistry.PERSONAL_SHELF_CHEST
    );

    public static final RegistryObject<BlockEntityType<RiftHoleBlockEntity>> RIFT_HOLE = reg(
            "rift_hole", RiftHoleBlockEntity::new, BlockRegistry.RIFT_HOLE
    );

    public static final RegistryObject<BlockEntityType<EssenceSmokerBlockEntity>> ESSENCE_SMOKER = reg(
            "essence_smoker", EssenceSmokerBlockEntity::new, BlockRegistry.ESSENCE_SMOKER
    );

    public static final RegistryObject<BlockEntityType<AtelierStationBlockEntity>> ATELIER_STATION = reg(
            "atelier_station", AtelierStationBlockEntity::new, BlockRegistry.ATELIER_STATION
    );

    public static final RegistryObject<BlockEntityType<ParticleTestBlockEntity>> PARTICLE_TEST_BLOCK = reg(
            "particle_test_block", ParticleTestBlockEntity::new, BlockRegistry.PARTICLE_TEST_BLOCK
    );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
