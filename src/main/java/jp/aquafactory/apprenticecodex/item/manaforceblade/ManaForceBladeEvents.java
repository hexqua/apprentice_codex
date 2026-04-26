package jp.aquafactory.apprenticecodex.item.manaforceblade;

import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.ManaForceBlade;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ManaForceBladeEvents {
    private static final int PERFECT_GUARD_TICKS = 10;
    private static final int COST_INTERVAL_TICKS = 20;
    private static final int ACTION_INTERVAL_TICKS = 5;
    private static final float RANGED_GUARD_MANA_COST = 20.0F;
    private static final int RANGED_GUARD_DURABILITY_COST = 1;
    private static final float MELEE_GUARD_MANA_COST = 50.0F;
    private static final int MELEE_GUARD_DURABILITY_COST = 2;
    private static final double RANGED_DISTANCE_SQR = 3.0D * 3.0D;
    private static final double PROJECTILE_SPEED = 2.45D;
    private static final String RANGED_COST_TICK_TAG = ApprenticeCodex.MODID + ":mana_force_blade_ranged_cost_tick";
    private static final String RANGED_ACTION_TICK_TAG = ApprenticeCodex.MODID + ":mana_force_blade_ranged_action_tick";
    private static final String MELEE_COST_TICK_TAG = ApprenticeCodex.MODID + ":mana_force_blade_melee_cost_tick";
    private static final String MELEE_ACTION_TICK_TAG = ApprenticeCodex.MODID + ":mana_force_blade_melee_action_tick";
    private static final String MAIN_HAND_MODIFIER_KEY = "item.modifiers.mainhand";
    private static final String ATTACK_DAMAGE_MODIFIER_KEY = "attribute.modifier.equals.0";

    private static boolean applyingGuardCounterDamage;

    private ManaForceBladeEvents() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (applyingGuardCounterDamage || event.isCanceled()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)
                || event.getSource().getDirectEntity() != attacker) {
            return;
        }

        var stack = attacker.getMainHandItem();
        if (!ManaForceBlade.isManaForceBlade(stack)) {
            return;
        }

        var baseDamage = ManaForceBlade.resolveBladeAttackDamage(stack);
        var multiplier = ManaForceBlade.resolveDamageMultiplier(attacker, stack);
        var additionalDamage = baseDamage * (multiplier - 1.0F);
        event.setAmount(Math.max(0.0F, event.getAmount() + additionalDamage));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0.0F || event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isUsingItem()) {
            return;
        }

        var stack = player.getUseItem();
        if (!ManaForceBlade.isManaForceBlade(stack)) {
            return;
        }

        var usedTicks = stack.getUseDuration() - player.getUseItemRemainingTicks();
        var perfectGuard = usedTicks <= PERFECT_GUARD_TICKS;
        if (isRangedAttack(event, player)) {
            handleRangedGuard(event, player, stack, perfectGuard);
        } else {
            handleMeleeGuard(event, player, stack, perfectGuard);
        }
    }

    @SubscribeEvent
    public static void onChangeMana(ChangeManaEvent event) {
        if (event.getNewMana() <= event.getOldMana()) {
            return;
        }
        var player = event.getEntity();
        if (player != null && player.isUsingItem() && ManaForceBlade.isManaForceBlade(player.getUseItem())) {
            // Iron's 1.20.1 の ChangeManaEvent は回復源を区別しないため、構え中の正の変化をまとめて抑制する。
            event.setNewMana(event.getOldMana());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (!ManaForceBlade.isManaForceBlade(stack) || event.getEntity() == null) {
            return;
        }

        var damage = ManaForceBlade.resolveFinalAttackDamage(event.getEntity(), stack);
        replaceMainHandAttackDamageTooltip(event.getToolTip(), damage);
    }

    private static void replaceMainHandAttackDamageTooltip(List<Component> tooltip, float damage) {
        var inMainHandSection = false;
        for (var i = 0; i < tooltip.size(); i++) {
            var translatableContents = findFirstTranslatableContents(tooltip.get(i));
            if (translatableContents == null) {
                continue;
            }

            var key = translatableContents.getKey();
            if (key.startsWith("item.modifiers.")) {
                inMainHandSection = MAIN_HAND_MODIFIER_KEY.equals(key);
                continue;
            }
            if (inMainHandSection && isAttackDamageTooltipLine(translatableContents)) {
                tooltip.set(i, createAttackDamageTooltipLine(damage));
                return;
            }
        }
    }

    @Nullable
    private static TranslatableContents findFirstTranslatableContents(Component component) {
        if (component.getContents() instanceof TranslatableContents translatableContents) {
            return translatableContents;
        }

        for (var sibling : component.getSiblings()) {
            var translatableContents = findFirstTranslatableContents(sibling);
            if (translatableContents != null) {
                return translatableContents;
            }
        }
        return null;
    }

    private static boolean isAttackDamageTooltipLine(TranslatableContents translatableContents) {
        if (!ATTACK_DAMAGE_MODIFIER_KEY.equals(translatableContents.getKey())) {
            return false;
        }

        var args = translatableContents.getArgs();
        if (args.length < 2 || !(args[1] instanceof Component attributeName)) {
            return false;
        }
        return attributeName.getContents() instanceof TranslatableContents attributeNameContents
                && Attributes.ATTACK_DAMAGE.getDescriptionId().equals(attributeNameContents.getKey());
    }

    private static Component createAttackDamageTooltipLine(float damage) {
        return CommonComponents.space().append(Component.translatable(
                ATTACK_DAMAGE_MODIFIER_KEY,
                Utils.stringTruncation(damage, 2),
                Component.translatable(Attributes.ATTACK_DAMAGE.getDescriptionId())
        )).withStyle(ChatFormatting.DARK_GREEN);
    }

    private static boolean isRangedAttack(LivingAttackEvent event, ServerPlayer player) {
        var directEntity = event.getSource().getDirectEntity();
        if (directEntity instanceof Projectile) {
            return true;
        }

        var sourceEntity = event.getSource().getEntity();
        return sourceEntity != null && sourceEntity.distanceToSqr(player) >= RANGED_DISTANCE_SQR;
    }

    private static void handleRangedGuard(LivingAttackEvent event, ServerPlayer player, ItemStack stack, boolean perfectGuard) {
        var now = player.level().getGameTime();
        if (!perfectGuard && !tryPayPeriodicGuardCost(
                player,
                stack,
                RANGED_COST_TICK_TAG,
                now,
                RANGED_GUARD_MANA_COST,
                RANGED_GUARD_DURABILITY_COST
        )) {
            return;
        }

        var origin = resolveGuardOrigin(event.getSource().getDirectEntity(), event.getSource().getEntity(), player);
        if (event.getSource().getDirectEntity() instanceof Projectile projectile) {
            projectile.discard();
        }

        if (tryMarkAction(stack, RANGED_ACTION_TICK_TAG, now)) {
            shootGuardProjectile(player, stack, origin, perfectGuard);
        }
        event.setCanceled(true);
    }

    private static void handleMeleeGuard(LivingAttackEvent event, ServerPlayer player, ItemStack stack, boolean perfectGuard) {
        var now = player.level().getGameTime();
        if (!perfectGuard && !tryPayPeriodicGuardCost(
                player,
                stack,
                MELEE_COST_TICK_TAG,
                now,
                MELEE_GUARD_MANA_COST,
                MELEE_GUARD_DURABILITY_COST
        )) {
            return;
        }

        if (tryMarkAction(stack, MELEE_ACTION_TICK_TAG, now)) {
            applyMeleeCounter(player, stack, event.getSource().getEntity(), perfectGuard);
        }
        event.setCanceled(true);
    }

    private static boolean tryPayPeriodicGuardCost(
            ServerPlayer player,
            ItemStack stack,
            String lastCostTickTag,
            long now,
            float manaCost,
            int durabilityCost
    ) {
        var tag = stack.getOrCreateTag();
        if (tag.contains(lastCostTickTag) && now - tag.getLong(lastCostTickTag) < COST_INTERVAL_TICKS) {
            return true;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (!player.getAbilities().instabuild && (magicData == null || magicData.getMana() < manaCost)) {
            return false;
        }

        ManaForceBlade.spendMana(player, manaCost);
        stack.hurtAndBreak(durabilityCost, player, brokenPlayer -> brokenPlayer.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        tag.putLong(lastCostTickTag, now);
        return true;
    }

    private static boolean tryMarkAction(ItemStack stack, String actionTickTag, long now) {
        var tag = stack.getOrCreateTag();
        if (tag.contains(actionTickTag) && now - tag.getLong(actionTickTag) < ACTION_INTERVAL_TICKS) {
            return false;
        }

        tag.putLong(actionTickTag, now);
        return true;
    }

    private static net.minecraft.world.phys.Vec3 resolveGuardOrigin(
            @Nullable Entity directEntity,
            @Nullable Entity sourceEntity,
            ServerPlayer player
    ) {
        if (directEntity instanceof Projectile) {
            return directEntity.position();
        }
        if (sourceEntity != null) {
            return sourceEntity.position().add(0.0D, sourceEntity.getBbHeight() * 0.5D, 0.0D);
        }
        return player.getEyePosition();
    }

    private static void shootGuardProjectile(ServerPlayer player, ItemStack stack, net.minecraft.world.phys.Vec3 origin, boolean perfectGuard) {
        var projectile = new ManaForceBladeProjectileEntity(EntityRegistry.MANA_FORCE_BLADE_PROJECTILE.get(), player.level(), player);
        projectile.setPos(origin);
        projectile.setDamage(ManaForceBlade.resolveBladeAttackDamage(stack) * (perfectGuard ? 1.5F : 1.0F));
        projectile.setProjectileVelocity(player.getLookAngle().normalize(), PROJECTILE_SPEED);
        player.level().addFreshEntity(projectile);
    }

    private static void applyMeleeCounter(ServerPlayer player, ItemStack stack, @Nullable Entity sourceEntity, boolean perfectGuard) {
        if (sourceEntity == null) {
            return;
        }

        var direction = sourceEntity.position().subtract(player.position());
        if (direction.lengthSqr() < 1.0E-7D) {
            direction = player.getLookAngle();
        }
        direction = direction.normalize();
        sourceEntity.push(direction.x * 1.6D, 0.35D, direction.z * 1.6D);
        sourceEntity.hurtMarked = true;

        if (perfectGuard && sourceEntity instanceof LivingEntity livingTarget) {
            applyingGuardCounterDamage = true;
            try {
                CombatTools.applyDamage(
                        livingTarget,
                        ManaForceBlade.resolveBladeAttackDamage(stack),
                        CombatTools.getDamageSource(player.level(), player, DamageTypes.MANA_FORCE_BLADE),
                        null,
                        CombatTools.KnockbackTypes.NO_KNOCKBACK
                );
            } finally {
                applyingGuardCounterDamage = false;
            }
        }
    }
}
