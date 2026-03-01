package jp.aquafactory.apprenticecodex.registry;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.apprenticedesk.ApprenticeDeskMenu;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.item.curios.endergrimoire.EnderGrimoireInscriptionMenu;
import jp.aquafactory.apprenticecodex.spell.personalshelf.PersonalShelfMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ApprenticeCodex.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<PersonalShelfMenu>> PERSONAL_SHELF =
            MENUS.register("personal_shelf", () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                var shelf = Capabilities.getPersonalInventory(inv.player).orElseThrow(() -> new IllegalStateException("personal inventory (for Personal Shelf spell) capability missing"));
                return new PersonalShelfMenu(windowId, inv, shelf.getHandler(), data.readBlockPos());
            }));

    public static final DeferredHolder<MenuType<?>, MenuType<ApprenticeDeskMenu>> APPRENTICE_DESK =
            MENUS.register("apprentice_desk", () -> IMenuTypeExtension.create((windowId, inv, data) -> new ApprenticeDeskMenu(windowId, inv)));

    public static final DeferredHolder<MenuType<?>, MenuType<EnderGrimoireInscriptionMenu>> ENDER_GRIMOIRE_INSCRIPTION =
            MENUS.register("ender_grimoire_inscription", () -> IMenuTypeExtension.create((windowId, inv, data) -> new EnderGrimoireInscriptionMenu(windowId, inv)));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}

