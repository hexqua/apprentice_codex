package jp.aquafactory.apprenticecodex.item.curios.manashieldcharm;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.ManaShieldCharmState;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.forcefield.ForceFieldDefenseEvent;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.function.Consumer;

final class ManaShieldCharmLogic {
    private static final float MANA_PER_DAMAGE = 25.0F;
    private static final float SYNCHRONIZATION_MANA_PER_DAMAGE = 30.0F;
    private static final float DAMAGE_STEP = 1.0F;
    private static final float RECOVERY_THRESHOLD_MANA = 100.0F;
    private static final int VANILLA_INVULNERABLE_TIME_TICKS = 20;
    private static final int VANILLA_COOLDOWN_DAMAGE_GATE_TICKS = VANILLA_INVULNERABLE_TIME_TICKS / 2;

    private ManaShieldCharmLogic() {
    }

    static void onCurioTick(SlotContext slotContext) {
        var entity = slotContext.entity();
        if (entity.level().isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }

        if (!isPrimaryEquippedCurio(slotContext)) {
            return;
        }

        refreshCooldownIfRecovered(player);
    }

    static void onLivingAttack(LivingIncomingDamageEvent event, ServerPlayer player) {
        if (event.isCanceled()) {
            return;
        }
        if (event.getAmount() <= 0.0F || !player.isAlive()) {
            return;
        }
        if (player.getAbilities().invulnerable) {
            return;
        }
        if (!isEquippedBy(player)) {
            return;
        }
        var charmStack = getEquippedCharm(player);
        if (charmStack.isEmpty()) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        refreshCooldownIfRecovered(player);

        var state = getState(player);
        if (state == null || state.manualReentryGuard) {
            return;
        }
        if (shouldIgnoreDuringVanillaStyleIFrame(player, event)) {
            event.setCanceled(true);
            return;
        }

        if (event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)
                && getExclusiveEnchantmentLevel(charmStack, EnchantmentRegistry.NEUTRALIZATION) > 0) {
            handleNeutralization(event, player, magicData);
            return;
        }
        if (state.cooldownActive) {
            return;
        }

        var enchantmentMode = resolveEnchantmentMode(charmStack, event.getSource());
        var resolution = resolveDamage(event.getAmount(), magicData.getMana(), player, event.getSource(), enchantmentMode);
        if (resolution.negatedDamage() <= 0.0F) {
            return;
        }

        applyManaResult(player, magicData, resolution.remainingMana());
        event.setCanceled(true);
        if (resolution.consumeShellArmorDurability()) {
            damageArmorPiecesForShell(player);
        }

