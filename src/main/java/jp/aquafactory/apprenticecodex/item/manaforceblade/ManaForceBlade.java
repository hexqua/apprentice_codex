package jp.aquafactory.apprenticecodex.item.manaforceblade;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.SpellSlotUpgradeItem;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBladeConfigState;
import jp.aquafactory.apprenticecodex.renderer.item.ManaForceBladeRenderer;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import jp.aquafactory.apprenticecodex.item.SpellSlotUpgradeableItem;
import jp.aquafactory.apprenticecodex.item.offhand.OffhandMagicModifierHelper;

public class ManaForceBlade extends SwordItem implements GeoItem, IPresetSpellContainer, SpellSlotUpgradeableItem {
    public static final float DISPLAY_ATTACK_DAMAGE = 6.0F;
    public static final int DEFAULT_RELEASE_COOLDOWN_TICKS = 40;
    private static final String EPICFIGHT_MOD_ID = "epicfight";
    private static final String DESCRIPTION_TRANSLATION_KEY = "item.apprenticecodex.mana_force_blade.desc";
    private static final String EPICFIGHT_DESCRIPTION_TRANSLATION_KEY = DESCRIPTION_TRANSLATION_KEY + ".epicfight";

    private static final int DURABILITY = 2031;
    private static final int USE_DURATION = 72000;
    private static final int ENCHANTMENT_VALUE = 15;
    private static final double ATTACK_DAMAGE_MODIFIER_AMOUNT = DISPLAY_ATTACK_DAMAGE - 1.0D;
    private static final double ATTACK_SPEED_MODIFIER_AMOUNT = -2.0D;
    private static final String ATTACK_MANA_SPENT_TICK_TAG = "apprenticecodex:mana_force_blade_attack_mana_spent_tick";
    private static final String PERFECT_GUARD_RELEASE_COOLDOWN_GRACE_TICK_TAG =
            "apprenticecodex:mana_force_blade_perfect_guard_release_cooldown_grace_tick";
    private static final String PERFECT_GUARD_RELEASE_COOLDOWN_GRACE_USES_TAG =
            "apprenticecodex:mana_force_blade_perfect_guard_release_cooldown_grace_uses";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final ItemStack SWORD_ENCHANTMENT_PROBE_STACK = new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
    private static final Set<String> EXTRA_ENCHANTMENTS = Set.of(
            "apprenticecodex:surge",
            "apprenticecodex:attunement",
            "apprenticecodex:wisdom",
            "apprenticecodex:transcendence"
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Multimap<Holder<Attribute>, AttributeModifier> mainhandModifiers = buildMainhandModifiers();

    public ManaForceBlade() {
        super(Tiers.DIAMOND, new Item.Properties()
                .stacksTo(1)
                .durability(DURABILITY)
                .rarity(Rarity.RARE)
                .fireResistant()
                .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 0, (float) ATTACK_SPEED_MODIFIER_AMOUNT)));
        GeoItem.registerSyncedAnimatable(this);
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
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty() || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        ISpellContainer.set(itemStack, ISpellContainer.create(1, true, false));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        if (ModList.get().isLoaded(EPICFIGHT_MOD_ID)) {
            // Epic Fight では GuardSkill と専用互換 listener 側へ任せる。ここで使用状態に入ると vanilla 由来の防御姿勢が混ざる。
            return InteractionResultHolder.consume(stack);
        }

        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity, int timeLeft) {
        if (!level.isClientSide && livingEntity instanceof Player player) {
            if (consumePerfectGuardReleaseCooldownGrace(stack, level)) {
                return;
            }

            var cooldownTicks = ApprenticeCodexServerConfig.manaForceBladeReleaseCooldownTicks();
            if (cooldownTicks > 0) {
                player.getCooldowns().addCooldown(this, cooldownTicks);
            }
        }
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        if (shouldSpendAttackMana(stack) && !attacker.level().isClientSide && attacker instanceof Player player) {
            trySpendAttackManaOncePerTick(player, stack);
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        lines.add(Component.translatable(resolveDescriptionTranslationKey()).withStyle(ChatFormatting.GRAY));
        if (!isImbueDamageChangeEnabled(ManaForceBladeConfigState.imbueDamageMultiplierScale())) {
            return;
        }

        if (!hasImbuedSpell(stack)) {
            lines.add(Component.translatable("item.apprenticecodex.mana_force_blade.desc.no_imbue")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        var manaCost = resolveBladeAttackManaCost(
                null,
                stack,
                ManaForceBladeConfigState.attackManaCostMultiplier(),
                ManaForceBladeConfigState.attackManaSchoolMultiplierScale(),
                ManaForceBladeConfigState.imbueDamageMultiplierScale()
        );
        if (manaCost > 0.0F) {
            lines.add(Component.translatable(
                    "item.apprenticecodex.mana_force_blade.desc.imbue_help",
                    Mth.ceil(manaCost)
            ).withStyle(ChatFormatting.AQUA));
        }
    }

    private static String resolveDescriptionTranslationKey() {
        return ModList.get().isLoaded(EPICFIGHT_MOD_ID)
                ? EPICFIGHT_DESCRIPTION_TRANSLATION_KEY
                : DESCRIPTION_TRANSLATION_KEY;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        // Iron's の upgrade 処理は同 Attribute/Operation の既存補正 1 本を置換するため、表示前に自前補正を合算しておく。
        var modifiers = OffhandMagicModifierHelper.buildEquippedModifiers(mainhandModifiers, stack, "mana_force_blade");
        var builder = ItemAttributeModifiers.builder();
        for (var entry : modifiers.entries()) {
            builder.add(entry.getKey(), entry.getValue(), EquipmentSlotGroup.MAINHAND);
        }
        return builder.build();
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ENCHANTMENT_VALUE;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (super.supportsEnchantment(stack, enchantment)) {
            return true;
        }

        var enchantmentId = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
        return enchantmentId != null
                && (MalumCompatibility.isMagicCapableWeaponEnchantment(stack, enchantmentId)
                || EXTRA_ENCHANTMENTS.contains(enchantmentId.toString())
                || SWORD_ENCHANTMENT_PROBE_STACK.supportsEnchantment(enchantment));
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
        return enchantments.isEmpty() || enchantments.keySet().stream()
                .allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
        return repair.is(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public boolean canPerformAction(@NotNull ItemStack stack, @NotNull ItemAbility itemAbility) {
        return itemAbility == ItemAbilities.SWORD_SWEEP || super.canPerformAction(stack, itemAbility);
    }

    @Override
    public ItemStack createSpellSlotUpgradeResult(ItemStack baseStack, SpellSlotUpgradeItem upgradeItem) {
        return ItemStack.EMPTY;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ManaForceBladeRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new ManaForceBladeRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "main", 0, state -> {
            state.setAnimation(ANIM_IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public static boolean isManaForceBlade(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ManaForceBlade;
    }

    public static float resolveBladeAttackDamage(ItemStack stack) {
        return resolveBladeAttackDamage(stack, DISPLAY_ATTACK_DAMAGE);
    }

    private static float resolveBladeAttackDamage(ItemStack stack, float baseDamage) {
        var damage = baseDamage;
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            var enchantmentLevel = entry.getIntValue();
            for (var effect : entry.getKey().value().getEffects(EnchantmentEffectComponents.DAMAGE)) {
                if (effect.requirements().isEmpty()) {
                    damage = effect.effect().process(enchantmentLevel, RandomSource.create(0L), damage);
                }
            }
        }
        return damage;
    }

    public static float resolveBladeAttackManaCost(ItemStack stack) {
        return resolveBladeAttackManaCost(null, stack);
    }

    public static float resolveBladeAttackManaCost(@Nullable LivingEntity attacker, ItemStack stack) {
        return resolveBladeAttackManaCost(
                attacker,
                stack,
                ApprenticeCodexServerConfig.manaForceBladeAttackManaCostMultiplier(),
                ApprenticeCodexServerConfig.manaForceBladeAttackManaSchoolMultiplierScale(),
                ApprenticeCodexServerConfig.manaForceBladeImbueDamageMultiplierScale()
        );
    }

    public static float resolveBladeAttackManaCost(
            @Nullable LivingEntity attacker,
            ItemStack stack,
            float attackManaCostMultiplier,
            float attackManaSchoolMultiplierScale,
            float imbueDamageMultiplierScale
    ) {
        if (attackManaCostMultiplier <= 0.0F || !isImbueDamageChangeEnabled(imbueDamageMultiplierScale)
                || !hasImbuedSpell(stack)) {
            return 0.0F;
        }

        var damageMultiplier = attacker == null
                ? 1.0F
                : resolveDamageMultiplier(attacker, stack, imbueDamageMultiplierScale);
        var schoolManaFactor = 1.0F + (damageMultiplier - 1.0F) * attackManaSchoolMultiplierScale;
        return resolveBladeAttackDamage(stack) * attackManaCostMultiplier * Math.max(0.0F, schoolManaFactor);
    }

    public static boolean hasImbuedSpell(ItemStack stack) {
        return MagicTools.getImbuedSpellSchool(stack) != null;
    }

    public static float resolveFinalAttackDamage(LivingEntity attacker, ItemStack stack) {
        return resolveBladeAttackDamage(stack) * resolveDamageMultiplier(attacker, stack);
    }

    public static float resolveFinalAttackDamage(LivingEntity attacker, ItemStack stack, float imbueDamageMultiplierScale) {
        return resolveBladeAttackDamage(stack) * resolveDamageMultiplier(attacker, stack, imbueDamageMultiplierScale);
    }

    public static float resolveDamageMultiplier(LivingEntity attacker, ItemStack stack) {
        return resolveDamageMultiplier(
                attacker,
                stack,
                ApprenticeCodexServerConfig.manaForceBladeImbueDamageMultiplierScale()
        );
    }

    public static float resolveDamageMultiplier(LivingEntity attacker, ItemStack stack, float imbueDamageMultiplierScale) {
        if (!isImbueDamageChangeEnabled(imbueDamageMultiplierScale)) {
            return 1.0F;
        }

        var imbuedSchool = MagicTools.getImbuedSpellSchool(stack);
        if (imbuedSchool == null) {
            return 1.0F;
        }

        var spellPower = (float) attacker.getAttributeValue(AttributeRegistry.SPELL_POWER);
        Attribute schoolPowerAttribute = MagicTools.resolveSchoolPowerAttribute(imbuedSchool);
        var schoolSpellPower = schoolPowerAttribute == null
                ? 1.0F
                : (float) attacker.getAttributeValue(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(schoolPowerAttribute));
        return spellPower * schoolSpellPower * imbueDamageMultiplierScale;
    }

    public static void spendMana(Player player, float manaCost) {
        if (manaCost <= 0.0F || player.getAbilities().instabuild) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        magicData.setMana(Math.max(0.0F, magicData.getMana() - manaCost));
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(magicData));
        }
    }

    public static void rememberPerfectGuardReleaseCooldownGrace(ItemStack stack, long gameTime) {
        var graceTicks = ApprenticeCodexServerConfig.manaForceBladePerfectGuardReleaseCooldownGraceTicks();
        var graceUses = ApprenticeCodexServerConfig.manaForceBladePerfectGuardReleaseCooldownGraceUses();
        if (stack.isEmpty() || graceTicks <= 0 || graceUses <= 0) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong(PERFECT_GUARD_RELEASE_COOLDOWN_GRACE_TICK_TAG, gameTime);
            tag.putInt(PERFECT_GUARD_RELEASE_COOLDOWN_GRACE_USES_TAG, graceUses);
        });
    }

    private static boolean consumePerfectGuardReleaseCooldownGrace(ItemStack stack, Level level) {
        var graceTicks = ApprenticeCodexServerConfig.manaForceBladePerfectGuardReleaseCooldownGraceTicks();
        var configuredGraceUses = ApprenticeCodexServerConfig.manaForceBladePerfectGuardReleaseCooldownGraceUses();
        if (stack.isEmpty() || graceTicks <= 0 || configuredGraceUses <= 0) {
            return false;
        }

        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        var tag = customData.copyTag();
        if (tag == null
                || !tag.contains(PERFECT_GUARD_RELEASE_COOLDOWN_GRACE_TICK_TAG, Tag.TAG_LONG)
                || !tag.contains(PERFECT_GUARD_RELEASE_COOLDOWN_GRACE_USES_TAG, Tag.TAG_INT)) {
            return false;
        }

        var remainingUses = tag.getInt(PERFECT_GUARD_RELEASE_COOLDOWN_GRACE_USES_TAG);
        var elapsedTicks = level.getGameTime() - tag.getLong(PERFECT_GUARD_RELEASE_COOLDOWN_GRACE_TICK_TAG);
        if (remainingUses <= 0 || elapsedTicks < 0 || elapsedTicks > graceTicks) {
            clearPerfectGuardReleaseCooldownGrace(stack);
            return false;
        }

        remainingUses -= 1;
        var updatedRemainingUses = remainingUses;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, updatedTag -> {
            if (updatedRemainingUses <= 0) {
                clearPerfectGuardReleaseCooldownGrace(updatedTag);
            } else {
                updatedTag.putInt(PERFECT_GUARD_RELEASE_COOLDOWN_GRACE_USES_TAG, updatedRemainingUses);
            }
        });
        return true;
    }

    private static void clearPerfectGuardReleaseCooldownGrace(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, ManaForceBlade::clearPerfectGuardReleaseCooldownGrace);
    }

    private static void clearPerfectGuardReleaseCooldownGrace(CompoundTag tag) {
        tag.remove(PERFECT_GUARD_RELEASE_COOLDOWN_GRACE_TICK_TAG);
        tag.remove(PERFECT_GUARD_RELEASE_COOLDOWN_GRACE_USES_TAG);
    }

    private static void trySpendAttackManaOncePerTick(Player player, ItemStack stack) {
        var tag = player.getPersistentData();
        var now = player.level().getGameTime();
        if (tag.contains(ATTACK_MANA_SPENT_TICK_TAG) && tag.getLong(ATTACK_MANA_SPENT_TICK_TAG) == now) {
            return;
        }

        // BetterCombat は複数対象を同一 tick 内で個別に攻撃処理するため、攻撃 1 回分の消費に揃える。
        tag.putLong(ATTACK_MANA_SPENT_TICK_TAG, now);
        spendMana(player, resolveBladeAttackManaCost(player, stack));
    }

    public static boolean isImbueDamageChangeEnabled(float imbueDamageMultiplierScale) {
        return imbueDamageMultiplierScale > 0.0F;
    }

    private static boolean shouldSpendAttackMana(ItemStack stack) {
        return hasImbuedSpell(stack)
                && isImbueDamageChangeEnabled(ApprenticeCodexServerConfig.manaForceBladeImbueDamageMultiplierScale())
                && ApprenticeCodexServerConfig.manaForceBladeAttackManaCostMultiplier() > 0.0F;
    }

    private static Multimap<Holder<Attribute>, AttributeModifier> buildMainhandModifiers() {
        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_ID,
                        ATTACK_DAMAGE_MODIFIER_AMOUNT,
                        AttributeModifier.Operation.ADD_VALUE
                )
        );
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        Item.BASE_ATTACK_SPEED_ID,
                        ATTACK_SPEED_MODIFIER_AMOUNT,
                        AttributeModifier.Operation.ADD_VALUE
                )
        );
        return builder.build();
    }
}
