package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class TamersPocketState implements ICodexSpellState {
    private final List<StoredPet> storedPets = new ArrayList<>();
    private int clientSyncedStoredPetCount = -1;

    public int getStoredPetCount() {
        if (clientSyncedStoredPetCount >= 0) {
            return clientSyncedStoredPetCount;
        }
        return storedPets.size();
    }

    public void setClientSyncedStoredPetCount(int clientSyncedStoredPetCount) {
        this.clientSyncedStoredPetCount = Math.max(0, clientSyncedStoredPetCount);
    }

    public List<StoredPet> getStoredPetsSnapshot() {
        return storedPets.stream().map(StoredPet::copy).toList();
    }

    public void addStoredPet(StoredPet storedPet) {
        storedPets.add(storedPet);
    }

    public void removeStoredPets(Collection<UUID> entryIds) {
        if (entryIds.isEmpty()) {
            return;
        }
        storedPets.removeIf(storedPet -> entryIds.contains(storedPet.entryId()));
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        var pets = new ListTag();
        for (var storedPet : storedPets) {
            pets.add(storedPet.save());
        }
        tag.put("StoredPets", pets);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        storedPets.clear();
        clientSyncedStoredPetCount = -1;
        var pets = tag.getList("StoredPets", Tag.TAG_COMPOUND);
        for (int i = 0; i < pets.size(); ++i) {
            storedPets.add(StoredPet.load(pets.getCompound(i)));
        }
    }

    public record StoredPet(
            UUID entryId,
            String entityTypeId,
            CompoundTag entityData,
            long storedGameTime,
            @Nullable String displayName
    ) {
        public StoredPet copy() {
            return new StoredPet(entryId, entityTypeId, entityData.copy(), storedGameTime, displayName);
        }

        public CompoundTag createSpawnTag() {
            var tag = entityData.copy();
            tag.putString("id", entityTypeId);
            return tag;
        }

        private CompoundTag save() {
            var tag = new CompoundTag();
            tag.putUUID("EntryId", entryId);
            tag.putString("EntityTypeId", entityTypeId);
            tag.put("EntityData", entityData.copy());
            tag.putLong("StoredGameTime", storedGameTime);
            if (displayName != null && !displayName.isBlank()) {
                tag.putString("DisplayName", displayName);
            }
            return tag;
        }

        private static StoredPet load(CompoundTag tag) {
            return new StoredPet(
                    tag.hasUUID("EntryId") ? tag.getUUID("EntryId") : UUID.randomUUID(),
                    tag.getString("EntityTypeId"),
                    tag.getCompound("EntityData").copy(),
                    tag.getLong("StoredGameTime"),
                    tag.contains("DisplayName") ? tag.getString("DisplayName") : null
            );
        }
    }
}
