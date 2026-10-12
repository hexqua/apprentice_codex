package jp.aquafactory.apprenticecodex.compat.ironsartifice;

import io.redspace.irons_artifice.api.ComposeShotEvent;
import io.redspace.irons_artifice.api.GunAboutToShootEvent;
import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellboundArmsModuleServerConfig.Values;
import jp.aquafactory.apprenticecodex.item.modifier.commencemagimodule.CommenceMagiModuleConfigState;
import jp.aquafactory.apprenticecodex.item.modifier.commencemagimodule.CommenceMagiModuleModifier;
import jp.aquafactory.apprenticecodex.item.modifier.spellboundarmsmodule.SpellboundArmsModuleConfigState;
import jp.aquafactory.apprenticecodex.item.modifier.spellboundarmsmodule.SpellboundArmsModuleModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public final class IronsArtificeManaEvents {
    public static final ComponentType<Integer> MANA_COST = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "irons_artifice_mana_cost"), () -> 0);

    private IronsArtificeManaEvents() {}

    public static int calculate(ShotProfile profile, Values values, int commenceCost) {
        int count = profile.peek(SpellboundArmsModuleModifier.MODULE_COUNT);
        double cost = profile.peek(CommenceMagiModuleModifier.MODULE_COUNT) > 0 ? commenceCost : 0;
        if (count > 0) {
            var normalDamage = profile.peek(ShotComponents.DAMAGE).copy();
            var boost = profile.peek(SpellboundArmsModuleEvents.DAMAGE_BOOST);
            // 同値の他modifierを除外せず、このmoduleの補正だけを取り除く。倍率0でも通常部分を復元できる。
            normalDamage.modifiers().removeIf(modifier -> modifier == boost);
            double normal = Math.max(0, normalDamage.compute());
            double magic = Math.max(0, profile.value(ShotComponents.DAMAGE) - normal);
            cost += normal * values.normalManaCostMultiplier() + magic * values.magicManaCostMultiplier();
        }
        // 上流のマナ上限より十分大きい値へ飽和させ、整数overflowによる無料発射を防ぐ。
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(cost));
    }

    public static int cost(ShotProfile profile, boolean client) {
        return calculate(profile,
                client ? SpellboundArmsModuleConfigState.values() : ApprenticeCodexServerConfig.spellboundArmsModuleConfig(),
                (client ? CommenceMagiModuleConfigState.values() : ApprenticeCodexServerConfig.commenceMagiModuleConfig()).manaCost());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCompose(ComposeShotEvent event) {
        var shooter = event.getEntity();
        var profile = event.getShotProfile();
        profile.components().set(MANA_COST, cost(profile, shooter.level().isClientSide));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAboutToShoot(GunAboutToShootEvent event) {
        var shooter = event.getEntity();
        var profile = event.getShotProfile();
        int cost = cost(profile, shooter.level().isClientSide);
        profile.components().set(MANA_COST, cost);
        if (!(shooter instanceof Player player) || player.hasInfiniteMaterials() || cost <= 0) return;
        float mana = player.level().isClientSide ? ClientGate.mana() : MagicData.getPlayerMagicData(player).getMana();
        if (mana < cost) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("ui.apprenticecodex.irons_artifice.insufficient_mana")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientGate {
        private static float mana() { return ClientMagicData.getPlayerMana(); }
    }
}
