package jp.aquafactory.apprenticecodex.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.utility.InitialSpellContainerHelper;
import jp.aquafactory.apprenticecodex.utility.PresetSpellContainerStateHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class AbstractRightClickMagicWeaponItem extends Item implements IPresetSpellContainer, NonDamageableAnvilMergeItem {
    private static final String VANILLA_NAMESPACE = "minecraft";
    private static final String MALUM_NAMESPACE = "malum";
    private static final ResourceLocation FORGE_SHIELDS_TAG_ID = ResourceLocation.fromNamespaceAndPath("forge", "shields");
    private static final ResourceLocation FORGE_TOOLS_SHIELDS_TAG_ID =
            ResourceLocation.fromNamespaceAndPath("forge", "tools/shields");
    private static final ResourceLocation MALUM_SPIRIT_PLUNDER =
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "spirit_plunder");
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "soul_hunter_weapon")
    );
    private static final Set<ResourceLocation> ALLOWED_MAGIC_ITEM_ENCHANTMENTS = Set.of(
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "transcendence"),
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "wisdom")
    );
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);
    private static final UUID ATTACK_DAMAGE_MODIFIER_ID = Item.BASE_ATTACK_DAMAGE_UUID;
    private static final UUID ATTACK_SPEED_MODIFIER_ID = Item.BASE_ATTACK_SPEED_UUID;

    private final Supplier<? extends AbstractSpell> configuredSpell;
    private final int configuredSpellLevel;
    private final boolean startsWithPresetSpell;
    private final boolean spellWheelEnabled;
    private final int enchantmentValue;
    private final String itemKey;
    private final double attackDamage;
    private final double attackSpeed;
    private final List<AttributeBonus> handBonuses;
    private final Multimap<Attribute, AttributeModifier> mainhandModifiers;

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
            InitialSpellContainerHelper.addInitialSpellIfEnabled(
                    spellContainer,
                    configuredSpell,
                    configuredSpellLevel,
                    0,
                    true
            );
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
                spellWheelEnabled,
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
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            return mainhandModifiers;
        }

        return super.getAttributeModifiers(slot, stack);
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
    public int getEnchantmentValue(ItemStack stack) {
        return enchantmentValue;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (enchantmentId == null) {
            return false;
        }

        if (isDurabilityTargetEnchantment(enchantment)) {
            return false;
        }

        if (MalumHauntedCompat.isAnimatedEnchantment(enchantmentId)) {
            return false;
        }

        if (MalumHauntedCompat.isHauntedEnchantment(enchantmentId)
                && MalumHauntedCompat.isSupportedHauntedMainhandItem(stack)) {
            return true;
        }

        if (isMalumSpiritPlunder(stack, enchantmentId)) {
            return true;
        }

        if (ALLOWED_MAGIC_ITEM_ENCHANTMENTS.contains(enchantmentId)) {
            return true;
        }

        return enchantment.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD));
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantments(book);
        if (enchantments.isEmpty()) {
            return true;
        }

        return enchantments.keySet().stream()
                .allMatch(enchantment -> canApplyAtEnchantingTable(stack, enchantment));
    }

    @Override
    public boolean isAnvilMergeEnchantmentAllowed(ItemStack stack, Enchantment enchantment) {
        return canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        return true;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return ToolActions.SWORD_SWEEP == toolAction || super.canPerformAction(stack, toolAction);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        lines.add(Component.translatable("item.apprenticecodex.right_click_magic_weapon.desc")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, lines, flag);
    }

    public static boolean isFullyChargedAttack(Player player) {
        return player.getAttackStrengthScale(0.5f) > 0.9f;
    }

    public static boolean isShieldLikeOffhandItem(ItemStack stack) {
        // 継承元ではなく Forge の盾契約で判定し、ShieldItem 非継承の MOD 盾や
        // Shield Expansion のタグ拡張にも追従する。
        return stack.canPerformAction(ToolActions.SHIELD_BLOCK)
                || stack.is(ItemTags.create(FORGE_SHIELDS_TAG_ID))
                || stack.is(ItemTags.create(FORGE_TOOLS_SHIELDS_TAG_ID));
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

    private Multimap<Attribute, AttributeModifier> buildBaseMainhandModifiers() {
        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        ATTACK_DAMAGE_MODIFIER_ID,
                        "Weapon modifier",
                        attackDamage,
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        ATTACK_SPEED_MODIFIER_ID,
                        "Weapon modifier",
                        attackSpeed,
                        AttributeModifier.Operation.ADDITION
                )
        );

        var prefix = "apprenticecodex." + itemKey + ".mainhand";
        for (int i = 0; i < handBonuses.size(); ++i) {
            var bonus = handBonuses.get(i);
            var attribute = bonus.attributeSupplier().get();
            if (attribute == null || bonus.amount() == 0.0D) {
                continue;
            }

            var attributeKey = resolveAttributeKey(bonus, attribute, i);
            var modifierIdSeed = prefix + "." + attributeKey + "." + i;
            var modifierId = UUID.nameUUIDFromBytes(modifierIdSeed.getBytes(StandardCharsets.UTF_8));
            builder.put(
                    attribute,
                    new AttributeModifier(modifierId, modifierIdSeed, bonus.amount(), bonus.operation())
            );
        }
        return builder.build();
    }

    private static boolean isDurabilityTargetEnchantment(Enchantment enchantment) {
        return enchantment.canApplyAtEnchantingTable(DURABILITY_ENCHANTMENT_PROBE_STACK);
    }

    private static boolean isMalumSpiritPlunder(ItemStack stack, ResourceLocation enchantmentId) {
        return MALUM_SPIRIT_PLUNDER.equals(enchantmentId) && stack.is(MALUM_SOUL_HUNTER_WEAPON);
    }

    private static String resolveAttributeKey(AttributeBonus bonus, Attribute attribute, int index) {
        if (bonus.key() != null && !bonus.key().isBlank()) {
            return normalizeKeyToken(bonus.key());
        }

        var registryKey = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        if (registryKey != null) {
            return normalizeKeyToken(registryKey.toString());
        }

        // 登録キーが得られない属性でもUUIDが毎回安定するようフォールバックを固定化する.
        return "unknown_" + index;
    }

    protected static String normalizeKeyToken(String token) {
        return Objects.requireNonNull(token)
                .toLowerCase(Locale.ROOT)
                .replace(':', '.')
                .replace('/', '.')
                .replaceAll("[^a-z0-9._-]", "_");
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
