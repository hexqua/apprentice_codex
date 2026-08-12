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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    static @NotNull ItemStack readLegacy(ItemStack owner, int slot) {
        // 後方互換としての対応のため、旧形式を所有していた Item を列挙する。
        var item = owner.getItem();
        if (item instanceof ScrollcasterGauntlet
                || item instanceof RevolvercastStaff
                || item instanceof MithrilFreecastStaff
                || item instanceof AutocastAmulet
                || item instanceof SatelliteFollowcastAmulet) {
            return readLegacyFullStackList(owner, "SpellCalibration", slot);
        }
        if (item instanceof ChargecastCatalystbook) {
            return readLegacyFullStackList(owner, "ChargecastCalibration", slot);
        }
        if (item instanceof BulwarkGreatshield) {
            var adjustment = readLegacyFullStackList(owner, "BulwarkGreatshieldCalibration", slot);
            return adjustment.isEmpty() && slot == 0
                    ? readLegacySingleStack(owner, "BulwarkGreatshieldCalibration", "Adjustment")
                    : adjustment;
        }
        if (item instanceof ParrycastBuckler) {
            return readLegacyFullStackList(owner, "ParrycastBucklerCalibration", slot);
        }
        if (item instanceof ReflectcastShield) {
            return readLegacyFullStackList(owner, "ReflectcastShieldCalibration", slot);
        }
        if (item instanceof MagiAgentSuitItem) {
            return readLegacySingleStack(owner, "MagiAgentSuitCalibration", "Adjustment", "AdjustmentItem");
        }
        if (item instanceof AbstractSpellGunItem) {
            return readLegacySingleStack(owner, "SpellgunCalibration", "Adjustment", "AdjustmentItem");
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
            removeLegacyFields(owner, "SpellCalibration", ADJUSTMENTS_TAG);
        } else if (item instanceof ChargecastCatalystbook) {
            removeLegacyFields(owner, "ChargecastCalibration", ADJUSTMENTS_TAG);
        } else if (item instanceof BulwarkGreatshield) {
            removeLegacyFields(owner, "BulwarkGreatshieldCalibration", ADJUSTMENTS_TAG, "Adjustment");
        } else if (item instanceof ParrycastBuckler) {
            removeLegacyFields(owner, "ParrycastBucklerCalibration", ADJUSTMENTS_TAG);
        } else if (item instanceof ReflectcastShield) {
            removeLegacyFields(owner, "ReflectcastShieldCalibration", ADJUSTMENTS_TAG);
        } else if (item instanceof MagiAgentSuitItem) {
            removeLegacyFields(owner, "MagiAgentSuitCalibration", "Adjustment", "AdjustmentItem");
        } else if (item instanceof AbstractSpellGunItem) {
            removeLegacyFields(owner, "SpellgunCalibration", "Adjustment", "AdjustmentItem");
        }
    }

    private static ItemStack readLegacyFullStackList(ItemStack owner, String rootName, int slot) {
        var entry = findLegacyListEntry(owner, rootName, slot);
        if (entry == null || !entry.contains(ITEM_TAG, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        var stored = entry.getCompound(ITEM_TAG);
        var parsed = ItemStack.of(stored);
        if (!parsed.isEmpty()) {
            return parsed;
        }
        if (stored.contains("Spell", Tag.TAG_STRING)) {
            var spellId = ResourceLocation.tryParse(stored.getString("Spell"));
            var spell = spellId == null ? SpellRegistry.none() : SpellRegistry.getSpell(spellId);
            return spell == null || spell == SpellRegistry.none()
                    ? ItemStack.EMPTY
                    : SpellCalibrationImbueHelper.createScroll(
                            new SpellData(spell, Math.max(1, stored.getInt("Level")))
                    );
        }
        return createLegacyIdStack(stored.getString("ItemId"));
    }

    private static ItemStack readLegacySingleStack(
            ItemStack owner,
            String rootName,
            String fieldName,
            String... fallbackFieldNames
    ) {
        var root = getLegacyRoot(owner, rootName);
        if (root == null) {
            return ItemStack.EMPTY;
        }
        if (root.contains(fieldName, Tag.TAG_COMPOUND)) {
            var stored = root.getCompound(fieldName);
            var parsed = ItemStack.of(stored);
            return parsed.isEmpty() ? createLegacyIdStack(stored.getString("id")) : parsed;
        }
        var idOnly = createLegacyIdStack(root.getString(fieldName));
        if (!idOnly.isEmpty()) {
            return idOnly;
        }
        for (var fallbackFieldName : fallbackFieldNames) {
            var fallback = createLegacyIdStack(root.getString(fallbackFieldName));
            if (!fallback.isEmpty()) {
                return fallback;
            }
        }
        return ItemStack.EMPTY;
    }

    private static CompoundTag findLegacyListEntry(ItemStack owner, String rootName, int slot) {
        var root = getLegacyRoot(owner, rootName);
        if (root == null || !root.contains(ADJUSTMENTS_TAG, Tag.TAG_LIST)) {
            return null;
        }
        var adjustments = root.getList(ADJUSTMENTS_TAG, Tag.TAG_COMPOUND);
        for (var index = 0; index < adjustments.size(); ++index) {
            var entry = adjustments.getCompound(index);
            if (entry.getInt(SLOT_TAG) != slot) {
                continue;
            }
            if (entry.contains(ITEM_TAG, Tag.TAG_STRING)) {
                var migrated = entry.copy();
                var stack = createLegacyIdStack(entry.getString(ITEM_TAG));
                if (!stack.isEmpty()) {
                    migrated.put(ITEM_TAG, stack.save(new CompoundTag()));
                }
                return migrated;
            }
            return entry;
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
        var root = owner.getTag();
        return root != null && root.contains(rootName, Tag.TAG_COMPOUND)
                ? root.getCompound(rootName)
                : null;
    }

    private static void removeLegacyFields(ItemStack owner, String rootName, String... fieldNames) {
        var root = owner.getTag();
        if (root == null || !root.contains(rootName, Tag.TAG_COMPOUND)) {
            return;
        }
        var legacy = root.getCompound(rootName);
        for (var fieldName : fieldNames) {
            legacy.remove(fieldName);
        }
        if (legacy.isEmpty()) {
            owner.removeTagKey(rootName);
        } else {
            root.put(rootName, legacy);
        }
    }
}
