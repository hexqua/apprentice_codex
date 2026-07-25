package jp.aquafactory.apprenticecodex;

import com.mojang.logging.LogUtils;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.compat.create.CreateCompat;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightCompat;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexCommonConfig;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.event.ApprenticeDeskConfigSyncEvents;
import jp.aquafactory.apprenticecodex.event.LuminousDeviceConfigSyncEvents;
import jp.aquafactory.apprenticecodex.event.CircuitHeatStaffConfigSyncEvents;
import jp.aquafactory.apprenticecodex.event.EquipmentSpellTimingConfigSyncEvents;
import jp.aquafactory.apprenticecodex.event.IsekaiTravelGuidebookConfigSyncEvents;
import jp.aquafactory.apprenticecodex.event.ManaForceBladeConfigSyncEvents;
import jp.aquafactory.apprenticecodex.event.ManaShieldCharmConfigSyncEvents;
import jp.aquafactory.apprenticecodex.event.ManaThrusterConfigSyncEvents;
import jp.aquafactory.apprenticecodex.event.ModEntityAttributeEvent;
import jp.aquafactory.apprenticecodex.event.ZenithStaffConfigSyncEvents;
import jp.aquafactory.apprenticecodex.event.client.ClientModBusEvents;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeSchoolPowerBonusEvents;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowConfigSyncEvents;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowConfigSyncEvents;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbookConfigSyncEvents;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.CreativeTabRegistry;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.GlobalLootModifierRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.LootConditionRegistry;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.PoiTypeRegistry;
import jp.aquafactory.apprenticecodex.registry.PotionRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.registry.VillagerProfessionRegistry;
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
        ApprenticeCodexCommonConfig.register(modEventBus);
        BlockRegistry.register(modEventBus);
        BlockEntityRegistry.register(modEventBus);
        EntityRegistry.register(modEventBus);
        PoiTypeRegistry.register(modEventBus);
        VillagerProfessionRegistry.register(modEventBus);
        ItemRegistry.ITEMS.register(modEventBus);
        EffectRegistry.register(modEventBus);
        PotionRegistry.register(modEventBus);
        SpellRegistry.register(modEventBus);
        ApprenticeAttributeRegistry.register(modEventBus);
        SoundRegistry.register(modEventBus);
        MenuRegistry.register(modEventBus);
        CreativeTabRegistry.register(modEventBus);
        CreateCompat.register(modEventBus);
        EpicFightCompat.register(modEventBus);
        ParticleRegistry.PARTICLES.register(modEventBus);
        RecipeRegistry.register(modEventBus);
        LootConditionRegistry.register(modEventBus);
        GlobalLootModifierRegistry.register(modEventBus);
        AttachmentRegistry.register(modEventBus);
        ModEntityAttributeEvent.register(modEventBus);
        ApprenticeDeskConfigSyncEvents.register(modEventBus);
        LuminousDeviceConfigSyncEvents.register(modEventBus);
        CircuitHeatStaffConfigSyncEvents.register(modEventBus);
        EquipmentSpellTimingConfigSyncEvents.register(modEventBus);
        ChargecastCatalystbookConfigSyncEvents.register(modEventBus);
        ElementalBowConfigSyncEvents.register(modEventBus);
        FocusStaffbowConfigSyncEvents.register(modEventBus);
        IsekaiTravelGuidebookConfigSyncEvents.register(modEventBus);
        ManaForceBladeConfigSyncEvents.register(modEventBus);
        ManaShieldCharmConfigSyncEvents.register(modEventBus);
        ManaThrusterConfigSyncEvents.register(modEventBus);
        ZenithStaffConfigSyncEvents.register(modEventBus);
        ElementMaidenRobeSchoolPowerBonusEvents.register(modEventBus);
        CodexSpellStateTypeRegister.register();
        Networks.register(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientModBusEvents.register(modEventBus);
        }
    }
}

