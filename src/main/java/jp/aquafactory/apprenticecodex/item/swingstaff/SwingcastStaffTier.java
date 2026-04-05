package jp.aquafactory.apprenticecodex.item.swingstaff;

import jp.aquafactory.apprenticecodex.item.SpellGunCastType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public record SwingcastStaffTier(
        Rarity rarity,
        int enchantmentValue,
        double attackDamageModifier,
        double attackSpeedModifier,
        List<BonusSpec> handBonuses,
        Set<SpellGunCastType> supportedCastTypes,
        @Nullable Integer maxImbueSpellCooldownTicks,
        boolean requireZeroRecast,
        SwingcastCooldownMode swingcastCooldownMode,
        @Nullable Integer fixedSwingcastCooldownTicks,
        boolean allowImbuedSpellInSpellWheel
) {
    public SwingcastStaffTier {
        Objects.requireNonNull(rarity);
        handBonuses = List.copyOf(Objects.requireNonNull(handBonuses));
        supportedCastTypes = Set.copyOf(Objects.requireNonNull(supportedCastTypes));
        Objects.requireNonNull(swingcastCooldownMode);
    }

    public static SwingcastStaffTier fromDisplayedWeaponStats(
            Rarity rarity,
            int enchantmentValue,
            double displayedAttackDamage,
            double displayedAttackSpeed,
            List<BonusSpec> handBonuses,
            Set<SpellGunCastType> supportedCastTypes,
            @Nullable Integer maxImbueSpellCooldownTicks,
            boolean requireZeroRecast,
            SwingcastCooldownMode swingcastCooldownMode,
            @Nullable Integer fixedSwingcastCooldownTicks,
            boolean allowImbuedSpellInSpellWheel
    ) {
        return new SwingcastStaffTier(
                rarity,
                enchantmentValue,
                displayedAttackDamage - 1.0D,
                displayedAttackSpeed - 4.0D,
                handBonuses,
                supportedCastTypes,
                maxImbueSpellCooldownTicks,
                requireZeroRecast,
                swingcastCooldownMode,
                fixedSwingcastCooldownTicks,
                allowImbuedSpellInSpellWheel
        );
    }

    public record BonusSpec(
            Supplier<? extends Attribute> attributeSupplier,
            double amount,
            AttributeModifier.Operation operation,
            @Nullable String key
    ) {
        public BonusSpec {
            Objects.requireNonNull(attributeSupplier);
            Objects.requireNonNull(operation);
        }
    }
}
