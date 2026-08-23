package jp.aquafactory.apprenticecodex.compat.create;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class CreateCompat {
    public static final String MOD_ID = "create";
    private static final String SPELL_DISPENSER_COMPAT_CLASS =
            "jp.aquafactory.apprenticecodex.compat.create.SpellDispenserCreateCompat";
    private static final String ENDGAME_ARMOR_COMPAT_CLASS =
            "jp.aquafactory.apprenticecodex.compat.create.EndgameArmorCreateCompat";

    private CreateCompat() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CreateCompat::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }

        event.enqueueWork(() -> {
            registerSpellDispenserCompat();
            registerCompat(ENDGAME_ARMOR_COMPAT_CLASS, "endgame armor");
            MagiCompressorGadgetAirBridge.registerBacktankSupplier();
        });
    }

    private static void registerSpellDispenserCompat() {
        registerCompat(SPELL_DISPENSER_COMPAT_CLASS, "Spell Dispenser");
    }

    private static void registerCompat(String className, String featureName) {
        try {
            var compatClass = Class.forName(className);
            compatClass.getMethod("register").invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to initialize " + featureName + " compatibility for Create", exception);
        }

        ApprenticeCodex.LOGGER.info("Create {} compat enabled", featureName);
    }
}
