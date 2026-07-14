package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.CastingItem;
import io.redspace.ironsspellbooks.item.Scroll;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.PriorityOffhandUseDeferringItem;
import jp.aquafactory.apprenticecodex.item.RightClickSpellItemHelper;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.StorageStabilizer;
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

        if (shouldDeferMainHandToPriorityOffhandUse(mainHandStack, offHandStack)) {
            return resolveOffhandUseItemSpell(player, offHandStack);
        }

        var mainHandSpell = resolveMainHandSpell(player, mainHandStack);
        if (mainHandSpell.isPresent()) {
            return mainHandSpell;
        }

        var offhandUseSpell = resolveOffhandUseItemSpell(player, offHandStack);
        if (offhandUseSpell.isPresent()) {
            return offhandUseSpell;
        }

        // オフハンド魔法はメインハンドの右クリック優先条件と同じ判定でのみ解放する。
        if (offHandStack.getItem() instanceof AbstractOffhandMagicItem
                && !RightClickSpellItemHelper.hasMainHandRightClickBehavior(player, mainHandStack)) {
            return resolveSelectionSpell(player, InteractionHand.OFF_HAND, "offhand_magic_selection");
        }

        return Optional.empty();
    }

    private static Optional<ResolvedRightClickSpell> resolveMainHandSpell(Player player, ItemStack mainHandStack) {
        // 先頭 spell 固定で発動するアイテムは選択 spell より先に解決する。
        if (mainHandStack.getItem() instanceof AbstractSpellGunItem) {
            return resolveContainerSpell(mainHandStack, player, InteractionHand.MAIN_HAND, "spell_gun");
        }
        if (mainHandStack.getItem() instanceof Scroll) {
            return resolveContainerSpell(mainHandStack, player, InteractionHand.MAIN_HAND, "scroll");
        }
        if (mainHandStack.getItem() instanceof ScrollcasterGauntlet) {
            return createResolvedSpell(
                    ScrollcasterGauntlet.getSelectedSpellData(mainHandStack),
                    player,
                    InteractionHand.MAIN_HAND,
                    "scrollcaster_gauntlet_selected"
            );
        }
        if (mainHandStack.getItem() instanceof StorageStabilizer) {
            return createResolvedSpell(
                    StorageStabilizer.getSelectedSpellData(mainHandStack),
                    player,
                    InteractionHand.MAIN_HAND,
                    "storage_stabilizer_selected"
            );
        }
        // 独自右クリック武器は CastingItem 継承ではないが、右クリック時は選択 spell を使う。
        if (mainHandStack.getItem() instanceof AbstractRightClickMagicWeaponItem) {
            return resolveSelectionSpell(player, InteractionHand.MAIN_HAND, "right_click_magic_weapon_selection");
        }
        if (mainHandStack.getItem() instanceof CastingItem || RightClickSpellItemHelper.isRightClickSpellItem(mainHandStack)) {
            return resolveSelectionSpell(player, InteractionHand.MAIN_HAND, "casting_item_selection");
        }

        return Optional.empty();
    }

    private static Optional<ResolvedRightClickSpell> resolveOffhandUseItemSpell(Player player, ItemStack offHandStack) {
        if (offHandStack.getItem() instanceof AbstractSpellGunItem) {
            return resolveContainerSpell(offHandStack, player, InteractionHand.OFF_HAND, "spell_gun");
        }
        if (offHandStack.getItem() instanceof Scroll) {
            return resolveContainerSpell(offHandStack, player, InteractionHand.OFF_HAND, "scroll");
        }
        if (offHandStack.getItem() instanceof ScrollcasterGauntlet) {
            return createResolvedSpell(
                    ScrollcasterGauntlet.getSelectedSpellData(offHandStack),
                    player,
                    InteractionHand.OFF_HAND,
                    "scrollcaster_gauntlet_selected"
            );
        }
        if (offHandStack.getItem() instanceof StorageStabilizer) {
            return createResolvedSpell(
                    StorageStabilizer.getSelectedSpellData(offHandStack),
                    player,
                    InteractionHand.OFF_HAND,
                    "storage_stabilizer_selected"
            );
        }
        if (offHandStack.getItem() instanceof CastingItem || RightClickSpellItemHelper.isRightClickSpellItem(offHandStack)) {
            return resolveSelectionSpell(player, InteractionHand.OFF_HAND, "casting_item_selection");
        }

        return Optional.empty();
    }

    private static boolean shouldDeferMainHandToPriorityOffhandUse(ItemStack mainHandStack, ItemStack offHandStack) {
        if (!RightClickSpellItemHelper.isPriorityOffhandUseItem(offHandStack)) {
            return false;
        }

        return mainHandStack.getItem() instanceof PriorityOffhandUseDeferringItem;
    }

    private static Optional<ResolvedRightClickSpell> resolveSelectionSpell(Player player, InteractionHand hand, String resolutionPath) {
        var selectionOption = new SpellSelectionManager(player).getSelection();
        if (selectionOption == null) {
            return Optional.empty();
        }

        return createResolvedSpell(selectionOption.spellData, player, hand, resolutionPath);
    }

    private static Optional<ResolvedRightClickSpell> resolveContainerSpell(ItemStack stack, Player player, InteractionHand hand,
                                                                           String resolutionPath) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            return Optional.empty();
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return Optional.empty();
        }

        return createResolvedSpell(spellContainer.getSpellAtIndex(0), player, hand, resolutionPath);
    }

    private static Optional<ResolvedRightClickSpell> createResolvedSpell(@Nullable SpellData spellData, Player player, InteractionHand hand,
                                                                         String resolutionPath) {
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
                hand,
                resolutionPath
        ));
    }

    public record ResolvedRightClickSpell(
            SpellData spellData,
            ResourceLocation spellResource,
            int spellLevel,
            InteractionHand hand,
            String resolutionPath
    ) {
    }
}
