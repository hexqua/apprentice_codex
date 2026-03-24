package jp.aquafactory.apprenticecodex.utility;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SchoolAffinitySelectionPolicy(
        List<ResourceLocation> priorities,
        List<ResourceLocation> deny
) {
    public static final SchoolAffinitySelectionPolicy EMPTY = new SchoolAffinitySelectionPolicy(List.of(), List.of());
    public static final Codec<SchoolAffinitySelectionPolicy> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("priorities", List.of()).forGetter(SchoolAffinitySelectionPolicy::priorities),
            ResourceLocation.CODEC.listOf().optionalFieldOf("deny", List.of()).forGetter(SchoolAffinitySelectionPolicy::deny)
    ).apply(instance, SchoolAffinitySelectionPolicy::new));

    public SchoolAffinitySelectionPolicy {
        priorities = List.copyOf(priorities);
        deny = List.copyOf(deny);
    }

    public boolean isEmpty() {
        return priorities.isEmpty() && deny.isEmpty();
    }
}
