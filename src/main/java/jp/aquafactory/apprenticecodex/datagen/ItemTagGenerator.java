package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class ItemTagGenerator extends ItemTagsProvider {
    private static TagKey<Item> createTag(String namespace, String path) {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private static final TagKey<Item> IRONS_STAFF = createTag("irons_spellbooks", "staff");
    private static final TagKey<Item> MALUM_STAFF = createTag("malum", "staff");
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = createTag("malum", "soul_hunter_weapon");

    public ItemTagGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagsProvider.TagLookup<Block>> blockTagLookup,
            ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, blockTagLookup, ApprenticeCodex.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        tag(IRONS_STAFF).add(ItemRegistry.PASTEL_STAFF.get());
        tag(MALUM_STAFF).add(ItemRegistry.PASTEL_STAFF.get());
        tag(MALUM_SOUL_HUNTER_WEAPON).add(ItemRegistry.PASTEL_STAFF.get());
    }
}
