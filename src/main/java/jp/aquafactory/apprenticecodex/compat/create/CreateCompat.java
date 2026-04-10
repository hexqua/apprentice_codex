package jp.aquafactory.apprenticecodex.compat.create;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class CreateCompat {
    public static final String MOD_ID = "create";
    private static final String SPELL_DISPENSER_COMPAT_CLASS =
            "jp.aquafactory.apprenticecodex.compat.create.SpellDispenserCreateCompat";

    private CreateCompat() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CreateCompat::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }

        event.enqueueWork(CreateCompat::registerSpellDispenserCompat);
    }

    private static void registerSpellDispenserCompat() {
        try {
            var compatClass = Class.forName(SPELL_DISPENSER_COMPAT_CLASS);
            compatClass.getMethod("register").invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Create 用 Spell Dispenser 互換の初期化に失敗しました", exception);
        }

        ApprenticeCodex.LOGGER.info("Create Spell Dispenser compat enabled");
    }
}