        switch (resolution.mitigationResult()) {
            case FULLY_NEGATED_ACTIVE -> {
                event.setCanceled(true);
                applyVanillaStyleIFrame(player);
                event.setInvulnerabilityTicks(Math.max(player.invulnerableTime, VANILLA_INVULNERABLE_TIME_TICKS));
                ForceFieldDefenseEvent.spawnManaShieldWallEffect(player, event.getSource(), false);
            }
            case FULLY_NEGATED_BURNED_OUT -> {
                event.setCanceled(true);
                applyVanillaStyleIFrame(player);
                event.setInvulnerabilityTicks(Math.max(player.invulnerableTime, VANILLA_INVULNERABLE_TIME_TICKS));
                ForceFieldDefenseEvent.spawnManaShieldWallEffect(player, event.getSource(), true);
            }
            case PARTIALLY_NEGATED_FAILED -> {
                ForceFieldDefenseEvent.spawnManaShieldWallEffect(player, event.getSource(), true);
                // Forge 1.20.1 では LivingAttackEvent が防御計算前なので、cancel 後に残ダメージだけ hurt し直して
                // 防具・エンチャント・吸収・ノックバックの通常経路へ戻す。1.21.1 側へはそのまま運ばず再確認すること。
                withState(player, current -> current.manualReentryGuard = true);
                try {
                    applyResidualDamage(player, event.getSource(), resolution.remainingDamage(), resolution.residualDamageProfile());
                } finally {
                    withState(player, current -> current.manualReentryGuard = false);
                }
            }
        }
    }

    private static boolean shouldIgnoreDuringVanillaStyleIFrame(ServerPlayer player, LivingIncomingDamageEvent event) {
        return player.invulnerableTime > VANILLA_COOLDOWN_DAMAGE_GATE_TICKS
                && !event.getSource().is(DamageTypeTags.BYPASSES_COOLDOWN);
    }

    private static void applyVanillaStyleIFrame(ServerPlayer player) {
        // LivingIncomingDamageEvent を cancel すると通常の hurt 経路が走らず i-frame が付かないため、
        // 完全無効化時だけバニラ相当の無敵時間を明示的に与えて継続接触ダメージの多重消費を防ぐ。
        player.invulnerableTime = Math.max(player.invulnerableTime, VANILLA_INVULNERABLE_TIME_TICKS);
    }

    static void onDeath(ServerPlayer player) {
        withState(player, ManaShieldCharmState::reset);
    }

    private static DamageResolution resolveDamage(
            float incomingDamage,
            float currentMana,
            ServerPlayer player,
            DamageSource source,
            EnchantmentMode enchantmentMode
    ) {
        return switch (enchantmentMode) {
            case SHELL -> resolveShellDamage(incomingDamage, currentMana, player, source);
            case SYNCHRONIZATION -> resolveSynchronizationDamage(incomingDamage, currentMana, player, source);
            case NONE -> resolveBaseDamage(incomingDamage, currentMana);
        };
    }

    private static DamageResolution resolveBaseDamage(float incomingDamage, float currentMana) {
        var barrierResolution = negateDamageWithMana(incomingDamage, currentMana, MANA_PER_DAMAGE);
        return new DamageResolution(
                barrierResolution.negatedDamage(),
                barrierResolution.remainingDamage(),
                barrierResolution.remainingMana(),
                ResidualDamageProfile.VANILLA,
                false
        );
    }

    private static DamageResolution resolveShellDamage(
            float incomingDamage,
            float currentMana,
            ServerPlayer player,
            DamageSource source
    ) {
        var reducedDamage = applyArmorReduction(player, source, incomingDamage);
        var barrierResolution = negateDamageWithMana(reducedDamage, currentMana, MANA_PER_DAMAGE);
        return new DamageResolution(
                barrierResolution.negatedDamage(),
                barrierResolution.remainingDamage(),
                barrierResolution.remainingMana(),
                ResidualDamageProfile.SHELL,
                true
        );
    }

    private static DamageResolution resolveSynchronizationDamage(
            float incomingDamage,
            float currentMana,
            ServerPlayer player,
            DamageSource source
    ) {
        var reducedDamage = applyEnchantmentProtection(player, source, incomingDamage);
        var mitigatedBySynchronization = Math.max(incomingDamage - reducedDamage, 0.0F);
        var synchronizationCharge = consumeMitigationCost(mitigatedBySynchronization, currentMana, SYNCHRONIZATION_MANA_PER_DAMAGE);
        if (synchronizationCharge.exhaustedDuringMitigation()) {
            return new DamageResolution(
                    mitigatedBySynchronization,
                    reducedDamage,
                    0.0F,
                    ResidualDamageProfile.SYNCHRONIZATION,
                    false
            );
        }

        var barrierResolution = negateDamageWithMana(reducedDamage, synchronizationCharge.remainingMana(), MANA_PER_DAMAGE);
        return new DamageResolution(
                mitigatedBySynchronization + barrierResolution.negatedDamage(),
                barrierResolution.remainingDamage(),
                barrierResolution.remainingMana(),
                ResidualDamageProfile.SYNCHRONIZATION,
                false
        );
    }

    private static void applyManaResult(ServerPlayer player, MagicData magicData, float remainingMana) {
        magicData.setMana(Math.max(0.0F, remainingMana));
        if (remainingMana <= 0.0F) {
            withState(player, state -> state.cooldownActive = true);
        }
    }

    private static void refreshCooldownIfRecovered(ServerPlayer player) {
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || magicData.getMana() < RECOVERY_THRESHOLD_MANA) {
            return;
        }

        withState(player, state -> state.cooldownActive = false);
    }

    static boolean isEquippedBy(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> inventory.isEquipped(ItemRegistry.MANA_SHIELD_CHARM.get()))
                .orElse(false);
    }

    private static ItemStack getEquippedCharm(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .resolve()
                .flatMap(inventory -> inventory.findFirstCurio(ItemRegistry.MANA_SHIELD_CHARM.get()))
                .map(slotResult -> slotResult.stack().copy())
                .orElse(ItemStack.EMPTY);
    }

    private static boolean isPrimaryEquippedCurio(SlotContext slotContext) {
        return CuriosApi.getCuriosInventory(slotContext.entity())
                .resolve()
                .flatMap(inventory -> inventory.findFirstCurio(ItemRegistry.MANA_SHIELD_CHARM.get()))
                .map(slotResult -> slotResult.slotContext().index() == slotContext.index()
                        && slotResult.slotContext().identifier().equals(slotContext.identifier()))
                .orElse(false);
    }

    private static @Nullable ManaShieldCharmState getState(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return null;
        }

        return spellData.get(CodexSpellStateTypeRegister.MANA_SHIELD_CHARM_STATE);
    }

    private static void withState(ServerPlayer player, Consumer<ManaShieldCharmState> consumer) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.MANA_SHIELD_CHARM_STATE, consumer);
    }

    private static void handleNeutralization(LivingIncomingDamageEvent event, ServerPlayer player, MagicData magicData) {
        var recoverMana = calculateRecoveryMana(event.getAmount());
        if (recoverMana <= 0.0F) {
            return;
        }

        MagicTools.recoverManaSafely(player, magicData, recoverMana);
        refreshCooldownIfRecovered(player);
        event.setCanceled(true);
        applyVanillaStyleIFrame(player);
        event.setInvulnerabilityTicks(Math.max(player.invulnerableTime, VANILLA_INVULNERABLE_TIME_TICKS));
        ForceFieldDefenseEvent.spawnManaShieldWallEffect(player, event.getSource(), false);
    }

    private static float calculateRecoveryMana(float incomingDamage) {
        return countWholeDamageSteps(incomingDamage) * MANA_PER_DAMAGE;
    }

    private static EnchantmentMode resolveEnchantmentMode(ItemStack charmStack, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return EnchantmentMode.NONE;
        }
        if (getExclusiveEnchantmentLevel(charmStack, EnchantmentRegistry.SHELL) > 0) {
            return EnchantmentMode.SHELL;
        }
        if (getExclusiveEnchantmentLevel(charmStack, EnchantmentRegistry.SYNCHRONIZATION) > 0) {
            return EnchantmentMode.SYNCHRONIZATION;
        }
        return EnchantmentMode.NONE;
    }

    private static int getExclusiveEnchantmentLevel(ItemStack stack, EnchantmentRegistry.EnchantmentRef enchantment) {
        return Enchantments.getLevel(stack, enchantment.key());
    }

    private static DamageResolution negateDamageWithMana(float incomingDamage, float currentMana, float manaPerDamage) {
        var remainingDamage = incomingDamage;
        var remainingMana = currentMana;
        var negatedDamage = 0.0F;

        while (remainingDamage >= DAMAGE_STEP && remainingMana >= manaPerDamage) {
            remainingDamage -= DAMAGE_STEP;
            remainingMana -= manaPerDamage;
            negatedDamage += DAMAGE_STEP;
        }

        return new DamageResolution(
                negatedDamage,
                Math.max(remainingDamage, 0.0F),
                remainingMana,
                ResidualDamageProfile.VANILLA,
                false
        );
    }

    private static ManaCostResult consumeMitigationCost(float mitigatedDamage, float currentMana, float manaPerDamage) {
        var remainingDamage = mitigatedDamage;
        var remainingMana = currentMana;
        var exhaustedDuringMitigation = false;

        while (remainingDamage >= DAMAGE_STEP && remainingMana >= manaPerDamage) {
            remainingDamage -= DAMAGE_STEP;
            remainingMana -= manaPerDamage;
        }

        if (remainingDamage >= DAMAGE_STEP) {
            exhaustedDuringMitigation = true;
        }

        return new ManaCostResult(remainingMana, exhaustedDuringMitigation);
    }

    private static float applyArmorReduction(ServerPlayer player, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return damage;
        }

        var armorStats = resolveArmorStats(player);
        return CombatRules.getDamageAfterAbsorb(
                damage,
                armorStats.armor(),
                armorStats.toughness()
        );
    }

    private static float applyEnchantmentProtection(ServerPlayer player, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BYPASSES_EFFECTS)) {
            return damage;
        }
        if (damage <= 0.0F || source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            return Math.max(damage, 0.0F);
        }

        var protection = EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source);
        if (protection > 0) {
            return CombatRules.getDamageAfterMagicAbsorb(damage, protection);
        }
        return damage;
    }

    private static void damageArmorPiecesForShell(ServerPlayer player) {
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var armorStack = player.getItemBySlot(slot);
            if (armorStack.isEmpty() || !armorStack.isDamageableItem()) {
                continue;
            }

            armorStack.hurtAndBreak(1, player, brokenEntity -> brokenEntity.broadcastBreakEvent(slot));
        }
    }

    private static void applyResidualDamage(
            ServerPlayer player,
            DamageSource source,
            float damage,
            ResidualDamageProfile profile
    ) {
        if (damage <= 0.0F) {
            return;
        }

        switch (profile) {
            case VANILLA -> player.hurt(source, damage);
            case SHELL -> hurtWithShellResidualProfile(player, source, damage);
            case SYNCHRONIZATION -> hurtWithSynchronizationResidualProfile(player, source, damage);
        }
    }

    private static void hurtWithShellResidualProfile(ServerPlayer player, DamageSource source, float damage) {
        damage = applyResistanceReduction(player, source, damage);
        damage = applyEnchantmentProtection(player, source, damage);
        applyDirectResidualDamage(player, source, damage);
    }

    private static void hurtWithSynchronizationResidualProfile(ServerPlayer player, DamageSource source, float damage) {
        damage = applyArmorReduction(player, source, damage);
        damage = applyResistanceReduction(player, source, damage);
        applyDirectResidualDamage(player, source, damage);
    }

    private static float applyResistanceReduction(ServerPlayer player, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BYPASSES_EFFECTS)) {
            return damage;
        }

        if (player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE)
                && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            var resistanceInstance = player.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE);
            if (resistanceInstance == null) {
                return damage;
            }

            var resistanceLevel = (resistanceInstance.getAmplifier() + 1) * 5;
            var remainingRatio = 25 - resistanceLevel;
            return Math.max(damage * remainingRatio / 25.0F, 0.0F);
        }

        return damage;
    }

    private static void applyDirectResidualDamage(ServerPlayer player, DamageSource source, float damage) {
        if (damage <= 0.0F) {
            return;
        }

        var healthDamage = Math.max(damage - player.getAbsorptionAmount(), 0.0F);
        player.setAbsorptionAmount(player.getAbsorptionAmount() - (damage - healthDamage));
        if (healthDamage <= 0.0F) {
            return;
        }

        player.getCombatTracker().recordDamage(source, healthDamage);
        player.setHealth(player.getHealth() - healthDamage);
        player.gameEvent(GameEvent.ENTITY_DAMAGE);
        player.invulnerableTime = Math.max(player.invulnerableTime, VANILLA_INVULNERABLE_TIME_TICKS);
    }

    private static ArmorStats resolveArmorStats(ServerPlayer player) {
        var playerArmor = (float) player.getArmorValue();
        var playerToughness = (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        var equipmentArmor = 0.0F;
        var equipmentToughness = 0.0F;

        for (var slot : ARMOR_SLOTS) {
            var stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            equipmentArmor += sumAttributeModifierAmount(stack, slot, Attributes.ARMOR);
            equipmentToughness += sumAttributeModifierAmount(stack, slot, Attributes.ARMOR_TOUGHNESS);
        }

        return new ArmorStats(
                Math.max(playerArmor, equipmentArmor),
                Math.max(playerToughness, equipmentToughness)
        );
    }

    private static float sumAttributeModifierAmount(
            ItemStack stack,
            EquipmentSlot slot,
            net.minecraft.world.entity.ai.attributes.Attribute attribute
    ) {
        return (float) stack.getAttributeModifiers(slot).get(attribute).stream()
                .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
                .mapToDouble(AttributeModifier::getAmount)
                .sum();
    }

    private static int countWholeDamageSteps(float damage) {
        var remainingDamage = damage;
        var count = 0;
        while (remainingDamage >= DAMAGE_STEP) {
            remainingDamage -= DAMAGE_STEP;
            ++count;
        }
        return count;
    }

    private enum MitigationResult {
        FULLY_NEGATED_ACTIVE,
        FULLY_NEGATED_BURNED_OUT,
        PARTIALLY_NEGATED_FAILED
    }

    private enum ResidualDamageProfile {
        VANILLA,
        SHELL,
        SYNCHRONIZATION
    }

    private enum EnchantmentMode {
        NONE,
        SHELL,
        SYNCHRONIZATION
    }

    private record ManaCostResult(float remainingMana, boolean exhaustedDuringMitigation) {
    }

    private record ArmorStats(float armor, float toughness) {
    }

    private record DamageResolution(
            float negatedDamage,
            float remainingDamage,
            float remainingMana,
            ResidualDamageProfile residualDamageProfile,
            boolean consumeShellArmorDurability
    ) {
        private MitigationResult mitigationResult() {
            if (remainingDamage <= 0.0F) {
                return remainingMana > 0.0F
                        ? MitigationResult.FULLY_NEGATED_ACTIVE
                        : MitigationResult.FULLY_NEGATED_BURNED_OUT;
            }
            return MitigationResult.PARTIALLY_NEGATED_FAILED;
        }
    }

    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };
}
