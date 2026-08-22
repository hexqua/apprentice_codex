package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.datagen.recipe.EssenceSmokerRecipeDataGenerator;
import jp.aquafactory.apprenticecodex.datagen.recipe.GrindRunnerRecipeDataGenerator;
import jp.aquafactory.apprenticecodex.datagen.recipe.SpellcasterWorkbenchRecipeDataGenerator;
import jp.aquafactory.apprenticecodex.datagen.recipe.AlchemyBrewerRecipeDataGenerator;
import jp.aquafactory.apprenticecodex.datagen.spell.AutocastAmuletSpellProfileDataGenerator;
import jp.aquafactory.apprenticecodex.datagen.spell.ElementalBowModeDataGenerator;
import jp.aquafactory.apprenticecodex.datagen.spell.ScrollcasterSchoolRuneOverrideDataGenerator;
import jp.aquafactory.apprenticecodex.datagen.spell.SchoolAffinityCatalystOverrideDataGenerator;
import jp.aquafactory.apprenticecodex.datagen.spell.SchoolAffinitySelectionPolicyDataGenerator;
import jp.aquafactory.apprenticecodex.datagen.spell.SearchBeaconTargetDataGenerator;
import jp.aquafactory.apprenticecodex.datagen.spell.SenseEvilHighlightDataGenerator;
import jp.aquafactory.apprenticecodex.datagen.spell.MulticastEchoStaffAttackProfileDataGenerator;
import jp.aquafactory.apprenticecodex.datagen.spell.MulticastEchoStaffMobEffectProfileDataGenerator;
import jp.aquafactory.apprenticecodex.datagen.spell.RemoteOwnerCastSpellProfileDataGenerator;
import jp.aquafactory.apprenticecodex.datagen.spell.SpellDispenserSpellProfileDataGenerator;
import jp.aquafactory.apprenticecodex.datagen.spell.SpellGunSpellListDataGenerator;
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
        generator.addProvider(event.includeServer(), new EntityTypeTagGenerator(output, lookupProvider, existing));
        generator.addProvider(event.includeServer(), new ItemTagGenerator(output, lookupProvider, blockTagGenerator.contentsGetter(), existing));
        generator.addProvider(event.includeServer(), new PoiTypeTagGenerator(output, lookupProvider, existing));
        generator.addProvider(event.includeServer(), new SpellDispenserSpellProfileDataGenerator(output, existing));
        generator.addProvider(event.includeServer(), new AutocastAmuletSpellProfileDataGenerator(output, existing));
        generator.addProvider(event.includeServer(), new RemoteOwnerCastSpellProfileDataGenerator(output, existing));
        generator.addProvider(event.includeServer(), new MulticastEchoStaffAttackProfileDataGenerator(output, existing));
        generator.addProvider(event.includeServer(), new MulticastEchoStaffMobEffectProfileDataGenerator(output, existing));
        generator.addProvider(event.includeServer(), new SpellGunSpellListDataGenerator(output, existing));
        generator.addProvider(event.includeServer(), new ElementalBowModeDataGenerator(output, existing));
        generator.addProvider(event.includeServer(), new SearchBeaconTargetDataGenerator(output, existing));
        generator.addProvider(event.includeServer(), new ScrollcasterSchoolRuneOverrideDataGenerator(output, existing));
        generator.addProvider(event.includeServer(), new SchoolAffinitySelectionPolicyDataGenerator(output, existing));
        generator.addProvider(event.includeServer(), new SchoolAffinityCatalystOverrideDataGenerator(output, existing));
        generator.addProvider(event.includeServer(), new RecipeGenerator(output));
        generator.addProvider(event.includeServer(), new GrindRunnerRecipeDataGenerator(output));
        generator.addProvider(event.includeServer(), new CurioLootDataGenerator(output));
        generator.addProvider(event.includeServer(), new EssenceSmokerRecipeDataGenerator(output));
        generator.addProvider(event.includeServer(), new SpellcasterWorkbenchRecipeDataGenerator(output));
        generator.addProvider(event.includeServer(), new AlchemyBrewerRecipeDataGenerator(output));
        generator.addProvider(event.includeServer(), new LootTableGenerator(output));
        generator.addProvider(event.includeServer(), new SenseEvilHighlightDataGenerator(output, existing));
        generator.addProvider(event.includeServer(), new DamageTypeTagGenerator(output, datapackProvider.getRegistryProvider(), existing));
        generator.addProvider(event.includeServer(), new ForgeAdvancementProvider(output, lookupProvider, existing, List.of(new AdvancementGenerator())));
        generator.addProvider(event.includeServer(), new ConditionalAdvancementDataGenerator(output));
    }
}
