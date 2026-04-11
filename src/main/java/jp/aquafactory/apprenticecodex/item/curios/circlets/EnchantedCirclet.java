package jp.aquafactory.apprenticecodex.item.curios.circlets;

import com.google.common.collect.ImmutableMultimap;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import jp.aquafactory.apprenticecodex.item.OffhandMagicCompatibleItem;
import jp.aquafactory.apprenticecodex.item.OffhandMagicModifierHelper;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;

public class EnchantedCirclet extends AbstractCircletItem
        implements NonDamageableAnvilMergeItem, OffhandMagicCompatibleItem {
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
    public int getEnchantmentValue(ItemStack stack) {
        return OffhandMagicModifierHelper.enchantmentValue();
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return OffhandMagicModifierHelper.isEnchantable(stack);
    }

    @Override
    protected void addAdditionalHeadModifiers(
            ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> builder,
            SlotContext slotContext,
            ItemStack stack,
            String modifierSlotName
    ) {
        builder.putAll(OffhandMagicModifierHelper.buildEquippedModifiers(
                ImmutableMultimap.of(),
                stack,
                ITEM_KEY
        ));
    }
}
