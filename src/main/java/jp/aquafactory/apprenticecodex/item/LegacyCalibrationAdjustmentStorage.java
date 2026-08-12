package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitItem;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbook;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmulet;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmulet;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.revolvercaststaff.RevolvercastStaff;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshield;
import jp.aquafactory.apprenticecodex.item.shield.ParrycastBuckler;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShield;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

/**
 * 共通保存形式の導入前に作られた調整データだけを読み取る互換層。
 * 新しい調整対象はこのクラスへ形式を追加せず、{@link CalibrationAdjustmentStorage} のみを使用する。
 */
final class LegacyCalibrationAdjustmentStorage {
    private static final String ADJUSTMENTS_TAG = "Adjustments";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";

    private LegacyCalibrationAdjustmentStorage() {
    }

    static @NotNull ItemStack readLegacy(
            ItemStack owner,
            int slot,
            HolderLookup.Provider lookupProvider
    ) {
        // 後方互換としての対応のため、片っ端からinstanceofをしているのは許容.
        var item = owner.getItem();
        if (item instanceof ScrollcasterGauntlet
                || item instanceof RevolvercastStaff
                || item instanceof MithrilFreecastStaff) {
            return readLegacyFullStackList(owner, "SpellCalibration", slot, lookupProvider);
        }
        if (item instanceof ChargecastCatalystbook) {
            return readLegacyFullStackList(owner, "ChargecastCalibration", slot, lookupProvider);
        }
        if (item instanceof AutocastAmulet || item instanceof SatelliteFollowcastAmulet) {
            return readLegacyAmuletList(owner, slot);
        }
        if (item instanceof BulwarkGreatshield) {
            return readLegacyIdList(owner, "BulwarkGreatshieldCalibration", slot);
        }
        if (item instanceof ParrycastBuckler) {
            return readLegacyIdList(owner, "ParrycastBucklerCalibration", slot);
        }
        if (item instanceof ReflectcastShield) {
            return readLegacyIdList(owner, "ReflectcastShieldCalibration", slot);
        }
        if (item instanceof MagiAgentSuitItem) {
            return readLegacySingleId(owner, "MagiAgentSuitCalibration", "AdjustmentItem");
        }
        if (item instanceof AbstractSpellGunItem) {
            return readLegacySpellGun(owner, lookupProvider);
        }
        return ItemStack.EMPTY;
    }

    static void removeLegacy(ItemStack owner) {
        var item = owner.getItem();
        if (item instanceof ScrollcasterGauntlet
                || item instanceof RevolvercastStaff
                || item instanceof MithrilFreecastStaff
                || item instanceof AutocastAmulet
                || item instanceof SatelliteFollowcastAmulet) {
            removeLegacyField(owner, "SpellCalibration", ADJUSTMENTS_TAG);
        } else if (item instanceof ChargecastCatalystbook) {
            removeLegacyField(owner, "ChargecastCalibration", ADJUSTMENTS_TAG);
        } else if (item instanceof BulwarkGreatshield) {
            removeLegacyField(owner, "BulwarkGreatshieldCalibration", ADJUSTMENTS_TAG);
        } else if (item instanceof ParrycastBuckler) {
            removeLegacyField(owner, "ParrycastBucklerCalibration", ADJUSTMENTS_TAG);
        } else if (item instanceof ReflectcastShield) {
            removeLegacyField(owner, "ReflectcastShieldCalibration", ADJUSTMENTS_TAG);
        } else if (item instanceof MagiAgentSuitItem) {
            removeLegacyField(owner, "MagiAgentSuitCalibration", "AdjustmentItem");
        } else if (item instanceof AbstractSpellGunItem) {
            removeLegacySpellGun(owner);
        }
    }

    private static ItemStack readLegacyFullStackList(
            ItemStack owner,
            String rootName,
            int slot,
            HolderLookup.Provider lookupProvider
    ) {
        var entry = findLegacyListEntry(owner, rootName, slot);
        return entry == null || !entry.contains(ITEM_TAG, Tag.TAG_COMPOUND)
                ? ItemStack.EMPTY
                : ItemStack.parseOptional(lookupProvider, entry.getCompound(ITEM_TAG));
    }

