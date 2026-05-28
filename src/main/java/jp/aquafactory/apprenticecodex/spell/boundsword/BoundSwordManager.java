package jp.aquafactory.apprenticecodex.spell.boundsword;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.network.EquipmentChangedPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.BoundSwordState;
import jp.aquafactory.apprenticecodex.item.BoundSwordItem;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncBoundSwordStatePacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class BoundSwordManager {
    private BoundSwordManager() {
    }

    public static void activate(ServerPlayer player, int spellLevel, CastSource castSource, MagicData magicData,
                                BoundSword spell, float displayDamage) {
        deactivate(player, true);

        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var instanceId = UUID.randomUUID();
        var originalMainhand = player.getMainHandItem().copy();
        var sword = BoundSwordItem.create(instanceId, displayDamage);

        spellData.edit(CodexSpellStateTypeRegister.BOUND_SWORD_STATE, state -> {
            state.active = true;
            state.setInstanceId(instanceId);
            state.setStoredMainhandStack(originalMainhand);
            state.displayDamage = displayDamage;
        });
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
        player.containerMenu.broadcastFullState();
        syncSpellSelection(player);

        var recastInstance = new RecastInstance(
                spell.getSpellId(),
                spellLevel,
                spell.getRecastCount(spellLevel, player),
                spell.getDuration(),
                castSource,
                null
        );
        magicData.getPlayerRecasts().addRecast(recastInstance, magicData);
        syncToClient(player, spellData.get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE));
    }

    public static boolean deactivate(ServerPlayer player, boolean removeRecast) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return false;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE);
        if (!state.active) {
            removeStaleGeneratedSwords(player);
            syncToClient(player, state);
            return false;
        }

        var instanceId = state.getInstanceId();
        var storedMainhandStack = state.getStoredMainhandStack().copy();
        removeGeneratedSword(player, instanceId);
        spellData.edit(CodexSpellStateTypeRegister.BOUND_SWORD_STATE, BoundSwordState::reset);

        if (!storedMainhandStack.isEmpty()) {
            restoreStoredMainhand(player, storedMainhandStack);
        }

        if (removeRecast) {
            removeActiveRecast(player);
        }

        player.containerMenu.broadcastFullState();
        syncSpellSelection(player);
        syncToClient(player, spellData.get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE));
        return true;
    }

    public static void syncToClient(ServerPlayer player, BoundSwordState state) {

        Networks.sendToPlayer(player, new SyncBoundSwordStatePacket(
                state.active,
                state.getInstanceId(),
                state.getStoredMainhandStack(),
                state.displayDamage
        ));
    }

    private static void syncSpellSelection(ServerPlayer player) {

        // Iron's 1.20.1 側の装備変更検知を待つと、Imbue済みメインハンドを即時置換した時に
        // クライアントの魔法選択が古いまま残ることがあるため、差し替え直後に明示的に再構築させる。
        PacketDistributor.sendToPlayer(player, new EquipmentChangedPacket());
    }

    public static boolean handleContainerClick(ServerPlayer player, ServerboundContainerClickPacket packet) {
        if (player.containerMenu.containerId != packet.getContainerId()) {
            return false;
        }

        var menu = player.containerMenu;
        if (BoundSwordItem.isGeneratedBoundSword(menu.getCarried())) {
            deactivate(player, true);
            return true;
        }

        var slotId = packet.getSlotNum();
        if (!menu.isValidSlotIndex(slotId) || slotId < 0 || slotId >= menu.slots.size()) {
            return false;
        }

        var clickedStack = menu.slots.get(slotId).getItem();
        if (!BoundSwordItem.isGeneratedBoundSword(clickedStack)) {
            return false;
        }

        if (packet.getClickType() == ClickType.SWAP && isAllowedDirectSwapButton(packet.getButtonNum())) {
            return false;
        }

        deactivate(player, true);
        return true;
    }

    public static boolean handlePlayerAction(ServerPlayer player, ServerboundPlayerActionPacket packet) {
        var action = packet.getAction();
        if (action != ServerboundPlayerActionPacket.Action.DROP_ITEM
                && action != ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS) {
            return false;
        }
        if (!BoundSwordItem.isGeneratedBoundSword(player.getInventory().getSelected())) {
            return false;
        }

        deactivate(player, true);
        return true;
    }

    public static void validateActiveSwordLocation(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.BOUND_SWORD_STATE);
        if (!state.active) {
            removeStaleGeneratedSwords(player);
            return;
        }

        var instanceId = state.getInstanceId();
        if (instanceId == null) {
            deactivate(player, true);
            return;
        }

        if (isInAllowedSlot(player, instanceId)) {
            return;
        }

        deactivate(player, true);
    }

    private static boolean isAllowedDirectSwapButton(int buttonNum) {
        return (buttonNum >= 0 && buttonNum <= 8) || buttonNum == 40;
    }

    private static boolean isInAllowedSlot(ServerPlayer player, UUID instanceId) {
        var inventory = player.getInventory();
        for (var slot = 0; slot < 9; ++slot) {
            if (BoundSwordItem.hasInstanceId(inventory.items.get(slot), instanceId)) {
                return true;
            }
        }

        return !inventory.offhand.isEmpty()
                && BoundSwordItem.hasInstanceId(inventory.offhand.get(0), instanceId);
    }

    private static void removeGeneratedSword(ServerPlayer player, UUID instanceId) {
        var inventory = player.getInventory();
        for (var slot = 0; slot < inventory.items.size(); ++slot) {
            if (BoundSwordItem.hasInstanceId(inventory.items.get(slot), instanceId)) {
                inventory.items.set(slot, ItemStack.EMPTY);
            }
        }
        for (var slot = 0; slot < inventory.offhand.size(); ++slot) {
            if (BoundSwordItem.hasInstanceId(inventory.offhand.get(slot), instanceId)) {
                inventory.offhand.set(slot, ItemStack.EMPTY);
            }
        }
        for (var slot = 0; slot < inventory.armor.size(); ++slot) {
            if (BoundSwordItem.hasInstanceId(inventory.armor.get(slot), instanceId)) {
                inventory.armor.set(slot, ItemStack.EMPTY);
            }
        }
        if (BoundSwordItem.hasInstanceId(player.containerMenu.getCarried(), instanceId)) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        inventory.setChanged();
    }

    private static void removeStaleGeneratedSwords(ServerPlayer player) {
        var inventory = player.getInventory();
        for (var slot = 0; slot < inventory.items.size(); ++slot) {
            if (inventory.items.get(slot).is(ItemRegistry.BOUND_SWORD.get())
                    && BoundSwordItem.isGeneratedBoundSword(inventory.items.get(slot))) {
                inventory.items.set(slot, ItemStack.EMPTY);
            }
        }
        for (var slot = 0; slot < inventory.offhand.size(); ++slot) {
            if (inventory.offhand.get(slot).is(ItemRegistry.BOUND_SWORD.get())
                    && BoundSwordItem.isGeneratedBoundSword(inventory.offhand.get(slot))) {
                inventory.offhand.set(slot, ItemStack.EMPTY);
            }
        }
        if (BoundSwordItem.isGeneratedBoundSword(player.containerMenu.getCarried())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        inventory.setChanged();
    }

    private static void restoreStoredMainhand(ServerPlayer player, ItemStack storedMainhandStack) {
        var selectedSlot = player.getInventory().selected;
        if (player.getInventory().items.get(selectedSlot).isEmpty()) {
            player.getInventory().items.set(selectedSlot, storedMainhandStack);
        } else {
            player.getInventory().placeItemBackInInventory(storedMainhandStack);
        }
        player.getInventory().setChanged();
    }

    private static void removeActiveRecast(ServerPlayer player) {
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        var recast = magicData.getPlayerRecasts().getRecastInstance(SpellRegistry.BOUND_SWORD.get().getSpellId());
        if (recast != null) {
            magicData.getPlayerRecasts().removeRecast(recast, RecastResult.USED_ALL_RECASTS);
        }
    }
}
