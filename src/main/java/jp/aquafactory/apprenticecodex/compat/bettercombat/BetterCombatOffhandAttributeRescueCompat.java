package jp.aquafactory.apprenticecodex.compat.bettercombat;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class BetterCombatOffhandAttributeRescueCompat {
    private static final String RESCUE_ID_PREFIX = "bettercombat_offhand_rescue";
    private static final Map<java.util.UUID, Multimap<Holder<Attribute>, AttributeModifier>> APPLIED_MODIFIERS = new HashMap<>();

    private BetterCombatOffhandAttributeRescueCompat() {
    }

    public static void sync(ServerPlayer player) {
        var desiredModifiers = resolveDesiredModifiers(player);
        var playerId = player.getUUID();
        var appliedModifiers = APPLIED_MODIFIERS.get(playerId);
        if (Objects.equals(appliedModifiers, desiredModifiers)) {
            return;
        }

        removeModifiers(player, appliedModifiers);
        if (desiredModifiers.isEmpty()) {
            APPLIED_MODIFIERS.remove(playerId);
            return;
        }

        applyModifiers(player, desiredModifiers);
        APPLIED_MODIFIERS.put(playerId, desiredModifiers);
    }

    public static void clear(Player player) {
        if (player == null) {
            return;
        }

        removeModifiers(player, APPLIED_MODIFIERS.remove(player.getUUID()));
    }

    public static boolean isRescueActive(Player player) {
        if (player == null || !player.isAlive()) {
            return false;
        }

        if (!isTwoHandedMainHandWeapon(player.getMainHandItem())) {
            return false;
        }

        return allowsRescue(getPhysicalOffhandStack(player));
    }

    public static ItemStack getPhysicalOffhandStack(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }

        NonNullList<ItemStack> offhandInventory = player.getInventory().offhand;
        return offhandInventory.isEmpty() ? ItemStack.EMPTY : offhandInventory.getFirst();
    }

    public static Multimap<Holder<Attribute>, AttributeModifier> buildRescueModifiers(ItemStack offhandStack) {
        if (offhandStack.isEmpty()) {
            return ImmutableMultimap.of();
        }

        return remapForRescue(offhandStack.getAttributeModifiers().modifiers());
    }

    private static Multimap<Holder<Attribute>, AttributeModifier> resolveDesiredModifiers(ServerPlayer player) {
        if (!isRescueActive(player)) {
            return ImmutableMultimap.of();
        }

        return buildRescueModifiers(getPhysicalOffhandStack(player));
    }

    private static boolean isTwoHandedMainHandWeapon(ItemStack mainHandStack) {
        if (mainHandStack.isEmpty()) {
            return false;
        }

        var weaponAttributes = WeaponRegistry.getAttributes(mainHandStack);
        return weaponAttributes != null && weaponAttributes.isTwoHanded();
    }

    private static boolean allowsRescue(ItemStack offhandStack) {
        return offhandStack.getItem() instanceof AbstractOffhandMagicItem offhandMagicItem
                && offhandMagicItem.allowsBetterCombatOffhandRescue(offhandStack);
    }

    private static Multimap<Holder<Attribute>, AttributeModifier> remapForRescue(
            Iterable<ItemAttributeModifiers.Entry> sourceModifiers
    ) {
        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        boolean foundModifier = false;
        for (var entry : sourceModifiers) {
            foundModifier = true;
            var attribute = entry.attribute();
            var modifier = entry.modifier();
            var attributeToken = attribute.unwrapKey()
                    .map(resourceKey -> normalizeToken(resourceKey.location().toString()))
                    .orElse("unknown_attribute");
            var modifierId = ResourceLocation.fromNamespaceAndPath(
                    ApprenticeCodex.MODID,
                    RESCUE_ID_PREFIX + "/"
                            + attributeToken + "/"
                            + normalizeToken(modifier.id().toString())
            );
            builder.put(attribute, new AttributeModifier(modifierId, modifier.amount(), modifier.operation()));
        }
        return foundModifier ? builder.build() : ImmutableMultimap.of();
    }

    private static void applyModifiers(Player player, Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return;
        }

        for (var entry : modifiers.entries()) {
            var attributeInstance = player.getAttribute(entry.getKey());
            if (attributeInstance == null) {
                continue;
            }

            attributeInstance.removeModifier(entry.getValue().id());
            attributeInstance.addTransientModifier(entry.getValue());
        }
    }

    private static void removeModifiers(Player player, Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return;
        }

        for (var entry : modifiers.entries()) {
            var attributeInstance = player.getAttribute(entry.getKey());
            if (attributeInstance == null) {
                continue;
            }

            removeModifier(attributeInstance, entry.getValue().id());
        }
    }

    private static void removeModifier(AttributeInstance attributeInstance, ResourceLocation modifierId) {
        attributeInstance.removeModifier(modifierId);
    }

    private static String normalizeToken(String token) {
        return token.toLowerCase(Locale.ROOT)
                .replace(':', '_')
                .replace('/', '_')
                .replaceAll("[^a-z0-9._-]", "_");
    }
}
