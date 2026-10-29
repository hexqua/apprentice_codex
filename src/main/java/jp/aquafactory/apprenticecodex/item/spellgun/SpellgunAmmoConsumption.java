package jp.aquafactory.apprenticecodex.item.spellgun;

import java.util.UUID;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitEffects;
import jp.aquafactory.apprenticecodex.registry.ApprenticeAttributeRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SpellgunAmmoConsumption {
    public static final UUID HOOD_MODIFIER_ID = UUID.fromString("76bdc49e-88d8-4b31-8f51-d60ccca6b94d");

    private SpellgunAmmoConsumption() {
    }

    public static AttributeModifier hoodModifier(double chance) {
        return new AttributeModifier(HOOD_MODIFIER_ID, "magi_agent_suit_hood_ammo_conservation", -chance, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    public static void refreshHoodModifier(ServerPlayer player) {
        var attribute = player.getAttribute(ApprenticeAttributeRegistry.SPELLGUN_AMMO_CONSERVATION.get());
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
            // 1.20.1のequalsはUUIDしか比較しないため、設定変更は値と演算種別で検出する。
            if (existing == null || existing.getAmount() != expected.getAmount()
                    || existing.getOperation() != expected.getOperation()) {
                // 装備を付け直さずに設定を変更しても、フードの補正だけを更新する。
                attribute.removeModifier(HOOD_MODIFIER_ID);
                attribute.addTransientModifier(expected);
            }
        }
    }

    public static double consumeChance(ServerPlayer player) {
        // ログイン直後や装備更新と同じtickの発射でも、旧フード抽選を重ねず最新値を使う。
        refreshHoodModifier(player);
        return Mth.clamp(player.getAttributeValue(ApprenticeAttributeRegistry.SPELLGUN_AMMO_CONSERVATION.get()), 0.0D, 1.0D);
    }

    public static boolean shouldSkip(ServerPlayer player) {
        var chance = consumeChance(player);
        return chance <= 0.0D || chance < 1.0D && player.getRandom().nextDouble() >= chance;
    }

    public static boolean shouldSkipManaCostWithAmmo() {
        return ApprenticeCodexServerConfig.magiAgentSuitSkipStaffrifleManaCostWhenAmmoNotConsumed();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player instanceof ServerPlayer player) {
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
