package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationBlockEntity;
import jp.aquafactory.apprenticecodex.block.alchemybrewer.AlchemyBrewerBlockEntity;
import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJarBlockEntity;
import jp.aquafactory.apprenticecodex.block.essencesmoker.EssenceSmokerBlockEntity;
import jp.aquafactory.apprenticecodex.block.spellcasteraccessorycase.SpellcasterAccessoryCaseBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.spell.frostrune.FrostRuneTrapBlockEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomLightBlockEntity;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLightTorchBlockEntity;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfChestBlockEntity;
import jp.aquafactory.apprenticecodex.spell.rifthole.RiftHoleBlockEntity;
import jp.aquafactory.apprenticecodex.spell.wizardlamp.WizardlampLanternBlockEntity;
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

    @SafeVarargs
    private static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> reg(
            String id, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block>... blocks
    ) {
        return BLOCK_ENTITY_TYPES.register(id,
                () -> BlockEntityType.Builder.of(
                        factory,
                        java.util.Arrays.stream(blocks)
                                .map(Supplier::get)
                                .toArray(Block[]::new)
                ).build(NO_DFU));
    }

    public static final RegistryObject<BlockEntityType<MageLightTorchBlockEntity>> MAGE_LIGHT_TORCH = reg(
            "mage_light_torch", MageLightTorchBlockEntity::new, BlockRegistry.MAGE_LIGHT_TORCH
    );

    public static final RegistryObject<BlockEntityType<WizardlampLanternBlockEntity>> WIZARDLAMP_LANTERN = reg(
            "wizardlamp_lantern", WizardlampLanternBlockEntity::new, BlockRegistry.WIZARDLAMP_LANTERN
    );

    public static final RegistryObject<BlockEntityType<FrostRuneTrapBlockEntity>> FROST_RUNE_TRAP = reg(
            "frost_rune_trap", FrostRuneTrapBlockEntity::new, BlockRegistry.FROST_RUNE_TRAP
    );

    public static final RegistryObject<BlockEntityType<HealingBloomLightBlockEntity>> HEALING_BLOOM_LIGHT = reg(
            "healing_bloom_light", HealingBloomLightBlockEntity::new, BlockRegistry.HEALING_BLOOM_LIGHT
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
    public static final RegistryObject<BlockEntityType<AlchemyBrewerBlockEntity>> ALCHEMY_BREWER = reg(
            "alchemy_brewer", AlchemyBrewerBlockEntity::new, BlockRegistry.ALCHEMY_BREWER
    );

    public static final RegistryObject<BlockEntityType<SpellDispenserBlockEntity>> SPELL_DISPENSER = reg(
            "spell_dispenser", SpellDispenserBlockEntity::new, BlockRegistry.SPELL_DISPENSER, BlockRegistry.CREATIVE_SPELL_DISPENSER
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpellcasterAccessoryCaseBlockEntity>> SPELLCASTER_ACCESSORY_CASE = reg(
            "spellcaster_accessory_case", SpellcasterAccessoryCaseBlockEntity::new, BlockRegistry.SPELLCASTER_ACCESSORY_CASE
    );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
