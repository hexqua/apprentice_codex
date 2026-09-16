package jp.aquafactory.apprenticecodex.datagen.gems;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.redspace.ironsjewelry.core.data.MaterialDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

public final class GemsMaterialDataProvider implements DataProvider {
    private final PackOutput.PathProvider materials;
    private final PackOutput.PathProvider tags;
    private final CompletableFuture<HolderLookup.Provider> lookup;

    public GemsMaterialDataProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        materials = output.createPathProvider(PackOutput.Target.DATA_PACK, "irons_jewelry/material");
        tags = output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/irons_jewelry/material");
        this.lookup = lookup;
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput output) {
        return lookup.thenCompose(registries -> {
            var definitions = GemsMaterials.definitions();
            validateUniqueIds(definitions);
            var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
            var writes = new ArrayList<CompletableFuture<?>>();
            Map<ResourceLocation, List<TagEntry>> memberships = new TreeMap<>();
            for (var material : definitions) {
                var json = encode(material, ops);
                writes.add(DataProvider.saveStable(output, json, materials.json(material.id())));
                for (var tag : material.tags()) {
                    memberships.computeIfAbsent(tag.location(), ignored -> new ArrayList<>()).add(TagEntry.element(material.id()));
                }
            }
            memberships.forEach((id, entries) -> {
                var json = TagFile.CODEC.encodeStart(JsonOps.INSTANCE, new TagFile(entries, false)).getOrThrow();
                writes.add(DataProvider.saveStable(output, json, tags.json(id)));
            });
            return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
        });
    }

    public static void validateUniqueIds(List<GemsMaterial> definitions) {
        var ids = new HashSet<ResourceLocation>();
        for (var material : definitions) {
            if (!ids.add(material.id())) {
                throw new IllegalArgumentException("Duplicate jewelry material ID: " + material.id());
            }
        }
    }

    public static JsonElement encode(GemsMaterial material, RegistryOps<JsonElement> ops) {
        return MaterialDefinition.CODEC.encodeStart(ops, material.definition())
                .getOrThrow(message -> new IllegalStateException("Could not encode jewelry material " + material.id() + ": " + message));
    }

    @Override
    public @NotNull String getName() {
        return "ApprenticeCodex Iron's Gems Materials";
    }
}
