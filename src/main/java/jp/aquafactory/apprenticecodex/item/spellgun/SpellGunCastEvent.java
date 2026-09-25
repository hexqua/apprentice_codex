package jp.aquafactory.apprenticecodex.item.spellgun;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.SpellcasterRoundItem;
import jp.aquafactory.apprenticecodex.item.ammo.EmptyCasingReturnPolicy;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitEffects;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch.SpellcasterAmmoPouch;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

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

        var spell = SpellRegistry.getSpell(event.getSpellId());
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

        var completion = SpellgunRecastCompletion.find(player, event.getSpell());
        if (completion != null && completion.cooldown() != null) {
            // 別の武器を構えていても、遅延CDへその武器の固定CDや短縮を適用しない。
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
        if (overriddenCooldown != null) {
            // 固定値は最終値ではなく基準値として扱い、Iron's の SWORD 倍率を重ねず Attribute 短縮だけを適用する。
            event.setEffectiveCooldown(WeaponImbueCooldownHelper.applyCooldownReductionAttribute(
                    overriddenCooldown,
                    player
            ));
            return;
        }

        var adjustedCooldown = spellGunItem.getAdjustedCooldownTicks(event.getEffectiveCooldown());
        if (adjustedCooldown == null) {
            return;
        }

        event.setEffectiveCooldown(adjustedCooldown);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInstantCastTimeCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var completion = SpellgunRecastCompletion.find(player, event.getSpell());
        if (completion != null && completion.cooldown() != null) {
            var policy = completion.cooldown();
            if (!policy.fullauto()) event.setEffectiveCooldown(event.getEffectiveCooldown() + policy.castTime());
            return;
        }
        var magic = MagicData.getPlayerMagicData(player);
        if (magic == null || !(magic.getPlayerCastingItem().getItem() instanceof AbstractSpellGunItem gun)
                || !gun.addsInstantCastTimeToCooldown() || event.getSpell().getCastType() != CastType.LONG) return;
        // 装備によるCD短縮が加算分を吸収しないよう、魔法の実効CDを確定してから加える。
        event.setEffectiveCooldown(event.getEffectiveCooldown()
                + event.getSpell().getEffectiveCastTime(Math.max(1, magic.getCastingSpellLevel()), player));
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
    public static void consumeAmmo(ServerPlayer player, Inventory inventory, Item ammoItem, FullautoRapidcastSpellrifle staffrifle) {
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

    private static int countAmmo(List<ItemStack> stacks, Item ammoItem) {
        var total = 0;
        for (var stack : stacks) {
            if (stack.is(ammoItem)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static boolean consumeOne(List<ItemStack> stacks, Item ammoItem) {
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
