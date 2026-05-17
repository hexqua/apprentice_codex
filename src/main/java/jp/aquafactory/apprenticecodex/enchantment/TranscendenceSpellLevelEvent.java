package jp.aquafactory.apprenticecodex.enchantment;

import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.OffhandMagicCompatibleItem;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeItem;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class TranscendenceSpellLevelEvent {
    private TranscendenceSpellLevelEvent() {
    }

    @SubscribeEvent
    public static void onModifySpellLevel(ModifySpellLevelEvent event) {
        var caster = event.getEntity();
        if (caster == null || !EnchantmentRegistry.TRANSCENDENCE.isPresent()) {
            return;
        }

        var addedLevels = getApplicableTranscendenceLevels(caster.getMainHandItem(), event.getSpell(), false)
                + getApplicableTranscendenceLevels(caster.getOffhandItem(), event.getSpell(), true)
                + getApplicableCurioTranscendenceLevels(caster, event.getSpell())
                + getApplicableArmorTranscendenceLevels(caster.getItemBySlot(EquipmentSlot.CHEST), event.getSpell());
        if (addedLevels <= 0) {
            return;
        }

        event.addLevels(addedLevels);
    }

    private static int getApplicableTranscendenceLevels(ItemStack stack, AbstractSpell spell, boolean isOffhandSlot) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        var item = stack.getItem();
        // 魔法補助具は従来通りオフハンド限定、spell gun は両手、
        // 右クリック武器系はメイン限定にする。
        var isSupportedSlot =
                (isOffhandSlot && item instanceof AbstractOffhandMagicItem)
                        || item instanceof AbstractSpellGunItem
                        || item instanceof AlchemistsFlask
                        || (!isOffhandSlot && (item instanceof AbstractRightClickMagicWeaponItem
                                || item instanceof ScrollcasterGauntlet));
        if (!isSupportedSlot) {
            return 0;
        }

        var transcendenceLevel = stack.getEnchantmentLevel(EnchantmentRegistry.TRANSCENDENCE.get());
        if (transcendenceLevel <= 0 || !ISpellContainer.isSpellContainer(stack)) {
            return 0;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return 0;
        }

        var imbuedSpell = spellContainer.getSpellAtIndex(0);
        if (imbuedSpell == SpellData.EMPTY || !imbuedSpell.getSpell().equals(spell)) {
            return 0;
        }

        return transcendenceLevel;
    }

    private static int getApplicableCurioTranscendenceLevels(LivingEntity caster, AbstractSpell spell) {
        return CuriosApi.getCuriosInventory(caster)
                .map(inventory -> inventory.findCurios(stack -> stack.getItem() instanceof OffhandMagicCompatibleItem))
                .orElse(List.of())
                .stream()
                .filter(slotResult -> CuriosSlotConstants.HEAD.equals(slotResult.slotContext().identifier()))
                .mapToInt(slotResult -> getApplicableSpellContainerTranscendenceLevels(slotResult.stack(), spell, item -> true))
                .sum();
    }

    private static int getApplicableArmorTranscendenceLevels(ItemStack stack, AbstractSpell spell) {
        return getApplicableSpellContainerTranscendenceLevels(
                stack,
                spell,
                item -> item instanceof EnchantressRobeItem robeItem && robeItem.hasImbueSlot()
        );
    }

    private static int getApplicableSpellContainerTranscendenceLevels(
            ItemStack stack,
            AbstractSpell spell,
            Predicate<Item> itemPredicate
    ) {
        if (stack == null || stack.isEmpty() || !itemPredicate.test(stack.getItem())) {
            return 0;
        }

        var transcendenceLevel = stack.getEnchantmentLevel(EnchantmentRegistry.TRANSCENDENCE.get());
        if (transcendenceLevel <= 0 || !ISpellContainer.isSpellContainer(stack)) {
            return 0;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return 0;
        }

        var imbuedSpell = spellContainer.getSpellAtIndex(0);
        if (imbuedSpell == SpellData.EMPTY || !imbuedSpell.getSpell().equals(spell)) {
            return 0;
        }

        return transcendenceLevel;
    }
}
