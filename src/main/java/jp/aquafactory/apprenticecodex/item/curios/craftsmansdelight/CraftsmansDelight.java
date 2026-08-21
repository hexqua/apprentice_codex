package jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.EquipmentSpellTimingConfigState;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

public class CraftsmansDelight extends Item implements ICurioItem, IJeiInfoItem {
    private static final float BREAK_SPEED_BONUS_MULTIPLIER = 2.0f;
    private static final float PROCESS_SPEED_BONUS_MULTIPLIER = 1.5f;
    private static final float MANA_COST_DISCOUNT_MULTIPLIER = 0.5f;
    private static final int TOUCH_DIG_RANGE_BLOCKS = 8;
    private static final int TOUCH_DIG_RANGE_WITH_BONUS_BLOCKS = 16;
    private static final int CASTING_MOBILITY_EFFECT_REFRESH_TICKS = 5;

    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.craftsmans_delight.desc_";
    private static final String SPELL_HINT_KEY = "item.apprenticecodex.common.desc.spell_hint";
    private static final String SPELL_HINT_OPEN_KEY = "item.apprenticecodex.common.desc.spell_hint_open";
    private static final List<DeferredHolder<AbstractSpell, AbstractSpell>> TARGET_SPELLS = List.of(
            SpellRegistry.TINY_LUMBERJACK,
            SpellRegistry.WORLD_FLATTER,
            SpellRegistry.THERMAL_PROCESS,
            SpellRegistry.GRIND_RUNNER,
            SpellRegistry.GRACED_RAIN,
            SpellRegistry.HARVEST_MOON,
            SpellRegistry.EARTH_FORGE,
            SpellRegistry.HEAVENLY_FIST,
            SpellRegistry.MANA_MENDING,
            SpellRegistry.LINEAR_BUILD
    );
    private final String slotIdentifier;

