package jp.aquafactory.apprenticecodex.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.ScrollcasterGauntletRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;
import java.util.function.Consumer;

public final class ScrollcasterGauntlet extends Item implements GeoItem {
    private static final String MAIN_CONTROLLER = "main";
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final double ATTACK_DAMAGE_BONUS = 5.0D;
    private static final double ATTACK_SPEED_BONUS = -2.2D;
    private static final double SPELL_POWER_BONUS = 0.05D;
    private static final UUID SPELL_POWER_MODIFIER_ID = UUID.fromString("be797f84-cdc5-41fd-871f-685cebb23f5c");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ScrollcasterGauntlet() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return slot == EquipmentSlot.MAINHAND ? buildMainhandModifiers(stack) : super.getAttributeModifiers(slot, stack);
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ScrollcasterGauntletRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new ScrollcasterGauntletRenderer();
                }

                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, MAIN_CONTROLLER, 0, state -> {
            state.setAnimation(ANIM_IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private static Multimap<Attribute, AttributeModifier> buildMainhandModifiers(ItemStack stack) {
        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_UUID,
                        "Weapon modifier",
                        ATTACK_DAMAGE_BONUS,
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        Item.BASE_ATTACK_SPEED_UUID,
                        "Weapon modifier",
                        ATTACK_SPEED_BONUS,
                        AttributeModifier.Operation.ADDITION
                )
        );
        if (shouldApplyBaseSpellPowerBonus(stack)) {
            builder.put(
                    AttributeRegistry.SPELL_POWER.get(),
                    new AttributeModifier(
                            SPELL_POWER_MODIFIER_ID,
                            "apprenticecodex.scrollcaster_gauntlet.mainhand.spell_power",
                            SPELL_POWER_BONUS,
                            AttributeModifier.Operation.MULTIPLY_BASE
                    )
            );
        }
        return builder.build();
    }

    private static boolean shouldApplyBaseSpellPowerBonus(ItemStack stack) {
        return stack != null && !stack.isEmpty();
    }
}
