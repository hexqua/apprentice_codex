package jp.aquafactory.apprenticecodex.item.curios.circlets;

import com.google.common.collect.ImmutableMultimap;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentPolicy;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentResolver;
import jp.aquafactory.apprenticecodex.enchantment.AttributeEnchantmentType;
import jp.aquafactory.apprenticecodex.enchantment.TranscendencePolicy;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.OffhandMagicCompatibleItem;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class EnchantedCirclet extends AbstractCircletItem
        implements NonDamageableAnvilMergeItem, OffhandMagicCompatibleItem, TranscendencePolicy,
        AttributeEnchantmentPolicy, WisdomPolicy {
    private static final String ITEM_KEY = "enchanted_circlet";
    private static final AttributeContainer[] CIRCLET_ATTRIBUTES = {
            new AttributeContainer(
                    Attributes.ATTACK_DAMAGE,
                    -0.10D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            )
    };

    public EnchantedCirclet() {
        super(Rarity.UNCOMMON, CIRCLET_ATTRIBUTES);
    }

    @Override
    public boolean isWisdomActiveWhileHeld() {
        return false;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 15;
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return getEnchantmentValue(stack) > 0;
    }

    @Override
    public Set<AttributeEnchantmentType> directlyApplicableAttributeEnchantments() {
        return ALL_ATTRIBUTE_ENCHANTMENTS;
    }

    @Override
    protected void addAdditionalModifiers(
            ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> builder,
            ItemStack stack,
            ResourceLocation slotId
    ) {
        builder.putAll(AttributeEnchantmentResolver.resolveMergedModifiers(
                ImmutableMultimap.of(),
                stack,
                "apprenticecodex." + ITEM_KEY + "." + slotId
        ));
    }
}
