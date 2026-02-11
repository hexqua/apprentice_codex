package jp.aquafactory.apprenticecodex.common.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.spells.magelight.MageLightTorchBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ApprenticeCodex.MODID);

    public static final RegistryObject<BlockEntityType<MageLightTorchBlockEntity>> MAGE_LIGHT_TORCH =
            BLOCK_ENTITY_TYPES.register("mage_light_torch",
                    () -> BlockEntityType.Builder.of(MageLightTorchBlockEntity::new, BlockRegistry.MAGE_LIGHT_TORCH.get())
                            .build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
