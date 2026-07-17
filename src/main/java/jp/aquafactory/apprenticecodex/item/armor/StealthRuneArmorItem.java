package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentPolicy;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentResolver;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.enchantment.TranscendencePolicy;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.renderer.armor.StealthRuneArmorRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class StealthRuneArmorItem extends ArmorItem implements GeoItem, IPresetSpellContainer,
        WisdomPolicy, TranscendencePolicy, AttributeEnchantmentPolicy {
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final Set<AttributeEnchantmentType> DIRECT_ATTRIBUTE_ENCHANTMENTS = Set.of(
            AttributeEnchantmentType.ALACRITY,
            AttributeEnchantmentType.REFLUX,
            AttributeEnchantmentType.RESERVOIR,
            AttributeEnchantmentType.TENSE
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ItemAttributeModifiers armorAttributeModifiers;

    public StealthRuneArmorItem(Type type) {
        super(Holder.direct(StealthRuneArmorStats.MATERIAL), type, StealthRuneArmorStats.createProperties(type));
        this.armorAttributeModifiers = StealthRuneArmorStats.createAttributeModifiers(type);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public boolean isWisdomActiveWhileHeld() {
        return false;
    }

    public boolean hasImbueSlot() {
        return getType() == Type.CHESTPLATE;
    }

    @Override
    public boolean isTranscendenceActiveWhileHeld() {
        return false;
    }

    @Override
    public boolean supportsDirectTranscendenceApplication() {
        return hasImbueSlot();
    }

    @Override
    public Set<AttributeEnchantmentType> directlyApplicableAttributeEnchantments() {
        return DIRECT_ATTRIBUTE_ENCHANTMENTS;
    }

    public static boolean isStealthRuneArmor(ItemStack stack) {
        return stack.getItem() instanceof StealthRuneArmorItem;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private StealthRuneArmorRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    @Nullable T livingEntity,
                    ItemStack itemStack,
                    @Nullable EquipmentSlot equipmentSlot,
                    @Nullable HumanoidModel<T> original
            ) {
                if (this.renderer == null) {
                    this.renderer = new StealthRuneArmorRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", state -> {
            state.getController().setAnimation(ANIM_IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || !hasImbueSlot() || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        // 胴だけに魔法枠を持たせ、既存ローブ系と同じ装備感に揃える。
        ISpellContainer.set(itemStack, ISpellContainer.create(1, true, true));
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        var baseModifiers = super.getDefaultAttributeModifiers(stack);
        var builder = ItemAttributeModifiers.builder();
        for (var entry : baseModifiers.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        for (var entry : armorAttributeModifiers.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }
        StealthRuneArmorStats.addSpellPowerModifier(
                builder,
                getType(),
                ApprenticeCodexServerConfig.stealthRuneArmorSpellPowerBonusPerPiece()
        );
        AttributeEnchantmentResolver.addModifiers(
                builder,
                stack,
                EquipmentSlotGroup.bySlot(getType().getSlot()),
                "stealth_rune_armor_" + StealthRuneArmorStats.typeToken(getType()) + "_enchant"
        );
        return builder.build();
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (super.supportsEnchantment(stack, enchantment)) {
            return true;
        }

        var attributeEnchantment = AttributeEnchantmentType.from(enchantment);
        if (attributeEnchantment.isPresent()) {
            return supportsDirectAttributeEnchantment(attributeEnchantment.get());
        }
        var enchantmentId = enchantment.unwrapKey().map(key -> key.location()).orElse(null);
        return enchantmentId != null && isSupportedArmorEnchantment(enchantmentId);
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
        return enchantments.isEmpty()
                || enchantments.keySet().stream().allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return StealthRuneArmorStats.enchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
        return StealthRuneArmorStats.isRepairIngredient(repair) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            Item.TooltipContext context,
            @NotNull List<Component> lines,
            @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("item." + ApprenticeCodex.MODID + ".stealth_rune_armor.desc")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private boolean isSupportedArmorEnchantment(ResourceLocation enchantmentId) {
        return enchantmentId.equals(Enchantments.WISDOM.location())
                || hasImbueSlot() && enchantmentId.equals(Enchantments.TRANSCENDENCE.location());
    }
}
