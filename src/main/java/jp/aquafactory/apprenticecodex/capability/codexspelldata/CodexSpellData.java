package jp.aquafactory.apprenticecodex.capability.codexspelldata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class CodexSpellData {
    private final Map<ResourceLocation, ICodexSpellState> states = new HashMap<>();
    private final Set<ResourceLocation> dirty = new HashSet<>();

    public <T extends ICodexSpellState> T get(CodexSpellStateType<T> type) {
        // 必要になった時だけ生成.
        //noinspection unchecked
        return (T) states.computeIfAbsent(type.id(), id -> type.create());
    }

    public void markDirty(ResourceLocation id) {
        dirty.add(id);
    }

    public Set<ResourceLocation> consumeDirty() {
        var copy = Set.copyOf(dirty);
        dirty.clear();
        return copy;
    }

    public CompoundTag saveAll() {
        var root = new CompoundTag();
        var data = new CompoundTag();

        for (var e : states.entrySet()) {
            data.put(e.getKey().toString(), e.getValue().save());
        }
        root.put("states", data);
        return root;
    }

    public void loadAll(CompoundTag root) {
        states.clear();
        dirty.clear();

        var data = root.getCompound("states");
        for (var key : data.getAllKeys()) {
            var id = ResourceLocation.parse(key);
            var type = CodexSpellStates.get(id);
            if (type == null) {
                // 未知のものであれば捨てる.
                continue;
            }
            var state = type.create();
            state.load(data.getCompound(key));
            states.put(id, state);
        }
    }

    public <T extends ICodexSpellState> void edit(CodexSpellStateType<T> type, Consumer<T> editor) {
        T state = get(type);
        editor.accept(state);
        markDirty(type.id());
    }
}
