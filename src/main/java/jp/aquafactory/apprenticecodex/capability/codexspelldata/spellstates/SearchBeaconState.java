package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class SearchBeaconState implements ICodexSpellState {
    private static final String SEARCHED_KEY = "searched_structures";
    private static final String TRAVERSED_KEY = "traversed_structures";
    private static final String MARKER_SEPARATOR = "|";

    private final Set<StructureMarker> searchedStructures = new LinkedHashSet<>();
    private final Set<StructureMarker> traversedStructures = new LinkedHashSet<>();

    public StructureKnowledge getKnowledge(StructureMarker marker) {
        if (traversedStructures.contains(marker)) {
            return StructureKnowledge.TRAVERSED;
        }
        if (searchedStructures.contains(marker)) {
            return StructureKnowledge.SEARCHED;
        }
        return StructureKnowledge.UNKNOWN;
    }

    public boolean markSearched(Collection<StructureMarker> markers) {
        return searchedStructures.addAll(markers);
    }

    public boolean markTraversed(Collection<StructureMarker> markers) {
        return traversedStructures.addAll(markers);
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.put(SEARCHED_KEY, saveIds(searchedStructures));
        tag.put(TRAVERSED_KEY, saveIds(traversedStructures));
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        searchedStructures.clear();
        traversedStructures.clear();
        loadIds(tag.getList(SEARCHED_KEY, Tag.TAG_STRING), searchedStructures);
        loadIds(tag.getList(TRAVERSED_KEY, Tag.TAG_STRING), traversedStructures);
    }

    private static ListTag saveIds(Collection<StructureMarker> ids) {
        var list = new ListTag();
        for (var id : ids) {
            list.add(StringTag.valueOf(id.asString()));
        }
        return list;
    }

    private static void loadIds(ListTag list, Set<StructureMarker> out) {
        for (int i = 0; i < list.size(); i++) {
            var id = StructureMarker.parse(list.getString(i));
            if (id != null) {
                out.add(id);
            }
        }
    }

    public record StructureMarker(ResourceLocation dimensionId, ResourceLocation structureId, long startChunkPos) {
        public String asString() {
            return dimensionId + MARKER_SEPARATOR + structureId + MARKER_SEPARATOR + Long.toUnsignedString(startChunkPos);
        }

        public static StructureMarker parse(String raw) {
            var firstSeparator = raw.indexOf(MARKER_SEPARATOR);
            if (firstSeparator < 0) {
                return null;
            }

            var secondSeparator = raw.indexOf(MARKER_SEPARATOR, firstSeparator + MARKER_SEPARATOR.length());
            if (secondSeparator < 0) {
                return null;
            }

            var dimensionId = ResourceLocation.tryParse(raw.substring(0, firstSeparator));
            var structureId = ResourceLocation.tryParse(raw.substring(firstSeparator + MARKER_SEPARATOR.length(), secondSeparator));
            if (dimensionId == null || structureId == null) {
                return null;
            }

            try {
                var startChunkPos = Long.parseUnsignedLong(raw.substring(secondSeparator + MARKER_SEPARATOR.length()));
                return new StructureMarker(dimensionId, structureId, startChunkPos);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }

    public enum StructureKnowledge {
        UNKNOWN,
        SEARCHED,
        TRAVERSED
    }
}