    private static ItemStack readLegacyIdList(ItemStack owner, String rootName, int slot) {
        var entry = findLegacyListEntry(owner, rootName, slot);
        return entry == null ? ItemStack.EMPTY : createLegacyIdStack(entry.getString(ITEM_TAG));
    }

    private static ItemStack readLegacyAmuletList(ItemStack owner, int slot) {
        var entry = findLegacyListEntry(owner, "SpellCalibration", slot);
        if (entry == null || !entry.contains(ITEM_TAG, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        var item = entry.getCompound(ITEM_TAG);
        if (item.contains("Spell", Tag.TAG_STRING)) {
            var spellId = ResourceLocation.tryParse(item.getString("Spell"));
            var spell = spellId == null ? SpellRegistry.none() : SpellRegistry.getSpell(spellId);
            return spell == null || spell == SpellRegistry.none()
                    ? ItemStack.EMPTY
                    : SpellCalibrationImbueHelper.createScroll(new SpellData(spell, Math.max(1, item.getInt("Level"))));
        }
        return createLegacyIdStack(item.getString("ItemId"));
    }

    private static ItemStack readLegacySingleId(ItemStack owner, String rootName, String fieldName) {
        var root = getLegacyRoot(owner, rootName);
        return root == null ? ItemStack.EMPTY : createLegacyIdStack(root.getString(fieldName));
    }

    private static ItemStack readLegacySpellGun(ItemStack owner, HolderLookup.Provider lookupProvider) {
        var root = getLegacyRoot(owner, "SpellgunCalibration");
        if (root == null) {
            return ItemStack.EMPTY;
        }
        if (root.contains("Adjustment", Tag.TAG_COMPOUND)) {
            var stored = root.getCompound("Adjustment");
            var parsed = ItemStack.parseOptional(lookupProvider, stored);
            if (!parsed.isEmpty()) {
                return parsed;
            }
            var fallback = createLegacyIdStack(stored.getString("id"));
            if (!fallback.isEmpty()) {
                return fallback;
            }
        }
        return createLegacyIdStack(root.getString("AdjustmentItem"));
    }

    private static CompoundTag findLegacyListEntry(ItemStack owner, String rootName, int slot) {
        var root = getLegacyRoot(owner, rootName);
        if (root == null || !root.contains(ADJUSTMENTS_TAG, Tag.TAG_LIST)) {
            return null;
        }
        var adjustments = root.getList(ADJUSTMENTS_TAG, Tag.TAG_COMPOUND);
        for (var index = 0; index < adjustments.size(); ++index) {
            var entry = adjustments.getCompound(index);
            if (entry.getInt(SLOT_TAG) == slot) {
                return entry;
            }
        }
        return null;
    }

    private static ItemStack createLegacyIdStack(String value) {
        var id = ResourceLocation.tryParse(value);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        var item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static CompoundTag getLegacyRoot(ItemStack owner, String rootName) {
        var customData = owner.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        var root = customData.copyTag();
        return root.contains(rootName, Tag.TAG_COMPOUND) ? root.getCompound(rootName) : null;
    }

    private static void removeLegacyField(ItemStack owner, String rootName, String fieldName) {
        CustomData.update(DataComponents.CUSTOM_DATA, owner, root -> {
            if (!root.contains(rootName, Tag.TAG_COMPOUND)) {
                return;
            }
            var legacy = root.getCompound(rootName);
            legacy.remove(fieldName);
            if (legacy.isEmpty()) {
                root.remove(rootName);
            } else {
                root.put(rootName, legacy);
            }
        });
    }

    private static void removeLegacySpellGun(ItemStack owner) {
        CustomData.update(DataComponents.CUSTOM_DATA, owner, root -> {
            if (!root.contains("SpellgunCalibration", Tag.TAG_COMPOUND)) {
                return;
            }
            var legacy = root.getCompound("SpellgunCalibration");
            legacy.remove("Adjustment");
            legacy.remove("AdjustmentItem");
            if (legacy.isEmpty()) {
                root.remove("SpellgunCalibration");
            } else {
                root.put("SpellgunCalibration", legacy);
            }
        });
    }
}
