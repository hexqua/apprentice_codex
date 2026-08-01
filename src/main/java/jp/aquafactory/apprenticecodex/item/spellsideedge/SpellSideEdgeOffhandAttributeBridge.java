package jp.aquafactory.apprenticecodex.item.spellsideedge;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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

    public static Multimap<Holder<Attribute>, AttributeModifier> buildBridgeModifiers(ItemStack offhandStack) {
        if (offhandStack == null || offhandStack.isEmpty()) {
            return ImmutableMultimap.of();
        }

        return buildBridgeModifiers(
                resolveRuntimeModifiers(offhandStack, net.minecraft.world.entity.EquipmentSlot.MAINHAND),
                resolveRuntimeModifiers(offhandStack, net.minecraft.world.entity.EquipmentSlot.OFFHAND)
        );
    }

    public static Multimap<Holder<Attribute>, AttributeModifier> buildBridgeModifiers(
            Multimap<Holder<Attribute>, AttributeModifier> mainhandModifiers,
            Multimap<Holder<Attribute>, AttributeModifier> offhandModifiers
    ) {
        if (mainhandModifiers == null || mainhandModifiers.isEmpty()) {
            return ImmutableMultimap.of();
        }

        var mainComparableAmounts = new LinkedHashMap<ModifierKey, Double>();
        var offhandComparableAmounts = new LinkedHashMap<ModifierKey, Double>();
        var mainMultiplyTotalModifiers = new ArrayList<Map.Entry<Holder<Attribute>, AttributeModifier>>();
        var offhandMultiplyTotalKeys = new HashSet<ModifierKey>();

        for (var entry : mainhandModifiers.entries()) {
            if (!isBridgeableAttribute(entry.getKey())) {
                continue;
            }

            var modifier = entry.getValue();
            var key = new ModifierKey(entry.getKey(), modifier.operation());
            switch (modifier.operation()) {
                case ADD_VALUE, ADD_MULTIPLIED_BASE -> mainComparableAmounts.merge(key, modifier.amount(), Double::sum);
                case ADD_MULTIPLIED_TOTAL -> mainMultiplyTotalModifiers.add(entry);
            }
        }

        if (offhandModifiers != null && !offhandModifiers.isEmpty()) {
            for (var entry : offhandModifiers.entries()) {
                if (!isBridgeableAttribute(entry.getKey())) {
                    continue;
                }

                var modifier = entry.getValue();
                var key = new ModifierKey(entry.getKey(), modifier.operation());
                switch (modifier.operation()) {
                    case ADD_VALUE, ADD_MULTIPLIED_BASE -> offhandComparableAmounts.merge(key, modifier.amount(), Double::sum);
                    case ADD_MULTIPLIED_TOTAL -> offhandMultiplyTotalKeys.add(key);
                }
            }
        }

        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
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
                            ResourceLocationHelper.fromStableId(modifierIdSeed),
                            delta,
                            key.operation()
                    )
            );
        }

        for (var entry : mainMultiplyTotalModifiers) {
            var key = new ModifierKey(entry.getKey(), entry.getValue().operation());
            if (offhandMultiplyTotalKeys.contains(key)) {
                continue;
            }

            builder.put(entry.getKey(), remapModifier(entry.getKey(), entry.getValue()));
        }
        return builder.build();
    }

    private static Multimap<Holder<Attribute>, AttributeModifier> resolveDesiredModifiers(ServerPlayer player) {
        if (!player.isAlive() || !SpellSideEdge.isSpellSideEdge(player.getMainHandItem())) {
            return ImmutableMultimap.of();
        }

        return buildBridgeModifiers(player.getOffhandItem());
    }

    private static Multimap<Holder<Attribute>, AttributeModifier> resolveRuntimeModifiers(
            ItemStack stack,
            net.minecraft.world.entity.EquipmentSlot slot
    ) {
        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        // 1.21.1のAttributeエンチャントはDataComponent本体ではなく、この列挙時に合成される。
        stack.forEachModifier(slot, builder::put);
        return builder.build();
    }

    private static boolean isBridgeableAttribute(Holder<Attribute> attribute) {
        // 近接攻撃力と攻撃速度は利き手武器側の戦闘性能として扱い、魔法行使用のブリッジには乗せない。
        return !attribute.equals(Attributes.ATTACK_DAMAGE) && !attribute.equals(Attributes.ATTACK_SPEED);
    }

    private static Set<ModifierSnapshot> snapshotModifiers(Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return Set.of();
        }

        var amounts = new LinkedHashMap<ModifierKey, Double>();
        for (var entry : modifiers.entries()) {
            var modifier = entry.getValue();
            var key = new ModifierKey(entry.getKey(), modifier.operation());
            switch (modifier.operation()) {
                case ADD_VALUE, ADD_MULTIPLIED_BASE -> amounts.merge(key, modifier.amount(), Double::sum);
                case ADD_MULTIPLIED_TOTAL -> amounts.merge(key, 1.0D + modifier.amount(), (left, right) -> left * right);
            }
        }

        var snapshots = new HashSet<ModifierSnapshot>();
        for (var entry : amounts.entrySet()) {
            var key = entry.getKey();
            var amount = key.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
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

    private static AttributeModifier remapModifier(Holder<Attribute> attribute, AttributeModifier modifier) {
        var modifierIdSeed = MODIFIER_NAME_PREFIX
                + ".copy."
                + resolveAttributeToken(attribute)
                + "."
                + modifier.operation().name().toLowerCase(Locale.ROOT)
                + "."
                + modifier.id();
        return new AttributeModifier(
                ResourceLocationHelper.fromStableId(modifierIdSeed),
                modifier.amount(),
                modifier.operation()
        );
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

    private static void removeModifier(AttributeInstance attributeInstance, net.minecraft.resources.ResourceLocation modifierId) {
        attributeInstance.removeModifier(modifierId);
    }

    private static String resolveAttributeToken(Holder<Attribute> attribute) {
        return attribute.unwrapKey()
                .map(key -> normalizeToken(key.location().toString()))
                .orElseGet(() -> normalizeToken(BuiltInRegistries.ATTRIBUTE.getKey(attribute.value()).toString()));
    }

    private static String normalizeToken(String token) {
        return token.toLowerCase(Locale.ROOT)
                .replace(':', '.')
                .replace('/', '.')
                .replaceAll("[^a-z0-9._-]", "_");
    }

    private record ModifierKey(
            Holder<Attribute> attribute,
            AttributeModifier.Operation operation
    ) {
    }

    private record ModifierSnapshot(
            Holder<Attribute> attribute,
            AttributeModifier.Operation operation,
            long amountBits
    ) {
    }

    private record AppliedBridgeModifiers(
            Multimap<Holder<Attribute>, AttributeModifier> modifiers,
            Set<ModifierSnapshot> snapshot
    ) {
    }

    private static final class ResourceLocationHelper {
        private ResourceLocationHelper() {
        }

        private static net.minecraft.resources.ResourceLocation fromStableId(String value) {
            return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    "apprenticecodex",
                    normalizeToken(value)
            );
        }
    }
}
