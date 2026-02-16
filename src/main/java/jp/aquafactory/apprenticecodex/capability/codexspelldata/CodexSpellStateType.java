package jp.aquafactory.apprenticecodex.capability.codexspelldata;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class CodexSpellStateType<T extends ICodexSpellState> {
    private final ResourceLocation id;
    private final Supplier<T> factory;

    public CodexSpellStateType(ResourceLocation id, Supplier<T> factory) {
        this.id = id;
        this.factory = factory;
    }

    public ResourceLocation id() { return id; }
    public T create() { return factory.get(); }
}
