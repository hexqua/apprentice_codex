package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class DataGenerator {
    private DataGenerator() {
    }

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        var lookupProvider = event.getLookupProvider();
        var existing = event.getExistingFileHelper();
        var datapackProvider = new RegistryDataGenerator(output, lookupProvider);
        var blockTagGenerator = new BlockTagGenerator(output, lookupProvider, existing);

        generator.addProvider(event.includeServer(), datapackProvider);
        generator.addProvider(event.includeServer(), blockTagGenerator);
        generator.addProvider(event.includeServer(), new ItemTagGenerator(output, lookupProvider, blockTagGenerator.contentsGetter(), existing));
        generator.addProvider(event.includeServer(), new EnchantmentTagGenerator(output, datapackProvider.getRegistryProvider(), existing));
        generator.addProvider(event.includeServer(), new RecipeGenerator(output, lookupProvider));
        generator.addProvider(event.includeServer(), new GrindRunnerRecipeDataGenerator(output));
        generator.addProvider(event.includeServer(), new LootTableGenerator(output, lookupProvider));
        generator.addProvider(event.includeServer(), new SenseEvilHighlightDataGenerator(output, existing));
        generator.addProvider(event.includeServer(), new DamageTypeTagGenerator(output, datapackProvider.getRegistryProvider(), existing));
        generator.addProvider(event.includeServer(), new AdvancementProvider(output, lookupProvider, existing, List.of(new AdvancementGenerator())));
    }
}
