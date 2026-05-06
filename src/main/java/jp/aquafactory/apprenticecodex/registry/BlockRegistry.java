package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.apprenticedesk.ApprenticeDesk;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStation;
import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJar;
import jp.aquafactory.apprenticecodex.block.comfortberrybush.ComfortBerryBushBlock;
import jp.aquafactory.apprenticecodex.block.essencesmoker.EssenceSmoker;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenser;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbench;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomLightBlock;
import jp.aquafactory.apprenticecodex.spell.frostrune.FrostRuneTrapBlock;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLightTorchBlock;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfChestBlock;
import net.minecraft.core.registries.Registries;
import jp.aquafactory.apprenticecodex.spell.rifthole.RiftHoleBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BlockRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, ApprenticeCodex.MODID);

    public static final DeferredHolder<Block, Block> MAGE_LIGHT_TORCH =
            BLOCKS.register("mage_light_torch", MageLightTorchBlock::new);

    public static final DeferredHolder<Block, Block> FROST_RUNE_TRAP =
            BLOCKS.register("frost_rune_trap", FrostRuneTrapBlock::new);

    public static final DeferredHolder<Block, Block> HEALING_BLOOM_LIGHT =
            BLOCKS.register("healing_bloom_light", () -> new HealingBloomLightBlock());

    public static final DeferredHolder<Block, Block> PERSONAL_SHELF_CHEST =
            BLOCKS.register("personal_shelf_chest", () -> new PersonalShelfChestBlock());

    public static final DeferredHolder<Block, Block> RIFT_HOLE =
            BLOCKS.register("rift_hole", () -> new RiftHoleBlock());

    public static final DeferredHolder<Block, Block> APPRENTICE_DESK =
            BLOCKS.register("apprentice_desk", ApprenticeDesk::new);

    public static final DeferredHolder<Block, Block> SPELLCASTER_WORKBENCH =
            BLOCKS.register("spellcaster_workbench", SpellcasterWorkbench::new);

    public static final DeferredHolder<Block, Block> SPELL_DISPENSER =
            BLOCKS.register("spell_dispenser", () -> new SpellDispenser());

    public static final DeferredHolder<Block, Block> ARCANUM_IN_A_JAR =
            BLOCKS.register("arcanum_in_a_jar", () -> new ArcanumInAJar());

    public static final DeferredHolder<Block, Block> ESSENCE_SMOKER =
            BLOCKS.register("essence_smoker", () -> new EssenceSmoker());

    public static final DeferredHolder<Block, Block> ATELIER_STATION =
            BLOCKS.register("atelier_station", () -> new AtelierStation());

    public static final DeferredHolder<Block, Block> COMFORT_BERRY_BUSH =
            BLOCKS.register("comfort_berry_bush", () -> new ComfortBerryBushBlock());

    public static final DeferredHolder<Block, Block> POTTED_COMFORT_BERRY_BUSH =
            BLOCKS.register("potted_comfort_berry_bush", () -> new FlowerPotBlock(
                    () -> (FlowerPotBlock) Blocks.FLOWER_POT,
                    COMFORT_BERRY_BUSH,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_BLUE_ORCHID)
            ));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        eventBus.addListener(BlockRegistry::registerFlowerPotPlants);
    }

    private static void registerFlowerPotPlants(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "comfort_berry_bush"),
                POTTED_COMFORT_BERRY_BUSH
        ));
    }
}

