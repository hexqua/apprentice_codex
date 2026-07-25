package jp.aquafactory.apprenticecodex.item.luminousdevice;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.compat.malum.MalumLuminousDeviceBridge;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.InventoryInsertTarget;
import jp.aquafactory.apprenticecodex.item.ItemManaBypassCastEvent;
import jp.aquafactory.apprenticecodex.item.ManaBypassSpellItem;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.item.SneakSelectionUiItem;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncRemainingCountNotificationPacket;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.renderer.item.LuminousDeviceRenderer;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLight;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLightCastContext;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.BlockTools;
import jp.aquafactory.apprenticecodex.utility.CompactCountFormatter;
import jp.aquafactory.apprenticecodex.utility.ManaPotionRecoveryHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class LuminousDevice extends Item implements SneakSelectionUiItem, ManaBypassSpellItem, UniqueItem, GeoItem, InventoryInsertTarget {
    private static final String STORAGE_TAG = "LuminousDevice";
    private static final String CONTENTS_TAG = "Contents";
    private static final String SELECTED_STACK_TAG = "SelectedStack";
    private static final String SELECTED_SPELL_TAG = "SelectedSpell";
    private static final String MODE_TAG = "Mode";
    private static final String MANA_TAG = "Mana";
    private static final String UPGRADES_TAG = "Upgrades";
    private static final String STACK_TAG = "Stack";
    private static final String COUNT_TAG = "Count";
    private static final int SELECTION_COUNT_COLOR = 0xFFFFFF;
    private static final int EMPTY_SELECTION_COUNT_COLOR = 0xFF5555;
    private static final int MANA_BAR_COLOR = 0x4F88E8;
    private static final int CLEAN_COOLDOWN_TICKS = 20;
    private static final int SPELL_LEVEL = 1;
    private static final String MAIN_CONTROLLER = "main";
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LuminousDevice() {
        super(new Properties().stacksTo(1).fireResistant());
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private LuminousDeviceRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new LuminousDeviceRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, MAIN_CONTROLLER, 0, state -> {
            state.setAnimation(IDLE_ANIMATION);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        if (getMode(stack) == Mode.CLEAN) {
            return Component.translatable(
                    "item.apprenticecodex.luminous_device.with_select",
                    super.getName(stack),
                    Component.translatable("item.apprenticecodex.luminous_device.mode.clean")
            );
        }

        var selectedSpell = getSelectedSpellData(stack);
        if (selectedSpell != SpellData.EMPTY) {
            return Component.translatable(
                    "item.apprenticecodex.luminous_device.with_select",
                    super.getName(stack),
                    selectedSpell.getSpell().getDisplayName(null).withStyle(Style.EMPTY)
            );
        }

        var selectedStack = getSelectedStack(stack);
        if (selectedStack.isEmpty()) {
            return super.getName(stack);
        }

        return Component.translatable(
                "item.apprenticecodex.luminous_device.with_select",
                super.getName(stack),
                selectedStack.getHoverName()
        );
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        var displayStacks = NonNullList.<ItemStack>create();
        var contents = readContents(stack);
        for (var entry : contents) {
            displayStacks.add(entry.displayStack.copyWithCount(entry.count));
        }
        return Optional.of(new LuminousDeviceTooltip(
                displayStacks,
                findRemovalCandidateIndex(contents, getStoredSelectedStack(stack)),
                getStoredItemCount(stack) >= LuminousDeviceConfigState.maxStoredItems()
        ));
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            @Nullable Level level,
            @NotNull List<Component> lines,
            @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, level, lines, flag);
        var maxStoredItems = LuminousDeviceConfigState.maxStoredItems();
        var maxStoredMana = clientMaxStoredMana(stack);
        lines.add(Component.literal(
                "(" + getStoredItemCount(stack) + "/" + maxStoredItems + ")"
        ).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(getDescriptionId() + ".desc_1").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(getDescriptionId() + ".desc_2").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                getDescriptionId() + ".mana_label",
                Component.literal(Integer.toString(getStoredMana(stack))).withStyle(ChatFormatting.AQUA),
                Component.literal(Integer.toString(maxStoredMana)).withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                getDescriptionId() + ".upgrade",
                createUpgradeTooltipEntry(stack, LuminousDeviceUpgrade.CLEAN),
                createUpgradeTooltipEntry(stack, LuminousDeviceUpgrade.ENHANCED_MAGE_LIGHT),
                createUpgradeTooltipEntry(stack, LuminousDeviceUpgrade.MANA_WIZARDLAMP)
        ).withStyle(ChatFormatting.GRAY));
        appendModeTooltip(stack, lines);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return getStoredMana(stack) > 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        var storedMana = getStoredMana(stack);
        if (storedMana <= 0) {
            return 0;
        }

        var maxStoredMana = clientMaxStoredMana(stack);
        if (maxStoredMana <= 0) {
            return 13;
        }
        return Math.min(13, Math.max(1, Math.round(13.0F * storedMana / maxStoredMana)));
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return MANA_BAR_COLOR;
    }

    @Override
    public boolean overrideStackedOnOther(@NotNull ItemStack deviceStack, @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }

        if (slot.hasItem()) {
            var slotStack = slot.getItem();
            if (!accepts(slotStack)) {
                return false;
            }

            var extracted = slot.safeTake(slotStack.getCount(), slotStack.getCount(), player);
            if (extracted.isEmpty()) {
                return false;
            }

            var inserted = addToDevice(deviceStack, extracted, maxStoredItems(player));
            var leftoverCount = extracted.getCount() - inserted;
            if (leftoverCount > 0) {
                slot.safeInsert(extracted.copyWithCount(leftoverCount));
            }
            return inserted > 0;
        }

        var previousTag = deviceStack.getTag() == null ? null : deviceStack.getTag().copy();
        var previousSelection = getStoredSelectedStack(deviceStack);
        var removedStack = removeStackForInventory(deviceStack);
        if (removedStack.isEmpty()) {
            return false;
        }

        // safeInsert は渡したスタック自体を減らすため、挿入前の個数を退避して差分を求める。
        var removedCount = removedStack.getCount();
        var leftover = slot.safeInsert(removedStack);
        var insertedCount = removedCount - leftover.getCount();
        if (insertedCount <= 0) {
            deviceStack.setTag(previousTag);
            return false;
        }
        if (!leftover.isEmpty()) {
            addToDeviceWithoutAutoSelection(deviceStack, leftover, maxStoredItems(player));
            if (!previousSelection.isEmpty() && getStoredCount(deviceStack, previousSelection) > 0) {
                setSelectedStackInternal(deviceStack, previousSelection);
            }
        }
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(
            @NotNull ItemStack deviceStack,
            @NotNull ItemStack otherStack,
            @NotNull Slot slot,
            @NotNull ClickAction action,
            @NotNull Player player,
            @NotNull SlotAccess slotAccess
    ) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }
        if (!InventoryInsertTarget.canModifyStorageSlot(deviceStack, slot, player)) {
            return false;
        }

        if (tryRefillMana(deviceStack, otherStack, slotAccess, player)) {
            slot.setChanged();
            player.playSound(SoundEvents.BOTTLE_EMPTY, 1.0F, 1.0F);
            return true;
        }

        if (otherStack.isEmpty()) {
            var previousTag = deviceStack.getTag() == null ? null : deviceStack.getTag().copy();
            var removedStack = removeStackForInventory(deviceStack);
            if (removedStack.isEmpty()) {
                return false;
            }

            if (!slotAccess.set(removedStack)) {
                deviceStack.setTag(previousTag);
                return false;
            }

            slot.setChanged();
            return true;
        }

        var inserted = addToDevice(deviceStack, otherStack, maxStoredItems(player));
        if (inserted <= 0) {
            return false;
        }

        otherStack.shrink(inserted);
        slotAccess.set(otherStack);
        slot.setChanged();
        return true;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level,
            @NotNull Player player,
            @NotNull InteractionHand usedHand
    ) {
        var deviceStack = player.getItemInHand(usedHand);
        if (getMode(deviceStack) == Mode.CLEAN) {
            return InteractionResultHolder.pass(deviceStack);
        }
        if (getMode(deviceStack) == Mode.SPELL) {
            return tryCastSelectedSpell(level, player, usedHand, deviceStack);
        }
        var selectedStack = getSelectedStack(deviceStack);
        if (selectedStack.isEmpty()) {
            return InteractionResultHolder.pass(deviceStack);
        }
        if (getStoredCount(deviceStack, selectedStack) <= 0) {
            displayOutOfItem(player, selectedStack);
            return InteractionResultHolder.fail(deviceStack);
        }

        var interactionStack = BlockTools.copyForTemporaryUse(selectedStack);
        InteractionResultHolder<ItemStack> delegatedResult;
        try {
            player.setItemInHand(usedHand, interactionStack);
            delegatedResult = interactionStack.getItem().use(level, player, usedHand);
        } finally {
            player.setItemInHand(usedHand, deviceStack);
        }

        if (!level.isClientSide
                && delegatedResult.getResult().consumesAction()) {
            consumeSelectedForUse(player, deviceStack);
            sendItemRemainingNotification(player, deviceStack, selectedStack);
        }
        return new InteractionResultHolder<>(delegatedResult.getResult(), deviceStack);
    }

    private static InteractionResultHolder<ItemStack> tryCastSelectedSpell(
            Level level,
            Player player,
            InteractionHand usedHand,
            ItemStack deviceStack
    ) {
        var spellData = getSelectedSpellData(deviceStack);
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return InteractionResultHolder.pass(deviceStack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(deviceStack, true);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(deviceStack);
        }

        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var magicData = MagicData.getPlayerMagicData(serverPlayer);
        if (magicData == null || magicData.isCasting()) {
            return InteractionResultHolder.fail(deviceStack);
        }

        var consumesMana = !(player.isCreative() && !ServerConfigs.CREATIVE_MANA_COST.get());
        var validationManaCost = consumesMana ? spell.getManaCost(spellLevel) : 0;
        var deviceManaCost = validationManaCost;
        MageLight.CastTarget mageLightTarget = null;
        if (spell == SpellRegistry.MAGE_LIGHT.get()) {
            var mageLight = (MageLight) SpellRegistry.MAGE_LIGHT.get();
            var profile = mageLight.createCastProfile(
                    spellLevel,
                    player,
                    ApprenticeCodexServerConfig.luminousDeviceMageLightExtendedRange()
            );
            var resolvedTarget = mageLight.resolveCastTarget(level, player, profile.effectiveRange());
            if (resolvedTarget.isEmpty()) {
                player.displayClientMessage(
                        Component.translatable("ui.apprenticecodex.cant_place", spell.getDisplayName(player))
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return InteractionResultHolder.fail(deviceStack);
            }
            mageLightTarget = resolvedTarget.get();
            deviceManaCost = consumesMana ? profile.manaCostAt(mageLightTarget.distance()) : 0;
        }

        if (getStoredMana(deviceStack) < deviceManaCost) {
            player.displayClientMessage(
                    Component.translatable(
                            "ui.irons_spellbooks.cast_error_mana",
                            spell.getDisplayName(player)
                    ).withStyle(ChatFormatting.RED),
                    true
            );
            return InteractionResultHolder.fail(deviceStack);
        }

        // Iron's の発動前判定だけを通すため不足分を一時補填し、実消費はデバイス側へ付け替える。
        var borrowedMana = Math.max(0.0F, validationManaCost - magicData.getMana());
        if (borrowedMana > 0.0F) {
            magicData.addMana(borrowedMana);
        }
        var slotId = usedHand == InteractionHand.OFF_HAND
                ? SpellSelectionManager.OFFHAND
                : SpellSelectionManager.MAINHAND;
        var resolvedMageLightTarget = mageLightTarget;
        var casted = resolvedMageLightTarget == null
                ? spell.attemptInitiateCast(
                        deviceStack,
                        spellLevel,
                        level,
                        player,
                        CastSource.SWORD,
                        true,
                        slotId
                )
                : MageLightCastContext.withTarget(player, resolvedMageLightTarget, () ->
                        spell.attemptInitiateCast(
                                deviceStack,
                                spellLevel,
                                level,
                                player,
                                CastSource.SWORD,
                                true,
                                slotId
                        )
                );
        if (!casted) {
            if (borrowedMana > 0.0F) {
                magicData.setMana(Math.max(0.0F, magicData.getMana() - borrowedMana));
            }
            return InteractionResultHolder.fail(deviceStack);
        }

        if (borrowedMana > 0.0F) {
            ItemManaBypassCastEvent.reserveBorrowedMana(serverPlayer, borrowedMana);
        }
        if (deviceManaCost > 0) {
            setStoredMana(deviceStack, getStoredMana(deviceStack) - deviceManaCost);
        }
        sendManaRemainingNotification(serverPlayer, deviceStack);
        return InteractionResultHolder.success(deviceStack);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        var usedHand = context.getHand();
        var deviceStack = player.getItemInHand(usedHand);
        if (getMode(deviceStack) == Mode.CLEAN) {
            return cleanLightSources(context, player, deviceStack);
        }
        var selectedStack = getSelectedStack(deviceStack);
        if (selectedStack.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (getStoredCount(deviceStack, selectedStack) <= 0) {
            displayOutOfItem(player, selectedStack);
            return InteractionResult.FAIL;
        }

        var interactionStack = BlockTools.copyForTemporaryUse(selectedStack);
        InteractionResult delegatedResult;
        try {
            // UseOnContext は生成時の手持ちを保持するため、差し替え後に作り直して元アイテムの設置処理へ渡す。
            player.setItemInHand(usedHand, interactionStack);
            var delegatedHit = new BlockHitResult(
                    context.getClickLocation(),
                    context.getClickedFace(),
                    context.getClickedPos(),
                    context.isInside()
            );
            var delegatedContext = new UseOnContext(player, usedHand, delegatedHit);
            delegatedResult = interactionStack.getItem().useOn(delegatedContext);
        } finally {
            player.setItemInHand(usedHand, deviceStack);
        }

        if (!context.getLevel().isClientSide
                && delegatedResult.consumesAction()) {
            consumeSelectedForUse(player, deviceStack);
            sendItemRemainingNotification(player, deviceStack, selectedStack);
        }
        return delegatedResult;
    }

    private static InteractionResult cleanLightSources(
            UseOnContext context,
            Player player,
            ItemStack deviceStack
    ) {
        var level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        var radius = ApprenticeCodexServerConfig.luminousDeviceCleanRadius();
        var center = context.getClickedPos();
        long recoveredMana = 0;
        var removedCount = 0;
        for (var pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius)
        )) {
            var block = level.getBlockState(pos).getBlock();
            int blockRecovery;
            if (block == BlockRegistry.MAGE_LIGHT_TORCH.get()) {
                blockRecovery = ApprenticeCodexServerConfig.luminousDeviceMageLightManaRecovery();
            } else if (block == BlockRegistry.WIZARDLAMP_LANTERN.get()) {
                blockRecovery = ApprenticeCodexServerConfig.luminousDeviceWizardlampManaRecovery();
            } else {
                continue;
            }

            if (level.destroyBlock(pos, false)) {
                removedCount++;
                recoveredMana += blockRecovery;
            }
        }

        player.getCooldowns().addCooldown(deviceStack.getItem(), CLEAN_COOLDOWN_TICKS);
        if (removedCount <= 0) {
            player.displayClientMessage(
                    Component.translatable("ui.apprenticecodex.luminous_device.not_found")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return InteractionResult.CONSUME;
        }

        var maxStoredMana = serverMaxStoredMana(deviceStack);
        setStoredMana(deviceStack, (int) Math.min(
                maxStoredMana,
                (long) getStoredMana(deviceStack) + recoveredMana
        ));
        sendManaRemainingNotification(player, deviceStack);
        AudioTools.playSoundFromEntity(
                level,
                player,
                SoundRegistry.VANILLA_INSCRIBE_MANA.get(),
                SoundSource.PLAYERS
        );
        return InteractionResult.CONSUME;
    }

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(TagRegistry.Items.LUMINOUS_DEVICE_STORABLE)
                || MalumLuminousDeviceBridge.isSupportedEther(stack));
    }

    public static int addToDevice(ItemStack deviceStack, ItemStack stack) {
        return addToDevice(
                deviceStack,
                stack,
                ApprenticeCodexServerConfig.luminousDeviceMaxStoredItems()
        );
    }

    private static int addToDevice(ItemStack deviceStack, ItemStack stack, int maxStoredItems) {
        var inserted = addToDeviceWithoutAutoSelection(deviceStack, stack, maxStoredItems);
        if (inserted > 0 && getStoredSelectedStack(deviceStack).isEmpty()) {
            var contents = readContents(deviceStack);
            if (!contents.isEmpty()) {
                setSelectedStackInternal(deviceStack, contents.get(0).displayStack);
            }
        }
        return inserted;
    }

    public static boolean canStorePickedUpStack(ItemStack deviceStack, ItemStack stack) {
        if (!(deviceStack.getItem() instanceof LuminousDevice) || !accepts(stack)) {
            return false;
        }

        var normalizedStack = normalizeStoredStack(stack);
        for (var entry : readContents(deviceStack)) {
            if (sameAutoPickupItem(entry.displayStack, normalizedStack)) {
                return true;
            }
        }
        return sameAutoPickupItem(getStoredSelectedStack(deviceStack), normalizedStack);
    }

    public static int storePickedUpStackInInventoryDevices(Player player, ItemStack stack) {
        if (stack.isEmpty() || !accepts(stack)) {
            return 0;
        }

        var beforeCount = stack.getCount();
        storePickedUpStackInDevices(player.getInventory().items, stack);
        storePickedUpStackInDevices(player.getInventory().offhand, stack);
        return beforeCount - stack.getCount();
    }

    public static ItemStack removeStackForInventory(ItemStack deviceStack) {
        var contents = readContents(deviceStack);
        var selectedStack = getStoredSelectedStack(deviceStack);
        var candidateIndex = findRemovalCandidateIndex(contents, selectedStack);
        if (candidateIndex < 0) {
            return ItemStack.EMPTY;
        }

        var entry = contents.get(candidateIndex);
        var removedSelectedEntry = sameStoredItem(entry.displayStack, selectedStack);
        var removedCount = Math.min(entry.count, entry.displayStack.getMaxStackSize());
        var removedStack = entry.displayStack.copyWithCount(removedCount);
        entry.count -= removedCount;
        if (entry.count <= 0) {
            contents.remove(candidateIndex);
        }
        writeContents(deviceStack, contents);

        if (contents.isEmpty()) {
            clearSelectedStack(deviceStack);
        } else if (removedSelectedEntry && countStoredItem(contents, selectedStack) <= 0) {
            setSelectedStackInternal(deviceStack, contents.get(0).displayStack);
        }
        return removedStack;
    }

    public static int getStoredItemCount(ItemStack deviceStack) {
        var total = 0;
        for (var entry : readContents(deviceStack)) {
            total += entry.count;
        }
        return total;
    }

    public static int getStoredMana(ItemStack deviceStack) {
        if (!(deviceStack.getItem() instanceof LuminousDevice)) {
            return 0;
        }

        var storageTag = deviceStack.getTagElement(STORAGE_TAG);
        return storageTag == null ? 0 : Math.max(0, storageTag.getInt(MANA_TAG));
    }

    public static void setStoredMana(ItemStack deviceStack, int mana) {
        if (!(deviceStack.getItem() instanceof LuminousDevice)) {
            return;
        }

        var normalizedMana = Math.max(0, mana);
        if (normalizedMana > 0) {
            deviceStack.getOrCreateTagElement(STORAGE_TAG).putInt(MANA_TAG, normalizedMana);
            return;
        }

        var storageTag = deviceStack.getTagElement(STORAGE_TAG);
        if (storageTag != null) {
            storageTag.remove(MANA_TAG);
            removeEmptyStorageTag(deviceStack);
        }
    }

    public static boolean hasUpgrade(ItemStack deviceStack, LuminousDeviceUpgrade upgrade) {
        if (!(deviceStack.getItem() instanceof LuminousDevice)) {
            return false;
        }
        var storageTag = deviceStack.getTagElement(STORAGE_TAG);
        if (storageTag == null || !storageTag.contains(UPGRADES_TAG, Tag.TAG_LIST)) {
            return false;
        }
        var upgrades = storageTag.getList(UPGRADES_TAG, Tag.TAG_STRING);
        for (var index = 0; index < upgrades.size(); ++index) {
            if (upgrade.id().toString().equals(upgrades.getString(index))) {
                return true;
            }
        }
        return false;
    }

    public static boolean addUpgrade(ItemStack deviceStack, LuminousDeviceUpgrade upgrade) {
        if (!(deviceStack.getItem() instanceof LuminousDevice) || hasUpgrade(deviceStack, upgrade)) {
            return false;
        }
        var storageTag = deviceStack.getOrCreateTagElement(STORAGE_TAG);
        var upgrades = storageTag.contains(UPGRADES_TAG, Tag.TAG_LIST)
                ? storageTag.getList(UPGRADES_TAG, Tag.TAG_STRING)
                : new ListTag();
        upgrades.add(StringTag.valueOf(upgrade.id().toString()));
        storageTag.put(UPGRADES_TAG, upgrades);
        return true;
    }

    @Override
    public InsertHint getInventoryInsertHint(ItemStack storageStack, ItemStack incomingStack, Player player) {
        if (!(storageStack.getItem() instanceof LuminousDevice) || incomingStack.isEmpty()) {
            return InsertHint.NONE;
        }
        if (createManaRefillPlan(storageStack, incomingStack, player) != null) {
            return InsertHint.MANA;
        }
        return accepts(incomingStack) && getStoredItemCount(storageStack) < maxStoredItems(player)
                ? InsertHint.ITEM
                : InsertHint.NONE;
    }

    public static ItemStack createUpgradeResult(ItemStack deviceStack, LuminousDeviceUpgrade upgrade) {
        if (!(deviceStack.getItem() instanceof LuminousDevice) || hasUpgrade(deviceStack, upgrade)) {
            return ItemStack.EMPTY;
        }
        var result = deviceStack.copyWithCount(1);
        return addUpgrade(result, upgrade) ? result : ItemStack.EMPTY;
    }

    public static int getStoredCount(ItemStack deviceStack, ItemStack targetStack) {
        return countStoredItem(readContents(deviceStack), targetStack);
    }

    public static boolean consumeOneStored(ItemStack deviceStack, ItemStack targetStack) {
        var normalizedTarget = normalizeStoredStack(targetStack);
        var contents = readContents(deviceStack);
        for (int i = 0; i < contents.size(); ++i) {
            var entry = contents.get(i);
            if (!sameStoredItem(entry.displayStack, normalizedTarget) || entry.count <= 0) {
                continue;
            }

            entry.count -= 1;
            if (entry.count <= 0) {
                contents.remove(i);
            }
            writeContents(deviceStack, contents);
            // LinearBuild から消費した場合も、補充対象とテンプレートを維持するため選択状態は残す。
            return true;
        }
        return false;
    }

    public static ItemStack getSelectedStack(ItemStack deviceStack) {
        if (getMode(deviceStack) != Mode.PLACE) {
            return ItemStack.EMPTY;
        }
        return getStoredSelectedStack(deviceStack);
    }

    private static void appendModeTooltip(ItemStack stack, List<Component> lines) {
        if (getMode(stack) == Mode.CLEAN) {
            var size = LuminousDeviceConfigState.cleanSize();
            lines.add(Component.translatable(
                    descriptionIdFor(stack) + ".mode",
                    Component.translatable(descriptionIdFor(stack) + ".mode.clean"),
                    Component.translatable(
                            descriptionIdFor(stack) + ".mode.clean_size",
                            size,
                            size,
                            size
                    )
            ).withStyle(ChatFormatting.GRAY));
            return;
        }

        var selectedSpell = getSelectedSpellData(stack);
        if (selectedSpell != SpellData.EMPTY) {
            lines.add(Component.translatable(
                    descriptionIdFor(stack) + ".mode",
                    Component.translatable(descriptionIdFor(stack) + ".mode.spell"),
                    createSpellDisplayName(selectedSpell)
            ).withStyle(ChatFormatting.GRAY));
            return;
        }

        var selectedStack = getSelectedStack(stack);
        if (!selectedStack.isEmpty()) {
            lines.add(Component.translatable(
                    descriptionIdFor(stack) + ".mode",
                    Component.translatable(descriptionIdFor(stack) + ".mode.place"),
                    selectedStack.getHoverName()
            ).withStyle(ChatFormatting.GRAY));
        }
    }

    private static String descriptionIdFor(ItemStack stack) {
        return stack.getItem().getDescriptionId();
    }

    private static ItemStack getStoredSelectedStack(ItemStack deviceStack) {
        if (!(deviceStack.getItem() instanceof LuminousDevice)) {
            return ItemStack.EMPTY;
        }

        var storageTag = deviceStack.getTagElement(STORAGE_TAG);
        if (storageTag == null || !storageTag.contains(SELECTED_STACK_TAG, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        return normalizeStoredStack(ItemStack.of(storageTag.getCompound(SELECTED_STACK_TAG)));
    }

    public static boolean setSelectedStack(ItemStack deviceStack, ItemStack requestedStack) {
        if (!(deviceStack.getItem() instanceof LuminousDevice) || requestedStack.isEmpty()) {
            return false;
        }

        var normalizedRequest = normalizeStoredStack(requestedStack);
        var currentSelection = getStoredSelectedStack(deviceStack);
        if (!sameStoredItem(currentSelection, normalizedRequest)
                && getStoredCount(deviceStack, normalizedRequest) <= 0) {
            return false;
        }
        setSelectedStackInternal(deviceStack, normalizedRequest);
        setModeInternal(deviceStack, Mode.PLACE);
        return true;
    }

    public static Mode getMode(ItemStack deviceStack) {
        if (!(deviceStack.getItem() instanceof LuminousDevice)) {
            return Mode.PLACE;
        }

        var storageTag = deviceStack.getTagElement(STORAGE_TAG);
        if (storageTag == null || !storageTag.contains(MODE_TAG, Tag.TAG_STRING)) {
            return Mode.PLACE;
        }
        var mode = Mode.fromSerializedName(storageTag.getString(MODE_TAG));
        if (mode == Mode.CLEAN && !hasUpgrade(deviceStack, LuminousDeviceUpgrade.CLEAN)) {
            return Mode.PLACE;
        }
        if (mode == Mode.SPELL) {
            var spellId = ResourceLocation.tryParse(storageTag.getString(SELECTED_SPELL_TAG));
            if (getAllowedSpell(deviceStack, spellId) == null) {
                return Mode.PLACE;
            }
        }
        return mode;
    }

    public static boolean setCleanMode(ItemStack deviceStack) {
        if (!(deviceStack.getItem() instanceof LuminousDevice)
                || !hasUpgrade(deviceStack, LuminousDeviceUpgrade.CLEAN)) {
            return false;
        }
        setModeInternal(deviceStack, Mode.CLEAN);
        return true;
    }

    public static SpellData getSelectedSpellData(ItemStack deviceStack) {
        if (!(deviceStack.getItem() instanceof LuminousDevice) || getMode(deviceStack) != Mode.SPELL) {
            return SpellData.EMPTY;
        }

        var storageTag = deviceStack.getTagElement(STORAGE_TAG);
        if (storageTag == null || !storageTag.contains(SELECTED_SPELL_TAG, Tag.TAG_STRING)) {
            return SpellData.EMPTY;
        }
        var spellId = ResourceLocation.tryParse(storageTag.getString(SELECTED_SPELL_TAG));
        var spell = getAllowedSpell(deviceStack, spellId);
        return spell == null ? SpellData.EMPTY : new SpellData(spell, SPELL_LEVEL);
    }

    public static boolean setSelectedSpell(ItemStack deviceStack, @Nullable ResourceLocation spellId) {
        if (!(deviceStack.getItem() instanceof LuminousDevice) || getAllowedSpell(deviceStack, spellId) == null) {
            return false;
        }

        deviceStack.getOrCreateTagElement(STORAGE_TAG).putString(SELECTED_SPELL_TAG, spellId.toString());
        setModeInternal(deviceStack, Mode.SPELL);
        return true;
    }

    public static List<SelectionView> getSelectionViews(ItemStack deviceStack) {
        if (!(deviceStack.getItem() instanceof LuminousDevice)) {
            return List.of();
        }

        var mode = getMode(deviceStack);
        var selectedStack = getStoredSelectedStack(deviceStack);
        var views = new ArrayList<SelectionView>();
        var selectedPresent = false;
        for (var entry : readContents(deviceStack)) {
            var currentSelection = mode == Mode.PLACE && sameStoredItem(entry.displayStack, selectedStack);
            selectedPresent |= currentSelection;
            views.add(createSelectionView(entry.displayStack, entry.count, currentSelection));
        }
        if (mode == Mode.PLACE && !selectedStack.isEmpty() && !selectedPresent) {
            views.add(createSelectionView(selectedStack, 0, true));
        }
        if (hasUpgrade(deviceStack, LuminousDeviceUpgrade.CLEAN)) {
            views.add(new SelectionView(
                    Mode.CLEAN,
                    deviceStack.copyWithCount(1),
                    null,
                    null,
                    Component.translatable("ui.apprenticecodex.luminous_device.clean.desc"),
                    "",
                    SELECTION_COUNT_COLOR,
                    mode == Mode.CLEAN
            ));
        }
        if (hasUpgrade(deviceStack, LuminousDeviceUpgrade.ENHANCED_MAGE_LIGHT)) {
            addSpellSelectionView(views, SpellRegistry.MAGE_LIGHT.get(), mode, getSelectedSpellData(deviceStack));
        }
        if (hasUpgrade(deviceStack, LuminousDeviceUpgrade.MANA_WIZARDLAMP)) {
            addSpellSelectionView(views, SpellRegistry.WIZARDLAMP.get(), mode, getSelectedSpellData(deviceStack));
        }
        return List.copyOf(views);
    }

    private static void addSpellSelectionView(
            List<SelectionView> views,
            AbstractSpell spell,
            Mode mode,
            SpellData selectedSpell
    ) {
        views.add(new SelectionView(
                Mode.SPELL,
                ItemStack.EMPTY,
                spell.getSpellIconResource(),
                spell.getSpellResource(),
                createSpellDisplayName(new SpellData(spell, SPELL_LEVEL)),
                "",
                SELECTION_COUNT_COLOR,
                mode == Mode.SPELL
                        && selectedSpell != SpellData.EMPTY
                        && selectedSpell.getSpell() == spell
        ));
    }

    private static SelectionView createSelectionView(ItemStack stack, int count, boolean currentSelection) {
        return new SelectionView(
                Mode.PLACE,
                stack.copyWithCount(1),
                null,
                null,
                stack.getHoverName(),
                CompactCountFormatter.format(count).toLowerCase(java.util.Locale.ROOT),
                count > 0 ? SELECTION_COUNT_COLOR : EMPTY_SELECTION_COUNT_COLOR,
                currentSelection
        );
    }

    private static Component createSpellDisplayName(SpellData spellData) {
        var spell = spellData.getSpell();
        return spell.getDisplayName(null)
                .copy()
                .append(" ")
                .append(Integer.toString(spellData.getLevel()))
                .withStyle(spell.getSchoolType().getDisplayName().getStyle());
    }

    @Nullable
    private static AbstractSpell getAllowedSpell(ItemStack deviceStack, @Nullable ResourceLocation spellId) {
        if (spellId == null) {
            return null;
        }
        if (spellId.equals(SpellRegistry.MAGE_LIGHT.get().getSpellResource())
                && hasUpgrade(deviceStack, LuminousDeviceUpgrade.ENHANCED_MAGE_LIGHT)) {
            return SpellRegistry.MAGE_LIGHT.get();
        }
        if (spellId.equals(SpellRegistry.WIZARDLAMP.get().getSpellResource())
                && hasUpgrade(deviceStack, LuminousDeviceUpgrade.MANA_WIZARDLAMP)) {
            return SpellRegistry.WIZARDLAMP.get();
        }
        return null;
    }

    @Override
    public boolean supportsManaBypass(@Nullable AbstractSpell spell) {
        return spell == SpellRegistry.MAGE_LIGHT.get() || spell == SpellRegistry.WIZARDLAMP.get();
    }

    private static int addToDeviceWithoutAutoSelection(
            ItemStack deviceStack,
            ItemStack stack,
            int maxStoredItems
    ) {
        if (!(deviceStack.getItem() instanceof LuminousDevice) || !accepts(stack)) {
            return 0;
        }

        var remainingSpace = maxStoredItems - getStoredItemCount(deviceStack);
        var inserted = Math.min(remainingSpace, stack.getCount());
        if (inserted <= 0) {
            return 0;
        }

        var normalizedStack = normalizeStoredStack(stack);
        var contents = readContents(deviceStack);
        for (var entry : contents) {
            if (!sameStoredItem(entry.displayStack, normalizedStack)) {
                continue;
            }
            entry.count += inserted;
            writeContents(deviceStack, contents);
            return inserted;
        }

        // ItemStack の Count を1024まで拡張せず、表示用スタックと実数を分けて保持する。
        contents.add(new StoredEntry(normalizedStack, inserted));
        writeContents(deviceStack, contents);
        return inserted;
    }

    private static void storePickedUpStackInDevices(List<ItemStack> inventoryStacks, ItemStack pickedUpStack) {
        for (var deviceStack : inventoryStacks) {
            if (!canStorePickedUpStack(deviceStack, pickedUpStack)) {
                continue;
            }

            pickedUpStack.shrink(addToDeviceWithoutAutoSelection(
                    deviceStack,
                    pickedUpStack,
                    ApprenticeCodexServerConfig.luminousDeviceMaxStoredItems()
            ));
            if (pickedUpStack.isEmpty()) {
                return;
            }
        }
    }

    private static boolean consumeSelectedForUse(Player player, ItemStack deviceStack) {
        // クリエイティブの非消費は設置・使用成功後のこの経路だけに限定する。
        if (player.getAbilities().instabuild) {
            return false;
        }

        var selectedStack = getSelectedStack(deviceStack);
        if (selectedStack.isEmpty()) {
            return false;
        }

        var contents = readContents(deviceStack);
        for (int i = 0; i < contents.size(); ++i) {
            var entry = contents.get(i);
            if (!sameStoredItem(entry.displayStack, selectedStack) || entry.count <= 0) {
                continue;
            }

            entry.count -= 1;
            if (entry.count <= 0) {
                contents.remove(i);
            }
            writeContents(deviceStack, contents);
            // 使用消費では、空になっても選択中の表示用スタックを意図的に残す。
            return true;
        }
        return false;
    }

    private static boolean tryRefillMana(
            ItemStack deviceStack,
            ItemStack inputStack,
            SlotAccess slotAccess,
            Player player
    ) {
        var refillPlan = createManaRefillPlan(deviceStack, inputStack, player);
        if (refillPlan == null) {
            return false;
        }
        if (!slotAccess.set(refillPlan.remainingStack())) {
            return false;
        }
        setStoredMana(deviceStack, (int) Math.min(
                refillPlan.maxStoredMana(),
                (long) getStoredMana(deviceStack) + refillPlan.recoveredMana()
        ));
        return true;
    }

    private static @Nullable ManaRefillPlan createManaRefillPlan(
            ItemStack deviceStack,
            ItemStack inputStack,
            Player player
    ) {
        if (!(deviceStack.getItem() instanceof LuminousDevice) || inputStack.isEmpty()) {
            return null;
        }

        var maxStoredMana = maxStoredMana(player, deviceStack);
        if (maxStoredMana <= 0 || getStoredMana(deviceStack) >= maxStoredMana) {
            return null;
        }

        ItemStack potionStack;
        ItemStack remainingStack;
        var amplifierBonus = 0;
        if (inputStack.is(ItemRegistry.SPELLCASTERS_FLASK.get())
                && SpellcastersFlask.canExtractOneDose(inputStack)) {
            potionStack = SpellcastersFlask.getStoredItem(inputStack);
            remainingStack = SpellcastersFlask.copyAfterExtractingOneDose(inputStack);
            amplifierBonus = EnchantmentRegistry.GLOW_ENERGY.isPresent()
                    ? inputStack.getEnchantmentLevel(EnchantmentRegistry.GLOW_ENERGY.get())
                    : 0;
        } else if (ManaPotionRecoveryHelper.isSupportedManaPotion(inputStack)) {
            potionStack = inputStack;
            remainingStack = new ItemStack(Items.GLASS_BOTTLE);
        } else {
            return null;
        }

        var recoveredMana = ManaPotionRecoveryHelper.getRecovery(
                potionStack,
                maxStoredMana,
                amplifierBonus
        );
        return recoveredMana > 0 && !remainingStack.isEmpty()
                ? new ManaRefillPlan(remainingStack, recoveredMana, maxStoredMana)
                : null;
    }

    private static int maxStoredItems(Player player) {
        return player.level().isClientSide
                ? LuminousDeviceConfigState.maxStoredItems()
                : ApprenticeCodexServerConfig.luminousDeviceMaxStoredItems();
    }

    private static int maxStoredMana(Player player, ItemStack deviceStack) {
        return player.level().isClientSide
                ? clientMaxStoredMana(deviceStack)
                : serverMaxStoredMana(deviceStack);
    }

    private static int clientMaxStoredMana(ItemStack deviceStack) {
        return hasUpgrade(deviceStack, LuminousDeviceUpgrade.MANA_WIZARDLAMP)
                ? Math.max(LuminousDeviceConfigState.maxStoredMana(), LuminousDeviceConfigState.upgradedMaxStoredMana())
                : LuminousDeviceConfigState.maxStoredMana();
    }

    private static int serverMaxStoredMana(ItemStack deviceStack) {
        return hasUpgrade(deviceStack, LuminousDeviceUpgrade.MANA_WIZARDLAMP)
                ? Math.max(
                        ApprenticeCodexServerConfig.luminousDeviceMaxStoredMana(),
                        ApprenticeCodexServerConfig.luminousDeviceUpgradedMaxStoredMana()
                )
                : ApprenticeCodexServerConfig.luminousDeviceMaxStoredMana();
    }

    private record ManaRefillPlan(ItemStack remainingStack, int recoveredMana, int maxStoredMana) {
    }

    private static Component createUpgradeTooltipEntry(ItemStack stack, LuminousDeviceUpgrade upgrade) {
        return Component.translatable(upgrade.translationKey()).withStyle(
                hasUpgrade(stack, upgrade) ? ChatFormatting.GREEN : ChatFormatting.GRAY
        );
    }

    private static void displayOutOfItem(Player player, ItemStack selectedStack) {
        if (player.level().isClientSide) {
            return;
        }
        player.displayClientMessage(
                Component.translatable(
                        "ui.apprenticecodex.luminous_device.out_of_item",
                        selectedStack.getHoverName()
                ).withStyle(ChatFormatting.RED),
                true
        );
    }

    private static void sendItemRemainingNotification(
            Player player,
            ItemStack deviceStack,
            ItemStack selectedStack
    ) {
        sendRemainingNotification(
                player,
                selectedStack,
                getStoredCount(deviceStack, selectedStack),
                SyncRemainingCountNotificationPacket.DisplayType.ITEM_REMAINING
        );
    }

    private static void sendManaRemainingNotification(Player player, ItemStack deviceStack) {
        sendRemainingNotification(
                player,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()),
                getStoredMana(deviceStack),
                SyncRemainingCountNotificationPacket.DisplayType.MANA_REMAINING
        );
    }

    private static void sendRemainingNotification(
            Player player,
            ItemStack iconStack,
            long remainingCount,
            SyncRemainingCountNotificationPacket.DisplayType displayType
    ) {
        if (player.getAbilities().instabuild
                || !(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.connection == null) {
            return;
        }

        Networks.sendToPlayer(serverPlayer, new SyncRemainingCountNotificationPacket(
                ItemRegistry.LUMINOUS_DEVICE.getId().toString(),
                iconStack,
                remainingCount,
                displayType
        ));
    }

    private static List<StoredEntry> readContents(ItemStack deviceStack) {
        var storageTag = deviceStack.getTagElement(STORAGE_TAG);
        if (storageTag == null || !storageTag.contains(CONTENTS_TAG, Tag.TAG_LIST)) {
            return new ArrayList<>();
        }

        var contentsTag = storageTag.getList(CONTENTS_TAG, Tag.TAG_COMPOUND);
        var contents = new ArrayList<StoredEntry>(contentsTag.size());
        for (var entryTag : contentsTag) {
            if (!(entryTag instanceof CompoundTag compoundTag)) {
                continue;
            }

            var storedStack = normalizeStoredStack(ItemStack.of(compoundTag.getCompound(STACK_TAG)));
            var storedCount = compoundTag.getInt(COUNT_TAG);
            if (!storedStack.isEmpty() && storedCount > 0) {
                mergeStoredEntry(contents, storedStack, storedCount);
            }
        }
        return contents;
    }

    private static void writeContents(ItemStack deviceStack, List<StoredEntry> contents) {
        var storageTag = deviceStack.getOrCreateTagElement(STORAGE_TAG);
        var contentsTag = new ListTag();
        for (var entry : contents) {
            if (entry.displayStack.isEmpty() || entry.count <= 0) {
                continue;
            }
            var entryTag = new CompoundTag();
            entryTag.put(STACK_TAG, normalizeStoredStack(entry.displayStack).save(new CompoundTag()));
            entryTag.putInt(COUNT_TAG, entry.count);
            contentsTag.add(entryTag);
        }

        if (contentsTag.isEmpty()) {
            storageTag.remove(CONTENTS_TAG);
        } else {
            storageTag.put(CONTENTS_TAG, contentsTag);
        }
        removeEmptyStorageTag(deviceStack);
    }

    private static void setSelectedStackInternal(ItemStack deviceStack, ItemStack selectedStack) {
        deviceStack.getOrCreateTagElement(STORAGE_TAG).put(
                SELECTED_STACK_TAG,
                normalizeStoredStack(selectedStack).save(new CompoundTag())
        );
    }

    private static void setModeInternal(ItemStack deviceStack, Mode mode) {
        var storageTag = deviceStack.getOrCreateTagElement(STORAGE_TAG);
        if (mode == Mode.PLACE) {
            storageTag.remove(MODE_TAG);
            removeEmptyStorageTag(deviceStack);
            return;
        }
        storageTag.putString(MODE_TAG, mode.serializedName);
    }

    private static void clearSelectedStack(ItemStack deviceStack) {
        var storageTag = deviceStack.getTagElement(STORAGE_TAG);
        if (storageTag == null) {
            return;
        }
        storageTag.remove(SELECTED_STACK_TAG);
        removeEmptyStorageTag(deviceStack);
    }

    private static void removeEmptyStorageTag(ItemStack deviceStack) {
        var storageTag = deviceStack.getTagElement(STORAGE_TAG);
        if (storageTag != null && storageTag.isEmpty()) {
            deviceStack.removeTagKey(STORAGE_TAG);
        }
    }

    private static int countStoredItem(List<StoredEntry> contents, ItemStack targetStack) {
        var normalizedTarget = normalizeStoredStack(targetStack);
        if (normalizedTarget.isEmpty()) {
            return 0;
        }

        var total = 0;
        for (var entry : contents) {
            if (sameStoredItem(entry.displayStack, normalizedTarget)) {
                total += entry.count;
            }
        }
        return total;
    }

    private static int findRemovalCandidateIndex(List<StoredEntry> contents, ItemStack selectedStack) {
        for (int i = 0; i < contents.size(); ++i) {
            var entry = contents.get(i);
            if (entry.count > 0 && sameStoredItem(entry.displayStack, selectedStack)) {
                return i;
            }
        }

        // 将来、魔法などアイテム以外が選択されている場合も取り出せるよう、従来の最少数規則を残す。
        var candidateIndex = -1;
        var candidateCount = Integer.MAX_VALUE;
        for (int i = 0; i < contents.size(); ++i) {
            var entry = contents.get(i);
            if (entry.displayStack.isEmpty() || entry.count <= 0) {
                continue;
            }
            if (entry.count < candidateCount) {
                candidateIndex = i;
                candidateCount = entry.count;
            }
        }
        return candidateIndex;
    }

    private static boolean sameStoredItem(ItemStack first, ItemStack second) {
        return !first.isEmpty() && !second.isEmpty() && ItemStack.isSameItemSameTags(first, second);
    }

    private static boolean sameAutoPickupItem(ItemStack first, ItemStack second) {
        return sameStoredItem(first, second)
                || MalumLuminousDeviceBridge.isSameEtherIgnoringColor(first, second);
    }

    private static ItemStack normalizeStoredStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var normalizedStack = stack.copyWithCount(1);
        MalumLuminousDeviceBridge.normalizeForStorage(normalizedStack);
        return normalizedStack;
    }

    private static void mergeStoredEntry(List<StoredEntry> contents, ItemStack stack, int count) {
        for (var entry : contents) {
            if (!sameStoredItem(entry.displayStack, stack)) {
                continue;
            }
            entry.count = (int) Math.min(Integer.MAX_VALUE, (long) entry.count + count);
            return;
        }
        contents.add(new StoredEntry(stack, count));
    }

    public enum Mode {
        PLACE("place"),
        CLEAN("clean"),
        SPELL("spell");

        private final String serializedName;

        Mode(String serializedName) {
            this.serializedName = serializedName;
        }

        private static Mode fromSerializedName(String serializedName) {
            for (var mode : values()) {
                if (mode.serializedName.equals(serializedName)) {
                    return mode;
                }
            }
            return PLACE;
        }
    }

    public record SelectionView(
            Mode mode,
            ItemStack iconStack,
            @Nullable ResourceLocation iconTexture,
            @Nullable ResourceLocation spellId,
            Component displayName,
            String badgeText,
            int badgeColor,
            boolean currentSelection
    ) {
        public SelectionView {
            iconStack = iconStack.copyWithCount(1);
        }
    }

    private static final class StoredEntry {
        private final ItemStack displayStack;
        private int count;

        private StoredEntry(ItemStack displayStack, int count) {
            this.displayStack = displayStack;
            this.count = count;
        }
    }
}
