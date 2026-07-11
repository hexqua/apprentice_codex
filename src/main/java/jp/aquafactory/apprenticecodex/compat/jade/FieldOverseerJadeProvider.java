package jp.aquafactory.apprenticecodex.compat.jade;

import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerStaffEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum FieldOverseerJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final String OWNER_NAME_TAG = "OwnerName";
    private static final String CURRENT_MANA_TAG = "CurrentMana";
    private static final String MAX_MANA_TAG = "MaxMana";

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof FieldOverseerStaffEntity staff)) {
            return;
        }

        var ownerName = staff.getOwnerName();
        if (ownerName != null && !ownerName.isBlank()) {
            data.putString(OWNER_NAME_TAG, ownerName);
        }
        data.putInt(CURRENT_MANA_TAG, Math.max(0, Math.round(staff.getCurrentMana())));
        data.putInt(MAX_MANA_TAG, Math.max(0, Math.round(staff.getMaxStaffMana())));
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();
        tooltip.add(Component.translatable(
                "jade.apprenticecodex.remaining_mana",
                Math.max(0, data.getInt(CURRENT_MANA_TAG)),
                Math.max(0, data.getInt(MAX_MANA_TAG))
        ));
    }

    @Override
    public int getDefaultPriority() {
        return -1000;
    }

    @Override
    public ResourceLocation getUid() {
        return ApprenticeCodexJadePlugin.FIELD_OVERSEER_UID;
    }
}
