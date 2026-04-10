package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationBlockEntity;
import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJarBlockEntity;
import jp.aquafactory.apprenticecodex.block.essencesmoker.EssenceSmokerBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomLightBlockEntity;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLightTorchBlockEntity;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfChestBlockEntity;
import net.minecraft.core.registries.Registries;
import jp.aquafactory.apprenticecodex.spell.rifthole.RiftHoleBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@SuppressWarnings("DataFlowIssue")
public final class BlockEntityRegistry {
    // 1.20.1Forgeだと入れるものがないらしいのでnullに(合わせて警告握りつぶし)
    private static final com.mojang.datafixers.types.Type<?> NO_DFU = null;

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ApprenticeCodex.MODID);

    private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> reg(
            String id, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block> block
    ) {
        return BLOCK_ENTITY_TYPES.register(id,
                () -> BlockEntityType.Builder.of(factory, block.get()).build(NO_DFU));
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MageLightTorchBlockEntity>> MAGE_LIGHT_TORCH = reg(
            "mage_light_torch", MageLightTorchBlockEntity::new, BlockRegistry.MAGE_LIGHT_TORCH
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HealingBloomLightBlockEntity>> HEALING_BLOOM_LIGHT = reg(
            "healing_bloom_light", HealingBloomLightBlockEntity::new, BlockRegistry.HEALING_BLOOM_LIGHT
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcanumInAJarBlockEntity>> ARCANUM_IN_A_JAR = reg(
            "arcanum_in_a_jar", ArcanumInAJarBlockEntity::new, BlockRegistry.ARCANUM_IN_A_JAR
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PersonalShelfChestBlockEntity>> PERSONAL_SHELF_CHEST = reg(
            "personal_shelf_chest", PersonalShelfChestBlockEntity::new, BlockRegistry.PERSONAL_SHELF_CHEST
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RiftHoleBlockEntity>> RIFT_HOLE = reg(
            "rift_hole", RiftHoleBlockEntity::new, BlockRegistry.RIFT_HOLE
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EssenceSmokerBlockEntity>> ESSENCE_SMOKER = reg(
            "essence_smoker", EssenceSmokerBlockEntity::new, BlockRegistry.ESSENCE_SMOKER
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AtelierStationBlockEntity>> ATELIER_STATION = reg(
            "atelier_station", AtelierStationBlockEntity::new, BlockRegistry.ATELIER_STATION
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpellDispenserBlockEntity>> SPELL_DISPENSER = reg(
            "spell_dispenser", SpellDispenserBlockEntity::new, BlockRegistry.SPELL_DISPENSER
    );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
        eventBus.addListener(BlockEntityRegistry::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ESSENCE_SMOKER.get(), EssenceSmokerBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SPELL_DISPENSER.get(), SpellDispenserBlockEntity::getItemHandler);
    }
}

