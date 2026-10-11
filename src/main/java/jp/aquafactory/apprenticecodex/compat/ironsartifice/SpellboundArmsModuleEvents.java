package jp.aquafactory.apprenticecodex.compat.ironsartifice;

import io.redspace.irons_artifice.api.ComposeShotEvent;
import io.redspace.irons_artifice.client.particle.ColorTransitionParticleOption;
import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.ValueModifier;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.damage.DamageSources;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellboundArmsModuleServerConfig.Values;
import jp.aquafactory.apprenticecodex.item.modifier.spellboundarmsmodule.SpellboundArmsModuleConfigState;
import jp.aquafactory.apprenticecodex.item.modifier.spellboundarmsmodule.SpellboundArmsModuleModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;

public final class SpellboundArmsModuleEvents {
    public static final ComponentType<Power> POWER = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellbound_arms_power"), () -> Power.NEUTRAL);
    public static final ComponentType<ValueModifier> DAMAGE_BOOST = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellbound_arms_damage_boost"),
            () -> new ValueModifier(0, ValueModifier.Operation.MULTIPLY_TOTAL, ValueModifier.Type.BENEFICIAL));

    private SpellboundArmsModuleEvents() {}

    public record Power(double multiplier, @Nullable SchoolType school) {
        public static final Power NEUTRAL = new Power(1, null);
    }

    public static Power power(@Nullable LivingEntity shooter) {
        if (shooter == null) return Power.NEUTRAL;
        double general = shooter.getAttributes().hasAttribute(AttributeRegistry.SPELL_POWER)
                ? shooter.getAttributeValue(AttributeRegistry.SPELL_POWER) : 1;
        SchoolType selected = null;
        double highest = 1;
        for (SchoolType school : SchoolRegistry.REGISTRY) {
            double value = school.getPowerFor(shooter);
            if (value > highest || (value == highest && value > 1
                    && school.getId().toString().compareTo(selected.getId().toString()) < 0)) {
                highest = value;
                selected = school;
            }
        }
        return new Power(general * highest, selected);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCompose(ComposeShotEvent event) {
        var profile = event.getShotProfile();
        int count = profile.peek(SpellboundArmsModuleModifier.MODULE_COUNT);
        if (count <= 0) return;
        var values = event.getEntity().level().isClientSide ? SpellboundArmsModuleConfigState.values()
                : ApprenticeCodexServerConfig.spellboundArmsModuleConfig();
        Power power = power(event.getEntity());
        profile.components().set(POWER, power);
        var boost = new ValueModifier(values.damageMultiplier(power.multiplier(), count) - 1,
                ValueModifier.Operation.MULTIPLY_TOTAL, ValueModifier.Type.BENEFICIAL);
        profile.components().set(DAMAGE_BOOST, boost);
        profile.modifyValue(ShotComponents.DAMAGE, boost);
        if (power.school() != null) {
            var color = power.school().getTargetingColor();
            int rgb = ((int) (color.x * 255) << 16) | ((int) (color.y * 255) << 8) | (int) (color.z * 255);
            // 空の候補だけが上流の初期色を使う。追加すれば初期色が消え、他moduleの候補は残る。
            profile.modify(ShotComponents.MUZZLE_FLASH, settings -> settings.addTint(color));
            profile.modify(ShotComponents.PARTICLE_TRAIL, particles -> particles.add(ColorTransitionParticleOption.bulletTrail(rgb, rgb)));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getSource().getDirectEntity() instanceof Bullet bullet)
                || bullet.getProfile().peek(SpellboundArmsModuleModifier.MODULE_COUNT) <= 0
                || IronsArtificeCompat.defersSpellResistance(event.getSource())) return;
        // SoulfireCoinはNORMALで未軽減の値から追加damageを生成する。各damageをLOWで一度だけ軽減する。
        event.setAmount(event.getAmount() * DamageSources.getResist(event.getEntity(), bullet.getProfile().peek(POWER).school()));
    }

    public static String number(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    public static void describe(Consumer<Component> builder, Power power, Values values) {
        String key = "item.apprenticecodex.spellbound_arms_module";
        builder.accept(Component.translatable(key + ".desc_1", number(values.damageMultiplier(power.multiplier(), 1) * 100),
                number((power.multiplier() - 1) * values.additionalModuleMultiplier() * 100)).withStyle(ChatFormatting.GREEN));
        builder.accept(Component.translatable(key + ".desc_2").withStyle(ChatFormatting.GREEN));
        builder.accept((power.school() == null ? Component.translatable(key + ".desc_3.no_school")
                : Component.translatable(key + ".desc_3", power.school().getDisplayName().copy())).withStyle(ChatFormatting.YELLOW));
        if (values.normalManaCostMultiplier() > 0 || values.magicManaCostMultiplier() > 0)
            builder.accept(Component.translatable(key + ".desc_4").withStyle(ChatFormatting.RED));
    }
}
