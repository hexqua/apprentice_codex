package jp.aquafactory.apprenticecodex.spell.assistwings;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class AssistWingsWingEntity extends SummonWeaponEntity implements AntiMagicSusceptible {
    private static final int REMOVAL_GRACE_TICKS = 10;
    private static final EntityDataAccessor<Boolean> DATA_FALL_PROTECTION_BLOCKED =
            SynchedEntityData.defineId(AssistWingsWingEntity.class, EntityDataSerializers.BOOLEAN);

    private int removalGraceTicks = REMOVAL_GRACE_TICKS;

    public AssistWingsWingEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public AssistWingsWingEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FALL_PROTECTION_BLOCKED, false);
    }

    @Override
    public void onClientRemoval() {
        var level = level();
        EffectTools.createRingParticle(
                RotationTools.calculateBehindPosition(this, 0, -0.6, -0.2),
                getLookAngle(),
                0.2,
                6,
                0.1f,
                0.02,
                ParticleTypes.END_ROD,
                level
        );
        EffectTools.createRingParticle(
                RotationTools.calculateBehindPosition(this, 0, +0.6, -0.2),
                getLookAngle(),
                0.2,
                6,
                0.1f,
                0.02,
                ParticleTypes.END_ROD,
                level
        );

        super.onClientRemoval();
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof Player owner)) {
            discard();
            return;
        }

        var locatePosition = getBackPosition(owner);
        followTargetPosition(locatePosition);

        var removalGraceActive = removalGraceTicks > 0;
        if (removalGraceActive) {
            --removalGraceTicks;
        }

        var blockingItem = findFallProtectionBlockingItem(owner);
        updateFallProtectionBlocked(owner, blockingItem);

        if (!removalGraceActive && (owner.onGround() || isTouchingWater(owner))) {
            finishFlight(owner);
            return;
        }

        applyFallProtection(owner, !blockingItem.isEmpty());

        setYRot(owner.getYRot());
        setXRot(0);
        setRot(getYRot(), getXRot());
    }

    private void updateFallProtectionBlocked(Player owner, ItemStack blockingItem) {
        var blocked = !blockingItem.isEmpty();
        var wasBlocked = entityData.get(DATA_FALL_PROTECTION_BLOCKED);
        if (blocked == wasBlocked) {
            return;
        }

        entityData.set(DATA_FALL_PROTECTION_BLOCKED, blocked);
        if (blocked && owner instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable(
                            "ui.apprenticecodex.assist_wings.fall_protection_blocked",
                            blockingItem.getHoverName()
                    ).withStyle(ChatFormatting.YELLOW)
            ));
        }
    }

    private static ItemStack findFallProtectionBlockingItem(Player owner) {
        var mainHandItem = owner.getMainHandItem();
        if (mainHandItem.is(TagRegistry.Items.ASSIST_WINGS_ONLY_JUMP_ITEMS)) {
            return mainHandItem;
        }

        var offhandItem = owner.getOffhandItem();
        if (offhandItem.is(TagRegistry.Items.ASSIST_WINGS_ONLY_JUMP_ITEMS)) {
            return offhandItem;
        }

        return ItemStack.EMPTY;
    }

    private static void applyFallProtection(Player owner, boolean fallProtectionBlocked) {
        if (fallProtectionBlocked) {
            return;
        }

        owner.fallDistance = 0.0F;
    }

    private static boolean isTouchingWater(Player owner) {
        var level = owner.level();
        var box = owner.getBoundingBox().deflate(1.0E-4D);
        var minX = Mth.floor(box.minX);
        var maxX = Mth.floor(box.maxX);
        var minY = Mth.floor(box.minY);
        var maxY = Mth.floor(box.maxY);
        var minZ = Mth.floor(box.minZ);
        var maxZ = Mth.floor(box.maxZ);

        var mutablePos = new BlockPos.MutableBlockPos();
        for (var y = minY; y <= maxY; ++y) {
            for (var x = minX; x <= maxX; ++x) {
                for (var z = minZ; z <= maxZ; ++z) {
                    mutablePos.set(x, y, z);
                    var fluidState = level.getFluidState(mutablePos);
                    if (!fluidState.is(FluidTags.WATER)) {
                        continue;
                    }

                    var fluidTop = y + fluidState.getHeight(level, mutablePos);
                    if (box.maxY > y && box.minY < fluidTop) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void finishFlight(Player owner) {
        Capabilities.withSpellData(owner, data -> data.edit(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE, state -> {
            if (state.localEntityId == getId()) {
                state.localEntityId = -1;
                state.doneJump = 0;
            }
        }));
        discard();
    }

    public boolean isFallProtectionBlocked() {
        return entityData.get(DATA_FALL_PROTECTION_BLOCKED);
    }

    @Override
    public Vec3 getStandbyPosition() {
        if ((getOwner() instanceof LivingEntity owner)) {
            return getBackPosition(owner);
        }

        return Vec3.ZERO;
    }

    public Vec3 getBackPosition(LivingEntity owner){
        return RotationTools.calculateBehindPosition(owner, 0.25, 0, -0.4);
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (level().isClientSide || isRemoved()) {
            return;
        }

        if (getOwner() instanceof Player owner) {
            clearManagedState(owner);
        }
        discard();
    }

    private void clearManagedState(Player owner) {
        Capabilities.withSpellData(owner, data -> data.edit(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE, state -> {
            if (state.localEntityId == getId()) {
                state.localEntityId = -1;
            }
        }));
    }

}

