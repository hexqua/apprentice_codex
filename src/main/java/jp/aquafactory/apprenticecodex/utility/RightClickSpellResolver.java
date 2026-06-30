package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.CastingItem;
import io.redspace.ironsspellbooks.item.Scroll;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.RightClickSpellSourceItem;
import jp.aquafactory.apprenticecodex.item.RightClickSpellItemHelper;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class RightClickSpellResolver {
    private RightClickSpellResolver() {
    }

    public static Optional<ResolvedRightClickSpell> resolve(Player player) {
        var mainHandStack = player.getMainHandItem();
        var offHandStack = player.getOffhandItem();

        // 先頭 spell 固定で発動するアイテムは選択 spell より先に解決する。
        if (mainHandStack.getItem() instanceof AbstractSpellGunItem) {
            return resolveContainerSpell(mainHandStack, player, "spell_gun");
        }
        if (mainHandStack.getItem() instanceof Scroll) {
            return resolveContainerSpell(mainHandStack, player, "scroll");
        }
        if (mainHandStack.getItem() instanceof ScrollcasterGauntlet) {
            return createResolvedSpell(
                    ScrollcasterGauntlet.getSelectedSpellData(mainHandStack),
                    player,
                    "scrollcaster_gauntlet_selected"
            );
        }
        if (mainHandStack.getItem() instanceof RightClickSpellSourceItem rightClickSpellSourceItem) {
            return createResolvedSpell(
                    rightClickSpellSourceItem.getRightClickSpellData(mainHandStack, player, InteractionHand.MAIN_HAND),
                    player,
                    "right_click_spell_source_item"
            );
        }
        // 独自右クリック武器は CastingItem 継承ではないが、右クリック時は選択 spell を使う。
        if (mainHandStack.getItem() instanceof AbstractRightClickMagicWeaponItem) {
            return resolveSelectionSpell(player, "right_click_magic_weapon_selection");
        }
        if (mainHandStack.getItem() instanceof CastingItem || RightClickSpellItemHelper.isRightClickSpellItem(mainHandStack)) {
            return resolveSelectionSpell(player, "casting_item_selection");
        }
        // オフハンド魔法はメインハンドの右クリック優先条件と同じ判定でのみ解放する。
        if (offHandStack.getItem() instanceof AbstractOffhandMagicItem
                && !RightClickSpellItemHelper.hasMainHandRightClickBehavior(player, mainHandStack)) {
            return resolveSelectionSpell(player, "offhand_magic_selection");
        }

        return Optional.empty();
    }

    private static Optional<ResolvedRightClickSpell> resolveSelectionSpell(Player player, String resolutionPath) {
        var selectionOption = new SpellSelectionManager(player).getSelection();
        if (selectionOption == null) {
            return Optional.empty();
        }

        return createResolvedSpell(selectionOption.spellData, player, resolutionPath);
    }

    private static Optional<ResolvedRightClickSpell> resolveContainerSpell(ItemStack stack, Player player, String resolutionPath) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            return Optional.empty();
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return Optional.empty();
        }

        return createResolvedSpell(spellContainer.getSpellAtIndex(0), player, resolutionPath);
    }

    private static Optional<ResolvedRightClickSpell> createResolvedSpell(@Nullable SpellData spellData, Player player, String resolutionPath) {
        if (spellData == null || spellData == SpellData.EMPTY) {
            return Optional.empty();
        }

        var spell = spellData.getSpell();
        if (spell == null || spell == SpellRegistry.none()) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedRightClickSpell(
                spellData,
                spell.getSpellResource(),
                spell.getLevelFor(spellData.getLevel(), player),
                resolutionPath
        ));
    }

    public record ResolvedRightClickSpell(
            SpellData spellData,
            ResourceLocation spellResource,
            int spellLevel,
            String resolutionPath
    ) {
    }
}
