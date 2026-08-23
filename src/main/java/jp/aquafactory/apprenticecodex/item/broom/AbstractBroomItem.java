package jp.aquafactory.apprenticecodex.item.broom;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentEffects;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentHints;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentProfile;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentRule;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationImbueState;
import jp.aquafactory.apprenticecodex.item.StoredSpellCalibrationImbueTarget;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.spell.callbroom.CallBroomDeploymentManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractBroomItem extends Item implements GeoItem, ICurioItem,
        StoredSpellCalibrationImbueTarget, SpellCalibrationAdjustmentTarget {
    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 3;
    public static final int CALIBRATION_SCROLL_SLOT_COUNT = 3;
    private static final CalibrationAdjustmentProfile CALIBRATION_ADJUSTMENT_PROFILE =
            createCalibrationAdjustmentProfile();
    private static final String CALIBRATION_TAG = "SpellCalibration";
    private static final String SCROLLS_TAG = "Scrolls";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";
    private static final RawAnimation STATIC = RawAnimation.begin().thenLoop("mount");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    protected AbstractBroomItem() {
        super(new Properties().stacksTo(1).fireResistant());
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player,
                                                           @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            // Curios外へ強制移動・複製されたstackへ展開状態を持ち込まず、従来の通常設置へ戻す。
            BroomDeploymentState.clear(stack);
        }
        var hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        }

        var eye = player.getEyePosition();
        var look = player.getViewVector(1.0F);
        var obstructionBox = player.getBoundingBox().expandTowards(look.scale(5.0D)).inflate(1.0D);
        for (Entity entity : level.getEntities(player, obstructionBox, Entity::isPickable)) {
            var box = entity.getBoundingBox().inflate(entity.getPickRadius());
            if (box.contains(eye)) {
                return InteractionResultHolder.pass(stack);
            }
        }

        var broom = createBroom(level);
        broom.setBroomItemStack(stack);
        var customName = stack.hasCustomHoverName() ? stack.getHoverName() : null;
        broom.setCustomName(customName);
        broom.setCustomNameVisible(customName != null);
        broom.setPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
        broom.setYRot(player.getYRot());
        var spawnBox = broom.getBoundingBox();
        if (!level.noCollision(broom, spawnBox)
                || !level.getEntities(broom, spawnBox, Entity::isPickable).isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            // 他MODが生成イベントを拒否した場合などに、生成されていない箒の消費を防ぐ.
            // 基本はバニラボートと同じよう警告表示は現時点ではいれない.
            if (!level.addFreshEntity(broom)) {
                return InteractionResultHolder.fail(stack);
            }
            level.gameEvent(player, GameEvent.ENTITY_PLACE, hit.getLocation());
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    protected abstract AbstractBroomEntity createBroom(Level level);

    protected static CalibrationAdjustmentProfile createCalibrationAdjustmentProfile(
            CalibrationAdjustmentRule... additionalRules
    ) {
        var rules = new ArrayList<CalibrationAdjustmentRule>();
        rules.add(CalibrationAdjustmentRule.repeatable(
                "slot_upgrade",
                AbstractBroomItem::isCalibrationSlotUpgrade,
                CalibrationAdjustmentHints.slotUpgrades()
        ).withEffectLines(CalibrationAdjustmentEffects.addScrollSlot(1)));
        rules.add(CalibrationAdjustmentRule.unique(
                "adapt_back_curios",
                stack -> stack.is(Items.SADDLE),
                CalibrationAdjustmentHints.saddle()
        ).withEffectLines(CalibrationAdjustmentEffects.adaptBackCurios()));
        rules.add(CalibrationAdjustmentRule.unique(
                "gain_fireward",
                stack -> stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.FIREWARD_RING.get()),
                CalibrationAdjustmentHints.firewardRing()
        ).withEffectLines(CalibrationAdjustmentEffects.gainFireward()));
        rules.addAll(List.of(additionalRules));
        return CalibrationAdjustmentProfile.of(rules.toArray(CalibrationAdjustmentRule[]::new));
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        return createCalibrationAdjustmentTooltip(stack);
    }

    @Override
    public int getCalibrationAdjustmentSlotCount(@NotNull ItemStack targetStack) {
        return CALIBRATION_ADJUSTMENT_SLOT_COUNT;
    }

    @Override
    public @NotNull CalibrationAdjustmentProfile getCalibrationAdjustmentProfile(@NotNull ItemStack targetStack) {
        return CALIBRATION_ADJUSTMENT_PROFILE;
    }

    @Override
    public @NotNull SpellCalibrationImbueState evaluateCalibrationImbue(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull SpellData spellData
    ) {
        if (slot < 0 || slot >= getEnabledCalibrationScrollSlotCount(targetStack)
                || spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return SpellCalibrationImbueState.REJECTED;
        }
        return SpellCalibrationImbueState.ACCEPTED_USABLE;
    }

    @Override
    public boolean hasAnyStoredCalibrationScroll(@NotNull ItemStack targetStack) {
        for (var slot = 0; slot < CALIBRATION_SCROLL_SLOT_COUNT; ++slot) {
            if (!getCalibrationScroll(targetStack, slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static int getEnabledCalibrationScrollSlotCount(@NotNull ItemStack broomStack) {
        if (!isValidBroomStack(broomStack)) {
            return 0;
        }
        var upgradeCount = 0;
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (isCalibrationSlotUpgrade(CalibrationAdjustmentStorage.get(
                    broomStack,
                    slot,
                    CALIBRATION_ADJUSTMENT_SLOT_COUNT
            ))) {
                ++upgradeCount;
            }
        }
        return Math.min(CALIBRATION_SCROLL_SLOT_COUNT, upgradeCount);
    }

    public static boolean isBackCurioEnabled(@NotNull ItemStack broomStack) {
        if (!isValidBroomStack(broomStack)) {
            return false;
        }
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (CalibrationAdjustmentStorage.get(broomStack, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT)
                    .is(Items.SADDLE)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFirewardEnabled(@NotNull ItemStack broomStack) {
        if (!isValidBroomStack(broomStack)) {
            return false;
        }
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (CalibrationAdjustmentStorage.get(broomStack, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT)
                    .is(io.redspace.ironsspellbooks.registries.ItemRegistry.FIREWARD_RING.get())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOverdriveEnabled(@NotNull ItemStack broomStack) {
        if (!isValidBroomStack(broomStack)) {
            return false;
        }
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (CalibrationAdjustmentStorage.get(broomStack, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT)
                    .is(ItemRegistry.OVERDRIVE_BROOM_ENGINE.get())) {
                return true;
            }
        }
        return false;
    }

    public static @NotNull ItemStack getCalibrationScroll(@NotNull ItemStack broomStack, int slot) {
        if (!isValidScrollAccess(broomStack, slot)) {
            return ItemStack.EMPTY;
        }
        var calibrationTag = broomStack.getTagElement(CALIBRATION_TAG);
        if (calibrationTag == null || !calibrationTag.contains(SCROLLS_TAG, Tag.TAG_LIST)) {
            return ItemStack.EMPTY;
        }
        var scrolls = calibrationTag.getList(SCROLLS_TAG, Tag.TAG_COMPOUND);
        for (var index = 0; index < scrolls.size(); ++index) {
            var entry = scrolls.getCompound(index);
            if (entry.getInt(SLOT_TAG) == slot && entry.contains(ITEM_TAG, Tag.TAG_COMPOUND)) {
                return ItemStack.of(entry.getCompound(ITEM_TAG));
            }
        }
        return ItemStack.EMPTY;
    }

    public static void setCalibrationScroll(
            @NotNull ItemStack broomStack,
            int slot,
            @NotNull ItemStack scrollStack
    ) {
        if (!isValidScrollAccess(broomStack, slot)) {
            return;
        }
        var calibrationTag = broomStack.getOrCreateTagElement(CALIBRATION_TAG);
        var scrolls = calibrationTag.contains(SCROLLS_TAG, Tag.TAG_LIST)
                ? calibrationTag.getList(SCROLLS_TAG, Tag.TAG_COMPOUND)
                : new ListTag();
        removeStoredScroll(scrolls, slot);
        if (!scrollStack.isEmpty()) {
            var storedScroll = scrollStack.copy();
            storedScroll.setCount(1);
            var entry = new CompoundTag();
            entry.putInt(SLOT_TAG, slot);
            entry.put(ITEM_TAG, storedScroll.save(new CompoundTag()));
            scrolls.add(entry);
        }
        if (scrolls.isEmpty()) {
            calibrationTag.remove(SCROLLS_TAG);
        } else {
            calibrationTag.put(SCROLLS_TAG, scrolls);
        }
        if (calibrationTag.isEmpty()) {
            broomStack.removeTagKey(CALIBRATION_TAG);
        }
    }

    public static @NotNull SpellData getCalibrationSpellData(@NotNull ItemStack broomStack, int slot) {
        var container = ISpellContainer.get(getCalibrationScroll(broomStack, slot));
        if (container == null || container.getActiveSpellCount() != 1) {
            return SpellData.EMPTY;
        }
        var spellData = container.getSpellAtIndex(0);
        return spellData == null ? SpellData.EMPTY : spellData;
    }

    private static boolean isCalibrationSlotUpgrade(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(TagRegistry.Items.SCROLLCASTER_GAUNTLET_SLOT_UPGRADES);
    }

    private static boolean isValidBroomStack(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof AbstractBroomItem;
    }

    private static boolean isValidScrollAccess(ItemStack stack, int slot) {
        return isValidBroomStack(stack) && slot >= 0 && slot < CALIBRATION_SCROLL_SLOT_COUNT;
    }

    private static SpellData getScrollSpellData(ItemStack scrollStack) {
        var container = ISpellContainer.get(scrollStack);
        if (container == null || container.getActiveSpellCount() != 1) {
            return SpellData.EMPTY;
        }
        var spellData = container.getSpellAtIndex(0);
        return spellData == null ? SpellData.EMPTY : spellData;
    }

    private static void removeStoredScroll(ListTag scrolls, int slot) {
        for (var index = scrolls.size() - 1; index >= 0; --index) {
            if (scrolls.getCompound(index).getInt(SLOT_TAG) == slot) {
                scrolls.remove(index);
            }
        }
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return BroomCurioSupport.canEquip(slotContext, stack);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        // 従来の設置操作を維持し、装備はCurios画面からの明示操作に限定する.
        return false;
    }

    @Override
    public List<Component> getSlotsTooltip(
            List<Component> tooltips,
            ItemStack stack
    ) {
        if (!isBackCurioEnabled(stack)) {
            return tooltips;
        }

        var result = new ArrayList<>(tooltips);
        result.add(Component.empty());
        result.add(Component.translatable(
                "curios.modifiers." + BroomCurioSupport.CURIO_SLOT
        ).withStyle(ChatFormatting.GOLD));
        for (var line = 1; line <= 3; ++line) {
            result.add(Component.literal(" ")
                    .append(Component.translatable("item.apprenticecodex.brooms.curios.desc_" + line))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        }
        // Curios固有説明と通常の設置・操作説明を別段落として表示する。
        result.add(Component.empty());
        return result;
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (slotContext.entity() instanceof ServerPlayer player) {
            CallBroomDeploymentManager.onUnequip(player, stack);
        }
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof ServerPlayer player) {
            CallBroomDeploymentManager.reconcileEquippedStack(player, stack);
        }
    }

    protected static void appendPlacementAndRecoveryTooltip(
            @NotNull List<Component> tooltipComponents,
            String keyPrefix
    ) {
        tooltipComponents.add(Component.translatable(
                keyPrefix + ".desc_1",
                Component.keybind("key.use")
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                keyPrefix + ".desc_2",
                Component.keybind("key.sneak"),
                Component.keybind("key.use")
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "item.apprenticecodex.broom.desc_calibration"
        ).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public abstract void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                         @NotNull List<Component> tooltipComponents,
                                         @NotNull TooltipFlag tooltipFlag);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> {
            state.setAnimation(STATIC);
            return software.bernie.geckolib.core.object.PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
