package jp.aquafactory.apprenticecodex;

import com.mojang.logging.LogUtils;
import jp.aquafactory.apprenticecodex.common.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.common.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.common.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.common.registry.SpellsRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ApprenticeCodex.MODID)
public class ApprenticeCodex
{
    public static final String MODID = "apprenticecodex";
    public static final String NAME = "Apprentice's Codex";

    // いずれログを使うため未使用警告を無効化.
    @SuppressWarnings("unused")
    public static final Logger LOGGER = LogUtils.getLogger();

    public ApprenticeCodex() {
        @SuppressWarnings("removal") var bus = FMLJavaModLoadingContext.get().getModEventBus();
        SpellsRegistry.register(bus);
        EntityRegistry.register(bus);
        ItemRegistry.ITEMS.register(bus);
        EffectRegistry.EFFECTS.register(bus);
    }
}
