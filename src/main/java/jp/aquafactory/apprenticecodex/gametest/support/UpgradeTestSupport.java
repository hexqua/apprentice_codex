package jp.aquafactory.apprenticecodex.gametest.support;

import io.redspace.ironsspellbooks.api.backwards_compat.UpgradeTypeCache;
import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.item.armor.UpgradeOrbType;
import io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry;
import io.redspace.ironsspellbooks.util.ModTags;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public final class UpgradeTestSupport {
    private UpgradeTestSupport() {}

    public static void assertUpgradeable(GameTestHelper helper, ItemStack stack, String message) {
        helper.assertTrue(stack.is(ModTags.CAN_BE_UPGRADED),
                message + " (missing upgrade whitelist tag on " + ForgeRegistries.ITEMS.getKey(stack.getItem()) + ")");
        helper.assertTrue(Utils.canBeUpgraded(stack),
                message + " (Utils.canBeUpgraded returned false for " + ForgeRegistries.ITEMS.getKey(stack.getItem()) + ")");
    }

    public static UpgradeData createUpgradeData(
            RegistryAccess registryAccess,
            ItemStack stack,
            ResourceKey<UpgradeOrbType> upgradeKey,
            String slotName
    ) {
        UpgradeTypeCache.doCache(registryAccess);
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
        if (Objects.equals(spellPowerAttribute, AttributeRegistry.FIRE_SPELL_POWER.get())) {
            return UpgradeOrbTypeRegistry.FIRE_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, AttributeRegistry.ICE_SPELL_POWER.get())) {
            return UpgradeOrbTypeRegistry.ICE_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, AttributeRegistry.LIGHTNING_SPELL_POWER.get())) {
            return UpgradeOrbTypeRegistry.LIGHTNING_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, AttributeRegistry.HOLY_SPELL_POWER.get())) {
            return UpgradeOrbTypeRegistry.HOLY_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, AttributeRegistry.ENDER_SPELL_POWER.get())) {
            return UpgradeOrbTypeRegistry.ENDER_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, AttributeRegistry.BLOOD_SPELL_POWER.get())) {
            return UpgradeOrbTypeRegistry.BLOOD_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, AttributeRegistry.EVOCATION_SPELL_POWER.get())) {
            return UpgradeOrbTypeRegistry.EVOCATION_SPELL_POWER;
        }
        if (Objects.equals(spellPowerAttribute, AttributeRegistry.NATURE_SPELL_POWER.get())) {
            return UpgradeOrbTypeRegistry.NATURE_SPELL_POWER;
        }
        return null;
    }
}
