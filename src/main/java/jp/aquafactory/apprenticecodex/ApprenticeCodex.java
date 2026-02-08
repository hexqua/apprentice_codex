package jp.aquafactory.apprenticecodex;

import com.mojang.logging.LogUtils;
import jp.aquafactory.apprenticecodex.common.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ApprenticeCodex.MODID)
public class ApprenticeCodex
{
    public static final String MODID = "apprenticecodex";
    public static final String NAME = "Apprentice's Codex";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ApprenticeCodex(IEventBus modEventBus) {
        LOGGER.info("Loading {}", NAME);

        SpellsRegistry.register(modEventBus);
        EntityRegistry.register(modEventBus);
        ItemRegistry.ITEMS.register(modEventBus);
        ParticleRegistry.PARTICLES.register(modEventBus);
        SoundRegistry.register(modEventBus);
        EffectRegistry.register(modEventBus);
    }
}
