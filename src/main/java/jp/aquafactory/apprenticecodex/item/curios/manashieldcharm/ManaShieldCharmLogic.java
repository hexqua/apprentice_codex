package jp.aquafactory.apprenticecodex.item.curios.manashieldcharm;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.ManaShieldCharmState;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.forcefield.ForceFieldDefenseEvent;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.function.Consumer;

final class ManaShieldCharmLogic {
    private static final float DAMAGE_STEP = 1.0F;

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

    static void onLivingAttack(LivingAttackEvent event, ServerPlayer player) {
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
                applyVanillaStyleIFrame(player);
                ForceFieldDefenseEvent.spawnManaShieldWallEffect(player, event.getSource(), false);
            }
            case FULLY_NEGATED_BURNED_OUT -> {
                applyVanillaStyleIFrame(player);
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

    private static boolean shouldIgnoreDuringVanillaStyleIFrame(ServerPlayer player, LivingAttackEvent event) {
        var invulnerableTimeTicks = invulnerableTimeTicks();
        if (invulnerableTimeTicks <= 0) {
            return false;
        }

        return player.invulnerableTime > invulnerableTimeTicks / 2
                && !event.getSource().is(DamageTypeTags.BYPASSES_COOLDOWN);
    }

    private static void applyVanillaStyleIFrame(ServerPlayer player) {
        // LivingAttackEvent を cancel すると通常の hurt 経路が走らず i-frame が付かないため、
        // 完全無効化時だけバニラ相当の無敵時間を明示的に与えて継続接触ダメージの多重消費を防ぐ。
        var invulnerableTimeTicks = invulnerableTimeTicks();
        if (invulnerableTimeTicks > 0) {
            player.invulnerableTime = Math.max(player.invulnerableTime, invulnerableTimeTicks);
        }
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
        // 各モードで軽減順は違っても、低マナ時に赤字で通せる不足ステップは 1 ヒット 1 回だけに揃える。
        // 1.21.1 側ではイベント順や再計算位置が変わり得るため、処理形状ではなくこの救済意図を優先して移植判断すること。
        return switch (enchantmentMode) {
            case SHELL -> resolveShellDamage(incomingDamage, currentMana, player, source);
            case SYNCHRONIZATION -> resolveSynchronizationDamage(incomingDamage, currentMana, player, source);
            case NONE -> resolveBaseDamage(incomingDamage, currentMana);
        };
    }

    private static DamageResolution resolveBaseDamage(float incomingDamage, float currentMana) {
        var barrierResolution = negateDamageWithMana(
                incomingDamage,
                HitManaBudget.forIncomingHit(currentMana),
                manaPerDamage()
        );
        return new DamageResolution(
                barrierResolution.negatedDamage(),
                barrierResolution.remainingDamage(),
                barrierResolution.hitManaBudget().remainingMana(),
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
        var barrierResolution = negateDamageWithMana(
                reducedDamage,
                HitManaBudget.forIncomingHit(currentMana),
                manaPerDamage()
        );
        return new DamageResolution(
                barrierResolution.negatedDamage(),
                barrierResolution.remainingDamage(),
                barrierResolution.hitManaBudget().remainingMana(),
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
        var synchronizationCharge = consumeMitigationCost(
                mitigatedBySynchronization,
                HitManaBudget.forIncomingHit(currentMana),
                synchronizationManaPerDamage()
        );
        if (synchronizationCharge.stopBeforeBarrierStage()) {
            return new DamageResolution(
                    mitigatedBySynchronization,
                    reducedDamage,
                    synchronizationCharge.hitManaBudget().remainingMana(),
                    ResidualDamageProfile.SYNCHRONIZATION,
                    false
            );
        }

        var barrierResolution = negateDamageWithMana(
                reducedDamage,
                synchronizationCharge.hitManaBudget(),
                manaPerDamage()
        );
        return new DamageResolution(
                mitigatedBySynchronization + barrierResolution.negatedDamage(),
                barrierResolution.remainingDamage(),
                barrierResolution.hitManaBudget().remainingMana(),
                ResidualDamageProfile.SYNCHRONIZATION,
                false
        );
    }

    private static void applyManaResult(ServerPlayer player, MagicData magicData, float remainingMana) {
        var spentMana = magicData.getMana() > remainingMana + 1.0e-4F;
        magicData.setMana(Math.max(0.0F, remainingMana));
        if (remainingMana <= 0.0F && spentMana && recoveryThresholdMana() > 0) {
            withState(player, state -> state.cooldownActive = true);
        } else {
            withState(player, state -> state.cooldownActive = false);
        }
    }

    private static void refreshCooldownIfRecovered(ServerPlayer player) {
        if (recoveryThresholdMana() <= 0) {
            withState(player, state -> state.cooldownActive = false);
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || magicData.getMana() < recoveryThresholdMana()) {
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

    private static void handleNeutralization(LivingAttackEvent event, ServerPlayer player, MagicData magicData) {
        var recoverMana = calculateRecoveryMana(event.getAmount());
        if (recoverMana > 0.0F) {
            MagicTools.recoverManaSafely(player, magicData, recoverMana);
            refreshCooldownIfRecovered(player);
        }

        event.setCanceled(true);
        applyVanillaStyleIFrame(player);
        ForceFieldDefenseEvent.spawnManaShieldWallEffect(player, event.getSource(), false);
    }

    private static float calculateRecoveryMana(float incomingDamage) {
        return countWholeDamageSteps(incomingDamage) * neutralizationRecoverManaPerDamage();
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

    private static int getExclusiveEnchantmentLevel(ItemStack stack, net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.enchantment.Enchantment> enchantment) {
        return enchantment.isPresent() ? stack.getEnchantmentLevel(enchantment.get()) : 0;
    }

    private static BarrierResolution negateDamageWithMana(
            float incomingDamage,
            HitManaBudget hitManaBudget,
            float manaPerDamage
    ) {
        var remainingDamage = incomingDamage;
        var remainingMana = hitManaBudget.remainingMana();
        var negatedDamage = 0.0F;
        var overdraftAvailable = hitManaBudget.overdraftAvailable();

        if (manaPerDamage <= 0.0F) {
            while (remainingDamage >= DAMAGE_STEP) {
                remainingDamage -= DAMAGE_STEP;
                negatedDamage += DAMAGE_STEP;
            }

            return new BarrierResolution(
                    negatedDamage,
                    Math.max(remainingDamage, 0.0F),
                    new HitManaBudget(remainingMana, overdraftAvailable)
            );
        }

        while (remainingDamage >= DAMAGE_STEP) {
            if (remainingMana >= manaPerDamage) {
                remainingDamage -= DAMAGE_STEP;
                remainingMana -= manaPerDamage;
                negatedDamage += DAMAGE_STEP;
                continue;
            }
            if (overdraftAvailable && remainingMana > 0.0F) {
                remainingDamage -= DAMAGE_STEP;
                negatedDamage += DAMAGE_STEP;
                remainingMana = 0.0F;
                overdraftAvailable = false;
            }
            break;
        }

        return new BarrierResolution(
                negatedDamage,
                Math.max(remainingDamage, 0.0F),
                new HitManaBudget(remainingMana, overdraftAvailable)
        );
    }

    private static MitigationChargeResult consumeMitigationCost(
            float mitigatedDamage,
            HitManaBudget hitManaBudget,
            float manaPerDamage
    ) {
        var remainingDamage = mitigatedDamage;
        var remainingMana = hitManaBudget.remainingMana();

        if (manaPerDamage <= 0.0F) {
            return new MitigationChargeResult(hitManaBudget, false);
        }

        while (remainingDamage >= DAMAGE_STEP && remainingMana >= manaPerDamage) {
            remainingDamage -= DAMAGE_STEP;
            remainingMana -= manaPerDamage;
        }

        if (remainingDamage >= DAMAGE_STEP) {
            // Synchronization は enchant 軽減コスト段階で不足した時点で hit を burned-out 扱いにし、
            // 後段の通常障壁まで進ませない。ここでさらに barrier 側も救済すると 1 ヒット 2 回無料になる。
            if (hitManaBudget.overdraftAvailable() && remainingMana > 0.0F) {
                return new MitigationChargeResult(new HitManaBudget(0.0F, false), true);
            }
            return new MitigationChargeResult(new HitManaBudget(0.0F, false), true);
        }

        return new MitigationChargeResult(
                new HitManaBudget(remainingMana, hitManaBudget.overdraftAvailable()),
                false
        );
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
        var durabilityDamage = ApprenticeCodexServerConfig.manaShieldCharmShellArmorDurabilityDamage();
        if (durabilityDamage <= 0) {
            return;
        }

        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var armorStack = player.getItemBySlot(slot);
            if (armorStack.isEmpty() || !armorStack.isDamageableItem()) {
                continue;
            }

            armorStack.hurtAndBreak(durabilityDamage, player, brokenEntity -> brokenEntity.broadcastBreakEvent(slot));
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
        var invulnerableTimeTicks = invulnerableTimeTicks();
        if (invulnerableTimeTicks > 0) {
            player.invulnerableTime = Math.max(player.invulnerableTime, invulnerableTimeTicks);
        }
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

    private static float manaPerDamage() {
        return ApprenticeCodexServerConfig.manaShieldCharmManaPerDamage();
    }

    private static int recoveryThresholdMana() {
        return ApprenticeCodexServerConfig.manaShieldCharmRecoveryThresholdMana();
    }

    private static float synchronizationManaPerDamage() {
        return ApprenticeCodexServerConfig.manaShieldCharmSynchronizationManaPerDamage();
    }

    private static float neutralizationRecoverManaPerDamage() {
        return ApprenticeCodexServerConfig.manaShieldCharmNeutralizationRecoverManaPerDamage();
    }

    private static int invulnerableTimeTicks() {
        return ApprenticeCodexServerConfig.manaShieldCharmInvulnerableTimeTicks();
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

    private record HitManaBudget(float remainingMana, boolean overdraftAvailable) {
        private static HitManaBudget forIncomingHit(float currentMana) {
            return new HitManaBudget(currentMana, currentMana > 0.0F);
        }
    }

    private record BarrierResolution(
            float negatedDamage,
            float remainingDamage,
            HitManaBudget hitManaBudget
    ) {
    }

    private record MitigationChargeResult(HitManaBudget hitManaBudget, boolean stopBeforeBarrierStage) {
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
