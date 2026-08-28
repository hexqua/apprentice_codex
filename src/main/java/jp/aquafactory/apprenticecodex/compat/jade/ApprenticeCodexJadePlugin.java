package jp.aquafactory.apprenticecodex.compat.jade;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJar;
import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJarBlockEntity;
import jp.aquafactory.apprenticecodex.block.alchemybrewer.AlchemyBrewerBlockEntity;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStation;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationBlockEntity;
import jp.aquafactory.apprenticecodex.block.essencesmoker.EssenceSmoker;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenser;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerStaffEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomEntity;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconEntity;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class ApprenticeCodexJadePlugin implements IWailaPlugin {
    public static final ResourceLocation ARCANUM_IN_A_JAR_UID = id("arcanum_in_a_jar");
    public static final ResourceLocation ALCHEMY_BREWER_UID = id("alchemy_brewer");
    public static final ResourceLocation ALCHEMY_BREWER_FLUID_UID = id("alchemy_brewer_fluid");
    public static final ResourceLocation ATELIER_STATION_UID = id("atelier_station");
    public static final ResourceLocation ATELIER_STATION_FLUID_UID = id("atelier_station_fluid");
    public static final ResourceLocation ESSENCE_SMOKER_UID = id("essence_smoker");
    public static final ResourceLocation SPELL_DISPENSER_UID = id("spell_dispenser");
    public static final ResourceLocation HEALING_BLOOM_UID = id("healing_bloom");
    public static final ResourceLocation AUTO_TURRET_UID = id("auto_turret");
    public static final ResourceLocation FIELD_OVERSEER_UID = id("field_overseer");
    public static final ResourceLocation SEARCH_BEACON_UID = id("search_beacon");

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, path);
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ArcanumInAJarJadeProvider.INSTANCE, ArcanumInAJarBlockEntity.class);
        registration.registerItemStorage(AlchemyBrewerJadeItemStorageProvider.INSTANCE, AlchemyBrewerBlockEntity.class);
        registration.registerFluidStorage(AlchemyBrewerJadeFluidStorageProvider.INSTANCE, AlchemyBrewerBlockEntity.class);
        registration.registerFluidStorage(AtelierStationJadeFluidStorageProvider.INSTANCE, AtelierStationBlockEntity.class);
        registration.registerEntityDataProvider(HealingBloomJadeProvider.INSTANCE, HealingBloomEntity.class);
        registration.registerEntityDataProvider(AutoTurretJadeProvider.INSTANCE, AutoTurretEntity.class);
        registration.registerEntityDataProvider(FieldOverseerJadeProvider.INSTANCE, FieldOverseerStaffEntity.class);
        registration.registerEntityDataProvider(SearchBeaconJadeProvider.INSTANCE, SearchBeaconEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ArcanumInAJarJadeProvider.INSTANCE, ArcanumInAJar.class);
        registration.registerItemStorageClient(AlchemyBrewerJadeItemStorageProvider.INSTANCE);
        registration.registerFluidStorageClient(AlchemyBrewerJadeFluidStorageProvider.INSTANCE);
        registration.registerBlockComponent(AtelierStationJadeProvider.INSTANCE, AtelierStation.class);
        registration.registerFluidStorageClient(AtelierStationJadeFluidStorageProvider.INSTANCE);
        registration.registerBlockComponent(EssenceSmokerJadeProvider.INSTANCE, EssenceSmoker.class);
        registration.registerBlockComponent(SpellDispenserJadeProvider.INSTANCE, SpellDispenser.class);
        registration.registerEntityComponent(HealingBloomJadeProvider.INSTANCE, HealingBloomEntity.class);
        registration.registerEntityComponent(AutoTurretJadeProvider.INSTANCE, AutoTurretEntity.class);
        registration.registerEntityComponent(FieldOverseerJadeProvider.INSTANCE, FieldOverseerStaffEntity.class);
        registration.registerEntityComponent(SearchBeaconJadeProvider.INSTANCE, SearchBeaconEntity.class);
        registration.hideTarget(EntityRegistry.MYSTIC_SHIELD_SHIELD.get());
        registration.hideTarget(EntityRegistry.DUAL_ACROBAT_SMG.get());
        registration.hideTarget(EntityRegistry.SERVANT_GAZE_STAFF.get());
        registration.hideTarget(EntityRegistry.ARCANE_BEAM.get());
    }
}
