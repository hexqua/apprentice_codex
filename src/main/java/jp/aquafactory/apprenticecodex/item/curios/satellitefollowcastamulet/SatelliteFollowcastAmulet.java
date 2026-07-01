package jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet;

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
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

public class SatelliteFollowcastAmulet extends Item implements ICurioItem, IJeiInfoItem, RestrictedSpellImbuableItem,
        SpellSlotUpgradeableItem {
    public static final int MIN_SPELL_SLOTS = 1;
    public static final int MAX_SPELL_SLOTS = 2;
    public static final ResourceLocation LESSER_SPELL_SLOT_UPGRADE_ID =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "lesser_spell_slot_upgrade");
    public static final double CRYSTAL_ORBIT_RADIUS = 1.35D;
    public static final double CRYSTAL_ORBIT_HEIGHT = 1.05D;
    public static final double CRYSTAL_ORBIT_SPEED = Math.PI / 60.0D;
    public static final double CRYSTAL_FLOAT_SPEED = Math.PI / 24.0D;
    public static final double CRYSTAL_FLOAT_RANGE = 0.12D;

    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.satellite_followcast_amulet.desc_";
    private static final String NEXT_SEARCH_INDEX_TAG = ApprenticeCodex.MODID + ":satellite_followcast_next_search_index";

    private final String slotIdentifier;

    public SatelliteFollowcastAmulet() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
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
        tooltips.add(Component.empty());
        return tooltips;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        appendFollowcastTooltip(lines);
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
        return RemoteOwnerCastRules.checkImbue(spell, spellLevel, RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST)
                .isAllowed();
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
        return collectFollowcastRestrictTooltipSection();
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

    public static boolean isSupportedSpellSlotUpgrade(SpellSlotUpgradeItem upgradeItem) {
        var itemId = ForgeRegistries.ITEMS.getKey(upgradeItem);
        return LESSER_SPELL_SLOT_UPGRADE_ID.equals(itemId);
    }

    public static SpellData getSpellAtIndex(ItemStack stack, int slotIndex) {
        if (stack == null || stack.isEmpty() || slotIndex < 0 || !ISpellContainer.isSpellContainer(stack)) {
            return SpellData.EMPTY;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || slotIndex >= spellContainer.getMaxSpellCount()) {
            return SpellData.EMPTY;
        }

        return spellContainer.getSpellAtIndex(slotIndex);
    }

    public static int getMaxSpellSlots(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !ISpellContainer.isSpellContainer(stack)) {
            return MIN_SPELL_SLOTS;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null) {
            return MIN_SPELL_SLOTS;
        }

        return clampSpellSlotCount(spellContainer.getMaxSpellCount());
    }

    public static int advanceAndGetSearchStartIndex(ItemStack stack, int maxSpellSlots) {
        var clampedSlots = clampSpellSlotCount(maxSpellSlots);
        var tag = stack.getOrCreateTag();
        var current = tag.contains(NEXT_SEARCH_INDEX_TAG) ? tag.getInt(NEXT_SEARCH_INDEX_TAG) : -1;
        var next = Math.floorMod(current + 1, clampedSlots);
        tag.putInt(NEXT_SEARCH_INDEX_TAG, next);
        return next;
    }

    public static Vec3 getCrystalOffset(LivingEntity owner, int slotIndex, int maxSpellSlots, float partialTick) {
        var clampedSlots = Math.max(1, clampSpellSlotCount(maxSpellSlots));
        var time = owner.tickCount + partialTick;
        var angle = (Math.PI * 2.0D * slotIndex / clampedSlots) + time * CRYSTAL_ORBIT_SPEED;
        var floatOffset = Math.sin(time * CRYSTAL_FLOAT_SPEED + slotIndex * 0.7D) * CRYSTAL_FLOAT_RANGE;
        var height = owner.getBbHeight() * 0.6D + CRYSTAL_ORBIT_HEIGHT + floatOffset;
        return new Vec3(Math.cos(angle) * CRYSTAL_ORBIT_RADIUS, height, Math.sin(angle) * CRYSTAL_ORBIT_RADIUS);
    }

    public static Vec3 getCrystalPosition(LivingEntity owner, int slotIndex, int maxSpellSlots, float partialTick) {
        return owner.position().add(getCrystalOffset(owner, slotIndex, maxSpellSlots, partialTick));
    }

    private static void appendFollowcastTooltip(List<Component> lines) {
        ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
        if (ImbueTooltipHelper.appendHintIfDetailsHidden(lines)) {
            return;
        }

        ImbueTooltipHelper.appendTooltipSection(
                lines,
                collectFollowcastAbilityTooltipSection(),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_none"
        );
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                collectFollowcastRestrictTooltipSection(),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_none"
        );
    }

    private static List<Component> collectFollowcastAbilityTooltipSection() {
        var translatedLines = new ArrayList<Component>();
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_long_to_instant"
        ));
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_extend_cooldown"
        ));
        return translatedLines;
    }

    private static List<Component> collectFollowcastRestrictTooltipSection() {
        var translatedLines = new ArrayList<Component>();
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_no_recast"
        ));
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_by_profile"
        ));
        return translatedLines;
    }

}
