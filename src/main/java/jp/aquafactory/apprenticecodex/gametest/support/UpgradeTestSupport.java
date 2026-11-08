package jp.aquafactory.apprenticecodex.gametest.support;

import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.item.armor.UpgradeOrbType;
import io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry;
import io.redspace.ironsspellbooks.util.ModTags;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import org.jetbrains.annotations.Nullable;

import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.assertModifierAmount;

public final class UpgradeTestSupport {
    private UpgradeTestSupport() {}

    public static void assertMainhandUpgradeBridge(
            GameTestHelper helper,
            ItemStack stack,
            String message
    ) {
        var upgradeData = createUpgradeData(
                helper.getLevel().registryAccess(),
                stack,
                UpgradeOrbTypeRegistry.MANA,
                EquipmentSlot.MAINHAND.getName()
        );

        var event = new ItemAttributeModifierEvent(stack, stack.getItem().getDefaultAttributeModifiers(stack));
        NeoForge.EVENT_BUS.post(event);

        assertModifierAmount(
                helper,
                event.build(),
                AttributeRegistry.MAX_MANA.value(),
                EquipmentSlotGroup.MAINHAND,
                50.0D,
                AttributeModifier.Operation.ADD_VALUE,
                message + ": expected +50 max mana from " + upgradeData
        );
    }

    public static void assertUpgradeable(GameTestHelper helper, ItemStack stack, String message) {
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        helper.assertTrue(stack.is(ModTags.CAN_BE_UPGRADED),
                message + " (missing upgrade whitelist tag on " + itemId + ")");
        helper.assertTrue(Utils.canBeUpgraded(stack),
                message + " (Utils.canBeUpgraded returned false for " + itemId + ")");
    }

    public static UpgradeData createUpgradeData(
            RegistryAccess registryAccess,
            ItemStack stack,
            ResourceKey<UpgradeOrbType> upgradeKey,
            String slotName
    ) {
        var upgradeRegistry = registryAccess.registryOrThrow(UpgradeOrbTypeRegistry.UPGRADE_ORB_REGISTRY_KEY);
        var upgradeHolder = upgradeRegistry.getHolder(upgradeKey)
                .orElseThrow(() -> new IllegalStateException("Missing upgrade orb type: " + upgradeKey.location()));
        var upgradeData = new UpgradeData(Map.of(upgradeHolder, 1), slotName);
        UpgradeData.set(stack, upgradeData);
        return upgradeData;
    }

    @Nullable
    public static ResourceKey<UpgradeOrbType> findUpgradeKeyForPowerAttribute(
            Attribute spellPowerAttribute
    ) {
        if (Objects.equals(spellPowerAttribute, AttributeRegistry.FIRE_SPELL_POWER.value())) {
            return UpgradeOrbTypeRegistry.FIRE_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, AttributeRegistry.ICE_SPELL_POWER.value())) {
            return UpgradeOrbTypeRegistry.ICE_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, AttributeRegistry.LIGHTNING_SPELL_POWER.value())) {
            return UpgradeOrbTypeRegistry.LIGHTNING_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, AttributeRegistry.HOLY_SPELL_POWER.value())) {
            return UpgradeOrbTypeRegistry.HOLY_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, AttributeRegistry.ENDER_SPELL_POWER.value())) {
            return UpgradeOrbTypeRegistry.ENDER_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, AttributeRegistry.BLOOD_SPELL_POWER.value())) {
            return UpgradeOrbTypeRegistry.BLOOD_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, AttributeRegistry.EVOCATION_SPELL_POWER.value())) {
            return UpgradeOrbTypeRegistry.EVOCATION_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, AttributeRegistry.NATURE_SPELL_POWER.value())) {
            return UpgradeOrbTypeRegistry.NATURE_SPELL_POWER;
        }
        return null;
    }
}
