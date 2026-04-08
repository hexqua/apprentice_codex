package jp.aquafactory.apprenticecodex.datagen.spell;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetList;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SearchBeaconTargetDataGenerator extends JsonCodecProvider<SearchBeaconTargetList> {
    public SearchBeaconTargetDataGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                PackOutput.Target.DATA_PACK,
                SearchBeaconTargetManager.DIRECTORY,
                PackType.SERVER_DATA,
                SearchBeaconTargetList.CODEC,
                lookupProvider,
                ApprenticeCodex.MODID,
                existingFileHelper
        );
    }

    @Override
    protected void gather() {
        unconditional(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "default"),
                new SearchBeaconTargetList(List.of(
                        new SearchBeaconTargetList.Definition(
                                ResourceLocation.withDefaultNamespace("blaze_rod"),
                                List.of(new SearchBeaconTargetList.TargetReference(false,
                                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "pyromancer_tower")))
                        ),
                        new SearchBeaconTargetList.Definition(
                                ResourceLocation.withDefaultNamespace("emerald"),
                                List.of(new SearchBeaconTargetList.TargetReference(false,
                                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "evoker_fort")))
                        ),
                        new SearchBeaconTargetList.Definition(
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "divine_pearl"),
                                List.of(new SearchBeaconTargetList.TargetReference(true,
                                        ResourceLocation.withDefaultNamespace("village")))
                        ),
                        new SearchBeaconTargetList.Definition(
                                ResourceLocation.withDefaultNamespace("poisonous_potato"),
                                List.of(new SearchBeaconTargetList.TargetReference(false,
                                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "mangrove_hut")))
                        ),
                        new SearchBeaconTargetList.Definition(
                                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "frozen_bone"),
                                List.of(new SearchBeaconTargetList.TargetReference(false,
                                        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "mountain_tower")))
                        ),
                        new SearchBeaconTargetList.Definition(
                                ResourceLocation.withDefaultNamespace("sculk_sensor"),
                                List.of(new SearchBeaconTargetList.TargetReference(false,
                                        ResourceLocation.withDefaultNamespace("ancient_city")))
                        )
                ))
        );
    }
}
