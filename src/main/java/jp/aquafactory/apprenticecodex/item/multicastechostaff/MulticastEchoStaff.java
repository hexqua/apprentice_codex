package jp.aquafactory.apprenticecodex.item.multicastechostaff;

import io.redspace.ironsspellbooks.api.item.weapons.ExtendedSwordItem;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.UniqueItem;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import io.redspace.ironsspellbooks.item.weapons.StaffTier;
import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.InitialSpellContainerHelper;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MulticastEchoStaff extends StaffItem implements GeoItem, IPresetSpellContainer, UniqueItem, WisdomPolicy {

    @Override
    public boolean isWisdomActiveWhileHeld() {
        return true;
    }
    private static final StaffTier MULTICAST_ECHO_STAFF_TIER = new StaffTier(
            3.0F,
            -3.0F,
            new AttributeContainer(
                    AttributeRegistry.SPELL_POWER,
                    0.05D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ),
            new AttributeContainer(
                    AttributeRegistry.ELDRITCH_SPELL_POWER,
                    0.15D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ),
            new AttributeContainer(
                    AttributeRegistry.CAST_TIME_REDUCTION,
                    0.2D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            )
    );

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MulticastEchoStaff() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant()
                .attributes(ExtendedSwordItem.createAttributes(MULTICAST_ECHO_STAFF_TIER)));
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
        if (itemStack == null || itemStack.isEmpty() || ISpellContainer.isSpellContainer(itemStack)) {
            return;
        }

        InitialSpellContainerHelper.setInitialContainer(itemStack, 1, true, false, SpellRegistry.ECHO_CAST, 1);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (enchantment.is(net.minecraft.world.item.enchantment.Enchantments.FORTUNE)
                || enchantment.is(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH)) {
            return false;
        }
        if (super.supportsEnchantment(stack, enchantment)) {
            return true;
        }

        var enchantmentId = enchantment.unwrapKey().map(key -> key.location()).orElse(null);
        if (MalumCompatibility.isMagicCapableWeaponEnchantment(stack, enchantmentId)) {
            return true;
        }

        return false;
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
    public int getEnchantmentValue(ItemStack stack) {
        // Pastel Staff と同じく金ツール相当。
        return 22;
    }

    public static boolean isMulticastEchoStaff(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof MulticastEchoStaff;
    }
}