    public CraftsmansDelight() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        slotIdentifier = Curios.RING_SLOT;
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, Item.TooltipContext context, ItemStack stack) {
        var result = new ArrayList<>(tooltips);
        if (slotIdentifier != null) {
            result.add(Component.empty());
            result.add(Component.translatable("curios.modifiers." + slotIdentifier).withStyle(ChatFormatting.GOLD));
            result.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            result.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_enchant"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            appendTargetSpellHintOrTooltips(result);
        }

        return result;
    }

    private static void appendTargetSpellHintOrTooltips(List<Component> tooltips) {
        if (!ImbueTooltipHelper.hasDetailsKeyDown()) {
            tooltips.add(Component.translatable(SPELL_HINT_KEY).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltips.add(Component.translatable(SPELL_HINT_OPEN_KEY).withStyle(ChatFormatting.GRAY));
        appendTargetSpellTooltips(tooltips);
    }

    private static void appendTargetSpellTooltips(List<Component> tooltips) {
        for (var spellEntry : TARGET_SPELLS) {
            var spell = spellEntry.get();
            if (!spell.isEnabled()) {
                continue;
            }
            appendTargetSpellTooltip(tooltips, spell);
        }
        for (var spell : CraftsmansDelightSpellSupport.getExternalTargetSpells()) {
            if (!spell.isEnabled()) {
                continue;
            }
            appendTargetSpellTooltip(tooltips, spell);
        }
    }

    private static void appendTargetSpellTooltip(List<Component> tooltips, AbstractSpell spell) {
        tooltips.add(Component.literal("- ")
                    .append(spell.getDisplayName(null))
                    .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    public static boolean isEquippedBy(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        return !getEquippedStack(entity).isEmpty();
    }

    public static float applyBreakSpeedBonus(float breakSpeed, @Nullable LivingEntity entity) {
        if (!isEquippedBy(entity)) {
            return breakSpeed;
        }

        return breakSpeed * BREAK_SPEED_BONUS_MULTIPLIER;
    }

    public static float applyProcessSpeedBonus(float processSpeed, @Nullable LivingEntity entity) {
        if (!isEquippedBy(entity)) {
            return processSpeed;
        }

        return processSpeed * PROCESS_SPEED_BONUS_MULTIPLIER;
    }

    public static int applyManaCostDiscount(int manaCost, @Nullable LivingEntity entity) {
        if (manaCost <= 0 || !isEquippedBy(entity)) {
            return manaCost;
        }

        return Math.max(1, Math.round(manaCost * MANA_COST_DISCOUNT_MULTIPLIER));
    }

    public static int applyCooldownDiscount(int baseCooldown, @Nullable LivingEntity entity) {
        if (baseCooldown <= 0 || !isEquippedBy(entity)) {
            return baseCooldown;
        }

        // 実行中の SERVER config リロード後も予測結果を一致させるため、クライアントでは同期値を使う。
        var cooldownMultiplier = entity.level().isClientSide
                ? EquipmentSpellTimingConfigState.craftsmansDelightCooldownMultiplier()
                : ApprenticeCodexServerConfig.craftsmansDelightCooldownMultiplier();
        return WeaponImbueCooldownHelper.applyLimitedCooldownMultiplier(
                baseCooldown,
                cooldownMultiplier
        );
    }

    public static int getTouchDigRangeBlocks(@Nullable LivingEntity entity) {
        return isEquippedBy(entity) ? TOUCH_DIG_RANGE_WITH_BONUS_BLOCKS : TOUCH_DIG_RANGE_BLOCKS;
    }

    public static double getTouchDigRange(@Nullable LivingEntity entity) {
        return getTouchDigRangeBlocks(entity);
    }

    public static int getReducedEffectiveCooldown(AbstractSpell spell, @Nullable LivingEntity entity, CastSource castSource) {
        if (!(entity instanceof Player player)) {
            return spell.getSpellCooldown();
        }

        return WeaponImbueCooldownHelper.getEffectiveSpellCooldown(spell, player, castSource);
    }

    public static void applyCastingMobility(@Nullable LivingEntity entity) {
        if (entity == null || entity.level().isClientSide || !isEquippedBy(entity)) {
            return;
        }

        // 防御魔法とは切り離し、CraftsmansDelight 専用の継続詠唱補助として扱う.
        entity.addEffect(new MobEffectInstance(
                EffectRegistry.CRAFTSMANS_DELIGHT_MOBILITY,
                CASTING_MOBILITY_EFFECT_REFRESH_TICKS,
                0,
                false,
                false,
                true
        ));
    }

    public static ItemStack applyMainHandEnchantmentsToTool(ItemStack baseTool, @Nullable LivingEntity entity) {
        if (entity == null || !isEquippedBy(entity)) {
            return baseTool;
        }

        // 外部 MOD の採掘エンチャントも扱えるよう、破壊時点のメインハンドから全件転写する。
        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(entity.getMainHandItem());
        if (!enchantments.isEmpty()) {
            EnchantmentHelper.setEnchantments(baseTool, enchantments);
        }

        return baseTool;
    }

    public static ItemStack createSpectralHammerTool(@Nullable LivingEntity entity) {
        if (entity == null || !isEquippedBy(entity)) {
            return ItemStack.EMPTY;
        }

        return entity.getMainHandItem().copy();
    }

    public static ItemStack createHeavenlyFistCrystalHarvestTool(@Nullable LivingEntity entity) {
        return applyMainHandEnchantmentsToTool(new ItemStack(Items.DIAMOND_PICKAXE), entity);
    }

    private static ItemStack getEquippedStack(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> inventory.getStacksHandler(Curios.RING_SLOT)
                        .map(ringHandler -> {
                            var stacks = ringHandler.getStacks();
                            for (var slot = 0; slot < stacks.getSlots(); slot++) {
                                var stack = stacks.getStackInSlot(slot);
                                if (stack.is(ItemRegistry.CRAFTSMANS_DELIGHT.get())) {
                                    return stack.copy();
                                }
                            }
                            return inventory.findFirstCurio(ItemRegistry.CRAFTSMANS_DELIGHT.get())
                                    .map(slotResult -> slotResult.stack().copy())
                                    .orElse(ItemStack.EMPTY);
                        })
                        .orElseGet(() -> inventory.findFirstCurio(ItemRegistry.CRAFTSMANS_DELIGHT.get())
                                .map(slotResult -> slotResult.stack().copy())
                                .orElse(ItemStack.EMPTY)))
                .orElse(ItemStack.EMPTY);
    }

}
