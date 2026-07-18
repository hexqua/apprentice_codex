package jp.aquafactory.apprenticecodex.item.circuitheatstaff;

import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.item.weapons.ExtendedSwordItem;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.CooldownInstance;
import io.redspace.ironsspellbooks.item.UniqueItem;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import io.redspace.ironsspellbooks.item.weapons.StaffTier;
import io.redspace.ironsspellbooks.network.casting.CancelCastPacket;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffConfigState;
import jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffCoolingHandler;
import jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager;
import jp.aquafactory.apprenticecodex.utility.PersistentGameTimeSanitizer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.List;
import java.util.Set;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;

public class CircuitHeatStaff extends StaffItem implements GeoItem, UniqueItem, NonDamageableAnvilMergeItem,
        IJeiInfoItem, WisdomPolicy {
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.circuit_heat_staff.desc_";
    private static final String FRAME_CONTROLLER = "frame";
    private static final String COG_CONTROLLER = "cog";
    private static final String OVERHEAT_EXPIRE_GAME_TIME_TAG = "CircuitHeatStaffOverheatExpireGameTime";
    private static final String OVERHEAT_DURATION_TICKS_TAG = "CircuitHeatStaffOverheatDurationTicks";
    private static final String VANILLA_NAMESPACE = "minecraft";
    private static final Set<ResourceLocation> ALLOWED_APPRENTICE_ENCHANTMENTS = Set.of(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "wisdom")
    );
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);
    private static final RawAnimation ANIM_IDLE_FRAME = RawAnimation.begin().thenLoop("idle_frame");
    private static final RawAnimation ANIM_IDLE_COG = RawAnimation.begin().thenLoop("idle_cog");
    private static final int ENCHANTMENT_VALUE = 14;
    private static final StaffTier CIRCUIT_HEAT_STAFF_TIER = new StaffTier(
            3.0F,
            -3.0F,
            new AttributeContainer(AttributeRegistry.SPELL_POWER, 0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new AttributeContainer(AttributeRegistry.FIRE_SPELL_POWER, 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new AttributeContainer(AttributeRegistry.LIGHTNING_SPELL_POWER, 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CircuitHeatStaff() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .fireResistant()
                .attributes(ExtendedSwordItem.createAttributes(CIRCUIT_HEAT_STAFF_TIER)));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public boolean hasCustomRendering() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(
                new AnimationController<>(this, FRAME_CONTROLLER, 0, state -> {
                    state.setAnimation(ANIM_IDLE_FRAME);
                    state.getController().setAnimationSpeed(CircuitHeatStaffClientRenderState.resolveFrameAnimationSpeed(
                            state.getData(DataTickets.ITEMSTACK),
                            state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE)
                    ));
                    return PlayState.CONTINUE;
                }),
                new AnimationController<>(this, COG_CONTROLLER, 0, state -> {
                    state.setAnimation(ANIM_IDLE_COG);
                    state.getController().setAnimationSpeed(CircuitHeatStaffClientRenderState.resolveCogAnimationSpeed(
                            state.getData(DataTickets.ITEMSTACK),
                            state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE)
                    ));
                    return PlayState.CONTINUE;
                })
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            return useClient(level, player, usedHand, stack);
        }

        return useServer(level, player, usedHand, stack);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        CircuitHeatStaffCoolingHandler.onEntityItemUpdate(stack, entity);
        return false;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (enchantment.is(net.minecraft.world.item.enchantment.Enchantments.FORTUNE)
                || enchantment.is(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH)) {
            return false;
        }
        var enchantmentId = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
        if (enchantmentId == null || isDurabilityTargetEnchantment(enchantment)) {
            return false;
        }

        if (super.supportsEnchantment(stack, enchantment)) {
            return true;
        }

        if (MalumCompatibility.isMagicCapableWeaponEnchantment(stack, enchantmentId)
                || MalumCompatibility.isSpiritPlunderSupported(stack, enchantmentId)) {
            return true;
        }

        if (ALLOWED_APPRENTICE_ENCHANTMENTS.contains(enchantmentId)) {
            return true;
        }

        return VANILLA_NAMESPACE.equals(enchantmentId.getNamespace())
                && new ItemStack(Items.DIAMOND_SWORD).supportsEnchantment(enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.isPrimaryItemFor(stack, enchantment) || supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(book);
        if (enchantments.isEmpty()) {
            return true;
        }

        return enchantments.keySet().stream()
                .allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public boolean isAnvilMergeEnchantmentAllowed(ItemStack stack, Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        lines.add(Component.translatable("item.apprenticecodex.circuit_heat_staff.tooltip").withStyle(ChatFormatting.GRAY));
        var cooldownBypassMaxRemainingTicks = CircuitHeatStaffConfigState.cooldownBypassMaxRemainingTicks();
        if (cooldownBypassMaxRemainingTicks > 0) {
            lines.add(Component.translatable(
                    "item.apprenticecodex.circuit_heat_staff.tooltip.cooldown_limit",
                    Math.max(1, (cooldownBypassMaxRemainingTicks + 19) / 20)
            ).withStyle(ChatFormatting.GRAY));
        }
        var remainingTicks = getStaffOverheatRemainingTicks(stack, context.level());
        if (remainingTicks > 0) {
            lines.add(Component.translatable(
                    "item.apprenticecodex.circuit_heat_staff.tooltip.overheat_remaining",
                    Math.max(1, (remainingTicks + 19) / 20)
            ).withStyle(ChatFormatting.RED));
        }

        super.appendHoverText(stack, context, lines, flag);
    }

    public static boolean isStaffOverheated(ItemStack stack, @Nullable Level level) {
        return getStaffOverheatRemainingTicks(stack, level) > 0;
    }

    public static int getStaffOverheatRemainingTicks(ItemStack stack, @Nullable Level level) {
        if (stack == null || stack.isEmpty() || level == null) {
            return 0;
        }

        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return 0;
        }

        var tag = customData.copyTag();
        if (!tag.contains(OVERHEAT_EXPIRE_GAME_TIME_TAG, Tag.TAG_LONG)) {
            return 0;
        }

        var expireGameTime = tag.getLong(OVERHEAT_EXPIRE_GAME_TIME_TAG);
        var sanitizedExpireGameTime = PersistentGameTimeSanitizer.repairPersistedFutureUntil(
                level.getGameTime(),
                expireGameTime,
                resolveStoredStaffOverheatRepairMaxTicks(tag)
        );
        if (sanitizedExpireGameTime != expireGameTime) {
            expireGameTime = sanitizedExpireGameTime;
            if (!level.isClientSide) {
                CustomData.update(
                        DataComponents.CUSTOM_DATA,
                        stack,
                        data -> data.putLong(OVERHEAT_EXPIRE_GAME_TIME_TAG, sanitizedExpireGameTime)
                );
            }
        }

        var remainingTicks = (int)Math.max(0L, expireGameTime - level.getGameTime());
        if (remainingTicks <= 0 && !level.isClientSide) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
                data.remove(OVERHEAT_EXPIRE_GAME_TIME_TAG);
                data.remove(OVERHEAT_DURATION_TICKS_TAG);
            });
        }
        return remainingTicks;
    }

    public static void startStaffOverheat(ItemStack stack, Level level, int cooldownTicks) {
        if (stack == null || stack.isEmpty() || cooldownTicks <= 0) {
            return;
        }

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    tag.putLong(
                            OVERHEAT_EXPIRE_GAME_TIME_TAG,
                            level.getGameTime() + cooldownTicks
                    );
                    tag.putInt(OVERHEAT_DURATION_TICKS_TAG, cooldownTicks);
                }
        );
    }

    public static int reduceStaffOverheatTicks(ItemStack stack, Level level, int reductionTicks) {
        if (stack == null || stack.isEmpty() || level == null || reductionTicks <= 0) {
            return getStaffOverheatRemainingTicks(stack, level);
        }

        var remainingTicks = getStaffOverheatRemainingTicks(stack, level);
        if (remainingTicks <= reductionTicks) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
                data.remove(OVERHEAT_EXPIRE_GAME_TIME_TAG);
                data.remove(OVERHEAT_DURATION_TICKS_TAG);
            });
            return 0;
        }

        var reducedTicks = remainingTicks - reductionTicks;
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    tag.putLong(OVERHEAT_EXPIRE_GAME_TIME_TAG, level.getGameTime() + reducedTicks);
                    tag.putInt(OVERHEAT_DURATION_TICKS_TAG, reducedTicks);
                }
        );
        return reducedTicks;
    }

    private InteractionResultHolder<ItemStack> useClient(Level level, Player player, InteractionHand usedHand, ItemStack stack) {
        var selection = new SpellSelectionManager(player).getSelection();
        if (selection == null || selection.spellData == SpellData.EMPTY) {
            return InteractionResultHolder.pass(stack);
        }

        if (ClientMagicData.isCasting()) {
            return InteractionResultHolder.consume(stack);
        }

        var spell = selection.spellData.getSpell();
        var hasRecast = ClientMagicData.getRecasts().hasRecastForSpell(spell);
        if (isStaffOverheated(stack, level) && !hasRecast) {
            return InteractionResultHolder.fail(stack);
        }

        var cooldown = ClientMagicData.getCooldowns().isOnCooldown(spell);
        if (cooldown) {
            return InteractionResultHolder.consume(stack);
        }

        return super.use(level, player, usedHand);
    }

    private InteractionResultHolder<ItemStack> useServer(Level level, Player player, InteractionHand usedHand, ItemStack stack) {
        var selection = new SpellSelectionManager(player).getSelection();
        if (selection == null || selection.spellData == SpellData.EMPTY) {
            return InteractionResultHolder.pass(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        var slot = usedHand == InteractionHand.MAIN_HAND ? SpellSelectionManager.MAINHAND : SpellSelectionManager.OFFHAND;
        return tryInitiateSelectedCast(level, serverPlayer, stack, selection, slot)
                ? InteractionResultHolder.consume(stack)
                : InteractionResultHolder.fail(stack);
    }

    private boolean tryInitiateSelectedCast(Level level, ServerPlayer player, ItemStack stack,
                                            SpellSelectionManager.SelectionOption selection, String slot) {
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return false;
        }

        var spellData = selection.spellData;
        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var castSource = selection.getCastSource();
        if (magicData.isCasting() && !magicData.getCastingSpellId().equals(spell.getSpellId())) {
            CancelCastPacket.cancelCast(player, magicData.getCastType() != CastType.LONG);
        }

        if (magicData.getPlayerRecasts().hasRecastForSpell(spell.getSpellId())) {
            // Iron's の Recast は通常 cooldown とマナ消費を使わないため、杖過熱中でも踏み倒し連鎖とは独立して扱う。
            return spell.attemptInitiateCast(stack, spellLevel, level, player, castSource, true, slot);
        }

        if (isStaffOverheated(stack, level)) {
            player.displayClientMessage(Component.translatable(
                    "ui.apprenticecodex.circuit_heat_staff.overheat_cool_down"
            ).withStyle(ChatFormatting.RED), true);
            return false;
        }

        if (!magicData.getPlayerCooldowns().isOnCooldown(spell)) {
            var casted = spell.attemptInitiateCast(stack, spellLevel, level, player, castSource, true, slot);
            if (casted) {
                CircuitHeatStaffOverheatManager.clear(player, spell.getSpellId());
            }
            return casted;
        }

        return useBypassingCooldown(level, player, stack, spell, spellLevel, castSource, slot, magicData)
                .getResult()
                .consumesAction();
    }

    private InteractionResultHolder<ItemStack> useBypassingCooldown(Level level, ServerPlayer player, ItemStack stack,
                                                                    AbstractSpell spell, int spellLevel, CastSource castSource,
                                                                    String slot, MagicData magicData) {
        var cooldowns = magicData.getPlayerCooldowns();
        var spellId = spell.getSpellId();
        var removedCooldown = cooldowns.getSpellCooldowns().get(spellId);
        if (removedCooldown == null) {
            return InteractionResultHolder.fail(stack);
        }

        var originalMana = magicData.getMana();
        var baseManaCost = spell.getManaCost(spellLevel);
        var remainingCooldownTicks = Math.max(0, removedCooldown.getCooldownRemaining());
        if (isCooldownBypassDenied(spellId, remainingCooldownTicks)) {
            return InteractionResultHolder.fail(stack);
        }

        var step = CircuitHeatStaffOverheatManager.getNextStep(player, spellId);
        var additionalManaCost = CircuitHeatStaffOverheatManager.getAdditionalManaCost(
                baseManaCost,
                step,
                remainingCooldownTicks
        );
        var plannedManaCost = baseManaCost + additionalManaCost;
        var effectiveCooldown = Math.max(0, WeaponImbueCooldownHelper.getEffectiveSpellCooldown(spell, player, castSource));
        var overheatTicks = resolveConfiguredStaffOverheatTicks((long) effectiveCooldown + remainingCooldownTicks);

        // cooldown だけ通常判定から外し、基礎マナ不足などの失敗条件は Iron's の判定をそのまま使う。
        // 追加マナ分だけは SpellOnCastEvent で上乗せし、足りなければ発動後に杖の過熱 cooldown へ入れる。
        cooldowns.removeCooldown(spellId);
        CircuitHeatStaffCastEvent.reserveOverheatCast(
                player,
                spellId,
                plannedManaCost,
                originalMana,
                overheatTicks,
                spell.getCastType() == CastType.CONTINUOUS
        );

        var casted = spell.attemptInitiateCast(stack, spellLevel, level, player, castSource, true, slot);
        if (!casted) {
            restoreCooldown(cooldowns.getSpellCooldowns(), spellId, removedCooldown);
            CircuitHeatStaffCastEvent.clearReservedOverheatCast(player);
            return InteractionResultHolder.fail(stack);
        }

        CircuitHeatStaffOverheatManager.applyAfterBypass(player, spellId, effectiveCooldown);
        player.displayClientMessage(Component.translatable(
                "ui.apprenticecodex.circuit_heat_staff.overheat_mana_warning",
                formatOverheatManaCostForDisplay(spell, plannedManaCost)
        ).withStyle(ChatFormatting.RED), true);
        return InteractionResultHolder.consume(stack);
    }

    public static String formatOverheatManaCostForDisplay(AbstractSpell spell, int manaCost) {
        if (spell.getCastType() != CastType.CONTINUOUS) {
            return Integer.toString(manaCost);
        }

        return manaCost * (20 / MagicManager.CONTINUOUS_CAST_TICK_INTERVAL) + "/s";
    }

    private static void restoreCooldown(java.util.Map<String, CooldownInstance> cooldowns, String spellId, CooldownInstance cooldown) {
        cooldowns.put(spellId, cooldown);
    }

    private static boolean isCooldownBypassDenied(String spellId, int remainingCooldownTicks) {
        var spellResourceLocation = ResourceLocation.tryParse(spellId);
        if (spellResourceLocation != null && ApprenticeCodexServerConfig.isCircuitHeatStaffSpellDenied(spellResourceLocation)) {
            return true;
        }

        var maxRemainingTicks = ApprenticeCodexServerConfig.circuitHeatStaffCooldownBypassMaxRemainingTicks();
        return maxRemainingTicks > 0 && remainingCooldownTicks > maxRemainingTicks;
    }

    private static int resolveConfiguredStaffOverheatTicks(long baseOverheatTicks) {
        var multipliedTicks = Math.ceil(Math.max(0L, baseOverheatTicks)
                * ApprenticeCodexServerConfig.circuitHeatStaffOverheatDurationMultiplier());
        var overheatTicks = (long) Math.min(multipliedTicks, Integer.MAX_VALUE);
        overheatTicks = Math.max(overheatTicks, ApprenticeCodexServerConfig.circuitHeatStaffOverheatDurationMinTicks());

        var capTicks = ApprenticeCodexServerConfig.circuitHeatStaffOverheatDurationCapTicks();
        if (capTicks > 0) {
            overheatTicks = Math.min(overheatTicks, capTicks);
        }

        return (int) Math.min(overheatTicks, Integer.MAX_VALUE);
    }

    private static int resolveStoredStaffOverheatRepairMaxTicks(CompoundTag tag) {
        var storedDurationTicks = Math.max(0, tag.getInt(OVERHEAT_DURATION_TICKS_TAG));
        if (storedDurationTicks > 0) {
            return storedDurationTicks;
        }

        var capTicks = ApprenticeCodexServerConfig.circuitHeatStaffOverheatDurationCapTicks();
        return Math.max(capTicks, 0);
    }

    private static boolean isDurabilityTargetEnchantment(Holder<Enchantment> enchantment) {
        return DURABILITY_ENCHANTMENT_PROBE_STACK.supportsEnchantment(enchantment);
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isSelectedSpellOverheated(Player player) {
        var selection = new SpellSelectionManager(player).getSelection();
        if (selection == null || selection.spellData == SpellData.EMPTY) {
            return false;
        }

        return CircuitHeatStaffOverheatManager.getState(player, selection.spellData.getSpell().getSpellId()).active();
    }
}
