package jp.aquafactory.apprenticecodex.item.armor;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class ChromaticMagiaDressHistory {
    static final int MAX_HISTORY_SIZE = 20;

    private static final String HISTORY_TAG = "ChromaticMagiaDressSchoolHistory";

    private ChromaticMagiaDressHistory() {
    }

    static void append(ItemStack stack, @Nullable SchoolType schoolType) {
        if (stack.isEmpty() || schoolType == null) {
            return;
        }

        var schoolId = schoolType.getId();
        if (schoolId == null) {
            return;
        }

        var history = readSchoolIds(stack);
        history.add(schoolId.toString());
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            var listTag = new ListTag();
            history.forEach(id -> listTag.add(StringTag.valueOf(id)));
            tag.put(HISTORY_TAG, listTag);
        });
    }

    static List<SchoolType> readSchools(ItemStack stack) {
        var result = new ArrayList<SchoolType>();
        for (var schoolId : readSchoolIds(stack)) {
            var resourceLocation = ResourceLocation.tryParse(schoolId);
            if (resourceLocation == null) {
                continue;
            }

            var schoolType = SchoolRegistry.REGISTRY.get(resourceLocation);
            if (schoolType != null) {
                result.add(schoolType);
            }
        }
        return result;
    }

    private static List<String> readSchoolIds(ItemStack stack) {
        var result = new ArrayList<String>();
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return result;
        }

        var tag = customData.copyTag();
        if (!tag.contains(HISTORY_TAG, Tag.TAG_LIST)) {
            return result;
        }

        var history = tag.getList(HISTORY_TAG, Tag.TAG_STRING);
        for (int i = 0; i < history.size(); ++i) {
            result.add(history.getString(i));
        }
        return result;
    }
}
