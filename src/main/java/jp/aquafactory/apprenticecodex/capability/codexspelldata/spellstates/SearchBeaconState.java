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

    private final Set<ResourceLocation> searchedStructures = new LinkedHashSet<>();
    private final Set<ResourceLocation> traversedStructures = new LinkedHashSet<>();

    public StructureKnowledge getKnowledge(ResourceLocation structureId) {
        if (traversedStructures.contains(structureId)) {
            return StructureKnowledge.TRAVERSED;
        }
        if (searchedStructures.contains(structureId)) {
            return StructureKnowledge.SEARCHED;
        }
        return StructureKnowledge.UNKNOWN;
    }

    public boolean markSearched(Collection<ResourceLocation> structureIds) {
        return searchedStructures.addAll(structureIds);
    }

    public boolean markTraversed(Collection<ResourceLocation> structureIds) {
        return traversedStructures.addAll(structureIds);
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

    private static ListTag saveIds(Collection<ResourceLocation> ids) {
        var list = new ListTag();
        for (var id : ids) {
            list.add(StringTag.valueOf(id.toString()));
        }
        return list;
    }

    private static void loadIds(ListTag list, Set<ResourceLocation> out) {
        for (int i = 0; i < list.size(); i++) {
            var id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                out.add(id);
            }
        }
    }

    public enum StructureKnowledge {
        UNKNOWN,
        SEARCHED,
        TRAVERSED
    }
}
