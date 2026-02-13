package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DataGenerator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        var lookupProvider = event.getLookupProvider();
        var existing = event.getExistingFileHelper();
        var datapackProvider = new RegistryDataGenerator(output, lookupProvider);

        generator.addProvider(event.includeServer(), datapackProvider);
        generator.addProvider(event.includeServer(), new DamageTypeTagGenerator(output, datapackProvider.getRegistryProvider(), existing));
    }
}
