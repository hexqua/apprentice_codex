package jp.aquafactory.apprenticecodex.item.magicitem;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.enchantment.PlunderTarget;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class WoodenWand extends Item implements UniqueItem, WisdomPolicy, PlunderTarget {
    public static final int MAX_DAMAGE = 59;
    private static final int ENCHANTMENT_VALUE = 15;
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE = new ItemStack(Items.ELYTRA);

    public WoodenWand() {
        super(new Item.Properties().durability(MAX_DAMAGE));
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        var spellData = getImbuedSpell(stack);
        if (spellData == null) {
            return super.getName(stack);
        }

        return Component.translatable(
                getDescriptionId() + ".imbue_spell",
                spellData.getSpell().getDisplayName(null)
        );
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level,
            @NotNull Player player,
            @NotNull InteractionHand usedHand
    ) {
        var stack = player.getItemInHand(usedHand);
        var spellData = getImbuedSpell(stack);
        if (spellData == null) {
            return InteractionResultHolder.pass(stack);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }

        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var castingSlot = usedHand == InteractionHand.MAIN_HAND
                ? SpellSelectionManager.MAINHAND
                : SpellSelectionManager.OFFHAND;
        return spell.attemptInitiateCast(
                stack,
                spellLevel,
                level,
                player,
                CastSource.SWORD,
                true,
                castingSlot
        )
                ? InteractionResultHolder.consume(stack)
                : InteractionResultHolder.fail(stack);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return 7200;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void releaseUsing(
            @NotNull ItemStack stack,
            @NotNull Level level,
            @NotNull LivingEntity entity,
            int timeLeft
    ) {
        entity.stopUsingItem();
        Utils.releaseUsingHelper(entity, stack, timeLeft);
        super.releaseUsing(stack, level, entity, timeLeft);
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(@NotNull ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return DURABILITY_ENCHANTMENT_PROBE.supportsEnchantment(enchantment)
                || enchantment.is(jp.aquafactory.apprenticecodex.enchantment.Enchantments.WISDOM)
                || enchantment.is(jp.aquafactory.apprenticecodex.enchantment.Enchantments.PLUNDER);
    }

    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(book);
        return enchantments.isEmpty()
                || enchantments.keySet().stream().allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
        return repair.is(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            Item.@NotNull TooltipContext context,
            @NotNull List<Component> lines,
            @NotNull TooltipFlag flag
    ) {
        lines.add(Component.translatable(
                getDescriptionId() + ".desc",
                Component.keybind("key.use")
        ).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, lines, flag);
    }

    public static @Nullable SpellData getImbuedSpell(ItemStack stack) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            return null;
        }

        var container = ISpellContainer.get(stack);
        if (container == null || container.isEmpty()) {
            return null;
        }

        var spellData = container.getSpellAtIndex(0);
        if (spellData == SpellData.EMPTY
                || spellData.getSpell() == null
                || spellData.getSpell() == SpellRegistry.none()) {
            return null;
        }
        return spellData;
    }
}
