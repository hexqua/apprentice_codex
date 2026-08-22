package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.apprenticedesk.ApprenticeDesk;
import jp.aquafactory.apprenticecodex.block.alchemybrewer.AlchemyBrewer;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStation;
import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJar;
import jp.aquafactory.apprenticecodex.block.comfortberrybush.ComfortBerryBushBlock;
import jp.aquafactory.apprenticecodex.block.essencesmoker.EssenceSmoker;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBench;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenser;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbench;
import jp.aquafactory.apprenticecodex.block.spellcasteraccessorycase.SpellcasterAccessoryCaseBlock;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomLightBlock;
import jp.aquafactory.apprenticecodex.spell.frostrune.FrostRuneTrapBlock;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLightTorchBlock;
import jp.aquafactory.apprenticecodex.spell.otherworldlens.OtherworldLensBlock;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfChestBlock;
import jp.aquafactory.apprenticecodex.spell.rifthole.RiftHoleBlock;
import jp.aquafactory.apprenticecodex.spell.wizardlamp.WizardlampLanternBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BlockRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ApprenticeCodex.MODID);

    public static final RegistryObject<Block> MAGE_LIGHT_TORCH =
            BLOCKS.register("mage_light_torch", MageLightTorchBlock::new);

    public static final RegistryObject<Block> WIZARDLAMP_LANTERN =
            BLOCKS.register("wizardlamp_lantern", WizardlampLanternBlock::new);

    public static final RegistryObject<Block> FROST_RUNE_TRAP =
            BLOCKS.register("frost_rune_trap", FrostRuneTrapBlock::new);

    public static final RegistryObject<Block> HEALING_BLOOM_LIGHT =
            BLOCKS.register("healing_bloom_light", HealingBloomLightBlock::new);

    public static final RegistryObject<Block> PERSONAL_SHELF_CHEST =
            BLOCKS.register("personal_shelf_chest", PersonalShelfChestBlock::new);

    public static final RegistryObject<Block> RIFT_HOLE =
            BLOCKS.register("rift_hole", RiftHoleBlock::new);

    public static final RegistryObject<Block> OTHERWORLD_LENS_LENS =
            BLOCKS.register("otherworld_lens_lens", OtherworldLensBlock::new);

    public static final RegistryObject<Block> APPRENTICE_DESK =
            BLOCKS.register("apprentice_desk", ApprenticeDesk::new);

    public static final RegistryObject<Block> SPELLCASTER_WORKBENCH =
            BLOCKS.register("spellcaster_workbench", SpellcasterWorkbench::new);

    public static final RegistryObject<Block> SPELL_CALIBRATION_BENCH =
            BLOCKS.register("spell_calibration_bench", SpellCalibrationBench::new);

    public static final RegistryObject<Block> SPELLCASTER_ACCESSORY_CASE =
            BLOCKS.register("spellcaster_accessory_case", SpellcasterAccessoryCaseBlock::new);

    public static final RegistryObject<Block> SPELL_DISPENSER =
            BLOCKS.register("spell_dispenser", SpellDispenser::new);

    public static final RegistryObject<Block> CREATIVE_SPELL_DISPENSER =
            BLOCKS.register("creative_spell_dispenser", SpellDispenser::newCreative);

    public static final RegistryObject<Block> ARCANUM_IN_A_JAR =
            BLOCKS.register("arcanum_in_a_jar", ArcanumInAJar::new);

    public static final RegistryObject<Block> ESSENCE_SMOKER =
            BLOCKS.register("essence_smoker", EssenceSmoker::new);

    public static final RegistryObject<Block> ATELIER_STATION =
            BLOCKS.register("atelier_station", AtelierStation::new);
    public static final RegistryObject<Block> ALCHEMY_BREWER =
            BLOCKS.register("alchemy_brewer", AlchemyBrewer::new);

    public static final RegistryObject<Block> COMFORT_BERRY_BUSH =
            BLOCKS.register("comfort_berry_bush", ComfortBerryBushBlock::new);

    public static final RegistryObject<Block> POTTED_COMFORT_BERRY_BUSH =
            BLOCKS.register("potted_comfort_berry_bush", () -> new FlowerPotBlock(
                    () -> (FlowerPotBlock) Blocks.FLOWER_POT,
                    COMFORT_BERRY_BUSH,
                    BlockBehaviour.Properties.copy(Blocks.POTTED_BLUE_ORCHID)
                            .lightLevel(state -> 10)
            ));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        eventBus.addListener(BlockRegistry::registerFlowerPotPlants);
        eventBus.addListener(BlockRegistry::registerCompostables);
    }

    private static void registerFlowerPotPlants(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "comfort_berry_bush"),
                POTTED_COMFORT_BERRY_BUSH
        ));
    }

    private static void registerCompostables(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ComposterBlock.COMPOSTABLES.put(ItemRegistry.COMFORT_BERRIES.get(), 0.65F));
    }
}
