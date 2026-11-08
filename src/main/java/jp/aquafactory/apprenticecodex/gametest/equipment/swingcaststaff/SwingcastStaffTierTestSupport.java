package jp.aquafactory.apprenticecodex.gametest.equipment.swingcaststaff;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.Set;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastType;
import jp.aquafactory.apprenticecodex.item.swingstaff.AbstractSwingcastStaffItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastCooldownMode;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.assertModifierAmount;

final class SwingcastStaffTierTestSupport {
    private SwingcastStaffTierTestSupport() {}

    static void assertSwingcastStaffTier(
            GameTestHelper helper,
            AbstractSwingcastStaffItem item,
            Set<SpellGunCastType> expectedCastTypes,
            SwingcastCooldownMode expectedCooldownMode,
            AbstractSpell instantSpell,
            AbstractSpell longSpell,
            AbstractSpell continuousSpell,
            String itemName
    ) {
        var tier = item.getSwingcastStaffTier();
        helper.assertTrue(tier.supportedCastTypes().equals(expectedCastTypes),
                itemName + " cast type regression: expected " + expectedCastTypes + " but got " + tier.supportedCastTypes());
        helper.assertTrue(tier.swingcastCooldownMode() == expectedCooldownMode,
                itemName + " cooldown mode regression: expected " + expectedCooldownMode + " but got " + tier.swingcastCooldownMode());

        var allowsLong = expectedCastTypes.contains(SpellGunCastType.LONG);
        helper.assertTrue(item.canImbueSpell(instantSpell, 1),
                itemName + " should allow instant spell imbuing");
        helper.assertTrue(item.canImbueSpell(longSpell, 1) == allowsLong,
                itemName + " long spell imbue regression: expected " + allowsLong);
        helper.assertFalse(item.canImbueSpell(continuousSpell, 1),
                itemName + " should continue rejecting continuous spells");
        assertModifierAmount(
                helper,
                new ItemStack(item).getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY),
                Attributes.ATTACK_SPEED.value(),
                EquipmentSlotGroup.MAINHAND,
                tier.attackSpeedModifier(),
                AttributeModifier.Operation.ADD_VALUE,
                itemName + " attack speed component regression"
        );
    }
}
