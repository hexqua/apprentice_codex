package jp.aquafactory.apprenticecodex.utility;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record ScrollcasterSchoolRuneOverrides(
        List<Entry> overrides
) {
    public static final ScrollcasterSchoolRuneOverrides EMPTY = new ScrollcasterSchoolRuneOverrides(List.of());
    public static final Codec<ScrollcasterSchoolRuneOverrides> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Entry.CODEC.listOf().optionalFieldOf("overrides", List.of()).forGetter(ScrollcasterSchoolRuneOverrides::overrides)
    ).apply(instance, ScrollcasterSchoolRuneOverrides::new));

    public ScrollcasterSchoolRuneOverrides {
        overrides = List.copyOf(overrides);
    }

    public boolean isEmpty() {
        return overrides.isEmpty();
    }

    public record Entry(
            ResourceLocation item,
            ResourceLocation school
    ) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(Entry::item),
                ResourceLocation.CODEC.fieldOf("school").forGetter(Entry::school)
        ).apply(instance, Entry::new));
    }
}
