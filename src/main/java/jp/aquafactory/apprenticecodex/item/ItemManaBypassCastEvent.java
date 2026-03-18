package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ItemManaBypassCastEvent {
    private static final Map<UUID, Float> RESERVED_MANA = new ConcurrentHashMap<>();

    private ItemManaBypassCastEvent() {
    }

    public static void reserveBorrowedMana(ServerPlayer player, float borrowedMana) {
        RESERVED_MANA.put(player.getUUID(), borrowedMana);
    }

    public static void reserveBorrowedMana(net.minecraft.world.entity.player.Player player, float borrowedMana) {
        if (player instanceof ServerPlayer serverPlayer) {
            reserveBorrowedMana(serverPlayer, borrowedMana);
        }
    }

    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        var castingItem = magicData.getPlayerCastingItem();
        if (!(castingItem.getItem() instanceof ManaBypassSpellItem manaBypassItem)) {
            releaseBorrowedMana(player, magicData);
            return;
        }

        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(event.getSpellId());
        if (!manaBypassItem.supportsManaBypass(spell)) {
            releaseBorrowedMana(player, magicData);
            return;
        }

        event.setManaCost(0);
        releaseBorrowedMana(player, magicData);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var reservedMana = RESERVED_MANA.get(player.getUUID());
        if (reservedMana == null || reservedMana <= 0f) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || magicData.isCasting()) {
            return;
        }

        releaseBorrowedMana(player, magicData);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        releaseBorrowedMana(player, magicData);
    }

    private static void releaseBorrowedMana(ServerPlayer player, MagicData magicData) {
        var reservedMana = RESERVED_MANA.remove(player.getUUID());
        if (reservedMana == null || reservedMana <= 0f) {
            return;
        }

        magicData.setMana(Math.max(0f, magicData.getMana() - reservedMana));
    }
}
