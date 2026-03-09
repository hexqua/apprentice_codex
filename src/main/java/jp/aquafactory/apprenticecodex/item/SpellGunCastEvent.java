package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SpellGunCastEvent {
    private static final Map<UUID, Float> RESERVED_MANA = new ConcurrentHashMap<>();

    private SpellGunCastEvent() {
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
        if (!(castingItem.getItem() instanceof AbstractSpellGunItem spellGunItem)) {
            releaseBorrowedMana(player, magicData);
            return;
        }

        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(event.getSpellId());
        if (!spellGunItem.supportsManaBypass(spell)) {
            releaseBorrowedMana(player, magicData);
            return;
        }

        event.setManaCost(0);
        releaseBorrowedMana(player, magicData);
        if (!player.isCreative()) {
            var ammoItem = spellGunItem.getAmmoItem(castingItem, spellGunItem.getPrimarySpellData(castingItem));
            if (ammoItem != null) {
                consumeAmmo(player.getInventory(), ammoItem);
            }
        }
    }

    @SubscribeEvent
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        var castingItem = magicData.getPlayerCastingItem();
        if (!(castingItem.getItem() instanceof AbstractSpellGunItem spellGunItem)) {
            return;
        }

        var overriddenCooldown = spellGunItem.getOverriddenCooldownTicks();
        if (overriddenCooldown == null) {
            return;
        }

        event.setEffectiveCooldown(overriddenCooldown);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
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

    public static boolean hasAmmo(Inventory inventory, Item ammoItem) {
        return containsAmmo(inventory.items, ammoItem) || containsAmmo(inventory.offhand, ammoItem);
    }

    private static void consumeAmmo(Inventory inventory, Item ammoItem) {
        if (consumeOne(inventory.items, ammoItem)) {
            return;
        }

        consumeOne(inventory.offhand, ammoItem);
    }

    private static boolean containsAmmo(java.util.List<ItemStack> stacks, Item ammoItem) {
        for (var stack : stacks) {
            if (stack.is(ammoItem)) {
                return true;
            }
        }

        return false;
    }

    private static boolean consumeOne(java.util.List<ItemStack> stacks, Item ammoItem) {
        for (var stack : stacks) {
            if (!stack.is(ammoItem)) {
                continue;
            }

            stack.shrink(1);
            return true;
        }

        return false;
    }
}
