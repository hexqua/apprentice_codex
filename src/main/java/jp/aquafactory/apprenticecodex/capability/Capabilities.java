package jp.aquafactory.apprenticecodex.capability;

import jp.aquafactory.apprenticecodex.capability.personalinventory.PersonalInventory;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData;
import jp.aquafactory.apprenticecodex.capability.endergrimoire.EnderGrimoireSpellbookData;
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
    public static Capability<EnderGrimoireSpellbookData> ENDER_GRIMOIRE_SPELLBOOK = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static void withSpellData(Entity entity, NonNullConsumer<CodexSpellData> consumer) {
        entity.getCapability(Capabilities.SPELL_DATA).ifPresent(consumer);
    }

    public static void withEnderGrimoireSpellbook(Entity entity, NonNullConsumer<EnderGrimoireSpellbookData> consumer) {
        entity.getCapability(Capabilities.ENDER_GRIMOIRE_SPELLBOOK).ifPresent(consumer);
    }

    @SuppressWarnings("DataFlowIssue")
    public @Nullable
    static CodexSpellData getSpellDataOrNull(Entity entity) {
        return entity.getCapability(Capabilities.SPELL_DATA).orElse(null);
    }

    @SuppressWarnings("DataFlowIssue")
    public static @Nullable EnderGrimoireSpellbookData getEnderGrimoireSpellbookOrNull(Entity entity) {
        return entity.getCapability(Capabilities.ENDER_GRIMOIRE_SPELLBOOK).orElse(null);
    }
}
