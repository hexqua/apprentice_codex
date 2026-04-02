package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class EntityTypeTagGenerator extends TagsProvider<EntityType<?>> {
    public EntityTypeTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                  ExistingFileHelper existingFileHelper) {
        super(output, Registries.ENTITY_TYPE, lookupProvider, ApprenticeCodex.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        // 1.21.1 ではこのMOD独自のアンデッド扱いも EntityType タグで統一する。
        tag(TagRegistry.EntityTypes.COUNTS_AS_UNDEAD)
                .addOptional(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "summoned_zombie"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "summoned_skeleton"));
    }
}
