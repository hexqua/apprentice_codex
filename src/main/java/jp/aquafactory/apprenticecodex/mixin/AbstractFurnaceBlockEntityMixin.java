package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.accessor.ArcaneCinderFurnaceAccess;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin implements ArcaneCinderFurnaceAccess {
    @Unique
    private static final String APPRENTICE_CODEX_ARCANE_CINDER_FUEL_ACTIVE_TAG =
            "apprenticecodex:arcane_cinder_fuel_active";

    @Unique
    private boolean apprenticeCodex$arcaneCinderFuelActive;

    @Shadow
    int litTime;

    @Shadow
    int cookingProgress;

    @Shadow
    int cookingTotalTime;

    @Inject(method = "load", at = @At("RETURN"))
    private void apprenticeCodex$loadArcaneCinderFuelState(CompoundTag tag, CallbackInfo ci) {
        apprenticeCodex$arcaneCinderFuelActive = tag.getBoolean(APPRENTICE_CODEX_ARCANE_CINDER_FUEL_ACTIVE_TAG);
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void apprenticeCodex$saveArcaneCinderFuelState(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean(APPRENTICE_CODEX_ARCANE_CINDER_FUEL_ACTIVE_TAG, apprenticeCodex$arcaneCinderFuelActive);
    }

    @Inject(
            method = "serverTick",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;litTime:I",
                    opcode = Opcodes.PUTFIELD,
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private static void apprenticeCodex$consumeArcaneCinderFaster(
            Level level,
            net.minecraft.core.BlockPos pos,
            BlockState state,
            AbstractFurnaceBlockEntity blockEntity,
            CallbackInfo ci
    ) {
        var self = (ArcaneCinderFurnaceAccess) blockEntity;
        if (!apprenticeCodex$shouldSpeedUp(blockEntity)) {
            return;
        }

        if (self.apprenticeCodex$getLitTime() > 0) {
            self.apprenticeCodex$setLitTime(self.apprenticeCodex$getLitTime() - 1);
        }

        if (self.apprenticeCodex$getLitTime() <= 0) {
            self.apprenticeCodex$setArcaneCinderFuelActive(false);
        }
    }

    @Inject(
            method = "serverTick",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;litTime:I",
                    opcode = Opcodes.PUTFIELD,
                    ordinal = 1,
                    shift = At.Shift.AFTER
            )
    )
    private static void apprenticeCodex$trackConsumedFuel(
            Level level,
            net.minecraft.core.BlockPos pos,
            BlockState state,
            AbstractFurnaceBlockEntity blockEntity,
            CallbackInfo ci
    ) {
        var self = (ArcaneCinderFurnaceAccess) blockEntity;
        if (self.apprenticeCodex$getLitTime() <= 0) {
            self.apprenticeCodex$setArcaneCinderFuelActive(false);
            return;
        }

        // 実際に消費した燃料を追跡しないと、ホッパー差し替えや同 burn time の燃料で誤判定する.
        self.apprenticeCodex$setArcaneCinderFuelActive(blockEntity.getItem(1).is(ItemRegistry.ARCANE_CINDER.get()));
    }

    @Inject(
            method = "serverTick",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;cookingProgress:I",
                    opcode = Opcodes.PUTFIELD,
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private static void apprenticeCodex$advanceArcaneCinderCookingProgress(
            Level level,
            net.minecraft.core.BlockPos pos,
            BlockState state,
            AbstractFurnaceBlockEntity blockEntity,
            CallbackInfo ci
    ) {
        var self = (ArcaneCinderFurnaceAccess) blockEntity;
        if (!apprenticeCodex$shouldSpeedUp(blockEntity)
                || self.apprenticeCodex$getCookingProgress() >= self.apprenticeCodex$getCookingTotalTime()) {
            return;
        }

        // 完了判定の直前で total まで丸めることで、既存の completion 分岐をそのまま使う.
        self.apprenticeCodex$setCookingProgress(
                Math.min(self.apprenticeCodex$getCookingProgress() + 1, self.apprenticeCodex$getCookingTotalTime())
        );
    }

    @Unique
    private static boolean apprenticeCodex$shouldSpeedUp(AbstractFurnaceBlockEntity blockEntity) {
        var self = (ArcaneCinderFurnaceAccess) blockEntity;
        return self.apprenticeCodex$isArcaneCinderFuelActive()
                && apprenticeCodex$isTargetFurnace(blockEntity);
    }

    @Unique
    private static boolean apprenticeCodex$isTargetFurnace(AbstractFurnaceBlockEntity blockEntity) {
        if (!ApprenticeCodexServerConfig.limitArcaneCinderSpeedupToVanillaFurnaces()) {
            return true;
        }

        return blockEntity instanceof FurnaceBlockEntity
                || blockEntity instanceof BlastFurnaceBlockEntity
                || blockEntity instanceof SmokerBlockEntity;
    }

    @Override
    public boolean apprenticeCodex$isArcaneCinderFuelActive() {
        return apprenticeCodex$arcaneCinderFuelActive;
    }

    @Override
    public void apprenticeCodex$setArcaneCinderFuelActive(boolean active) {
        apprenticeCodex$arcaneCinderFuelActive = active;
    }

    @Override
    public int apprenticeCodex$getLitTime() {
        return litTime;
    }

    @Override
    public void apprenticeCodex$setLitTime(int litTime) {
        this.litTime = litTime;
    }

    @Override
    public int apprenticeCodex$getCookingProgress() {
        return cookingProgress;
    }

    @Override
    public void apprenticeCodex$setCookingProgress(int cookingProgress) {
        this.cookingProgress = cookingProgress;
    }

    @Override
    public int apprenticeCodex$getCookingTotalTime() {
        return cookingTotalTime;
    }
}
