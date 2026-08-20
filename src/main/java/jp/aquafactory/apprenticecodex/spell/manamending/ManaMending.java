package jp.aquafactory.apprenticecodex.spell.manamending;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.spell.ICraftsmansDelightAffectedSpell;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

public class ManaMending extends AbstractSpell implements ICraftsmansDelightAffectedSpell {
    private static final int MENDING_INTERVAL_TICKS = 10;
    private static final int MIN_PROGRESS_MILESTONE = 10;
    private static final int MAX_PROGRESS_MILESTONE = 90;
    private static final DustParticleOptions MANA_MENDING_PARTICLE =
            new DustParticleOptions(new Vector3f(1.00f, 0.85f, 0.35f), 1.0f);
    private static final int MANA_MENDING_PARTICLE_COUNT = 8;
    private static final double MANA_MENDING_PARTICLE_SPEED = 0.01D;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mana_mending");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(30)
            .build();

    public ManaMending() {
        baseSpellPower = 100;
        spellPowerPerLevel = 50;
        baseManaCost = 25;
        manaCostPerLevel = 0;
        castTime = 200;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.apprenticecodex.mana_mending.repair_per_second", Utils.stringTruncation(getMendingSpeedPerSecond(spellLevel, caster), 1))
        );
    }

    private float getMendingSpeedPerSecond(int spellLevel, LivingEntity caster){
        return getMendingAmountPerProcess(spellLevel, caster) * 2.0f;
    }

    private float getMendingAmountPerProcess(int spellLevel, LivingEntity caster) {
        var baseAmount = getSpellPower(spellLevel, caster) / 100.0f;
        return CraftsmansDelight.applyProcessSpeedBonus(baseAmount, caster);
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return config;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.VANILLA_POWER_ACTIVATE.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new ManaMendingCastData();
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (!super.checkPreCastConditions(level, spellLevel, entity, playerMagicData)) {
            return false;
        }

        var result = findInitialTarget(entity);
        if (!result.success()) {
            result.failure().send(entity, result.stack());
            return false;
        }

        if (playerMagicData != null) {
            var castData = new ManaMendingCastData();
            castData.lockTarget(result.hand(), result.stack());
            playerMagicData.setAdditionalCastData(castData);
        }
        return true;
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        var castData = getOrCreateCastData(entity, playerMagicData);
        if (castData == null) {
            failAndCancel(entity, ManaMendingFailure.EMPTY, ItemStack.EMPTY);
            return;
        }

        var targetValidation = validateTargetHand(entity, castData.targetHand());
        if (!targetValidation.success()) {
            failAndCancel(entity, targetValidation.failure(), targetValidation.stack());
            return;
        }
        if (!castData.matchesTarget(targetValidation.stack())) {
            failAndCancel(entity, ManaMendingFailure.CANCELED_BY_SWAP, ItemStack.EMPTY);
            return;
        }

        if (playerMagicData != null){
            var tick = playerMagicData.getCastDurationRemaining();
            if (shouldMendThisTick(spellLevel, entity, playerMagicData)){
                mendTarget(entity, spellLevel, targetValidation.stack(), castData);
            }

            if (tick % 30 == 0){
                AudioTools.playSoundFromEntity(level, entity, SoundEvents.ANVIL_USE, SoundSource.PLAYERS);
            }
        }

        spawnManaChargeParticles(level, entity);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        if (playerMagicData != null) {
            if (playerMagicData.getAdditionalCastData() instanceof ManaMendingCastData castData) {
                castData.reset();
            }
            playerMagicData.setAdditionalCastData(null);
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    private @Nullable ManaMendingCastData getOrCreateCastData(LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (playerMagicData != null && playerMagicData.getAdditionalCastData() instanceof ManaMendingCastData castData && castData.hasTarget()) {
            return castData;
        }

        var result = findInitialTarget(entity);
        if (!result.success()) {
            return null;
        }

        var castData = new ManaMendingCastData();
        castData.lockTarget(result.hand(), result.stack());
        if (playerMagicData != null) {
            playerMagicData.setAdditionalCastData(castData);
        }
        return castData;
    }

    private TargetResult findInitialTarget(LivingEntity entity) {
        var offhandResult = validateInitialHand(entity, InteractionHand.OFF_HAND);
        if (offhandResult.terminal()) {
            return offhandResult;
        }
        if (offhandResult.success()) {
            return offhandResult;
        }

        var mainHandResult = validateInitialHand(entity, InteractionHand.MAIN_HAND);
        if (mainHandResult.success() || mainHandResult.terminal()) {
            return mainHandResult;
        }
        return TargetResult.failure(ManaMendingFailure.EMPTY, ItemStack.EMPTY, true);
    }

    private TargetResult validateInitialHand(LivingEntity entity, InteractionHand hand) {
        var stack = entity.getItemInHand(hand);
        if (stack.isEmpty()) {
            return TargetResult.skipped(stack);
        }
        if (stack.is(TagRegistry.Items.MANA_MENDING_DENYLIST)) {
            return TargetResult.failure(ManaMendingFailure.DENY_LIST, stack, true);
        }
        if (!stack.isDamageableItem() || !stack.isDamaged()) {
            return TargetResult.skipped(stack);
        }
        if (!canRepairWithManaMending(stack)) {
            return TargetResult.failure(ManaMendingFailure.INVALID_MENDING, stack, true);
        }
        return TargetResult.success(hand, stack);
    }

    private TargetResult validateTargetHand(LivingEntity entity, InteractionHand hand) {
        var result = validateInitialHand(entity, hand);
        return result.success() ? result : TargetResult.failure(result.failure(), result.stack(), true);
    }

    private boolean canRepairWithManaMending(ItemStack stack) {
        return Enchantments.UNBREAKING.canEnchant(stack) || Enchantments.MENDING.canEnchant(stack);
    }

    private boolean shouldMendThisTick(int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var elapsedTicks = Math.max(0, getEffectiveCastTime(spellLevel, entity) - playerMagicData.getCastDurationRemaining());
        return elapsedTicks > 0 && elapsedTicks % MENDING_INTERVAL_TICKS == 0;
    }

    private void mendTarget(LivingEntity entity, int spellLevel, ItemStack targetStack, ManaMendingCastData castData) {
        var availableRepair = castData.pendingRepairAmount + getMendingAmountPerProcess(spellLevel, entity);
        var repairAmount = Mth.floor(availableRepair);
        if (repairAmount <= 0) {
            castData.pendingRepairAmount = availableRepair;
            return;
        }

        var oldDamage = targetStack.getDamageValue();
        var newDamage = Math.max(0, oldDamage - repairAmount);
        var actualRepairAmount = oldDamage - newDamage;
        targetStack.setDamageValue(newDamage);
        castData.pendingRepairAmount = availableRepair - actualRepairAmount;

        if (!targetStack.isDamaged()) {
            finishMending(entity, targetStack, castData);
            return;
        }

        sendProgressIfNeeded(entity, targetStack, castData);
    }

    private void sendProgressIfNeeded(LivingEntity entity, ItemStack targetStack, ManaMendingCastData castData) {
        var milestone = getProgressMilestone(targetStack);
        if (milestone <= castData.lastProgressMilestone || milestone < MIN_PROGRESS_MILESTONE) {
            return;
        }

        castData.lastProgressMilestone = milestone;
        sendActionBar(
                entity,
                Component.translatable(
                        "ui.apprenticecodex.mana_mending.repair_progress",
                        targetStack.getHoverName(),
                        milestone
                ).withStyle(ChatFormatting.GREEN)
        );
    }

    private void finishMending(LivingEntity entity, ItemStack targetStack, ManaMendingCastData castData) {
        castData.reset();
        sendActionBar(
                entity,
                Component.translatable(
                        "ui.apprenticecodex.mana_mending.repair_finished",
                        targetStack.getHoverName()
                ).withStyle(ChatFormatting.GREEN)
        );
        MagicTools.cancelCasting(entity, true);
    }

    private int getProgressMilestone(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return 0;
        }

        var maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return 0;
        }

        var remainingDurability = maxDamage - stack.getDamageValue();
        var percent = Mth.floor(remainingDurability * 100.0f / maxDamage);
        return Math.min(MAX_PROGRESS_MILESTONE, percent / 10 * 10);
    }

    private void failAndCancel(LivingEntity entity, ManaMendingFailure failure, ItemStack stack) {
        failure.send(entity, stack);
        MagicTools.cancelCasting(entity, true);
    }

    private static void sendActionBar(LivingEntity entity, Component message) {
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (serverPlayer.connection == null) {
            return;
        }

        serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
    }

    private void spawnManaChargeParticles(Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (entity.tickCount % 3 != 0) {
            return;
        }

        var horizontalSpread = Math.max(0.2D, entity.getBbWidth() * 0.45D);
        var verticalSpread = Math.max(0.35D, entity.getBbHeight() * 0.45D);
        var centerY = entity.getY() + entity.getBbHeight() * 0.5D;
        serverLevel.sendParticles(
                MANA_MENDING_PARTICLE,
                entity.getX(),
                centerY,
                entity.getZ(),
                MANA_MENDING_PARTICLE_COUNT,
                horizontalSpread,
                verticalSpread,
                horizontalSpread,
                MANA_MENDING_PARTICLE_SPEED
        );
    }

    private record TargetResult(
            boolean success,
            boolean terminal,
            InteractionHand hand,
            ItemStack stack,
            ManaMendingFailure failure
    ) {
        private static TargetResult success(InteractionHand hand, ItemStack stack) {
            return new TargetResult(true, true, hand, stack, ManaMendingFailure.NONE);
        }

        private static TargetResult skipped(ItemStack stack) {
            return new TargetResult(false, false, InteractionHand.MAIN_HAND, stack, ManaMendingFailure.EMPTY);
        }

        private static TargetResult failure(ManaMendingFailure failure, ItemStack stack, boolean terminal) {
            return new TargetResult(false, terminal, InteractionHand.MAIN_HAND, stack, failure);
        }
    }

    private enum ManaMendingFailure {
        NONE(""),
        EMPTY("ui.apprenticecodex.mana_mending.invalid_by_empty"),
        CANCELED_BY_SWAP("ui.apprenticecodex.mana_mending.canceled_by_swap"),
        DENY_LIST("ui.apprenticecodex.mana_mending.invalid_by_deny_list"),
        INVALID_MENDING("ui.apprenticecodex.mana_mending.invalid_mending");

        private final String messageKey;

        ManaMendingFailure(String messageKey) {
            this.messageKey = messageKey;
        }

        private void send(LivingEntity entity, ItemStack stack) {
            if (this == NONE) {
                return;
            }

            var message = this == EMPTY || this == CANCELED_BY_SWAP
                    ? Component.translatable(messageKey)
                    : Component.translatable(messageKey, stack.getHoverName());
            sendActionBar(entity, message.withStyle(ChatFormatting.RED));
        }
    }

    public static class ManaMendingCastData implements ICastDataSerializable {
        private boolean hasTarget;
        private InteractionHand targetHand = InteractionHand.MAIN_HAND;
        private transient ItemStack targetStack = ItemStack.EMPTY;
        private float pendingRepairAmount;
        private int lastProgressMilestone;

        private void lockTarget(InteractionHand targetHand, ItemStack targetStack) {
            this.hasTarget = true;
            this.targetHand = targetHand;
            this.targetStack = targetStack;
            this.pendingRepairAmount = 0.0f;
            this.lastProgressMilestone = initialProgressMilestone(targetStack);
        }

        private boolean hasTarget() {
            return hasTarget && !targetStack.isEmpty();
        }

        private InteractionHand targetHand() {
            return targetHand;
        }

        private boolean matchesTarget(ItemStack stack) {
            return hasTarget() && targetStack == stack;
        }

        private static int initialProgressMilestone(ItemStack stack) {
            if (!stack.isDamageableItem()) {
                return 0;
            }
            var maxDamage = stack.getMaxDamage();
            if (maxDamage <= 0) {
                return 0;
            }
            var remainingDurability = maxDamage - stack.getDamageValue();
            var percent = Mth.floor(remainingDurability * 100.0f / maxDamage);
            return Math.min(MAX_PROGRESS_MILESTONE, percent / 10 * 10);
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeBoolean(hasTarget);
            friendlyByteBuf.writeEnum(targetHand);
            friendlyByteBuf.writeFloat(pendingRepairAmount);
            friendlyByteBuf.writeInt(lastProgressMilestone);
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            hasTarget = friendlyByteBuf.readBoolean();
            targetHand = friendlyByteBuf.readEnum(InteractionHand.class);
            pendingRepairAmount = friendlyByteBuf.readFloat();
            lastProgressMilestone = friendlyByteBuf.readInt();
        }

        @Override
        public CompoundTag serializeNBT() {
            var tag = new CompoundTag();
            tag.putBoolean("HasTarget", hasTarget);
            tag.putString("TargetHand", targetHand.name());
            tag.putFloat("PendingRepairAmount", pendingRepairAmount);
            tag.putInt("LastProgressMilestone", lastProgressMilestone);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            hasTarget = nbt.getBoolean("HasTarget");
            targetHand = nbt.contains("TargetHand")
                    ? InteractionHand.valueOf(nbt.getString("TargetHand"))
                    : InteractionHand.MAIN_HAND;
            pendingRepairAmount = nbt.getFloat("PendingRepairAmount");
            lastProgressMilestone = nbt.getInt("LastProgressMilestone");
        }

        @Override
        public void reset() {
            hasTarget = false;
            targetHand = InteractionHand.MAIN_HAND;
            targetStack = ItemStack.EMPTY;
            pendingRepairAmount = 0.0f;
            lastProgressMilestone = 0;
        }
    }
}
