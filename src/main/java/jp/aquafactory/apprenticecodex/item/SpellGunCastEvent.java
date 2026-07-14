package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitEffects;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch.SpellcasterAmmoPouch;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SpellGunCastEvent {
    private SpellGunCastEvent() {
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
            return;
        }

        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(event.getSpellId());
        if (!spellGunItem.supportsManaBypass(spell)) {
            return;
        }

        if (spellGunItem.isRecastCast(magicData, spell)) {
            return;
        }

        if (!player.isCreative()) {
            var ammoItem = spellGunItem.getAmmoItem(castingItem, spellGunItem.getPrimarySpellData(castingItem));
            if (ammoItem != null) {
                if (MagiAgentSuitEffects.shouldSkipAmmoConsumption(player)) {
                    return;
                }
                consumeAmmo(player, player.getInventory(), ammoItem, spellGunItem);
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

    public static boolean hasAmmo(Player player, Inventory inventory, Item ammoItem) {
        return countAvailableAmmo(player, inventory, ammoItem) > 0;
    }

    public static int countAvailableAmmo(Player player, Inventory inventory, Item ammoItem) {
        return SpellcasterAmmoPouch.countAmmoInAccessiblePouches(player, ammoItem)
                + countAmmo(inventory.items, ammoItem)
                + countAmmo(inventory.offhand, ammoItem);
    }

    public static void consumeAmmo(ServerPlayer player, Inventory inventory, Item ammoItem, AbstractSpellGunItem spellGunItem) {
        consumeAmmo(player, inventory, ammoItem, spellGunItem::shouldReturnEmptyCasing);
    }

    public static void consumeAmmo(ServerPlayer player, Inventory inventory, Item ammoItem, MultipurposeStaffrifle staffrifle) {
        consumeAmmo(player, inventory, ammoItem, staffrifle::shouldReturnEmptyCasing);
    }

    private static void consumeAmmo(ServerPlayer player, Inventory inventory, Item ammoItem, EmptyCasingReturnPolicy returnPolicy) {
        if (SpellcasterAmmoPouch.consumeAmmoFromAccessiblePouches(player, ammoItem)) {
            tryGiveEmptyCasing(player, inventory, ammoItem, returnPolicy);
            return;
        }

        if (consumeOne(inventory.items, ammoItem)) {
            tryGiveEmptyCasing(player, inventory, ammoItem, returnPolicy);
            return;
        }

        if (consumeOne(inventory.offhand, ammoItem)) {
            tryGiveEmptyCasing(player, inventory, ammoItem, returnPolicy);
        }
    }

    private static int countAmmo(java.util.List<ItemStack> stacks, Item ammoItem) {
        var total = 0;
        for (var stack : stacks) {
            if (stack.is(ammoItem)) {
                total += stack.getCount();
            }
        }
        return total;
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

    private static void tryGiveEmptyCasing(ServerPlayer player, Inventory inventory, Item ammoItem, EmptyCasingReturnPolicy returnPolicy) {
        if (!(ammoItem instanceof SpellcasterRoundItem roundItem)) {
            return;
        }

        var emptyCasingItem = roundItem.getEmptyCasingItem();
        if (emptyCasingItem == null || !returnPolicy.shouldReturnEmptyCasing(player)) {
            return;
        }

        var emptyCasingStack = new ItemStack(emptyCasingItem);
        SpellcasterAmmoPouch.storeInAccessiblePouches(player, emptyCasingStack);
        if (!inventory.add(emptyCasingStack) && !emptyCasingStack.isEmpty()) {
            player.drop(emptyCasingStack, false);
        }
    }

    @FunctionalInterface
    private interface EmptyCasingReturnPolicy {
        boolean shouldReturnEmptyCasing(Player player);
    }
}
