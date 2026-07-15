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
import net.minecraft.core.particles.ParticleTypes;
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
    private static final double MAX_FALL_SPEED = -0.08D;
    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(AssistWingsWingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_GLIDE_BLOCKED =
            SynchedEntityData.defineId(AssistWingsWingEntity.class, EntityDataSerializers.BOOLEAN);

    private int KeepTick = 10;

    public AssistWingsWingEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public AssistWingsWingEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
        entityData.set(DATA_OWNER_ID, owner.getId());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_OWNER_ID, -1);
        entityData.define(DATA_GLIDE_BLOCKED, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            tickOnClient();
        }
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

        if (KeepTick > 0) {
            --KeepTick;
        }

        var blockingItem = findGlideBlockingItem(owner);
        updateGlideBlocked(owner, blockingItem);
        applyGlide(owner, !blockingItem.isEmpty());

        if (owner.onGround() && KeepTick <= 0) {
            Capabilities.withSpellData(owner, data -> data.edit(CodexSpellStateTypeRegister.ASSIST_WINGS_STATE, spell -> {
                spell.localEntityId = -1;
                spell.doneJump = 0;
                this.discard();
            }));
        }

        setYRot(owner.getYRot());
        setXRot(0);
        setRot(getYRot(), getXRot());
    }

    private void tickOnClient() {
        int ownerId = entityData.get(DATA_OWNER_ID);
        if (ownerId < 0 || !(level().getEntity(ownerId) instanceof Player owner) || !owner.isLocalPlayer()) {
            return;
        }

        // ポーション効果を使わないため、操作中の本人にも同じ落下制御を適用して予測ずれを抑える。
        applyGlide(owner, isGlideBlocked());
    }

    private void updateGlideBlocked(Player owner, ItemStack blockingItem) {
        var blocked = !blockingItem.isEmpty();
        var wasBlocked = entityData.get(DATA_GLIDE_BLOCKED);
        if (blocked == wasBlocked) {
            return;
        }

        entityData.set(DATA_GLIDE_BLOCKED, blocked);
        if (blocked && owner instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable(
                            "ui.apprenticecodex.assist_wings.too_heavy_for_glide",
                            blockingItem.getHoverName()
                    ).withStyle(ChatFormatting.YELLOW)
            ));
        }
    }

    private static ItemStack findGlideBlockingItem(Player owner) {
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

    private static void applyGlide(Player owner, boolean glideBlocked) {
        if (glideBlocked || !isAirborneGlideTarget(owner)) {
            return;
        }

        owner.fallDistance = 0.0F;
        if (owner.isShiftKeyDown()) {
            return;
        }

        var movement = owner.getDeltaMovement();
        if (movement.y < MAX_FALL_SPEED) {
            owner.setDeltaMovement(movement.x, MAX_FALL_SPEED, movement.z);
        }
    }

    private static boolean isAirborneGlideTarget(Player owner) {
        return !owner.isInWaterOrBubble() && !owner.isFallFlying() && !owner.getAbilities().flying
                && !owner.onClimbable() && !owner.isPassenger() && !owner.isSwimming() && !owner.onGround();
    }

    public boolean isGlideBlocked() {
        return entityData.get(DATA_GLIDE_BLOCKED);
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
