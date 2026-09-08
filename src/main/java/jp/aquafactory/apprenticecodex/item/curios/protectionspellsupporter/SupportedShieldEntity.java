package jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter;

import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SupportedShieldPassagePacket;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SupportedShieldEntity extends ShieldEntity {
    private @Nullable UUID ownerUuid;
    private final Map<ServerPlayer, Boolean> viewers = new HashMap<>();

    public SupportedShieldEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void setOwner(Player owner) {
        ownerUuid = owner.getUUID();
    }

    public @Nullable ServerPlayer getActiveOwner() {
        var server = level().getServer();
        var owner = server == null || ownerUuid == null ? null : server.getPlayerList().getPlayer(ownerUuid);
        return ProtectionSpellSupporter.isEquippedBy(owner) ? owner : null;
    }

    public boolean allowsPassage(Player player) {
        var owner = getActiveOwner();
        return CombatTools.isProtectedCombatTarget(player, owner,
                CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES);
    }

    public void startTracking(ServerPlayer player) {
        boolean allowed = allowsPassage(player);
        viewers.put(player, allowed);
        sendPassage(player, allowed);
    }

    public void stopTracking(ServerPlayer player) {
        viewers.remove(player);
        sendPassage(player, false);
    }

    private void sendPassage(ServerPlayer player, boolean allowed) {
        Networks.sendToPlayer(player, new SupportedShieldPassagePacket(level().dimension().location(), getUUID(), allowed));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && !isRemoved()) {
            viewers.entrySet().removeIf(entry -> entry.getKey().hasDisconnected());
            for (var entry : viewers.entrySet()) {
                boolean allowed = allowsPassage(entry.getKey());
                if (allowed != entry.getValue()) {
                    entry.setValue(allowed);
                    sendPassage(entry.getKey(), allowed);
                }
            }
        }
    }

    @Override
    public void takeDamage(DamageSource source, float amount, @Nullable Vec3 location) {
        var owner = getActiveOwner();
        // 命中・遮断と対魔法による消去は本体に任せ、術者由来の耐久減少だけを抑える。
        if (owner != null && owner == CombatTools.resolveDamageOwner(source)) {
            amount = 0;
        }
        super.takeDamage(source, amount, location);
    }

    @Override
    protected @NotNull Component getTypeName() {
        // 自前でlangを持たずにベースのIron'sのシールド魔法の名前を参照する.
        return io.redspace.ironsspellbooks.registries.EntityRegistry.SHIELD_ENTITY.get().getDescription();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUuid != null) tag.putUUID("SupportedShieldOwner", ownerUuid);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerUuid = tag.hasUUID("SupportedShieldOwner") ? tag.getUUID("SupportedShieldOwner") : null;
    }
}
