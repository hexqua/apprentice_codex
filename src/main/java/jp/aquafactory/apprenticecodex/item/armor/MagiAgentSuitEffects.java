package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.EquipmentSpellTimingConfigState;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.IMagiAgentSuitAffectedSpell;
import jp.aquafactory.apprenticecodex.spell.divinepossession.DivinePossessionPowerHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public final class MagiAgentSuitEffects {
    private static final List<Supplier<? extends AbstractSpell>> TARGET_SPELLS = List.of(
            SpellRegistry.COMMENCE_FIRE,
            SpellRegistry.QUICK_ARMS,
            SpellRegistry.BREACHING_ENEMY,
            SpellRegistry.BULLET_STREAM,
            SpellRegistry.FLY_SWATTER,
            SpellRegistry.THERMAL_PROCESS,
            SpellRegistry.SILENT_ASSASSIN,
            SpellRegistry.TIRO_VOLLEY,
            SpellRegistry.LETHAL_ASSAULT,
            SpellRegistry.DUAL_ACROBAT,
            SpellRegistry.ARTISAN_SMASH
    );

    private MagiAgentSuitEffects() {
    }

    public static List<AbstractSpell> targetSpells() {
        return TARGET_SPELLS.stream()
                .<AbstractSpell>map(Supplier::get)
                .toList();
    }

    public static boolean isTargetSpell(@Nullable AbstractSpell spell) {
        return spell instanceof IMagiAgentSuitAffectedSpell;
    }

    public static boolean canBeInterrupted(
            AbstractSpell spell,
            @Nullable Player player,
            boolean originalCanBeInterrupted
    ) {
        if (!originalCanBeInterrupted || player == null) {
            return originalCanBeInterrupted;
        }

        if (spell.getCastType() != CastType.LONG || !isTargetSpell(spell)) {
            return originalCanBeInterrupted;
        }

        return !isWearingSuitPiece(player, ArmorItem.Type.LEGGINGS);
    }

    public static boolean shouldSkipAmmoConsumption(ServerPlayer player) {
        var chance = ApprenticeCodexServerConfig.magiAgentSuitAmmoNoConsumeChance();
        if (chance <= 0.0D || !isWearingSuitPiece(player, ArmorItem.Type.HELMET)) {
            return false;
        }
        return chance >= 1.0D || player.getRandom().nextDouble() < chance;
    }

    public static boolean shouldSkipStaffrifleManaCostWithAmmo() {
        return ApprenticeCodexServerConfig.magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed();
    }

    public static double resolveSchoolPower(AbstractSpell spell, SchoolType originalSchool, @Nullable LivingEntity caster) {
        if (caster == null) {
            return originalSchool.getPowerFor(null);
        }

        var effectiveSchool = resolveSchoolOverride(spell, originalSchool, caster);
        return DivinePossessionPowerHelper.resolveSchoolPower(effectiveSchool, caster);
    }

    public static int applyBootsCooldownDiscount(int baseCooldown, AbstractSpell spell, Player player) {
        if (baseCooldown <= 0 || !isTargetSpell(spell) || !isWearingSuitPiece(player, ArmorItem.Type.BOOTS)) {
            return baseCooldown;
        }
        // 実行中の SERVER config リロード後も予測結果を一致させるため、クライアントでは同期値を使う。
        var cooldownMultiplier = player.level().isClientSide
                ? EquipmentSpellTimingConfigState.magiAgentSuitBootsCooldownMultiplier()
                : ApprenticeCodexServerConfig.magiAgentSuitBootsCooldownMultiplier();
        return WeaponImbueCooldownHelper.applyLimitedCooldownMultiplier(
                baseCooldown,
                cooldownMultiplier
        );
    }

    public static int applyBootsCastTimeReduction(AbstractSpell spell, int effectiveCastTime, @Nullable LivingEntity entity) {
        if (effectiveCastTime <= 0 || entity == null || spell.getCastType() != CastType.LONG
                || !isTargetSpell(spell) || !isWearingSuitPiece(entity, ArmorItem.Type.BOOTS)) {
            return effectiveCastTime;
        }
        var castTimeMultiplier = entity.level().isClientSide
                ? EquipmentSpellTimingConfigState.magiAgentSuitBootsCastTimeMultiplier()
                : ApprenticeCodexServerConfig.magiAgentSuitBootsCastTimeMultiplier();
        return Math.max(1, (int) Math.round(
                effectiveCastTime * castTimeMultiplier
        ));
    }

    public static int applyBootsCommenceFireRecastCastTime(AbstractSpell spell, int effectiveCastTime, LivingEntity entity) {
        if (!isTargetSpell(spell) || !isWearingSuitPiece(entity, ArmorItem.Type.BOOTS)) {
            return effectiveCastTime;
        }
        return 0;
    }

    public static boolean shouldCancelCastingMovePenalty(
            LivingEntity entity,
            @Nullable AbstractSpell spell,
            CastType castType
    ) {
        if (!isTargetSpell(spell) || (castType != CastType.LONG && castType != CastType.CONTINUOUS)
                || !isWearingSuitPiece(entity, ArmorItem.Type.LEGGINGS)) {
            return false;
        }

        var castingMoveSpeed = entity.getAttribute(AttributeRegistry.CASTING_MOVESPEED.get());
        if (castingMoveSpeed == null) {
            return true;
        }

        var ironsInputMultiplier = 0.2D + castingMoveSpeed.getValue() - 1.0D;
        return ironsInputMultiplier < 1.0D;
    }

    public static boolean isWearingSuitPiece(LivingEntity entity, ArmorItem.Type armorType) {
        var stack = entity.getItemBySlot(armorType.getSlot());
        return isSuitPiece(stack, armorType);
    }

    public static boolean isSuitPiece(ItemStack stack, ArmorItem.Type armorType) {
        return !stack.isEmpty()
                && stack.getItem() instanceof MagiAgentSuitItem suitItem
                && suitItem.getArmorType() == armorType;
    }

    private static SchoolType resolveSchoolOverride(AbstractSpell spell, SchoolType originalSchool, LivingEntity caster) {
        if (!isTargetSpell(spell)) {
            return originalSchool;
        }

        var chestStack = caster.getItemBySlot(EquipmentSlot.CHEST);
        if (!isSuitPiece(chestStack, ArmorItem.Type.CHESTPLATE)) {
            return originalSchool;
        }

        var school = MagiAgentSuitItem.getResolvedCalibrationSchool(chestStack);
        return school == null ? originalSchool : school;
    }
}
