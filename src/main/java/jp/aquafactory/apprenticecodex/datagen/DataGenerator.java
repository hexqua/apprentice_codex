package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraftforge.common.data.ForgeAdvancementProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DataGenerator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        var lookupProvider = event.getLookupProvider();
        var existing = event.getExistingFileHelper();
        var datapackProvider = new RegistryDataGenerator(output, lookupProvider);
        var blockTagGenerator = new BlockTagGenerator(output, lookupProvider, existing);

        generator.addProvider(event.includeServer(), datapackProvider);
        generator.addProvider(event.includeServer(), blockTagGenerator);
        generator.addProvider(event.includeServer(), new ItemTagGenerator(output, lookupProvider, blockTagGenerator.contentsGetter(), existing));
        generator.addProvider(event.includeServer(), new RecipeGenerator(output));
        generator.addProvider(event.includeServer(), new GrindRunnerRecipeDataGenerator(output));
        generator.addProvider(event.includeServer(), new LootTableGenerator(output));
        generator.addProvider(event.includeServer(), new SenseEvilHighlightDataGenerator(output, existing));
        generator.addProvider(event.includeServer(), new DamageTypeTagGenerator(output, datapackProvider.getRegistryProvider(), existing));
        generator.addProvider(event.includeServer(), new ForgeAdvancementProvider(output, lookupProvider, existing, List.of(new AdvancementGenerator())));
    }
}
