package jp.aquafactory.apprenticecodex.capability.codexspelldata;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CodexSpellStates {
    private static final Map<ResourceLocation, CodexSpellStateType<?>> TYPES = new HashMap<>();

    public static <T extends ICodexSpellState> CodexSpellStateType<T> register(String path, Supplier<T> factory) {
        var id = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, path);
        var type = new CodexSpellStateType<>(id, factory);
        if (TYPES.putIfAbsent(id, type) != null) {
            throw new IllegalStateException("Duplicate SpellStateType id: " + id);
        }
        return type;
    }

    public static <T extends ICodexSpellState> CodexSpellStateType<T> get(ResourceLocation id) {
        //noinspection unchecked
        return (CodexSpellStateType<T>) TYPES.get(id);
    }

    public static Collection<CodexSpellStateType<?>> all() {
        return TYPES.values();
    }
}
