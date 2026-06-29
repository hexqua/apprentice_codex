package jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;
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
    private static final int COOLDOWN_DIVISOR = 3;
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
            SpellRegistry.MANA_MENDING
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
            if (ApprenticeCodexServerConfig.craftsmansDelightCanImbueEnchantment()) {
                result.add(Component.literal(" ")
                        .append(Component.translatable(getDescriptionId() + ".desc_enchant"))
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            }
            appendTargetSpellHintOrTooltips(result);
        }

        return result;
    }

    private static void appendTargetSpellHintOrTooltips(List<Component> tooltips) {
        if (!isShiftDown()) {
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

    private static boolean isShiftDown() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return false;
        }

        try {
            var screenClass = Class.forName("net.minecraft.client.gui.screens.Screen");
            var hasShiftDown = screenClass.getMethod("hasShiftDown");
            return Boolean.TRUE.equals(hasShiftDown.invoke(null));
        } catch (ReflectiveOperationException | LinkageError e) {
            return false;
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, usedHand);
        }

        // 設定が無効なら通常挙動に戻す。
        if (!ApprenticeCodexServerConfig.craftsmansDelightCanImbueEnchantment()) {
            return super.use(level, player, usedHand);
        }

        if (!level.isClientSide) {
            if (!consumeManaForSneakUse(player, stack)) {
                return InteractionResultHolder.fail(stack);
            }

            AudioTools.playSoundFromEntity(level, player, SoundRegistry.VANILLA_INSCRIBE_MANA.get(), SoundSource.PLAYERS);
            applySneakUseEnchantment(player, stack);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return !slotContext.entity().isShiftKeyDown();
    }

    private static void applySneakUseEnchantment(Player player, ItemStack stack) {
        var enchantmentRegistry = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var fortune = enchantmentRegistry.getOrThrow(Enchantments.FORTUNE);
        var silkTouch = enchantmentRegistry.getOrThrow(Enchantments.SILK_TOUCH);

        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        var fortuneLevel = ApprenticeCodexServerConfig.craftsmansDelightFortuneLevel();
        if (enchantments.isEmpty()) {
            stack.enchant(fortune, fortuneLevel);
            return;
        }

        var hasNonFortune = enchantments.keySet().stream()
                .anyMatch(enchantment -> !enchantment.is(Enchantments.FORTUNE));

        EnchantmentHelper.setEnchantments(stack, ItemEnchantments.EMPTY);

        if (hasNonFortune) {
            stack.enchant(fortune, fortuneLevel);
            return;
        }

        stack.enchant(silkTouch, 1);
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

        return Math.max(1, baseCooldown / COOLDOWN_DIVISOR);
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

        return WeaponImbueCooldownHelper.getEffectiveSpellCooldown(spell, player, castSource, ItemStack.EMPTY);
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

    public static ItemStack applyEnchantsToTool(ItemStack baseTool, @Nullable LivingEntity entity) {
        if (entity == null) {
            return baseTool;
        }

        var stack = getEquippedStack(entity);
        if (stack.isEmpty()) {
            return baseTool;
        }

        // 指輪に付いたエンチャントを魔法処理用ツールへ転写する。
        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (!enchantments.isEmpty()) {
            EnchantmentHelper.setEnchantments(baseTool, enchantments);
        }

        return baseTool;
    }

    public static ItemStack createTouchDigTool(@Nullable LivingEntity entity) {
        if (entity == null) {
            return ItemStack.EMPTY;
        }

        var ringStack = getEquippedStack(entity);
        var mainHandStack = entity.getMainHandItem();
        if (!mainHandStack.isEmpty()) {
            return applyMiningEnchants(mainHandStack, ringStack, entity);
        }

        if (!hasMiningEnchantments(ringStack)) {
            return ItemStack.EMPTY;
        }

        // TouchDig は素手だと downstream に空スタックが渡り、追加の採掘エンチャント効果が発火しない。
        return applyMiningEnchants(new ItemStack(Items.DIAMOND_PICKAXE), ringStack, entity);
    }

    public static ItemStack createSpectralHammerTool(@Nullable LivingEntity entity) {
        if (entity == null) {
            return ItemStack.EMPTY;
        }

        var ringStack = getEquippedStack(entity);
        if (!hasMiningEnchantments(ringStack)) {
            return ItemStack.EMPTY;
        }

        return applyMiningEnchants(new ItemStack(Items.DIAMOND_PICKAXE), ringStack, entity);
    }

    public static ItemStack createHeavenlyFistCrystalHarvestTool(@Nullable LivingEntity entity) {
        if (entity == null) {
            return new ItemStack(Items.DIAMOND_PICKAXE);
        }

        return applyMiningEnchants(new ItemStack(Items.DIAMOND_PICKAXE), getEquippedStack(entity), entity);
    }

    private static ItemStack applyMiningEnchants(ItemStack baseTool, ItemStack ringStack, @Nullable LivingEntity entity) {
        if (baseTool.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var tool = baseTool.copy();
        if (ringStack.isEmpty()) {
            return tool;
        }

        var baseFortuneLevel = getFortuneLevel(tool);
        var ringFortuneLevel = getFortuneLevel(ringStack);
        var hasSilkTouch = getSilkTouchLevel(tool) > 0 || getSilkTouchLevel(ringStack) > 0;
        if (ringFortuneLevel <= 0 && !hasSilkTouch) {
            return tool;
        }

        if (entity == null) {
            return tool;
        }

        var enchantmentRegistry = entity.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var fortune = enchantmentRegistry.getOrThrow(Enchantments.FORTUNE);
        var silkTouch = enchantmentRegistry.getOrThrow(Enchantments.SILK_TOUCH);
        var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(tool).entrySet()) {
            var enchantment = entry.getKey();
            if (enchantment == null
                    || enchantment.is(Enchantments.FORTUNE)
                    || enchantment.is(Enchantments.SILK_TOUCH)) {
                continue;
            }
            enchantments.set(enchantment, entry.getValue());
        }

        if (hasSilkTouch) {
            enchantments.set(silkTouch, 1);
        } else {
            enchantments.set(fortune, Math.max(baseFortuneLevel, ringFortuneLevel));
        }
        EnchantmentHelper.setEnchantments(tool, enchantments.toImmutable());
        return tool;
    }

    private static boolean hasMiningEnchantments(ItemStack stack) {
        return getFortuneLevel(stack) > 0 || getSilkTouchLevel(stack) > 0;
    }

    private static int getFortuneLevel(ItemStack stack) {
        return jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(stack, Enchantments.FORTUNE);
    }

    private static int getSilkTouchLevel(ItemStack stack) {
        return jp.aquafactory.apprenticecodex.enchantment.Enchantments.getLevel(stack, Enchantments.SILK_TOUCH);
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

    private static boolean consumeManaForSneakUse(Player player, ItemStack stack) {
        var requiredMana = ApprenticeCodexServerConfig.craftsmansDelightRequiredMana();
        var maxMana = (float) player.getAttributeValue(AttributeRegistry.MAX_MANA);
        if (maxMana < requiredMana) {
            sendUnsatisfiedMaxManaMessage(player, stack, requiredMana);
            return false;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || magicData.getMana() < requiredMana) {
            sendManaLackMessage(player, stack);
            return false;
        }

        // addManaは負値で消費できる。
        magicData.addMana(-requiredMana);
        return true;
    }

    private static void sendUnsatisfiedMaxManaMessage(Player player, ItemStack stack, float requiredMana) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                Component.translatable(
                        "ui.apprenticecodex.unsatisfied_max_mana_for_enchant",
                        stack.getHoverName(),
                        Math.round(requiredMana)
                ).withStyle(ChatFormatting.RED)
        ));
    }

    private static void sendManaLackMessage(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                Component.translatable("ui.irons_spellbooks.cast_error_mana", stack.getHoverName()).withStyle(ChatFormatting.RED)
        ));
    }
}
