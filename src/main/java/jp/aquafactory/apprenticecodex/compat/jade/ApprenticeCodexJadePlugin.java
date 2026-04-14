package jp.aquafactory.apprenticecodex.compat.jade;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJar;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationBlockEntity;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStation;
import jp.aquafactory.apprenticecodex.block.essencesmoker.EssenceSmoker;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenser;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class ApprenticeCodexJadePlugin implements IWailaPlugin {
    public static final ResourceLocation ARCANUM_IN_A_JAR_UID = id("arcanum_in_a_jar");
    public static final ResourceLocation ATELIER_STATION_UID = id("atelier_station");
    public static final ResourceLocation ESSENCE_SMOKER_UID = id("essence_smoker");
    public static final ResourceLocation SPELL_DISPENSER_UID = id("spell_dispenser");

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, path);
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerFluidStorage(AtelierStationJadeFluidStorageProvider.INSTANCE, AtelierStationBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ArcanumInAJarJadeProvider.INSTANCE, ArcanumInAJar.class);
        registration.registerBlockComponent(AtelierStationJadeProvider.INSTANCE, AtelierStation.class);
        registration.registerBlockComponent(EssenceSmokerJadeProvider.INSTANCE, EssenceSmoker.class);
        registration.registerBlockComponent(SpellDispenserJadeProvider.INSTANCE, SpellDispenser.class);
    }
}
