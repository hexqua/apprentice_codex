package jp.aquafactory.apprenticecodex.datagen.spell;

import com.mojang.serialization.JsonOps;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetList;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconTargetManager;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;

import java.util.List;
import java.util.Map;

public final class SearchBeaconTargetDataGenerator extends JsonCodecProvider<SearchBeaconTargetList> {
    public SearchBeaconTargetDataGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(
                output,
                existingFileHelper,
                ApprenticeCodex.MODID,
                JsonOps.INSTANCE,
                PackType.SERVER_DATA,
                SearchBeaconTargetManager.DIRECTORY,
                SearchBeaconTargetList.CODEC,
                Map.of(
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
                )
        );
    }
}
