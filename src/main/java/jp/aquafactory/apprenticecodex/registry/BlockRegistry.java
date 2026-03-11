package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.apprenticedesk.ApprenticeDesk;
import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJar;
import jp.aquafactory.apprenticecodex.block.essencesmoker.EssenceSmoker;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbench;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLightTorchBlock;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfChestBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BlockRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ApprenticeCodex.MODID);

    public static final RegistryObject<Block> MAGE_LIGHT_TORCH =
            BLOCKS.register("mage_light_torch", MageLightTorchBlock::new);

    public static final RegistryObject<Block> PERSONAL_SHELF_CHEST =
            BLOCKS.register("personal_shelf_chest", PersonalShelfChestBlock::new);

    public static final RegistryObject<Block> APPRENTICE_DESK =
            BLOCKS.register("apprentice_desk", ApprenticeDesk::new);

    public static final RegistryObject<Block> SPELLCASTER_WORKBENCH =
            BLOCKS.register("spellcaster_workbench", SpellcasterWorkbench::new);

    public static final RegistryObject<Block> ARCANUM_IN_A_JAR =
            BLOCKS.register("arcanum_in_a_jar", ArcanumInAJar::new);

    public static final RegistryObject<Block> ESSENCE_SMOKER =
            BLOCKS.register("essence_smoker", EssenceSmoker::new);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
