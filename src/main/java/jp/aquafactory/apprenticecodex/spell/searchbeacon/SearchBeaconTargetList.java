package jp.aquafactory.apprenticecodex.spell.searchbeacon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SearchBeaconTargetList(List<Definition> values) {
    public static final SearchBeaconTargetList EMPTY = new SearchBeaconTargetList(List.of());
    public static final Codec<SearchBeaconTargetList> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Definition.CODEC.listOf()
                            .optionalFieldOf("values", List.of())
                            .forGetter(SearchBeaconTargetList::values)
            ).apply(instance, SearchBeaconTargetList::new)
    );

    public SearchBeaconTargetList {
        values = List.copyOf(values);
    }

    public record Definition(ResourceLocation item, List<TargetReference> targets) {
        public static final Codec<Definition> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ResourceLocation.CODEC.fieldOf("item").forGetter(Definition::item),
                        TargetReference.CODEC.listOf().fieldOf("targets").forGetter(Definition::targets)
                ).apply(instance, Definition::new)
        );

        public Definition {
            targets = List.copyOf(targets);
        }
    }

    public record TargetReference(boolean tag, ResourceLocation id) {
        public static final Codec<TargetReference> CODEC = Codec.STRING.comapFlatMap(
                TargetReference::parse,
                TargetReference::toDataString
        );

        private static DataResult<TargetReference> parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return DataResult.error(() -> "SearchBeacon target reference must not be empty.");
            }

            var isTag = raw.startsWith("#");
            var idText = isTag ? raw.substring(1) : raw;
            var id = ResourceLocation.tryParse(idText);
            if (id == null) {
                return DataResult.error(() -> "Invalid SearchBeacon target reference: " + raw);
            }
            return DataResult.success(new TargetReference(isTag, id));
        }

        public String toDataString() {
            return tag ? "#" + id : id.toString();
        }

        public String toDisplayString() {
            return toDataString();
        }
    }
}
