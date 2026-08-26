package jp.aquafactory.apprenticecodex.item.curios.manashieldcharm;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.ManaShieldCharmState;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.item.antimanaarrow.AntiManaArrowEntity;
import jp.aquafactory.apprenticecodex.spell.forcefield.ForceFieldDefenseEvent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
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
        if (state.cooldownActive || player.hasEffect(EffectRegistry.INERT_MANA_SHIELD)) {
            return;
        }
        if (shouldIgnoreDuringVanillaStyleIFrame(player, event)) {
            event.setCanceled(true);
            return;
        }

        if (event.getSource().getDirectEntity() instanceof AntiManaArrowEntity) {
            var hasNeutralization = getExclusiveEnchantmentLevel(charmStack, Enchantments.NEUTRALIZATION) > 0;
            var resistanceCost = neutralizationAntiManaArrowManaCost();
            if (hasNeutralization && magicData.getMana() >= resistanceCost) {
                applyManaResult(player, magicData, magicData.getMana() - resistanceCost);
                event.setCanceled(true);
                applyVanillaStyleIFrame(player);
                event.setInvulnerabilityTicks(Math.max(player.invulnerableTime, invulnerableTimeTicks()));
                ForceFieldDefenseEvent.spawnManaShieldWallEffect(player, event.getSource(), magicData.getMana() <= 0.0F);
                return;
            }

            player.addEffect(new MobEffectInstance(EffectRegistry.INERT_MANA_SHIELD, 20 * 30));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundRegistry.VANILLA_DEMICREATOR_BREAK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        var enchantmentMode = resolveEnchantmentMode(charmStack);
        var resolution = resolveDamage(event.getAmount(), magicData.getMana(), player, event.getSource(), enchantmentMode);
        if (magicData.getMana() > resolution.remainingMana() + 1.0e-4F) {
            applyManaResult(player, magicData, resolution.remainingMana());
        }
        if (resolution.negatedDamage() <= 0.0F) {
            return;
        }

        event.setCanceled(true);
        if (resolution.shellArmorDurabilityDamage() > 0) {
            damageArmorPiecesForShell(player, resolution.shellArmorDurabilityDamage());
        }

        switch (resolution.mitigationResult()) {
            case FULLY_NEGATED_ACTIVE -> {
                event.setCanceled(true);
                applyVanillaStyleIFrame(player);
                event.setInvulnerabilityTicks(Math.max(player.invulnerableTime, invulnerableTimeTicks()));
                ForceFieldDefenseEvent.spawnManaShieldWallEffect(player, event.getSource(), false);
            }
            case FULLY_NEGATED_BURNED_OUT -> {
                event.setCanceled(true);
                applyVanillaStyleIFrame(player);
                event.setInvulnerabilityTicks(Math.max(player.invulnerableTime, invulnerableTimeTicks()));
                ForceFieldDefenseEvent.spawnManaShieldWallEffect(player, event.getSource(), true);
            }
            case PARTIALLY_NEGATED_FAILED -> {
                ForceFieldDefenseEvent.spawnManaShieldWallEffect(player, event.getSource(), true);
                // LivingIncomingDamageEvent の cancel 後も、残ダメージだけは防具・エンチャント・吸収・ノックバックの通常経路へ戻す。
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
        var invulnerableTimeTicks = invulnerableTimeTicks();
        if (invulnerableTimeTicks <= 0) {
            return false;
        }

        return player.invulnerableTime > invulnerableTimeTicks / 2
                && !event.getSource().is(DamageTypeTags.BYPASSES_COOLDOWN);
    }

    private static void applyVanillaStyleIFrame(ServerPlayer player) {
        // LivingIncomingDamageEvent を cancel すると通常の hurt 経路が走らず i-frame が付かないため、
        // 完全無効化時だけバニラ相当の無敵時間を明示的に与えて継続接触ダメージの多重消費を防ぐ。
        var invulnerableTimeTicks = invulnerableTimeTicks();
        if (invulnerableTimeTicks > 0) {
            player.invulnerableTime = Math.max(player.invulnerableTime, invulnerableTimeTicks);
        }
    }

    static void onDeath(ServerPlayer player) {
        withState(player, ManaShieldCharmState::reset);
    }

    static void onCounterSpell(CounterSpellEvent event, ServerPlayer player) {
        if (event.isCanceled() || !player.isAlive() || !isEquippedBy(player)
                || player.hasEffect(EffectRegistry.INERT_MANA_SHIELD)) {
            return;
        }
        refreshCooldownIfRecovered(player);
        var state = getState(player);
        if (state == null || state.cooldownActive || state.manualReentryGuard) {
            return;
        }
        var charmStack = getEquippedCharm(player);
        if (charmStack.isEmpty()
                || getExclusiveEnchantmentLevel(charmStack, Enchantments.NEUTRALIZATION) <= 0) {
            return;
        }
        var magicData = MagicData.getPlayerMagicData(player);
        var cost = neutralizationCounterspellManaCost();
        if (magicData == null || magicData.getMana() < cost) {
            return;
        }
        applyManaResult(player, magicData, magicData.getMana() - cost);
        event.setCanceled(true);
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
                0
        );
    }

    private static DamageResolution resolveShellDamage(
            float incomingDamage,
            float currentMana,
            ServerPlayer player,
            DamageSource source
    ) {
        var remainingManaAfterActivation = Math.max(currentMana - shellActivationManaCost(), 0.0F);
        if (remainingManaAfterActivation <= 0.0F) {
            return new DamageResolution(
                    0.0F,
                    incomingDamage,
                    remainingManaAfterActivation,
                    ResidualDamageProfile.VANILLA,
                    0
            );
        }

        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            var barrierResolution = negateDamageWithMana(
                    incomingDamage,
                    HitManaBudget.forIncomingHit(remainingManaAfterActivation),
                    manaPerDamage()
            );
            return new DamageResolution(
                    barrierResolution.negatedDamage(),
                    barrierResolution.remainingDamage(),
                    barrierResolution.hitManaBudget().remainingMana(),
                    ResidualDamageProfile.VANILLA,
                    0
            );
        }

        var armorReducedDamage = applyArmorReduction(player, source, incomingDamage);
        var reducedDamage = applyEnchantmentProtection(player, source, armorReducedDamage);
        var mitigatedDamage = Math.max(incomingDamage - reducedDamage, 0.0F);
        var barrierResolution = negateDamageWithMana(
                reducedDamage,
                HitManaBudget.forIncomingHit(remainingManaAfterActivation),
                manaPerDamage()
        );
        return new DamageResolution(
                mitigatedDamage + barrierResolution.negatedDamage(),
                barrierResolution.remainingDamage(),
                barrierResolution.hitManaBudget().remainingMana(),
                ResidualDamageProfile.SHELL,
                mitigatedDamage > 1.0e-4F ? shellArmorDurabilityDamage(incomingDamage) : 0
        );
    }

    private static DamageResolution resolveSynchronizationDamage(
            float incomingDamage,
            float currentMana,
            ServerPlayer player,
            DamageSource source
    ) {
        var manaCostPerDamage = synchronizationManaCostPerDamage(source);
        var barrierResolution = negateDamageWithMana(
                incomingDamage,
                HitManaBudget.forIncomingHit(currentMana),
                manaCostPerDamage
        );
        return new DamageResolution(
                barrierResolution.negatedDamage(),
                barrierResolution.remainingDamage(),
                barrierResolution.hitManaBudget().remainingMana(),
                ResidualDamageProfile.VANILLA,
                0
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
                .flatMap(inventory -> inventory.findFirstCurio(ItemRegistry.MANA_SHIELD_CHARM.get()))
                .map(slotResult -> slotResult.stack().copy())
                .orElse(ItemStack.EMPTY);
    }

    private static boolean isPrimaryEquippedCurio(SlotContext slotContext) {
        return CuriosApi.getCuriosInventory(slotContext.entity())
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

    private static EnchantmentMode resolveEnchantmentMode(ItemStack charmStack) {
        if (getExclusiveEnchantmentLevel(charmStack, Enchantments.SHELL) > 0) {
            return EnchantmentMode.SHELL;
        }
        if (getExclusiveEnchantmentLevel(charmStack, Enchantments.SYNCHRONIZATION) > 0) {
            return EnchantmentMode.SYNCHRONIZATION;
        }
        return EnchantmentMode.NONE;
    }

    private static int getExclusiveEnchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        return Enchantments.getLevel(stack, enchantment);
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

    private static float applyArmorReduction(ServerPlayer player, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return damage;
        }

        var armorStats = resolveArmorStats(player);
        return CombatRules.getDamageAfterAbsorb(
                player,
                damage,
                source,
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

        var protection = EnchantmentHelper.getDamageProtection(player.serverLevel(), player, source);
        if (protection > 0) {
            return CombatRules.getDamageAfterMagicAbsorb(damage, protection);
        }
        return damage;
    }

    private static void damageArmorPiecesForShell(ServerPlayer player, int durabilityDamage) {
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var armorStack = player.getItemBySlot(slot);
            if (armorStack.isEmpty() || !armorStack.isDamageableItem()) {
                continue;
            }

            armorStack.hurtAndBreak(durabilityDamage, player, slot);
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
        }
    }

    private static void hurtWithShellResidualProfile(ServerPlayer player, DamageSource source, float damage) {
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
            Holder<Attribute> attribute
    ) {
        return (float) stack.getAttributeModifiers().modifiers().stream()
                .filter(entry -> entry.slot().test(slot))
                .filter(entry -> entry.attribute().equals(attribute))
                .filter(entry -> entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE)
                .mapToDouble(entry -> entry.modifier().amount())
                .sum();
    }

    private static float manaPerDamage() {
        return ApprenticeCodexServerConfig.manaShieldCharmManaPerDamage();
    }

    private static int recoveryThresholdMana() {
        return ApprenticeCodexServerConfig.manaShieldCharmRecoveryThresholdMana();
    }

    private static int shellActivationManaCost() {
        return ApprenticeCodexServerConfig.manaShieldCharmShellActivationManaCost();
    }

    private static int shellArmorDurabilityDamage(float incomingDamage) {
        var multiplier = ApprenticeCodexServerConfig.manaShieldCharmShellArmorDurabilityDamage();
        return (int) Math.ceil(incomingDamage / 4.0F * multiplier);
    }

    private static float synchronizationManaCostPerDamage(DamageSource source) {
        var cost = manaPerDamage()
                + ApprenticeCodexServerConfig.manaShieldCharmSynchronizationAdditionalManaPerDamage();
        var benefitsFromSynchronization = source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || (source.is(DamageTypeTags.BYPASSES_ARMOR)
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY));
        if (benefitsFromSynchronization) {
            cost -= ApprenticeCodexServerConfig.manaShieldCharmSynchronizationArmorBypassManaReductionPerDamage();
        }
        return Math.max(cost, 0.0F);
    }

    private static int neutralizationAntiManaArrowManaCost() {
        return ApprenticeCodexServerConfig.manaShieldCharmNeutralizationAntiManaArrowManaCost();
    }

    private static int neutralizationCounterspellManaCost() {
        return ApprenticeCodexServerConfig.manaShieldCharmNeutralizationCounterspellManaCost();
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
        SHELL
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

    private record ArmorStats(float armor, float toughness) {
    }

    private record DamageResolution(
            float negatedDamage,
            float remainingDamage,
            float remainingMana,
            ResidualDamageProfile residualDamageProfile,
            int shellArmorDurabilityDamage
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
