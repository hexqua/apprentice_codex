package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.Predicate;

public abstract class AbstractRightClickMagicWeaponItem extends Item implements IPresetSpellContainer, NonDamageableAnvilMergeItem {
    private static final Set<ResourceLocation> ALLOWED_MAGIC_ITEM_ENCHANTMENTS = Set.of(
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "transcendence"),
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "wisdom")
    );
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);
    private final @Nullable Supplier<? extends AbstractSpell> configuredSpell;
    private final int configuredSpellLevel;
    private final boolean startsWithPresetSpell;
    private final boolean spellWheelEnabled;
    private final int enchantmentValue;
    private final String itemKey;
    private final double attackDamage;
    private final double attackSpeed;
    private final List<AttributeBonus> handBonuses;
    private final ItemAttributeModifiers mainhandModifiers;

    protected AbstractRightClickMagicWeaponItem(
            Properties properties,
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            boolean spellWheelEnabled,
            int enchantmentValue,
            String itemKey,
            double attackDamage,
            double attackSpeed,
            List<AttributeBonus> handBonuses
    ) {
        super(properties);
        this.configuredSpell = Objects.requireNonNull(configuredSpell);
        this.configuredSpellLevel = configuredSpellLevel;
        this.startsWithPresetSpell = true;
        this.spellWheelEnabled = spellWheelEnabled;
        this.enchantmentValue = enchantmentValue;
        this.itemKey = normalizeKeyToken(itemKey);
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.handBonuses = List.copyOf(handBonuses);
        this.mainhandModifiers = buildBaseMainhandModifiers();
    }

    protected AbstractRightClickMagicWeaponItem(
            Properties properties,
            boolean spellWheelEnabled,
            int enchantmentValue,
            String itemKey,
            double attackDamage,
            double attackSpeed,
            List<AttributeBonus> handBonuses
    ) {
        super(properties);
        this.configuredSpell = null;
        this.configuredSpellLevel = 0;
        this.startsWithPresetSpell = false;
        this.spellWheelEnabled = spellWheelEnabled;
        this.enchantmentValue = enchantmentValue;
        this.itemKey = normalizeKeyToken(itemKey);
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.handBonuses = List.copyOf(handBonuses);
        this.mainhandModifiers = buildBaseMainhandModifiers();
    }

    protected AbstractRightClickMagicWeaponItem(
            Properties properties,
            Supplier<? extends AbstractSpell> configuredSpell,
            int configuredSpellLevel,
            boolean spellWheelEnabled,
            int enchantmentValue,
            String itemKey,
            double attackDamage,
            double attackSpeed,
            AttributeBonus... handBonuses
    ) {
        this(
                properties,
                configuredSpell,
                configuredSpellLevel,
                spellWheelEnabled,
                enchantmentValue,
                itemKey,
                attackDamage,
                attackSpeed,
                List.of(handBonuses)
        );
    }

    protected AbstractRightClickMagicWeaponItem(
            Properties properties,
            boolean spellWheelEnabled,
            int enchantmentValue,
            String itemKey,
            double attackDamage,
            double attackSpeed,
            AttributeBonus... handBonuses
    ) {
        this(
                properties,
                spellWheelEnabled,
                enchantmentValue,
                itemKey,
                attackDamage,
                attackSpeed,
                List.of(handBonuses)
        );
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        if (repairPresetSpellContainerStateIfNeeded(itemStack)) {
            return;
        }

        if (ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        var spellContainer = ISpellContainer.create(1, spellWheelEnabled, false).mutableCopy();
        if (startsWithPresetSpell) {
            if (configuredSpell instanceof net.neoforged.neoforge.registries.DeferredHolder<?, ?> deferredHolder && !deferredHolder.isBound()) {
                return;
            }
            if (configuredSpell != null) {
                spellContainer.addSpellAtIndex(configuredSpell.get(), configuredSpellLevel, 0, true);
            }
        }
        ISpellContainer.set(itemStack, spellContainer.toImmutable());
    }

    public final boolean repairPresetSpellContainerStateIfNeeded(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }

        Predicate<SpellData> trackedStateValidator = this instanceof RestrictedSpellImbuableItem restrictedSpellImbuableItem
                ? restrictedSpellImbuableItem::canImbueSpell
                : spellData -> spellData != SpellData.EMPTY && spellData.getSpell() != SpellRegistry.none();
        if (PresetSpellContainerStateHelper.restoreIfNeeded(
                itemStack,
                1,
                false,
                false,
                trackedStateValidator
        )) {
            return true;
        }

        return normalizeLegacyOverriddenSpellContainerIfNeeded(itemStack);
    }

    protected boolean normalizeLegacyOverriddenSpellContainerIfNeeded(ItemStack stack) {
        return false;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND || shouldPrioritizeOffhandUse(player)) {
            return InteractionResultHolder.pass(stack);
        }

        var castResult = tryCastSelectedSpell(player, stack);
        return switch (castResult) {
            case SUCCESS -> InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            case FAIL -> InteractionResultHolder.fail(stack);
            case NONE -> InteractionResultHolder.pass(stack);
        };
    }

    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        return mainhandModifiers;
    }

    @Override
    public boolean canAttackBlock(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(@NotNull ItemStack stack) {
        return enchantmentValue;
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        if (super.supportsEnchantment(stack, enchantment)) {
            return true;
        }

        var enchantmentId = enchantment.unwrapKey().map(ResourceKey::location).orElse(null);
        if (enchantmentId == null || isDurabilityTargetEnchantment(enchantment)) {
            return false;
        }

        if (MalumCompatibility.isMagicCapableWeaponEnchantment(stack, enchantmentId)) {
            return true;
        }

        if (MalumCompatibility.isSpiritPlunderSupported(stack, enchantmentId)) {
            return true;
        }

        if (ALLOWED_MAGIC_ITEM_ENCHANTMENTS.contains(enchantmentId)) {
            return true;
        }

        return enchantment.value().canEnchant(new ItemStack(Items.DIAMOND_SWORD));
    }

    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return super.isPrimaryItemFor(stack, enchantment) || supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
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
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        return true;
    }

    @Override
    public boolean canPerformAction(@NotNull ItemStack stack, @NotNull ItemAbility itemAbility) {
        return itemAbility == ItemAbilities.SWORD_SWEEP || super.canPerformAction(stack, itemAbility);
    }

    public static boolean isFullyChargedAttack(Player player) {
        return player.getAttackStrengthScale(0.5f) > 0.9f;
    }

    public static boolean isShieldLikeOffhandItem(ItemStack stack) {
        return stack.canPerformAction(ItemAbilities.SHIELD_BLOCK)
                || stack.is(Tags.Items.TOOLS_SHIELD);
    }

    protected final boolean isSameItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == this;
    }

    protected final boolean shouldPrioritizeOffhandUse(Player player) {
        var offhandStack = player.getOffhandItem();
        return offhandStack.getItem() instanceof AbstractSpellGunItem || isShieldLikeOffhandItem(offhandStack);
    }

    protected final @Nullable SpellData getPrimarySpellData(ItemStack stack) {
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

    protected final boolean matchesConfiguredPresetSpell(@Nullable SpellData spellData) {
        return spellData != null
                && startsWithPresetSpell
                && configuredSpell != null
                && configuredSpell.get().equals(spellData.getSpell())
                && configuredSpellLevel == spellData.getLevel();
    }

    private CastResult tryCastSelectedSpell(Player player, ItemStack stack) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            initializeSpellContainer(stack);
        }

        var selectionOption = new SpellSelectionManager(player).getSelection();
        if (selectionOption == null || selectionOption.spellData == SpellData.EMPTY) {
            return CastResult.NONE;
        }

        var spellData = selectionOption.spellData;
        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var casted = spell.attemptInitiateCast(
                stack,
                spellLevel,
                player.level(),
                player,
                selectionOption.getCastSource(),
                true,
                SpellSelectionManager.MAINHAND
        );

        return casted ? CastResult.SUCCESS : CastResult.FAIL;
    }

    private ItemAttributeModifiers buildBaseMainhandModifiers() {
        var builder = ItemAttributeModifiers.builder();
        builder.add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        // Tooltip と実攻撃値の契約を vanilla sword/staff と揃えるため base ID を使う。
                        BASE_ATTACK_DAMAGE_ID,
                        attackDamage,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );
        builder.add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        BASE_ATTACK_SPEED_ID,
                        attackSpeed,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        var prefix = itemKey + "_mainhand_";
        for (int i = 0; i < handBonuses.size(); ++i) {
            var bonus = handBonuses.get(i);
            var attribute = bonus.attributeSupplier().get();
            if (attribute == null || bonus.amount() == 0.0D) {
                continue;
            }

            var attributeKey = resolveAttributeKey(bonus, attribute, i);
            var modifierId = ResourceLocation.fromNamespaceAndPath(
                    "apprenticecodex",
                    prefix + attributeKey + "_" + i
            );
            builder.add(
                    BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute),
                    new AttributeModifier(modifierId, bonus.amount(), bonus.operation()),
                    EquipmentSlotGroup.MAINHAND
            );
        }
        return builder.build();
    }

    private static boolean isDurabilityTargetEnchantment(Holder<Enchantment> enchantment) {
        return enchantment.value().canEnchant(DURABILITY_ENCHANTMENT_PROBE_STACK);
    }
    private static String resolveAttributeKey(AttributeBonus bonus, Attribute attribute, int index) {
        if (bonus.key() != null && !bonus.key().isBlank()) {
            return normalizeKeyToken(bonus.key());
        }

        var registryKey = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
        if (registryKey != null) {
            return normalizeKeyToken(registryKey.toString());
        }

        // 登録キーが得られない属性でもUUIDが毎回安定するようフォールバックを固定化する.
        return "unknown_" + index;
    }

    protected static String normalizeKeyToken(String token) {
        return Objects.requireNonNull(token)
                .toLowerCase(Locale.ROOT)
                .replace(':', '_')
                .replace('/', '_')
                .replace('.', '_')
                .replaceAll("[^a-z0-9_-]", "_");
    }

    protected static AttributeBonus bonus(
            Supplier<? extends Attribute> attributeSupplier,
            double amount,
            AttributeModifier.Operation operation
    ) {
        return new AttributeBonus(attributeSupplier, amount, operation, null);
    }

    protected static AttributeBonus bonus(
            Supplier<? extends Attribute> attributeSupplier,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        return new AttributeBonus(attributeSupplier, amount, operation, key);
    }

    protected static AttributeBonus bonus(
            Holder<Attribute> attributeHolder,
            double amount,
            AttributeModifier.Operation operation
    ) {
        return new AttributeBonus(attributeHolder::value, amount, operation, null);
    }

    protected static AttributeBonus bonus(
            Holder<Attribute> attributeHolder,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        return new AttributeBonus(attributeHolder::value, amount, operation, key);
    }

    protected static AttributeBonus bonus(
            Attribute attribute,
            double amount,
            AttributeModifier.Operation operation
    ) {
        return new AttributeBonus(() -> attribute, amount, operation, null);
    }

    protected static AttributeBonus bonus(
            Attribute attribute,
            double amount,
            AttributeModifier.Operation operation,
            String key
    ) {
        return new AttributeBonus(() -> attribute, amount, operation, key);
    }

    protected record AttributeBonus(
            Supplier<? extends Attribute> attributeSupplier,
            double amount,
            AttributeModifier.Operation operation,
            @Nullable String key
    ) {
        public AttributeBonus {
            Objects.requireNonNull(attributeSupplier);
            Objects.requireNonNull(operation);
        }
    }

    protected enum CastResult {
        NONE,
        SUCCESS,
        FAIL
    }
}
