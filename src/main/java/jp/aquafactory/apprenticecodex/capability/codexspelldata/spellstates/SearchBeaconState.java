package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class SearchBeaconState implements ICodexSpellState {
    private static final String SEARCHED_KEY = "searched_structures";
    private static final String TRAVERSED_KEY = "traversed_structures";
    private static final String PENDING_INSTANT_BRAZIER_UUID_KEY = "pending_instant_brazier_uuid";
    private static final String PENDING_INSTANT_BRAZIER_STACK_KEY = "pending_instant_brazier_stack";
    private static final String MARKER_SEPARATOR = "|";

    private final Set<StructureMarker> searchedStructures = new LinkedHashSet<>();
    private final Set<StructureMarker> traversedStructures = new LinkedHashSet<>();
    private @Nullable UUID pendingInstantBrazierUuid;
    private CompoundTag pendingInstantBrazierStack = new CompoundTag();

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

    public boolean reserveInstantBrazier(UUID beaconUuid, CompoundTag refundStack) {
        if (hasPendingInstantBrazier() || refundStack.isEmpty()) {
            return false;
        }
        pendingInstantBrazierUuid = beaconUuid;
        pendingInstantBrazierStack = refundStack.copy();
        return true;
    }

    public boolean hasPendingInstantBrazier() {
        return pendingInstantBrazierUuid != null && !pendingInstantBrazierStack.isEmpty();
    }

    public boolean matchesPendingInstantBrazier(UUID beaconUuid) {
        return hasPendingInstantBrazier() && beaconUuid.equals(pendingInstantBrazierUuid);
    }

    public CompoundTag claimPendingInstantBrazier(@Nullable UUID expectedBeaconUuid) {
        if (!hasPendingInstantBrazier()
                || expectedBeaconUuid != null && !expectedBeaconUuid.equals(pendingInstantBrazierUuid)) {
            return new CompoundTag();
        }
        var refundStack = pendingInstantBrazierStack.copy();
        clearPendingInstantBrazier();
        return refundStack;
    }

    private void clearPendingInstantBrazier() {
        pendingInstantBrazierUuid = null;
        pendingInstantBrazierStack = new CompoundTag();
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.put(SEARCHED_KEY, saveIds(searchedStructures));
        tag.put(TRAVERSED_KEY, saveIds(traversedStructures));
        if (hasPendingInstantBrazier()) {
            // 直前のhasPendingInstantBrazierはpendingInstantBrazierUuid != nullを判定している.
            //noinspection DataFlowIssue
            tag.putUUID(PENDING_INSTANT_BRAZIER_UUID_KEY, pendingInstantBrazierUuid);
            tag.put(PENDING_INSTANT_BRAZIER_STACK_KEY, pendingInstantBrazierStack.copy());
        }
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        searchedStructures.clear();
        traversedStructures.clear();
        loadIds(tag.getList(SEARCHED_KEY, Tag.TAG_STRING), searchedStructures);
        loadIds(tag.getList(TRAVERSED_KEY, Tag.TAG_STRING), traversedStructures);
        if (tag.hasUUID(PENDING_INSTANT_BRAZIER_UUID_KEY)
                && tag.contains(PENDING_INSTANT_BRAZIER_STACK_KEY, Tag.TAG_COMPOUND)) {
            pendingInstantBrazierUuid = tag.getUUID(PENDING_INSTANT_BRAZIER_UUID_KEY);
            pendingInstantBrazierStack = tag.getCompound(PENDING_INSTANT_BRAZIER_STACK_KEY).copy();
        } else {
            clearPendingInstantBrazier();
        }
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
