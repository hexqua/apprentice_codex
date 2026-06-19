package jp.aquafactory.apprenticecodex.spell.edgedancer;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.network.EquipmentChangedPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.EdgeDancerState;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdge;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdgeMirror;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncEdgeDancerStatePacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.boundbow.BoundBowManager;
import jp.aquafactory.apprenticecodex.spell.boundsword.BoundSwordManager;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class EdgeDancerManager {
    private EdgeDancerManager() {
    }

    public static void activate(ServerPlayer player, int spellLevel, CastSource castSource, MagicData magicData,
                                EdgeDancer spell) {
        deactivate(player, true);
        BoundSwordManager.deactivate(player, true);
        BoundBowManager.deactivate(player, true);

        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null || !SpellSideEdge.isSpellSideEdge(player.getMainHandItem())) {
            return;
        }

        var instanceId = UUID.randomUUID();
        var originalOffhand = getPhysicalOffhandStack(player).copy();
        var mirror = SpellSideEdgeMirror.create(instanceId, player.getMainHandItem());
        if (!setPhysicalOffhandStack(player, mirror)) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.EDGE_DANCER_STATE, state -> {
            state.active = true;
            state.setInstanceId(instanceId);
            state.setStoredOffhandStack(originalOffhand);
            state.setHadStoredOffhand(!originalOffhand.isEmpty());
        });
        player.containerMenu.broadcastFullState();
        syncSpellSelection(player);

        var recastInstance = new RecastInstance(
                spell.getSpellId(),
                spellLevel,
                spell.getRecastCount(spellLevel, player),
                spell.getDuration(spellLevel, player),
                castSource,
                null
        );
        magicData.getPlayerRecasts().addRecast(recastInstance, magicData);
        syncToClient(player, spellData.get(CodexSpellStateTypeRegister.EDGE_DANCER_STATE));
    }

    public static boolean deactivate(ServerPlayer player, boolean removeRecast) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return false;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.EDGE_DANCER_STATE);
        if (!state.active) {
            removeStaleMirrors(player);
            syncToClient(player, state);
            return false;
        }

        var instanceId = state.getInstanceId();
        var storedOffhandStack = state.getStoredOffhandStack().copy();
        var hadStoredOffhand = state.hadStoredOffhand();
        removeGeneratedMirror(player, instanceId);
        spellData.edit(CodexSpellStateTypeRegister.EDGE_DANCER_STATE, EdgeDancerState::reset);

        if (hadStoredOffhand && !storedOffhandStack.isEmpty()) {
            restoreStoredOffhand(player, storedOffhandStack);
        }

        if (removeRecast) {
            removeActiveRecast(player);
        }

        player.containerMenu.broadcastFullState();
        syncSpellSelection(player);
        syncToClient(player, spellData.get(CodexSpellStateTypeRegister.EDGE_DANCER_STATE));
        return true;
    }

    public static void syncToClient(ServerPlayer player, EdgeDancerState state) {
        Networks.sendToPlayer(player, new SyncEdgeDancerStatePacket(
                state.active,
                state.getInstanceId(),
                state.getStoredOffhandStack(),
                state.hadStoredOffhand()
        ));
    }

    public static boolean handleContainerClick(ServerPlayer player, ServerboundContainerClickPacket packet) {
        if (player.containerMenu.containerId != packet.getContainerId()) {
            return false;
        }

        var menu = player.containerMenu;
        var slotId = packet.getSlotNum();
        var clickedPlayerInventorySlot = isPlayerInventorySlot(player, slotId);
        if (SpellSideEdgeMirror.isGeneratedMirror(menu.getCarried())) {
            if (clickedPlayerInventorySlot && isAllowedPlayerInventoryClick(packet.getClickType())) {
                return false;
            }
            deactivate(player, true);
            return true;
        }

        if (SpellSideEdge.isSpellSideEdge(menu.getCarried())) {
            if (hasActiveEdgeDancer(player) && packet.getClickType() == ClickType.THROW) {
                deactivate(player, true);
                return true;
            }
            deactivateIfActive(player);
            return false;
        }

        if (!menu.isValidSlotIndex(slotId) || slotId < 0 || slotId >= menu.slots.size()) {
            return false;
        }

        var clickedStack = menu.slots.get(slotId).getItem();
        if (SpellSideEdgeMirror.isGeneratedMirror(clickedStack)) {
            if (clickedPlayerInventorySlot && isAllowedPlayerInventoryClick(packet.getClickType())) {
                return false;
            }
            deactivate(player, true);
            return true;
        }

        if (SpellSideEdge.isSpellSideEdge(clickedStack)) {
            if (hasActiveEdgeDancer(player) && packet.getClickType() == ClickType.THROW) {
                deactivate(player, true);
                return true;
            }
            if (!(clickedPlayerInventorySlot && isAllowedPlayerInventoryClick(packet.getClickType()))) {
                deactivateIfActive(player);
            }
            return false;
        }

        if (packet.getClickType() == ClickType.SWAP) {
            var swapButtonStack = getSwapButtonStack(player, packet.getButtonNum());
            if (SpellSideEdgeMirror.isGeneratedMirror(swapButtonStack)) {
                if (clickedPlayerInventorySlot) {
                    return false;
                }
                deactivate(player, true);
                return true;
            }
            if (SpellSideEdge.isSpellSideEdge(swapButtonStack) && !clickedPlayerInventorySlot) {
                deactivateIfActive(player);
                return false;
            }
        }

        return false;
    }

    public static boolean handlePlayerAction(ServerPlayer player, ServerboundPlayerActionPacket packet) {
        var action = packet.getAction();
        if (action != ServerboundPlayerActionPacket.Action.DROP_ITEM
                && action != ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS
                && action != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
            return false;
        }

        if (action == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND && hasActiveEdgeDancer(player)) {
            deactivate(player, true);
            return true;
        }

        var selected = player.getInventory().getSelected();
        if (SpellSideEdgeMirror.isGeneratedMirror(selected)) {
            deactivate(player, true);
            return true;
        }
        if (SpellSideEdge.isSpellSideEdge(selected)) {
            if (hasActiveEdgeDancer(player)) {
                deactivate(player, true);
                return true;
            }
            return false;
        }

        return false;
    }

    public static void validateActiveMirrorLocation(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.EDGE_DANCER_STATE);
        if (!state.active) {
            removeStaleMirrors(player);
            return;
        }

        var instanceId = state.getInstanceId();
        if (instanceId == null || !hasAnySpellSideEdgeInPlayerInventory(player)
                || !hasExpectedMirrorInAllowedSlots(player, instanceId)
                || hasMirrorOutsideAllowedSlots(player, instanceId)) {
            deactivate(player, true);
        }
    }

    public static boolean hasActiveEdgeDancer(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        return spellData != null && spellData.get(CodexSpellStateTypeRegister.EDGE_DANCER_STATE).active;
    }

    private static void deactivateIfActive(ServerPlayer player) {
        if (hasActiveEdgeDancer(player)) {
            deactivate(player, true);
        }
    }

    private static boolean isAllowedPlayerInventoryClick(ClickType clickType) {
        return clickType == ClickType.PICKUP || clickType == ClickType.SWAP;
    }

    private static boolean isPlayerInventorySlot(ServerPlayer player, int slotId) {
        var menu = player.containerMenu;
        return menu.isValidSlotIndex(slotId)
                && slotId >= 0
                && slotId < menu.slots.size()
                && menu.slots.get(slotId).container == player.getInventory();
    }

    private static ItemStack getSwapButtonStack(ServerPlayer player, int buttonNum) {
        var inventory = player.getInventory();
        if (buttonNum >= 0 && buttonNum <= 8) {
            return inventory.items.get(buttonNum);
        }
        if (buttonNum == 40 && !inventory.offhand.isEmpty()) {
            return inventory.offhand.get(0);
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack getPhysicalOffhandStack(ServerPlayer player) {
        var offhand = player.getInventory().offhand;
        return offhand.isEmpty() ? ItemStack.EMPTY : offhand.get(0);
    }

    private static boolean setPhysicalOffhandStack(ServerPlayer player, ItemStack stack) {
        var offhand = player.getInventory().offhand;
        if (offhand.isEmpty()) {
            return false;
        }

        offhand.set(0, stack);
        player.getInventory().setChanged();
        return true;
    }

    private static boolean hasAnySpellSideEdgeInPlayerInventory(ServerPlayer player) {
        var inventory = player.getInventory();
        for (var slot = 0; slot < inventory.items.size(); ++slot) {
            if (SpellSideEdge.isSpellSideEdge(inventory.items.get(slot))) {
                return true;
            }
        }
        return SpellSideEdge.isSpellSideEdge(player.containerMenu.getCarried());
    }

    private static boolean hasExpectedMirrorInAllowedSlots(ServerPlayer player, UUID instanceId) {
        var inventory = player.getInventory();
        for (var slot = 0; slot < inventory.items.size(); ++slot) {
            if (SpellSideEdgeMirror.hasInstanceId(inventory.items.get(slot), instanceId)) {
                return true;
            }
        }
        if (!inventory.offhand.isEmpty() && SpellSideEdgeMirror.hasInstanceId(inventory.offhand.get(0), instanceId)) {
            return true;
        }
        return SpellSideEdgeMirror.hasInstanceId(player.containerMenu.getCarried(), instanceId);
    }

    private static boolean hasMirrorOutsideAllowedSlots(ServerPlayer player, UUID instanceId) {
        var inventory = player.getInventory();
        for (var slot = 0; slot < inventory.armor.size(); ++slot) {
            if (SpellSideEdgeMirror.hasInstanceId(inventory.armor.get(slot), instanceId)) {
                return true;
            }
        }
        return false;
    }

    private static void removeGeneratedMirror(ServerPlayer player, UUID instanceId) {
        var inventory = player.getInventory();
        for (var slot = 0; slot < inventory.items.size(); ++slot) {
            if (SpellSideEdgeMirror.hasInstanceId(inventory.items.get(slot), instanceId)) {
                inventory.items.set(slot, ItemStack.EMPTY);
            }
        }
        for (var slot = 0; slot < inventory.offhand.size(); ++slot) {
            if (SpellSideEdgeMirror.hasInstanceId(inventory.offhand.get(slot), instanceId)) {
                inventory.offhand.set(slot, ItemStack.EMPTY);
            }
        }
        for (var slot = 0; slot < inventory.armor.size(); ++slot) {
            if (SpellSideEdgeMirror.hasInstanceId(inventory.armor.get(slot), instanceId)) {
                inventory.armor.set(slot, ItemStack.EMPTY);
            }
        }
        if (SpellSideEdgeMirror.hasInstanceId(player.containerMenu.getCarried(), instanceId)) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        inventory.setChanged();
    }

    private static void removeStaleMirrors(ServerPlayer player) {
        var inventory = player.getInventory();
        for (var slot = 0; slot < inventory.items.size(); ++slot) {
            if (inventory.items.get(slot).is(ItemRegistry.SPELL_SIDE_EDGE_MIRROR.get())
                    && SpellSideEdgeMirror.isGeneratedMirror(inventory.items.get(slot))) {
                inventory.items.set(slot, ItemStack.EMPTY);
            }
        }
        for (var slot = 0; slot < inventory.offhand.size(); ++slot) {
            if (inventory.offhand.get(slot).is(ItemRegistry.SPELL_SIDE_EDGE_MIRROR.get())
                    && SpellSideEdgeMirror.isGeneratedMirror(inventory.offhand.get(slot))) {
                inventory.offhand.set(slot, ItemStack.EMPTY);
            }
        }
        if (SpellSideEdgeMirror.isGeneratedMirror(player.containerMenu.getCarried())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        inventory.setChanged();
    }

    private static void restoreStoredOffhand(ServerPlayer player, ItemStack storedOffhandStack) {
        if (getPhysicalOffhandStack(player).isEmpty() && setPhysicalOffhandStack(player, storedOffhandStack)) {
            return;
        }

        player.getInventory().placeItemBackInInventory(storedOffhandStack);
        player.getInventory().setChanged();
    }

    private static void removeActiveRecast(ServerPlayer player) {
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        var recast = magicData.getPlayerRecasts().getRecastInstance(SpellRegistry.EDGE_DANCER.get().getSpellId());
        if (recast != null) {
            magicData.getPlayerRecasts().removeRecast(recast, RecastResult.USED_ALL_RECASTS);
        }
    }

    private static void syncSpellSelection(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new EquipmentChangedPacket());
    }
}
