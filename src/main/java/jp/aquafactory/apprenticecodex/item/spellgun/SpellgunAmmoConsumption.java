package jp.aquafactory.apprenticecodex.item.spellgun;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitEffects;
import jp.aquafactory.apprenticecodex.registry.ApprenticeAttributeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SpellgunAmmoConsumption {
    public static final ResourceLocation HOOD_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "magi_agent_suit_hood_ammo_conservation");

    private SpellgunAmmoConsumption() {
    }

    public static AttributeModifier hoodModifier(double chance) {
        return new AttributeModifier(HOOD_MODIFIER_ID, -chance, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public static void refreshHoodModifier(ServerPlayer player) {
        var attribute = player.getAttribute(ApprenticeAttributeRegistry.SPELLGUN_AMMO_CONSERVATION);
        if (attribute == null) {
            return;
        }
        var chance = MagiAgentSuitEffects.isWearingSuitPiece(player, ArmorItem.Type.HELMET)
                ? ApprenticeCodexServerConfig.magiAgentSuitAmmoNoConsumeChance() : 0.0D;
        var existing = attribute.getModifier(HOOD_MODIFIER_ID);
        if (chance <= 0.0D) {
            if (existing != null) {
                attribute.removeModifier(HOOD_MODIFIER_ID);
            }
        } else {
            var expected = hoodModifier(chance);
            if (!expected.equals(existing)) {
                // 装備を付け直さずに設定を変更しても、フードの補正だけを更新する。
                attribute.removeModifier(HOOD_MODIFIER_ID);
                attribute.addTransientModifier(expected);
            }
        }
    }

    public static double consumeChance(ServerPlayer player) {
        // ログイン直後や装備更新と同じtickの発射でも、旧フード抽選を重ねず最新値を使う。
        refreshHoodModifier(player);
        return Mth.clamp(player.getAttributeValue(ApprenticeAttributeRegistry.SPELLGUN_AMMO_CONSERVATION), 0.0D, 1.0D);
    }

    public static boolean shouldSkip(ServerPlayer player) {
        var chance = consumeChance(player);
        return chance <= 0.0D || chance < 1.0D && player.getRandom().nextDouble() >= chance;
    }

    public static boolean shouldSkipManaCostWithAmmo() {
        return ApprenticeCodexServerConfig.magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refreshHoodModifier(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refreshHoodModifier(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refreshHoodModifier(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refreshHoodModifier(player);
        }
    }
}
