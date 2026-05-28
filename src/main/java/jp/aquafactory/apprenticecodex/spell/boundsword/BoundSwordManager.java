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
import jp.aquafactory.apprenticecodex.spell.boundbow.BoundBowManager;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.UUID;

public final class BoundSwordManager {
    private static final String BETTER_COMBAT_MOD_ID = "bettercombat";
    private static final String EPIC_FIGHT_MOD_ID = "epicfight";

    private BoundSwordManager() {
    }

    public static void activate(ServerPlayer player, int spellLevel, CastSource castSource, MagicData magicData,
                                BoundSword spell, float displayDamage) {
        activate(player, spellLevel, castSource, magicData, spell, displayDamage, false);
    }

    public static void activate(ServerPlayer player, int spellLevel, CastSource castSource, MagicData magicData,
                                BoundSword spell, float displayDamage, boolean forceTryDualWield) {
        deactivate(player, true);
        BoundBowManager.deactivate(player, true);

        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var instanceId = UUID.randomUUID();
        var originalMainhand = player.getMainHandItem().copy();
        var originalOffhand = getPhysicalOffhandStack(player).copy();
        var shouldGenerateOffhand = shouldGenerateOffhandSword(player, forceTryDualWield);
        var sword = BoundSwordItem.create(instanceId, displayDamage, EquipmentSlot.MAINHAND);
        var offhandSword = BoundSwordItem.create(instanceId, displayDamage, EquipmentSlot.OFFHAND);

        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        var offhandSwordGenerated = shouldGenerateOffhand && setPhysicalOffhandStack(player, offhandSword);

        spellData.edit(CodexSpellStateTypeRegister.BOUND_SWORD_STATE, state -> {
            state.active = true;
            state.setInstanceId(instanceId);
            state.setStoredMainhandStack(originalMainhand);
            state.setStoredOffhandStack(offhandSwordGenerated ? originalOffhand : ItemStack.EMPTY);
            state.setOffhandSwordGenerated(offhandSwordGenerated);
            state.displayDamage = displayDamage;
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
        var storedOffhandStack = state.getStoredOffhandStack().copy();
        var offhandSwordGenerated = state.isOffhandSwordGenerated();
        removeGeneratedSword(player, instanceId);
        spellData.edit(CodexSpellStateTypeRegister.BOUND_SWORD_STATE, BoundSwordState::reset);

        if (!storedMainhandStack.isEmpty()) {
            restoreStoredMainhand(player, storedMainhandStack);
        }
        if (offhandSwordGenerated && !storedOffhandStack.isEmpty()) {
            restoreStoredOffhand(player, storedOffhandStack);
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
                state.getStoredOffhandStack(),
                state.isOffhandSwordGenerated(),
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

        if (areExpectedSwordsInAllowedSlots(player, instanceId, state.isOffhandSwordGenerated())) {
            return;
        }

        deactivate(player, true);
    }

    private static boolean isAllowedDirectSwapButton(int buttonNum) {
        return (buttonNum >= 0 && buttonNum <= 8) || buttonNum == 40;
    }

    public static boolean hasDualWieldCompat() {
        return ModList.get().isLoaded(BETTER_COMBAT_MOD_ID) || ModList.get().isLoaded(EPIC_FIGHT_MOD_ID);
    }

    private static boolean shouldGenerateOffhandSword(ServerPlayer player, boolean forceTryDualWield) {
        if (!hasDualWieldCompat()) {
            return false;
        }

        // Better Combat 1.20.1 は両手武器中に getOffhandItem() を空へ見せるため、
        // 二刀流生成の可否は実インベントリの offhand スロットから判定する。
        return forceTryDualWield || getPhysicalOffhandStack(player).isEmpty();
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

    private static boolean areExpectedSwordsInAllowedSlots(ServerPlayer player, UUID instanceId,
                                                          boolean offhandSwordGenerated) {
        var expectedCount = offhandSwordGenerated ? 2 : 1;
        return countAllowedGeneratedSwords(player, instanceId) >= expectedCount
                && !hasGeneratedSwordOutsideAllowedSlots(player, instanceId);
    }

    private static int countAllowedGeneratedSwords(ServerPlayer player, UUID instanceId) {
        var count = 0;
        var inventory = player.getInventory();
        for (var slot = 0; slot < 9; ++slot) {
            if (BoundSwordItem.hasInstanceId(inventory.items.get(slot), instanceId)) {
                ++count;
            }
        }

        if (!inventory.offhand.isEmpty()
                && BoundSwordItem.hasInstanceId(inventory.offhand.get(0), instanceId)) {
            ++count;
        }
        return count;
    }

    private static boolean hasGeneratedSwordOutsideAllowedSlots(ServerPlayer player, UUID instanceId) {
        var inventory = player.getInventory();
        for (var slot = 9; slot < inventory.items.size(); ++slot) {
            if (BoundSwordItem.hasInstanceId(inventory.items.get(slot), instanceId)) {
                return true;
            }
        }
        for (var slot = 0; slot < inventory.armor.size(); ++slot) {
            if (BoundSwordItem.hasInstanceId(inventory.armor.get(slot), instanceId)) {
                return true;
            }
        }
        return BoundSwordItem.hasInstanceId(player.containerMenu.getCarried(), instanceId);
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

        var recast = magicData.getPlayerRecasts().getRecastInstance(SpellRegistry.BOUND_SWORD.get().getSpellId());
        if (recast != null) {
            magicData.getPlayerRecasts().removeRecast(recast, RecastResult.USED_ALL_RECASTS);
        }
    }
}
