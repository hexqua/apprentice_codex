package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.item.weapons.ExtendedSwordItem;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.UniqueItem;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import io.redspace.ironsspellbooks.item.weapons.StaffTier;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Set;

public class PastelStaff extends StaffItem implements GeoItem, IPresetSpellContainer, UniqueItem {
    public static final String STONE_TINT_COLOR_TAG = "StoneTintColor";
    public static final String STONE_AFFINITY_SCHOOL_TAG = "StoneAffinitySchool";
    public static final int DEFAULT_STONE_TINT_COLOR = 0xFFFFFF;

    private static final String VANILLA_NAMESPACE = "minecraft";
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);
    private static final Set<ResourceLocation> ALLOWED_VANILLA_WEAPON_ENCHANTMENTS = Set.of(
            ResourceLocation.withDefaultNamespace("looting"),
            ResourceLocation.withDefaultNamespace("knockback"),
            ResourceLocation.withDefaultNamespace("fortune"),
            ResourceLocation.withDefaultNamespace("silk_touch")
    );

    private static final double BASE_STAFF_SPELL_POWER_BONUS = 0.10D;
    private static final StaffTier PASTEL_STAFF_WEAPON_TIER = new StaffTier(3.0F, -3.0F);
    private static final String AFFINITY_MODIFIER_PATH_PREFIX = "pastel_staff_affinity_";
    private static final ResourceLocation BASE_SPELL_POWER_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            "apprenticecodex",
            "pastel_staff_base_spell_power"
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PastelStaff() {
        super(new Item.Properties().stacksTo(1).attributes(ExtendedSwordItem.createAttributes(PASTEL_STAFF_WEAPON_TIER)));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public boolean hasCustomRendering() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        // Datagen時はSpellRegistry未バインドのため、初期呪文の注入をスキップする.
        if (!SpellRegistry.PALETTE_SHIFT.isBound()) {
            return;
        }

        var spellContainer = ISpellContainer.create(1, true, false).mutableCopy();
        spellContainer.addSpell(SpellRegistry.PALETTE_SHIFT.get(), 1, true);
        ISpellContainer.set(itemStack, spellContainer.toImmutable());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        var baseModifiers = super.getDefaultAttributeModifiers(stack);
        var builder = ItemAttributeModifiers.builder();

        for (var entry : baseModifiers.modifiers()) {
            var idPath = entry.modifier().id().getPath();
            if (!idPath.startsWith(AFFINITY_MODIFIER_PATH_PREFIX)
                    && !entry.modifier().id().equals(BASE_SPELL_POWER_MODIFIER_ID)) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        builder.add(
                AttributeRegistry.SPELL_POWER,
                new AttributeModifier(
                        BASE_SPELL_POWER_MODIFIER_ID,
                        BASE_STAFF_SPELL_POWER_BONUS,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        var schoolType = readStoneAffinitySchool(stack);
        if (schoolType != null) {
            var powerAttribute = MagicTools.resolveSchoolPowerAttribute(schoolType);
            if (powerAttribute != null) {
                var schoolId = schoolType.getId();
                var affinitySpellPowerBonus = ApprenticeCodexServerConfig.pastelStaffAmplifyTintedMagicMultiplier();
                builder.add(
                        BuiltInRegistries.ATTRIBUTE.wrapAsHolder(powerAttribute),
                        new AttributeModifier(
                                createAffinityModifierId(schoolId),
                                affinitySpellPowerBonus,
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                        ),
                        EquipmentSlotGroup.MAINHAND
                );
            }
        }

        return builder.build();
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        var enchantmentId = enchantment.unwrapKey().map(key -> key.location()).orElse(null);
        if (enchantmentId == null) {
            return false;
        }

        if (isDurabilityTargetEnchantment(enchantment)) {
            return false;
        }

        if (VANILLA_NAMESPACE.equals(enchantmentId.getNamespace())) {
            return ALLOWED_VANILLA_WEAPON_ENCHANTMENTS.contains(enchantmentId);
        }

        return enchantment.value().isSupportedItem(new ItemStack(Items.DIAMOND_SWORD));
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
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

        return enchantments.keySet().stream().allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 22;
    }

    private static boolean isDurabilityTargetEnchantment(Holder<Enchantment> enchantment) {
        return enchantment.value().isSupportedItem(DURABILITY_ENCHANTMENT_PROBE_STACK);
    }

    public int getStoneTintColor(ItemStack stack) {
        return readStoneTintColor(stack);
    }

    public static int readStoneTintColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return DEFAULT_STONE_TINT_COLOR;
        }

        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return DEFAULT_STONE_TINT_COLOR;
        }

        var tag = customData.copyTag();
        if (!tag.contains(STONE_TINT_COLOR_TAG, Tag.TAG_ANY_NUMERIC)) {
            return DEFAULT_STONE_TINT_COLOR;
        }

        return tag.getInt(STONE_TINT_COLOR_TAG) & 0xFFFFFF;
    }

    public static void writeStoneTintColor(ItemStack stack, int rgb) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(STONE_TINT_COLOR_TAG, rgb & 0xFFFFFF));
    }

    public static void writeStoneAffinitySchool(ItemStack stack, SchoolType schoolType) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.putString(STONE_AFFINITY_SCHOOL_TAG, schoolType.getId().toString())
        );
    }

    public static SchoolType readStoneAffinitySchool(ItemStack stack) {
        var schoolId = readStoneAffinitySchoolId(stack);
        if (schoolId == null) {
            return null;
        }
        return SchoolRegistry.getSchool(schoolId);
    }

    public static boolean isPastelStaff(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof PastelStaff;
    }

    private static ResourceLocation readStoneAffinitySchoolId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }

        var tag = customData.copyTag();
        if (!tag.contains(STONE_AFFINITY_SCHOOL_TAG, Tag.TAG_STRING)) {
            return null;
        }

        return ResourceLocation.tryParse(tag.getString(STONE_AFFINITY_SCHOOL_TAG));
    }

    private static ResourceLocation createAffinityModifierId(ResourceLocation schoolId) {
        return ResourceLocation.fromNamespaceAndPath(
                "apprenticecodex",
                AFFINITY_MODIFIER_PATH_PREFIX + schoolId.getNamespace() + "_" + schoolId.getPath()
        );
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(itemStack, context, lines, flag);

        var schoolType = readStoneAffinitySchool(itemStack);
        if (schoolType == null) {
            return;
        }

        lines.add(Component.translatable("item.apprenticecodex.pastel_staff.desc.affinity", schoolType.getDisplayName())
                .withStyle(ChatFormatting.GRAY));
    }
}
