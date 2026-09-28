package jp.aquafactory.apprenticecodex.capability;

import jp.aquafactory.apprenticecodex.capability.companiontrunkinventory.CompanionTrunkInventory;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateType;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import jp.aquafactory.apprenticecodex.capability.endergrimoire.EnderGrimoireSpellbookData;
import jp.aquafactory.apprenticecodex.capability.personalinventory.PersonalInventory;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.NonNullConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public final class Capabilities {
    public static Capability<PersonalInventory> PERSONAL_INVENTORY = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static Capability<CompanionTrunkInventory> COMPANION_TRUNK_INVENTORY = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static Capability<CodexSpellData> SPELL_DATA = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static Capability<EnderGrimoireSpellbookData> ENDER_GRIMOIRE_SPELLBOOK = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static void withSpellData(Entity entity, NonNullConsumer<CodexSpellData> consumer) {
        tryWithSpellData(entity, consumer);
    }

    public static boolean tryWithSpellData(Entity entity, NonNullConsumer<CodexSpellData> consumer) {
        var data = getSpellDataOrNull(entity);
        if (entity.isRemoved() || data == null) {
            return false;
        }
        consumer.accept(data);
        return true;
    }

    public static void withEnderGrimoireSpellbook(Entity entity, NonNullConsumer<EnderGrimoireSpellbookData> consumer) {
        var data = getEnderGrimoireSpellbookOrNull(entity);
        if (entity.isRemoved() || data == null) {
            return;
        }
        consumer.accept(data);
    }

    // ForgeのCapabilityが失効しても読み取り結果だけは既定値へ退避する。
    // Capability自体の代用品を渡すと、保存されない書き込みを成功扱いしてしまう。
    public static <T, R> R readOrDefault(Entity entity, Capability<T> capability,
                                         Function<T, R> reader, Supplier<R> defaultSupplier) {
        return entity.getCapability(capability).resolve().map(reader).orElseGet(defaultSupplier);
    }

    public static <T extends ICodexSpellState, R> R readSpellStateOrDefault(
            Entity entity, CodexSpellStateType<T> type, Function<T, R> reader) {
        return readOrDefault(entity, SPELL_DATA, data -> reader.apply(data.get(type)),
                () -> reader.apply(type.create()));
    }

    @SuppressWarnings("DataFlowIssue")
    public @Nullable
    static CodexSpellData getSpellDataOrNull(Entity entity) {
        return entity.getCapability(Capabilities.SPELL_DATA).orElse(null);
    }

    @SuppressWarnings("DataFlowIssue")
    public static @Nullable CompanionTrunkInventory getCompanionTrunkInventoryOrNull(Entity entity) {
        return entity.getCapability(Capabilities.COMPANION_TRUNK_INVENTORY).orElse(null);
    }

    @SuppressWarnings("DataFlowIssue")
    public static @Nullable PersonalInventory getPersonalInventoryOrNull(Entity entity) {
        return entity.getCapability(Capabilities.PERSONAL_INVENTORY).orElse(null);
    }

    @SuppressWarnings("DataFlowIssue")
    public static @Nullable EnderGrimoireSpellbookData getEnderGrimoireSpellbookOrNull(Entity entity) {
        return entity.getCapability(Capabilities.ENDER_GRIMOIRE_SPELLBOOK).orElse(null);
    }
}
