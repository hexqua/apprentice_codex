package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightSpellSupport;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

public final class WeaponImbueCooldownHelper {
    private WeaponImbueCooldownHelper() {
    }

    public static int getEffectiveSpellCooldown(
            AbstractSpell spell,
            Player player,
            CastSource castSource,
            @Nullable ItemStack castingStack
    ) {
        var baseCooldown = resolveLimitedBaseCooldown(spell, player);
        var playerCooldownModifier = player.getAttributeValue(AttributeRegistry.COOLDOWN_REDUCTION);
        var itemCooldownModifier = castSource == CastSource.SWORD
                && !shouldIgnoreWeaponImbueCooldownMultiplier(castingStack, spell, castSource)
                ? ServerConfigs.SWORDS_CD_MULTIPLIER.get().floatValue()
                : 1.0f;
        return (int) (baseCooldown * (2 - Utils.softCapFormula(playerCooldownModifier)) * itemCooldownModifier);
    }

    private static int resolveLimitedBaseCooldown(AbstractSpell spell, Player player) {
        var baseCooldown = Math.max(0, spell.getSpellCooldown());
        if (baseCooldown == 0) {
            return 0;
        }

        var craftsmansDelightCooldown = CraftsmansDelightSpellSupport.isCooldownReductionTarget(spell)
                ? CraftsmansDelight.applyCooldownDiscount(baseCooldown, player)
                : baseCooldown;
        return selectStrongestLimitedBaseCooldown(baseCooldown, craftsmansDelightCooldown);
    }

    public static int selectStrongestLimitedBaseCooldown(int baseCooldown, int... candidateCooldowns) {
        var selectedCooldown = Math.max(0, baseCooldown);
        if (selectedCooldown == 0) {
            return 0;
        }

        for (var candidateCooldown : candidateCooldowns) {
            if (candidateCooldown > 0 && candidateCooldown < selectedCooldown) {
                selectedCooldown = candidateCooldown;
            }
        }
        return selectedCooldown;
    }

    public static int getEffectiveSpellCooldown(
            AbstractSpell spell,
            Player player,
            CastSource castSource,
            @Nullable String selectionSlot
    ) {
        return getEffectiveSpellCooldown(spell, player, castSource, resolveSelectionStack(player, selectionSlot));
    }

    public static ItemStack resolveSelectionStack(Player player, @Nullable String selectionSlot) {
        if (selectionSlot == null || selectionSlot.isBlank()) {
            return ItemStack.EMPTY;
        }
        if (SpellSelectionManager.MAINHAND.equals(selectionSlot)) {
            return player.getMainHandItem();
        }
        if (SpellSelectionManager.OFFHAND.equals(selectionSlot)) {
            return player.getOffhandItem();
        }
        for (var slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && slot.getName().equals(selectionSlot)) {
                return player.getItemBySlot(slot);
            }
        }
        if (selectionSlot.equals(Curios.SPELLBOOK_SLOT) || selectionSlot.startsWith(Curios.SPELLBOOK_SLOT + "_")) {
            var spellbook = Utils.getPlayerSpellbookStack(player);
            return spellbook == null ? ItemStack.EMPTY : spellbook;
        }

        var separator = selectionSlot.lastIndexOf('_');
        if (separator <= 0 || separator == selectionSlot.length() - 1) {
            return ItemStack.EMPTY;
        }
        var identifier = selectionSlot.substring(0, separator);
        try {
            var index = Integer.parseInt(selectionSlot.substring(separator + 1));
            return CuriosApi.getCuriosInventory(player)
                    .flatMap(inventory -> inventory.findCurio(identifier, index))
                    .map(slotResult -> slotResult.stack())
                    .orElse(ItemStack.EMPTY);
        } catch (NumberFormatException ignored) {
            return ItemStack.EMPTY;
        }
    }

    public static boolean shouldIgnoreWeaponImbueCooldownMultiplier(
            @Nullable ItemStack stack,
            @Nullable AbstractSpell spell,
            CastSource castSource
    ) {
        return stack != null
                && !stack.isEmpty()
                && stack.getItem() instanceof WeaponImbueCooldownPolicyItem policyItem
                && policyItem.ignoresWeaponImbueCooldownMultiplier(stack, spell, castSource);
    }
}
