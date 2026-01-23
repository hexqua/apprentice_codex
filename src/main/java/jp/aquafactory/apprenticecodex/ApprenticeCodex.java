package jp.aquafactory.apprenticecodex;

import com.mojang.logging.LogUtils;
import jp.aquafactory.apprenticecodex.common.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.common.registry.SpellsRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ApprenticeCodex.MODID)
public class ApprenticeCodex
{
    public static final String MODID = "apprenticecodex";
    public static final String NAME = "Apprentice Codex";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ApprenticeCodex() {
        // 現時点で1.20.1専用アドオンのため、deprecatedを無視する.
        @SuppressWarnings("removal") var bus = FMLJavaModLoadingContext.get().getModEventBus();
        SpellsRegistry.register(bus);
        EntityRegistry.register(bus);
    }
}
