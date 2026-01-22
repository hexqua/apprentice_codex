package jp.aquafactory.apprenticecodex;

import jp.aquafactory.apprenticecodex.common.registry.SpellsRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ApprenticeCodex.MODID)
public class ApprenticeCodex
{
    public static final String MODID = "apprenticecodex";

    public ApprenticeCodex() {
        // 現時点で1.20.1専用アドオンのため、deprecatedを無視する.
        @SuppressWarnings("removal") var bus = FMLJavaModLoadingContext.get().getModEventBus();
        SpellsRegistry.register(bus);
    }
}
