package jp.aquafactory.apprenticecodex.item.flask;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.UniqueItem;
import io.redspace.ironsspellbooks.item.consumables.SimpleElixir;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AlchemistsFlask extends AbstractPotionFlaskItem
        implements IPresetSpellContainer, RestrictedSpellImbuableItem, NonDamageableAnvilMergeItem, UniqueItem {
    private static final int BASE_MAX_STORED_DOSES = 16;
    private static final int LARGE_MUG_BONUS_PER_LEVEL = 4;

    public AlchemistsFlask() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        var spellContainer = ISpellContainer.create(1, false, false).mutableCopy();
        spellContainer.addSpellAtIndex(SpellRegistry.EXTRACT.get(), 1, 0, true);
        ISpellContainer.set(itemStack, spellContainer.toImmutable());
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        var stack = super.getDefaultInstance();
        initializeSpellContainer(stack);
        return stack;
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Level level, @NotNull Player player) {
        super.onCraftedBy(stack, level, player);
        initializeSpellContainer(stack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId,
                              boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide) {
            initializeSpellContainer(stack);
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        var castResult = tryCastSpell(player, stack, usedHand);
        return switch (castResult) {
            case SUCCESS -> InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            case FAIL -> InteractionResultHolder.fail(stack);
            case NONE -> InteractionResultHolder.pass(stack);
        };
    }

    @Override
    public final boolean canImbueSpell(SpellData spellData) {
        return spellData != SpellData.EMPTY && canImbueSpell(spellData.getSpell(), spellData.getLevel());
    }

    @Override
    public boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel) {
        return spell != null && spell != io.redspace.ironsspellbooks.api.registry.SpellRegistry.none();
    }

    @Override
    public void normalizeImbuedSpellContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        var spellData = getPrimarySpellData(stack);
        var normalized = ISpellContainer.create(1, false, false).mutableCopy();
        if (spellData != null && canImbueSpell(spellData)) {
            normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, true);
        }
        ISpellContainer.set(stack, normalized.toImmutable());
    }

    @Override
    protected int getBaseStoredDoseCapacity() {
        return BASE_MAX_STORED_DOSES;
    }

    @Override
    protected int getLargeMugBonusPerLevel() {
        return LARGE_MUG_BONUS_PER_LEVEL;
    }

    @Override
    protected boolean isSupportedStoredItem(ItemStack stack) {
        var item = stack.getItem();
        return isSupportedPotionItem(stack)
                || item instanceof SimpleElixir;
    }

    @Override
    protected boolean isSupportedPotionItem(ItemStack stack) {
        return stack.getItem() instanceof PotionItem
                && (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION));
    }

    @Override
    protected boolean isSupportedFlaskEnchantment(Enchantment enchantment) {
        return (EnchantmentRegistry.TRANSCENDENCE.isPresent() && enchantment == EnchantmentRegistry.TRANSCENDENCE.get())
                || super.isSupportedFlaskEnchantment(enchantment);
    }

    private @Nullable SpellData getPrimarySpellData(ItemStack stack) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            return null;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return null;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        return spellData == SpellData.EMPTY ? null : spellData;
    }

    private CastResult tryCastSpell(Player player, ItemStack stack, InteractionHand usedHand) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            initializeSpellContainer(stack);
        }

        var spellData = getPrimarySpellData(stack);
        if (spellData == null) {
            return CastResult.NONE;
        }
        if (!canImbueSpell(spellData)) {
            return CastResult.FAIL;
        }

        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var slotId = usedHand == InteractionHand.OFF_HAND
                ? SpellSelectionManager.OFFHAND
                : SpellSelectionManager.MAINHAND;
        var casted = spell.attemptInitiateCast(
                stack,
                spellLevel,
                player.level(),
                player,
                CastSource.SWORD,
                true,
                slotId
        );
        return casted ? CastResult.SUCCESS : CastResult.FAIL;
    }

    private enum CastResult {
        NONE,
        SUCCESS,
        FAIL
    }
}
