package jp.aquafactory.apprenticecodex.item.curios.undyingemblem;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.network.EquipmentChangedPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class UndyingEmblemEvents {
    private UndyingEmblemEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !isEligibleDamage(event)
                || !UndyingEmblemRuntime.isEquipped(player)
                || UndyingEmblemRuntime.isOnCooldown(player)) {
            return;
        }

        // 効果処理中に別の死亡判定へ再入しても二重発動しないよう、最初にクールダウンを確定する.
        UndyingEmblemRuntime.startCooldown(player);
        event.setCanceled(true);
        player.setHealth(1.0F);
        // 1.20.1の不死のトーテムと同じく全効果を解除する。
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 45 * 20, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40 * 20, 0));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 5 * 20, 1));
        player.level().broadcastEntityEvent(player, (byte) 35);
    }

    private static boolean isEligibleDamage(LivingDeathEvent event) {
        var source = event.getSource();
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellSelection(SpellSelectionManager.SpellSelectionEvent event) {
        var player = event.getEntity();
        if (!UndyingEmblemRuntime.isEquipped(player)
                || !isOnCooldown(player)) {
            return;
        }
        event.addSelectionOption(
                new SpellData(SpellRegistry.IDOL_RECONSTRUCTION.get(), 1),
                UndyingEmblemRuntime.SPELL_SELECTION_SLOT,
                0
        );
    }

    private static boolean isOnCooldown(Player player) {
        return player.level().isClientSide
                ? UndyingEmblemClientState.getRemainingCooldownTicks() > 0
                : UndyingEmblemRuntime.isOnCooldown(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            UndyingEmblemRuntime.tickCooldown(player);
        }
    }

    @SubscribeEvent
    public static void onCurioChanged(CurioChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || (!(event.getFrom().getItem() instanceof UndyingEmblem)
                && !(event.getTo().getItem() instanceof UndyingEmblem))) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new EquipmentChangedPacket());
        if (!UndyingEmblemRuntime.isEquipped(player)) {
            UndyingEmblemRuntime.cancelReconstructionCast(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sync(event);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event);
    }

    private static void sync(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UndyingEmblemRuntime.sync(player);
            // 保存済みクールダウンの有無を、ログイン・respawn・dimension変更後の選択一覧へ反映する.
            PacketDistributor.sendToPlayer(player, new EquipmentChangedPacket());
        }
    }

    @Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            UndyingEmblemClientState.reset();
            UndyingEmblemConfigState.reset();
        }
    }
}
