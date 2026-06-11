package jp.aquafactory.apprenticecodex.item.curios.autocastamulet;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.item.SpellSlotUpgradeItem;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.SpellSlotUpgradeableItem;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownPolicyItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.ArrayList;

public class AutocastAmulet extends Item implements ICurioItem, IJeiInfoItem, RestrictedSpellImbuableItem,
        SpellSlotUpgradeableItem, WeaponImbueCooldownPolicyItem {
    public static final int MIN_SPELL_SLOTS = 1;
    public static final int MAX_SPELL_SLOTS = 3;
    public static final ResourceLocation LESSER_SPELL_SLOT_UPGRADE_ID =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "lesser_spell_slot_upgrade");

    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.autocast_amulet.desc_";
    private static final String RETRY_SEQUENCE_TICK_TAG = ApprenticeCodex.MODID + ":autocast_retry_sequence_tick";
    private static final String RETRY_SKIP_SLOT_TAG = ApprenticeCodex.MODID + ":autocast_retry_skip_slot";
    public static final int ERROR_RETRY_DELAY_TICKS = 60;

    private final String slotIdentifier;

    public AutocastAmulet() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        this.slotIdentifier = Curios.NECKLACE_SLOT;
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
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide) {
            initializeSpellContainer(stack);
        }
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, ItemStack stack) {
        tooltips.add(Component.empty());
        tooltips.add(Component.translatable("curios.modifiers." + slotIdentifier).withStyle(ChatFormatting.GOLD));
        tooltips.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_1"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        tooltips.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_2"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        return tooltips;
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public boolean canImbueSpell(SpellData spellData) {
        return spellData != SpellData.EMPTY && canImbueSpell(spellData.getSpell(), spellData.getLevel());
    }

    @Override
    public boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel) {
        return spell != null
                && spell != io.redspace.ironsspellbooks.api.registry.SpellRegistry.none()
                && AutocastAmuletSpellListManager.isAllowlisted(spell);
    }

    @Override
    public void normalizeImbuedSpellContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        initializeSpellContainer(stack);
        var current = ISpellContainer.get(stack);
        if (current == null) {
            return;
        }

        var maxSpellCount = clampSpellSlotCount(current.getMaxSpellCount());
        var normalized = ISpellContainer.create(maxSpellCount, false, false).mutableCopy();
        for (var index = 0; index < current.getMaxSpellCount() && normalized.getActiveSpellCount() < maxSpellCount; ++index) {
            var spellData = current.getSpellAtIndex(index);
            if (spellData == SpellData.EMPTY || !canImbueSpell(spellData)) {
                continue;
            }

            normalized.addSpellAtIndex(
                    spellData.getSpell(),
                    spellData.getLevel(),
                    normalized.getActiveSpellCount(),
                    false
            );
        }

        ISpellContainer.set(stack, normalized.toImmutable());
    }

    @Override
    public ItemStack createArcaneAnvilImbueResult(ItemStack baseStack, SpellData spellData) {
        var resultStack = baseStack.copy();
        initializeSpellContainer(resultStack);
        var current = ISpellContainer.get(resultStack);
        if (current == null) {
            return ItemStack.EMPTY;
        }

        var mutable = current.mutableCopy();
        var targetIndex = mutable.getNextAvailableIndex();
        if (targetIndex < 0) {
            targetIndex = 0;
        }
        mutable.removeSpellAtIndex(targetIndex);
        mutable.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), targetIndex, false);
        ISpellContainer.set(resultStack, mutable.toImmutable());
        normalizeImbuedSpellContainer(resultStack);
        return resultStack;
    }

    @Override
    public int getWorkbenchSpellExtractionIndex(ItemStack stack, ISpellContainer spellContainer) {
        for (var index = spellContainer.getMaxSpellCount() - 1; index >= 0; --index) {
            if (spellContainer.getSpellAtIndex(index) != SpellData.EMPTY) {
                return index;
            }
        }
        return 0;
    }

    @Override
    public boolean canRemoveWorkbenchSpell(ItemStack stack, ISpellContainer spellContainer, int spellIndex, SpellData spellData) {
        return spellData != SpellData.EMPTY;
    }

    @Override
    public List<Component> getImbueRestrictionTooltipLines() {
        return List.of(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_by_allowlist"
        ));
    }

    @Override
    public ItemStack createSpellSlotUpgradeResult(ItemStack baseStack, SpellSlotUpgradeItem upgradeItem) {
        if (!isSupportedSpellSlotUpgrade(upgradeItem)) {
            return ItemStack.EMPTY;
        }

        var resultStack = baseStack.copy();
        initializeSpellContainer(resultStack);
        var current = ISpellContainer.get(resultStack);
        if (current == null) {
            return ItemStack.EMPTY;
        }
        if (current.getMaxSpellCount() >= MAX_SPELL_SLOTS) {
            return ItemStack.EMPTY;
        }

        var mutable = current.mutableCopy();
        mutable.setMaxSpellCount(Math.min(MAX_SPELL_SLOTS, current.getMaxSpellCount() + 1));
        ISpellContainer.set(resultStack, mutable.toImmutable());
        normalizeImbuedSpellContainer(resultStack);
        return resultStack;
    }

    public void initializeSpellContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (ISpellContainer.isSpellContainer(stack)) {
            return;
        }

        ISpellContainer.set(stack, ISpellContainer.create(MIN_SPELL_SLOTS, false, false));
    }

    public static int clampSpellSlotCount(int spellSlotCount) {
        return Math.max(MIN_SPELL_SLOTS, Math.min(MAX_SPELL_SLOTS, spellSlotCount));
    }

    public static double getManaMultiplier(int activeSpellCount) {
        if (activeSpellCount <= 1) {
            return 1.0D;
        }

        var base = 1.0D + (activeSpellCount - 1) * 0.2D;
        return base * base;
    }

    public static int getScaledManaCost(AbstractSpell spell, int spellLevel, int activeSpellCount) {
        var baseManaCost = spell.getManaCost(spellLevel);
        if (baseManaCost <= 0) {
            return 0;
        }

        return Math.max(1, (int) Math.round(baseManaCost * getManaMultiplier(activeSpellCount)));
    }

    public static void scheduleRetrySequence(ItemStack stack, long currentTick, int spellIndex) {
        var tag = stack.getOrCreateTag();
        tag.putLong(RETRY_SEQUENCE_TICK_TAG, currentTick + ERROR_RETRY_DELAY_TICKS);
        tag.putInt(RETRY_SKIP_SLOT_TAG, Math.max(0, spellIndex));
    }

    public static boolean isRetrySequenceCoolingDown(ItemStack stack, long currentTick) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(RETRY_SEQUENCE_TICK_TAG)) {
            return false;
        }
        return tag.getLong(RETRY_SEQUENCE_TICK_TAG) > currentTick;
    }

    public static int consumeReadyRetrySkipSlot(ItemStack stack, long currentTick) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(RETRY_SEQUENCE_TICK_TAG)) {
            return -1;
        }

        if (tag.getLong(RETRY_SEQUENCE_TICK_TAG) > currentTick) {
            return -1;
        }

        var skipSlot = tag.contains(RETRY_SKIP_SLOT_TAG) ? tag.getInt(RETRY_SKIP_SLOT_TAG) : -1;
        tag.remove(RETRY_SEQUENCE_TICK_TAG);
        tag.remove(RETRY_SKIP_SLOT_TAG);
        cleanupAutocastTags(stack, tag);
        return skipSlot;
    }

    public static long getRetrySequenceTick(ItemStack stack) {
        var tag = stack.getTag();
        return tag == null || !tag.contains(RETRY_SEQUENCE_TICK_TAG) ? -1L : tag.getLong(RETRY_SEQUENCE_TICK_TAG);
    }

    public static int getRetrySkipSlot(ItemStack stack) {
        var tag = stack.getTag();
        return tag == null || !tag.contains(RETRY_SKIP_SLOT_TAG) ? -1 : tag.getInt(RETRY_SKIP_SLOT_TAG);
    }

    public static boolean isSupportedSpellSlotUpgrade(SpellSlotUpgradeItem upgradeItem) {
        var itemId = ForgeRegistries.ITEMS.getKey(upgradeItem);
        return LESSER_SPELL_SLOT_UPGRADE_ID.equals(itemId);
    }

    public static Component createInsufficientManaMessage(AbstractSpell spell, Player player, int requiredMana) {
        return Component.translatable(
                        "ui.apprenticecodex.autocast_amulet.insufficient_mana",
                        spell.getDisplayName(player),
                        requiredMana
                )
                .withStyle(ChatFormatting.RED);
    }

    public static List<SpellData> getImbuedSpells(ItemStack stack) {
        var spells = new ArrayList<SpellData>();
        if (!ISpellContainer.isSpellContainer(stack)) {
            return spells;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return spells;
        }

        for (var index = 0; index < spellContainer.getMaxSpellCount(); ++index) {
            var spellData = spellContainer.getSpellAtIndex(index);
            if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
                continue;
            }

            spells.add(spellData);
        }
        return spells;
    }

    @Override
    public boolean ignoresWeaponImbueCooldownMultiplier(ItemStack stack, @Nullable AbstractSpell spell, io.redspace.ironsspellbooks.api.spells.CastSource castSource) {
        // Autocast Amulet は剣 Imbue 扱いの接着だけ借りるが、実時間 cooldown は武器 Imbue の短縮調整へ寄せない。
        return castSource == io.redspace.ironsspellbooks.api.spells.CastSource.SWORD;
    }

    private static void cleanupAutocastTags(ItemStack stack, CompoundTag tag) {
        if (!tag.isEmpty()) {
            return;
        }
        stack.setTag(null);
    }
}
