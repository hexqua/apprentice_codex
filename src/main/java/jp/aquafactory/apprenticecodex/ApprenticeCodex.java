package jp.aquafactory.apprenticecodex;

import com.mojang.logging.LogUtils;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexCommonConfig;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.event.client.ClientModBusEvents;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.registry.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
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
        context.registerConfig(ModConfig.Type.CLIENT, ApprenticeCodexClientConfig.SPEC);
        context.registerConfig(ModConfig.Type.COMMON, ApprenticeCodexCommonConfig.SPEC);
        context.registerConfig(ModConfig.Type.SERVER, ApprenticeCodexServerConfig.SPEC);
        RecipeConditionRegistry.register();

        var bus = context.getModEventBus();
        SpellRegistry.register(bus);
        EntityRegistry.register(bus);
        BlockRegistry.register(bus);
        BlockEntityRegistry.register(bus);
        ItemRegistry.ITEMS.register(bus);
        ParticleRegistry.PARTICLES.register(bus);
        SoundRegistry.register(bus);
        RecipeRegistry.register(bus);
        EffectRegistry.register(bus);
        EnchantmentRegistry.register(bus);
        MenuRegistry.register(bus);
        CreativeTabRegistry.register(bus);
        CodexSpellStateTypeRegister.register();
        Networks.register();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientModBusEvents.register(bus));
    }
}
