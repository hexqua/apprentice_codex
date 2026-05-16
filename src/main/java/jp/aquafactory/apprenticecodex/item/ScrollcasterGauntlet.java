package jp.aquafactory.apprenticecodex.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.ScrollcasterGauntletRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 3;
    public static final int CALIBRATION_SCROLL_SLOT_COUNT = 10;

    private static final String MAIN_CONTROLLER = "main";
    private static final String CALIBRATION_TAG = "SpellCalibration";
    private static final String ADJUSTMENTS_TAG = "Adjustments";
    private static final String SCROLLS_TAG = "Scrolls";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";
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

    public static @NotNull ItemStack getCalibrationAdjustment(@NotNull ItemStack gauntletStack, int slot) {
        return getCalibrationItem(gauntletStack, ADJUSTMENTS_TAG, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT);
    }

    public static void setCalibrationAdjustment(@NotNull ItemStack gauntletStack, int slot, @NotNull ItemStack stack) {
        setCalibrationItem(gauntletStack, ADJUSTMENTS_TAG, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT, stack);
    }

    public static @NotNull ItemStack getCalibrationScroll(@NotNull ItemStack gauntletStack, int slot) {
        return getCalibrationItem(gauntletStack, SCROLLS_TAG, slot, CALIBRATION_SCROLL_SLOT_COUNT);
    }

    public static void setCalibrationScroll(@NotNull ItemStack gauntletStack, int slot, @NotNull ItemStack stack) {
        setCalibrationItem(gauntletStack, SCROLLS_TAG, slot, CALIBRATION_SCROLL_SLOT_COUNT, stack);
    }

    public static boolean hasAnyCalibrationItem(@NotNull ItemStack gauntletStack) {
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (!getCalibrationAdjustment(gauntletStack, slot).isEmpty()) {
                return true;
            }
        }
        for (var slot = 0; slot < CALIBRATION_SCROLL_SLOT_COUNT; ++slot) {
            if (!getCalibrationScroll(gauntletStack, slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static @NotNull ItemStack getCalibrationItem(@NotNull ItemStack gauntletStack, String listName,
                                                         int slot, int slotCount) {
        if (!isValidCalibrationAccess(gauntletStack, slot, slotCount)) {
            return ItemStack.EMPTY;
        }

        var calibrationTag = gauntletStack.getTagElement(CALIBRATION_TAG);
        if (calibrationTag == null || !calibrationTag.contains(listName, Tag.TAG_LIST)) {
            return ItemStack.EMPTY;
        }

        var list = calibrationTag.getList(listName, Tag.TAG_COMPOUND);
        for (var index = 0; index < list.size(); ++index) {
            var entry = list.getCompound(index);
            if (entry.getInt(SLOT_TAG) != slot || !entry.contains(ITEM_TAG, Tag.TAG_COMPOUND)) {
                continue;
            }
            return ItemStack.of(entry.getCompound(ITEM_TAG));
        }
        return ItemStack.EMPTY;
    }

    private static void setCalibrationItem(@NotNull ItemStack gauntletStack, String listName, int slot, int slotCount,
                                           @NotNull ItemStack stack) {
        if (!isValidCalibrationAccess(gauntletStack, slot, slotCount)) {
            return;
        }

        var calibrationTag = gauntletStack.getOrCreateTagElement(CALIBRATION_TAG);
        var list = calibrationTag.contains(listName, Tag.TAG_LIST)
                ? calibrationTag.getList(listName, Tag.TAG_COMPOUND)
                : new ListTag();
        removeCalibrationItem(list, slot);

        if (!stack.isEmpty()) {
            var storedStack = stack.copy();
            storedStack.setCount(1);
            var entry = new CompoundTag();
            entry.putInt(SLOT_TAG, slot);
            entry.put(ITEM_TAG, storedStack.save(new CompoundTag()));
            list.add(entry);
        }

        if (list.isEmpty()) {
            calibrationTag.remove(listName);
        } else {
            calibrationTag.put(listName, list);
        }
        if (calibrationTag.isEmpty()) {
            gauntletStack.removeTagKey(CALIBRATION_TAG);
        }
    }

    private static void removeCalibrationItem(ListTag list, int slot) {
        for (var index = list.size() - 1; index >= 0; --index) {
            if (list.getCompound(index).getInt(SLOT_TAG) == slot) {
                list.remove(index);
            }
        }
    }

    private static boolean isValidCalibrationAccess(@NotNull ItemStack gauntletStack, int slot, int slotCount) {
        return !gauntletStack.isEmpty()
                && gauntletStack.getItem() instanceof ScrollcasterGauntlet
                && slot >= 0
                && slot < slotCount;
    }
}
