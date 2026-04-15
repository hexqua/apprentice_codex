package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Locale;
import java.util.function.Predicate;

public final class PresetSpellContainerStateHelper {
    private static final String ROOT_TAG = "ApprenticeCodexPresetSpellContainerState";
    private static final String STATE_TAG = "State";
    private static final String SPELL_ID_TAG = "SpellId";
    private static final String SPELL_LEVEL_TAG = "SpellLevel";

    private PresetSpellContainerStateHelper() {
    }

    public static boolean restoreIfNeeded(
            ItemStack stack,
            int maxSpellCount,
            boolean spellWheelEnabled,
            boolean mustEquip,
            Predicate<SpellData> validator
    ) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        var rememberedState = getRememberedState(stack);
        if (rememberedState == null) {
            return false;
        }

        if (matchesCurrentState(stack, rememberedState, validator)) {
            return true;
        }

        if (applyRememberedState(stack, rememberedState, maxSpellCount, spellWheelEnabled, mustEquip, validator)) {
            return true;
        }

        clearRememberedState(stack);
        return false;
    }

    public static void rememberOverridden(ItemStack stack, SpellData spellData) {
        if (stack == null || stack.isEmpty() || spellData == null || spellData == SpellData.EMPTY) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            var stateTag = new CompoundTag();
            stateTag.putString(STATE_TAG, RememberedStateType.OVERRIDDEN.serializedName());
            stateTag.putString(SPELL_ID_TAG, spellData.getSpell().getSpellResource().toString());
            stateTag.putInt(SPELL_LEVEL_TAG, spellData.getLevel());
            tag.put(ROOT_TAG, stateTag);
        });
    }

    public static void rememberCleared(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            var stateTag = new CompoundTag();
            stateTag.putString(STATE_TAG, RememberedStateType.CLEARED.serializedName());
            tag.put(ROOT_TAG, stateTag);
        });
    }

    public static void clearRememberedState(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(ROOT_TAG));
    }

    private static boolean matchesCurrentState(ItemStack stack, RememberedState rememberedState, Predicate<SpellData> validator) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            return false;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null) {
            return false;
        }

        return switch (rememberedState.type()) {
            case CLEARED -> spellContainer.getActiveSpellCount() <= 0
                    || spellContainer.getSpellAtIndex(0) == SpellData.EMPTY;
            case OVERRIDDEN -> {
                var spellData = spellContainer.getSpellAtIndex(0);
                yield rememberedState.spellData() != null
                        && spellData != SpellData.EMPTY
                        && spellData.canRemove()
                        && spellData.getSpell().equals(rememberedState.spellData().getSpell())
                        && spellData.getLevel() == rememberedState.spellData().getLevel()
                        && validator.test(spellData);
            }
        };
    }

    private static boolean applyRememberedState(
            ItemStack stack,
            RememberedState rememberedState,
            int maxSpellCount,
            boolean spellWheelEnabled,
            boolean mustEquip,
            Predicate<SpellData> validator
    ) {
        var normalized = ISpellContainer.create(maxSpellCount, spellWheelEnabled, mustEquip).mutableCopy();
        if (rememberedState.type() == RememberedStateType.CLEARED) {
            ISpellContainer.set(stack, normalized.toImmutable());
            return true;
        }

        var spellData = rememberedState.spellData();
        if (spellData == null || !validator.test(spellData)) {
            return false;
        }

        if (!normalized.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, false)) {
            return false;
        }

        ISpellContainer.set(stack, normalized.toImmutable());
        return true;
    }

    private static RememberedState getRememberedState(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }

        var root = customData.copyTag();
        if (!root.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }

        var stateTag = root.getCompound(ROOT_TAG);
        var type = RememberedStateType.fromSerializedName(stateTag.getString(STATE_TAG));
        if (type == null) {
            return null;
        }

        if (type == RememberedStateType.CLEARED) {
            return new RememberedState(type, null);
        }

        if (!stateTag.contains(SPELL_ID_TAG, Tag.TAG_STRING) || !stateTag.contains(SPELL_LEVEL_TAG, Tag.TAG_INT)) {
            return null;
        }

        var spellId = ResourceLocation.tryParse(stateTag.getString(SPELL_ID_TAG));
        if (spellId == null) {
            return null;
        }

        var spell = SpellRegistry.getSpell(spellId);
        if (spell == null || spell == SpellRegistry.none()) {
            return null;
        }

        return new RememberedState(type, new SpellData(spell, stateTag.getInt(SPELL_LEVEL_TAG), false));
    }

    private record RememberedState(RememberedStateType type, SpellData spellData) {
    }

    private enum RememberedStateType {
        OVERRIDDEN,
        CLEARED;

        private String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        private static RememberedStateType fromSerializedName(String serializedName) {
            for (var type : values()) {
                if (type.serializedName().equals(serializedName)) {
                    return type;
                }
            }
            return null;
        }
    }
}
