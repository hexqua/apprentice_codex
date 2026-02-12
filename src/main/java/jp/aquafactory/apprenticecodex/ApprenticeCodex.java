package jp.aquafactory.apprenticecodex;

import com.mojang.logging.LogUtils;
import jp.aquafactory.apprenticecodex.common.registry.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ApprenticeCodex.MODID)
public class ApprenticeCodex
{
    public static final String MODID = "apprenticecodex";
    public static final String NAME = "Apprentice's Codex";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ApprenticeCodex(FMLJavaModLoadingContext context) {
        LOGGER.info("Loading {}", NAME);

        var bus = context.getModEventBus();
        SpellsRegistry.register(bus);
        EntityRegistry.register(bus);
        BlockRegistry.register(bus);
        BlockEntityRegistry.register(bus);
        ItemRegistry.ITEMS.register(bus);
        ParticleRegistry.PARTICLES.register(bus);
        SoundRegistry.register(bus);
        EffectRegistry.register(bus);
        MenuRegistry.register(bus);
    }
}
