package jp.aquafactory.apprenticecodex.spell.boundbow;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.network.EquipmentChangedPacket;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.BoundBowState;
import jp.aquafactory.apprenticecodex.item.boundweapon.BoundBowItem;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncBoundBowStatePacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.boundsword.BoundSwordManager;
import jp.aquafactory.apprenticecodex.spell.edgedancer.EdgeDancerManager;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public final class BoundBowManager {
    private BoundBowManager() {
    }

    public static void activate(ServerPlayer player, int spellLevel, CastSource castSource, MagicData magicData,
                                BoundBow spell, int powerLevel) {
        activate(player, spellLevel, castSource, magicData, spell, powerLevel, 1.0F);
    }

    public static void activate(ServerPlayer player, int spellLevel, CastSource castSource, MagicData magicData,
                                BoundBow spell, int powerLevel, float summonDamageMultiplier) {
        deactivate(player, true);
        BoundSwordManager.deactivate(player, true);
        EdgeDancerManager.deactivate(player, true);

        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var instanceId = UUID.randomUUID();
        var originalMainhand = player.getMainHandItem().copy();
        var bow = BoundBowItem.create(instanceId, powerLevel, player.registryAccess(), summonDamageMultiplier);

        player.setItemInHand(InteractionHand.MAIN_HAND, bow);
        spellData.edit(CodexSpellStateTypeRegister.BOUND_BOW_STATE, state -> {
            state.active = true;
            state.setInstanceId(instanceId);
            state.setStoredMainhandStack(originalMainhand);
            state.powerLevel = powerLevel;
        });
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
        syncToClient(player, spellData.get(CodexSpellStateTypeRegister.BOUND_BOW_STATE));
    }

    public static boolean deactivate(ServerPlayer player, boolean removeRecast) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return false;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.BOUND_BOW_STATE);
        if (!state.active) {
            removeStaleGeneratedBows(player);
            syncToClient(player, state);
            return false;
        }

        var instanceId = state.getInstanceId();
        var storedMainhandStack = state.getStoredMainhandStack().copy();
        removeGeneratedBow(player, instanceId);
        spellData.edit(CodexSpellStateTypeRegister.BOUND_BOW_STATE, BoundBowState::reset);

        if (!storedMainhandStack.isEmpty()) {
            restoreStoredMainhand(player, storedMainhandStack);
        }
        if (removeRecast) {
            removeActiveRecast(player);
        }

        player.containerMenu.broadcastFullState();
        syncSpellSelection(player);
        syncToClient(player, spellData.get(CodexSpellStateTypeRegister.BOUND_BOW_STATE));
        return true;
    }

    public static void syncToClient(ServerPlayer player, BoundBowState state) {
        Networks.sendToPlayer(player, new SyncBoundBowStatePacket(
                state.active,
                state.getInstanceId(),
                state.getStoredMainhandStack(),
                state.powerLevel
        ));
    }

    public static boolean handleContainerClick(ServerPlayer player, ServerboundContainerClickPacket packet) {
        if (player.containerMenu.containerId != packet.getContainerId()) {
            return false;
        }

        var menu = player.containerMenu;
        var slotId = packet.getSlotNum();
        var clickedPlayerInventorySlot = isPlayerInventorySlot(player, slotId);
        if (BoundBowItem.isGeneratedBoundBow(menu.getCarried())) {
            if (clickedPlayerInventorySlot && isAllowedPlayerInventoryClick(packet.getClickType())) {
                return false;
            }
            deactivate(player, true);
            return true;
        }

        if (!menu.isValidSlotIndex(slotId) || slotId < 0 || slotId >= menu.slots.size()) {
            return false;
        }

        var clickedStack = menu.slots.get(slotId).getItem();
        if (BoundBowItem.isGeneratedBoundBow(clickedStack)) {
            if (clickedPlayerInventorySlot && isAllowedPlayerInventoryClick(packet.getClickType())) {
                return false;
            }
            deactivate(player, true);
            return true;
        }

        if (packet.getClickType() == ClickType.SWAP
                && BoundBowItem.isGeneratedBoundBow(getSwapButtonStack(player, packet.getButtonNum()))) {
            if (clickedPlayerInventorySlot) {
                return false;
            }
            deactivate(player, true);
            return true;
        }

        return false;
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

    public static boolean handlePlayerAction(ServerPlayer player, ServerboundPlayerActionPacket packet) {
        var action = packet.getAction();
        if (action != ServerboundPlayerActionPacket.Action.DROP_ITEM
                && action != ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS) {
            return false;
        }
        if (!BoundBowItem.isGeneratedBoundBow(player.getInventory().getSelected())) {
            return false;
        }

        deactivate(player, true);
        return true;
    }

    public static void validateActiveBowLocation(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.BOUND_BOW_STATE);
        if (!state.active) {
            removeStaleGeneratedBows(player);
            return;
        }

        var instanceId = state.getInstanceId();
        if (instanceId == null) {
            deactivate(player, true);
            return;
        }

        if (countAllowedGeneratedBows(player, instanceId) >= 1
                && !hasGeneratedBowOutsideAllowedSlots(player, instanceId)) {
            return;
        }

        deactivate(player, true);
    }

    private static void syncSpellSelection(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new EquipmentChangedPacket());
    }

    private static int countAllowedGeneratedBows(ServerPlayer player, UUID instanceId) {
        var count = 0;
        var inventory = player.getInventory();
        for (var slot = 0; slot < inventory.items.size(); ++slot) {
            if (BoundBowItem.hasInstanceId(inventory.items.get(slot), instanceId)) {
                ++count;
            }
        }

        if (!inventory.offhand.isEmpty()
                && BoundBowItem.hasInstanceId(inventory.offhand.get(0), instanceId)) {
            ++count;
        }
        if (BoundBowItem.hasInstanceId(player.containerMenu.getCarried(), instanceId)) {
            ++count;
        }
        return count;
    }

    private static boolean hasGeneratedBowOutsideAllowedSlots(ServerPlayer player, UUID instanceId) {
        var inventory = player.getInventory();
        for (var slot = 0; slot < inventory.armor.size(); ++slot) {
            if (BoundBowItem.hasInstanceId(inventory.armor.get(slot), instanceId)) {
                return true;
            }
        }
        return false;
    }

    private static void removeGeneratedBow(ServerPlayer player, UUID instanceId) {
        var inventory = player.getInventory();
        for (var slot = 0; slot < inventory.items.size(); ++slot) {
            if (BoundBowItem.hasInstanceId(inventory.items.get(slot), instanceId)) {
                inventory.items.set(slot, ItemStack.EMPTY);
            }
        }
        for (var slot = 0; slot < inventory.offhand.size(); ++slot) {
            if (BoundBowItem.hasInstanceId(inventory.offhand.get(slot), instanceId)) {
                inventory.offhand.set(slot, ItemStack.EMPTY);
            }
        }
        for (var slot = 0; slot < inventory.armor.size(); ++slot) {
            if (BoundBowItem.hasInstanceId(inventory.armor.get(slot), instanceId)) {
                inventory.armor.set(slot, ItemStack.EMPTY);
            }
        }
        if (BoundBowItem.hasInstanceId(player.containerMenu.getCarried(), instanceId)) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        inventory.setChanged();
    }

    private static void removeStaleGeneratedBows(ServerPlayer player) {
        var inventory = player.getInventory();
        for (var slot = 0; slot < inventory.items.size(); ++slot) {
            if (inventory.items.get(slot).is(ItemRegistry.BOUND_BOW.get())
                    && BoundBowItem.isGeneratedBoundBow(inventory.items.get(slot))) {
                inventory.items.set(slot, ItemStack.EMPTY);
            }
        }
        for (var slot = 0; slot < inventory.offhand.size(); ++slot) {
            if (inventory.offhand.get(slot).is(ItemRegistry.BOUND_BOW.get())
                    && BoundBowItem.isGeneratedBoundBow(inventory.offhand.get(slot))) {
                inventory.offhand.set(slot, ItemStack.EMPTY);
            }
        }
        if (BoundBowItem.isGeneratedBoundBow(player.containerMenu.getCarried())) {
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

        var recast = magicData.getPlayerRecasts().getRecastInstance(SpellRegistry.BOUND_BOW.get().getSpellId());
        if (recast != null) {
            magicData.getPlayerRecasts().removeRecast(recast, RecastResult.USED_ALL_RECASTS);
        }
    }
}
