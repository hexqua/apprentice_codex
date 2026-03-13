package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.item.CastingImplementData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.CastingItem;
import io.redspace.ironsspellbooks.item.Scroll;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class OffhandMagicItemCastEvent {
    private OffhandMagicItemCastEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        var player = event.getEntity();
        var mainhandStack = player.getMainHandItem();
        if (!(player.getOffhandItem().getItem() instanceof AbstractOffhandMagicItem)) {
            return;
        }

        // メインハンド側に操作優先条件がある場合はオフハンド魔法を割り込ませない。
        if (hasMainHandRightClickBehavior(player, mainhandStack)) {
            return;
        }

        var castResult = tryCastOffhandSpell(player);
        if (castResult == CastResult.NONE) {
            return;
        }

        event.setCancellationResult(castResult == CastResult.SUCCESS ? InteractionResult.CONSUME : InteractionResult.FAIL);
        event.setCanceled(true);
    }

    private static CastResult tryCastOffhandSpell(Player player) {
        var offhandStack = player.getOffhandItem();
        if (!(offhandStack.getItem() instanceof AbstractOffhandMagicItem offhandMagicItem)) {
            return CastResult.NONE;
        }

        if (!ISpellContainer.isSpellContainer(offhandStack)) {
            offhandMagicItem.initializeSpellContainer(offhandStack);
        }

        var spellSelectionManager = new SpellSelectionManager(player);
        var selectionOption = spellSelectionManager.getSelection();
        if (selectionOption == null || selectionOption.spellData == SpellData.EMPTY) {
            return CastResult.NONE;
        }

        var spellData = selectionOption.spellData;
        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var casted = spell.attemptInitiateCast(
                offhandStack,
                spellLevel,
                player.level(),
                player,
                selectionOption.getCastSource(),
                true,
                SpellSelectionManager.OFFHAND
        );

        return casted ? CastResult.SUCCESS : CastResult.FAIL;
    }

    private static boolean hasMainHandRightClickBehavior(Player player, ItemStack stack) {
        // 素手は処理の割り込みがうまくいかないため、オフハンドキャスト対象外にする.
        if (stack.isEmpty()) {
            return true;
        }

        var item = stack.getItem();
        // クールダウン中でもメインハンド側の操作を優先し、オフハンド魔法は割り込ませない。
        if (player.getCooldowns().isOnCooldown(item)) {
            return true;
        }

        if (isRightClickSpellItem(stack)) {
            return true;
        }

        if (stack.isEdible() || item.getUseDuration(stack) > 0) {
            return true;
        }

        // ブロック設置のような `useOn` 系アイテムは、視線先に対象がなくても
        // メインハンド操作を優先してオフハンド魔法の誤発動を防ぐ。
        return hasUseOverride(item) || hasUseOnOverride(item);
    }

    private static boolean isRightClickSpellItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.getItem() instanceof CastingItem || stack.getItem() instanceof Scroll) {
            return true;
        }

        return CastingImplementData.has(stack) && CastingImplementData.get(stack);
    }

    private static boolean hasUseOverride(Item item) {
        return ITEM_USE_OVERRIDE_CACHE.get(item.getClass());
    }

    private static boolean hasUseOnOverride(Item item) {
        return ITEM_USE_ON_OVERRIDE_CACHE.get(item.getClass());
    }

    private static final ClassValue<Boolean> ITEM_USE_OVERRIDE_CACHE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> itemClass) {
            try {
                var useMethod = itemClass.getMethod("use", Level.class, Player.class, InteractionHand.class);
                return useMethod.getDeclaringClass() != Item.class;
            } catch (NoSuchMethodException ignored) {
                return false;
            }
        }
    };

    private static final ClassValue<Boolean> ITEM_USE_ON_OVERRIDE_CACHE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> itemClass) {
            try {
                var useOnMethod = itemClass.getMethod("useOn", UseOnContext.class);
                return useOnMethod.getDeclaringClass() != Item.class;
            } catch (NoSuchMethodException ignored) {
                return false;
            }
        }
    };

    private enum CastResult {
        NONE,
        SUCCESS,
        FAIL
    }
}
