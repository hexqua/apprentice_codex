package jp.aquafactory.apprenticecodex.utility;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SchoolAffinityCatalystOverrides(
        List<Entry> overrides
) {
    public static final SchoolAffinityCatalystOverrides EMPTY = new SchoolAffinityCatalystOverrides(List.of());
    public static final Codec<SchoolAffinityCatalystOverrides> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Entry.CODEC.listOf().optionalFieldOf("overrides", List.of()).forGetter(SchoolAffinityCatalystOverrides::overrides)
    ).apply(instance, SchoolAffinityCatalystOverrides::new));

    public SchoolAffinityCatalystOverrides {
        overrides = List.copyOf(overrides);
    }

    public boolean isEmpty() {
        return overrides.isEmpty();
    }

    public record Entry(
            ResourceLocation school,
            ResourceLocation item
    ) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("school").forGetter(Entry::school),
                ResourceLocation.CODEC.fieldOf("item").forGetter(Entry::item)
        ).apply(instance, Entry::new));
    }
}
