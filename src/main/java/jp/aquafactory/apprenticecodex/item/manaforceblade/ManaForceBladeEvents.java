package jp.aquafactory.apprenticecodex.item.manaforceblade;

import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightManaForceBladeCompat;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.ManaForceBlade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ManaForceBladeEvents {
    private static final String EPICFIGHT_MOD_ID = "epicfight";
    private static final String MAIN_HAND_MODIFIER_KEY = "item.modifiers.mainhand";
    private static final String ATTACK_DAMAGE_MODIFIER_KEY = "attribute.modifier.equals.0";
    private static final String IMBUE_HELP_KEY = "item.apprenticecodex.mana_force_blade.desc.imbue_help";

    private ManaForceBladeEvents() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (ManaForceBladeGuardLogic.isApplyingGuardCounterDamage() || event.isCanceled()) {
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
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0.0F || event.getEntity().level().isClientSide) {
            return;
        }
        if (ModList.get().isLoaded(EPICFIGHT_MOD_ID)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isUsingItem()) {
            return;
        }

        var stack = player.getUseItem();
        if (!ManaForceBlade.isManaForceBlade(stack)) {
            return;
        }

        var usedTicks = stack.getUseDuration(player) - player.getUseItemRemainingTicks();
        if (ManaForceBladeGuardLogic.tryHandleGuard(
                player,
                stack,
                event.getSource(),
                ManaForceBladeGuardLogic.isPerfectGuard(usedTicks)
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onChangeMana(ChangeManaEvent event) {
        if (event.getNewMana() <= event.getOldMana()) {
            return;
        }
        var player = event.getEntity();
        if (player == null) {
            return;
        }

        if (ModList.get().isLoaded(EPICFIGHT_MOD_ID)) {
            if (ApprenticeCodexServerConfig.manaForceBladeDisableManaRecoveryWhileGuarding()
                    && player instanceof ServerPlayer serverPlayer && EpicFightManaForceBladeCompat.isGuarding(serverPlayer)) {
                event.setNewMana(event.getOldMana());
            }
            return;
        }

        if (ApprenticeCodexServerConfig.manaForceBladeDisableManaRecoveryWhileGuarding()
                && player.isUsingItem() && ManaForceBlade.isManaForceBlade(player.getUseItem())) {
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

        var damage = ManaForceBlade.resolveFinalAttackDamage(
                event.getEntity(),
                stack,
                ManaForceBladeConfigState.imbueDamageMultiplierScale()
        );
        replaceMainHandAttackDamageTooltip(event.getToolTip(), damage);
        replaceAttackManaCostTooltip(event.getToolTip(), event.getEntity(), stack);
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
                && Attributes.ATTACK_DAMAGE.value().getDescriptionId().equals(attributeNameContents.getKey());
    }

    private static Component createAttackDamageTooltipLine(float damage) {
        return CommonComponents.space().append(Component.translatable(
                ATTACK_DAMAGE_MODIFIER_KEY,
                Utils.stringTruncation(damage, 2),
                Component.translatable(Attributes.ATTACK_DAMAGE.value().getDescriptionId())
        )).withStyle(ChatFormatting.DARK_GREEN);
    }

    private static void replaceAttackManaCostTooltip(List<Component> tooltip, LivingEntity entity, net.minecraft.world.item.ItemStack stack) {
        var manaCost = ManaForceBlade.resolveBladeAttackManaCost(
                entity,
                stack,
                ManaForceBladeConfigState.attackManaCostMultiplier(),
                ManaForceBladeConfigState.attackManaSchoolMultiplierScale(),
                ManaForceBladeConfigState.imbueDamageMultiplierScale()
        );
        for (var i = 0; i < tooltip.size(); i++) {
            var translatableContents = findFirstTranslatableContents(tooltip.get(i));
            if (translatableContents == null || !IMBUE_HELP_KEY.equals(translatableContents.getKey())) {
                continue;
            }

            if (manaCost <= 0.0F) {
                tooltip.remove(i);
            } else {
                tooltip.set(i, Component.translatable(IMBUE_HELP_KEY, net.minecraft.util.Mth.ceil(manaCost))
                        .withStyle(ChatFormatting.AQUA));
            }
            return;
        }
    }

    public static void playBlueGuardEffect(ServerPlayer player, Vec3 position, int sparkCount) {
        ManaForceBladeGuardLogic.playBlueGuardEffect(player, position, sparkCount);
    }
}
