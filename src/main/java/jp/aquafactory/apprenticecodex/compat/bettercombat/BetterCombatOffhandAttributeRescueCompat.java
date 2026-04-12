package jp.aquafactory.apprenticecodex.compat.bettercombat;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class BetterCombatOffhandAttributeRescueCompat {
    private static final String RESCUE_NAME_PREFIX = "apprenticecodex.bettercombat_offhand_rescue";
    private static final Map<UUID, Multimap<Attribute, AttributeModifier>> APPLIED_MODIFIERS = new HashMap<>();

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

    private static Multimap<Attribute, AttributeModifier> resolveDesiredModifiers(ServerPlayer player) {
        if (!isRescueActive(player)) {
            return ImmutableMultimap.of();
        }

        return buildRescueModifiers(getPhysicalOffhandStack(player));
    }

    public static boolean isRescueActive(Player player) {
        if (!player.isAlive()) {
            return false;
        }

        if (!isTwoHandedMainHandWeapon(player.getMainHandItem())) {
            return false;
        }

        return allowsRescue(getPhysicalOffhandStack(player));
    }

    public static Multimap<Attribute, AttributeModifier> buildRescueModifiers(ItemStack offhandStack) {
        return remapForRescue(resolveRuntimeOffhandModifiers(offhandStack));
    }

    public static ItemStack getPhysicalOffhandStack(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }

        // Better Combat 1.20.1 は両手武器中に OFFHAND 装備参照を空へ差し替えるため、
        // getOffhandItem() ではなく実インベントリの offhand スロットを直読みする。
        return player.getInventory().offhand.isEmpty()
                ? ItemStack.EMPTY
                : player.getInventory().offhand.get(0);
    }

    private static boolean isTwoHandedMainHandWeapon(ItemStack mainHandStack) {
        if (mainHandStack.isEmpty()) {
            return false;
        }

        var weaponAttributes = WeaponRegistry.getAttributes(mainHandStack);
        return weaponAttributes != null && weaponAttributes.isTwoHanded();
    }

    private static boolean allowsRescue(ItemStack offhandStack) {
        // Better Combat が壊しているのは「両手武器中の OFFHAND 装備参照」なので、
        // 救済可否の責務は武器タグではなく offhand 専用品側へ寄せる。
        return offhandStack.getItem() instanceof AbstractOffhandMagicItem offhandMagicItem
                && offhandMagicItem.allowsBetterCombatOffhandRescue(offhandStack);
    }

    private static Multimap<Attribute, AttributeModifier> resolveRuntimeOffhandModifiers(ItemStack offhandStack) {
        if (offhandStack.isEmpty()) {
            return ImmutableMultimap.of();
        }

        var event = new ItemAttributeModifierEvent(
                offhandStack,
                EquipmentSlot.OFFHAND,
                offhandStack.getItem().getAttributeModifiers(EquipmentSlot.OFFHAND, offhandStack)
        );
        MinecraftForge.EVENT_BUS.post(event);
        return ImmutableMultimap.copyOf(event.getModifiers());
    }

    private static Multimap<Attribute, AttributeModifier> remapForRescue(
            Multimap<Attribute, AttributeModifier> sourceModifiers
    ) {
        if (sourceModifiers.isEmpty()) {
            return ImmutableMultimap.of();
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        for (var entry : sourceModifiers.entries()) {
            var attribute = entry.getKey();
            var modifier = entry.getValue();
            var attributeKey = ForgeRegistries.ATTRIBUTES.getKey(attribute);
            var modifierSeed = RESCUE_NAME_PREFIX
                    + "."
                    + normalizeToken(attributeKey == null ? "unknown" : attributeKey.toString())
                    + "."
                    + modifier.getId();
            var modifierId = UUID.nameUUIDFromBytes(modifierSeed.getBytes(StandardCharsets.UTF_8));
            builder.put(
                    attribute,
                    new AttributeModifier(
                            modifierId,
                            modifierSeed,
                            modifier.getAmount(),
                            modifier.getOperation()
                    )
            );
        }
        return builder.build();
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

            // Better Combat 1.20.1 は両手武器中に OFFHAND 装備参照を空へ差し替えるため、
            // offhand 専用品が明示的に許可した場合だけ、最終値だけを transient modifier で補う。
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

    private static String normalizeToken(String token) {
        return token.toLowerCase(Locale.ROOT)
                .replace(':', '.')
                .replace('/', '.')
                .replaceAll("[^a-z0-9._-]", "_");
    }
}
