package jp.aquafactory.apprenticecodex.compat.jade;

import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum AutoTurretJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final String REMAINING_AMMO_TAG = "RemainingAmmo";

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof AutoTurretEntity turret)) {
            return;
        }

        var ownerName = turret.getOwnerName();
        if (ownerName != null && !ownerName.isBlank()) {
            data.putString(JadeTooltipHelper.OWNER_NAME_TAG, ownerName);
        }

        data.putInt(REMAINING_AMMO_TAG, turret.getRestBulletCount());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();
        JadeTooltipHelper.appendOwnerLine(tooltip, data.getString(JadeTooltipHelper.OWNER_NAME_TAG));
        var remainingAmmo = Math.max(0, data.getInt(REMAINING_AMMO_TAG));
        JadeTooltipHelper.appendItemLine(
                tooltip,
                new ItemStack(Items.ARROW),
                Component.translatable("jade.apprenticecodex.remaining_ammo", remainingAmmo)
        );
    }

    @Override
    public int getDefaultPriority() {
        return -1000;
    }

    @Override
    public ResourceLocation getUid() {
        return ApprenticeCodexJadePlugin.AUTO_TURRET_UID;
    }
}
