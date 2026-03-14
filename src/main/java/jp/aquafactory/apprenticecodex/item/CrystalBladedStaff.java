package jp.aquafactory.apprenticecodex.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.renderer.item.CrystalBladedStaffRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;

public class CrystalBladedStaff extends Item implements GeoItem, IPresetSpellContainer {
    private static final String VANILLA_NAMESPACE = "minecraft";
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final UUID ATTACK_DAMAGE_MODIFIER_ID = Item.BASE_ATTACK_DAMAGE_UUID;
    private static final UUID ATTACK_SPEED_MODIFIER_ID = Item.BASE_ATTACK_SPEED_UUID;
    private static final UUID ENTITY_REACH_MODIFIER_ID =
            UUID.nameUUIDFromBytes("apprenticecodex:crystal_bladed_staff/entity_reach".getBytes(StandardCharsets.UTF_8));
    private static final UUID SPELL_POWER_MODIFIER_ID =
            UUID.nameUUIDFromBytes("apprenticecodex:crystal_bladed_staff/spell_power".getBytes(StandardCharsets.UTF_8));
    private static final double ATTACK_DAMAGE = 5.0D;
    private static final double ATTACK_SPEED = -2.4D;
    private static final double ENTITY_REACH_BONUS = 1.0D;
    private static final double SPELL_POWER_BONUS = 0.10D;
    private static final int ENCHANTMENT_VALUE = 14;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Multimap<Attribute, AttributeModifier> mainhandModifiers;

    public CrystalBladedStaff() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
        this.mainhandModifiers = buildMainhandModifiers();
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void initializeSpellContainer(ItemStack itemStack) {
        if (itemStack == null || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        ISpellContainer.set(itemStack, ISpellContainer.create(1, true, false));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private CrystalBladedStaffRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new CrystalBladedStaffRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(
                this, "main", 0,
                state -> {
                    state.setAnimation(ANIM_IDLE);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            return mainhandModifiers;
        }

        return super.getAttributeModifiers(slot, stack);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand usedHand) {
        return InteractionResultHolder.pass(player.getItemInHand(usedHand));
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
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
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (enchantmentId == null) {
            return false;
        }

        if (isDurabilityTargetEnchantment(enchantment)) {
            return false;
        }

        if (VANILLA_NAMESPACE.equals(enchantmentId.getNamespace())) {
            return enchantment.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD));
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
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.DIAMOND) || super.isValidRepairItem(toRepair, repair);
    }

    private static boolean isDurabilityTargetEnchantment(Enchantment enchantment) {
        return enchantment.canApplyAtEnchantingTable(DURABILITY_ENCHANTMENT_PROBE_STACK);
    }

    private static Multimap<Attribute, AttributeModifier> buildMainhandModifiers() {
        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        ATTACK_DAMAGE_MODIFIER_ID,
                        "Weapon modifier",
                        ATTACK_DAMAGE,
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        ATTACK_SPEED_MODIFIER_ID,
                        "Weapon modifier",
                        ATTACK_SPEED,
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                ForgeMod.ENTITY_REACH.get(),
                new AttributeModifier(
                        ENTITY_REACH_MODIFIER_ID,
                        "apprenticecodex.crystal_bladed_staff.entity_reach",
                        ENTITY_REACH_BONUS,
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                AttributeRegistry.SPELL_POWER.get(),
                new AttributeModifier(
                        SPELL_POWER_MODIFIER_ID,
                        "apprenticecodex.crystal_bladed_staff.spell_power",
                        SPELL_POWER_BONUS,
                        AttributeModifier.Operation.MULTIPLY_BASE
                )
        );
        return builder.build();
    }
}
