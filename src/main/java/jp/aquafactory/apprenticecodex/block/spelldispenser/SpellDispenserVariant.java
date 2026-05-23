package jp.aquafactory.apprenticecodex.block.spelldispenser;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public enum SpellDispenserVariant {
    NORMAL(false),
    CREATIVE(true);

    private final boolean creative;

    SpellDispenserVariant(boolean creative) {
        this.creative = creative;
    }

    public boolean isCreative() {
        return creative;
    }

    public boolean storesOwnerProfile() {
        return !creative;
    }

    public boolean dropsStoredItems() {
        return !creative;
    }

    public boolean restrictsGuiAccess() {
        return creative;
    }

    public boolean restrictsFailureNotices() {
        return creative;
    }

    public boolean isManaConsumptionExempt() {
        return creative && !ApprenticeCodexServerConfig.creativeSpellDispenserManaConsumption();
    }

    public double cooldownMultiplier() {
        return creative
                ? ApprenticeCodexServerConfig.creativeSpellDispenserCooldownMultiplier()
                : ApprenticeCodexServerConfig.spellDispenserCooldownMultiplier();
    }

    public boolean canOpenMenu(@NotNull Player player) {
        return !restrictsGuiAccess() || canUseCreativeVariant(player);
    }

    public static boolean canUseCreativeVariant(@NotNull Player player) {
        return player.isCreative() || player.hasPermissions(2);
    }

    public Item getItem() {
        return switch (this) {
            case NORMAL -> ItemRegistry.SPELL_DISPENSER.get();
            case CREATIVE -> ItemRegistry.CREATIVE_SPELL_DISPENSER.get();
        };
    }

    public static SpellDispenserVariant fromState(@NotNull BlockState state) {
        if (state.is(BlockRegistry.CREATIVE_SPELL_DISPENSER.get())) {
            return CREATIVE;
        }
        return NORMAL;
    }
}
