package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.apprenticedesk.ApprenticeDeskMenu;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationMenu;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserMenu;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbenchMenu;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoireMenu;
import jp.aquafactory.apprenticecodex.item.curios.endergrimoire.EnderGrimoireInscriptionMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ApprenticeCodex.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ApprenticeDeskMenu>> APPRENTICE_DESK =
            MENUS.register("apprentice_desk", () -> IMenuTypeExtension.create((windowId, inv, data) -> new ApprenticeDeskMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<SpellcasterWorkbenchMenu>> SPELLCASTER_WORKBENCH =
            MENUS.register("spellcaster_workbench", () -> IMenuTypeExtension.create((windowId, inv, data) -> new SpellcasterWorkbenchMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<SpellDispenserMenu>> SPELL_DISPENSER =
            MENUS.register("spell_dispenser", () -> IMenuTypeExtension.create(SpellDispenserMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AtelierStationMenu>> ATELIER_STATION =
            MENUS.register("atelier_station", () -> IMenuTypeExtension.create((windowId, inv, data) -> new AtelierStationMenu(windowId, inv, data.readBlockPos())));

    public static final DeferredHolder<MenuType<?>, MenuType<SpellCalibrationBenchMenu>> SPELL_CALIBRATION_BENCH =
            MENUS.register("spell_calibration_bench", () -> IMenuTypeExtension.create((windowId, inv, data) -> new SpellCalibrationBenchMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<EnderGrimoireInscriptionMenu>> ENDER_GRIMOIRE_INSCRIPTION =
            MENUS.register("ender_grimoire_inscription", () -> IMenuTypeExtension.create((windowId, inv, data) -> new EnderGrimoireInscriptionMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<ArchivistsGrimoireMenu>> ARCHIVISTS_GRIMOIRE =
            MENUS.register("archivists_grimoire", () -> IMenuTypeExtension.create(ArchivistsGrimoireMenu::new));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}

