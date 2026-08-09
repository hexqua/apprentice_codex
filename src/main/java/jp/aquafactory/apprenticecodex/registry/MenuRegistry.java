package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.apprenticedesk.ApprenticeDeskMenu;
import jp.aquafactory.apprenticecodex.block.alchemybrewer.AlchemyBrewerMenu;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationMenu;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserMenu;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbenchMenu;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoireMenu;
import jp.aquafactory.apprenticecodex.item.curios.endergrimoire.EnderGrimoireInscriptionMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ApprenticeCodex.MODID);

    public static final RegistryObject<MenuType<ApprenticeDeskMenu>> APPRENTICE_DESK =
            MENUS.register("apprentice_desk", () -> IForgeMenuType.create((windowId, inv, data) -> new ApprenticeDeskMenu(windowId, inv)));

    public static final RegistryObject<MenuType<SpellcasterWorkbenchMenu>> SPELLCASTER_WORKBENCH =
            MENUS.register("spellcaster_workbench", () -> IForgeMenuType.create((windowId, inv, data) -> new SpellcasterWorkbenchMenu(windowId, inv)));

    public static final RegistryObject<MenuType<SpellDispenserMenu>> SPELL_DISPENSER =
            MENUS.register("spell_dispenser", () -> IForgeMenuType.create(SpellDispenserMenu::new));

    public static final RegistryObject<MenuType<AtelierStationMenu>> ATELIER_STATION =
            MENUS.register("atelier_station", () -> IForgeMenuType.create((windowId, inv, data) -> new AtelierStationMenu(windowId, inv, data.readBlockPos())));
    public static final RegistryObject<MenuType<AlchemyBrewerMenu>> ALCHEMY_BREWER =
            MENUS.register("alchemy_brewer", () -> IForgeMenuType.create((windowId, inv, data) -> new AlchemyBrewerMenu(windowId, inv, data.readBlockPos())));

    public static final RegistryObject<MenuType<SpellCalibrationBenchMenu>> SPELL_CALIBRATION_BENCH =
            MENUS.register("spell_calibration_bench", () -> IForgeMenuType.create((windowId, inv, data) -> new SpellCalibrationBenchMenu(windowId, inv)));

    public static final RegistryObject<MenuType<EnderGrimoireInscriptionMenu>> ENDER_GRIMOIRE_INSCRIPTION =
            MENUS.register("ender_grimoire_inscription", () -> IForgeMenuType.create((windowId, inv, data) -> new EnderGrimoireInscriptionMenu(windowId, inv)));

    public static final RegistryObject<MenuType<ArchivistsGrimoireMenu>> ARCHIVISTS_GRIMOIRE =
            MENUS.register("archivists_grimoire", () -> IForgeMenuType.create(ArchivistsGrimoireMenu::new));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
