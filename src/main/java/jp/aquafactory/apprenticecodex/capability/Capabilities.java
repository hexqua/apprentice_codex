package jp.aquafactory.apprenticecodex.capability;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData;
import jp.aquafactory.apprenticecodex.capability.endergrimoire.EnderGrimoireSpellbookData;
import jp.aquafactory.apprenticecodex.capability.personalinventory.PersonalInventory;
import jp.aquafactory.apprenticecodex.registry.AttachmentRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

public final class Capabilities {
    private Capabilities() {
    }

    public static Optional<PersonalInventory> getPersonalInventory(Entity entity) {
        if (!(entity instanceof Player player)) {
            return Optional.empty();
        }
        return Optional.of(player.getData(AttachmentRegistry.PERSONAL_INVENTORY));
    }

    public static Optional<CodexSpellData> getSpellData(Entity entity) {
        if (!(entity instanceof Player player)) {
            return Optional.empty();
        }
        return Optional.of(player.getData(AttachmentRegistry.SPELL_DATA));
    }

    public static Optional<EnderGrimoireSpellbookData> getEnderGrimoireSpellbook(Entity entity) {
        if (!(entity instanceof Player player)) {
            return Optional.empty();
        }
        return Optional.of(player.getData(AttachmentRegistry.ENDER_GRIMOIRE_SPELLBOOK));
    }

    public static void withSpellData(Entity entity, Consumer<CodexSpellData> consumer) {
        getSpellData(entity).ifPresent(consumer);
    }

    public static void withEnderGrimoireSpellbook(Entity entity, Consumer<EnderGrimoireSpellbookData> consumer) {
        getEnderGrimoireSpellbook(entity).ifPresent(consumer);
    }

    public static @Nullable CodexSpellData getSpellDataOrNull(Entity entity) {
        return getSpellData(entity).orElse(null);
    }

    public static @Nullable EnderGrimoireSpellbookData getEnderGrimoireSpellbookOrNull(Entity entity) {
        return getEnderGrimoireSpellbook(entity).orElse(null);
    }
}
