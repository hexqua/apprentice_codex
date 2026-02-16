package jp.aquafactory.apprenticecodex.capability;

import jp.aquafactory.apprenticecodex.capability.personalinventory.PersonalInventory;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.NonNullConsumer;
import org.jetbrains.annotations.Nullable;

public final class Capabilities {
    public static Capability<PersonalInventory> PERSONAL_INVENTORY = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static Capability<CodexSpellData> SPELL_DATA = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static void withSpellData(Entity entity, NonNullConsumer<CodexSpellData> consumer) {
        entity.getCapability(Capabilities.SPELL_DATA).ifPresent(consumer);
    }

    @SuppressWarnings("DataFlowIssue")
    public @Nullable
    static CodexSpellData getSpellDataOrNull(Entity entity) {
        return entity.getCapability(Capabilities.SPELL_DATA).orElse(null);
    }
}