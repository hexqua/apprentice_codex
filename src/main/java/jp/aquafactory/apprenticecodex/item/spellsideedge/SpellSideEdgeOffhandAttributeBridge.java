package jp.aquafactory.apprenticecodex.item.spellsideedge;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SpellSideEdgeOffhandAttributeBridge {
    private static final String MODIFIER_NAME_PREFIX = "apprenticecodex.spell_side_edge.offhand_mainhand_attribute";
    private static final Map<UUID, AppliedBridgeModifiers> APPLIED_MODIFIERS = new HashMap<>();

    private SpellSideEdgeOffhandAttributeBridge() {
    }

    public static void sync(ServerPlayer player) {
        var desiredModifiers = resolveDesiredModifiers(player);
        var desiredSnapshot = snapshotModifiers(desiredModifiers);
        var playerId = player.getUUID();
        var appliedModifiers = APPLIED_MODIFIERS.get(playerId);
        if (appliedModifiers != null && appliedModifiers.snapshot().equals(desiredSnapshot)) {
            return;
        }

        removeModifiers(player, appliedModifiers == null ? null : appliedModifiers.modifiers());
        if (desiredModifiers.isEmpty()) {
            APPLIED_MODIFIERS.remove(playerId);
            return;
        }

        applyModifiers(player, desiredModifiers);
        APPLIED_MODIFIERS.put(playerId, new AppliedBridgeModifiers(desiredModifiers, desiredSnapshot));
    }

    public static void clear(Player player) {
        if (player == null) {
            return;
        }

        var appliedModifiers = APPLIED_MODIFIERS.remove(player.getUUID());
        removeModifiers(player, appliedModifiers == null ? null : appliedModifiers.modifiers());
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
            if (!isBridgeableAttribute(entry.getKey())) {
                continue;
            }

            var modifier = entry.getValue();
            var key = new ModifierKey(entry.getKey(), modifier.getOperation());
            switch (modifier.getOperation()) {
                case ADDITION, MULTIPLY_BASE -> mainComparableAmounts.merge(key, modifier.getAmount(), Double::sum);
                case MULTIPLY_TOTAL -> mainMultiplyTotalModifiers.add(entry);
            }
        }

        if (offhandModifiers != null && !offhandModifiers.isEmpty()) {
            for (var entry : offhandModifiers.entries()) {
                if (!isBridgeableAttribute(entry.getKey())) {
                    continue;
                }

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
        return ImmutableMultimap.copyOf(stack.getAttributeModifiers(slot));
    }

    private static boolean isBridgeableAttribute(Attribute attribute) {
        // 近接攻撃力と攻撃速度は利き手武器側の戦闘性能として扱い、魔法行使用のブリッジには乗せない。
        return attribute != Attributes.ATTACK_DAMAGE && attribute != Attributes.ATTACK_SPEED;
    }

    private static Set<ModifierSnapshot> snapshotModifiers(Multimap<Attribute, AttributeModifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return Set.of();
        }

        var amounts = new LinkedHashMap<ModifierKey, Double>();
        for (var entry : modifiers.entries()) {
            var modifier = entry.getValue();
            var key = new ModifierKey(entry.getKey(), modifier.getOperation());
            switch (modifier.getOperation()) {
                case ADDITION, MULTIPLY_BASE -> amounts.merge(key, modifier.getAmount(), Double::sum);
                case MULTIPLY_TOTAL -> amounts.merge(key, 1.0D + modifier.getAmount(), (left, right) -> left * right);
            }
        }

        var snapshots = new HashSet<ModifierSnapshot>();
        for (var entry : amounts.entrySet()) {
            var key = entry.getKey();
            var amount = key.operation() == AttributeModifier.Operation.MULTIPLY_TOTAL
                    ? entry.getValue() - 1.0D
                    : entry.getValue();
            snapshots.add(new ModifierSnapshot(
                    key.attribute(),
                    key.operation(),
                    Double.doubleToLongBits(normalizeAmount(amount))
            ));
        }
        return Set.copyOf(snapshots);
    }

    private static double normalizeAmount(double amount) {
        return amount == 0.0D ? 0.0D : amount;
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

    private record ModifierSnapshot(
            Attribute attribute,
            AttributeModifier.Operation operation,
            long amountBits
    ) {
    }

    private record AppliedBridgeModifiers(
            Multimap<Attribute, AttributeModifier> modifiers,
            Set<ModifierSnapshot> snapshot
    ) {
    }
}
