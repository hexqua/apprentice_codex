package jp.aquafactory.apprenticecodex.compat.sable;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class SableCompat {
    public static final String MOD_ID = "sable";
    private static final String SPELL_DISPENSER_COMPAT_CLASS =
            "jp.aquafactory.apprenticecodex.compat.sable.SpellDispenserSableCompat";

    private SableCompat() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(SableCompat::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }

        event.enqueueWork(SableCompat::registerSpellDispenserCompat);
    }

    private static void registerSpellDispenserCompat() {
        try {
            var compatClass = Class.forName(SPELL_DISPENSER_COMPAT_CLASS);
            compatClass.getMethod("register").invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to initialize Spell Dispenser compatibility for Sable", exception);
        }

        ApprenticeCodex.LOGGER.info("Sable Spell Dispenser compat enabled");
    }
}
