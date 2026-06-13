package jp.aquafactory.apprenticecodex.spell.manaslash;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastStaffCastContext;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

import java.util.List;
import java.util.Optional;

public class ManaSlash extends AbstractSpell {
    private static final String DAMAGE_MULTIPLIER_KEY =
            "ui.apprenticecodex.referenced_weapon_damage_multiplier";
    private static final String MISSING_CATALYST_KEY =
            "ui.apprenticecodex.mana_slash.missing_catalyst";

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mana_slash");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(0)
            .setAllowCrafting(false)
            .build();

    public ManaSlash() {
        baseSpellPower = 75;
        spellPowerPerLevel = 50;
        baseManaCost = 30;
        manaCostPerLevel = 0;
        castTime = 0;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        DAMAGE_MULTIPLIER_KEY,
                        Utils.stringTruncation(getReferencedWeaponDamagePercent(spellLevel, caster), 1)
                )
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity, ItemStack catalystStack) {
        // ベースのマナパワーが低いので武器ダメージより下がるのは意図的.
        var rawDamage = Math.max(
                1,
                resolveCatalystWeaponDamage(entity, catalystStack, MobType.UNDEFINED)
                        * getSpellPowerPercent(spellLevel, entity) / 100.0f
        );
        return rawDamage * getDamageMultiplier();
    }

    private float getReferencedWeaponDamagePercent(int spellLevel, LivingEntity entity) {
        return getSpellPowerPercent(spellLevel, entity) * getDamageMultiplier();
    }

    private float getSpellPowerPercent(int spellLevel, LivingEntity entity) {
        return getSpellPower(spellLevel, entity);
    }

    private float getDamageMultiplier() {
        return ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.MANA_SLASH);
    }

    public static float resolveCatalystWeaponDamage(LivingEntity entity, ItemStack catalystStack, MobType mobType) {
        if (entity == null || catalystStack == null || catalystStack.isEmpty()) {
            return 0.0f;
        }

        var baseDamage = entity.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        var displayDamage = resolveDisplayedAttributeValue(
                catalystStack,
                EquipmentSlotGroup.MAINHAND,
                Attributes.ATTACK_DAMAGE,
                baseDamage
        );
        if (displayDamage <= baseDamage) {
            displayDamage -= baseDamage;
        }

        return (float) displayDamage;
    }

    private static double resolveDisplayedAttributeValue(
            ItemStack stack,
            EquipmentSlotGroup slotGroup,
            Holder<Attribute> attribute,
            double baseValue
    ) {
        var event = new ItemAttributeModifierEvent(stack, stack.getItem().getDefaultAttributeModifiers(stack));
        NeoForge.EVENT_BUS.post(event);
        return resolveAttributeValue(event.build(), slotGroup, attribute, baseValue);
    }

    private static double resolveAttributeValue(
            ItemAttributeModifiers modifiers,
            EquipmentSlotGroup slotGroup,
            Holder<Attribute> attribute,
            double baseValue
    ) {
        var valueWithAdditions = baseValue;
        for (var entry : modifiers.modifiers()) {
            if (matches(entry, slotGroup, attribute)
                    && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                valueWithAdditions += entry.modifier().amount();
            }
        }

        var valueWithBaseMultipliers = valueWithAdditions;
        for (var entry : modifiers.modifiers()) {
            if (matches(entry, slotGroup, attribute)
                    && entry.modifier().operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                valueWithBaseMultipliers += valueWithAdditions * entry.modifier().amount();
            }
        }

        var valueWithTotalMultipliers = valueWithBaseMultipliers;
        for (var entry : modifiers.modifiers()) {
            if (matches(entry, slotGroup, attribute)
                    && entry.modifier().operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                valueWithTotalMultipliers *= 1.0d + entry.modifier().amount();
            }
        }

        return valueWithTotalMultipliers;
    }

    private static boolean matches(
            ItemAttributeModifiers.Entry entry,
            EquipmentSlotGroup slotGroup,
            Holder<Attribute> attribute
    ) {
        return entry.slot().equals(slotGroup) && entry.attribute().equals(attribute);
    }

    private Optional<ItemStack> resolveCatalystStack(LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        return SwingcastStaffCastContext.getCastingStack(entity.getUUID(), this)
                .filter(stack -> !stack.isEmpty());
    }

    private boolean hasSwingcastContext(LivingEntity entity) {
        return entity != null && SwingcastStaffCastContext.matches(entity.getUUID(), getSpellId());
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return config;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.MANA_SLASH.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.pass();
    }

    @Override
    public CastResult canBeCastedBy(int spellLevel, CastSource castSource, MagicData playerMagicData, Player player) {
        if (hasSwingcastContext(player) && resolveCatalystStack(player).isEmpty()) {
            return new CastResult(
                    CastResult.Type.FAILURE,
                    Component.translatable(MISSING_CATALYST_KEY).withStyle(ChatFormatting.RED)
            );
        }

        return super.canBeCastedBy(spellLevel, castSource, playerMagicData, player);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var catalystStack = resolveCatalystStack(entity);
        var projectile = new ManaSlashProjectileEntity(EntityRegistry.MANA_SLASH_PROJECTILE.get(), level, entity);
        projectile.setPos(entity.getEyePosition().add(entity.getLookAngle().scale(0.35d)));
        projectile.shoot(entity.getLookAngle());
        projectile.setDamage(catalystStack
                .map(stack -> getDamage(spellLevel, entity, stack))
                .orElseGet(() -> {
                    var rawDamage = Math.max(
                            1,
                            Utils.getWeaponDamage(entity, MobType.UNDEFINED)
                                    * getSpellPowerPercent(spellLevel, entity) / 100.0f
                    );
                    return rawDamage * getDamageMultiplier();
                }));
        level.addFreshEntity(projectile);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
