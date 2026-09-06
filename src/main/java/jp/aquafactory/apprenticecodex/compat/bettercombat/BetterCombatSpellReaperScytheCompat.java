package jp.aquafactory.apprenticecodex.compat.bettercombat;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumSpellReaperScytheBridge;
import jp.aquafactory.apprenticecodex.mixin.BetterCombatWeaponRegistryAccessor;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.bettercombat.api.WeaponAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class BetterCombatSpellReaperScytheCompat {
    public static final ResourceLocation NO_SWEEP_ATTRIBUTES = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "spell_reaper_scythe_no_sweep"
    );

    private BetterCombatSpellReaperScytheCompat() {
    }

    public static WeaponAttributes resolveAttackAttributes(
            Player player,
            ItemStack stack,
            WeaponAttributes originalAttributes
    ) {
        if (jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowManager.isThrown(stack)) return null;
        if (!stack.is(ItemRegistry.SPELL_REAPER_SCYTHE.get())
                || !MalumSpellReaperScytheBridge.shouldUseNoSweepCombo(player)) {
            return originalAttributes;
        }

        var noSweepAttributes = BetterCombatWeaponRegistryAccessor
                .apprenticecodex$getAttributes(NO_SWEEP_ATTRIBUTES);
        return noSweepAttributes != null ? noSweepAttributes : originalAttributes;
    }
}
