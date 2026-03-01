package jp.aquafactory.apprenticecodex.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.UniqueItem;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import io.redspace.ironsspellbooks.item.weapons.StaffTier;
import io.redspace.ironsspellbooks.render.ClientStaffItemExtensions;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.PastelStaffRenderer;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class PastelStaff extends StaffItem implements GeoItem, IPresetSpellContainer, UniqueItem {
    public static final String STONE_TINT_COLOR_TAG = "StoneTintColor";
    public static final String STONE_AFFINITY_SCHOOL_TAG = "StoneAffinitySchool";
    public static final int DEFAULT_STONE_TINT_COLOR = 0xFFFFFF;
    private static final String AFFINITY_MODIFIER_NAME_PREFIX = "apprenticecodex.pastel_staff.affinity.";
    private static final String VANILLA_NAMESPACE = "minecraft";
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);
    private static final Set<ResourceLocation> ALLOWED_VANILLA_WEAPON_ENCHANTMENTS = Set.of(
            ResourceLocation.withDefaultNamespace("looting"),
            ResourceLocation.withDefaultNamespace("knockback"),
            ResourceLocation.withDefaultNamespace("fortune"),
            ResourceLocation.withDefaultNamespace("silk_touch")
    );
    private static final StaffTier PASTEL_STAFF_TIER = new StaffTier(
            3.0F,
            -3.0F,
            new AttributeContainer(
                    AttributeRegistry.SPELL_POWER,
                    0.10D,
                    AttributeModifier.Operation.MULTIPLY_BASE
            )
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PastelStaff() {
        super(new Item.Properties().stacksTo(1), PASTEL_STAFF_TIER);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public boolean hasCustomRendering() {
        return true;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new ClientStaffItemExtensions() {
            private PastelStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new PastelStaffRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || ISpellContainer.isSpellContainer(itemStack)) {
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
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        var baseModifiers = super.getAttributeModifiers(slot, stack);
        if (slot != EquipmentSlot.MAINHAND) {
            return baseModifiers;
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        // 再計算前に染色前の由来の親和 modifier を取り除く.
        for (var entry : baseModifiers.entries()) {
            if (!entry.getValue().getName().startsWith(AFFINITY_MODIFIER_NAME_PREFIX)) {
                builder.put(entry);
            }
        }

        var schoolType = readStoneAffinitySchool(stack);
        if (schoolType == null) {
            return builder.build();
        }

        var powerAttribute = MagicTools.resolveSchoolPowerAttribute(schoolType);
        if (powerAttribute == null) {
            return builder.build();
        }

        var schoolId = schoolType.getId();
        var affinitySpellPowerBonus = ApprenticeCodexServerConfig.pastelStaffAmplifyTintedMagicMultiplier();
        builder.put(
                powerAttribute,
                new AttributeModifier(
                        createAffinityModifierId(schoolId),
                        AFFINITY_MODIFIER_NAME_PREFIX + schoolId,
                        affinitySpellPowerBonus,
                        AttributeModifier.Operation.MULTIPLY_BASE
                )
        );
        return builder.build();
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

        if (VANILLA_NAMESPACE.equals(enchantmentId.getNamespace())) {
            return ALLOWED_VANILLA_WEAPON_ENCHANTMENTS.contains(enchantmentId);
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
    public int getEnchantmentValue(ItemStack stack) {
        // 金ツール相当.
        return 22;
    }

    private static boolean isDurabilityTargetEnchantment(Enchantment enchantment) {
        // エリトラは耐久値を持つが武器/ツール系カテゴリではないため,
        // ここに付くエンチャントを「耐久値持ちアイテム向け」とみなして除外する.
        return enchantment.canApplyAtEnchantingTable(DURABILITY_ENCHANTMENT_PROBE_STACK);
    }

    public int getStoneTintColor(ItemStack stack) {
        return readStoneTintColor(stack);
    }

    public static int readStoneTintColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return DEFAULT_STONE_TINT_COLOR;
        }

        var tag = stack.getTag();
        if (tag == null || !tag.contains(STONE_TINT_COLOR_TAG, Tag.TAG_ANY_NUMERIC)) {
            return DEFAULT_STONE_TINT_COLOR;
        }

        return tag.getInt(STONE_TINT_COLOR_TAG) & 0xFFFFFF;
    }

    public static void writeStoneTintColor(ItemStack stack, int rgb) {
        stack.getOrCreateTag().putInt(STONE_TINT_COLOR_TAG, rgb & 0xFFFFFF);
    }

    public static void writeStoneAffinitySchool(ItemStack stack, SchoolType schoolType) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.getOrCreateTag().putString(STONE_AFFINITY_SCHOOL_TAG, schoolType.getId().toString());
    }

    @Nullable
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

    @Nullable
    private static ResourceLocation readStoneAffinitySchoolId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        var tag = stack.getTag();
        if (tag == null || !tag.contains(STONE_AFFINITY_SCHOOL_TAG, Tag.TAG_STRING)) {
            return null;
        }

        return ResourceLocation.tryParse(tag.getString(STONE_AFFINITY_SCHOOL_TAG));
    }

    private static UUID createAffinityModifierId(ResourceLocation schoolId) {
        return UUID.nameUUIDFromBytes(
                ("apprenticecodex:pastel_staff_affinity/" + schoolId).getBytes(StandardCharsets.UTF_8)
        );
    }
    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(itemStack, context, lines, flag);

        var schoolType = readStoneAffinitySchool(itemStack);
        if (schoolType == null) {
            return;
        }

        lines.add(Component.translatable("item.apprenticecodex.pastel_staff.desc.affinity", schoolType.getDisplayName())
                .withStyle(ChatFormatting.GRAY));
    }

}
