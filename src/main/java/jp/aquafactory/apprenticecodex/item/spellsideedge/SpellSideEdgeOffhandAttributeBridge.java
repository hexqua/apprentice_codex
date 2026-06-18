package jp.aquafactory.apprenticecodex.item.spellsideedge;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class SpellSideEdgeOffhandAttributeBridge {
    private static final String MODIFIER_NAME_PREFIX = "apprenticecodex.spell_side_edge.offhand_mainhand_attribute";
    private static final Map<UUID, Multimap<Attribute, AttributeModifier>> APPLIED_MODIFIERS = new HashMap<>();

    private SpellSideEdgeOffhandAttributeBridge() {
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

    public static Multimap<Attribute, AttributeModifier> buildBridgeModifiers(ItemStack offhandStack) {
        if (offhandStack == null || offhandStack.isEmpty()) {
            return ImmutableMultimap.of();
        }

        return buildBridgeModifiers(
                resolveRuntimeModifiers(offhandStack, EquipmentSlot.MAINHAND),
                resolveRuntimeModifiers(offhandStack, EquipmentSlot.OFFHAND)
        );
    }

    public static Multimap<Attribute, AttributeModifier> buildBridgeModifiers(
            Multimap<Attribute, AttributeModifier> mainhandModifiers,
            Multimap<Attribute, AttributeModifier> offhandModifiers
    ) {
        if (mainhandModifiers == null || mainhandModifiers.isEmpty()) {
            return ImmutableMultimap.of();
        }

        var mainComparableAmounts = new LinkedHashMap<ModifierKey, Double>();
        var offhandComparableAmounts = new LinkedHashMap<ModifierKey, Double>();
        var mainMultiplyTotalModifiers = new ArrayList<Map.Entry<Attribute, AttributeModifier>>();
        var offhandMultiplyTotalKeys = new HashSet<ModifierKey>();

        for (var entry : mainhandModifiers.entries()) {
            var modifier = entry.getValue();
            var key = new ModifierKey(entry.getKey(), modifier.getOperation());
            switch (modifier.getOperation()) {
                case ADDITION, MULTIPLY_BASE -> mainComparableAmounts.merge(key, modifier.getAmount(), Double::sum);
                case MULTIPLY_TOTAL -> mainMultiplyTotalModifiers.add(entry);
            }
        }

        if (offhandModifiers != null && !offhandModifiers.isEmpty()) {
            for (var entry : offhandModifiers.entries()) {
                var modifier = entry.getValue();
                var key = new ModifierKey(entry.getKey(), modifier.getOperation());
                switch (modifier.getOperation()) {
                    case ADDITION, MULTIPLY_BASE -> offhandComparableAmounts.merge(key, modifier.getAmount(), Double::sum);
                    case MULTIPLY_TOTAL -> offhandMultiplyTotalKeys.add(key);
                }
            }
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        for (var entry : mainComparableAmounts.entrySet()) {
            var key = entry.getKey();
            var delta = entry.getValue() - offhandComparableAmounts.getOrDefault(key, 0.0D);
            if (delta <= 0.0D) {
                continue;
            }

            var modifierIdSeed = MODIFIER_NAME_PREFIX
                    + ".delta."
                    + resolveAttributeToken(key.attribute())
                    + "."
                    + key.operation().name().toLowerCase(Locale.ROOT);
            builder.put(
                    key.attribute(),
                    new AttributeModifier(
                            UUID.nameUUIDFromBytes(modifierIdSeed.getBytes(StandardCharsets.UTF_8)),
                            modifierIdSeed,
                            delta,
                            key.operation()
                    )
            );
        }

        for (var entry : mainMultiplyTotalModifiers) {
            var key = new ModifierKey(entry.getKey(), entry.getValue().getOperation());
            if (offhandMultiplyTotalKeys.contains(key)) {
                continue;
            }

            builder.put(entry.getKey(), remapModifier(entry.getKey(), entry.getValue()));
        }
        return builder.build();
    }

    private static Multimap<Attribute, AttributeModifier> resolveDesiredModifiers(ServerPlayer player) {
        if (!player.isAlive() || !SpellSideEdge.isSpellSideEdge(player.getMainHandItem())) {
            return ImmutableMultimap.of();
        }

        return buildBridgeModifiers(player.getOffhandItem());
    }

    private static Multimap<Attribute, AttributeModifier> resolveRuntimeModifiers(ItemStack stack, EquipmentSlot slot) {
        var event = new ItemAttributeModifierEvent(
                stack,
                slot,
                stack.getAttributeModifiers(slot)
        );
        MinecraftForge.EVENT_BUS.post(event);
        return ImmutableMultimap.copyOf(event.getModifiers());
    }

    private static AttributeModifier remapModifier(Attribute attribute, AttributeModifier modifier) {
        var modifierIdSeed = MODIFIER_NAME_PREFIX
                + ".copy."
                + resolveAttributeToken(attribute)
                + "."
                + modifier.getOperation().name().toLowerCase(Locale.ROOT)
                + "."
                + modifier.getId();
        return new AttributeModifier(
                UUID.nameUUIDFromBytes(modifierIdSeed.getBytes(StandardCharsets.UTF_8)),
                modifierIdSeed,
                modifier.getAmount(),
                modifier.getOperation()
        );
    }

    private static void applyModifiers(Player player, Multimap<Attribute, AttributeModifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return;
        }

        for (var entry : modifiers.entries()) {
            var attributeInstance = player.getAttribute(entry.getKey());
            if (attributeInstance == null) {
                continue;
            }

            attributeInstance.removeModifier(entry.getValue().getId());
            attributeInstance.addTransientModifier(entry.getValue());
        }
    }

    private static void removeModifiers(Player player, Multimap<Attribute, AttributeModifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return;
        }

        for (var entry : modifiers.entries()) {
            var attributeInstance = player.getAttribute(entry.getKey());
            if (attributeInstance == null) {
                continue;
            }

            removeModifier(attributeInstance, entry.getValue().getId());
        }
    }

    private static void removeModifier(AttributeInstance attributeInstance, UUID modifierId) {
        attributeInstance.removeModifier(modifierId);
    }

    private static String resolveAttributeToken(Attribute attribute) {
        var registryKey = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        if (registryKey == null) {
            return "unknown";
        }
        return normalizeToken(registryKey.toString());
    }

    private static String normalizeToken(String token) {
        return token.toLowerCase(Locale.ROOT)
                .replace(':', '.')
                .replace('/', '.')
                .replaceAll("[^a-z0-9._-]", "_");
    }

    private record ModifierKey(
            Attribute attribute,
            AttributeModifier.Operation operation
    ) {
    }
}
