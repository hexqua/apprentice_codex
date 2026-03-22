package jp.aquafactory.apprenticecodex;

import com.mojang.logging.LogUtils;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexCommonConfig;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.event.client.ClientModBusEvents;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.CreativeTabRegistry;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.PotionRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeConditionRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.registry.AttachmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ApprenticeAttributeRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(ApprenticeCodex.MODID)
public class ApprenticeCodex
{
    public static final String MODID = "apprenticecodex";
    public static final String NAME = "Apprentice's Codex";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ApprenticeCodex(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Loading {}", NAME);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ApprenticeCodexClientConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, ApprenticeCodexCommonConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, ApprenticeCodexServerConfig.SPEC);
        BlockRegistry.register(modEventBus);
        ItemRegistry.ITEMS.register(modEventBus);
        BlockEntityRegistry.register(modEventBus);
        EntityRegistry.register(modEventBus);
        EffectRegistry.register(modEventBus);
        PotionRegistry.register(modEventBus);
        SpellRegistry.register(modEventBus);
        ApprenticeAttributeRegistry.register(modEventBus);
        SoundRegistry.register(modEventBus);
        MenuRegistry.register(modEventBus);
        CreativeTabRegistry.register(modEventBus);
        ParticleRegistry.PARTICLES.register(modEventBus);
        RecipeRegistry.register(modEventBus);
        RecipeConditionRegistry.register(modEventBus);
        AttachmentRegistry.register(modEventBus);
        CodexSpellStateTypeRegister.register();
        Networks.register(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientModBusEvents.register(modEventBus);
        }
    }
}

